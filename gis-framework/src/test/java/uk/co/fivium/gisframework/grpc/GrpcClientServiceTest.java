package uk.co.fivium.gisframework.grpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.esri.core.geometry.Point;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.gisframework.feature.CoordinateSystemUtils;
import uk.co.fivium.gisframework.feature.EntityBackedFeature;
import uk.co.fivium.gisframework.feature.FeatureTestUtil;
import uk.co.fivium.gisframework.feature.Line;
import uk.co.fivium.gisframework.feature.LineTestUtil;
import uk.co.fivium.gisframework.feature.PolygonTestUtil;
import uk.co.fivium.gisframework.migration.MigrationResponseDto;
import uk.co.fivium.gisframework.migration.oracle.OracleBoundaryLineTestUtil;
import uk.co.fivium.gisframework.migration.oracle.OracleBoundaryLineWithRing;
import uk.co.fivium.gisframework.operator.LineWithStartEndPoints;
import uk.co.fivium.grpc.gis.ArcGisServiceGrpc;
import uk.co.fivium.grpc.gis.BlockAndSubareaValidationRequest;
import uk.co.fivium.grpc.gis.BuildPolygonRequest;
import uk.co.fivium.grpc.gis.BuildPolygonResponse;
import uk.co.fivium.grpc.gis.CalculateAreaRequest;
import uk.co.fivium.grpc.gis.CalculateAreaResponse;
import uk.co.fivium.grpc.gis.ChildLineMatch;
import uk.co.fivium.grpc.gis.Coordinate;
import uk.co.fivium.grpc.gis.CoordinateSystem;
import uk.co.fivium.grpc.gis.EsriJsonLineWithNavigationAndId;
import uk.co.fivium.grpc.gis.EsriJsonPolygonLineWrappers;
import uk.co.fivium.grpc.gis.EsriJsonPolygonLines;
import uk.co.fivium.grpc.gis.EsriJsonPolylineAndOracleId;
import uk.co.fivium.grpc.gis.ExplodePolygonRequest;
import uk.co.fivium.grpc.gis.ExplodePolygonResponse;
import uk.co.fivium.grpc.gis.FindNorthwestMostLineRequest;
import uk.co.fivium.grpc.gis.FindNorthwestMostLineResponse;
import uk.co.fivium.grpc.gis.FindParentLinesRequest;
import uk.co.fivium.grpc.gis.FindParentLinesResponse;
import uk.co.fivium.grpc.gis.GeoJsonLineWrapper;
import uk.co.fivium.grpc.gis.GetLineStartAndEndPointsRequest;
import uk.co.fivium.grpc.gis.GetLineStartAndEndPointsResponse;
import uk.co.fivium.grpc.gis.LineNavigationType;
import uk.co.fivium.grpc.gis.LineWithId;
import uk.co.fivium.grpc.gis.LineWithNavigationType;
import uk.co.fivium.grpc.gis.LineWithStartAndEndPoint;
import uk.co.fivium.grpc.gis.MigrateBlockOrSubAreaRequest;
import uk.co.fivium.grpc.gis.MigrateBlockOrSubAreaResponse;
import uk.co.fivium.grpc.gis.MigrateReferenceBlockRequest;
import uk.co.fivium.grpc.gis.MigrateReferenceBlockResponse;
import uk.co.fivium.grpc.gis.OrderedLineSegment;
import uk.co.fivium.grpc.gis.ParentLine;
import uk.co.fivium.grpc.gis.ReferenceBlockValidationRequest;
import uk.co.fivium.grpc.gis.SplitPolygonRequest;
import uk.co.fivium.grpc.gis.SplitPolygonResponse;
import uk.co.fivium.grpc.gis.TopologicallyEqualValidationRequest;
import uk.co.fivium.grpc.gis.ValidatePolygonReconstructionFromPolylinesRequest;
import uk.co.fivium.grpc.gis.ValidatePolygonReconstructionFromPolylinesResponse;
import uk.co.fivium.grpc.gis.ValidationResponse;

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
        .setProjectToWgs84(true)
        .build();
    var expectedResponse = BuildPolygonResponse.newBuilder()
        .setPolygonEsriJson(builtPolygon)
        .build();

    when(arcgisClient.buildPolygon(expectedRequest)).thenReturn(expectedResponse);
    assertThat(grpcClientService.buildPolygon(esriJsonPolylines, coordinateSystem, true)).isEqualTo(builtPolygon);
  }

  @Test
  void explodePolygon_verifyServiceClientCall() {
    var esriJsonPolygon = "dummy esriJson polygon";

    var esriJsonLine1 = "dummy esriJson line 1";
    var esriJsonLine2 = "dummy esriJson line 2";
    var expectedRequest = ExplodePolygonRequest.newBuilder()
        .setEsriJsonPolygon(esriJsonPolygon)
        .build();
    var expectedResponse = ExplodePolygonResponse.newBuilder()
        .addAllEsriJsonLines(List.of(esriJsonLine1, esriJsonLine2))
        .build();

    when(arcgisClient.explodePolygon(expectedRequest)).thenReturn(expectedResponse);
    assertThat(grpcClientService.explodePolygon(esriJsonPolygon)).containsExactly(esriJsonLine1, esriJsonLine2);
  }

  @Test
  void findParentLines_verifyServiceClientCall() {
    var parentLineId1 = UUID.randomUUID();
    var parentLineId2 = UUID.randomUUID();
    var parentLineEsriJson1 = "dummy parent line esriJson 1";
    var parentLineEsriJson2 = "dummy parent line esriJson 2";
    var parentLine1 = LineTestUtil.newBuilder().withId(parentLineId1).withEsriJson(parentLineEsriJson1).build();
    var parentLine2 = LineTestUtil.newBuilder().withId(parentLineId2).withEsriJson(parentLineEsriJson2).build();

    var childLineEsriJson1 = "dummy child line esriJson 1";
    var childLineEsriJson2 = "dummy child line esriJson 2";
    var orphanLineEsriJson = "dummy orphan line esriJson";
    var childLineEsriJson = List.of(childLineEsriJson1, childLineEsriJson2, orphanLineEsriJson);

    var expectedRequest = FindParentLinesRequest.newBuilder()
        .addParentLines(ParentLine.newBuilder()
            .setId(parentLineId1.toString())
            .setEsriJsonPolyline(parentLineEsriJson1)
            .build())
        .addParentLines(ParentLine.newBuilder()
            .setId(parentLineId2.toString())
            .setEsriJsonPolyline(parentLineEsriJson2)
            .build())
        .addAllChildrenEsriJsonPolylines(childLineEsriJson)
        .build();
    var expectedResponse = FindParentLinesResponse.newBuilder()
        .addLinesWithParentMatch(ChildLineMatch.newBuilder()
            .setParentId(parentLineId1.toString())
            .setChildEsriJsonPolyline(childLineEsriJson1)
            .build())
        .addLinesWithParentMatch(ChildLineMatch.newBuilder()
            .setParentId(parentLineId2.toString())
            .setChildEsriJsonPolyline(childLineEsriJson2)
            .build())
        .addOrphanedChildrenEsriJsonPolylines(orphanLineEsriJson)
        .build();

    when(arcgisClient.findParentLines(expectedRequest)).thenReturn(expectedResponse);
    assertThat(grpcClientService.findParentLines(List.of(parentLine1, parentLine2), childLineEsriJson))
        .isEqualTo(new FindParentLineResponse(
            Map.of(
                childLineEsriJson1, parentLineId1,
                childLineEsriJson2, parentLineId2
            ),
            List.of(orphanLineEsriJson)
        ));
  }

  @Test
  void validatePolygonReconstructionFromPolylines_verifyServiceClientCall() {
    var line1 = LineTestUtil.newBuilder()
        .withEsriJson("dummy esriJson line 1")
        .withRingNumber(1)
        .withRingConnectionOrder(1)
        .build();
    var line2 = LineTestUtil.newBuilder()
        .withEsriJson("dummy esriJson line 2")
        .withRingNumber(2)
        .withRingConnectionOrder(3)
        .build();
    var originalPolygonEsriJson = "dummy original polygon esriJson";

    var expectedRequest = ValidatePolygonReconstructionFromPolylinesRequest.newBuilder()
        .addLines(OrderedLineSegment.newBuilder()
            .setEsriJsonPolyline(line1.getEsriJson())
            .setRingNumber(line1.getRingNumber())
            .setConnectionOrder(line1.getRingConnectionOrder())
            .build())
        .addLines(OrderedLineSegment.newBuilder()
            .setEsriJsonPolyline(line2.getEsriJson())
            .setRingNumber(line2.getRingNumber())
            .setConnectionOrder(line2.getRingConnectionOrder())
            .build())
        .setOriginalPolygonEsriJson(originalPolygonEsriJson)
        .build();
    var expectedResponse = ValidatePolygonReconstructionFromPolylinesResponse.newBuilder()
        .setIsValid(true)
        .build();

    when(arcgisClient.validatePolygonReconstructionFromPolylines(expectedRequest)).thenReturn(expectedResponse);
    assertThat(grpcClientService
        .validatePolygonReconstructionFromPolylines(List.of(line1, line2), originalPolygonEsriJson)).isTrue();
  }

  @Test
  void migrateBlockOrSubarea() {
    var geodesicOracleLine = new OracleBoundaryLineWithRing(
        OracleBoundaryLineTestUtil.newBuilder()
            .withLineGeojson("some json 1")
            .withLineNavigationType(LineNavigationType.GEODESIC)
            .withConnectionOrder(1L)
            .withLineSidId(10)
            .build(),
        100
    );
    var loxodromeOracleLine = new OracleBoundaryLineWithRing(
        OracleBoundaryLineTestUtil.newBuilder()
            .withLineGeojson("some json 2")
            .withLineNavigationType(LineNavigationType.LOXODROME)
            .withConnectionOrder(2L)
            .withLineSidId(20)
            .build(),
        200
    );


    var linesWithRing = List.of(geodesicOracleLine, loxodromeOracleLine);
    var coordinateSystem = CoordinateSystem.ED50;
    var parentLines = List.of("parent esri json line 1", "parent esri json line 2");
    var childShapeId = 555;

    var expectedRequest = MigrateBlockOrSubAreaRequest.newBuilder()
        .setCoordinateSystem(coordinateSystem)
        .addAllParentLineEsriJsonStrings(parentLines)
        .setShapeId(String.valueOf(childShapeId));
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

    assertThat(grpcClientService.migrateBlockOrSubarea(linesWithRing, coordinateSystem, parentLines, childShapeId))
        .isEqualTo(new MigrationResponseDto(
            Map.of(1, "some new json 1", 2, "some new json 2"),
            1000.0
        ));

    verify(arcgisClient).migrateBlockOrSubarea(expectedRequest.build());
  }

  @Test
  void migrateReferenceBlock() {
    var geodesicOracleLine = new OracleBoundaryLineWithRing(
        OracleBoundaryLineTestUtil.newBuilder()
            .withLineGeojson("some json 1")
            .withLineNavigationType(LineNavigationType.GEODESIC)
            .withConnectionOrder(1L)
            .withLineSidId(10)
            .build(),
        100
    );
    var loxodromeOracleLine = new OracleBoundaryLineWithRing(
        OracleBoundaryLineTestUtil.newBuilder()
            .withLineGeojson("some json 2")
            .withLineNavigationType(LineNavigationType.LOXODROME)
            .withConnectionOrder(2L)
            .withLineSidId(20)
            .build(),
        200
    );
    var licenseBlockLine = LineTestUtil.newBuilder()
        .withEsriJson("license esri json")
        .withNavigationType(LineNavigationType.GEODESIC)
        .build();

    var expectedRequest = MigrateReferenceBlockRequest.newBuilder()
        .setCoordinateSystem(CoordinateSystem.ED50)
        .addGeoJsonLineWrappers(GeoJsonLineWrapper.newBuilder()
            .setGeoJsonString("some json 1")
            .setIsGeodesic(true)
            .setConnectionOrder(1)
            .setOracleLineSsid(10)
            .setRingNumber(100)
            .build())
        .addGeoJsonLineWrappers(GeoJsonLineWrapper.newBuilder()
            .setGeoJsonString("some json 2")
            .setIsGeodesic(false)
            .setConnectionOrder(2)
            .setOracleLineSsid(20)
            .setRingNumber(200)
            .build())
        .addLicenseBlockLines(EsriJsonLineWithNavigationAndId.newBuilder()
            .setEsriJsonString("license esri json")
            .setNavigationType(LineNavigationType.GEODESIC)
            .build());

    var response = MigrateReferenceBlockResponse.newBuilder()
        .addEsriJsonLineWithId(EsriJsonPolylineAndOracleId.newBuilder()
            .setEsriJsonString("some new json 1")
            .setOracleLineSsid(10)
            .build())
        .addEsriJsonLineWithId(EsriJsonPolylineAndOracleId.newBuilder()
            .setEsriJsonString("some new json 2")
            .setOracleLineSsid(20)
            .build())
        .build();
    when(arcgisClient.migrateReferenceBlock(expectedRequest.build())).thenReturn(response);

    assertThat(grpcClientService.migrateReferenceBlock(
        List.of(geodesicOracleLine, loxodromeOracleLine),
        CoordinateSystem.ED50,
        List.of(licenseBlockLine)
    )).isEqualTo(Map.of(10, "some new json 1", 20, "some new json 2"));

    verify(arcgisClient).migrateReferenceBlock(expectedRequest.build());
  }

  @Test
  void validateBlockAndSubarea() {
    var feature = FeatureTestUtil.newBuilder()
        .withCoordinateSystem(CoordinateSystem.ED50)
        .build();

    var childPolygon = PolygonTestUtil.newBuilder().withFeature(feature).build();
    var childLine = LineTestUtil.newBuilder()
        .withPolygon(childPolygon)
        .withEsriJson("child esri json")
        .withNavigationType(LineNavigationType.GEODESIC)
        .withLegacyId(10)
        .build();
    var childFeature = new EntityBackedFeature(feature, Map.of(childPolygon, List.of(childLine)));

    var parentPolygon = PolygonTestUtil.newBuilder().withFeature(feature).build();
    var parentLine = LineTestUtil.newBuilder()
        .withPolygon(parentPolygon)
        .withEsriJson("parent esri json")
        .withNavigationType(LineNavigationType.LOXODROME)
        .withLegacyId(20)
        .build();
    var parentFeature = new EntityBackedFeature(feature, Map.of(parentPolygon, List.of(parentLine)));

    var expectedRequest = BlockAndSubareaValidationRequest.newBuilder()
        .setCoordinateSystem(CoordinateSystem.ED50)
        .addChildPolygonLineWrappersLists(EsriJsonPolygonLineWrappers.newBuilder()
            .addLineWrappers(EsriJsonLineWithNavigationAndId.newBuilder()
                .setEsriJsonString("child esri json")
                .setNavigationType(LineNavigationType.GEODESIC)
                .setOracleLineSsid(10)
                .build()))
        .addParentPolygonLineWrappersLists(EsriJsonPolygonLineWrappers.newBuilder()
            .addLineWrappers(EsriJsonLineWithNavigationAndId.newBuilder()
                .setEsriJsonString("parent esri json")
                .setNavigationType(LineNavigationType.LOXODROME)
                .setOracleLineSsid(20)
                .build()));

    var response = ValidationResponse.newBuilder()
        .setIsValid(true)
        .setMessage("Valid")
        .build();
    when(arcgisClient.validateBlockAndSubarea(expectedRequest.build())).thenReturn(response);

    assertThat(grpcClientService.validateBlockAndSubarea(childFeature, parentFeature)).isEqualTo(response);

    verify(arcgisClient).validateBlockAndSubarea(expectedRequest.build());
  }

  @Test
  void validateBlockAndSubarea_withMultipleParents_addsEveryParentToRequest() {
    var feature = FeatureTestUtil.newBuilder()
        .withCoordinateSystem(CoordinateSystem.ED50)
        .build();

    var childPolygon = PolygonTestUtil.newBuilder().withFeature(feature).build();
    var childLine = LineTestUtil.newBuilder()
        .withPolygon(childPolygon)
        .withEsriJson("child esri json")
        .withNavigationType(LineNavigationType.GEODESIC)
        .withLegacyId(10)
        .build();
    var childFeature = new EntityBackedFeature(feature, Map.of(childPolygon, List.of(childLine)));

    var parentPolygon1 = PolygonTestUtil.newBuilder().withFeature(feature).build();
    var parentLine1 = LineTestUtil.newBuilder()
        .withPolygon(parentPolygon1)
        .withEsriJson("parent esri json 1")
        .withNavigationType(LineNavigationType.LOXODROME)
        .withLegacyId(20)
        .build();
    var parentFeature1 = new EntityBackedFeature(feature, Map.of(parentPolygon1, List.of(parentLine1)));

    var parentPolygon2 = PolygonTestUtil.newBuilder().withFeature(feature).build();
    var parentLine2 = LineTestUtil.newBuilder()
        .withPolygon(parentPolygon2)
        .withEsriJson("parent esri json 2")
        .withNavigationType(LineNavigationType.GEODESIC)
        .withLegacyId(30)
        .build();
    var parentFeature2 = new EntityBackedFeature(feature, Map.of(parentPolygon2, List.of(parentLine2)));

    var expectedRequest = BlockAndSubareaValidationRequest.newBuilder()
        .setCoordinateSystem(CoordinateSystem.ED50)
        .addChildPolygonLineWrappersLists(EsriJsonPolygonLineWrappers.newBuilder()
            .addLineWrappers(EsriJsonLineWithNavigationAndId.newBuilder()
                .setEsriJsonString("child esri json")
                .setNavigationType(LineNavigationType.GEODESIC)
                .setOracleLineSsid(10)
                .build()))
        .addParentPolygonLineWrappersLists(EsriJsonPolygonLineWrappers.newBuilder()
            .addLineWrappers(EsriJsonLineWithNavigationAndId.newBuilder()
                .setEsriJsonString("parent esri json 1")
                .setNavigationType(LineNavigationType.LOXODROME)
                .setOracleLineSsid(20)
                .build()))
        .addParentPolygonLineWrappersLists(EsriJsonPolygonLineWrappers.newBuilder()
            .addLineWrappers(EsriJsonLineWithNavigationAndId.newBuilder()
                .setEsriJsonString("parent esri json 2")
                .setNavigationType(LineNavigationType.GEODESIC)
                .setOracleLineSsid(30)
                .build()));

    var response = ValidationResponse.newBuilder()
        .setIsValid(true)
        .setMessage("Valid")
        .build();
    when(arcgisClient.validateBlockAndSubarea(expectedRequest.build())).thenReturn(response);

    assertThat(grpcClientService.validateBlockAndSubarea(childFeature, List.of(parentFeature1, parentFeature2)))
        .isEqualTo(response);

    verify(arcgisClient).validateBlockAndSubarea(expectedRequest.build());
  }

  @Test
  void validateTopologicallyEqual() {
    var feature = FeatureTestUtil.newBuilder()
        .withCoordinateSystem(CoordinateSystem.ED50)
        .build();

    var parentPolygon = PolygonTestUtil.newBuilder().withFeature(feature).build();
    var parentLine = LineTestUtil.newBuilder()
        .withPolygon(parentPolygon)
        .withEsriJson("parent esri json")
        .withNavigationType(LineNavigationType.LOXODROME)
        .withLegacyId(20)
        .build();
    var parentFeature = new EntityBackedFeature(feature, Map.of(parentPolygon, List.of(parentLine)));

    var childPolygonsLines = List.of(
        List.of("child esri json line 1", "child esri json line 2"),
        List.of("child esri json line 3")
    );

    var expectedRequest = TopologicallyEqualValidationRequest.newBuilder()
        .setCoordinateSystem(CoordinateSystem.ED50)
        .addChildPolygons(EsriJsonPolygonLines.newBuilder()
            .addAllEsriJsonPolyline(List.of("child esri json line 1", "child esri json line 2"))
            .build())
        .addChildPolygons(EsriJsonPolygonLines.newBuilder()
            .addAllEsriJsonPolyline(List.of("child esri json line 3"))
            .build())
        .addParentPolygons(EsriJsonPolygonLines.newBuilder()
            .addAllEsriJsonPolyline(List.of("parent esri json"))
            .build());

    var response = ValidationResponse.newBuilder()
        .setIsValid(true)
        .setMessage("Valid")
        .build();
    when(arcgisClient.validateTopologicallyEqual(expectedRequest.build())).thenReturn(response);

    assertThat(grpcClientService.validateTopologicallyEqual(childPolygonsLines, parentFeature)).isEqualTo(response);

    verify(arcgisClient).validateTopologicallyEqual(expectedRequest.build());
  }

  @Test
  void getLineStartAndEndPoints_verifyServiceClientCall() {
    var line1 = LineTestUtil.newBuilder().withEsriJson("dummy esriJson line 1").build();
    var line2 = LineTestUtil.newBuilder().withEsriJson("dummy esriJson line 2").build();

    //lines have random IDs so we cannot create an expected request/response
    when(arcgisClient.getLineStartAndEndPoints(any(GetLineStartAndEndPointsRequest.class)))
        .thenAnswer(invocation -> {
          var request = invocation.getArgument(0, GetLineStartAndEndPointsRequest.class);
          var responseBuilder = GetLineStartAndEndPointsResponse.newBuilder();

          for (var line : request.getLinesList()) {
            if (line.getPolyLineEsriJson().equals(line1.getEsriJson())) {
              responseBuilder.addLines(LineWithStartAndEndPoint.newBuilder()
                  .setLineId(line.getId())
                  .setStartPoint(Coordinate.newBuilder().setX(1.1).setY(2.2).build())
                  .setEndPoint(Coordinate.newBuilder().setX(3.3).setY(4.4).build())
                  .build());
            } else {
              responseBuilder.addLines(LineWithStartAndEndPoint.newBuilder()
                  .setLineId(line.getId())
                  .setStartPoint(Coordinate.newBuilder().setX(5.5).setY(6.6).build())
                  .setEndPoint(Coordinate.newBuilder().setX(7.7).setY(8.8).build())
                  .build());
            }
          }

          return responseBuilder.build();
        });

    var result = grpcClientService.getLineStartAndEndPoints(List.of(line1, line2));

    assertThat(result)
        .extracting(LineWithStartEndPoints::line)
        .containsExactlyInAnyOrder(line1, line2);

    var expectedLine1Result = new LineWithStartEndPoints(line1,
        new Point(1.1, 2.2),
        new Point(3.3, 4.4));
    var expectedLine2Result = new LineWithStartEndPoints(line2,
        new com.esri.core.geometry.Point(5.5, 6.6),
        new com.esri.core.geometry.Point(7.7, 8.8));

    assertThat(result).containsExactlyInAnyOrder(expectedLine1Result, expectedLine2Result);
  }

  @Test
  void findNorthwestMostLine_verifyServiceClientCall() {
    var line1 = LineTestUtil.newBuilder().withEsriJson("dummy esriJson line 1").build();
    var line2 = LineTestUtil.newBuilder().withEsriJson("dummy esriJson line 2").build();
    var idToLine = new LinkedHashMap<UUID, Line>();
    idToLine.put(line1.getId(), line1);
    idToLine.put(line2.getId(), line2);

    var expectedRequest = FindNorthwestMostLineRequest.newBuilder()
        .addLines(LineWithId.newBuilder()
            .setId(line1.getId().toString())
            .setPolyLineEsriJson(line1.getEsriJson())
            .build())
        .addLines(LineWithId.newBuilder()
            .setId(line2.getId().toString())
            .setPolyLineEsriJson(line2.getEsriJson())
            .build())
        .build();
    var expectedResponse = FindNorthwestMostLineResponse.newBuilder()
        .setLineId(line2.getId().toString())
        .build();

    when(arcgisClient.findNorthwestMostLine(expectedRequest)).thenReturn(expectedResponse);
    assertThat(grpcClientService.findNorthwestMostLine(idToLine)).isEqualTo(line2.getId());
  }

  @Test
  void calculateArea_verifyServiceClientCall_assertAbsoluteAreaIsReturned() {
    var line1 = LineTestUtil.newBuilder()
        .withEsriJson("dummy esriJson line 1")
        .withNavigationType(LineNavigationType.LOXODROME)
        .build();
    var line2 = LineTestUtil.newBuilder()
        .withEsriJson("dummy esriJson line 2")
        .withNavigationType(LineNavigationType.GEODESIC)
        .build();
    var coordinateSystem = CoordinateSystem.ED50;

    var expectedRequest = CalculateAreaRequest.newBuilder()
        .setCoordinateSystem(coordinateSystem)
        .addLinesWithNavigationType(LineWithNavigationType.newBuilder()
            .setEsriJsonPolyline(line1.getEsriJson())
            .setLineNavigationType(line1.getNavigationType())
            .build())
        .addLinesWithNavigationType(LineWithNavigationType.newBuilder()
            .setEsriJsonPolyline(line2.getEsriJson())
            .setLineNavigationType(line2.getNavigationType())
            .build())
        .build();
    var response = CalculateAreaResponse.newBuilder()
        .setArea(123.54321)
        .build();

    when(arcgisClient.calculateArea(expectedRequest)).thenReturn(response);

    assertThat(grpcClientService.calculateArea(coordinateSystem, List.of(line1, line2)))
        .isEqualByComparingTo(BigDecimal.valueOf(123.54321));
  }

  @Test
  void validateReferenceBlock() {
    var feature = FeatureTestUtil.newBuilder()
        .withCoordinateSystem(CoordinateSystem.ED50)
        .build();

    var refBlockPolygon = PolygonTestUtil.newBuilder().withFeature(feature).build();
    var refBlockLine = LineTestUtil.newBuilder()
        .withPolygon(refBlockPolygon)
        .withEsriJson("ref block esri json")
        .withNavigationType(LineNavigationType.GEODESIC)
        .withLegacyId(10)
        .build();
    var refBlockFeature = new EntityBackedFeature(feature, Map.of(refBlockPolygon, List.of(refBlockLine)));

    var licenceBlockPolygon = PolygonTestUtil.newBuilder().withFeature(feature).build();
    var licenceBlockLine = LineTestUtil.newBuilder()
        .withPolygon(licenceBlockPolygon)
        .withEsriJson("licence block esri json")
        .withNavigationType(LineNavigationType.LOXODROME)
        .withLegacyId(20)
        .build();
    var licenceBlockFeature = new EntityBackedFeature(feature, Map.of(licenceBlockPolygon, List.of(licenceBlockLine)));

    var expectedRequest = ReferenceBlockValidationRequest.newBuilder()
        .setCoordinateSystem(CoordinateSystem.ED50)
        .addRefBlockPolygonLineWrappersList(EsriJsonPolygonLineWrappers.newBuilder()
            .addLineWrappers(EsriJsonLineWithNavigationAndId.newBuilder()
                .setEsriJsonString("ref block esri json")
                .setNavigationType(LineNavigationType.GEODESIC)
                .setOracleLineSsid(10)
                .build()))
        .addLicenceBlockPolygonLineWrappersList(EsriJsonPolygonLineWrappers.newBuilder()
            .addLineWrappers(EsriJsonLineWithNavigationAndId.newBuilder()
                .setEsriJsonString("licence block esri json")
                .setNavigationType(LineNavigationType.LOXODROME)
                .setOracleLineSsid(20)
                .build()));

    var response = ValidationResponse.newBuilder()
        .setIsValid(true)
        .setMessage("Valid")
        .build();
    when(arcgisClient.validateReferenceBlock(expectedRequest.build())).thenReturn(response);

    assertThat(grpcClientService.validateReferenceBlock(refBlockFeature, List.of(licenceBlockFeature))).isEqualTo(response);

    verify(arcgisClient).validateReferenceBlock(expectedRequest.build());
  }
}
