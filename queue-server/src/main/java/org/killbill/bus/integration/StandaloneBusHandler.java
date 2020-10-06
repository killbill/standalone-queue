/*
 * Copyright 2014-2018 Groupon, Inc
 * Copyright 2014-2018 The Billing Project, LLC
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

package org.killbill.bus.integration;

import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;


public class StandaloneBusHandler {

    private final Logger logger = LoggerFactory.getLogger(StandaloneBusHandler.class);


    private final AtomicLong counter;

    private final long sleepTimeMsec = 10;

    public StandaloneBusHandler() {
        this.counter = new AtomicLong();

        logger.info("BusHandler configured to sleep {} mSec", sleepTimeMsec);
    }

    @AllowConcurrentEvents
    @Subscribe
    public void dispatchEvent(final StandaloneBusEvent event) {
        long dispatched = counter.incrementAndGet();

        sleepIfRequired();

    }


    private void sleepIfRequired() {
        if (sleepTimeMsec > 0) {
            try {
                Thread.sleep(sleepTimeMsec);
            } catch (final Exception e) {

            }
        }
    }
}
