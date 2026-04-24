package uk.co.fivium.gisframework.migration;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
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
import uk.co.fivium.gisframework.migration.oracle.OracleService;
import uk.co.fivium.gisframework.migration.oracle.OracleShapeCompositeKey;
import uk.co.fivium.grpc.gis.CoordinateSystem;

@Profile("gis-migration")
@Service
public class MigrationService {

  private static final Logger LOGGER = LoggerFactory.getLogger(MigrationService.class);

  private final FeatureService featureService;
  private final PolygonService polygonService;
  private final LineService lineService;

  private final OracleService oracleService;
  private final GrpcClientService grpcClientService;

  public MigrationService(
      FeatureService featureService,
      PolygonService polygonService,
      LineService lineService,
      OracleService oracleService,
      GrpcClientService grpcClientService
  ) {
    this.featureService = featureService;
    this.polygonService = polygonService;
    this.lineService = lineService;
    this.oracleService = oracleService;
    this.grpcClientService = grpcClientService;
  }

  void migrateKnownIds() {
    // TODO EPGF-16: convert this into an actuator
    lineService.deleteAll();
    polygonService.deleteAll();
    featureService.deleteAll();

    migrateBlocksAndSubarea(
        oracleService.getEntityBackedOracleShapesByIdsIn(List.of(
            new OracleShapeCompositeKey(25629177, "GISA-137"),
            new OracleShapeCompositeKey(23922223, "GISA-27"),
            new OracleShapeCompositeKey(29960964, "GISA-63"),
            new OracleShapeCompositeKey(5610939, "GISA-36 and GISA-38"),
            new OracleShapeCompositeKey(26239556, "GISA-49-simple"),
            new OracleShapeCompositeKey(27240908, "GISA-49-coastline"),
            new OracleShapeCompositeKey(27912658, "GISA-65"),
            new OracleShapeCompositeKey(51662549, "GISA-115"),
            new OracleShapeCompositeKey(9082218, "GISA-145"),
            new OracleShapeCompositeKey(26282337, "GISA-146")
        ))
    );
    migrateBlocksAndSubarea(
        oracleService.getEntityBackedOracleShapesByIdsIn(List.of(
            new OracleShapeCompositeKey(25629221, "GISA-137"),
            new OracleShapeCompositeKey(25629205, "GISA-137"),
            new OracleShapeCompositeKey(23922738, "GISA-27"),
            new OracleShapeCompositeKey(56750115, "GISA-64"),
            new OracleShapeCompositeKey(56973846, "GISA-36"),
            new OracleShapeCompositeKey(56973884, "GISA-38"),
            new OracleShapeCompositeKey(56973868, "GISA-36 and GISA-38"),
            new OracleShapeCompositeKey(31965677, "GISA-49-simple"),
            new OracleShapeCompositeKey(56973797, "GISA-49-coastline"),
            new OracleShapeCompositeKey(27912705, "GISA-65"),
            new OracleShapeCompositeKey(51663540, "GISA-115"),
            new OracleShapeCompositeKey(51662110, "GISA-115"),
            new OracleShapeCompositeKey(51662171, "GISA-115"),
            new OracleShapeCompositeKey(51661936, "GISA-115"),
            new OracleShapeCompositeKey(51662326, "GISA-115"),
            new OracleShapeCompositeKey(51662420, "GISA-115")
        ))
    );
  }

