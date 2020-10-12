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

import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Strings;
import com.zaxxer.hikari.HikariDataSource;
import org.killbill.clock.Clock;
import org.killbill.clock.DefaultClock;
import org.killbill.commons.jdbi.guice.DaoConfig;
import org.killbill.commons.jdbi.guice.DataSourceProvider;
import org.killbill.commons.jdbi.notification.DatabaseTransactionNotificationApi;
import org.killbill.commons.jdbi.transaction.NotificationTransactionHandler;
import org.killbill.queue.InTransaction;
import org.killbill.queue.api.PersistentQueueConfig;
import org.killbill.queue.standalone.rpc.QueueServer;
import org.skife.config.ConfigSource;
import org.skife.config.ConfigurationObjectFactory;
import org.skife.jdbi.v2.DBI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public abstract class StandaloneQueueBase implements StandaloneQueue {

    // TODO
    private static final String MAX_POOL_CONNECTIONS = "13";
    public final String QUEUE_NAME = "embs-queue";

    private final Logger logger = LoggerFactory.getLogger(StandaloneQueueBase.class);

    protected final DatabaseTransactionNotificationApi databaseTransactionNotificationApi;
    protected final DaoConfig daoConfig;
    protected final DBI dbi;
    protected final DataSource dataSource;
    protected final Clock clock;
    protected final MetricRegistry metricRegistry;
    protected final AtomicLong nbEvents;
    protected final PersistentQueueConfig queueConfig;
    protected final JmxReporter reporter;

    public StandaloneQueueBase(final PersistentQueueConfig queueConfig,
                               final String jdbcConnection,
                               final String dbUsername,
                               final String dbPassword) {
        this.nbEvents = new AtomicLong(0);
        this.metricRegistry = new MetricRegistry();
        this.clock = new DefaultClock();
        this.databaseTransactionNotificationApi = new DatabaseTransactionNotificationApi();
        this.daoConfig = setupDaoConfig(jdbcConnection, dbUsername, dbPassword, MAX_POOL_CONNECTIONS);
        this.dataSource = setupDataSource(daoConfig);
        this.dbi = setupDBI(dataSource);
        this.queueConfig = queueConfig;

        reporter = JmxReporter.forRegistry(metricRegistry)
                .inDomain("org.killbill.queue.standalone." + QUEUE_NAME)
                .build();
        reporter.start();
    }


    @Override
    public void stop() throws Exception {
        boolean shutdownPool = false;
        if (dataSource instanceof HikariDataSource) {
            ((HikariDataSource) dataSource).close();
            shutdownPool = true;
        }

        reporter.stop();

        logger.info("Stopped test instance {}, (shutdown pool={})", QUEUE_NAME, shutdownPool);
    }

    public long getNbEvents() {
        return nbEvents.get();
    }

    public long incNbEvents() {
        return nbEvents.incrementAndGet();

    }

    protected static void insertNonNullValue(final Map<String, String> config, final String key, final String value) {
        if (Strings.isNullOrEmpty(value)) {
            return;
        }
        config.put(key, value);
    }

    private DataSource setupDataSource(final DaoConfig daoConfig) {
        final DataSourceProvider dataSourceProvider = new DataSourceProvider(daoConfig);
        return dataSourceProvider.get();
    }

    private DBI setupDBI(final DataSource dataSource) {
        final DBI dbi = new DBI(dataSource);
        InTransaction.setupDBI(dbi);
        dbi.setTransactionHandler(new NotificationTransactionHandler(databaseTransactionNotificationApi));
        return dbi;
    }

    private DaoConfig setupDaoConfig(final String jdbcConnection,
                                     final String username,
                                     final String password,
                                     final String maxActive) {

        final Map<String, String> config = new HashMap<String, String>();
        insertNonNullValue(config, "org.killbill.dao.url", jdbcConnection);
        insertNonNullValue(config, "org.killbill.dao.user", username);
        insertNonNullValue(config, "org.killbill.dao.password", password);
        insertNonNullValue(config, "org.killbill.dao.maxActive", maxActive);

        final ConfigSource configSource = new ConfigSource() {
            @Override
            public String getString(final String propertyName) {
                return config.get(propertyName);
            }
        };
        return new ConfigurationObjectFactory(configSource).build(DaoConfig.class);
    }


    private static double nanoToMSecIfMetricsTime(final String metricsName, double value) {
        if (metricsName.endsWith("Time")) {
            return value / QueueServer.NANO_TO_MSEC;
        } else {
            return value;
        }
    }

}
