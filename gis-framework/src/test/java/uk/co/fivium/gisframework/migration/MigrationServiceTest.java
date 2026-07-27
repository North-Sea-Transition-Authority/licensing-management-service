package uk.co.fivium.gisframework.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.co.fivium.gisframework.LoggerTestUtil.detachLogAppender;
import static uk.co.fivium.gisframework.LoggerTestUtil.getLogAppender;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.fivium.gisframework.feature.FeatureService;
import uk.co.fivium.gisframework.feature.FeatureTestUtil;
import uk.co.fivium.gisframework.feature.Layer;
import uk.co.fivium.gisframework.feature.Line;
import uk.co.fivium.gisframework.feature.LineTestUtil;
import uk.co.fivium.gisframework.feature.Polygon;
import uk.co.fivium.gisframework.feature.PolygonTestUtil;
import uk.co.fivium.gisframework.grpc.GrpcClientService;
import uk.co.fivium.gisframework.migration.oracle.AttributeLevel;
import uk.co.fivium.gisframework.migration.oracle.EntityBackedOracleShape;
import uk.co.fivium.gisframework.migration.oracle.OracleBoundaryLineTestUtil;
import uk.co.fivium.gisframework.migration.oracle.OracleBoundaryLineWithRing;
import uk.co.fivium.gisframework.migration.oracle.OracleLayerTestUtil;
import uk.co.fivium.gisframework.migration.oracle.OraclePolygonBoundaryTestUtil;
import uk.co.fivium.gisframework.migration.oracle.OracleService;
import uk.co.fivium.gisframework.migration.oracle.OracleShapeTestUtil;
import uk.co.fivium.gisframework.operator.OperatorResultProcessingService;
import uk.co.fivium.grpc.gis.CoordinateSystem;
import uk.co.fivium.grpc.gis.LineNavigationType;

@ExtendWith(MockitoExtension.class)
class MigrationServiceTest {

  @Mock
  private FeatureService featureService;

  @Mock
  private GrpcClientService grpcClientService;

  @Mock
  private OracleService oracleService;

  @Mock
  private OperatorResultProcessingService operatorResultProcessingService;

  @InjectMocks
  private MigrationService migrationService;

  @ParameterizedTest
  @MethodSource("depthToExpectedValue")
  void migratePolygon(Long depth, Long expectedDepth) {
    var feature = new Feature();
    var attributes = Map.of("key", (Object) "value");

    var result = migrationService.migratePolygon(42, feature, depth, depth, attributes);

    var expected = PolygonTestUtil.newBuilder()
        .withLegacyId(42)
        .withFeature(feature)
        .withAttributes(attributes)
        .withStartDepth(expectedDepth)
        .withEndDepth(expectedDepth)
        .build();

    assertThat(result).usingRecursiveComparison().ignoringFields("id").isEqualTo(expected);
  }

