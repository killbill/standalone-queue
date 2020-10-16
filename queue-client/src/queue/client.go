package queue

import (
	"context"
	"io"
	"sync"
	"time"

	"github.com/golang/protobuf/ptypes/timestamp"
	"github.com/google/uuid"
	qapi "github.com/killbill/standalone-queue/gen-go/api"
	"github.com/sirupsen/logrus"
	"google.golang.org/grpc"
	"google.golang.org/grpc/keepalive"
	"google.golang.org/grpc/status"
)

type State int

const (
	// Initial state prior we called SubscribeEvents or after we have called Close
	Closed State = iota
	// Successful connected state
	Connected
	// Disconnected
	// - Go routine receiving stream events got an error
	Disconnected
)

var _ Queue = &queue{}

type Queue interface {
	PostEvent(ctx context.Context, json string) error
	SubscribeEvents(ctx context.Context, evtCh chan<- *qapi.EventMsg) error
	Close(ctx context.Context)
}

func NewQueue(serverAddr string, clientId string, searchKey1 int64, searchKey2 int64, keepAlive bool, logger *logrus.Logger) (Queue, error) {

	if !keepAlive {
		logger.WithFields(logrus.Fields{}).Warn("Queue created with keepAlive=false")
	}

	queue := &queue{
		serverAddr: serverAddr,
		clientId:   clientId,
		searchKey1: searchKey1,
		searchKey2: searchKey2,
		keepAlive: keepAlive,
		state:      Closed,
		log:        logger,
	}

	err := queue.createTransport()
	if err != nil {
		return nil, err
	}
	return queue, nil
}

type queue struct {
	// Queue settings
	serverAddr string
	clientId   string
	searchKey1 int64
	searchKey2 int64
	// Client app specifies its configured logger
	log *logrus.Logger
	// This is should be set to true unless in test mode
	keepAlive bool
	// Connection/api management
	mux   sync.Mutex
	state State
	conn  *grpc.ClientConn
	qapi  qapi.QueueApiClient
}

func (q *queue) PostEvent(ctx context.Context, json string) error {
	_, err := q.qapi.PostEvent(ctx, &qapi.EventMsg{
		ClientId:        q.clientId,
		EventJson:       json,
		UserToken:       uuid.New().String(),
		FutureUserToken: "",
		EffectiveDate:   toTimestamp(time.Now().UTC()),
		SearchKey1:      q.searchKey1,
		SearchKey2:      q.searchKey2,
	})
	if err != nil {
		q.log.WithFields(logrus.Fields{"err": err}).Error("Queue::PostEvent: failed to post event")
		return err
	}
	return nil
}

func (q *queue) SubscribeEvents(_ context.Context, evtCh chan<- *qapi.EventMsg) error {

	q.mux.Lock()
	defer q.mux.Unlock()

	// If we are Connected or in the process or connecting, return
	if q.state != Closed {
		q.log.WithFields(logrus.Fields{"state": q.state}).Info("Queue::SubscribeEvents: already connected, ignore")
		return nil
	}

	const maxAttempts = 1
	stream, err := q.subscribeWithAttempts(maxAttempts)
	if err != nil {
		// We don't try to automatically re-subscribe, we let the client decide and handle retries if necessary
		return err
	}
	// Start gor to listen to events (and reconnect if/when necessary)
	go func() {
		q.listen(stream, evtCh)
	}()

	return nil
}

// This is called with Lock held
func (q *queue) subscribeWithAttempts(maxAttempts int) (qapi.QueueApi_SubscribeEventsClient, error) {

	q.log.WithFields(logrus.Fields{"state": q.state}).Info("Queue::subscribeWithAttempts: enter")

	var delaySec time.Duration = 1
	var stream qapi.QueueApi_SubscribeEventsClient
	var err error

	curAttempts := 0
	for stream == nil {
		// TODO do we need a context.Background() here?
		stream, err = q.qapi.SubscribeEvents(context.Background(), &qapi.SubscriptionRequest{
			ClientId:   q.clientId,
			SearchKey2: q.searchKey2,
		})
		if err == nil {
			q.state = Connected
			break
		}

		curAttempts++
		if curAttempts >= maxAttempts {
			q.state = Closed
			q.log.WithFields(logrus.Fields{"err": err, "state": q.state}).Errorf("Queue::subscribeWithAttempts: failed to subscribe to events attempts=[%d/%d]", curAttempts, maxAttempts)
			return nil, err
		}

		q.log.WithFields(logrus.Fields{"err": err, "state": q.state}).Errorf("Queue::subscribeWithAttempts: failed to re-subscribe to events, attempts=[%d/%d] sleeping %d sec and retry",
			curAttempts, maxAttempts, delaySec)
		time.Sleep(delaySec * time.Second)
		delaySec = delaySec * 2
	}
	return stream, nil
}

