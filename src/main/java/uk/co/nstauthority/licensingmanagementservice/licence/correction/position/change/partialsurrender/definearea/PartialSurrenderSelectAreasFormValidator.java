package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.definearea;

import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.fivium.gisframework.relationship.FeatureRelationshipService;

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
      Feature blockFeature
  ) {
    var featureIds = activeFeatures.stream().map(Feature::getId).collect(Collectors.toSet());
    var selected = form.getSurrenderedFeatureIds();

    if (CollectionUtils.isEmpty(selected) || !featureIds.containsAll(selected)) {
      errors.rejectValue(
          SURRENDERED_FEATURE_IDS_FIELD,
          "surrenderedFeatureIds.required",
          "Select the areas being surrendered"
      );
    } else if (selected.size() >= featureIds.size()) {
      errors.rejectValue(
          SURRENDERED_FEATURE_IDS_FIELD,
          "surrenderedFeatureIds.invalid",
          "You cannot surrender all the areas in a partial surrender"
      );
    } else if (featureRelationshipService.isTopologicallyEqual(
        activeFeatures.stream().filter(feature -> selected.contains(feature.getId())).toList(),
        blockFeature
    )) {
      errors.rejectValue(
          SURRENDERED_FEATURE_IDS_FIELD,
          "surrenderedFeatureIds.wholeBlock",
          "Select areas that do not cover the whole block"
      );
    }

    return errors.hasErrors();
  }
}
