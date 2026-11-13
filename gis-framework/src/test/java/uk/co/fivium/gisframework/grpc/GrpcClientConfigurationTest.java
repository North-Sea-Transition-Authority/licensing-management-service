package uk.co.fivium.gisframework.grpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.grpc.ManagedChannel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.grpc.client.GrpcChannelFactory;

@ExtendWith(MockitoExtension.class)
class GrpcClientConfigurationTest {

  @Mock
  private GrpcChannelFactory grpcChannelFactory;

  @Mock
  private ManagedChannel nodeServerChannel;

  @Test
  void arcGisServiceBlockingStub() {
    when(grpcChannelFactory.createChannel("node-server")).thenReturn(nodeServerChannel);

    var stub = new GrpcClientConfiguration().arcGisServiceBlockingStub(grpcChannelFactory);

    assertThat(stub.getChannel()).isSameAs(nodeServerChannel);
  }
}
