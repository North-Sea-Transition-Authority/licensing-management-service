package uk.co.fivium.gisframework.grpc;

import com.esri.core.geometry.Point;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import uk.co.fivium.gisframework.feature.CoordinateSystemUtils;
import uk.co.fivium.gisframework.feature.EntityBackedFeature;
import uk.co.fivium.gisframework.feature.Line;
import uk.co.fivium.gisframework.migration.MigrationResponseDto;
import uk.co.fivium.gisframework.migration.oracle.OracleBoundaryLineWithRing;
import uk.co.fivium.gisframework.operator.LineWithStartEndPoints;
import uk.co.fivium.grpc.gis.ArcGisServiceGrpc;
import uk.co.fivium.grpc.gis.BlockAndSubareaValidationRequest;
import uk.co.fivium.grpc.gis.BuildPolygonRequest;
import uk.co.fivium.grpc.gis.CalculateAreaRequest;
import uk.co.fivium.grpc.gis.ChildLineMatch;
import uk.co.fivium.grpc.gis.Coordinate;
import uk.co.fivium.grpc.gis.CoordinateSystem;
import uk.co.fivium.grpc.gis.EsriJsonLineWithNavigationAndId;
import uk.co.fivium.grpc.gis.EsriJsonPolygonLineWrappers;
import uk.co.fivium.grpc.gis.EsriJsonPolygonLines;
import uk.co.fivium.grpc.gis.ExplodePolygonRequest;
import uk.co.fivium.grpc.gis.FindNorthwestMostLineRequest;
import uk.co.fivium.grpc.gis.FindParentLinesRequest;
import uk.co.fivium.grpc.gis.GeoJsonLineWrapper;
import uk.co.fivium.grpc.gis.GetLineStartAndEndPointsRequest;
import uk.co.fivium.grpc.gis.LineNavigationType;
import uk.co.fivium.grpc.gis.LineWithId;
import uk.co.fivium.grpc.gis.LineWithNavigationType;
import uk.co.fivium.grpc.gis.MigrateBlockOrSubAreaRequest;
import uk.co.fivium.grpc.gis.MigrateBlockOrSubAreaResponse;
import uk.co.fivium.grpc.gis.MigrateReferenceBlockRequest;
import uk.co.fivium.grpc.gis.MigrateReferenceBlockResponse;
import uk.co.fivium.grpc.gis.ParentLine;
import uk.co.fivium.grpc.gis.ReferenceBlockValidationRequest;
import uk.co.fivium.grpc.gis.SplitPolygonRequest;
import uk.co.fivium.grpc.gis.TopologicallyEqualValidationRequest;
import uk.co.fivium.grpc.gis.ValidatePolygonReconstructionFromPolylinesRequest;
import uk.co.fivium.grpc.gis.ValidationResponse;

@Service
public class GrpcClientService {

  @GrpcClient("node-server")
  private ArcGisServiceGrpc.ArcGisServiceBlockingStub arcgisClient;

  /**
   * Split a polygon using a cutter line.
   *
   * @param esriJsonPolygon    The polygon to split.
   * @param esriJsonCutterLine The cutter line.
   * @return A list of output EsriJSON polygons.
   */
  public List<String> splitPolygon(String esriJsonPolygon,
                                   String esriJsonCutterLine) {
    var request = SplitPolygonRequest.newBuilder()
        .setEsriJsonPolygonTarget(esriJsonPolygon)
        .setEsriJsonLineCutter(esriJsonCutterLine)
        .build();

    var response = arcgisClient.splitPolygon(request);
    return response.getOutputPolygonEsriJsonsList();
  }

  /**
   * Takes a list of polylines EsriJSON and combines them into a polygon using the arcgis js sdk.
   *
   * @param esriJsonPolylines An ordered list of EsriJSON polylines. Must be sorted by their ring connection order.
   * @param coordinateSystem  The coordinate system of the polylines. Must be the same for all polylines.
   * @param projectToWgs84 True if the constructed polygon should be projected to WGS84.
   * @return EsriJSON of the built polygon as a string.
   */
  public String buildPolygon(List<String> esriJsonPolylines,
                             CoordinateSystem coordinateSystem,
                             boolean projectToWgs84) {
    var request = BuildPolygonRequest.newBuilder()
        .addAllEsriJsonPolylines(esriJsonPolylines)
        .setCoordinateSystemWkid(CoordinateSystemUtils.getWkid(coordinateSystem))
        .setProjectToWgs84(projectToWgs84)
        .build();

    var response = arcgisClient.buildPolygon(request);
    return response.getPolygonEsriJson();
  }

