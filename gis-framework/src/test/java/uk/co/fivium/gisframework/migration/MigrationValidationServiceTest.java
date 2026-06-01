package uk.co.fivium.gisframework.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.co.fivium.gisframework.LoggerTestUtil.detachLogAppender;
import static uk.co.fivium.gisframework.LoggerTestUtil.getLogAppender;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.gisframework.feature.EntityBackedFeature;
import uk.co.fivium.gisframework.feature.FeatureService;
import uk.co.fivium.gisframework.feature.FeatureTestUtil;
import uk.co.fivium.gisframework.feature.LineService;
import uk.co.fivium.gisframework.feature.LineTestUtil;
import uk.co.fivium.gisframework.feature.PolygonService;
import uk.co.fivium.gisframework.feature.PolygonTestUtil;
import uk.co.fivium.gisframework.grpc.GrpcClientService;
import uk.co.fivium.gisframework.migration.configuration.BrokenBlockConfigurationProperties;
import uk.co.fivium.grpc.gis.ValidationResponse;

@ExtendWith(MockitoExtension.class)
class MigrationValidationServiceTest {

  private static final String BROKEN_LICENSE_BLOCK_NAME = "16/30c";

  @Mock
  private FeatureService featureService;

  @Mock
  private GrpcClientService grpcClientService;

  @Mock
  private LineService lineService;

  @Mock
  private PolygonService polygonService;

  private MigrationValidationService migrationValidationService;

