package uk.co.fivium.gisframework.grpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.grpc.gis.ArcGisServiceGrpc;
import uk.co.fivium.grpc.gis.CalculateAreaRequest;
import uk.co.fivium.grpc.gis.CalculateAreaResponse;

@ExtendWith(MockitoExtension.class)
class GrpcCallRecordingInterceptorTest {

  @Mock
  private Channel channel;

  @Mock
  private ClientCall<CalculateAreaRequest, CalculateAreaResponse> clientCall;

  private final GrpcRequestHandler grpcRequestHandler = new GrpcRequestHandler();

  private GrpcCallRecordingInterceptor grpcCallRecordingInterceptor;

  @BeforeEach
  void setUp() {
    grpcCallRecordingInterceptor = new GrpcCallRecordingInterceptor(grpcRequestHandler);
  }

  @AfterEach
  void tearDown() {
    grpcRequestHandler.clear();
  }

  @Test
  void interceptCall() {
    var method = ArcGisServiceGrpc.getCalculateAreaMethod();
    when(channel.newCall(method, CallOptions.DEFAULT)).thenReturn(clientCall);

    var result = grpcCallRecordingInterceptor.interceptCall(method, CallOptions.DEFAULT, channel);

    assertThat(result).isSameAs(clientCall);
    assertThat(grpcRequestHandler.getCountByRequest()).isEqualTo(Map.of("calculateArea", 1L));
  }
}