  /**
   * Explode a polygon into its constituent lines.
   *
   * @param polygonEsriJson The EsriJSON of the polygon to explode.
   * @return A list of EsriJSON lines that make up the polygon.
   */
  public List<String> explodePolygon(String polygonEsriJson) {
    var request = ExplodePolygonRequest.newBuilder()
        .setEsriJsonPolygon(polygonEsriJson)
        .build();

    var response = arcgisClient.explodePolygon(request);
    return response.getEsriJsonLinesList();
  }

  /**
   * Find the parent line for a list of child lines.
   * If multiple child lines share the same parent line, they are merged into a single polyline if they form a
   * continuous line.
   *
   * @param parentLines       The list of potential parent lines to match against.
   * @param childLineEsriJson The list of child lines to match against the parent lines.
   * @return A record containing the mapping of child lines to their parent lines and orphaned child lines.
   */
  public FindParentLineResponse findParentLines(List<Line> parentLines,
                                                List<String> childLineEsriJson) {
    var requestBuilder = FindParentLinesRequest.newBuilder();
    for (Line parentLine : parentLines) {
      requestBuilder.addParentLines(ParentLine.newBuilder()
          .setId(parentLine.getId().toString())
          .setEsriJsonPolyline(parentLine.getEsriJson())
          .build());
    }
    requestBuilder.addAllChildrenEsriJsonPolylines(childLineEsriJson);

    var response = arcgisClient.findParentLines(requestBuilder.build());

    Map<String, UUID> polylineToParentLineId = new HashMap<>();
    for (ChildLineMatch childLineMatch : response.getLinesWithParentMatchList()) {
      polylineToParentLineId.put(childLineMatch.getChildEsriJsonPolyline(),
          UUID.fromString(childLineMatch.getParentId()));
    }

    return new FindParentLineResponse(polylineToParentLineId, response.getOrphanedChildrenEsriJsonPolylinesList());
  }


  /**
   * Validate that a polygon can be reconstructed from a list of lines.
   * @param lines the lines to validate.
   * @param originalPolygonEsriJson the EsriJSON of the original polygon.
   * @return true if the polygon can be reconstructed from the lines, false otherwise.
   */
  public boolean validatePolygonReconstructionFromPolylines(List<Line> lines, String originalPolygonEsriJson) {
    var request = ValidatePolygonReconstructionFromPolylinesRequest.newBuilder()
        .addAllEsriJsonPolylines(lines.stream().map(Line::getEsriJson).toList())
        .setOriginalPolygonEsriJson(originalPolygonEsriJson)
        .build();

    var response = arcgisClient.validatePolygonReconstructionFromPolylines(request);
    return response.getIsValid();
  }

  public MigrationResponseDto migrateBlockOrSubarea(
      List<OracleBoundaryLineWithRing> linesWithRing,
      CoordinateSystem coordinateSystem,
      List<String> parentLines,
      Integer childShapeId
  ) {
    var requestBuilder = MigrateBlockOrSubAreaRequest.newBuilder()
        .setCoordinateSystem(coordinateSystem)
        .addAllParentLineEsriJsonStrings(parentLines)
        .setShapeId(String.valueOf(childShapeId));

    for (var entry : linesWithRing) {
      var oracleLine = entry.oracleBoundaryLine();
      requestBuilder.addGeoJsonLineWrappers(GeoJsonLineWrapper.newBuilder()
          .setGeoJsonString(oracleLine.getLineGeojson())
          .setIsGeodesic(oracleLine.getLineNavigationType() == LineNavigationType.GEODESIC)
          .setOracleLineSsid(oracleLine.getLineSidId())
          .setConnectionOrder(oracleLine.getConnectionOrder().intValue())
          .setRingNumber(entry.ringNumber())
          .build()
      );
    }

    MigrateBlockOrSubAreaResponse response = arcgisClient.migrateBlockOrSubarea(requestBuilder.build());

    Map<Integer, String> oracleSsidToEsriJsonLineString = new HashMap<>();
    for (var lineOutput : response.getEsriJsonLineAndOracleIdsList()) {
      oracleSsidToEsriJsonLineString.put(lineOutput.getOracleLineSsid(), lineOutput.getEsriJsonString());
    }

    return new MigrationResponseDto(oracleSsidToEsriJsonLineString, response.getArea());
  }

