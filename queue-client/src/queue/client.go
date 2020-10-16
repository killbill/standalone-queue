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

const DefaultTimeout = time.Second * 5

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

func NewQueue(serverAddr string, clientId string, searchKey1 int64, searchKey2 int64, logger *logrus.Logger) (Queue, error) {

	queue := &queue{
		serverAddr: serverAddr,
		clientId:   clientId,
		searchKey1: searchKey1,
		searchKey2: searchKey2,
		state:      Closed,
		log:        logger,
	}

	err := queue.createConnection()
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

func (q *queue) SubscribeEvents(ctx context.Context, evtCh chan<- *qapi.EventMsg) error {

	q.mux.Lock()
	defer func() {
		q.mux.Unlock()
	}()

	if q.state == Connected {
		q.log.WithFields(logrus.Fields{}).Info("Queue::SubscribeEvents: already connected, ignore")
		return nil
	}

	err := q.subscribeEventsWithLock(ctx, evtCh)
	return err
}

func (q *queue) subscribeEventsWithLock(ctx context.Context, evtCh chan<- *qapi.EventMsg) error {

	if q.state == Connected {
		q.log.WithFields(logrus.Fields{}).Info("Queue::SubscribeEvents: already connected, ignore")
		return nil
	}

	stream, err := q.qapi.SubscribeEvents(ctx, &qapi.SubscriptionRequest{
		ClientId:   q.clientId,
		SearchKey2: q.searchKey2,
	})
	if err != nil {
		// We don't try to automatically re-subscribe, we let the client decide and handle retries if necessary
		q.state = Closed
		q.log.WithFields(logrus.Fields{"err": err}).Error("Queue::SubscribeEvents: failed to subscribe to events")
		return err
	}

	go func(stream qapi.QueueApi_SubscribeEventsClient, evtCh chan<- *qapi.EventMsg) {
		q.log.WithFields(logrus.Fields{}).Info("Queue::SubscribeEvents (gor): started")
		for {
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
				q.log.WithFields(logrus.Fields{}).Info("Queue::SubscribeEvents (gor): EOF stream upon Close")
				return
			}

			// Either an unexpected EOF or an error
			q.state = Disconnected
			if err == io.EOF {
				q.log.WithFields(logrus.Fields{}).Info("Queue::SubscribeEvents (gor): unexpected EOF stream")
			} else if err != nil {
				serr, ok := status.FromError(err)
				if !ok {
					q.log.WithFields(logrus.Fields{"err": err}).Error("Queue::SubscribeEvents (gor): failed to receive event stream")
				} else {
					status := serr.Code().String()
					q.log.WithFields(logrus.Fields{"err": err, "status": status}).Error("Queue::SubscribeEvents (gor): failed to receive event stream")
				}
			}
			// Exit for loop with lock
			break
		}
		// Re-enter the routine with lock to restart the SubscribeEvents process
		ctx2, canc2 := context.WithTimeout(context.Background(), DefaultTimeout)
		defer canc2()
		q.log.WithFields(logrus.Fields{}).Info("Queue::SubscribeEvents (gor): exit and re-subscribe")
		q.subscribeEventsWithLock(ctx2, evtCh)
		return
	}(stream, evtCh)
	return nil
}

func (q *queue) Close(ctx context.Context) {

	q.mux.Lock()
	defer func() {
		// Final state
		q.state = Closed
		// Close our side of the connection(s)
		q.log.WithFields(logrus.Fields{}).Info("Queue::Close: closing client transport")
		err := q.conn.Close()
		if err != nil {
			q.log.WithFields(logrus.Fields{"err": err}).Error("Queue::Close: failed to close client transport")
		}
		q.mux.Unlock()
	}()

	if q.state == Closed {
		q.log.WithFields(logrus.Fields{}).Info("Queue::Close: not connected ignore")
		return
	}

	// Attempt the Close call regardless of state
	q.log.WithFields(logrus.Fields{}).Info("Queue::Close: closing server connection")
	_, err := q.qapi.Close(ctx, &qapi.CloseRequest{
		ClientId: q.clientId,
	})
	if err != nil {
		// If we have an error and we were in Connected mode, log the error, otherwise ignore
		if q.state == Connected {
			q.log.WithFields(logrus.Fields{"err": err}).Error("Queue::Close: failed to close event stream")
		}
	}
}

func (q *queue) createConnection() error {

	q.mux.Lock()
	defer q.mux.Unlock()

	q.log.WithFields(logrus.Fields{}).Info("Queue::createConnection: start")

	var opts []grpc.DialOption
	var kacp = keepalive.ClientParameters{
		Time:                10 * time.Second, // send pings every 10 seconds if there is no activity
		Timeout:             3 * time.Second,  // wait 3 second for ping ack before considering the connection dead
		PermitWithoutStream: true,             // send pings even without active streams
	}

	opts = append(opts, grpc.WithInsecure(), grpc.WithKeepaliveParams(kacp))
	conn, err := grpc.Dial(q.serverAddr, opts...)
	if err != nil {
		q.log.WithFields(logrus.Fields{"err": err}).Error("Queue::createConnection : Failed to create connection")
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
