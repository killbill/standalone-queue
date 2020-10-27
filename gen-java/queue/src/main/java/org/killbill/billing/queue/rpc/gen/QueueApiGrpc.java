package org.killbill.billing.queue.rpc.gen;

import static io.grpc.MethodDescriptor.generateFullMethodName;
import static io.grpc.stub.ClientCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ClientCalls.asyncClientStreamingCall;
import static io.grpc.stub.ClientCalls.asyncServerStreamingCall;
import static io.grpc.stub.ClientCalls.asyncUnaryCall;
import static io.grpc.stub.ClientCalls.blockingServerStreamingCall;
import static io.grpc.stub.ClientCalls.blockingUnaryCall;
import static io.grpc.stub.ClientCalls.futureUnaryCall;
import static io.grpc.stub.ServerCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ServerCalls.asyncClientStreamingCall;
import static io.grpc.stub.ServerCalls.asyncServerStreamingCall;
import static io.grpc.stub.ServerCalls.asyncUnaryCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedStreamingCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.30.0)",
    comments = "Source: api/queue.proto")
public final class QueueApiGrpc {

  private QueueApiGrpc() {}

  public static final String SERVICE_NAME = "queue.QueueApi";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<org.killbill.billing.queue.rpc.gen.EventMsg,
      org.killbill.billing.queue.rpc.gen.PostEventResponse> getPostEventMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "PostEvent",
      requestType = org.killbill.billing.queue.rpc.gen.EventMsg.class,
      responseType = org.killbill.billing.queue.rpc.gen.PostEventResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<org.killbill.billing.queue.rpc.gen.EventMsg,
      org.killbill.billing.queue.rpc.gen.PostEventResponse> getPostEventMethod() {
    io.grpc.MethodDescriptor<org.killbill.billing.queue.rpc.gen.EventMsg, org.killbill.billing.queue.rpc.gen.PostEventResponse> getPostEventMethod;
    if ((getPostEventMethod = QueueApiGrpc.getPostEventMethod) == null) {
      synchronized (QueueApiGrpc.class) {
        if ((getPostEventMethod = QueueApiGrpc.getPostEventMethod) == null) {
          QueueApiGrpc.getPostEventMethod = getPostEventMethod =
              io.grpc.MethodDescriptor.<org.killbill.billing.queue.rpc.gen.EventMsg, org.killbill.billing.queue.rpc.gen.PostEventResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "PostEvent"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.killbill.billing.queue.rpc.gen.EventMsg.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.killbill.billing.queue.rpc.gen.PostEventResponse.getDefaultInstance()))
              .setSchemaDescriptor(new QueueApiMethodDescriptorSupplier("PostEvent"))
              .build();
        }
      }
    }
    return getPostEventMethod;
  }

  private static volatile io.grpc.MethodDescriptor<org.killbill.billing.queue.rpc.gen.SubscriptionRequest,
      org.killbill.billing.queue.rpc.gen.EventMsg> getSubscribeEventsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "SubscribeEvents",
      requestType = org.killbill.billing.queue.rpc.gen.SubscriptionRequest.class,
      responseType = org.killbill.billing.queue.rpc.gen.EventMsg.class,
      methodType = io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
  public static io.grpc.MethodDescriptor<org.killbill.billing.queue.rpc.gen.SubscriptionRequest,
      org.killbill.billing.queue.rpc.gen.EventMsg> getSubscribeEventsMethod() {
    io.grpc.MethodDescriptor<org.killbill.billing.queue.rpc.gen.SubscriptionRequest, org.killbill.billing.queue.rpc.gen.EventMsg> getSubscribeEventsMethod;
    if ((getSubscribeEventsMethod = QueueApiGrpc.getSubscribeEventsMethod) == null) {
      synchronized (QueueApiGrpc.class) {
        if ((getSubscribeEventsMethod = QueueApiGrpc.getSubscribeEventsMethod) == null) {
          QueueApiGrpc.getSubscribeEventsMethod = getSubscribeEventsMethod =
              io.grpc.MethodDescriptor.<org.killbill.billing.queue.rpc.gen.SubscriptionRequest, org.killbill.billing.queue.rpc.gen.EventMsg>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "SubscribeEvents"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.killbill.billing.queue.rpc.gen.SubscriptionRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.killbill.billing.queue.rpc.gen.EventMsg.getDefaultInstance()))
              .setSchemaDescriptor(new QueueApiMethodDescriptorSupplier("SubscribeEvents"))
              .build();
        }
      }
    }
    return getSubscribeEventsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<org.killbill.billing.queue.rpc.gen.AckRequest,
      org.killbill.billing.queue.rpc.gen.AckResponse> getAckMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Ack",
      requestType = org.killbill.billing.queue.rpc.gen.AckRequest.class,
      responseType = org.killbill.billing.queue.rpc.gen.AckResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<org.killbill.billing.queue.rpc.gen.AckRequest,
      org.killbill.billing.queue.rpc.gen.AckResponse> getAckMethod() {
    io.grpc.MethodDescriptor<org.killbill.billing.queue.rpc.gen.AckRequest, org.killbill.billing.queue.rpc.gen.AckResponse> getAckMethod;
    if ((getAckMethod = QueueApiGrpc.getAckMethod) == null) {
      synchronized (QueueApiGrpc.class) {
        if ((getAckMethod = QueueApiGrpc.getAckMethod) == null) {
          QueueApiGrpc.getAckMethod = getAckMethod =
              io.grpc.MethodDescriptor.<org.killbill.billing.queue.rpc.gen.AckRequest, org.killbill.billing.queue.rpc.gen.AckResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "Ack"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.killbill.billing.queue.rpc.gen.AckRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.killbill.billing.queue.rpc.gen.AckResponse.getDefaultInstance()))
              .setSchemaDescriptor(new QueueApiMethodDescriptorSupplier("Ack"))
              .build();
        }
      }
    }
    return getAckMethod;
  }

  private static volatile io.grpc.MethodDescriptor<org.killbill.billing.queue.rpc.gen.CloseRequest,
      org.killbill.billing.queue.rpc.gen.CloseResponse> getCloseMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Close",
      requestType = org.killbill.billing.queue.rpc.gen.CloseRequest.class,
      responseType = org.killbill.billing.queue.rpc.gen.CloseResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<org.killbill.billing.queue.rpc.gen.CloseRequest,
      org.killbill.billing.queue.rpc.gen.CloseResponse> getCloseMethod() {
    io.grpc.MethodDescriptor<org.killbill.billing.queue.rpc.gen.CloseRequest, org.killbill.billing.queue.rpc.gen.CloseResponse> getCloseMethod;
    if ((getCloseMethod = QueueApiGrpc.getCloseMethod) == null) {
      synchronized (QueueApiGrpc.class) {
        if ((getCloseMethod = QueueApiGrpc.getCloseMethod) == null) {
          QueueApiGrpc.getCloseMethod = getCloseMethod =
              io.grpc.MethodDescriptor.<org.killbill.billing.queue.rpc.gen.CloseRequest, org.killbill.billing.queue.rpc.gen.CloseResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "Close"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.killbill.billing.queue.rpc.gen.CloseRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.killbill.billing.queue.rpc.gen.CloseResponse.getDefaultInstance()))
              .setSchemaDescriptor(new QueueApiMethodDescriptorSupplier("Close"))
              .build();
        }
      }
    }
    return getCloseMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static QueueApiStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<QueueApiStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<QueueApiStub>() {
        @java.lang.Override
        public QueueApiStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new QueueApiStub(channel, callOptions);
        }
      };
    return QueueApiStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static QueueApiBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<QueueApiBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<QueueApiBlockingStub>() {
        @java.lang.Override
        public QueueApiBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new QueueApiBlockingStub(channel, callOptions);
        }
      };
    return QueueApiBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static QueueApiFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<QueueApiFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<QueueApiFutureStub>() {
        @java.lang.Override
        public QueueApiFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new QueueApiFutureStub(channel, callOptions);
        }
      };
    return QueueApiFutureStub.newStub(factory, channel);
  }

  /**
   */
  public static abstract class QueueApiImplBase implements io.grpc.BindableService {

    /**
     */
    public void postEvent(org.killbill.billing.queue.rpc.gen.EventMsg request,
        io.grpc.stub.StreamObserver<org.killbill.billing.queue.rpc.gen.PostEventResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getPostEventMethod(), responseObserver);
    }

    /**
     */
    public void subscribeEvents(org.killbill.billing.queue.rpc.gen.SubscriptionRequest request,
        io.grpc.stub.StreamObserver<org.killbill.billing.queue.rpc.gen.EventMsg> responseObserver) {
      asyncUnimplementedUnaryCall(getSubscribeEventsMethod(), responseObserver);
    }

    /**
     */
    public void ack(org.killbill.billing.queue.rpc.gen.AckRequest request,
        io.grpc.stub.StreamObserver<org.killbill.billing.queue.rpc.gen.AckResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getAckMethod(), responseObserver);
    }

    /**
     */
    public void close(org.killbill.billing.queue.rpc.gen.CloseRequest request,
        io.grpc.stub.StreamObserver<org.killbill.billing.queue.rpc.gen.CloseResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getCloseMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getPostEventMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.killbill.billing.queue.rpc.gen.EventMsg,
                org.killbill.billing.queue.rpc.gen.PostEventResponse>(
                  this, METHODID_POST_EVENT)))
          .addMethod(
            getSubscribeEventsMethod(),
            asyncServerStreamingCall(
              new MethodHandlers<
                org.killbill.billing.queue.rpc.gen.SubscriptionRequest,
                org.killbill.billing.queue.rpc.gen.EventMsg>(
                  this, METHODID_SUBSCRIBE_EVENTS)))
          .addMethod(
            getAckMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.killbill.billing.queue.rpc.gen.AckRequest,
                org.killbill.billing.queue.rpc.gen.AckResponse>(
                  this, METHODID_ACK)))
          .addMethod(
            getCloseMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.killbill.billing.queue.rpc.gen.CloseRequest,
                org.killbill.billing.queue.rpc.gen.CloseResponse>(
                  this, METHODID_CLOSE)))
          .build();
    }
  }

  /**
   */
  public static final class QueueApiStub extends io.grpc.stub.AbstractAsyncStub<QueueApiStub> {
    private QueueApiStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected QueueApiStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new QueueApiStub(channel, callOptions);
    }

    /**
     */
    public void postEvent(org.killbill.billing.queue.rpc.gen.EventMsg request,
        io.grpc.stub.StreamObserver<org.killbill.billing.queue.rpc.gen.PostEventResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getPostEventMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void subscribeEvents(org.killbill.billing.queue.rpc.gen.SubscriptionRequest request,
        io.grpc.stub.StreamObserver<org.killbill.billing.queue.rpc.gen.EventMsg> responseObserver) {
      asyncServerStreamingCall(
          getChannel().newCall(getSubscribeEventsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void ack(org.killbill.billing.queue.rpc.gen.AckRequest request,
        io.grpc.stub.StreamObserver<org.killbill.billing.queue.rpc.gen.AckResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getAckMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void close(org.killbill.billing.queue.rpc.gen.CloseRequest request,
        io.grpc.stub.StreamObserver<org.killbill.billing.queue.rpc.gen.CloseResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getCloseMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class QueueApiBlockingStub extends io.grpc.stub.AbstractBlockingStub<QueueApiBlockingStub> {
    private QueueApiBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected QueueApiBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new QueueApiBlockingStub(channel, callOptions);
    }

    /**
     */
    public org.killbill.billing.queue.rpc.gen.PostEventResponse postEvent(org.killbill.billing.queue.rpc.gen.EventMsg request) {
      return blockingUnaryCall(
          getChannel(), getPostEventMethod(), getCallOptions(), request);
    }

    /**
     */
    public java.util.Iterator<org.killbill.billing.queue.rpc.gen.EventMsg> subscribeEvents(
        org.killbill.billing.queue.rpc.gen.SubscriptionRequest request) {
      return blockingServerStreamingCall(
          getChannel(), getSubscribeEventsMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.killbill.billing.queue.rpc.gen.AckResponse ack(org.killbill.billing.queue.rpc.gen.AckRequest request) {
      return blockingUnaryCall(
          getChannel(), getAckMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.killbill.billing.queue.rpc.gen.CloseResponse close(org.killbill.billing.queue.rpc.gen.CloseRequest request) {
      return blockingUnaryCall(
          getChannel(), getCloseMethod(), getCallOptions(), request);
    }
  }

  /**
   */
  public static final class QueueApiFutureStub extends io.grpc.stub.AbstractFutureStub<QueueApiFutureStub> {
    private QueueApiFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected QueueApiFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new QueueApiFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.killbill.billing.queue.rpc.gen.PostEventResponse> postEvent(
        org.killbill.billing.queue.rpc.gen.EventMsg request) {
      return futureUnaryCall(
          getChannel().newCall(getPostEventMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.killbill.billing.queue.rpc.gen.AckResponse> ack(
        org.killbill.billing.queue.rpc.gen.AckRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getAckMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.killbill.billing.queue.rpc.gen.CloseResponse> close(
        org.killbill.billing.queue.rpc.gen.CloseRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getCloseMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_POST_EVENT = 0;
  private static final int METHODID_SUBSCRIBE_EVENTS = 1;
  private static final int METHODID_ACK = 2;
  private static final int METHODID_CLOSE = 3;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final QueueApiImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(QueueApiImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_POST_EVENT:
          serviceImpl.postEvent((org.killbill.billing.queue.rpc.gen.EventMsg) request,
              (io.grpc.stub.StreamObserver<org.killbill.billing.queue.rpc.gen.PostEventResponse>) responseObserver);
          break;
        case METHODID_SUBSCRIBE_EVENTS:
          serviceImpl.subscribeEvents((org.killbill.billing.queue.rpc.gen.SubscriptionRequest) request,
              (io.grpc.stub.StreamObserver<org.killbill.billing.queue.rpc.gen.EventMsg>) responseObserver);
          break;
        case METHODID_ACK:
          serviceImpl.ack((org.killbill.billing.queue.rpc.gen.AckRequest) request,
              (io.grpc.stub.StreamObserver<org.killbill.billing.queue.rpc.gen.AckResponse>) responseObserver);
          break;
        case METHODID_CLOSE:
          serviceImpl.close((org.killbill.billing.queue.rpc.gen.CloseRequest) request,
              (io.grpc.stub.StreamObserver<org.killbill.billing.queue.rpc.gen.CloseResponse>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  private static abstract class QueueApiBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    QueueApiBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return org.killbill.billing.queue.rpc.gen.Queue.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("QueueApi");
    }
  }

  private static final class QueueApiFileDescriptorSupplier
      extends QueueApiBaseDescriptorSupplier {
    QueueApiFileDescriptorSupplier() {}
  }

  private static final class QueueApiMethodDescriptorSupplier
      extends QueueApiBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    QueueApiMethodDescriptorSupplier(String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (QueueApiGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new QueueApiFileDescriptorSupplier())
              .addMethod(getPostEventMethod())
              .addMethod(getSubscribeEventsMethod())
              .addMethod(getAckMethod())
              .addMethod(getCloseMethod())
              .build();
        }
      }
    }
    return result;
  }
}
