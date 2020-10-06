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

import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.killbill.billing.queue.rpc.gen.QueueApiGrpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

public class QueueServer {

    public static final long NANO_TO_MSEC = 1000 * 1000;

    private static final String SERVER_PORT_PROP = "org.killbill.queue.standalone.server.port";

    private static final String JDBC_CONN_PROP = "org.killbill.queue.standalone.jdbc.conn";
    private static final String JDBC_USER_PROP = "org.killbill.queue.standalone.jdbc.user";
    private static final String JDBC_PWD_PROP = "org.killbill.queue.standalone.jdbc.addr";

    private static final String GRPC_THREADS_PROP = "org.killbill.queue.standalone.grpc.threads";

    public static final String DEFAULT_DATA_SERVER_PORT = "21345";
    public static final String DEFAULT_GRPC_THREADS = "30";


    // TODO
    private static final String DEFAULT_JDBC_CONNECTION = "jdbc:postgres://127.0.0.1:5432/standalone_queue";
    private static final String DEFAULT_DB_USERNAME = "embs_test";
    private static final String DEFAULT_DB_PWD = "embs_test";

    private final int serverPort;
    private final String jdbcConn;
    private final String jdbcUser;
    private final String jdbcPwd;

    private final int grpcThreads;

    private static final Logger logger = LoggerFactory.getLogger(QueueServer.class);

    public QueueServer(final String serverPort,
                       final String grpcThreads,
                       final String jdbcConn,
                       final String jdbcUser,
                       final String jdbcPwd) {
        this.serverPort = Integer.valueOf(serverPort);
        this.grpcThreads = Integer.valueOf(grpcThreads);
        this.jdbcConn = jdbcConn;
        this.jdbcUser = jdbcUser;
        this.jdbcPwd = jdbcPwd;

        logger.info(
                "Started queue server serverPort='{}', grpcThreads='{}'" +
                        " jdbcConn='{}', jdbcUser='{}', jdbcPwd='{}'",
                this.serverPort,
                this.grpcThreads,
                this.jdbcConn,
                this.jdbcUser,
                this.jdbcPwd);

    }

    private void startServer() throws IOException, InterruptedException {

        final QueueGRPCServer queueServer = new QueueGRPCServer(serverPort,
                grpcThreads,
                jdbcConn,
                jdbcUser,
                jdbcPwd);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                // Use stderr here since the logger may has been reset by its JVM shutdown hook.
                System.err.println("*** shutting down gRPC server since JVM is shutting down");
                try {
                    queueServer.stop();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.err.println("*** server shut down");
            }
        });

        queueServer.startAndWait();
    }

    public static class QueueGRPCServer {

        private final Server server;
        private final Map<String, StandaloneQueue> activeTests;

        public QueueGRPCServer(final int port,
                               final int grpcThreads,
                               final String jdbcConn,
                               final String jdbcUser,
                               final String jdbcPwd) {
            activeTests = new HashMap<String, StandaloneQueue>();
            server = ServerBuilder.forPort(port)
                    .executor(Executors.newFixedThreadPool(grpcThreads))
                    .addService(new QueueService())
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


        public QueueService() {

        }

        public void postEvent(org.killbill.billing.queue.rpc.gen.EventMsg request,
                              io.grpc.stub.StreamObserver<org.killbill.billing.queue.rpc.gen.PostEventResponse> responseObserver) {
        }

        public void subscribeEvents(org.killbill.billing.queue.rpc.gen.SubscriptionRequest request,
                                    io.grpc.stub.StreamObserver<org.killbill.billing.queue.rpc.gen.EventMsg> responseObserver) {
        }

        /*
        public void sendEvent(EventMsg request, StreamObserver<StatusMsg> responseObserver) {


            long before = System.nanoTime();

            final TestInstance instance = activeTests.get(request.getName());
            if (instance == null) {
                logger.warn("Ignoring event for test name {}", request.getName());
                responseObserver.onNext(StatusMsg.newBuilder().setSuccess(false).build());
                responseObserver.onCompleted();
                return;
            }


            long v = instance.incNbEvents();
            boolean success = false;

            try {


                // We use the source to decide whether this is an event or an entry we want to manually add in the queue
                if (Strings.isNullOrEmpty(request.getSource())) {
                    instance.postEntry(request);
                } else {
                    instance.insertEntryIntoQueue(request);
                }

                success = true;


            } catch (final Exception e) {
                logger.warn("Exception inserting event ", e);
                responseObserver.onNext(StatusMsg.newBuilder().setSuccess(false).build());
                responseObserver.onError(e);

            } finally {

                long after = System.nanoTime();

                final long deltaMilliSec = (after - before) / NANO_TO_MSEC;
                if (deltaMilliSec > 1000) {
                    logger.info("Inserting entry took {} mSec !!!!\n", deltaMilliSec);
                }
                if (v % 1000 == 0) {
                    logger.info("Test {} : got {} events, current evt latency (mSec) = {}",
                            request.getName(),
                            v,
                            deltaMilliSec);
                }
                responseObserver.onNext(StatusMsg.newBuilder().setSuccess(success).build());
                responseObserver.onCompleted();

            }
        }

 */
    }

    public static void main(final String[] args) throws IOException, InterruptedException {

        final QueueServer queueServer = new QueueServer(System.getProperty(SERVER_PORT_PROP,
                DEFAULT_DATA_SERVER_PORT),
                System.getProperty(GRPC_THREADS_PROP,
                        DEFAULT_GRPC_THREADS),
                System.getProperty(JDBC_CONN_PROP,
                        DEFAULT_JDBC_CONNECTION),
                System.getProperty(JDBC_USER_PROP,
                        DEFAULT_DB_USERNAME),
                System.getProperty(JDBC_PWD_PROP, DEFAULT_DB_PWD));
        queueServer.startServer();
    }

}
