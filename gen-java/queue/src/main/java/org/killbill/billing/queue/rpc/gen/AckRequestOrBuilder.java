// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: api/queue.proto

package org.killbill.billing.queue.rpc.gen;

public interface AckRequestOrBuilder extends
    // @@protoc_insertion_point(interface_extends:queue.AckRequest)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <code>string user_token = 1;</code>
   * @return The userToken.
   */
  java.lang.String getUserToken();
  /**
   * <code>string user_token = 1;</code>
   * @return The bytes for userToken.
   */
  com.google.protobuf.ByteString
      getUserTokenBytes();

  /**
   * <code>bool success = 2;</code>
   * @return The success.
   */
  boolean getSuccess();
}
