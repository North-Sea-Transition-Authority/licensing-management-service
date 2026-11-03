package uk.co.fivium.gisframework.operator.rename;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.fivium.gisframework.feature.FeatureService;
import uk.co.fivium.gisframework.operator.OperatorResultProcessingService;

@Service
public class RenameOperatorService {

  private final FeatureService featureService;
  private final OperatorResultProcessingService operatorResultProcessingService;

  public RenameOperatorService(
      FeatureService featureService,
      OperatorResultProcessingService operatorResultProcessingService
  ) {
    this.featureService = featureService;
    this.operatorResultProcessingService = operatorResultProcessingService;
  }

  /**
   * Copies each input feature under its new names.
   *
   * @param inputFeatures      the features to rename
   * @param renamesByFeatureId the new names for each input feature
   * @return the renamed copies of the input features.
   */
  @Transactional
  public List<Feature> renameFeatures(
      List<Feature> inputFeatures,
      Map<UUID, JsonFeatureRename> renamesByFeatureId
  ) {
    verifyNamesProvided(renamesByFeatureId.values());
    verifyNamesChanged(inputFeatures, renamesByFeatureId);

    var entityBackedFeatures = featureService.getEntityBackedFeatures(inputFeatures);

    if (entityBackedFeatures.size() != inputFeatures.size()) {
      throw new IllegalStateException("Could not load the polygons and lines of every feature to rename");
    }

    return entityBackedFeatures.stream()
        .map(entityBackedFeature -> {
          var featureRename = renamesByFeatureId.get(entityBackedFeature.feature().getId());
          return operatorResultProcessingService.processRenameFeature(
              entityBackedFeature,
              featureRename.featureName(),
              featureRename.name()
          );
        })
        .toList();
  }

  private void verifyNamesProvided(Collection<JsonFeatureRename> featureRenames) {
    if (featureRenames.stream()
        .anyMatch(rename -> !StringUtils.hasText(rename.featureName()) || !StringUtils.hasText(rename.name()))) {
      throw new IllegalArgumentException("A name and feature name must be provided for every feature to rename");
    }
  }

  private void verifyNamesChanged(List<Feature> inputFeatures, Map<UUID, JsonFeatureRename> renamesByFeatureId) {
    var anyUnchanged = inputFeatures.stream().anyMatch(feature -> {
      var featureRename = renamesByFeatureId.get(feature.getId());
      return Objects.equals(feature.getFeatureName(), featureRename.featureName())
          && Objects.equals(feature.getAttributes().get("NAME"), featureRename.name());
    });

    if (anyUnchanged) {
      throw new IllegalArgumentException("Each feature to rename must have a new name or feature name");
    }
  }
}