  @Test
  void migrateLines() {
    var feature = new Feature();
    feature.setCoordinateSystem(CoordinateSystem.ED50);
    feature.setFeatureArea(BigDecimal.ZERO);

    var polygon = new Polygon();

    var boundary1 = OraclePolygonBoundaryTestUtil.newBuilder()
        .withBoundarySidId(10)
        .build();

    var boundary2 = OraclePolygonBoundaryTestUtil.newBuilder()
        .withBoundarySidId(20)
        .build();

    var oracleLine1 = OracleBoundaryLineTestUtil.newBuilder()
        .withLineSidId(100)
        .withOraclePolygonBoundaryId(boundary1.getBoundarySidId())
        .withConnectionOrder(1L)
        .withLineNavigationType(LineNavigationType.GEODESIC)
        .build();

    var oracleLine2 = OracleBoundaryLineTestUtil.newBuilder()
        .withLineSidId(200)
        .withOraclePolygonBoundaryId(boundary2.getBoundarySidId())
        .withConnectionOrder(2L)
        .withLineNavigationType(LineNavigationType.LOXODROME)
        .build();

    var oracleBoundaries = List.of(boundary1, boundary2);

    var entityBackedShape = new EntityBackedOracleShape(
        OracleShapeTestUtil.newBuilder().build(),
        Map.of(),
        Map.of(
            boundary1, List.of(oracleLine1),
            boundary2, List.of(oracleLine2)
        )
    );

    var migrationResponse = new MigrationResponseDto(
        Map.of(
            100, "esri json 1",
            200, "esri json 2"
        ),
        500.0
    );

    var linesWithRing = List.of(
        new OracleBoundaryLineWithRing(oracleLine1, 0),
        new OracleBoundaryLineWithRing(oracleLine2, 1)
    );

    when(grpcClientService.migrateBlockOrSubarea(linesWithRing, CoordinateSystem.ED50, List.of(), oracleLine1.getShapeSiId()))
        .thenReturn(migrationResponse);
    when(oracleService.getIdToAttributeMapForSiIdAndLevel(
        List.of(boundary1.getBoundarySidId(), boundary2.getBoundarySidId()),
        AttributeLevel.POLYGON_BOUNDARY
    )).thenReturn(Map.of());
    when(oracleService.getIdToAttributeMapForSiIdAndLevel(
        List.of(oracleLine1.getLineSidId(), oracleLine2.getLineSidId()),
        AttributeLevel.BOUNDARY_LINE
    )).thenReturn(Map.of());

    var result = migrationService.migrateLines(feature, polygon, oracleBoundaries, entityBackedShape, List.of());

    var expectedLine1 = LineTestUtil.newBuilder()
        .withId(null)
        .withLegacyId(100)
        .withAttributes(Map.of())
        .withPolygon(polygon)
        .withNavigationType(LineNavigationType.GEODESIC)
        .withEsriJson("esri json 1")
        .withRingNumber(0)
        .withDisplayOrder(1)
        .build();

    var expectedLine2 = LineTestUtil.newBuilder()
        .withId(null)
        .withLegacyId(200)
        .withAttributes(Map.of())
        .withPolygon(polygon)
        .withNavigationType(LineNavigationType.LOXODROME)
        .withEsriJson("esri json 2")
        .withRingNumber(1)
        .withDisplayOrder(2)
        .build();

    var expected = List.of(expectedLine1, expectedLine2);

    assertThat(result).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void migrateFeature_whenNoParentShape_thenMigrateWithoutParent() {
    var oracleShape = OracleShapeTestUtil.newBuilder()
        .withShapeSidId(1)
        .withShapeName("Test Shape")
        .withShapeSrs("ED 50")
        .build();

    var entityBackedShape = new EntityBackedOracleShape(oracleShape, Map.of(), Map.of());
    when(oracleService.getAttributeMapForSiIdAndLevel(oracleShape.getShapeSidId(), AttributeLevel.SHAPE))
        .thenReturn(new HashMap<>());
    when(oracleService.getLinkedParentShapeSiIds(oracleShape.getShapeSiId())).thenReturn(List.of());

    var result = migrationService.migrateFeature(entityBackedShape);

    var expected = FeatureTestUtil.newBuilder()
        .withLegacyId(1)
        .withFeatureName("Test Shape")
        .withCoordinateSystem(CoordinateSystem.ED50)
        .withAttributes(Map.of("LAYER", Layer.SUBAREAS))
        .withParentFeature(null)
        .withFeatureArea(BigDecimal.ZERO)
        .withStartDate(oracleShape.getShapeStartDate())
        .withEndDate(oracleShape.getShapeEndDate())
        .build();

    assertThat(result).usingRecursiveComparison().ignoringFields("id").isEqualTo(expected);
  }

  @Test
  void migrateFeature_whenParentShape_thenMigrateWithParent() {
    var parentFeature = FeatureTestUtil.newBuilder()
        .withFeatureName("Parent Feature")
        .build();

    var oracleShape = OracleShapeTestUtil.newBuilder()
        .withShapeSidId(2)
        .withShapeSiId(2)
        .withShapeName("Child Shape")
        .withShapeSrs("OSGB NATIONAL GRID")
        .withOracleLayer(OracleLayerTestUtil.newBuilder().withLayer(Layer.BLOCKS).build())
        .build();

    var entityBackedShape = new EntityBackedOracleShape(oracleShape, Map.of(), Map.of());
    when(oracleService.getAttributeMapForSiIdAndLevel(oracleShape.getShapeSidId(), AttributeLevel.SHAPE))
        .thenReturn(new HashMap<>());
    when(oracleService.getLinkedParentShapeSiIds(oracleShape.getShapeSiId())).thenReturn(List.of(99));
    when(featureService.getByLegacyId(99)).thenReturn(parentFeature);

    var result = migrationService.migrateFeature(entityBackedShape);

    var expected = FeatureTestUtil.newBuilder()
        .withLegacyId(2)
        .withFeatureName("Child Shape")
        .withCoordinateSystem(CoordinateSystem.BRITISH_NATIONAL_GRID)
        .withAttributes(Map.of("LAYER", Layer.BLOCKS))
        .withParentFeature(parentFeature)
        .withFeatureArea(BigDecimal.ZERO)
        .withStartDate(oracleShape.getShapeStartDate())
        .withEndDate(oracleShape.getShapeEndDate())
        .build();

    assertThat(result).usingRecursiveComparison().ignoringFields("id").isEqualTo(expected);
  }

  @ParameterizedTest
  @EnumSource(value = Layer.class, mode = EnumSource.Mode.EXCLUDE, names = {"SUBAREAS", "RETENTION_AREAS", "BLOCKS"})
  void migrateFeature_whenReferenceBlockLayer_thenParentNotLinked(Layer refBlockLayer) {
    var oracleShape = OracleShapeTestUtil.newBuilder()
        .withShapeSidId(7)
        .withShapeSiId(7)
        .withShapeName("Ref Block Shape")
        .withShapeSrs("ED 50")
        .withOracleLayer(OracleLayerTestUtil.newBuilder().withLayer(refBlockLayer).build())
        .build();

    var entityBackedShape = new EntityBackedOracleShape(oracleShape, Map.of(), Map.of());
    when(oracleService.getAttributeMapForSiIdAndLevel(oracleShape.getShapeSidId(), AttributeLevel.SHAPE))
        .thenReturn(new HashMap<>());

    var result = migrationService.migrateFeature(entityBackedShape);

    var expected = FeatureTestUtil.newBuilder()
        .withId(null)
        .withLegacyId(7)
        .withFeatureName("Ref Block Shape")
        .withCoordinateSystem(CoordinateSystem.ED50)
        .withAttributes(Map.of("LAYER", refBlockLayer))
        .withParentFeature(null)
        .withFeatureArea(BigDecimal.ZERO)
        .withStartDate(oracleShape.getShapeStartDate())
        .withEndDate(oracleShape.getShapeEndDate())
        .build();

    assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    verify(oracleService, never()).getLinkedParentShapeSiIds(any());
    verify(featureService, never()).getByLegacyId(any());
  }

  @Test
  void migrateFeature_whenLinkedParentIsSelf_thenParentNotLinked() {
    var oracleShape = OracleShapeTestUtil.newBuilder()
        .withShapeSidId(5)
        .withShapeSiId(5)
        .withShapeName("Self Linked Shape")
        .withShapeSrs("ED 50")
        .withOracleLayer(OracleLayerTestUtil.newBuilder().withLayer(Layer.BLOCKS).build())
        .build();

    var entityBackedShape = new EntityBackedOracleShape(oracleShape, Map.of(), Map.of());
    when(oracleService.getAttributeMapForSiIdAndLevel(oracleShape.getShapeSidId(), AttributeLevel.SHAPE))
        .thenReturn(new HashMap<>());
    when(oracleService.getLinkedParentShapeSiIds(oracleShape.getShapeSiId())).thenReturn(List.of(99, 5));

    var result = migrationService.migrateFeature(entityBackedShape);

    var expected = FeatureTestUtil.newBuilder()
        .withId(null)
        .withLegacyId(5)
        .withFeatureName("Self Linked Shape")
        .withCoordinateSystem(CoordinateSystem.ED50)
        .withAttributes(Map.of("LAYER", Layer.BLOCKS))
        .withParentFeature(null)
        .withFeatureArea(BigDecimal.ZERO)
        .withStartDate(oracleShape.getShapeStartDate())
        .withEndDate(oracleShape.getShapeEndDate())
        .build();

    assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    verify(featureService, never()).getByLegacyId(any());
  }

  @Test
  void migrateFeature_whenUnknownCoordinateSystem_thenThrow() {
    var oracleShape = OracleShapeTestUtil.newBuilder()
        .withShapeSrs("UNKNOWN SRS")
        .build();

    var entityBackedShape = new EntityBackedOracleShape(oracleShape, Map.of(), Map.of());

    assertThatThrownBy(() -> migrationService.migrateFeature(entityBackedShape))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Unknown oracle coordinate system: UNKNOWN SRS");
  }

  @Test
  void combineAttributeMaps() {
    var logAppender = getLogAppender(MigrationService.class);

    Map<String, Object> result;
    try {
      result = migrationService.combineAttributeMaps(
          Map.of("SHARED_KEY", "boundary value"),
          Map.of("SHARED_KEY", "line value")
      );
    } finally {
      detachLogAppender(MigrationService.class, logAppender);
    }

    assertThat(result).containsExactlyInAnyOrderEntriesOf(Map.of(
        "POLYGON_BOUNDARY_SHARED_KEY", "boundary value",
        "BOUNDARY_LINE_SHARED_KEY", "line value"
    ));
    assertThat(logAppender.list)
        .extracting(ILoggingEvent::getLevel, ILoggingEvent::getFormattedMessage)
        .containsExactly(tuple(
            Level.WARN,
            "Duplicate attribute keys found while combining POLYGON_BOUNDARY and BOUNDARY_LINE attribute maps: [SHARED_KEY]"
        ));
  }

  @Test
  void renumberLinesAndCheckDifference() {
    var feature = FeatureTestUtil.newBuilder()
        .build();
    var polygon = PolygonTestUtil.newBuilder()
        .withFeature(feature)
        .build();
    var line1 = LineTestUtil.newBuilder()
        .withLegacyId(100)
        .withPolygon(polygon)
        .withDisplayOrder(1)
        .build();
    var line2 = LineTestUtil.newBuilder()
        .withLegacyId(200)
        .withPolygon(polygon)
        .withDisplayOrder(2)
        .build();
    var lines = List.of(line1, line2);

    doAnswer(invocation -> {
      List<Line> input = invocation.getArgument(0);
      var firstLine = input.get(0);
      var secondLine = input.get(1);

      firstLine.setDisplayOrder(2);
      secondLine.setDisplayOrder(1);
      return null;
    }).when(operatorResultProcessingService).numberLines(lines);

    var logAppender = getLogAppender(MigrationService.class);

    List<Line> result;
    try {
      result = migrationService.renumberLinesAndCheckDifference(lines);
    } finally {
      detachLogAppender(MigrationService.class, logAppender);
    }

    verify(operatorResultProcessingService).numberLines(lines);
    assertThat(result)
        .extracting(
            Line::getLegacyId,
            Line::getDisplayOrder
        )
        .containsExactly(
            tuple(100, 2),
            tuple(200, 1)
        );
    assertThat(logAppender.list)
        .extracting(ILoggingEvent::getLevel, ILoggingEvent::getFormattedMessage)
        .containsExactly(tuple(
            Level.INFO,
            "Line numbering for shape %s was updated: 100: 1 -> 2, 200: 2 -> 1".formatted(feature.getLegacyId())
        ));
  }

  private static Stream<Arguments> depthToExpectedValue() {
    return Stream.of(
        Arguments.of(500L, 500L),
        Arguments.of(999999999L, null),
        Arguments.of(-999999999L, null)
    );
  }
}