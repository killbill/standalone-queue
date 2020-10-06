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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableMap;
import org.killbill.billing.queue.rpc.gen.EventMsg;
import org.killbill.bus.DefaultPersistentBus;
import org.killbill.bus.api.PersistentBus;
import org.killbill.bus.api.PersistentBus.EventBusException;
import org.killbill.bus.api.PersistentBusConfig;
import org.killbill.bus.dao.BusEventModelDao;
import org.killbill.bus.dao.PersistentBusSqlDao;
import org.killbill.queue.QueueObjectMapper;
import org.skife.config.ConfigSource;
import org.skife.config.ConfigurationObjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class StandaloneQueueBus extends StandaloneQueueBase implements StandaloneQueue {

    private final Logger logger = LoggerFactory.getLogger(StandaloneQueueBus.class);

    private final StandaloneBusHandler busHandler;
    private final PersistentBus bus;

    public StandaloneQueueBus(final String jdbcConnection, final String dbUsername, final String dbPassword) {
        super(setupQueueConfig(), jdbcConnection, dbUsername, dbPassword);
        this.busHandler = new StandaloneBusHandler();
        this.bus = setupPersistentBus();
    }

    @Override
    public void start() throws EventBusException {
        if (bus != null) {
            bus.startQueue();
            bus.register(busHandler);
        }
        // TODO
        logger.info("Started test instance {}", "foo");
    }

    @Override
    public void stop() throws Exception {
        logger.info("Stopping test instance {}");
        if (bus != null) {
            bus.unregister(busHandler);
            bus.stopQueue();
        }
        super.stop();
    }

    @Override
    public void postEntry(final EventMsg request) throws EventBusException {
        if (bus != null) {
            bus.post(new StandaloneBusEvent(request));
        }
    }

    @Override
    public void insertEntryIntoQueue(final EventMsg request) throws EventBusException {
        final PersistentBusSqlDao dao = dbi.onDemand(PersistentBusSqlDao.class);
        final StandaloneBusEvent entry = new StandaloneBusEvent(request);

        final String json;
        try {
            json = QueueObjectMapper.get().writeValueAsString(entry);
            // We use the source info to override the creator name
            final BusEventModelDao model = new BusEventModelDao(request.getCreatingOwner(), clock.getUTCNow(), StandaloneBusEvent.class.getName(), json,
                    entry.getUserToken(), entry.getSearchKey1(), entry.getSearchKey2());

            dao.insertEntry(model, queueConfig.getTableName());
        } catch (final JsonProcessingException e) {
            throw new EventBusException("Unable to serialize event " + entry);
        }

    }

    private static PersistentBusConfig setupQueueConfig() {

        final Map<String, String> config = new HashMap<>();


        final ConfigSource configSource = new ConfigSource() {
            @Override
            public String getString(final String propertyName) {
                return config.get(propertyName);
            }
        };

        final PersistentBusConfig persistentBusConfig = new ConfigurationObjectFactory(configSource).buildWithReplacements(PersistentBusConfig.class,
                ImmutableMap.<String, String>of("instanceName", "main"));
        return persistentBusConfig;
    }

    public PersistentBus setupPersistentBus() {
        return new DefaultPersistentBus(dbi, clock, (PersistentBusConfig) queueConfig, metricRegistry, databaseTransactionNotificationApi);
    }

}
