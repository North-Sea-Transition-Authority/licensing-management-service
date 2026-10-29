package uk.co.fivium.gisframework.grpc;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class GrpcRequestHandlerTest {

  private final GrpcRequestHandler grpcRequestHandler = new GrpcRequestHandler();

  @AfterEach
  void tearDown() {
    grpcRequestHandler.clear();
  }

  @Test
  void recordCall() {
    grpcRequestHandler.recordCall("buildFeaturePolygons");
    grpcRequestHandler.recordCall("find2dOverlappingFeatures");
    grpcRequestHandler.recordCall("find2dOverlappingFeatures");

    assertThat(grpcRequestHandler.getCountByRequest()).isEqualTo(Map.of(
        "buildFeaturePolygons", 1L,
        "find2dOverlappingFeatures", 2L
    ));
  }

  @Test
  void recordCall_whenCalledOnAnotherThread_thenNotRecordedOnThisThread() throws InterruptedException {
    var otherThread = new Thread(() -> grpcRequestHandler.recordCall("buildFeaturePolygons"));

    otherThread.start();
    otherThread.join();

    assertThat(grpcRequestHandler.getCountByRequest()).isEmpty();
  }

  @Test
  void clear() {
    grpcRequestHandler.recordCall("buildFeaturePolygons");

    grpcRequestHandler.clear();

    assertThat(grpcRequestHandler.getCountByRequest()).isEmpty();
  }
}
