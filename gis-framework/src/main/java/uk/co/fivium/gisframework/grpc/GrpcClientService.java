package uk.co.fivium.gisframework.grpc;

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
import uk.co.fivium.grpc.gis.ArcGisServiceGrpc;
import uk.co.fivium.grpc.gis.BlockAndSubareaValidationRequest;
import uk.co.fivium.grpc.gis.BuildPolygonRequest;
import uk.co.fivium.grpc.gis.ChildLineMatch;
import uk.co.fivium.grpc.gis.CoordinateSystem;
import uk.co.fivium.grpc.gis.EsriJsonLineWithNavigationAndId;
import uk.co.fivium.grpc.gis.EsriJsonPolygonLineWrappers;
import uk.co.fivium.grpc.gis.EsriJsonPolygonLines;
import uk.co.fivium.grpc.gis.ExplodePolygonRequest;
import uk.co.fivium.grpc.gis.FindParentLinesRequest;
import uk.co.fivium.grpc.gis.GeoJsonLineWrapper;
import uk.co.fivium.grpc.gis.LineNavigationType;
import uk.co.fivium.grpc.gis.MigrateBlockOrSubAreaRequest;
import uk.co.fivium.grpc.gis.MigrateBlockOrSubAreaResponse;
import uk.co.fivium.grpc.gis.MigrateReferenceBlockRequest;
import uk.co.fivium.grpc.gis.MigrateReferenceBlockResponse;
import uk.co.fivium.grpc.gis.ParentLine;
import uk.co.fivium.grpc.gis.ReferenceBlockValidationRequest;
import uk.co.fivium.grpc.gis.SplitPolygonRequest;
import uk.co.fivium.grpc.gis.TopologicallyEqualValidationRequest;
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
   * @return EsriJSON of the built polygon as a string.
   */
  public String buildPolygon(List<String> esriJsonPolylines,
                             CoordinateSystem coordinateSystem) {
    var request = BuildPolygonRequest.newBuilder()
        .addAllEsriJsonPolylines(esriJsonPolylines)
        .setCoordinateSystemWkid(CoordinateSystemUtils.getWkid(coordinateSystem))
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

  public MigrationResponseDto migrateBlockOrSubarea(
      List<OracleBoundaryLineWithRing> linesWithRing,
      CoordinateSystem coordinateSystem,
      List<String> parentLines
  ) {
    var requestBuilder = MigrateBlockOrSubAreaRequest.newBuilder()
        .setCoordinateSystem(coordinateSystem)
        .addAllParentLineEsriJsonStrings(parentLines);

    for (var entry : linesWithRing) {
      var oracleLine = entry.oracleBoundaryLine();
      requestBuilder.addGeoJsonLineWrappers(GeoJsonLineWrapper.newBuilder()
          .setGeoJsonString(oracleLine.getLineGeojson())
          .setIsGeodesic(oracleLine.getLineNavigationType() == LineNavigationType.GEODESIC)
          .setOracleLineSsid(oracleLine.getLineSidId().intValue())
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
    var request = BlockAndSubareaValidationRequest.newBuilder()
        .setCoordinateSystem(childFeature.feature().getCoordinateSystem());

    buildPolygonLineWrappers(childFeature).forEach(request::addChildPolygonLineWrappersLists);
    buildPolygonLineWrappers(parentFeature).forEach(request::addParentPolygonLineWrappersLists);

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
}
