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
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.killbill.billing.queue.rpc.gen.EventMsg;
import org.killbill.notificationq.DefaultNotificationQueueService;
import org.killbill.notificationq.api.NotificationQueue;
import org.killbill.notificationq.api.NotificationQueueConfig;
import org.killbill.notificationq.api.NotificationQueueService;
import org.killbill.notificationq.api.NotificationQueueService.NotificationQueueAlreadyExists;
import org.killbill.queue.retry.RetryableHandler;
import org.killbill.queue.retry.RetryableService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

import static io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall;

public class StandaloneQueueNotification extends StandaloneQueueBase implements StandaloneQueue {

    private static final Logger logger = LoggerFactory.getLogger(StandaloneQueueNotification.class);

    private static final String SVC_NAME = "embs-svc";
    private final StandaloneNotificationQueueHandler notificationQueueHandler;
    private final NotificationQueueService notificationQueueService;
    private final RetryableNotificationQueueService retryableQueueService;
    private final RetryableHandler retryableHandler;
    private final NotificationQueue notificationQueue;

    private static final class RetryableNotificationQueueService extends RetryableService {

        public RetryableNotificationQueueService(final NotificationQueueService notificationQueueService) {
            super(notificationQueueService);
        }
    }

    public StandaloneQueueNotification(final String jdbcConnection,
                                       final String dbUsername,
                                       final String dbPassword,
                                       final NotificationQueueConfig config) throws NotificationQueueAlreadyExists {
        super(config, jdbcConnection, dbUsername, dbPassword);

        this.notificationQueueHandler = new StandaloneNotificationQueueHandler();
        this.notificationQueueService = new DefaultNotificationQueueService(dbi, clock, (NotificationQueueConfig) queueConfig, metricRegistry);
        this.retryableQueueService = new RetryableNotificationQueueService(notificationQueueService);

        this.retryableHandler = new RetryableHandler(clock, retryableQueueService, notificationQueueHandler);
        this.notificationQueue = notificationQueueService.createNotificationQueue(SVC_NAME,
                QUEUE_NAME,
                retryableHandler);

        retryableQueueService.initialize(notificationQueue, notificationQueueHandler);
    }

    @Override
    public void start() {
        if (notificationQueue != null) {
            retryableQueueService.start();
            notificationQueue.initQueue();
            notificationQueue.startQueue();
        }
        logger.info("Started test instance {}", QUEUE_NAME);
    }

    @Override
    public void stop() throws Exception {
        logger.info("Stopping test instance {}", QUEUE_NAME);
        if (notificationQueue != null) {
            retryableQueueService.stop();
            notificationQueue.stopQueue();
            notificationQueueService.deleteNotificationQueue(SVC_NAME, QUEUE_NAME);
        }
        super.stop();
    }

    @Override
    public void insertEntryIntoQueue(final EventMsg request) throws Exception {
        final StandaloneNotificationEvent entry = new StandaloneNotificationEvent(request.getEventJson(), request.getClientId());
        final UUID userToken = UUID.fromString(request.getUserToken());
        final DateTime effectiveDate = new DateTime(request.getEffectiveDate().getSeconds() * 1000L, DateTimeZone.UTC);
        notificationQueue.recordFutureNotification(effectiveDate, entry, userToken, request.getSearchKey1(), request.getSearchKey2());
    }

    @Override
    public void ackEvent(final String userToken, final boolean success) {
        notificationQueueHandler.notifyEventCompletion(userToken, success);
    }


    @Override
    public void registerResponseObserver(final String clientId, ServerCallStreamObserver<EventMsg> responseObserver) {
        notificationQueueHandler.registerResponseObserver(clientId, responseObserver);
    }

    @Override
    public void unregisterResponseObserver(final String clientId) {
        notificationQueueHandler.unregisterResponseObserver(clientId);
    }

}

