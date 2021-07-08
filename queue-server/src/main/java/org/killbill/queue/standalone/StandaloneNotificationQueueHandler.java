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

package org.killbill.queue.standalone;

import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.killbill.billing.queue.rpc.gen.EventMsg;
import org.killbill.billing.util.queue.QueueRetryException;
import org.killbill.notificationq.api.NotificationEvent;
import org.killbill.notificationq.api.NotificationQueueService.NotificationQueueHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class StandaloneNotificationQueueHandler implements NotificationQueueHandler {

    private static final Logger logger = LoggerFactory.getLogger(StandaloneNotificationQueueHandler.class);
    private final long ackTimeSec;

    private static final List<Period> RETRY_SCHEDULE = Arrays.asList(
            Period.minutes(1),
            Period.minutes(5),
            Period.minutes(15),
            Period.hours(1),
            Period.hours(6),
            Period.hours(24),
            Period.hours(48));

    private final Lock lock;
    private final Map<String, ServerCallStreamObserver<EventMsg>> observers;
    private final Map<String, CompletionSuccess> dispatchedEvents;

    public StandaloneNotificationQueueHandler(long ackTimeSec) {
        logger.info("StandaloneNotificationQueueHandler configured with ackTime value = {} sec", ackTimeSec);
        this.lock = new ReentrantLock();
        this.observers = new HashMap<>();
        this.dispatchedEvents = new HashMap();
        this.ackTimeSec = ackTimeSec;
    }

    public void notifyEventCompletion(final String userToken, final boolean success) {
        try {
            lock.lock();
            final CompletionSuccess complSuccess = dispatchedEvents.remove(userToken);
            if (complSuccess == null) {
                logger.warn("NotifyEventCompletion: Failed to find event thread waiting for userToken={}", userToken);
                return;
            }
            complSuccess.notify(success);
        } finally {
            lock.unlock();
        }
    }


    public void registerResponseObserver(final String owner, final io.grpc.stub.ServerCallStreamObserver<org.killbill.billing.queue.rpc.gen.EventMsg> responseObserver) {

        logger.info("registerResponseObserver: owner={}", owner);

        // Register OnCancelHandler to make sure we can cleanup upon cancelation
        responseObserver.setOnCancelHandler(() -> {
            try {
                logger.info("OnCancelHandler stream owner={}", owner);
                lock.lock();
                final StreamObserver<EventMsg> prev = observers.remove(owner);
                if (prev != null) {
                    streamComplete(owner, prev);
                }
            } finally {
                lock.unlock();
            }
        });

        try {
            lock.lock();
            final StreamObserver<EventMsg> prev = observers.remove(owner);
            if (prev != null) {
                streamComplete(owner, prev);
            }

            logger.info("Adding observer stream owner={}", owner);
            observers.put(owner, responseObserver);
        } finally {
            lock.unlock();
        }
    }

    public void unregisterResponseObserver(final String owner) {
        logger.info("unregisterResponseObserver: owner={}", owner);

        try {
            lock.lock();
            final StreamObserver<EventMsg> prev = observers.remove(owner);
            if (prev != null) {
                try {
                    streamComplete(owner, prev);
                } catch (final io.grpc.StatusRuntimeException e) {
                    logger.info("Ignoring exception owner={}, e={}", owner, e);

                }
            }
        } finally {
            lock.unlock();
        }
    }

    private void streamComplete(final String owner, final StreamObserver<EventMsg> stream) {
        logger.info("streamComplete: Closing stream owner={}", owner);
        try {
            lock.lock();
            if (stream != null) {
                try {
                    stream.onCompleted();
                } catch (final Exception e) {
                    logger.error("streamComplete: onCompleted exception = {} ", e);
                }
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void handleReadyNotification(final NotificationEvent inputEvent, final DateTime eventDateTime, final UUID userToken, final Long searchKey1, final Long searchKey2) {

        if (!(inputEvent instanceof StandaloneNotificationEvent)) {
            logger.error("Unexpected type of event class={}, event={}",
                    (inputEvent != null ? inputEvent.getClass() : null), inputEvent);
            // TODO We could retry but if we cannot deserialize, this will not help...
            return;
        }

        final StandaloneNotificationEvent inputEvent2 = (StandaloneNotificationEvent) inputEvent;
        final String userTokenStr = userToken.toString();
        final EventMsg.Builder msgBuilder = EventMsg.newBuilder();
        msgBuilder.setClientId(inputEvent2.getClientId());
        msgBuilder.setEventJson(inputEvent2.getEnvelope());
        msgBuilder.setUserToken(userTokenStr);
        msgBuilder.setSearchKey1(searchKey1);
        msgBuilder.setSearchKey2(searchKey2);
        final EventMsg event = msgBuilder.build();

        // Note that StreamObserver are not thread safe:
        //  "Since individual {@code StreamObserver}s are not thread-safe, if multiple threads will be
        //    writing to a {@code StreamObserver} concurrently, the application must synchronize calls."
        boolean foundValidObs = false;
        CompletionSuccess complSuccess = null;
        try {
            lock.lock();
            for (final Map.Entry<String, io.grpc.stub.ServerCallStreamObserver<org.killbill.billing.queue.rpc.gen.EventMsg>> entry : observers.entrySet()) {
                final String owner = entry.getKey();
                final io.grpc.stub.ServerCallStreamObserver<org.killbill.billing.queue.rpc.gen.EventMsg> obs = entry.getValue();
                if (obs.isCancelled()) {
                    logger.info("Detected canceled observer, owner={} closing...", owner);
                    streamComplete(owner, obs);
                    continue;
                }
                foundValidObs = true;
                try {
                    complSuccess = new CompletionSuccess(userTokenStr, ackTimeSec);
                    dispatchedEvents.put(userTokenStr, complSuccess);
                    obs.onNext(event);
                    // Break after first success
                    break;
                } catch (final Exception e) {
                    dispatchedEvents.remove(userTokenStr);
                    complSuccess = null;
                    logger.error("Failed to write inputEventStr event={}, e={}", event, e);
                }
            }
        } finally {
            lock.unlock();
        }

        // We did not send the event
        if (complSuccess == null) {
            if (!foundValidObs) {
                logger.info("No registered valid observer, requeuing event={},", event);
            } else {
                logger.info("Failed to send event, requeuing event={},", event);
            }
            throw new QueueRetryException(RETRY_SCHEDULE);
        } else {
            // This waits and potentially reschedules event by throwing QueueRetryException
            complSuccess.waitForCompletion();
        }
    }

    private static class CompletionSuccess {

        private final String userToken;
        private final CountDownLatch latch;
        private final long ackTimeSec;
        private boolean status;

        public CompletionSuccess(final String userToken, final long ackTimeSec) {
            this.userToken = userToken;
            this.latch = new CountDownLatch(1);
            this.ackTimeSec = ackTimeSec;
        }

        public void notify(boolean status) {
            this.status = status;
            latch.countDown();
        }

        public void waitForCompletion() throws QueueRetryException {
            try {
                final boolean res = latch.await(ackTimeSec, TimeUnit.SECONDS);
                if (!res) {
                    logger.warn("Thread waiting for event userToken={} timed out after {} seconds", userToken, ackTimeSec);
                    throw new QueueRetryException(RETRY_SCHEDULE);
                } else if (!status) {
                    logger.info("Client Nack for userToken={}", userToken);
                    throw new QueueRetryException(RETRY_SCHEDULE);
                }
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Thread waiting for event userToken={} got interrupted,", userToken);
                throw new QueueRetryException(RETRY_SCHEDULE);
            }
        }
    }
}
