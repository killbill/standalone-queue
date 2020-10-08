package org.killbill.queue.standalone.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
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
