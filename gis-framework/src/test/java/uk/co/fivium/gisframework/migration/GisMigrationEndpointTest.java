package uk.co.fivium.gisframework.migration;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.gisframework.feature.FeatureService;
import uk.co.fivium.gisframework.feature.Layer;
import uk.co.fivium.gisframework.feature.LineService;
import uk.co.fivium.gisframework.feature.PolygonService;
import uk.co.fivium.gisframework.migration.oracle.EntityBackedOracleShape;
import uk.co.fivium.gisframework.migration.oracle.OracleService;
import uk.co.fivium.gisframework.migration.oracle.OracleShapeTestUtil;

@ExtendWith(MockitoExtension.class)
class GisMigrationEndpointTest {

  @Mock
  private FeatureService featureService;

  @Mock
  private PolygonService polygonService;

  @Mock
  private LineService lineService;

  @Mock
  private OracleService oracleService;

  @Mock
  private MigrationService migrationService;

  @Mock
  private ReferenceBlockMigrationService referenceBlockMigrationService;

  @Mock
  private MigrationValidationService migrationValidationService;

  @InjectMocks
  private GisMigrationEndpoint gisMigrationEndpoint;

  @Test
  void migrate() {
    var rootBlocks = List.of(entityBackedOracleShapeWithShapeSiId(1));
    var redefinitionPoints = List.of(entityBackedOracleShapeWithShapeSiId(1));
    var blockChanges = List.of(entityBackedOracleShapeWithShapeSiId(3));
    var subAreas = List.of(entityBackedOracleShapeWithShapeSiId(4));
    var retentionAreas = List.of(entityBackedOracleShapeWithShapeSiId(5));
    var referenceBlocks = List.of(entityBackedOracleShapeWithShapeSiId(6));

    when(oracleService.getEntityBackedOracleShapesForMigrationOrderNumber(10)).thenReturn(rootBlocks);
    when(oracleService.getEntityBackedOracleShapesForMigrationOrderNumber(15)).thenReturn(redefinitionPoints);
    when(oracleService.getEntityBackedOracleShapesForMigrationOrderNumber(20)).thenReturn(blockChanges);
    when(oracleService.getEntityBackedOracleShapesForMigrationOrderNumber(30)).thenReturn(subAreas);
    when(oracleService.getEntityBackedOracleShapesForMigrationOrderNumber(40)).thenReturn(retentionAreas);
    when(oracleService.getEntityBackedOracleShapesForMigrationOrderNumber(50)).thenReturn(referenceBlocks);

    gisMigrationEndpoint.migrate();

    var inOrder = inOrder(migrationService, migrationValidationService, referenceBlockMigrationService);
    inOrder.verify(migrationService).migrateBlocksAndSubarea(rootBlocks);
    inOrder.verify(migrationService).migrateBlocksAndSubarea(redefinitionPoints);
    inOrder.verify(migrationService).migrateBlocksAndSubarea(blockChanges);
    inOrder.verify(migrationService).migrateBlocksAndSubarea(subAreas);
    inOrder.verify(migrationValidationService).childAndParentValidation(Layer.BLOCKS);
    inOrder.verify(migrationValidationService).childAndParentValidation(Layer.SUBAREAS);
    inOrder.verify(migrationValidationService).verifySubareasTopologicallyEqualToBlock();
    inOrder.verify(migrationService).migrateBlocksAndSubarea(retentionAreas);
    inOrder.verify(migrationValidationService).validateRetentionArea();
    inOrder.verify(referenceBlockMigrationService).migrate(referenceBlocks);
    inOrder.verify(migrationValidationService).validateReferenceBlocks();
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  void clearDown() {
    gisMigrationEndpoint.clearDown();

    var inOrder = inOrder(lineService, polygonService, featureService);
    inOrder.verify(lineService).deleteAll();
    inOrder.verify(polygonService).deleteAll();
    inOrder.verify(featureService).deleteAll();
    inOrder.verifyNoMoreInteractions();
    verifyNoInteractions(oracleService, migrationService, referenceBlockMigrationService, migrationValidationService);
  }

  private EntityBackedOracleShape entityBackedOracleShapeWithShapeSiId(Integer shapeSiId) {
    return new EntityBackedOracleShape(
        OracleShapeTestUtil.newBuilder().withShapeSiId(shapeSiId).build(),
        Map.of(),
        Map.of()
    );
  }
}
