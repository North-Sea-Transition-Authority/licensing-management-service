package uk.co.fivium.gisframework.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.when;
import static uk.co.fivium.gisframework.LoggerTestUtil.detachLogAppender;
import static uk.co.fivium.gisframework.LoggerTestUtil.getLogAppender;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.gisframework.feature.EntityBackedFeature;
import uk.co.fivium.gisframework.feature.FeatureService;
import uk.co.fivium.gisframework.feature.FeatureTestUtil;
import uk.co.fivium.gisframework.feature.LineTestUtil;
import uk.co.fivium.gisframework.feature.PolygonTestUtil;
import uk.co.fivium.gisframework.grpc.GrpcClientService;
import uk.co.fivium.gisframework.migration.configuration.BrokenBlockConfigurationProperties;
import uk.co.fivium.gisframework.migration.oracle.Layer;
import uk.co.fivium.gisframework.migration.oracle.OracleService;
import uk.co.fivium.gisframework.migration.oracle.OracleShapeLinkTestUtil;
import uk.co.fivium.grpc.gis.ValidationResponse;

@ExtendWith(MockitoExtension.class)
class MigrationValidationServiceTest {

  private static final String BROKEN_LICENSE_BLOCK_NAME = "16/30c";

  @Mock
  private FeatureService featureService;

  @Mock
  private GrpcClientService grpcClientService;

  @Mock
  private OracleService oracleService;

  private MigrationValidationService migrationValidationService;

