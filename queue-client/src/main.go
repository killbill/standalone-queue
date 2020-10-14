package main

import (
	"context"
	"flag"
	"fmt"
	"log"
	"math/rand"
	"os"
	"sync"
	"time"

	qapi "github.com/killbill/standalone-queue/gen-go/api"
	"golang.org/x/time/rate"
	"google.golang.org/grpc"
	"google.golang.org/grpc/keepalive"

	"github.com/killbill/standalone-queue/src/queue"
)

const Granularity = 1 * time.Second
const MinWarmup = 10 * Granularity

const DefaultBurst = 5

type WarmupStrategy int

const (
	Linear WarmupStrategy = 1 + iota
	Square
)

type Strategy interface {
	GetNextRate(idx int) float64
}

type LinearStrategy struct {
	Rate float64
	Init float64
}

func (ls LinearStrategy) GetNextRate(idx int) float64 {
	return ls.Rate * float64(idx) + ls.Init
}


type GradLimiter struct {
	// Warmup time. e.g  "30s"
	WarmupSec  time.Duration
	// TargetRate (events per seconds)
	TargetRate float64
	// Only implemented Linear
	Strategy   Strategy

	// Internal ticker every second
	ticker *time.Ticker
	// Rate limiting function
	limiter *rate.Limiter
	// Current rate
	curRate float64
	// Incremented on each tick
	wIdx    int
	// Internal mutex
	mu      sync.Mutex
}


func (gl *GradLimiter) Wait(ctx context.Context) (err error) {
	gl.mu.Lock()
	curLimiter := gl.limiter
	gl.mu.Unlock()
	return curLimiter.Wait(ctx)
}

func NewGradLimiter(warmup string, targetRate float64, unused WarmupStrategy) *GradLimiter {



	w, err := time.ParseDuration(warmup)
	if err != nil {
		log.Fatalf("Failed to parse warmup sequence: err = %s", err)
	}

	if w < MinWarmup {
		log.Fatalf("Minimum warmup allowed is %s", MinWarmup.String())
	}

	wSec := w / time.Second

	t := time.NewTicker(1 * time.Second)

	strategy := LinearStrategy{targetRate / float64(wSec), 1}
	curRate := strategy.GetNextRate(0)

	gl := &GradLimiter{
		WarmupSec:  wSec,
		TargetRate: targetRate,
		Strategy:   strategy,
		ticker:     t,
		curRate:    curRate,
		limiter:    rate.NewLimiter(rate.Limit(curRate), DefaultBurst),
		wIdx:       0,
	}

	go func() {

		for _ = range t.C {

			gl.mu.Lock()

			gl.wIdx +=1

			tmp := gl.Strategy.GetNextRate(gl.wIdx)
			if tmp > gl.TargetRate {
				gl.curRate = gl.TargetRate
			} else {
				gl.curRate = tmp
			}

			gl.limiter = rate.NewLimiter(rate.Limit(gl.curRate), DefaultBurst)
			gl.mu.Unlock()

			if gl.curRate == gl.TargetRate {
				fmt.Fprintf(os.Stderr, "Warmup completed rate = %e\n", gl.curRate)
				t.Stop()
			} else {
				fmt.Fprintf(os.Stderr, "Warmup current rate = %e\n", gl.curRate)
			}
		}
	}()

	return gl

}


func createConnection(serverAddr string) *grpc.ClientConn {

	var opts []grpc.DialOption
	var kacp = keepalive.ClientParameters{
		Time:                10 * time.Second, // send pings every 10 seconds if there is no activity
		Timeout:             time.Second,      // wait 1 second for ping ack before considering the connection dead
		PermitWithoutStream: true,             // send pings even without active streams
	}
	opts = append(opts, grpc.WithInsecure(), grpc.WithKeepaliveParams(kacp))
	conn, err := grpc.Dial(serverAddr, opts...)
	if err != nil {
		log.Fatalf("fail to create connection: %v", err)
	}
	return conn
}

func doTest(warmup string, targetRate float64, nbEvents int, queue queue.Queue)  {


	fmt.Printf("[doTest] Starting test nbEvents=%d\n", nbEvents)


	limiter := NewGradLimiter(warmup, targetRate, Linear)

	isStopping := false

	// Rate limiting
	bctx := context.Background()


	evtChan := make(chan *qapi.EventMsg, 1000)
	queue.SubscribeEvents(bctx, evtChan)

	doneCh := make(chan interface{})
	go func(evtCh <-chan *qapi.EventMsg, doneCh chan<- interface{}) {
		// TODO
		curRvc := 0
		for evt := range evtCh {
			curRvc += 1

			if curRvc % 50 == 0 {
				fmt.Printf("[doTest] Rcv curRvc=%d\n", curRvc)
				fmt.Printf("[doTest] Got event... %s\n", evt.EventJson)
			}

			if nbEvents > 0 && curRvc >= nbEvents {
				fmt.Printf("[doTest] Rcv all events, curRvc=%d\n", curRvc)
				break
			}
		}
		fmt.Printf("[doTest] Rcv %d events...\n", curRvc)
		doneCh <- struct{}{}
	}(evtChan, doneCh)

	curSent := 0
	for !isStopping {

		limiter.Wait(bctx)
		queue.PostEvent(bctx,"{\"foo\":\"something\",\"bar\":\"fab44c43-7a92-41f8-8adf-9234ba7b5b8f\",\"date\":\"2020-10-13T02:30:45.966Z\",\"isActive\":true}")
		curSent += 1

		if curSent % 50 == 0 {
			fmt.Printf("[doTest] Sent curSent=%d\n", curSent)
		}

		if nbEvents > 0 && curSent >= nbEvents {
			fmt.Printf("[doTest] Sent all events, curSent=%d\n", curSent)
			break
		}
	}

	<- doneCh

	fmt.Printf("[doTest] Exiting...\n")
	queue.Close(bctx)

}


func init() {
	rand.Seed(time.Now().UnixNano())
}

var letterRunes = []rune("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ")

func RandStringRunes(n int) string {
	b := make([]rune, n)
	for i := range b {
		b[i] = letterRunes[rand.Intn(len(letterRunes))]
	}
	return string(b)
}
func main() {

	serverAddr := flag.String("serverAddr", "127.0.0.1:9999", "Address of the server")
	rateEvents := flag.Float64("rateEvents", 100.0, "Nb events/sec")
	warmupSeq := flag.String("warmup", "10s", "Time period for the warmup. e.g 30s")
	nbEvents := flag.Int("nbEvents", 100000, "Nb events or -1 for infinite")



	flag.Parse()

	s := fmt.Sprintf("Starting test: server=%s, rateEvents=%f, warmup=%s, nbEvents=%d\n", *serverAddr, *rateEvents, *warmupSeq, *nbEvents)
	s += fmt.Sprintf("\n")
	fmt.Printf(s)

	var opts []grpc.DialOption
	opts = append(opts, grpc.WithInsecure())
	testConn := createConnection(*serverAddr)
	defer testConn.Close()


	owner := RandStringRunes(13)
	searchKey1 := 1
	searchKey2 := 2
	api := queue.NewQueue(owner, int64(searchKey1), int64(searchKey2), testConn)

	doTest(*warmupSeq, *rateEvents, *nbEvents, api)

	fmt.Printf("main Exiting...\n")

}