package uk.co.fivium.gisframework.feature;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.gisframework.migration.configuration.BrokenBlockConfigurationProperties;
import uk.co.fivium.gisframework.migration.oracle.Layer;

@ExtendWith(MockitoExtension.class)
class FeatureServiceTest {

  private static final Feature FEATURE = FeatureTestUtil.newBuilder().build();

  @Mock
  private FeatureRepository featureRepository;

  @Mock
  private LineService lineService;

  private FeatureService featureService;

  @BeforeEach
  void setUp() {
    featureService = new FeatureService(
        featureRepository,
        lineService,
        new BrokenBlockConfigurationProperties(Map.of("16/30", List.of("16/29c")))
    );
  }

  @Test
  void saveFeature() {
    featureService.saveFeature(FEATURE);

    verify(featureRepository).save(FEATURE);
  }

  @Test
  void findAllByParentFeature() {
    var childFeature1 = FeatureTestUtil.newBuilder().build();
    var childFeature2 = FeatureTestUtil.newBuilder().build();

    when(featureRepository.findAllByParentFeatureId(FEATURE.getId()))
        .thenReturn(List.of(childFeature1, childFeature2));

    var result = featureService.findAllByParentFeature(FEATURE);

    var expected = List.of(childFeature1, childFeature2);
    assertThat(result).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void findAllByParentFeature_whenNoResult_thenReturnsEmptyList() {
    when(featureRepository.findAllByParentFeatureId(FEATURE.getId()))
        .thenReturn(List.of());

    var result = featureService.findAllByParentFeature(FEATURE);

    assertThat(result).isEmpty();
  }

  @Test
  void findAllByAttribute() {
    when(featureRepository.findAllByAttribute("key", "value")).thenReturn(List.of(FEATURE));

    assertThat(featureService.findAllByAttribute("key", "value")).isEqualTo(List.of(FEATURE));
  }

  @Test
  void getEntityBackedFeature() {
    var polygon1 = PolygonTestUtil.newBuilder().withFeature(FEATURE).build();
    var polygon2 = PolygonTestUtil.newBuilder().withFeature(FEATURE).build();

    var line1 = LineTestUtil.newBuilder().withPolygon(polygon1).build();
    var line2 = LineTestUtil.newBuilder().withPolygon(polygon2).build();

    var polygonToLines = Map.of(polygon1, List.of(line1), polygon2, List.of(line2));
    when(lineService.getPolygonToLines(FEATURE)).thenReturn(polygonToLines);

    var result = featureService.getEntityBackedFeature(FEATURE);

    var expected = new EntityBackedFeature(FEATURE, polygonToLines);
    assertThat(result).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void getEntityBackedFeatures() {
    var feature1 = FeatureTestUtil.newBuilder().build();
    var feature2 = FeatureTestUtil.newBuilder().build();
    var features = List.of(feature1, feature2);

    var polygon1 = PolygonTestUtil.newBuilder().withFeature(feature1).build();
    var polygon2 = PolygonTestUtil.newBuilder().withFeature(feature1).build();
    var polygon3 = PolygonTestUtil.newBuilder().withFeature(feature2).build();

    var line1 = LineTestUtil.newBuilder().withPolygon(polygon1).build();
    var line2 = LineTestUtil.newBuilder().withPolygon(polygon2).build();
    var line3 = LineTestUtil.newBuilder().withPolygon(polygon3).build();

    when(lineService.getPolygonToLinesIn(features)).thenReturn(Map.of(
        polygon1, List.of(line1),
        polygon2, List.of(line2),
        polygon3, List.of(line3)
    ));

    var result = featureService.getEntityBackedFeatures(features);

    assertThat(result).containsExactlyInAnyOrder(
        new EntityBackedFeature(feature1, Map.of(polygon1, List.of(line1), polygon2, List.of(line2))),
        new EntityBackedFeature(feature2, Map.of(polygon3, List.of(line3)))
    );
  }

  @Test
  void getByLegacyId() {
    when(featureRepository.findByLegacyId(FEATURE.getLegacyId()))
        .thenReturn(Optional.of(FEATURE));

    var result = featureService.getByLegacyId(FEATURE.getLegacyId());

    assertThat(result).usingRecursiveComparison().isEqualTo(FEATURE);
  }

  @Test
  void getByLegacyId_whenNothing_thenThrow() {
    var legacyId = FEATURE.getLegacyId();
    when(featureRepository.findByLegacyId(legacyId))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> featureService.getByLegacyId(legacyId))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessage("Unable to find parent feature for shape %s"
            .formatted(FEATURE.getLegacyId()));
  }

  @Test
  void findAllByLegacyIdIn() {
    var feature1 = FeatureTestUtil.newBuilder().withLegacyId(1).build();
    var feature2 = FeatureTestUtil.newBuilder().withLegacyId(2).build();
    var legacyIds = List.of(1, 2);

    when(featureRepository.findAllByLegacyIdIn(legacyIds)).thenReturn(List.of(feature1, feature2));

    assertThat(featureService.findAllByLegacyIdIn(legacyIds)).isEqualTo(List.of(feature1, feature2));
  }

  @Test
  void deleteAll() {
    featureService.deleteAll();
    var inOrder = Mockito.inOrder(featureRepository);
    inOrder.verify(featureRepository).deleteAllByParentFeatureIsNotNull();
    inOrder.verify(featureRepository).deleteAll();
  }

  @Test
  void findLicenseBlocksForRefBlock() {
    var referenceBlock = FeatureTestUtil.newBuilder()
        .withFeatureName("16/30")
        .withAttributes(Map.of("LAYER", Layer.OFFSHORE_REF_BLOCKS.name(), "QUADRANT_NO", "16", "BLOCK_NO", "30"))
        .build();

    var matchingBlock = FeatureTestUtil.newBuilder()
        .withFeatureName("16/30a")
        .withAttributes(Map.of("LAYER", Layer.BLOCKS.name(), "QUADRANT_NO", "16", "BLOCK_NO", "30"))
        .build();
    var matchingBrokenBlock = FeatureTestUtil.newBuilder()
        .withFeatureName("16/29c")
        .withAttributes(Map.of("LAYER", Layer.BLOCKS.name(), "QUADRANT_NO", "16", "BLOCK_NO", "29"))
        .build();
    var nonMatchingBlock = FeatureTestUtil.newBuilder()
        .withFeatureName("16/31a")
        .withAttributes(Map.of("LAYER", Layer.BLOCKS.name(), "QUADRANT_NO", "16", "BLOCK_NO", "31"))
        .build();
    var nonBlockLayerFeature = FeatureTestUtil.newBuilder()
        .withFeatureName("16/30")
        .withAttributes(Map.of("LAYER", Layer.SUBAREAS.name(), "QUADRANT_NO", "16", "BLOCK_NO", "30"))
        .build();
    var featureMissingQuadrantNo = FeatureTestUtil.newBuilder()
        .withFeatureName("16/30b")
        .withAttributes(Map.of("BLOCK_NO", "30"))
        .build();

    var featureMissingBlockNo = FeatureTestUtil.newBuilder()
        .withFeatureName("16/30b")
        .withAttributes(Map.of("QUADRANT_NO", "16"))
        .build();

    var result = featureService.findLicenseBlocksForRefBlock(
        referenceBlock,
        List.of(
            matchingBlock,
            matchingBrokenBlock,
            nonMatchingBlock,
            nonBlockLayerFeature,
            featureMissingQuadrantNo,
            featureMissingBlockNo
        )
    );

    assertThat(result).containsExactly(matchingBlock, matchingBrokenBlock);
  }

  @Test
  void getFeatureOrThrow_entityFound() {
    var feature = FeatureTestUtil.newBuilder().build();
    when(featureRepository.findById(feature.getId())).thenReturn(Optional.of(feature));
    assertThat(featureService.getFeatureOrThrow(feature.getId())).isEqualTo(feature);
  }

  @Test
  void getFeatureOrThrow_entityNotFound_throw() {
    var featureId = UUID.randomUUID();
    when(featureRepository.findById(featureId)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> featureService.getFeatureOrThrow(featureId))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessage("Feature %s not found".formatted(featureId));
  }
}