  /**
   * Migrates a reference block based on the licence lines within that reference block.
   * When migrating reference blocks cartesian lines are treated as geodesic lines.
   * When handling reference block lines we want to treat cartesian lines as geodesic.
   *
   * @param referenceBlockLinesWithRing a list of objects with link the lines to their ring number
   * @param coordinateSystem            the coordinate system of the reference block
   * @param licenseBlockLines           a list of all lines of all licence blocks within the reference block.
   * @return a Map of the oracle line id to the migrated EsriJSON polyline string for the reference block.
   */
  public Map<Integer, String> migrateReferenceBlock(
      List<OracleBoundaryLineWithRing> referenceBlockLinesWithRing,
      CoordinateSystem coordinateSystem,
      List<Line> licenseBlockLines
  ) {
    var requestBuilder = MigrateReferenceBlockRequest.newBuilder()
        .setCoordinateSystem(coordinateSystem);

    for (var entry : referenceBlockLinesWithRing) {
      var oracleLine = entry.oracleBoundaryLine();
      requestBuilder.addGeoJsonLineWrappers(GeoJsonLineWrapper.newBuilder()
          .setGeoJsonString(oracleLine.getLineGeojson())
          .setIsGeodesic(oracleLine.getLineNavigationType() != LineNavigationType.LOXODROME)
          .setOracleLineSsid(oracleLine.getLineSidId().intValue())
          .setConnectionOrder(oracleLine.getConnectionOrder().intValue())
          .setRingNumber(entry.ringNumber())
          .build()
      );
    }

    for (Line line : licenseBlockLines) {
      requestBuilder.addLicenseBlockLines(
          EsriJsonLineWithNavigationAndId.newBuilder()
              .setEsriJsonString(line.getEsriJson())
              .setNavigationType(line.getNavigationType())
              .build()
      );
    }

    MigrateReferenceBlockResponse response =
        arcgisClient.migrateReferenceBlock(requestBuilder.build());

    Map<Integer, String> result = new HashMap<>();
    for (var lineOutput : response.getEsriJsonLineWithIdList()) {
      result.put(lineOutput.getOracleLineSsid(), lineOutput.getEsriJsonString());
    }
    return result;
  }

  public ValidationResponse validateBlockAndSubarea(
      EntityBackedFeature childFeature,
      EntityBackedFeature parentFeature
  ) {
    return validateBlockAndSubarea(childFeature, List.of(parentFeature));
  }

  /**
   * Validates that the child feature is contained by the combined geometry of all the given parent features, and that
   * any geodesic child lines overlap the parents' geodesic lines. Every parent's polygons and lines are sent to the
   * node side, allowing a child that straddles multiple parents (e.g. a retention area spanning several blocks) to be
   * validated against all of them at once.
   *
   * @param childFeature   The child feature represented as an object that maps a feature to its polygons and lines.
   * @param parentFeatures All the parent features represented by objects that map the feature to their polygons and lines.
   * @return a ValidationResponse object, indicating if the validation has passed or not, and if not, then the reason for why not.
   */
  public ValidationResponse validateBlockAndSubarea(
      EntityBackedFeature childFeature,
      List<EntityBackedFeature> parentFeatures
  ) {
    var request = BlockAndSubareaValidationRequest.newBuilder()
        .setCoordinateSystem(childFeature.feature().getCoordinateSystem());

    buildPolygonLineWrappers(childFeature).forEach(request::addChildPolygonLineWrappersLists);

    for (var parentFeature : parentFeatures) {
      buildPolygonLineWrappers(parentFeature).forEach(request::addParentPolygonLineWrappersLists);
    }

    return arcgisClient.validateBlockAndSubarea(request.build());
  }

  public ValidationResponse validateTopologicallyEqual(
      List<List<String>> childPolygonsLines,
      EntityBackedFeature parentFeature
  ) {
    var request = TopologicallyEqualValidationRequest.newBuilder()
        .setCoordinateSystem(parentFeature.feature().getCoordinateSystem())
        .addAllChildPolygons(
            childPolygonsLines
                .stream()
                .map(polygonLines -> EsriJsonPolygonLines.newBuilder()
                    .addAllEsriJsonPolyline(polygonLines)
                    .build()
                )
                .toList()
        )
        .addAllParentPolygons(
            parentFeature.polygonToLines().values().stream()
                .map(lines -> EsriJsonPolygonLines.newBuilder()
                    .addAllEsriJsonPolyline(
                        lines.stream().map(Line::getEsriJson).toList()
                    )
                    .build()
                )
                .toList()
        );

    return arcgisClient.validateTopologicallyEqual(request.build());
  }

