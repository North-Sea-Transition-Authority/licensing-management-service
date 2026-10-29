package uk.co.fivium.gisframework.grpc;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.MethodDescriptor;
import net.devh.boot.grpc.client.interceptor.GrpcGlobalClientInterceptor;

/**
 * Records every gRPC call with {@link GrpcRequestHandler} as it is made, against the thread making it.
 */
@GrpcGlobalClientInterceptor
class GrpcCallRecordingInterceptor implements ClientInterceptor {

  private final GrpcRequestHandler grpcRequestHandler;

  GrpcCallRecordingInterceptor(GrpcRequestHandler grpcRequestHandler) {
    this.grpcRequestHandler = grpcRequestHandler;
  }

  @Override
  public <Q, R> ClientCall<Q, R> interceptCall(
      MethodDescriptor<Q, R> method,
      CallOptions callOptions,
      Channel next
  ) {
    grpcRequestHandler.recordCall(method.getBareMethodName());
    return next.newCall(method, callOptions);
  }
}
