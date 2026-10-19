package uk.co.fivium.gisframework.operator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.gisframework.feature.EntityBackedFeature;
import uk.co.fivium.gisframework.feature.FeatureService;
import uk.co.fivium.gisframework.feature.FeatureTestUtil;
import uk.co.fivium.gisframework.feature.Line;
import uk.co.fivium.gisframework.feature.Polygon;
import uk.co.fivium.gisframework.feature.PolygonService;
import uk.co.fivium.gisframework.feature.PolygonTestUtil;
import uk.co.fivium.gisframework.grpc.GrpcClientService;

@ExtendWith(MockitoExtension.class)
class SplitOperatorServiceTest {

  @Mock
  private FeatureService featureService;

  @Mock
  private PolygonService polygonService;

  @Mock
  private GrpcClientService grpcClientService;

  @Mock
  private OperatorResultProcessingService operatorResultProcessingService;

  @InjectMocks
  private SplitOperatorService splitOperatorService;

  @Test
  void splitPolygon_whenOnePolygonIsCut_thenEachCutPieceAndUncutPolygonKeepItsSourcePolygon() {
    var feature = FeatureTestUtil.newBuilder().build();
    var cutterLineEsriJson = "dummy esriJson cutter line";
    var cutPolygon = PolygonTestUtil.newBuilder().withFeature(feature).withStartDepth(0L).withEndDepth(-100L).build();
    var uncutPolygon = PolygonTestUtil.newBuilder().withFeature(feature).withStartDepth(-100L).withEndDepth(-200L).build();
    var cutPolygonEsriJson = "dummy esriJson cut polygon";
    var uncutPolygonEsriJson = "dummy esriJson uncut polygon";
    var rawSplitResult1 = "dummy raw split result 1";
    var rawSplitResult2 = "dummy raw split result 2";
    var polygonToLines = new LinkedHashMap<Polygon, List<Line>>();
    polygonToLines.put(cutPolygon, List.of());
    polygonToLines.put(uncutPolygon, List.of());
    var entityBackedFeature = new EntityBackedFeature(feature, polygonToLines);
    var postProcessedFeature1 = FeatureTestUtil.newBuilder().build();
    var postProcessedFeature2 = FeatureTestUtil.newBuilder().build();
    var postProcessedFeature3 = FeatureTestUtil.newBuilder().build();

    when(featureService.getEntityBackedFeature(feature)).thenReturn(entityBackedFeature);
    when(polygonService.getPolygonIdToEsriJson(entityBackedFeature)).thenReturn(Map.of(
        cutPolygon.getId(), cutPolygonEsriJson,
        uncutPolygon.getId(), uncutPolygonEsriJson
    ));
    when(grpcClientService.splitPolygon(cutPolygonEsriJson, cutterLineEsriJson))
        .thenReturn(List.of(rawSplitResult1, rawSplitResult2));
    when(grpcClientService.splitPolygon(uncutPolygonEsriJson, cutterLineEsriJson)).thenReturn(List.of());
    var cutPiece1 = new EsriJsonPolygonWithSources(rawSplitResult1, List.of(cutPolygon));
    var cutPiece2 = new EsriJsonPolygonWithSources(rawSplitResult2, List.of(cutPolygon));
    var uncutPiece = new EsriJsonPolygonWithSources(uncutPolygonEsriJson, List.of(uncutPolygon));
    when(operatorResultProcessingService.processOutputPolygon(feature, cutPiece1, 1)).thenReturn(postProcessedFeature1);
    when(operatorResultProcessingService.processOutputPolygon(feature, cutPiece2, 2)).thenReturn(postProcessedFeature2);
    when(operatorResultProcessingService.processOutputPolygon(feature, uncutPiece, 3)).thenReturn(postProcessedFeature3);

    var result = splitOperatorService.splitPolygon(feature, cutterLineEsriJson, null);

    assertThat(result).containsExactly(postProcessedFeature1, postProcessedFeature2, postProcessedFeature3);
  }

  @Test
  void splitPolygon_whenNoSplitTookPlace_thenReturnsEmptyList() {
    var feature = FeatureTestUtil.newBuilder().build();
    var cutterLineEsriJson = "dummy esriJson cutter line";
    var polygon = PolygonTestUtil.newBuilder().withFeature(feature).build();
    var polygonEsriJson = "dummy esriJson polygon";
    var entityBackedFeature = new EntityBackedFeature(feature, Map.of(polygon, List.of()));

    when(featureService.getEntityBackedFeature(feature)).thenReturn(entityBackedFeature);
    when(polygonService.getPolygonIdToEsriJson(entityBackedFeature)).thenReturn(Map.of(polygon.getId(), polygonEsriJson));
    when(grpcClientService.splitPolygon(polygonEsriJson, cutterLineEsriJson)).thenReturn(List.of());

    var result = splitOperatorService.splitPolygon(feature, cutterLineEsriJson, null);

    assertThat(result).isEmpty();
    verifyNoInteractions(operatorResultProcessingService);
  }

