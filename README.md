# Overview

Standalone java server that provides a `gRPC` interface to post and receive notification events.

The event engine is based on the internal [Kill Bill queue](https://github.com/killbill/killbill-commons/tree/master/queue).
There is also a client package written in Go to post and subscribe to such notifications.

The goal of this package is to offer a way to *reliably* post and subscribe to notiifcations from a GO application.


## High Level Design

The transport is managed by the internal `gRPC` library (GO client and java server) and consists of one http2 connection.

There are [3 main apis](https://github.com/killbill/standalone-queue/blob/master/api/queue.proto#L13) offered:
* `PostEvent` is a unary gRPC call
* `SubscribeEvents` is a server-side stream gRPC call. The server replies to the client but keeps the stream open (indefinitely) to send events as they arrive
* `Close` Notifies server to unsubscribe the client


gRPC seems to correctly handle long connections, but of course connections can still break, so code requires special hardening logic, both on the client and server side.


### TCP Connections

Both the client and the server are configured to do their best to keep the underyling transport (tcp connection) alive, by sending ping keepAlive.
Note that we don't rely on TCP KeepAlive (which is also configured with a much greater period) as those are not always guaranteed to correctly
behave from end-end (e.g proxys in the middle, ...).

It is important that setings from [client](https://github.com/killbill/standalone-queue/blob/master/queue-client/src/queue/client.go#L258) and [server](https://github.com/killbill/standalone-queue/blob/master/queue-server/src/main/java/org/killbill/queue/standalone/rpc/QueueServer.java#L103)
match (otherwise we observe some pretty crazy behavior).

In normal scenario, we observe one TCP handshake and some periodic ping keepAlive (in addition to regular traffic, if any).

Once correctly configured, the transport is mostly handled by `gRPC` underlying layer, but tcp connections can (and will) still break so we need application client and server additional logic.

## Client State Machine

The client maintains a state machine that will trigger new `SubscribeEvents` call when any error has been detected.
The logic handles the following things:
* Server breaks -> TCP reset -> EOF from the stream -> reconnection from client until server comes back (up to 24 hours with exp backoff)
* TCP connection gets terminated (FIN) resulting in EOF or error -> client re-subscribes

## Server Retry Logic

The server is [configured](https://github.com/killbill/standalone-queue/blob/master/queue-server/src/main/java/org/killbill/queue/standalone/StandaloneNotificationQueueHandler.java#L44) 
to re-enqueue any failed attempts at sending notifications. In addition to that, it does best effort to cleanup state in the following situations:
* client explicitely issues a `Close` operation to cleanup state (perhaps as a result of cleaning its own state on error)
* server registers to `gRPC` internal cancelation events to cleanup state and also verify internal state prior posting events.
* server requeues when there is no connected client







