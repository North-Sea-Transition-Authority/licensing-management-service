package uk.co.fivium.gisframework.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.fivium.gisframework.feature.FeatureService;
import uk.co.fivium.gisframework.feature.FeatureTestUtil;
import uk.co.fivium.gisframework.feature.LineTestUtil;
import uk.co.fivium.gisframework.feature.Polygon;
import uk.co.fivium.gisframework.feature.PolygonTestUtil;
import uk.co.fivium.gisframework.grpc.GrpcClientService;
import uk.co.fivium.gisframework.migration.oracle.EntityBackedOracleShape;
import uk.co.fivium.gisframework.migration.oracle.OracleBoundaryLineTestUtil;
import uk.co.fivium.gisframework.migration.oracle.OraclePolygonBoundaryTestUtil;
import uk.co.fivium.gisframework.migration.oracle.OracleShapeTestUtil;
import uk.co.fivium.gisframework.migration.oracle.ShapeType;
import uk.co.fivium.grpc.gis.CoordinateSystem;
import uk.co.fivium.grpc.gis.LineNavigationType;

@ExtendWith(MockitoExtension.class)
class MigrationServiceTest {

  @Mock
  private FeatureService featureService;

  @Mock
  private GrpcClientService grpcClientService;

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
        .withBoundarySidId(10L)
        .build();

    var boundary2 = OraclePolygonBoundaryTestUtil.newBuilder()
        .withBoundarySidId(20L)
        .build();

    var oracleLine1 = OracleBoundaryLineTestUtil.newBuilder()
        .withLineSidId(100L)
        .withConnectionOrder(1L)
        .withLineNavigationType(LineNavigationType.GEODESIC)
        .build();

    var oracleLine2 = OracleBoundaryLineTestUtil.newBuilder()
        .withLineSidId(200L)
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

    when(grpcClientService.migrateBlockOrSubarea(any(), eq(CoordinateSystem.ED50), any()))
        .thenReturn(migrationResponse);

    var result = migrationService.migrateLines(feature, polygon, oracleBoundaries, entityBackedShape, List.of());

    var expectedLine1 = LineTestUtil.newBuilder()
        .withId(null)
        .withLegacyId(100)
        .withAttributes(Map.of())
        .withPolygon(polygon)
        .withNavigationType(LineNavigationType.GEODESIC)
        .withEsriJson("esri json 1")
        .withRingNumber(0)
        .withRingConnectionOrder(1)
        .build();

    var expectedLine2 = LineTestUtil.newBuilder()
        .withId(null)
        .withLegacyId(200)
        .withAttributes(Map.of())
        .withPolygon(polygon)
        .withNavigationType(LineNavigationType.LOXODROME)
        .withEsriJson("esri json 2")
        .withRingNumber(1)
        .withRingConnectionOrder(2)
        .build();

    var expected = List.of(expectedLine1, expectedLine2);

    assertThat(result).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void migrateFeature_whenNoParentShape_thenMigrateWithoutParent() {
    var oracleShape = OracleShapeTestUtil.newBuilder()
        .withShapeSidId(1)
        .withShapeName("Test Shape")
        .withTestCase("Test case 1")
        .withShapeSrs("ED 50")
        .withShapeType(ShapeType.SUBAREA)
        .withParentShapeId(null)
        .build();

    var entityBackedShape = new EntityBackedOracleShape(oracleShape, Map.of(), Map.of());

    var result = migrationService.migrateFeature(entityBackedShape);

    var expected = FeatureTestUtil.newBuilder()
        .withLegacyId(1)
        .withFeatureName("Test Shape")
        .withTestCase("Test case 1")
        .withCoordinateSystem(CoordinateSystem.ED50)
        .withAttributes(Map.of("SHAPE_TYPE", "SUBAREA"))
        .withParentFeature(null)
        .withFeatureArea(BigDecimal.ZERO)
        .build();

    assertThat(result).usingRecursiveComparison().ignoringFields("id").isEqualTo(expected);
  }

  @Test
  void migrateFeature_whenParentShape_thenMigrateWithParent() {
    var parentFeature = FeatureTestUtil.newBuilder()
        .withFeatureName("Parent Feature")
        .build();

    when(featureService.getByLegacyId(99)).thenReturn(parentFeature);

    var oracleShape = OracleShapeTestUtil.newBuilder()
        .withShapeSidId(2)
        .withShapeName("Child Shape")
        .withTestCase("Test case 2")
        .withShapeSrs("OSGB NATIONAL GRID")
        .withShapeType(ShapeType.BLOCK)
        .withParentShapeId(99)
        .build();

    var entityBackedShape = new EntityBackedOracleShape(oracleShape, Map.of(), Map.of());

    var result = migrationService.migrateFeature(entityBackedShape);

    var expected = FeatureTestUtil.newBuilder()
        .withLegacyId(2)
        .withFeatureName("Child Shape")
        .withTestCase("Test case 2")
        .withCoordinateSystem(CoordinateSystem.BRITISH_NATIONAL_GRID)
        .withAttributes(Map.of("SHAPE_TYPE", "BLOCK"))
        .withParentFeature(parentFeature)
        .withFeatureArea(BigDecimal.ZERO)
        .build();

    assertThat(result).usingRecursiveComparison().ignoringFields("id").isEqualTo(expected);
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

  private static Stream<Arguments> depthToExpectedValue() {
    return Stream.of(
        Arguments.of(500L, 500L),
        Arguments.of(999999999L, null),
        Arguments.of(-999999999L, null)
    );
  }
}