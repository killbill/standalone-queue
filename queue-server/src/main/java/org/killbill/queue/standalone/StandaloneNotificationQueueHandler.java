/*
 * Copyright 2014-2019 Groupon, Inc
 * Copyright 2014-2019 The Billing Project, LLC
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

package org.killbill.queue.standalone;

import org.joda.time.DateTime;
import org.killbill.notificationq.api.NotificationEvent;
import org.killbill.notificationq.api.NotificationQueueService.NotificationQueueHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class StandaloneNotificationQueueHandler implements NotificationQueueHandler {

    private final Logger logger = LoggerFactory.getLogger(StandaloneNotificationQueueHandler.class);


    private final AtomicLong counter;

    private final long sleepTimeMsec = 10;

    public StandaloneNotificationQueueHandler() {
        this.counter = new AtomicLong();
        logger.info("NotificationHandler configured to sleep {} mSec", sleepTimeMsec);
    }


    @Override
    public void handleReadyNotification(final NotificationEvent event, final DateTime eventDateTime, final UUID userToken, final Long searchKey1, final Long searchKey2) {
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
