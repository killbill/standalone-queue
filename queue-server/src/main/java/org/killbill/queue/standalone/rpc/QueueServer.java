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

package org.killbill.queue.standalone.rpc;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.killbill.billing.queue.rpc.gen.PostEventResponse;
import org.killbill.billing.queue.rpc.gen.QueueApiGrpc;
import org.killbill.bus.api.PersistentBus;
import org.killbill.notificationq.api.NotificationQueueService;
import org.killbill.queue.standalone.StandaloneQueueNotification;
import org.killbill.queue.standalone.config.Config;
import org.killbill.queue.standalone.config.ConfigModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.Executors;

public class QueueServer {

    private static final Logger logger = LoggerFactory.getLogger(QueueServer.class);

    public static final long NANO_TO_MSEC = 1000 * 1000;

    private final ConfigModel config;

    private StandaloneQueueNotification queue;
    private QueueGRPCServer queueServer;

    public QueueServer(final ConfigModel inputConfig) {
        this.config = inputConfig;
    }

    private synchronized void start() throws IOException, InterruptedException, NotificationQueueService.NotificationQueueAlreadyExists {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            // Use stderr here since the logger may has been reset by its JVM shutdown hook.
            System.err.println("*** shutting down gRPC server since JVM is shutting down");
            try {
                if (queue != null) {
                    queue.stop();
                }
                if (queueServer != null) {
                    queueServer.stop();
                }
            } catch (final Exception e) {
                e.printStackTrace();
            }
            System.err.println("*** server shut down");
        }));

        logger.info(
                "Starting queue server serverPort='{}', grpcThreads='{}'" +
                        " jdbcConn='{}', jdbcUser='{}', jdbcPwd='{}'",
                config.getApp().getPort(),
                config.getApp().getNbThreads(),
                config.getDatastore().getJdbcConn(),
                config.getDatastore().getUser(),
                config.getDatastore().getPassword());

        this.queue = new StandaloneQueueNotification(config.getDatastore().getJdbcConn(),
                config.getDatastore().getUser(),
                config.getDatastore().getPassword(),
                config.getNotificationQueueConfig());
        queue.start();

        this.queueServer = new QueueGRPCServer(config.getApp().getPort(), config.getApp().getNbThreads(), queue);
        queueServer.startAndWait();
    }


    public static class QueueGRPCServer {

        private final Server server;

        public QueueGRPCServer(final int port,
                               final int grpcThreads,
                               final StandaloneQueueNotification queue) {
            this.server = ServerBuilder.forPort(port)
                    .executor(Executors.newFixedThreadPool(grpcThreads))
                    .addService(new QueueService(queue))
                    .build();
        }

        public void startAndWait() throws IOException, InterruptedException {
            server.start();
            server.awaitTermination();
        }

        public void stop() throws IOException {
            server.shutdown();
        }
    }


    private static class QueueService extends QueueApiGrpc.QueueApiImplBase {

        private StandaloneQueueNotification queue;

        public QueueService(final StandaloneQueueNotification queue) {
            this.queue = queue;
        }

        public void postEvent(org.killbill.billing.queue.rpc.gen.EventMsg request,
                              io.grpc.stub.StreamObserver<org.killbill.billing.queue.rpc.gen.PostEventResponse> responseObserver) {



            try {
                queue.insertEntryIntoQueue(request);
                responseObserver.onNext(PostEventResponse.newBuilder().build());
                responseObserver.onCompleted();
            } catch (final PersistentBus.EventBusException e) {
                responseObserver.onError(e);
            }
        }

        public void subscribeEvents(org.killbill.billing.queue.rpc.gen.SubscriptionRequest request,
                                    io.grpc.stub.StreamObserver<org.killbill.billing.queue.rpc.gen.EventMsg> responseObserver) {


        }

    }

    public static void main(final String[] args) throws IOException, InterruptedException, URISyntaxException, NotificationQueueService.NotificationQueueAlreadyExists {
        final ConfigModel config = (new Config()).getConfig();
        final QueueServer queueServer = new QueueServer(config);
        queueServer.start();
    }

}