  /**
   * Validates that all licence blocks for a given reference block are contained, and that any geodesic licence block lines
   * overlap their the reference block's geodesic lines.
   *
   * @param refBlockFeature      The reference block represented as an object that maps a feature to its polygons and lines.
   * @param licenseBlockFeatures All the licence blocks represent by objects that map the feature to their polygons and lines.
   * @return a ValidationResponse object, indicating if the validation has passed or not, and if not, then the reason for why not.
   */
  public ValidationResponse validateReferenceBlock(
      EntityBackedFeature refBlockFeature,
      List<EntityBackedFeature> licenseBlockFeatures
  ) {
    var request = ReferenceBlockValidationRequest.newBuilder()
        .setCoordinateSystem(refBlockFeature.feature().getCoordinateSystem());

    buildPolygonLineWrappers(refBlockFeature).forEach(request::addRefBlockPolygonLineWrappersList);

    for (var licenseBlockFeature : licenseBlockFeatures) {
      buildPolygonLineWrappers(licenseBlockFeature).forEach(request::addLicenceBlockPolygonLineWrappersList);
    }

    return arcgisClient.validateReferenceBlock(request.build());
  }

  private List<EsriJsonPolygonLineWrappers.Builder> buildPolygonLineWrappers(
      EntityBackedFeature feature
  ) {
    return feature.polygonToLines().values().stream()
        .map(lines -> {
          var lineWrappers = lines.stream()
              .map(line -> EsriJsonLineWithNavigationAndId.newBuilder()
                  .setEsriJsonString(line.getEsriJson())
                  .setNavigationType(line.getNavigationType())
                  .setOracleLineSsid(line.getLegacyId())
                  .build())
              .toList();
          return EsriJsonPolygonLineWrappers.newBuilder()
              .addAllLineWrappers(lineWrappers);
        })
        .toList();
  }

  /**
   * Get the start and end points of a list of lines.
   * @param lines the lines to get the start and end points of.
   * @return a list of lines with their start and end points.
   */
  public List<LineWithStartEndPoints> getLineStartAndEndPoints(List<Line> lines) {
    //lines might not have an id yet
    Map<UUID, Line> tempIdToLine = new HashMap<>();
    lines.forEach(line -> tempIdToLine.put(UUID.randomUUID(), line));

    var linesWithId = tempIdToLine.entrySet().stream()
        .map(entry -> LineWithId.newBuilder()
            .setId(entry.getKey().toString())
            .setPolyLineEsriJson(entry.getValue().getEsriJson())
            .build())
        .toList();
    var request = GetLineStartAndEndPointsRequest.newBuilder()
        .addAllLines(linesWithId)
        .build();

    var response = arcgisClient.getLineStartAndEndPoints(request);

    return response.getLinesList().stream()
        .map(lineWithStartAndEndPoint -> {
          var line = tempIdToLine.get(UUID.fromString(lineWithStartAndEndPoint.getLineId()));
          Point startPoint = getEsriPoint(lineWithStartAndEndPoint.getStartPoint());
          Point endPoint = getEsriPoint(lineWithStartAndEndPoint.getEndPoint());
          return new LineWithStartEndPoints(line, startPoint, endPoint);
        })
        .toList();
  }

  /**
   * Get the UUID of the line with the northwest-most start point.
   * @param idToLine Map of line UUID to line entity. All lines should belong to the same ring.
   * @return The UUID of the line with the northwest-most start point.
   */
  public UUID findNorthwestMostLine(Map<UUID, Line> idToLine) {
    var request = FindNorthwestMostLineRequest.newBuilder()
        .addAllLines(idToLine.entrySet().stream()
            .map(lineEntry -> LineWithId.newBuilder()
                .setId(lineEntry.getKey().toString())
                .setPolyLineEsriJson(lineEntry.getValue().getEsriJson())
                .build())
            .toList())
        .build();

    var response = arcgisClient.findNorthwestMostLine(request);
    return UUID.fromString(response.getLineId());
  }

  /**
   * Calculate the area of a feature. Loxodrome lines that are not part of a feature with BRITISH_NATIONAL_GRID coordinate
   * system will be densified before calculating the area to ensure the curvature of the earth is taken into account.
   * @param coordinateSystem The coordinate system of the feature.
   * @param featureLines The lines that make up a feature.
   * @return The area of the feature.
   */
  public BigDecimal calculateArea(CoordinateSystem coordinateSystem, List<Line> featureLines) {
    var linesWithNavigationType = featureLines.stream()
        .map(line -> LineWithNavigationType.newBuilder()
            .setEsriJsonPolyline(line.getEsriJson())
            .setLineNavigationType(line.getNavigationType())
            .build())
        .toList();

    var request = CalculateAreaRequest.newBuilder()
        .setCoordinateSystem(coordinateSystem)
        .addAllLinesWithNavigationType(linesWithNavigationType)
        .build();

    var response = arcgisClient.calculateArea(request);
    return BigDecimal.valueOf(response.getArea());
  }

  private Point getEsriPoint(Coordinate grpcPoint) {
    return new Point(grpcPoint.getX(), grpcPoint.getY());
  }
}
