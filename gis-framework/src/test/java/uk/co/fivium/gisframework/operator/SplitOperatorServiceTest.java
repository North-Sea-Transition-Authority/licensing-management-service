package uk.co.fivium.gisframework.operator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.gisframework.feature.FeatureTestUtil;
import uk.co.fivium.gisframework.feature.PolygonService;
import uk.co.fivium.gisframework.grpc.GrpcClientService;

@ExtendWith(MockitoExtension.class)
class SplitOperatorServiceTest {

  @Mock
  private PolygonService polygonService;

  @Mock
  private GrpcClientService grpcClientService;

  @Mock
  private OperatorResultProcessingService operatorResultProcessingService;

  @InjectMocks
  private SplitOperatorService splitOperatorService;

  @Test
  void splitPolygon_verifyMethodCalls() {
    var feature = FeatureTestUtil.newBuilder().build();
    var cutterLineEsriJson = "dummy esriJson cutter line";
    var esriJsonPolygon1 = "dummy esriJson polygon 1";
    var esriJsonPolygon2 = "dummy esriJson polygon 2";

    var rawSplitResult1 = "dummy raw split result 1";
    var rawSplitResult2 = "dummy raw split result 2";
    var postProcessedFeature1 = FeatureTestUtil.newBuilder().build();
    var postProcessedFeature2 = FeatureTestUtil.newBuilder().build();

    when(polygonService.getPolygonsAsEsriJson(feature)).thenReturn(List.of(esriJsonPolygon1, esriJsonPolygon2));
    when(grpcClientService.splitPolygon(esriJsonPolygon1, cutterLineEsriJson))
        .thenReturn(List.of(rawSplitResult1, rawSplitResult2));
    when(grpcClientService.splitPolygon(esriJsonPolygon2, cutterLineEsriJson))
        .thenReturn(List.of());
    when(operatorResultProcessingService.processOutputPolygon(List.of(feature), rawSplitResult1, 1))
        .thenReturn(postProcessedFeature1);
    when(operatorResultProcessingService.processOutputPolygon(List.of(feature), rawSplitResult2, 2))
        .thenReturn(postProcessedFeature2);

    assertThat(splitOperatorService.splitPolygon(feature, cutterLineEsriJson))
        .containsExactly(postProcessedFeature1, postProcessedFeature2);
  }

  @Test
  void splitPolygon_whenNoSplitTookPlace_thenReturnsEmptyList() {
    var feature = FeatureTestUtil.newBuilder().build();
    var cutterLineEsriJson = "dummy esriJson cutter line";
    var esriJsonPolygon = "dummy esriJson polygon";

    when(polygonService.getPolygonsAsEsriJson(feature)).thenReturn(List.of(esriJsonPolygon));
    when(grpcClientService.splitPolygon(esriJsonPolygon, cutterLineEsriJson)).thenReturn(List.of());

    assertThat(splitOperatorService.splitPolygon(feature, cutterLineEsriJson)).isEmpty();
    verify(operatorResultProcessingService, never()).processOutputPolygon(anyList(), anyString(), anyInt());
  }
}