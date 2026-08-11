package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.Errors;
import uk.co.fivium.gisframework.feature.Feature;

@Service
public class PartialSurrenderDetailsFormValidator {

  private static final String FEATURE_IDS_FIELD = "featureIds";
  private static final String FEATURE_IDS_ERROR = "Select the licence blocks being surrendered";

  public boolean hasErrors(
      PartialSurrenderDetailsForm form,
      Errors errors,
      List<Feature> surrenderableBlockFeatures
  ) {
    if (CollectionUtils.isEmpty(form.getFeatureIds())) {
      errors.rejectValue(FEATURE_IDS_FIELD, "featureIds.required", FEATURE_IDS_ERROR);
    } else if (!surrenderableFeatureIds(surrenderableBlockFeatures).containsAll(form.getFeatureIds())) {
      errors.rejectValue(FEATURE_IDS_FIELD, "featureIds.invalid", FEATURE_IDS_ERROR);
    }

    return errors.hasErrors();
  }

  private Set<UUID> surrenderableFeatureIds(List<Feature> blockFeatures) {
    return blockFeatures.stream()
        .map(Feature::getId)
        .collect(Collectors.toSet());
  }
}
