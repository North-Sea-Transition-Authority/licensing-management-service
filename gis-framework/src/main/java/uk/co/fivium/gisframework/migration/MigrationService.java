package uk.co.fivium.gisframework.migration;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.fivium.gisframework.feature.FeatureService;
import uk.co.fivium.gisframework.feature.Line;
import uk.co.fivium.gisframework.feature.LineService;
import uk.co.fivium.gisframework.feature.Polygon;
import uk.co.fivium.gisframework.feature.PolygonService;
import uk.co.fivium.gisframework.grpc.GrpcClientService;
import uk.co.fivium.gisframework.migration.oracle.AttributeLevel;
import uk.co.fivium.gisframework.migration.oracle.EntityBackedOracleShape;
import uk.co.fivium.gisframework.migration.oracle.OracleBoundaryLineWithRing;
import uk.co.fivium.gisframework.migration.oracle.OraclePolygonBoundary;
import uk.co.fivium.gisframework.migration.oracle.OracleService;
import uk.co.fivium.gisframework.migration.oracle.OracleShapePolygon;
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

  private final MigrationValidationService migrationValidationService;
  private final TransactionTemplate transactionTemplate;

  public MigrationService(
      FeatureService featureService,
      PolygonService polygonService,
      LineService lineService,
      OracleService oracleService,
      GrpcClientService grpcClientService,
      MigrationValidationService migrationValidationService,
      TransactionTemplate transactionTemplate
  ) {
    this.featureService = featureService;
    this.polygonService = polygonService;
    this.lineService = lineService;
    this.oracleService = oracleService;
    this.grpcClientService = grpcClientService;
    this.migrationValidationService = migrationValidationService;
    this.transactionTemplate = transactionTemplate;
  }

  void migrate() {
    // TODO EPGF-16: convert this into an actuator
    lineService.deleteAll();
    polygonService.deleteAll();
    featureService.deleteAll();

    migrateBlocksAndSubarea(oracleService.getEntityBackedOracleShapesForMigrationOrderNumber(10)); // root blocks
    migrateBlocksAndSubarea(oracleService.getEntityBackedOracleShapesForMigrationOrderNumber(20)); // block changes
    migrateBlocksAndSubarea(oracleService.getEntityBackedOracleShapesForMigrationOrderNumber(30)); // sub areas

    migrationValidationService.childAndParentValidation();
    migrationValidationService.verifySubareasTopologicallyEqualToBlock();
  }

  void migrateBlocksAndSubarea(List<EntityBackedOracleShape> entityBackedOracleShapes) {
    for (var entityBackedShape : entityBackedOracleShapes) {
      try {
        if (entityBackedShape.shape().getShapeSrs().equals("ETRS89_DS")) {
          // shape in invalid state
          LOGGER.warn("Skipping ShapeSiId {} due to ETRS89_DS", entityBackedShape.shape().getShapeSiId());
          continue;
        }
        LOGGER.info("migrating {} {}", entityBackedShape.shape().getShapeSidId(), entityBackedShape.shape().getShapeName());
        var newFeature = migrateFeature(entityBackedShape);

        Map<Polygon, List<Line>> polygonToLine = new HashMap<>();

        var parentLines = new ArrayList<String>();
        if (newFeature.getParentFeature() != null) {
          // The linked ID should be the ID of the root block, which we use to copy the geodesic points from.
          var linkedShapeSiId = oracleService.getLinkedParentShapeSiId(newFeature.getLegacyId()).orElseThrow();
          parentLines.addAll(lineService.findAllByFeatureLegacyId(linkedShapeSiId).stream().map(Line::getEsriJson).toList());
        }

        var idToPolygonAttributeMap = oracleService.getIdToAttributeMapForSiIdAndLevel(
            entityBackedShape.polygonToBoundary()
                .keySet()
                .stream()
                .map(OracleShapePolygon::getPolygonSidId)
                .toList(),
            AttributeLevel.SHAPE_POLYGON
        );
        for (var polygonAndBoundary : entityBackedShape.polygonToBoundary().entrySet()) {
          var oraclePolygon = polygonAndBoundary.getKey();
          var oracleBoundaries = polygonAndBoundary.getValue();

          var newPolygon = migratePolygon(
              oraclePolygon.getPolygonSidId(),
              newFeature,
              oraclePolygon.getFeatureOffsetHighM(),
              oraclePolygon.getFeatureOffsetLowM(),
              new HashMap<>(idToPolygonAttributeMap.getOrDefault(oraclePolygon.getPolygonSidId(), Map.of()))
          );

          var newLines = migrateLines(
              newFeature,
              newPolygon,
              oracleBoundaries,
              entityBackedShape,
              parentLines
          );

          polygonToLine.put(newPolygon, newLines);

        }

        transactionTemplate.executeWithoutResult(transactionStatus -> {
          featureService.saveFeature(newFeature);
          polygonService.savePolygons(polygonToLine.keySet());
          lineService.saveLines(polygonToLine.values().stream().flatMap(List::stream).toList());
        });

        var areaDifference = newFeature.getFeatureArea().subtract(BigDecimal.valueOf(entityBackedShape.shape().getShareAreaM2()));
        if (areaDifference.abs().compareTo(BigDecimal.valueOf(50)) > 0) {
          LOGGER.warn("Feature {} has area difference of {}", newFeature.getFeatureName(), areaDifference);
        }
      } catch (Exception e) {
        LOGGER.error("Error while migrating shape si id {}", entityBackedShape.shape().getShapeSiId(), e);
      }

    }
  }

  Feature migrateFeature(EntityBackedOracleShape entityBackedShape) {
    var oracleShape = entityBackedShape.shape();
    var newFeature = new Feature();
    newFeature.setLegacyId(oracleShape.getShapeSiId());
    newFeature.setFeatureName(oracleShape.getShapeName());
    newFeature.setStartDate(oracleShape.getShapeStartDate());
    newFeature.setEndDate(oracleShape.getShapeEndDate());

    var coordinateSystem = switch (oracleShape.getShapeSrs()) {
      case "ED 50" -> CoordinateSystem.ED50;
      case "ETRS89" -> CoordinateSystem.ETRS89;
      case "OSGB NATIONAL GRID" -> CoordinateSystem.BRITISH_NATIONAL_GRID;
      default -> throw new IllegalStateException("Unknown oracle coordinate system: " + oracleShape.getShapeSrs());
    };

    newFeature.setCoordinateSystem(coordinateSystem);

    newFeature.setAttributes(
        oracleService.getAttributeMapForSiIdAndLevel(oracleShape.getShapeSidId(), AttributeLevel.SHAPE)
    );
    newFeature.getAttributes().put(
        "LAYER",
        entityBackedShape.shape().getOracleLayer().getLayer()
    );

    oracleService.getLinkedParentShapeSiId(oracleShape.getShapeSiId())
        .ifPresent(integer -> newFeature.setParentFeature(featureService.getByLegacyId(integer)));

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

    var boundaryIdToAttributes = oracleService.getIdToAttributeMapForSiIdAndLevel(
        linesWithRing.stream()
            .map(lineWithRing -> lineWithRing.oracleBoundaryLine().getOraclePolygonBoundaryId())
            .toList(),
        AttributeLevel.POLYGON_BOUNDARY
    );

    var lineIdToAttributes = oracleService.getIdToAttributeMapForSiIdAndLevel(
        linesWithRing.stream()
            .map(lineWithRing -> lineWithRing.oracleBoundaryLine().getLineSidId())
            .toList(),
        AttributeLevel.BOUNDARY_LINE
    );

    List<Line> newLines = new ArrayList<>();
    for (var entry : linesWithRing) {
      var oracleLine = entry.oracleBoundaryLine();
      var oracleLineSsid = oracleLine.getLineSidId();
      var line = new Line();
      line.setLegacyId(oracleLineSsid);
      line.setPolygon(polygon);
      line.setNavigationType(oracleLine.getLineNavigationType());
      line.setEsriJson(migrationResponseDto.oracleSsidToEsriJsonLineString().get(oracleLineSsid));
      line.setRingNumber(entry.ringNumber());
      line.setRingConnectionOrder(oracleLine.getConnectionOrder().intValue());

      var boundaryAttributeMap = boundaryIdToAttributes.getOrDefault(oracleLine.getOraclePolygonBoundaryId(), Map.of());
      var lineAttributesMap = lineIdToAttributes.getOrDefault(oracleLineSsid, Map.of());

      line.setAttributes(combineAttributeMaps(boundaryAttributeMap, lineAttributesMap));

      newLines.add(line);
    }

    BigDecimal area = feature.getFeatureArea().add(BigDecimal.valueOf(migrationResponseDto.area()));
    feature.setFeatureArea(area);
    return newLines;
  }

  Map<String, Object> combineAttributeMaps(
      Map<String, String> boundaryAttributes,
      Map<String, String> lineAttributes
  ) {
    var duplicateKeys = boundaryAttributes.keySet()
        .stream()
        .filter(lineAttributes::containsKey)
        .toList();

    if (!duplicateKeys.isEmpty()) {
      LOGGER.warn(
          "Duplicate attribute keys found while combining {} and {} attribute maps: {}",
          AttributeLevel.POLYGON_BOUNDARY,
          AttributeLevel.BOUNDARY_LINE,
          duplicateKeys
      );
    }

    Map<String, Object> combinedAttributes = new HashMap<>();
    putPrefixedAttributes(combinedAttributes, AttributeLevel.POLYGON_BOUNDARY, boundaryAttributes);
    putPrefixedAttributes(combinedAttributes, AttributeLevel.BOUNDARY_LINE, lineAttributes);

    return combinedAttributes;
  }

  private void putPrefixedAttributes(
      Map<String, Object> combinedAttributes,
      AttributeLevel attributeLevel,
      Map<String, String> attributes
  ) {
    attributes.forEach((key, value) ->
        combinedAttributes.put("%s_%s".formatted(attributeLevel, key), value)
    );
  }
}
