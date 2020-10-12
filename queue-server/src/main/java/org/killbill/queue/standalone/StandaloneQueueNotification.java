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

import com.fasterxml.jackson.core.JsonProcessingException;
import org.killbill.billing.queue.rpc.gen.EventMsg;
import org.killbill.bus.api.PersistentBus.EventBusException;
import org.killbill.notificationq.DefaultNotificationQueueService;
import org.killbill.notificationq.api.NotificationQueue;
import org.killbill.notificationq.api.NotificationQueueConfig;
import org.killbill.notificationq.api.NotificationQueueService;
import org.killbill.notificationq.api.NotificationQueueService.NotificationQueueAlreadyExists;
import org.killbill.notificationq.dao.NotificationEventModelDao;
import org.killbill.notificationq.dao.NotificationSqlDao;
import org.killbill.queue.QueueObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class StandaloneQueueNotification extends StandaloneQueueBase implements StandaloneQueue {

    private static final Logger logger = LoggerFactory.getLogger(StandaloneQueueNotification.class);

    // TODO config
    private static final String SVC_NAME = "embs-svc";

    private final StandaloneNotificationQueueHandler notificationQueueHandler;
    private final NotificationQueueService notificationQueueService;
    private final NotificationQueue notificationQueue;


    public StandaloneQueueNotification(final String jdbcConnection,
                                       final String dbUsername,
                                       final String dbPassword,
                                       final NotificationQueueConfig config) throws NotificationQueueAlreadyExists {
        super(config, jdbcConnection, dbUsername, dbPassword);
        this.notificationQueueHandler = new StandaloneNotificationQueueHandler();
        this.notificationQueueService = new DefaultNotificationQueueService(dbi, clock, (NotificationQueueConfig) queueConfig, metricRegistry);
        this.notificationQueue = notificationQueueService.createNotificationQueue(SVC_NAME,
                QUEUE_NAME,
                notificationQueueHandler);

    }

    @Override
    public void start() {
        if (notificationQueue != null) {
            notificationQueue.startQueue();
        }
        logger.info("Started test instance {}", QUEUE_NAME);
    }

    @Override
    public void stop() throws Exception {
        logger.info("Stopping test instance {}", QUEUE_NAME);
        if (notificationQueue != null) {
            notificationQueue.stopQueue();
            notificationQueueService.deleteNotificationQueue(SVC_NAME, QUEUE_NAME);
        }
        super.stop();
    }

    @Override
    public void insertEntryIntoQueue(final EventMsg request) throws EventBusException {
        final NotificationSqlDao dao = dbi.onDemand(NotificationSqlDao.class);

        final StandaloneNotificationEvent entry = new StandaloneNotificationEvent(request.getEventJson());
        final String json;
        try {
            json = QueueObjectMapper.get().writeValueAsString(entry);
            final UUID userToken = UUID.fromString(request.getUserToken());
            final NotificationEventModelDao model = new NotificationEventModelDao(request.getCreatingOwner(), clock.getUTCNow(), StandaloneNotificationEvent.class.getName(), json,
                    userToken, request.getSearchKey1(), request.getSearchKey2(), userToken, clock.getUTCNow(), QUEUE_NAME);

            dao.insertEntry(model, queueConfig.getTableName());
        } catch (final JsonProcessingException e) {
            throw new EventBusException("Unable to serialize event " + entry);
        }
    }

}