func (q *queue) listen(originalStream qapi.QueueApi_SubscribeEventsClient, evtCh chan<- *qapi.EventMsg) {

	// over a day of trying...
	const maxAttempts = 16
	stream := originalStream
	q.log.WithFields(logrus.Fields{"state": q.state}).Info("Queue::listen (gor): starting")

	for {
		// Receive and forwards events without lock until something happens...
		evt, err := stream.Recv()
		if err == nil && evt != nil {
			evtCh <- evt
			continue
		}

		// In all other case, EOF or error, we grab the lock and based on state/err decide whether we
		// want to resubscribe or exit
		q.mux.Lock()

		// Normal termination after Closed was called
		if err == io.EOF && q.state == Closed {
			defer func() {
				// Close client event channel
				close(evtCh)
				q.mux.Unlock()
			}()
			q.log.WithFields(logrus.Fields{"state": q.state}).Info("Queue::listen (gor): EOF stream upon Close, exit...")
			return
		}

		// Either an unexpected EOF or an error
		q.state = Disconnected
		stream = nil

		if err == io.EOF {
			q.log.WithFields(logrus.Fields{"state": q.state}).Info("Queue::listen (gor): unexpected EOF stream")
		} else if err != nil {
			serr, ok := status.FromError(err)
			if !ok {
				q.log.WithFields(logrus.Fields{"err": err, "state": q.state}).Error("Queue::listen (gor): failed to receive event stream")
			} else {
				status := serr.Code().String()
				q.log.WithFields(logrus.Fields{"err": err, "errStatus": status, "state": q.state}).Error("Queue::listen (gor): failed to receive event stream")
			}
		}

		if stream == nil {
			stream, err = q.subscribeWithAttempts(maxAttempts)
			if err != nil {
				q.log.WithFields(logrus.Fields{"state": q.state}).Errorf("Queue::listen (gor): fatal failure to re-connect after %d attempts, exiting :-(")
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
		q.log.WithFields(logrus.Fields{"state": q.state}).Info("Queue::Close: closing client transport")
		err := q.conn.Close()
		if err != nil {
			q.log.WithFields(logrus.Fields{"err": err, "state": q.state}).Error("Queue::Close: failed to close client transport")
		}
		q.mux.Unlock()
	}()

	if q.state == Closed {
		q.log.WithFields(logrus.Fields{"state": q.state}).Info("Queue::Close: not connected ignore")
		return
	}

	// Attempt the Close call regardless of state
	q.log.WithFields(logrus.Fields{"state": q.state}).Info("Queue::Close: closing server connection")
	_, err := q.qapi.Close(ctx, &qapi.CloseRequest{
		ClientId: q.clientId,
	})
	if err != nil {
		// If we have an error and we were in Connected mode, log the error, otherwise ignore
		if q.state == Connected {
			q.log.WithFields(logrus.Fields{"err": err, "state": q.state}).Error("Queue::Close: failed to close event stream")
		}
	}
}

func (q *queue) createTransport() error {

	q.mux.Lock()
	defer q.mux.Unlock()

	q.log.WithFields(logrus.Fields{"state": q.state}).Info("Queue::createTransport: start")

	var opts []grpc.DialOption
	opts = append(opts, grpc.WithInsecure())
	if q.keepAlive {
		var kacp = keepalive.ClientParameters{
			Time:                10 * time.Second, // send pings every 10 seconds if there is no activity
			Timeout:             3 * time.Second,  // wait 3 second for ping ack before considering the connection dead
			PermitWithoutStream: true,             // send pings even without active streams
		}
		opts = append(opts, grpc.WithKeepaliveParams(kacp))

	}
	conn, err := grpc.Dial(q.serverAddr, opts...)
	if err != nil {
		q.log.WithFields(logrus.Fields{"err": err, "state": q.state}).Error("Queue::createTransport : Failed to create connection")
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