  @BeforeEach
  void setUp() {
    migrationValidationService = new MigrationValidationService(
        featureService,
        grpcClientService,
        lineService,
        polygonService,
        new BrokenBlockConfigurationProperties(Map.of("16/30", List.of(BROKEN_LICENSE_BLOCK_NAME)))
    );
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void blockAndSubareaValidation(boolean isValid) {
    var parentFeature = FeatureTestUtil.newBuilder().withFeatureName("Parent").build();
    var childFeature1 = FeatureTestUtil.newBuilder().withFeatureName("Child 1").withParentFeature(parentFeature).build();
    var childFeature2 = FeatureTestUtil.newBuilder().withFeatureName("Child 2").withParentFeature(parentFeature).build();

    var childEntityBacked1 = new EntityBackedFeature(childFeature1, Map.of());
    var childEntityBacked2 = new EntityBackedFeature(childFeature2, Map.of());
    var parentEntityBacked = new EntityBackedFeature(parentFeature, Map.of());

    when(featureService.findAllChildFeatures()).thenReturn(List.of(childFeature1, childFeature2));
    when(featureService.getEntityBackedFeature(childFeature1)).thenReturn(childEntityBacked1);
    when(featureService.getEntityBackedFeature(childFeature2)).thenReturn(childEntityBacked2);
    when(featureService.getEntityBackedFeature(parentFeature)).thenReturn(parentEntityBacked);

    var response = ValidationResponse.newBuilder()
        .setIsValid(isValid)
        .setMessage("some message")
        .build();
    when(grpcClientService.validateBlockAndSubarea(childEntityBacked1, parentEntityBacked)).thenReturn(response);
    when(grpcClientService.validateBlockAndSubarea(childEntityBacked2, parentEntityBacked)).thenReturn(response);

    migrationValidationService.blockAndSubareaValidation();

    verify(featureService).findAllChildFeatures();
    verify(featureService).getEntityBackedFeature(childFeature1);
    verify(featureService).getEntityBackedFeature(childFeature2);
    verify(featureService, times(2)).getEntityBackedFeature(parentFeature);
    verify(grpcClientService).validateBlockAndSubarea(childEntityBacked1, parentEntityBacked);
    verify(grpcClientService).validateBlockAndSubarea(childEntityBacked2, parentEntityBacked);
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void verifySubareasTopologicallyEqualToBlock(boolean isValid) {
    var parentFeature = FeatureTestUtil.newBuilder()
        .withFeatureName("Parent Block")
        .withLegacyId(5610939)
        .withAttributes(Map.of("SHAPE_TYPE", "BLOCK"))
        .build();

    var childFeature1 = FeatureTestUtil.newBuilder().withFeatureName("Child 1").withParentFeature(parentFeature).build();
    var childFeature2 = FeatureTestUtil.newBuilder().withFeatureName("Child 2").withParentFeature(parentFeature).build();

    var polygon1 = PolygonTestUtil.newBuilder().withFeature(childFeature1).build();
    var polygon2 = PolygonTestUtil.newBuilder().withFeature(childFeature2).build();

    var line1 = LineTestUtil.newBuilder().withPolygon(polygon1).withEsriJson("line json 1").build();
    var line2 = LineTestUtil.newBuilder().withPolygon(polygon2).withEsriJson("line json 2").build();

    var parentEntityBacked = new EntityBackedFeature(parentFeature, Map.of());

    when(featureService.findAllByAttribute("SHAPE_TYPE", "BLOCK")).thenReturn(List.of(parentFeature));
    when(featureService.getEntityBackedFeature(parentFeature)).thenReturn(parentEntityBacked);
    when(featureService.findAllByParentFeature(parentFeature)).thenReturn(List.of(childFeature1, childFeature2));
    when(polygonService.findAllByFeatureIn(List.of(childFeature1, childFeature2))).thenReturn(List.of(polygon1, polygon2));
    when(lineService.findAllByPolygon(polygon1)).thenReturn(List.of(line1));
    when(lineService.findAllByPolygon(polygon2)).thenReturn(List.of(line2));

    var response = ValidationResponse.newBuilder()
        .setIsValid(isValid)
        .setMessage("some message")
        .build();
    when(grpcClientService.validateTopologicallyEqual(
        List.of(List.of("line json 1"), List.of("line json 2")),
        parentEntityBacked
    )).thenReturn(response);

    migrationValidationService.verifySubareasTopologicallyEqualToBlock();

    verify(featureService).findAllByAttribute("SHAPE_TYPE", "BLOCK");
    verify(featureService).getEntityBackedFeature(parentFeature);
    verify(featureService).findAllByParentFeature(parentFeature);
    verify(polygonService).findAllByFeatureIn(List.of(childFeature1, childFeature2));
    verify(lineService).findAllByPolygon(polygon1);
    verify(lineService).findAllByPolygon(polygon2);
    verify(grpcClientService).validateTopologicallyEqual(
        List.of(List.of("line json 1"), List.of("line json 2")),
        parentEntityBacked
    );
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void validateReferenceBlocks(boolean isValid) {
    var logAppender = getLogAppender(MigrationValidationService.class);

    var refBlockFeature = FeatureTestUtil.newBuilder()
        .withFeatureName("16/30")
        .withAttributes(Map.of("SHAPE_TYPE", "REF_BLOCK"))
        .build();
    var licenseBlockFeature = FeatureTestUtil.newBuilder()
        .withFeatureName("16/30a")
        .withAttributes(Map.of("SHAPE_TYPE", "BLOCK"))
        .build();
    var brokenLicenseBlockFeature = FeatureTestUtil.newBuilder()
        .withFeatureName(BROKEN_LICENSE_BLOCK_NAME)
        .withAttributes(Map.of("SHAPE_TYPE", "BLOCK"))
        .build();

    var refBlockEntityBacked = new EntityBackedFeature(refBlockFeature, Map.of());
    var licenseBlockEntityBacked = new EntityBackedFeature(licenseBlockFeature, Map.of());

    when(featureService.findAllByAttribute("SHAPE_TYPE", "REF_BLOCK")).thenReturn(List.of(refBlockFeature));
    when(featureService.findAllByAttribute("SHAPE_TYPE", "BLOCK"))
        .thenReturn(List.of(licenseBlockFeature, brokenLicenseBlockFeature));
    when(featureService.getEntityBackedFeature(refBlockFeature)).thenReturn(refBlockEntityBacked);
    when(featureService.getEntityBackedFeature(licenseBlockFeature)).thenReturn(licenseBlockEntityBacked);

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

    verify(featureService).findAllByAttribute("SHAPE_TYPE", "REF_BLOCK");
    verify(featureService).findAllByAttribute("SHAPE_TYPE", "BLOCK");
    verify(featureService).getEntityBackedFeature(refBlockFeature);
    verify(featureService).getEntityBackedFeature(licenseBlockFeature);
    verify(grpcClientService).validateReferenceBlock(refBlockEntityBacked, List.of(licenseBlockEntityBacked));

    assertThat(logAppender.list)
        .extracting(ILoggingEvent::getLevel, ILoggingEvent::getFormattedMessage)
        .containsExactly(tuple(
            isValid ? Level.INFO : Level.ERROR,
            isValid
                ? "All license blocks are contained by ref block 16/30"
                : "Validation error: some message Reference Block: 16/30"
        ));
  }
}
