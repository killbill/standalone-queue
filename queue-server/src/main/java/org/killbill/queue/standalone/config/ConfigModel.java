package org.killbill.queue.standalone.config;


import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.google.common.collect.ImmutableMap;
import org.killbill.notificationq.api.NotificationQueueConfig;
import org.skife.config.ConfigSource;
import org.skife.config.ConfigurationObjectFactory;

import java.util.Map;
import java.util.stream.Collectors;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class ConfigModel {

    private static final String NOTIFICATION_PREFIX = "org.killbill.notificationq.main.";

    private App app;
    private Logging logging;
    private Datastore datastore;
    private Map<String, String> notification;

    public ConfigModel() {
    }

    public void initialize() {
        final Map<String, String> newNotification =
                notification.entrySet().stream().collect(Collectors.toMap(
                        entry ->  NOTIFICATION_PREFIX + entry.getKey(),
                        entry -> entry.getValue())
                );
        this.notification = newNotification;
    }

    public App getApp() {
        return app;
    }

    public Logging getLogging() {
        return logging;
    }

    public Datastore getDatastore() {
        return datastore;
    }

    public NotificationQueueConfig getNotificationQueueConfig() {
        final ConfigSource configSource = propertyName -> notification.get(propertyName);
        final NotificationQueueConfig notificationQueueConfig = new ConfigurationObjectFactory(configSource).buildWithReplacements(NotificationQueueConfig.class,
                ImmutableMap.<String, String>of("instanceName", "main"));
        return notificationQueueConfig;
    }

    public static class App {
        private int port;
        private int nbThreads;

        public App() {
        }

        public int getPort() {
            return port;
        }
        public int getNbThreads() {
            return nbThreads;
        }
    }


    public static class Logging {
        private String level;
        private String format;

        public Logging() {
        }
        public String getLevel() {
            return level;
        }
        public String getFormat() {
            return format;
        }
    }

    public static class Datastore {
        private String database;
        private String host;
        private int port;
        private String user;
        private String password;

        public Datastore() {
        }
        public String getDatabase() {
            return database;
        }
        public String getHost() {
            return host;
        }
        public int getPort() {
            return port;
        }
        public String getUser() {
            return user;
        }
        public String getPassword() {
            return password;
        }
    }

}
