package uk.co.fivium.gisframework.operator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.gisframework.feature.EntityBackedFeature;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.fivium.gisframework.feature.FeatureService;
import uk.co.fivium.gisframework.feature.FeatureTestUtil;

@ExtendWith(MockitoExtension.class)
class RenameOperatorServiceTest {

  private static final UUID FEATURE_A_ID = UUID.randomUUID();
  private static final UUID FEATURE_B_ID = UUID.randomUUID();

  @Mock
  private FeatureService featureService;

  @Mock
  private OperatorResultProcessingService operatorResultProcessingService;

  @InjectMocks
  private RenameOperatorService renameOperatorService;

  @Test
  void renameFeatures_whenNamesAreValid_thenReturnsRenamedCopies() {
    var featureA = buildFeature(FEATURE_A_ID, "A", "Alpha");
    var featureB = buildFeature(FEATURE_B_ID, "B", "Bravo");
    var entityBackedFeatureA = new EntityBackedFeature(featureA, Map.of());
    var entityBackedFeatureB = new EntityBackedFeature(featureB, Map.of());
    var outputFeatureA = FeatureTestUtil.newBuilder().build();
    var outputFeatureB = FeatureTestUtil.newBuilder().build();
    var renamesByFeatureId = Map.of(
        FEATURE_A_ID, new JsonFeatureRename(FEATURE_A_ID, "A2", "Alpha 2"),
        FEATURE_B_ID, new JsonFeatureRename(FEATURE_B_ID, "B2", "Bravo 2")
    );

    when(featureService.getEntityBackedFeatures(List.of(featureA, featureB)))
        .thenReturn(List.of(entityBackedFeatureA, entityBackedFeatureB));
    when(operatorResultProcessingService.processRenameFeature(entityBackedFeatureA, "A2", "Alpha 2"))
        .thenReturn(outputFeatureA);
    when(operatorResultProcessingService.processRenameFeature(entityBackedFeatureB, "B2", "Bravo 2"))
        .thenReturn(outputFeatureB);

    var result = renameOperatorService.renameFeatures(List.of(featureA, featureB), renamesByFeatureId);

    assertThat(result).containsExactly(outputFeatureA, outputFeatureB);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = " ")
  void renameFeatures_whenFeatureNameIsBlank_thenThrows(String featureName) {
    var featureA = buildFeature(FEATURE_A_ID, "A", "Alpha");
    var renamesByFeatureId = Map.of(FEATURE_A_ID, new JsonFeatureRename(FEATURE_A_ID, featureName, "Alpha 2"));

    assertThatThrownBy(() -> renameOperatorService.renameFeatures(List.of(featureA), renamesByFeatureId))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("A name and feature name must be provided for every feature to rename");

    verifyNoInteractions(featureService, operatorResultProcessingService);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = " ")
  void renameFeatures_whenNameIsBlank_thenThrows(String name) {
    var featureA = buildFeature(FEATURE_A_ID, "A", "Alpha");
    var renamesByFeatureId = Map.of(FEATURE_A_ID, new JsonFeatureRename(FEATURE_A_ID, "A2", name));

    assertThatThrownBy(() -> renameOperatorService.renameFeatures(List.of(featureA), renamesByFeatureId))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("A name and feature name must be provided for every feature to rename");

    verifyNoInteractions(featureService, operatorResultProcessingService);
  }

  @Test
  void renameFeatures_whenNamesAreUnchanged_thenThrows() {
    var featureA = buildFeature(FEATURE_A_ID, "A", "Alpha");
    var featureB = buildFeature(FEATURE_B_ID, "B", "Bravo");
    var renamesByFeatureId = Map.of(
        FEATURE_A_ID, new JsonFeatureRename(FEATURE_A_ID, "A2", "Alpha 2"),
        FEATURE_B_ID, new JsonFeatureRename(FEATURE_B_ID, " B ", " Bravo ")
    );

    assertThatThrownBy(() -> renameOperatorService.renameFeatures(List.of(featureA, featureB), renamesByFeatureId))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Each feature to rename must have a new name or feature name");

    verifyNoInteractions(featureService, operatorResultProcessingService);
  }

  @Test
  void renameFeatures_whenOnlyCaseChanges_thenRenames() {
    var featureA = buildFeature(FEATURE_A_ID, "A", "Alpha");
    var entityBackedFeatureA = new EntityBackedFeature(featureA, Map.of());
    var outputFeatureA = FeatureTestUtil.newBuilder().build();
    var renamesByFeatureId = Map.of(FEATURE_A_ID, new JsonFeatureRename(FEATURE_A_ID, "a", "ALPHA"));

    when(featureService.getEntityBackedFeatures(List.of(featureA))).thenReturn(List.of(entityBackedFeatureA));
    when(operatorResultProcessingService.processRenameFeature(entityBackedFeatureA, "a", "ALPHA"))
        .thenReturn(outputFeatureA);

    var result = renameOperatorService.renameFeatures(List.of(featureA), renamesByFeatureId);

    assertThat(result).containsExactly(outputFeatureA);
  }

  @Test
  void renameFeatures_whenNotEveryFeatureLoads_thenThrows() {
    var featureA = buildFeature(FEATURE_A_ID, "A", "Alpha");
    var featureB = buildFeature(FEATURE_B_ID, "B", "Bravo");
    var renamesByFeatureId = Map.of(
        FEATURE_A_ID, new JsonFeatureRename(FEATURE_A_ID, "A2", "Alpha 2"),
        FEATURE_B_ID, new JsonFeatureRename(FEATURE_B_ID, "B2", "Bravo 2")
    );

    when(featureService.getEntityBackedFeatures(List.of(featureA, featureB)))
        .thenReturn(List.of(new EntityBackedFeature(featureA, Map.of())));

    assertThatThrownBy(() -> renameOperatorService.renameFeatures(List.of(featureA, featureB), renamesByFeatureId))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Could not load the polygons and lines of every feature to rename");

    verifyNoInteractions(operatorResultProcessingService);
  }

  private Feature buildFeature(UUID id, String featureName, String name) {
    return FeatureTestUtil.newBuilder()
        .withId(id)
        .withFeatureName(featureName)
        .withAttributes(Map.of("NAME", name))
        .build();
  }
}
