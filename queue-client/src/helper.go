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
	"fmt"
	"log"
	"math/rand"
	"os"
	"sync"
	"time"
	"golang.org/x/time/rate"
)

//
//  Rate limiting function
//
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
	return ls.Rate*float64(idx) + ls.Init
}

type GradLimiter struct {
	// Warmup time. e.g  "30s"
	WarmupSec time.Duration
	// TargetRate (events per seconds)
	TargetRate float64
	// Only implemented Linear
	Strategy Strategy

	// Internal ticker every second
	ticker *time.Ticker
	// Rate limiting function
	limiter *rate.Limiter
	// Current rate
	curRate float64
	// Incremented on each tick
	wIdx int
	// Internal mutex
	mu sync.Mutex
}

func (gl *GradLimiter) Wait(ctx context.Context) (err error) {
	gl.mu.Lock()
	curLimiter := gl.limiter
	gl.mu.Unlock()
	return curLimiter.Wait(ctx)
}

func (gl *GradLimiter) Stop() {
	gl.ticker.Stop()
}

func parseDuration(duration string) time.Duration {
	w, err := time.ParseDuration(duration)
	if err != nil {
		log.Fatalf("Failed to parse duration sequence: err = %s", err)
	}
	return w
}

func NewGradLimiter(warmup string, targetRate float64, unused WarmupStrategy) *GradLimiter {

	w := parseDuration(warmup)
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

			gl.wIdx += 1

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



//
//  Random strings for test
//
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