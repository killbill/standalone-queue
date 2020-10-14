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

import com.google.common.collect.Maps;
import io.grpc.stub.StreamObserver;
import org.joda.time.DateTime;
import org.killbill.billing.queue.rpc.gen.EventMsg;
import org.killbill.notificationq.api.NotificationEvent;
import org.killbill.notificationq.api.NotificationQueueService.NotificationQueueHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;

public class StandaloneNotificationQueueHandler implements NotificationQueueHandler {

    private static final Logger logger = LoggerFactory.getLogger(StandaloneNotificationQueueHandler.class);

    private final Map<String, StreamObserver<EventMsg>> observers;


    public StandaloneNotificationQueueHandler() {
        this.observers = Maps.newConcurrentMap();
    }

    public void registerResponseObserver(final String owner, final io.grpc.stub.StreamObserver<org.killbill.billing.queue.rpc.gen.EventMsg> responseObserver) {
        final StreamObserver<EventMsg> prev = observers.remove(owner);
        if (prev != null) {
            closeStream(owner, prev);
        }
        logger.info("Adding observer stream owner={}", owner);
        observers.put(owner, responseObserver);
    }

    public void unregisterResponseObserver(final String owner) {
        final StreamObserver<EventMsg> prev = observers.remove(owner);
        if (prev != null) {
            try {
                closeStream(owner, prev);
            } catch (final io.grpc.StatusRuntimeException e) {
                logger.info("Ignoring exception owner={}, e={}", owner, e);

            }
        }
    }

    private void closeStream(final String owner, final StreamObserver<EventMsg> stream) {
        logger.info("Closing stream owner={}", owner);
        stream.onCompleted();
    }

    @Override
    public void handleReadyNotification(final NotificationEvent inputEvent, final DateTime eventDateTime, final UUID userToken, final Long searchKey1, final Long searchKey2) {

        synchronized (this) {
            final String event = deserializeEvent(inputEvent);
            final EventMsg.Builder msgBuilder = EventMsg.newBuilder();

            // TODO
            //msgBuilder.setQueueName(StandaloneQueueBase.QUEUE_NAME);
            msgBuilder.setEventJson(event);
            msgBuilder.setUserToken(userToken.toString());
            msgBuilder.setSearchKey1(searchKey1);
            msgBuilder.setSearchKey2(searchKey2);
            final EventMsg msg = msgBuilder.build();
            for (io.grpc.stub.StreamObserver<org.killbill.billing.queue.rpc.gen.EventMsg> obs : observers.values()) {
                obs.onNext(msg);
            }
        }
    }


    static String deserializeEvent(final NotificationEvent inputEvent) {
        if (!(inputEvent instanceof StandaloneNotificationEvent)) {
            logger.error("Unexpected type of event class={}, event={}",
                    (inputEvent != null ? inputEvent.getClass() : null), inputEvent);
            return null;
        }

        final StandaloneNotificationEvent event = (StandaloneNotificationEvent) inputEvent;
        return event.getEnvelope();
    }

}
