package uk.co.fivium.gisframework.operator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.gisframework.feature.FeatureTestUtil;
import uk.co.fivium.gisframework.feature.Line;
import uk.co.fivium.gisframework.feature.LineService;
import uk.co.fivium.gisframework.feature.LineTestUtil;
import uk.co.fivium.gisframework.feature.PolygonService;
import uk.co.fivium.gisframework.feature.PolygonTestUtil;
import uk.co.fivium.gisframework.grpc.FindParentLineResponse;
import uk.co.fivium.gisframework.grpc.GrpcClientService;
import uk.co.fivium.grpc.gis.LineNavigationType;

@ExtendWith(MockitoExtension.class)
class OperatorResultProcessingServiceTest {

  @Mock
  private PolygonService polygonService;

  @Mock
  private LineService lineService;

  @Mock
  private GrpcClientService grpcClientService;

  @Captor
  private ArgumentCaptor<List<Line>> linesCaptor;

  @InjectMocks
  private OperatorResultProcessingService operatorResultProcessingService;

  @Test
  void processOutputPolygon_whenParentLineFound_savesLineWithCopiedParentAttributesAndNavigationType() {
    var feature = FeatureTestUtil.newBuilder().build();
    var polygon = PolygonTestUtil.newBuilder().withFeature(feature).build();
    var outputPolygonEsriJson = "output polygon";
    var outputLineEsriJson = "output line";
    var parentLineId = UUID.randomUUID();
    var parentAttributes = Map.<String, Object>of("source", "parent line");
    var parentLine = LineTestUtil.newBuilder()
        .withId(parentLineId)
        .withPolygon(polygon)
        .withNavigationType(LineNavigationType.CARTESIAN)
        .withAttributes(parentAttributes)
        .build();

    when(polygonService.getPolygons(List.of(feature))).thenReturn(List.of(polygon));
    when(lineService.getLines(List.of(polygon))).thenReturn(List.of(parentLine));
    when(grpcClientService.explodePolygon(outputPolygonEsriJson)).thenReturn(List.of(outputLineEsriJson));
    when(grpcClientService.findParentLines(List.of(parentLine), List.of(outputLineEsriJson)))
        .thenReturn(new FindParentLineResponse(Map.of(outputLineEsriJson, parentLineId), List.of()));

    operatorResultProcessingService.processOutputPolygon(List.of(feature), outputPolygonEsriJson);

    var expectedLine = LineTestUtil.newBuilder()
        .withEsriJson(outputLineEsriJson)
        .withAttributes(parentAttributes)
        .withNavigationType(LineNavigationType.CARTESIAN)
        .withId(null)
        .withLegacyId(null)
        .withPolygon(null)
        .withRingNumber(null)
        .withRingConnectionOrder(null)
        .build();

    verify(lineService).saveLines(linesCaptor.capture());
    assertThat(linesCaptor.getValue())
        .singleElement()
        .usingRecursiveComparison()
        .isEqualTo(expectedLine);
  }

  @Test
  void processOutputPolygon_whenOrphanLineAndAnyInputLineIsCartesian_savesOrphanAsCartesianWithEmptyAttributes() {
    var feature = FeatureTestUtil.newBuilder().build();
    var polygon = PolygonTestUtil.newBuilder().withFeature(feature).build();
    var outputPolygonEsriJson = "output polygon";
    var orphanLineEsriJson = "orphan line";
    var parentAttributes = Map.<String, Object>of("source", "parent line");
    var inputLine = LineTestUtil.newBuilder()
        .withPolygon(polygon)
        .withNavigationType(LineNavigationType.CARTESIAN)
        .withAttributes(parentAttributes)
        .build();

    when(polygonService.getPolygons(List.of(feature))).thenReturn(List.of(polygon));
    when(lineService.getLines(List.of(polygon))).thenReturn(List.of(inputLine));
    when(grpcClientService.explodePolygon(outputPolygonEsriJson)).thenReturn(List.of(orphanLineEsriJson));
    when(grpcClientService.findParentLines(List.of(inputLine), List.of(orphanLineEsriJson)))
        .thenReturn(new FindParentLineResponse(Map.of(), List.of(orphanLineEsriJson)));

    operatorResultProcessingService.processOutputPolygon(List.of(feature), outputPolygonEsriJson);

    var expectedLine = LineTestUtil.newBuilder()
        .withEsriJson(orphanLineEsriJson)
        .withAttributes(Collections.emptyMap())
        .withNavigationType(LineNavigationType.CARTESIAN)
        .withId(null)
        .withLegacyId(null)
        .withPolygon(null)
        .withRingNumber(null)
        .withRingConnectionOrder(null)
        .build();

    verify(lineService).saveLines(linesCaptor.capture());
    assertThat(linesCaptor.getValue())
        .singleElement()
        .usingRecursiveComparison()
        .isEqualTo(expectedLine);
  }

  @Test
  void processOutputPolygon_whenOrphanLineAndNoInputLineIsCartesian_savesOrphanAsLoxodromeWithEmptyAttributes() {
    var feature = FeatureTestUtil.newBuilder().build();
    var polygon = PolygonTestUtil.newBuilder().withFeature(feature).build();
    var outputPolygonEsriJson = "output polygon";
    var orphanLineEsriJson = "orphan line";
    var parentAttributes = Map.<String, Object>of("source", "parent line");
    var inputLine = LineTestUtil.newBuilder()
        .withPolygon(polygon)
        .withNavigationType(LineNavigationType.LOXODROME)
        .withAttributes(parentAttributes)
        .build();

    when(polygonService.getPolygons(List.of(feature))).thenReturn(List.of(polygon));
    when(lineService.getLines(List.of(polygon))).thenReturn(List.of(inputLine));
    when(grpcClientService.explodePolygon(outputPolygonEsriJson)).thenReturn(List.of(orphanLineEsriJson));
    when(grpcClientService.findParentLines(List.of(inputLine), List.of(orphanLineEsriJson)))
        .thenReturn(new FindParentLineResponse(Map.of(), List.of(orphanLineEsriJson)));

    operatorResultProcessingService.processOutputPolygon(List.of(feature), outputPolygonEsriJson);

    var expectedLine = LineTestUtil.newBuilder()
        .withEsriJson(orphanLineEsriJson)
        .withAttributes(Collections.emptyMap())
        .withNavigationType(LineNavigationType.LOXODROME)
        .withId(null)
        .withLegacyId(null)
        .withPolygon(null)
        .withRingNumber(null)
        .withRingConnectionOrder(null)
        .build();

    verify(lineService).saveLines(linesCaptor.capture());
    assertThat(linesCaptor.getValue())
        .singleElement()
        .usingRecursiveComparison()
        .isEqualTo(expectedLine);
  }
}
