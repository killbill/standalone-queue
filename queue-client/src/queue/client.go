package queue

import (
	"context"
	"io"
	"sync"
	"time"

	"github.com/golang/protobuf/ptypes/timestamp"
	"github.com/google/uuid"
	"github.com/sirupsen/logrus"
	"google.golang.org/grpc"
	"google.golang.org/grpc/keepalive"
	"google.golang.org/grpc/status"
)
import qapi "github.com/killbill/standalone-queue/gen-go/api"

var _ Queue = &queue{}

type Queue interface {
	PostEvent(ctx context.Context, json string) error
	SubscribeEvents(ctx context.Context,  evtCh chan<- *qapi.EventMsg) error
	Close(ctx context.Context)
}

func NewQueue(serverAddr string, owner string, searchKey1 int64, searchKey2 int64, logger *logrus.Logger) (Queue, error) {

	queue := &queue{
		serverAddr: serverAddr,
		owner:      owner,
		searchKey1: searchKey1,
		searchKey2: searchKey2,
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
	owner string
	searchKey1 int64
	searchKey2 int64
	// Client app specifies its configured logger
	log *logrus.Logger
	// Connection/api management
	mux sync.Mutex
	conn *grpc.ClientConn
	qapi qapi.QueueApiClient
}

func (q *queue) PostEvent(ctx context.Context, json string) error {
	_, err := q.qapi.PostEvent(ctx, &qapi.EventMsg{
		QueueName:       "",
		Owner:           q.owner,
		EventJson:       json,
		UserToken:       uuid.New().String(),
		FutureUserToken: "",
		EffectiveDate:   toTimestamp(time.Now().UTC()),
		SearchKey1:      q.searchKey1,
		SearchKey2:      q.searchKey2,
	})
	if err != nil {
		q.log.WithFields(logrus.Fields{"err": err}).Error("Failed to post event")
		return err
	}
	return nil
}

func (q *queue) SubscribeEvents(ctx context.Context, evtCh chan<- *qapi.EventMsg) error {
	stream, err := q.qapi.SubscribeEvents(ctx, &qapi.SubscriptionRequest{
		Owner: q.owner,
		SearchKey2: q.searchKey2,
	})
	if (err != nil) {
		q.log.WithFields(logrus.Fields{"err": err}).Error("Failed to subscribe to events")
		return err
	}


	go func(evtCh chan<- *qapi.EventMsg) {
		for {
			evt, err := stream.Recv()
			if err == io.EOF {
				q.log.WithFields(logrus.Fields{}).Info("SubscribeEvents : EOF stream")
				close(evtCh)
				break
			}
			if err != nil {
				serr, ok := status.FromError(err)
				if (!ok) {
					q.log.WithFields(logrus.Fields{"err": err}).Error("SubscribeEvents failed to receive event stream")
				} else {
					status := serr.Code().String()
					q.log.WithFields(logrus.Fields{"err": err, "status": status}).Error("SubscribeEvents failed to receive event stream")
				}
				close(evtCh)
				break
			}
			evtCh <- evt
		}
		q.log.WithFields(logrus.Fields{}).Info("Queue::SubscribeEvents routine returned")
	}(evtCh)
	return nil
}

func (q *queue) Close(ctx context.Context,) {
	// Close our side of the connection(s)
	defer func() {
		q.log.WithFields(logrus.Fields{}).Info("Queue::Close : Closing client connection")
		err := q.conn.Close()
		if err != nil {
			q.log.WithFields(logrus.Fields{"err": err}).Error("Queue::Close : failed to close client connection")
		}
	}()

	q.log.WithFields(logrus.Fields{}).Info("Queue::Close : Closing server connection")
	_, err := q.qapi.Close(ctx, &qapi.CloseRequest{
		Owner: q.owner,
	})
	q.log.WithFields(logrus.Fields{}).Info("Queue::Close : Closing server connection DONE")
	if err != nil {
		q.log.WithFields(logrus.Fields{"err": err}).Error("Failed to close event stream")
	}
}

func (q *queue) createConnection() error {

	q.mux.Lock()
	defer q.mux.Unlock()

	q.log.WithFields(logrus.Fields{}).Info("Queue::createConnection ")

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


