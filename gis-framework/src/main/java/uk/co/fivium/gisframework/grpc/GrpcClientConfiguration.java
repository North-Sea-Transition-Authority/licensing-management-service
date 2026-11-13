package uk.co.fivium.gisframework.grpc;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.GrpcChannelFactory;
import uk.co.fivium.grpc.gis.ArcGisServiceGrpc;

/**
 * Builds the blocking stub used to talk to the ArcGIS node server. The channel is named
 * {@value #NODE_SERVER_CHANNEL} and configured under
 * {@code spring.grpc.client.channel.node-server.*}.
 */
@Configuration
public class GrpcClientConfiguration {

  static final String NODE_SERVER_CHANNEL = "node-server";

  @Bean
  ArcGisServiceGrpc.ArcGisServiceBlockingStub arcGisServiceBlockingStub(
      GrpcChannelFactory grpcChannelFactory
  ) {
    return ArcGisServiceGrpc.newBlockingStub(grpcChannelFactory.createChannel(NODE_SERVER_CHANNEL));
  }
}
