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

package main

import (
	"context"
	"flag"
	"fmt"
	"os"
	"time"

	"github.com/google/uuid"
	qapi "github.com/killbill/standalone-queue/gen-go/api"
	"github.com/sirupsen/logrus"

	"github.com/killbill/standalone-queue/src/queue"
)

// Our app will want to pass its own (configured) logger
var logger = logrus.New()
var customFormatter = new(logrus.TextFormatter)

func doTest(warmup string, targetRate float64, sendEvts int, rcvEvts int, displayRate int, testLoops int , sleepLoops string, queue queue.Queue) {

	if rcvEvts == -1 {
		// Default expected recv events should match the one we send.
		rcvEvts = sendEvts
	}

	logger.Infof("[doTest] Starting test sendEvts=%d, rcvEvts=%d\n", sendEvts, rcvEvts)

	bctx := context.Background()

	// Send all events synchronously using the provided targetRate
	limiter := NewGradLimiter(warmup, targetRate, Linear)

	evtChan := make(chan *qapi.EventMsg, 1000)
	queue.SubscribeEvents(bctx, evtChan)

	defer func() {
		logger.Infof("[doTest] defer stopping limiter \n")
		limiter.Stop()
		logger.Infof("[doTest] defer closing queue \n")
		queue.Close(bctx)
		logger.Infof("[doTest] defer done \n")
	}()


	sleep := parseDuration(sleepLoops)
	curIteration := 0
	for curIteration < testLoops {

		doneCh := make(chan interface{})
		go func(evtCh <-chan *qapi.EventMsg, doneCh chan<- interface{}) {
			curRvc := 0
			for evt := range evtCh {
				curRvc += 1

				if curRvc%displayRate == 0 {
					logger.Infof("[doTest] Rcv curRvc=%d\n", curRvc)
					logger.Infof("[doTest] Got event... %s\n", evt.EventJson)
				}

				if rcvEvts >= 0 && curRvc >= rcvEvts {
					logger.Infof("[doTest] Rcv all events, curRvc=%d\n", curRvc)
					break
				}
			}
			logger.Infof("[doTest] Rcv %d events...\n", curRvc)
			doneCh <- struct{}{}
		}(evtChan, doneCh)

		curSent := 0
		for sendEvts == -1 /* send events forever */ ||
			curSent < sendEvts /* send events until we have reached sendEvts */ {
			limiter.Wait(bctx)
			uuid := uuid.New()
			queue.PostEvent(bctx, "{\"foo\":\"something\",\"bar\":\""+uuid.String()+"\",\"date\":\"2020-10-13T02:30:45.966Z\",\"isActive\":true}")
			curSent += 1

			if curSent%displayRate == 0 {
				logger.Infof("[doTest] Sent curSent=%d\n", curSent)
			}

			if sendEvts >= 0 && curSent >= sendEvts {
				logger.Infof("[doTest] Sent all events, curSent=%d\n", curSent)
				break
			}
		}

		// Wait for all events to be received or a non recoverable error
		<-doneCh

		curIteration++
		logger.Infof("[doTest] Done with iteration %d, sleep %s\n", curIteration, sleepLoops)
		if curIteration < testLoops {
			time.Sleep(sleep)
		}
	}

	logger.Infof("[doTest] Exiting...\n")
}


func main() {

	serverAddr := flag.String("serverAddr", "127.0.0.1:9999", "Address of the server")
	keepAlive := flag.Bool("keepAlive", true, "Set the ping keepAlive")
	rateEvents := flag.Float64("rateEvents", 100.0, "Nb events/sec")
	warmupSeq := flag.String("warmup", "10s", "Time period for the warmup. e.g 30s")
	sendEvts := flag.Int("sendEvts", 1000, "Nb events or -1 for infinite")
	rcvEvts := flag.Int("rcvEvts", -1, "Nb events or -1 for infinite")
	displayRate := flag.Int("displayRate", 100, "Print a trace for displayRate msg send or received")
	testLoops := flag.Int("testLoops", 1, "How many test iteration loops")
	sleepLoops := flag.String("sleepLoop", "1h", "How many test iteration loops")

	flag.Parse()
	s := fmt.Sprintf("Starting test: server=%s, rateEvents=%f, warmup=%s, sendEvts=%d, rcvEvts=%d, testLoops=%d, sleepLoops=%s\n",
		*serverAddr, *rateEvents, *warmupSeq, *sendEvts, *rcvEvts, *testLoops, *sleepLoops)
	s += fmt.Sprintf("\n")
	logger.Infof(s)

	// Log customization
	customFormatter.TimestampFormat = "2006-01-02 15:04:05"
	customFormatter.FullTimestamp = true
	logger.SetFormatter(customFormatter)

	clientId := RandStringRunes(13)
	searchKey1 := 1
	searchKey2 := 2
	api, err := queue.NewQueue(*serverAddr, clientId, int64(searchKey1), int64(searchKey2), *keepAlive, logger)
	if err != nil {
		logger.Errorf("[doTest] Failed to create connection, exiting err=%s...\n", err)
		os.Exit(1)
	}

	doTest(*warmupSeq, *rateEvents, *sendEvts, *rcvEvts, *displayRate, *testLoops, *sleepLoops, api)

	logger.Info("main Exiting...\n")
}
