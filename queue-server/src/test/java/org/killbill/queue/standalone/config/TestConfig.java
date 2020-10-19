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

package org.killbill.queue.standalone.config;

import org.testng.annotations.Test;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.testng.Assert.assertEquals;

public class TestConfig {

    @Test
    public void testConfig() throws IOException, URISyntaxException {

        //System.setProperty("org.killbill.queue.standalone.config", "file:///tmp/test.yml");

        final Config conf = new Config();
        final ConfigModel config = conf.getConfig();

        assertEquals(config.getApp().getNbThreads(), 30);
        assertEquals(config.getApp().getPort(), 9999);

        assertEquals(config.getDatastore().getPort(), 5432);
        assertEquals(config.getDatastore().getHost(), "localhost");
        assertEquals(config.getDatastore().getUser(), "postgres");
        assertEquals(config.getDatastore().getPassword(), "postgres");
        assertEquals(config.getDatastore().getDatabase(), "standalalone_test");

        assertEquals(config.getLogging().getLevel(), "debug");

        assertEquals(config.getNotificationQueueConfig().getClaimedTime().toString(), "5m");
        assertEquals(config.getNotificationQueueConfig().getPollingSleepTimeMs(), 3000);
        assertEquals(config.getNotificationQueueConfig().geMaxDispatchThreads(), 10);
        assertEquals(config.getNotificationQueueConfig().getMaxInFlightEntries(), -1);
        // Not the default, validates our config values are taken into account
        assertEquals(config.getNotificationQueueConfig().getPersistentQueueMode().name(), "POLLING");

        assertEquals(config.getNotificationQueueConfig().getTableName(), "standalone_notifications");
        assertEquals(config.getNotificationQueueConfig().getHistoryTableName(), "standalone_notifications_history");
    }
}
