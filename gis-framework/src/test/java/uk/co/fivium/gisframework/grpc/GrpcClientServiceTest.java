package uk.co.fivium.gisframework.grpc;

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
import uk.co.fivium.gisframework.migration.MigrationResponseDto;
import uk.co.fivium.gisframework.migration.oracle.OracleBoundaryLineTestUtil;
import uk.co.fivium.gisframework.migration.oracle.OracleBoundaryLineWithRing;
import uk.co.fivium.gisframework.feature.CoordinateSystemUtils;
import uk.co.fivium.grpc.gis.ArcGisServiceGrpc;
import uk.co.fivium.grpc.gis.BuildPolygonRequest;
import uk.co.fivium.grpc.gis.BuildPolygonResponse;
import uk.co.fivium.grpc.gis.CoordinateSystem;
import uk.co.fivium.grpc.gis.EsriJsonPolylineAndOracleId;
import uk.co.fivium.grpc.gis.GeoJsonLineWrapper;
import uk.co.fivium.grpc.gis.LineNavigationType;
import uk.co.fivium.grpc.gis.MigrateBlockOrSubAreaRequest;
import uk.co.fivium.grpc.gis.MigrateBlockOrSubAreaResponse;
import uk.co.fivium.grpc.gis.SplitPolygonRequest;
import uk.co.fivium.grpc.gis.SplitPolygonResponse;

@ExtendWith(MockitoExtension.class)
class GrpcClientServiceTest {

  @Mock
  private ArcGisServiceGrpc.ArcGisServiceBlockingStub arcgisClient;

  @InjectMocks
  private GrpcClientService grpcClientService;

  @Test
  void splitPolygons_verifyServiceClientCall() {
    var esriJsonPolygon = "dummy esriJson polygon";
    var esriJsonCutterLine = "dummy esriJson cutter line";

    var splitResult = "dummy esriJson split result";
    var expectedRequest = SplitPolygonRequest.newBuilder()
        .setEsriJsonPolygonTarget(esriJsonPolygon)
        .setEsriJsonLineCutter(esriJsonCutterLine)
        .build();
    var expectedResponse = SplitPolygonResponse.newBuilder()
        .addOutputPolygonEsriJsons(splitResult)
        .build();

    when(arcgisClient.splitPolygon(expectedRequest)).thenReturn(expectedResponse);
    assertThat(grpcClientService.splitPolygon(esriJsonPolygon, esriJsonCutterLine)).containsExactly(splitResult);
  }

  @Test
  void buildPolygon_verifyServiceClientCall() {
    var esriJsonPolylines = List.of(
        "dummy esriJson polyline 1",
        "dummy esriJson polyline 2"
    );
    var coordinateSystem = CoordinateSystem.ED50;

    var builtPolygon = "dummy esriJson polygon";
    var expectedRequest = BuildPolygonRequest.newBuilder()
        .addAllEsriJsonPolylines(esriJsonPolylines)
        .setCoordinateSystemWkid(CoordinateSystemUtils.getWkid(coordinateSystem))
        .build();
    var expectedResponse = BuildPolygonResponse.newBuilder()
        .setPolygonEsriJson(builtPolygon)
        .build();

    when(arcgisClient.buildPolygon(expectedRequest)).thenReturn(expectedResponse);
    assertThat(grpcClientService.buildPolygon(esriJsonPolylines, coordinateSystem)).isEqualTo(builtPolygon);
  }


  @Test
  void migrateBlockOrSubarea() {
    var geodesicOracleLine = new OracleBoundaryLineWithRing(
        OracleBoundaryLineTestUtil.newBuilder()
            .withLineGeojson("some json 1")
            .withLineNavigationType(LineNavigationType.GEODESIC)
            .withConnectionOrder(1L)
            .withLineSidId(10L)
            .build(),
        100
    );
    var loxodromeOracleLine = new OracleBoundaryLineWithRing(
        OracleBoundaryLineTestUtil.newBuilder()
            .withLineGeojson("some json 2")
            .withLineNavigationType(LineNavigationType.LOXODROME)
            .withConnectionOrder(2L)
            .withLineSidId(20L)
            .build(),
        200
    );


    var linesWithRing = List.of(geodesicOracleLine, loxodromeOracleLine);
    var coordinateSystem = CoordinateSystem.ED50;
    var parentLines = List.of("parent esri json line 1", "parent esri json line 2");

    var expectedRequest = MigrateBlockOrSubAreaRequest.newBuilder()
        .setCoordinateSystem(coordinateSystem)
        .addAllParentLineEsriJsonStrings(parentLines);
    expectedRequest.addGeoJsonLineWrappers(GeoJsonLineWrapper.newBuilder()
        .setGeoJsonString("some json 1")
        .setIsGeodesic(true)
        .setConnectionOrder(1)
        .setOracleLineSsid(10)
        .setRingNumber(100)
        .build());
    expectedRequest.addGeoJsonLineWrappers(GeoJsonLineWrapper.newBuilder()
        .setGeoJsonString("some json 2")
        .setIsGeodesic(false)
        .setConnectionOrder(2)
        .setOracleLineSsid(20)
        .setRingNumber(200)
        .build());

    var response = MigrateBlockOrSubAreaResponse.newBuilder()
        .addEsriJsonLineAndOracleIds(EsriJsonPolylineAndOracleId.newBuilder()
            .setEsriJsonString("some new json 1")
            .setOracleLineSsid(1)
            .build())
        .addEsriJsonLineAndOracleIds(EsriJsonPolylineAndOracleId.newBuilder()
            .setEsriJsonString("some new json 2")
            .setOracleLineSsid(2)
            .build())
        .setArea(1000)
        .build();
    when(arcgisClient.migrateBlockOrSubarea(expectedRequest.build())).thenReturn(response);

    assertThat(grpcClientService.migrateBlockOrSubarea(linesWithRing, coordinateSystem, parentLines))
        .isEqualTo(new MigrationResponseDto(
            Map.of(1, "some new json 1", 2, "some new json 2"),
            1000.0
        ));

    verify(arcgisClient).migrateBlockOrSubarea(expectedRequest.build());
  }
}