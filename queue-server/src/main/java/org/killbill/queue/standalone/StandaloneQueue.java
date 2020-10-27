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
import org.killbill.billing.queue.rpc.gen.EventMsg;

public interface StandaloneQueue {

    void start() throws Exception;

    void stop() throws Exception;

    void insertEntryIntoQueue(final EventMsg request) throws Exception;

    void ackEvent(final String userToken, final boolean success);

    void registerResponseObserver(final String clientId, ServerCallStreamObserver<EventMsg> responseObserver);

    void unregisterResponseObserver(final String clientId);
}
