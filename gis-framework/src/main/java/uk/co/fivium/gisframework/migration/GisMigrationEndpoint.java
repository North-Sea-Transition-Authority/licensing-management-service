package uk.co.fivium.gisframework.migration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.endpoint.annotation.DeleteOperation;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import uk.co.fivium.gisframework.feature.FeatureService;
import uk.co.fivium.gisframework.feature.LineService;
import uk.co.fivium.gisframework.feature.PolygonService;
import uk.co.fivium.gisframework.migration.oracle.Layer;
import uk.co.fivium.gisframework.migration.oracle.OracleService;

@Profile("gis-migration")
@Component
@Endpoint(id = "gismigration")
public class GisMigrationEndpoint {

  private static final Logger LOGGER = LoggerFactory.getLogger(GisMigrationEndpoint.class);

  private final FeatureService featureService;
  private final PolygonService polygonService;
  private final LineService lineService;
  private final OracleService oracleService;
  private final MigrationService migrationService;
  private final ReferenceBlockMigrationService referenceBlockMigrationService;
  private final MigrationValidationService migrationValidationService;

  public GisMigrationEndpoint(
      FeatureService featureService,
      PolygonService polygonService,
      LineService lineService,
      OracleService oracleService,
      MigrationService migrationService,
      ReferenceBlockMigrationService referenceBlockMigrationService,
      MigrationValidationService migrationValidationService
  ) {
    this.featureService = featureService;
    this.polygonService = polygonService;
    this.lineService = lineService;
    this.oracleService = oracleService;
    this.migrationService = migrationService;
    this.referenceBlockMigrationService = referenceBlockMigrationService;
    this.migrationValidationService = migrationValidationService;
  }

  @WriteOperation
  public void migrate() {
    LOGGER.info("GIS migration started");

    LOGGER.info("Root block migration starting");
    migrationService.migrateBlocksAndSubarea(oracleService.getEntityBackedOracleShapesForMigrationOrderNumber(10));

    LOGGER.info("Redefinition point migration starting");
    migrationService.migrateBlocksAndSubarea(oracleService.getEntityBackedOracleShapesForMigrationOrderNumber(15));

    LOGGER.info("Block change migration starting");
    migrationService.migrateBlocksAndSubarea(oracleService.getEntityBackedOracleShapesForMigrationOrderNumber(20));

    LOGGER.info("Subarea migration starting");
    migrationService.migrateBlocksAndSubarea(oracleService.getEntityBackedOracleShapesForMigrationOrderNumber(30));

    LOGGER.info("Block and subarea validation starting");
    migrationValidationService.childAndParentValidation(Layer.BLOCKS);
    migrationValidationService.childAndParentValidation(Layer.SUBAREAS);
    migrationValidationService.verifySubareasTopologicallyEqualToBlock();

    LOGGER.info("Retention area migration starting");
    migrationService.migrateBlocksAndSubarea(oracleService.getEntityBackedOracleShapesForMigrationOrderNumber(40));

    LOGGER.info("Retention area validation starting");
    migrationValidationService.validateRetentionArea();

    LOGGER.info("Reference block migration starting");
    referenceBlockMigrationService.migrate(oracleService.getEntityBackedOracleShapesForMigrationOrderNumber(50));

    LOGGER.info("Reference block validation starting");
    migrationValidationService.validateReferenceBlocks();

    LOGGER.info("GIS migration finished");
  }

  @DeleteOperation
  public void clearDown() {
    LOGGER.info("GIS migration clear down started");

    lineService.deleteAll();
    polygonService.deleteAll();
    featureService.deleteAll();

    LOGGER.info("GIS migration clear down finished");
  }
}
