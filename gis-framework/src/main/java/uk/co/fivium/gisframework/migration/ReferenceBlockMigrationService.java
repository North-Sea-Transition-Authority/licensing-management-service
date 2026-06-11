package uk.co.fivium.gisframework.migration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.fivium.gisframework.feature.FeatureService;
import uk.co.fivium.gisframework.feature.Line;
import uk.co.fivium.gisframework.feature.LineService;
import uk.co.fivium.gisframework.feature.Polygon;
import uk.co.fivium.gisframework.feature.PolygonService;
import uk.co.fivium.gisframework.grpc.GrpcClientService;
import uk.co.fivium.gisframework.migration.oracle.EntityBackedOracleShape;
import uk.co.fivium.gisframework.migration.oracle.OracleBoundaryLineWithRing;
import uk.co.fivium.gisframework.migration.oracle.OraclePolygonBoundary;
import uk.co.fivium.grpc.gis.LineNavigationType;

@Profile("gis-migration")
@Service
public class ReferenceBlockMigrationService {

  private static final Logger LOGGER = LoggerFactory.getLogger(ReferenceBlockMigrationService.class);

  private final FeatureService featureService;
  private final PolygonService polygonService;
  private final LineService lineService;

  private final GrpcClientService grpcClientService;

  private final MigrationService migrationService;

  public ReferenceBlockMigrationService(
      FeatureService featureService,
      PolygonService polygonService,
      LineService lineService,
      GrpcClientService grpcClientService,
      MigrationService migrationService
  ) {
    this.featureService = featureService;
    this.polygonService = polygonService;
    this.lineService = lineService;
    this.grpcClientService = grpcClientService;
    this.migrationService = migrationService;
  }

  public void migrate(Collection<EntityBackedOracleShape> entityBackedOracleShapes) {
    for (var entityBackedShape : entityBackedOracleShapes) {
      LOGGER.info("migrating {} {}", entityBackedShape.shape().getShapeSidId(), entityBackedShape.shape().getShapeName());
      var newFeature = migrationService.migrateFeature(entityBackedShape);

      var licenseBlocks = new ArrayList<>(featureService.findLicenseBlocksForRefBlock(entityBackedShape.shape().getShapeName()));

      var polygonToLine = new HashMap<Polygon, List<Line>>();

      for (var polygonAndBoundary : entityBackedShape.polygonToBoundary().entrySet()) {
        var oraclePolygon = polygonAndBoundary.getKey();
        var oracleBoundaries = polygonAndBoundary.getValue();

        var newPolygon = migrationService.migratePolygon(
            oraclePolygon.getPolygonSidId(),
            newFeature,
            oraclePolygon.getFeatureOffsetHighM(),
            oraclePolygon.getFeatureOffsetLowM(),
            Map.of() //TODO EPGF-120: Set attributes
        );

        var newLines = migrateRefBlockLines(
            newFeature,
            newPolygon,
            oracleBoundaries,
            entityBackedShape,
            lineService.findAllByFeatureIn(licenseBlocks)
                .stream()
                .filter(line -> LineNavigationType.GEODESIC.equals(line.getNavigationType()))
                .toList()
        );

        polygonToLine.put(newPolygon, newLines);

      }

      featureService.saveFeature(newFeature);
      for (var entry : polygonToLine.entrySet()) {
        var polygon = entry.getKey();
        polygonService.savePolygon(polygon);

        var lines = entry.getValue();
        lineService.saveLines(lines);
      }
    }
  }


  /**
   * This method will send the ref block lines to the node server so they can be processed by being converted to esriJSON,
   * densifying the geodesics and copying the license geodesics points onto the ref block geodesic.
   *
   * @param feature           The feature (ref block) we are currently migrating
   * @param polygon           The polygon of the feature (ref block) we are currently migrating
   * @param oracleBoundaries  The oracle boundary of the current shape
   * @param entityBackedShape The oracle entityBackedShape object for the shape being migrated
   * @param licenseLines      All geodesic lines of all license blocks that are within the ref block
   * @return migrated line entities.
   */
  List<Line> migrateRefBlockLines(
      Feature feature,
      Polygon polygon,
      List<OraclePolygonBoundary> oracleBoundaries,
      EntityBackedOracleShape entityBackedShape,
      List<Line> licenseLines
  ) {
    var linesWithRing = new ArrayList<OracleBoundaryLineWithRing>();
    for (int ringNumber = 0; ringNumber < oracleBoundaries.size(); ringNumber++) {
      var oracleBoundary = oracleBoundaries.get(ringNumber);
      for (var oracleLine : entityBackedShape.boundaryToLine().get(oracleBoundary)) {
        linesWithRing.add(new OracleBoundaryLineWithRing(oracleLine, ringNumber));
      }
    }

    var lineSsidToEsriJson = grpcClientService.migrateReferenceBlock(
        linesWithRing,
        feature.getCoordinateSystem(),
        licenseLines
    );

    var newLines = new ArrayList<Line>();
    for (var entry : linesWithRing) {
      var oracleLine = entry.oracleBoundaryLine();
      var oracleLineSsid = oracleLine.getLineSidId().intValue();

      if (!lineSsidToEsriJson.containsKey(oracleLineSsid)) {
        continue;
      }

      var line = new Line();
      line.setLegacyId(oracleLineSsid);
      line.setAttributes(Map.of());
      line.setPolygon(polygon);
      line.setNavigationType(oracleLine.getLineNavigationType());
      line.setEsriJson(lineSsidToEsriJson.get(oracleLineSsid));
      line.setRingNumber(entry.ringNumber());
      line.setRingConnectionOrder(oracleLine.getConnectionOrder().intValue());

      newLines.add(line);
    }
    return newLines;
  }
}
