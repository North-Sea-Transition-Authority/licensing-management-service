package uk.co.fivium.gisframework.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.gisframework.feature.FeatureTestUtil;
import uk.co.fivium.gisframework.feature.LineTestUtil;
import uk.co.fivium.gisframework.feature.PolygonTestUtil;
import uk.co.fivium.gisframework.grpc.GrpcClientService;
import uk.co.fivium.gisframework.migration.oracle.EntityBackedOracleShape;
import uk.co.fivium.gisframework.migration.oracle.OracleBoundaryLineTestUtil;
import uk.co.fivium.gisframework.migration.oracle.OracleBoundaryLineWithRing;
import uk.co.fivium.gisframework.migration.oracle.OraclePolygonBoundaryTestUtil;
import uk.co.fivium.gisframework.migration.oracle.OracleShapeTestUtil;
import uk.co.fivium.grpc.gis.CoordinateSystem;
import uk.co.fivium.grpc.gis.LineNavigationType;

@ExtendWith(MockitoExtension.class)
class ReferenceBlockMigrationServiceTest {

  @Mock
  private GrpcClientService grpcClientService;

  @InjectMocks
  private ReferenceBlockMigrationService referenceBlockMigrationService;

  @Test
  void migrateRefBlockLines() {
    var feature = FeatureTestUtil.newBuilder()
        .withCoordinateSystem(CoordinateSystem.ED50)
        .build();

    var polygon = PolygonTestUtil.newBuilder()
        .withFeature(feature)
        .build();

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
    var skippedOracleLine = OracleBoundaryLineTestUtil.newBuilder()
        .withLineSidId(300)
        .withOraclePolygonBoundaryId(boundary2.getBoundarySidId())
        .withConnectionOrder(3L)
        .withLineNavigationType(LineNavigationType.GEODESIC)
        .build();

    var entityBackedShape = new EntityBackedOracleShape(
        OracleShapeTestUtil.newBuilder().build(),
        Map.of(),
        Map.of(
            boundary1, List.of(oracleLine1),
            boundary2, List.of(oracleLine2, skippedOracleLine)
        )
    );

    var licenseLine = LineTestUtil.newBuilder()
        .withEsriJson("license esri json")
        .withNavigationType(LineNavigationType.GEODESIC)
        .build();

    var expectedLinesWithRing = List.of(
        new OracleBoundaryLineWithRing(oracleLine1, 0),
        new OracleBoundaryLineWithRing(oracleLine2, 1),
        new OracleBoundaryLineWithRing(skippedOracleLine, 1)
    );

    when(grpcClientService.migrateReferenceBlock(expectedLinesWithRing, CoordinateSystem.ED50, List.of(licenseLine)))
        .thenReturn(Map.of(
            100, "esri json 1",
            200, "esri json 2"
        ));

    var result = referenceBlockMigrationService.migrateRefBlockLines(
        feature,
        polygon,
        List.of(boundary1, boundary2),
        entityBackedShape,
        List.of(licenseLine)
    );

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

    assertThat(result).usingRecursiveComparison().isEqualTo(List.of(expectedLine1, expectedLine2));

    verify(grpcClientService).migrateReferenceBlock(expectedLinesWithRing, CoordinateSystem.ED50, List.of(licenseLine));
  }
}
