/*
 * Copyright 2020-2020 Equinix, Inc
 * Copyright 2014-2020 The Billing Project, LLC
 *
 * The Billing Project licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package queue

import (
	"context"
	"io"
	"sync"
	"time"

	"github.com/golang/protobuf/ptypes/timestamp"
	"github.com/google/uuid"
	qapi "github.com/killbill/standalone-queue/gen-go/api"
	"google.golang.org/grpc"
	"google.golang.org/grpc/keepalive"
	"google.golang.org/grpc/status"
)

// State defines the state of the queue
type State int

const (
	// Closed is the unitial state prior we called SubscribeEvents or after we have called Close
	Closed State = iota
	// Connected means a successfully connected state
	Connected
	// Disconnected means the Go routine receiving stream events got an error
	Disconnected
)

var _ Queue = &queue{}

// Queue defines a set of operations to interact with the queue
type Queue interface {
	PostEvent(ctx context.Context, json string) error
	SubscribeEvents(ctx context.Context, handlerFn func(ev *qapi.EventMsg) error, closeFn func()) error
	AckEvent(ctx context.Context, userToken string, success bool) error
	Close(ctx context.Context)
}

// Logger is a simple logger abstraction so that clients can provide their own logger
type Logger interface {
	Infof(context.Context, string, ...interface{})
	Warnf(context.Context, error, string, ...interface{})
	Errorf(context.Context, error, string, ...interface{})
}

// NewQueue creates a new queue instance
// Returns an error if it's not able to create a transport
func NewQueue(serverAddr string, apiAttempts int, clientID string, searchKey1 int64, searchKey2 int64, keepAlive bool, logger Logger, grpcOpts []grpc.DialOption) (Queue, error) {

	if !keepAlive {
		logger.Warnf(context.Background(), nil, "Queue created with keepAlive=false")
	}

	if apiAttempts < 1 {
		apiAttempts = 1
	}

	queue := &queue{
		serverAddr:  serverAddr,
		clientID:    clientID,
		searchKey1:  searchKey1,
		searchKey2:  searchKey2,
		keepAlive:   keepAlive,
		apiAttempts: apiAttempts,
		state:       Closed,
		log:         logger,
	}

	err := queue.createTransport(grpcOpts)
	if err != nil {
		return nil, err
	}
	return queue, nil
}

type queue struct {
	// Queue settings
	serverAddr string
	clientID   string
	searchKey1 int64
	searchKey2 int64
	// Client app specifies its configured logger
	log Logger
	// This is should be set to true unless in test mode
	keepAlive bool
	// # times we attempt an api call (post or subscribe) before given up
	apiAttempts int
	// Connection/api management
	mux   sync.Mutex
	state State
	conn  *grpc.ClientConn
	qapi  qapi.QueueApiClient
}

func (q *queue) PostEvent(ctx context.Context, json string) error {

	var delayDur = time.Second
	maxAttempts := q.apiAttempts
	curAttempts := 0

	for {
		_, err := q.qapi.PostEvent(ctx, &qapi.EventMsg{
			ClientId:        q.clientID,
			EventJson:       json,
			UserToken:       uuid.New().String(),
			FutureUserToken: "",
			EffectiveDate:   toTimestamp(time.Now().UTC()),
			SearchKey1:      q.searchKey1,
			SearchKey2:      q.searchKey2,
		})
		if err == nil {
			return nil
		}

		curAttempts++
		if curAttempts >= maxAttempts {
			q.log.Errorf(ctx, err, "Queue::PostEvent: failed to post event")
			return err
		}

		q.log.Warnf(ctx, err, "Queue::PostEvent: failed to post event, attempts=[%d/%d] sleeping %d sec and retry",
			curAttempts, maxAttempts, delayDur.Seconds())
		time.Sleep(delayDur)
		delayDur = delayDur * 2
	}
}

func (q *queue) AckEvent(ctx context.Context, userToken string, success bool) error {

	var delayDur = time.Second
	maxAttempts := q.apiAttempts
	curAttempts := 0

	for {
		_, err := q.qapi.Ack(ctx, &qapi.AckRequest{
			UserToken: userToken,
			Success:   success,
		})
		if err == nil {
			return nil
		}

		curAttempts++
		if curAttempts >= maxAttempts {
			q.log.Errorf(ctx, err, "Queue::Ack: failed to ack event")
			return err
		}

		q.log.Warnf(ctx, err, "Queue::Ack: failed to ack event, attempts=[%d/%d] sleeping %d sec and retry",
			curAttempts, maxAttempts, delayDur.Seconds())
		time.Sleep(delayDur)
		delayDur = delayDur * 2
	}
}

func (q *queue) SubscribeEvents(ctx context.Context, handlerFn func(ev *qapi.EventMsg) error, closeFn func()) error {

	q.mux.Lock()
	defer q.mux.Unlock()

	// If we are Connected or in the process or connecting, return
	if q.state != Closed {
		q.log.Infof(ctx, "Queue::SubscribeEvents: already connected state=%i, ignore", q.state)
		return nil
	}

	stream, err := q.subscribeWithAttempts(ctx, q.apiAttempts)
	if err != nil {
		// We don't try to automatically re-subscribe, we let the client decide and handle retries if necessary
		return err
	}
	// Start gor to listen to events (and reconnect if/when necessary)
	go func() {
		q.listen(ctx, stream, handlerFn, closeFn)
	}()

	return nil
}

// This is called with Lock held
func (q *queue) subscribeWithAttempts(ctx context.Context, maxAttempts int) (qapi.QueueApi_SubscribeEventsClient, error) {

	q.log.Infof(ctx, "Queue::subscribeWithAttempts: enter")

	var delayDur = time.Second
	var stream qapi.QueueApi_SubscribeEventsClient
	var err error

	curAttempts := 0
	for stream == nil {
		// TODO do we need a context.Background() here?
		stream, err = q.qapi.SubscribeEvents(ctx, &qapi.SubscriptionRequest{
			ClientId:   q.clientID,
			SearchKey2: q.searchKey2,
		})
		if err == nil {
			q.state = Connected
			break
		}

		curAttempts++
		if curAttempts >= maxAttempts {
			q.state = Closed
			q.log.Errorf(ctx, err, "Queue::subscribeWithAttempts: failed to subscribe to events attempts=[%d/%d] state=%i", curAttempts, maxAttempts, q.state)
			return nil, err
		}

		q.log.Errorf(ctx, err, "Queue::subscribeWithAttempts: failed to re-subscribe to events, attempts=[%d/%d] state=%d sleeping %d sec and retry",
			curAttempts, maxAttempts, q.state, delayDur.Seconds())
		time.Sleep(delayDur)
		delayDur = delayDur * 2
	}
	return stream, nil
}

func (q *queue) listen(ctx context.Context, originalStream qapi.QueueApi_SubscribeEventsClient, handlerFn func(ev *qapi.EventMsg) error, closeFn func()) {

	// over a day of trying...
	const maxAttempts = 16
	stream := originalStream
	q.log.Infof(ctx, "Queue::listen (gor) state=%i: starting ", q.state)

	for {
		// Receive and forwards events without lock until something happens...
		evt, err := stream.Recv()
		if err == nil && evt != nil {
			handlerFn(evt)
			continue
		}

		// In all other case, EOF or error, we grab the lock and based on state/err decide whether we
		// want to resubscribe or exit
		q.mux.Lock()

		// Normal termination after Closed was called
		if err == io.EOF && q.state == Closed {
			defer func() {
				closeFn()
				q.mux.Unlock()
			}()
			q.log.Infof(ctx, "Queue::listen (gor) state=%i: EOF stream upon Close, exit...", q.state)
			return
		}

		// Either an unexpected EOF or an error
		q.state = Disconnected
		stream = nil

		if err == io.EOF {
			q.log.Errorf(ctx, err, "Queue::listen (gor) state=%i: unexpected EOF stream", q.state)
		} else if err != nil {
			serr, ok := status.FromError(err)
			if !ok {
				q.log.Errorf(ctx, err, "Queue::listen (gor) state=%i: failed to receive event stream", q.state)
			} else {
				status := serr.Code().String()
				q.log.Errorf(ctx, err, "Queue::listen (gor) state=%i grpc_status=%s: failed to receive event stream", q.state, status)
			}
		}

		if stream == nil {
			stream, err = q.subscribeWithAttempts(ctx, maxAttempts)
			if err != nil {
				q.log.Errorf(ctx, err, "Queue::listen (gor) state=%i: fatal failure to re-connect after %d attempts, exiting :-(", q.state)
				q.mux.Unlock()
				// TODO Should we call os.Exit(1), maybe configurable?
				return
			}
		}
		q.mux.Unlock()
	}
}

func (q *queue) Close(ctx context.Context) {

	q.mux.Lock()
	defer func() {
		// Final state
		q.state = Closed
		// Close our side of the connection(s)
		q.log.Infof(ctx, "Queue::Close: closing client transport state=%i", q.state)
		err := q.conn.Close()
		if err != nil {
			q.log.Errorf(ctx, err, "Queue::Close: failed to close client transport state=%i", q.state)
		}
		q.mux.Unlock()
	}()

	if q.state == Closed {
		q.log.Infof(ctx, "Queue::Close: not connected ignore state=%i", q.state)
		return
	}

	// Attempt the Close call regardless of state
	q.log.Infof(ctx, "Queue::Close: closing server connection")
	_, err := q.qapi.Close(ctx, &qapi.CloseRequest{
		ClientId: q.clientID,
	})
	if err != nil {
		// If we have an error and we were in Connected mode, log the error, otherwise ignore
		if q.state == Connected {
			q.log.Errorf(ctx, err, "Queue::Close: failed to close event stream state=%i", q.state)
		}
	}
}

func (q *queue) createTransport(opts []grpc.DialOption) error {

	q.mux.Lock()
	defer q.mux.Unlock()

	q.log.Infof(context.Background(), "Queue::createTransport: start state=%i", q.state)

	opts = append(opts, grpc.WithInsecure())
	if q.keepAlive {
		// See https://github.com/grpc/grpc-go/tree/master/examples/features/keepalive
		var kacp = keepalive.ClientParameters{
			Time:                10 * time.Second, // send pings every 10 seconds if there is no activity
			Timeout:             3 * time.Second,  // wait 3 second for ping ack before considering the connection dead
			PermitWithoutStream: true,             // send pings even without active streams
		}
		opts = append(opts, grpc.WithKeepaliveParams(kacp))

	}
	conn, err := grpc.Dial(q.serverAddr, opts...)
	if err != nil {
		q.log.Errorf(context.Background(), err, "Queue::createTransport : Failed to create connection state=%i", q.state)
		return err
	}

	q.conn = conn
	q.qapi = qapi.NewQueueApiClient(conn)
	return nil
}

func toTimestamp(in time.Time) *timestamp.Timestamp {
	return &timestamp.Timestamp{
		Seconds: in.Unix(),
		Nanos:   0,
	}
}