  void migrateBlocksAndSubarea(List<EntityBackedOracleShape> entityBackedOracleShapes) {
    for (var entityBackedShape : entityBackedOracleShapes) {
      LOGGER.info("migrating {} {}", entityBackedShape.shape().getShapeSidId(), entityBackedShape.shape().getShapeName());
      var newFeature = migrateFeature(entityBackedShape);

      Map<Polygon, List<Line>> polygonToLine = new HashMap<>();

      for (var polygonAndBoundary : entityBackedShape.polygonToBoundary().entrySet()) {
        var oraclePolygon = polygonAndBoundary.getKey();
        var oracleBoundaries = polygonAndBoundary.getValue();

        var newPolygon = migratePolygon(
            oraclePolygon.getPolygonSidId(),
            newFeature,
            oraclePolygon.getFeatureOffsetHighM(),
            oraclePolygon.getFeatureOffsetLowM(),
            Map.of() //TODO: Set attributes
        );

        var parentLines = new ArrayList<String>();
        if (newFeature.getParentFeature() != null) {
          parentLines.addAll(
              lineService.findAllByFeatureIn(Collections.singletonList(newFeature.getParentFeature())).stream().map(
                  Line::getEsriJson).toList());
        }

        var newLines = migrateLines(
            newFeature,
            newPolygon,
            oracleBoundaries,
            entityBackedShape,
            parentLines
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
      var areaDifference = newFeature.getFeatureArea().subtract(BigDecimal.valueOf(entityBackedShape.shape().getShareAreaM2()));
      LOGGER.info("Feature {} has area difference of {}", newFeature.getFeatureName(), areaDifference);
    }
  }

  Feature migrateFeature(EntityBackedOracleShape entityBackedShape) {
    var oracleShape = entityBackedShape.shape();
    var newFeature = new Feature();
    newFeature.setLegacyId(oracleShape.getShapeSidId());
    newFeature.setFeatureName(oracleShape.getShapeName());
    newFeature.setTestCase(oracleShape.getTestCase());

    var coordinateSystem = switch (oracleShape.getShapeSrs()) {
      case "ED 50" -> CoordinateSystem.ED50;
      case "OSGB NATIONAL GRID" -> CoordinateSystem.BRITISH_NATIONAL_GRID;
      default -> throw new IllegalStateException("Unknown oracle coordinate system: " + oracleShape.getShapeSrs());
    };

    newFeature.setCoordinateSystem(coordinateSystem);

    newFeature.setAttributes(Map.of("SHAPE_TYPE", oracleShape.getShapeType().name()));

    var parentShapeId = oracleShape.getParentShapeId();
    if (parentShapeId != null) {
      newFeature.setParentFeature(featureService.getByLegacyId(parentShapeId));
    }

    newFeature.setFeatureArea(BigDecimal.ZERO); // this is calculated after line migration

    return newFeature;
  }

  Polygon migratePolygon(
      Integer polygonSidId,
      Feature feature,
      Long startDepth,
      Long endDepth,
      Map<String, Object> attributes
  ) {
    var polygon = new Polygon();
    polygon.setLegacyId(polygonSidId);
    polygon.setAttributes(attributes);
    polygon.setFeature(feature);
    polygon.setStartDepth(Math.abs(startDepth) == 999999999L ? null : startDepth);
    polygon.setEndDepth(Math.abs(endDepth) == 999999999L ? null : endDepth);
    return polygon;
  }

  List<Line> migrateLines(
      Feature feature,
      Polygon polygon,
      List<OraclePolygonBoundary> oracleBoundaries,
      EntityBackedOracleShape entityBackedShape,
      List<String> parentLines
  ) {
    List<OracleBoundaryLineWithRing> linesWithRing = new ArrayList<>();
    for (int ringNumber = 0; ringNumber < oracleBoundaries.size(); ringNumber++) {
      var oracleBoundary = oracleBoundaries.get(ringNumber);
      for (var oracleLine : entityBackedShape.boundaryToLine().get(oracleBoundary)) {
        linesWithRing.add(new OracleBoundaryLineWithRing(oracleLine, ringNumber));
      }
    }

    var migrationResponseDto = grpcClientService.migrateBlockOrSubarea(
        linesWithRing,
        feature.getCoordinateSystem(),
        parentLines
    );

    List<Line> newLines = new ArrayList<>();
    for (var entry : linesWithRing) {
      var oracleLine = entry.oracleBoundaryLine();
      var oracleLineSsid = oracleLine.getLineSidId().intValue();
      var line = new Line();
      line.setLegacyId(oracleLineSsid);
      line.setAttributes(Map.of());
      line.setPolygon(polygon);
      line.setNavigationType(oracleLine.getLineNavigationType());
      line.setEsriJson(migrationResponseDto.oracleSsidToEsriJsonLineString().get(oracleLineSsid));
      line.setRingNumber(entry.ringNumber());
      line.setRingConnectionOrder(oracleLine.getConnectionOrder().intValue());
      newLines.add(line);
    }

    BigDecimal area = feature.getFeatureArea().add(BigDecimal.valueOf(migrationResponseDto.area()));
    feature.setFeatureArea(area);
    return newLines;
  }
}