  @Test
  void splitPolygon_whenDepthProvided_thenOnlyCutsPolygonsCoveringDepthAndKeepsOthersUncut() {
    var feature = FeatureTestUtil.newBuilder().build();
    var cutterLineEsriJson = "dummy esriJson cutter line";
    var shallowPolygon = PolygonTestUtil.newBuilder().withFeature(feature).withStartDepth(0L).withEndDepth(-100L).build();
    var deepPolygon = PolygonTestUtil.newBuilder().withFeature(feature).withStartDepth(-100L).withEndDepth(-200L).build();
    var shallowPolygonEsriJson = "dummy esriJson shallow polygon";
    var deepPolygonEsriJson = "dummy esriJson deep polygon";
    var rawSplitResult1 = "dummy raw split result 1";
    var rawSplitResult2 = "dummy raw split result 2";
    var keptShallowFeature = FeatureTestUtil.newBuilder().build();
    var deepPieceFeature1 = FeatureTestUtil.newBuilder().build();
    var deepPieceFeature2 = FeatureTestUtil.newBuilder().build();

    var polygonToLines = new LinkedHashMap<Polygon, List<Line>>();
    polygonToLines.put(shallowPolygon, List.of());
    polygonToLines.put(deepPolygon, List.of());
    var entityBackedFeature = new EntityBackedFeature(feature, polygonToLines);

    when(featureService.getEntityBackedFeature(feature)).thenReturn(entityBackedFeature);
    when(polygonService.getPolygonIdToEsriJson(entityBackedFeature)).thenReturn(Map.of(
        shallowPolygon.getId(), shallowPolygonEsriJson,
        deepPolygon.getId(), deepPolygonEsriJson
    ));
    when(grpcClientService.splitPolygon(deepPolygonEsriJson, cutterLineEsriJson))
        .thenReturn(List.of(rawSplitResult1, rawSplitResult2));
    var shallowPiece = new EsriJsonPolygonWithSources(shallowPolygonEsriJson, List.of(shallowPolygon));
    var deepPiece1 = new EsriJsonPolygonWithSources(rawSplitResult1, List.of(deepPolygon));
    var deepPiece2 = new EsriJsonPolygonWithSources(rawSplitResult2, List.of(deepPolygon));
    when(operatorResultProcessingService.processOutputPolygon(feature, shallowPiece, 1)).thenReturn(keptShallowFeature);
    when(operatorResultProcessingService.processOutputPolygon(feature, deepPiece1, 2)).thenReturn(deepPieceFeature1);
    when(operatorResultProcessingService.processOutputPolygon(feature, deepPiece2, 3)).thenReturn(deepPieceFeature2);

    var result = splitOperatorService.splitPolygon(feature, cutterLineEsriJson, -150L);

    assertThat(result).containsExactly(keptShallowFeature, deepPieceFeature1, deepPieceFeature2);
    verify(grpcClientService, never()).splitPolygon(shallowPolygonEsriJson, cutterLineEsriJson);
  }

  @Test
  void splitPolygon_whenOnlyAPolygonOutsideTheDepthWouldBeCut_thenReturnsEmptyList() {
    var feature = FeatureTestUtil.newBuilder().build();
    var cutterLineEsriJson = "dummy esriJson cutter line";
    var shallowPolygon = PolygonTestUtil.newBuilder().withFeature(feature).withStartDepth(0L).withEndDepth(-100L).build();
    var deepPolygon = PolygonTestUtil.newBuilder().withFeature(feature).withStartDepth(-100L).withEndDepth(-200L).build();
    var shallowPolygonEsriJson = "dummy esriJson shallow polygon";
    var deepPolygonEsriJson = "dummy esriJson deep polygon";

    var polygonToLines = new LinkedHashMap<Polygon, List<Line>>();
    polygonToLines.put(shallowPolygon, List.of());
    polygonToLines.put(deepPolygon, List.of());
    var entityBackedFeature = new EntityBackedFeature(feature, polygonToLines);

    when(featureService.getEntityBackedFeature(feature)).thenReturn(entityBackedFeature);
    when(polygonService.getPolygonIdToEsriJson(entityBackedFeature)).thenReturn(Map.of(
        shallowPolygon.getId(), shallowPolygonEsriJson,
        deepPolygon.getId(), deepPolygonEsriJson
    ));
    when(grpcClientService.splitPolygon(deepPolygonEsriJson, cutterLineEsriJson)).thenReturn(List.of());

    var result = splitOperatorService.splitPolygon(feature, cutterLineEsriJson, -150L);

    assertThat(result).isEmpty();
    verify(grpcClientService, never()).splitPolygon(shallowPolygonEsriJson, cutterLineEsriJson);
    verifyNoInteractions(operatorResultProcessingService);
  }
}