  @BeforeEach
  void setUp() {
    migrationValidationService = new MigrationValidationService(
        featureService,
        grpcClientService,
        new BrokenBlockConfigurationProperties(Map.of("16/30", List.of(BROKEN_LICENSE_BLOCK_NAME))),
        oracleService
    );
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void childAndParentValidation_validatesOnlyChildrenMatchingGivenLayer(boolean isValid) {
    var logAppender = getLogAppender(MigrationValidationService.class);

    var parentFeature = FeatureTestUtil.newBuilder().withFeatureName("Parent").withLegacyId(1000).build();
    var blockChildFeature = FeatureTestUtil.newBuilder()
        .withFeatureName("Child 1")
        .withLegacyId(1001)
        .withParentFeature(parentFeature)
        .withAttributes(Map.of("LAYER", Layer.BLOCKS.name()))
        .build();
    var subareaChildFeature = FeatureTestUtil.newBuilder()
        .withFeatureName("Child 2")
        .withLegacyId(1002)
        .withParentFeature(parentFeature)
        .withAttributes(Map.of("LAYER", Layer.SUBAREAS.name()))
        .build();

    var blockChildEntityBacked = new EntityBackedFeature(blockChildFeature, Map.of());
    var parentEntityBacked = new EntityBackedFeature(parentFeature, Map.of());

    when(featureService.findAllChildFeatures())
        .thenReturn(List.of(blockChildFeature, subareaChildFeature));
    when(featureService.getEntityBackedFeatures(Set.of(parentFeature))).thenReturn(List.of(parentEntityBacked));
    when(featureService.getEntityBackedFeatures(List.of(blockChildFeature)))
        .thenReturn(List.of(blockChildEntityBacked));

    var response = ValidationResponse.newBuilder()
        .setIsValid(isValid)
        .setMessage("some message")
        .build();
    when(grpcClientService.validateBlockAndSubarea(blockChildEntityBacked, parentEntityBacked)).thenReturn(response);

    try {
      migrationValidationService.childAndParentValidation(Layer.BLOCKS);
    } finally {
      detachLogAppender(MigrationValidationService.class, logAppender);
    }

    assertThat(logAppender.list)
        .extracting(ILoggingEvent::getLevel, ILoggingEvent::getFormattedMessage)
        .containsExactly(tuple(
            isValid ? Level.INFO : Level.ERROR,
            isValid
                ? "Child %s passed validation checks".formatted(blockChildFeature.getLegacyId())
                : "Validation error: some message Child Feature: %s Parent Feature: %s"
                  .formatted(blockChildFeature.getLegacyId(), parentFeature.getLegacyId())
        ));
  }

  @Test
  void childAndParentValidation_whenRetentionAreaLayer_thenValidatesRetentionAreaChildren() {
    var logAppender = getLogAppender(MigrationValidationService.class);

    var parentFeature = FeatureTestUtil.newBuilder().withFeatureName("Parent").withLegacyId(2000).build();
    var retentionAreaChildFeature = FeatureTestUtil.newBuilder()
        .withFeatureName("Child RA")
        .withLegacyId(2001)
        .withParentFeature(parentFeature)
        .withAttributes(Map.of("LAYER", Layer.RETENTION_AREAS.name()))
        .build();
    var blockChildFeature = FeatureTestUtil.newBuilder()
        .withFeatureName("Child Block")
        .withLegacyId(2002)
        .withParentFeature(parentFeature)
        .withAttributes(Map.of("LAYER", Layer.BLOCKS.name()))
        .build();

    var retentionAreaChildEntityBacked = new EntityBackedFeature(retentionAreaChildFeature, Map.of());
    var parentEntityBacked = new EntityBackedFeature(parentFeature, Map.of());

    when(featureService.findAllChildFeatures())
        .thenReturn(List.of(retentionAreaChildFeature, blockChildFeature));
    when(featureService.getEntityBackedFeatures(Set.of(parentFeature))).thenReturn(List.of(parentEntityBacked));
    when(featureService.getEntityBackedFeatures(List.of(retentionAreaChildFeature)))
        .thenReturn(List.of(retentionAreaChildEntityBacked));

    var response = ValidationResponse.newBuilder().setIsValid(true).setMessage("some message").build();
    when(grpcClientService.validateBlockAndSubarea(retentionAreaChildEntityBacked, parentEntityBacked))
        .thenReturn(response);

    try {
      migrationValidationService.childAndParentValidation(Layer.RETENTION_AREAS);
    } finally {
      detachLogAppender(MigrationValidationService.class, logAppender);
    }

    assertThat(logAppender.list)
        .extracting(ILoggingEvent::getLevel, ILoggingEvent::getFormattedMessage)
        .containsExactly(tuple(
            Level.INFO,
            "Child %s passed validation checks".formatted(retentionAreaChildFeature.getLegacyId())
        ));
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void validateRetentionArea_validatesChildAgainstAllItsParents(boolean isValid) {
    var logAppender = getLogAppender(MigrationValidationService.class);

    var retentionAreaFeature = FeatureTestUtil.newBuilder()
        .withFeatureName("Retention Area")
        .withLegacyId(3001)
        .withAttributes(Map.of("LAYER", Layer.RETENTION_AREAS.name()))
        .build();
    var parentFeature1 = FeatureTestUtil.newBuilder().withFeatureName("Parent 1").withLegacyId(4001).build();
    var parentFeature2 = FeatureTestUtil.newBuilder().withFeatureName("Parent 2").withLegacyId(4002).build();

    var retentionAreaEntityBacked = new EntityBackedFeature(retentionAreaFeature, Map.of());
    var parentEntityBacked1 = new EntityBackedFeature(parentFeature1, Map.of());
    var parentEntityBacked2 = new EntityBackedFeature(parentFeature2, Map.of());

    var shapeLink1 = OracleShapeLinkTestUtil.newBuilder()
        .withChildShapeId(3001)
        .withParentShapeId(4001)
        .build();
    var shapeLink2 = OracleShapeLinkTestUtil.newBuilder()
        .withChildShapeId(3001)
        .withParentShapeId(4002)
        .build();

    when(featureService.findAllByAttribute("LAYER", Layer.RETENTION_AREAS.name())).thenReturn(List.of(retentionAreaFeature));
    when(featureService.getEntityBackedFeatures(List.of(retentionAreaFeature))).thenReturn(List.of(retentionAreaEntityBacked));

    when(oracleService.getShapeLinks(List.of(3001)))
        .thenReturn(List.of(shapeLink1, shapeLink2));
    when(featureService.findAllByLegacyIdIn(List.of(4001, 4002)))
        .thenReturn(List.of(parentFeature1, parentFeature2));
    when(featureService.getEntityBackedFeatures(List.of(parentFeature1, parentFeature2)))
        .thenReturn(List.of(parentEntityBacked1, parentEntityBacked2));

    var response = ValidationResponse.newBuilder().setIsValid(isValid).setMessage("some message").build();
    when(grpcClientService.validateBlockAndSubarea(
        retentionAreaEntityBacked,
        List.of(parentEntityBacked1, parentEntityBacked2)
    )).thenReturn(response);

    try {
      migrationValidationService.validateRetentionArea();
    } finally {
      detachLogAppender(MigrationValidationService.class, logAppender);
    }

    assertThat(logAppender.list)
        .extracting(ILoggingEvent::getLevel, ILoggingEvent::getFormattedMessage)
        .containsExactly(tuple(
            isValid ? Level.INFO : Level.ERROR,
            isValid
                ? "Child %s passed validation checks".formatted(retentionAreaFeature.getLegacyId())
                : "Validation error: some message Child Feature: %s Parent Features: %s"
                  .formatted(retentionAreaFeature.getLegacyId(), List.of(4001, 4002))
        ));
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void verifySubareasTopologicallyEqualToBlock(boolean isValid) {
    var logAppender = getLogAppender(MigrationValidationService.class);

    var parentFeature = FeatureTestUtil.newBuilder()
        .withFeatureName("Parent Block")
        .withLegacyId(5610939)
        .withAttributes(Map.of("LAYER", Layer.BLOCKS.name()))
        .build();

    var childFeature1 = FeatureTestUtil.newBuilder()
        .withFeatureName("Child 1")
        .withParentFeature(parentFeature)
        .withAttributes(Map.of("LAYER", Layer.SUBAREAS.name()))
        .build();
    var childFeature2 = FeatureTestUtil.newBuilder()
        .withFeatureName("Child 2")
        .withParentFeature(parentFeature)
        .withAttributes(Map.of("LAYER", Layer.SUBAREAS.name()))
        .build();
    var orphanSubareaFeature = FeatureTestUtil.newBuilder()
        .withFeatureName("Orphan subarea")
        .withAttributes(Map.of("LAYER", Layer.SUBAREAS.name()))
        .build();

    var polygon1 = PolygonTestUtil.newBuilder().withFeature(childFeature1).build();
    var polygon2 = PolygonTestUtil.newBuilder().withFeature(childFeature2).build();

    var line1 = LineTestUtil.newBuilder().withPolygon(polygon1).withEsriJson("line json 1").build();
    var line2 = LineTestUtil.newBuilder().withPolygon(polygon2).withEsriJson("line json 2").build();

    var parentEntityBacked = new EntityBackedFeature(parentFeature, Map.of());
    var childEntityBacked1 = new EntityBackedFeature(childFeature1, Map.of(polygon1, List.of(line1)));
    var childEntityBacked2 = new EntityBackedFeature(childFeature2, Map.of(polygon2, List.of(line2)));

    when(featureService.findAllByAttribute("LAYER", Layer.BLOCKS.name())).thenReturn(List.of(parentFeature));
    when(featureService.getEntityBackedFeatures(List.of(parentFeature))).thenReturn(List.of(parentEntityBacked));
    when(featureService.findAllByAttribute("LAYER", Layer.SUBAREAS.name()))
        .thenReturn(List.of(childFeature1, childFeature2, orphanSubareaFeature));
    when(featureService.getEntityBackedFeatures(List.of(childFeature1, childFeature2)))
        .thenReturn(List.of(childEntityBacked1, childEntityBacked2));

    var response = ValidationResponse.newBuilder()
        .setIsValid(isValid)
        .setMessage("some message")
        .build();
    when(grpcClientService.validateTopologicallyEqual(
        List.of(List.of("line json 1"), List.of("line json 2")),
        parentEntityBacked
    )).thenReturn(response);

    try {
      migrationValidationService.verifySubareasTopologicallyEqualToBlock();
    } finally {
      detachLogAppender(MigrationValidationService.class, logAppender);
    }

    assertThat(logAppender.list)
        .extracting(ILoggingEvent::getLevel, ILoggingEvent::getFormattedMessage)
        .containsExactly(tuple(
            isValid ? Level.INFO : Level.ERROR,
            isValid
                ? "Parent %s is topologically equal to all of its children".formatted(parentFeature.getLegacyId())
                : "Validation error: some message Feature: %s".formatted(parentFeature.getLegacyId())
        ));
  }

  @Test
  void verifySubareasTopologicallyEqualToBlock_whenBlockHasNoSubareas_thenWarnsAndSkips() {
    var logAppender = getLogAppender(MigrationValidationService.class);

    var parentFeature = FeatureTestUtil.newBuilder()
        .withFeatureName("Parent Block")
        .withLegacyId(7654321)
        .withAttributes(Map.of("LAYER", Layer.BLOCKS.name()))
        .build();

    var parentEntityBacked = new EntityBackedFeature(parentFeature, Map.of());

    when(featureService.findAllByAttribute("LAYER", Layer.BLOCKS.name())).thenReturn(List.of(parentFeature));
    when(featureService.getEntityBackedFeatures(List.of(parentFeature))).thenReturn(List.of(parentEntityBacked));
    when(featureService.findAllByAttribute("LAYER", Layer.SUBAREAS.name())).thenReturn(List.of());

    try {
      migrationValidationService.verifySubareasTopologicallyEqualToBlock();
    } finally {
      detachLogAppender(MigrationValidationService.class, logAppender);
    }

    assertThat(logAppender.list)
        .extracting(ILoggingEvent::getLevel, ILoggingEvent::getFormattedMessage)
        .containsExactly(tuple(
            Level.WARN,
            "Parent %s has no subareas".formatted(parentFeature.getLegacyId())
        ));
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void validateReferenceBlocks(boolean isValid) {
    var logAppender = getLogAppender(MigrationValidationService.class);

    var refBlockFeature = FeatureTestUtil.newBuilder()
        .withFeatureName("16/30")
        .withLegacyId(300)
        .withAttributes(Map.of("LAYER", Layer.OFFSHORE_REF_BLOCKS.name()))
        .build();
    var licenseBlockFeature = FeatureTestUtil.newBuilder()
        .withFeatureName("16/30a")
        .withAttributes(Map.of("LAYER", Layer.BLOCKS.name()))
        .build();
    var brokenLicenseBlockFeature = FeatureTestUtil.newBuilder()
        .withFeatureName(BROKEN_LICENSE_BLOCK_NAME)
        .withAttributes(Map.of("LAYER", Layer.BLOCKS.name()))
        .build();

    var refBlockEntityBacked = new EntityBackedFeature(refBlockFeature, Map.of());
    var licenseBlockEntityBacked = new EntityBackedFeature(licenseBlockFeature, Map.of());

    var refBlockLayers = List.of(
        Layer.OFFSHORE_CROP_REF_BLOCKS.name(),
        Layer.ONSHORE_CROP_REF_BLOCKS.name(),
        Layer.OFFSHORE_REF_BLOCKS.name()
    );
    when(featureService.findAllByAttribute("LAYER", Layer.BLOCKS.name()))
        .thenReturn(List.of(licenseBlockFeature, brokenLicenseBlockFeature));
    when(featureService.findAllByAttributeValueIn("LAYER", refBlockLayers)).thenReturn(List.of(refBlockFeature));
    when(featureService.getEntityBackedFeatures(List.of(refBlockFeature))).thenReturn(List.of(refBlockEntityBacked));
    when(featureService.getEntityBackedFeatures(List.of(licenseBlockFeature)))
        .thenReturn(List.of(licenseBlockEntityBacked));

    var response = ValidationResponse.newBuilder()
        .setIsValid(isValid)
        .setMessage("some message")
        .build();
    when(grpcClientService.validateReferenceBlock(refBlockEntityBacked, List.of(licenseBlockEntityBacked)))
        .thenReturn(response);

    try {
      migrationValidationService.validateReferenceBlocks();
    } finally {
      detachLogAppender(MigrationValidationService.class, logAppender);
    }

    assertThat(logAppender.list)
        .extracting(ILoggingEvent::getLevel, ILoggingEvent::getFormattedMessage)
        .containsExactly(tuple(
            isValid ? Level.INFO : Level.ERROR,
            isValid
                ? "All license blocks are contained by ref block %s".formatted(refBlockFeature.getLegacyId())
                : "Validation error: some message Reference Block: %s".formatted(refBlockFeature.getLegacyId())
        ));
  }
}
