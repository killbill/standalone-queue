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


import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.google.common.collect.ImmutableMap;
import org.killbill.notificationq.api.NotificationQueueConfig;
import org.skife.config.ConfigSource;
import org.skife.config.ConfigurationObjectFactory;

import java.util.Map;
import java.util.stream.Collectors;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class ConfigModel {

    static final String PROP_DATASTORE_PORT = "QUEUE_DATASTORE_PORT";
    static final String PROP_DATASTORE_DATABASE = "QUEUE_DATASTORE_DATABASE";
    static final String PROP_DATASTORE_HOST = "QUEUE_DATASTORE_HOST";
    static final String PROP_DATASTORE_USER = "QUEUE_DATASTORE_USER";
    static final String PROP_DATASTORE_PASSWORD = "QUEUE_DATASTORE_PASSWORD";

    static final String PROP_APP_PORT = "QUEUE_APP_PORT";
    static final String PROP_APP_ACK_TIME_SEC = "QUEUE_ACK_TIME_SEC";

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
                        entry -> NOTIFICATION_PREFIX + entry.getKey(),
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
        private Integer port;
        private Integer nbThreads;
        private Boolean recycleTcpConn;
        private Long ackTimeSec;

        public App() {
        }

        public int getPort() {
            return port;
        }

        public void setPort(final Integer port) {
            final Integer sysPort = fromSystemProperty(PROP_APP_PORT, Integer.valueOf(0));
            if (sysPort != null) {
                this.port = sysPort;
            } else {
                this.port = port;
            }
        }


        public int getNbThreads() {
            return nbThreads;
        }

        public boolean getRecycleTcpConn() {
            return recycleTcpConn;
        }

        public long getAckTimeSec() {
            return ackTimeSec;
        }

        public void setAckTimeSec(final Long ackTimeSec) {
            final Long oAckTimeSec = fromSystemProperty(PROP_APP_ACK_TIME_SEC, Long.valueOf(0));
            if (oAckTimeSec != null) {
                this.ackTimeSec = oAckTimeSec;
            } else {
                this.ackTimeSec = ackTimeSec;
            }
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
        private Integer port;
        private String user;
        private String password;

        public Datastore() {
        }

        public String getDatabase() {
            return database;
        }

        public void setDatabase(final String database) {
            final String sysDatabase = fromSystemProperty(PROP_DATASTORE_DATABASE, "");
            if (sysDatabase != null) {
                this.database = sysDatabase;
            } else {
                this.database = database;
            }
        }

        public void setHost(final String host) {
            final String sysHost = fromSystemProperty(PROP_DATASTORE_HOST, "");
            if (sysHost != null) {
                this.host = sysHost;
            } else {
                this.host = host;
            }
        }

        public void setUser(final String user) {
            final String sysUser = fromSystemProperty(PROP_DATASTORE_USER, "");
            if (sysUser != null) {
                this.user = sysUser;
            } else {
                this.user = user;
            }
        }

        public void setPassword(final String password) {
            final String sysPwd = fromSystemProperty(PROP_DATASTORE_PASSWORD, "");
            if (sysPwd != null) {
                this.password = sysPwd;
            } else {
                this.password = password;
            }
        }

        public String getHost() {
            return host;
        }

        public Integer getPort() {
            return port;
        }

        public void setPort(final Integer port) {
            final Integer sysPort = fromSystemProperty(PROP_DATASTORE_PORT, Integer.valueOf(0));
            if (sysPort != null) {
                this.port = sysPort;
            } else {
                this.port = port;
            }
        }

        public String getUser() {
            return user;
        }

        public String getPassword() {
            return password;
        }

        public String getJdbcConn() {
            return String.format("jdbc:postgresql://%s:%d/%s", host, port, database);
        }

    }


    private static <T> T fromSystemProperty(final String prop, final T dummy)  {
        final String value = System.getProperty(prop);
        if (value == null) {
            return null;
        }
        if (dummy instanceof Integer) {
            return (T) Integer.valueOf(value);
        } else if (dummy instanceof Long) {
            return (T) Long.valueOf(value);
        } else if (dummy instanceof String) {
            return (T) value;
        }
        throw new IllegalStateException(String.format("Unexpected property type %s", dummy.getClass()));
    }

}
