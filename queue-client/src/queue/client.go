package queue

import (
	"context"
	"io"
	"log"
	"time"

	"github.com/golang/protobuf/ptypes/timestamp"
	"github.com/google/uuid"
	"github.com/sirupsen/logrus"
	"google.golang.org/grpc"
)
import qapi "github.com/killbill/standalone-queue/gen-go/api"

var _ Queue = &queue{}

type Queue interface {
	PostEvent(ctx context.Context, json string)
	SubscribeEvents(ctx context.Context, evtCh chan<- *qapi.EventMsg)
}

func NewQueue(owner string, searchKey1 int64, searchKey2 int64, conn *grpc.ClientConn) Queue {
	return &queue{
		owner:      owner,
		searchKey1: searchKey1,
		searchKey2: searchKey2,
		conn:       conn,
		qapi: qapi.NewQueueApiClient(conn),
		log: logrus.New(), // TODO
	}
}

type queue struct {
	owner string
	searchKey1 int64
	searchKey2 int64
	conn *grpc.ClientConn
	qapi qapi.QueueApiClient
	log *logrus.Logger
}

func (q *queue) PostEvent(ctx context.Context, json string) {
	_, err := q.qapi.PostEvent(ctx, &qapi.EventMsg{
		QueueName:       "",
		CreatingOwner:   q.owner,
		EventJson:       json,
		UserToken:       uuid.New().String(),
		FutureUserToken: "",
		EffectiveDate:   toTimestamp(time.Now().UTC()),
		SearchKey1:      q.searchKey1,
		SearchKey2:      q.searchKey2,
	})
	if (err != nil) {
		q.log.WithFields(logrus.Fields{"err": err}).Error("Failed to post event")
		return
	}
}


func (q *queue) SubscribeEvents(ctx context.Context, evtCh chan<- *qapi.EventMsg) {
	stream, err := q.qapi.SubscribeEvents(ctx, &qapi.SubscriptionRequest{
		SearchKey2: q.searchKey2,
	})
	if (err != nil) {
		q.log.WithFields(logrus.Fields{"err": err}).Error("Failed to subscribe to events")
		return
	}
	for {
		evt, err := stream.Recv()
		if err == io.EOF {
			break
		}
		if err != nil {
			q.log.WithFields(logrus.Fields{"err": err}).Error("Failed to receive event stream")
		}
		evtCh <- evt
	}
}


func toTimestamp(in time.Time) *timestamp.Timestamp {
	return &timestamp.Timestamp{
		Seconds: in.Unix(),
		Nanos:   0,
	}
}
