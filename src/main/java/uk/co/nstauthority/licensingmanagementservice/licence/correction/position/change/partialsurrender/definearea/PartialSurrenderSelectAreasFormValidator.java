package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.definearea;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.fivium.gisframework.relationship.FeatureRelationshipService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.PartialSurrenderOperationRule;

@Service
public class PartialSurrenderSelectAreasFormValidator {

  private static final String SURRENDERED_FEATURE_IDS_FIELD = "surrenderedFeatureIds";

  private final FeatureRelationshipService featureRelationshipService;

  PartialSurrenderSelectAreasFormValidator(FeatureRelationshipService featureRelationshipService) {
    this.featureRelationshipService = featureRelationshipService;
  }

  public boolean hasErrors(
      PartialSurrenderSelectAreasForm form,
      Errors errors,
      List<Feature> activeFeatures,
      Feature blockFeature,
      Collection<Feature> otherActiveBlocks
  ) {
    var featureIds = activeFeatures.stream().map(Feature::getId).collect(Collectors.toSet());
    var selected = form.getSurrenderedFeatureIds();

    if (CollectionUtils.isEmpty(selected) || !featureIds.containsAll(selected)) {
      errors.rejectValue(
          SURRENDERED_FEATURE_IDS_FIELD,
          "surrenderedFeatureIds.required",
          PartialSurrenderOperationRule.AREAS_REQUIRED.getMessage()
      );
    } else if (selected.size() >= featureIds.size()) {
      errors.rejectValue(
          SURRENDERED_FEATURE_IDS_FIELD,
          "surrenderedFeatureIds.invalid",
          PartialSurrenderOperationRule.CANNOT_SURRENDER_ALL_AREAS.getMessage()
      );
    } else if (featureRelationshipService.is2dTopologicallyEqual(
        activeFeatures.stream().filter(feature -> selected.contains(feature.getId())).toList(),
        blockFeature
    )) {
      errors.rejectValue(
          SURRENDERED_FEATURE_IDS_FIELD,
          "surrenderedFeatureIds.wholeBlock",
          PartialSurrenderOperationRule.CANNOT_SURRENDER_WHOLE_BLOCK.getMessage()
      );
    } else {
      var keptAreas = activeFeatures.stream().filter(feature -> !selected.contains(feature.getId())).toList();
      getOverlapErrors(keptAreas, otherActiveBlocks).forEach(message -> errors.rejectValue(
          SURRENDERED_FEATURE_IDS_FIELD,
          "surrenderedFeatureIds.overlap",
          message
      ));
    }

    return errors.hasErrors();
  }

  public List<String> getOverlapErrors(
      Collection<Feature> keptAreas,
      Collection<Feature> otherActiveBlocks
  ) {
    return keptAreas.stream()
        .collect(Collectors.groupingBy(Feature::getCoordinateSystem))
        .entrySet()
        .stream()
        .flatMap(entry -> featureRelationshipService.get2dOverlappingFeatures(
            entry.getValue(),
            otherActiveBlocks.stream()
                .filter(block -> block.getCoordinateSystem() == entry.getKey())
                .toList()
        ).stream())
        .distinct()
        .map(overlappedBlock -> PartialSurrenderOperationRule.KEPT_AREA_OVERLAPS_BLOCK
            .getMessage(overlappedBlock.getFeatureName()))
        .toList();
  }
}
