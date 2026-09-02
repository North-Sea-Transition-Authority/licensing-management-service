package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.subarea;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import uk.co.fivium.gisframework.feature.Feature;

@Service
public class SubareaChangeStartFormValidator {

  private static final String FEATURE_ID_FIELD = "featureId";
  private static final String FEATURE_ID_ERROR = "Select the licence block to change";

  public boolean hasErrors(
      SubareaChangeStartForm form,
      Errors errors,
      List<Feature> blockFeatures
  ) {
    if (StringUtils.isBlank(form.getFeatureId())) {
      errors.rejectValue(FEATURE_ID_FIELD, "featureId.required", FEATURE_ID_ERROR);
    } else if (!blockFeatureIds(blockFeatures).contains(form.getFeatureId())) {
      errors.rejectValue(FEATURE_ID_FIELD, "featureId.invalid", FEATURE_ID_ERROR);
    }

    return errors.hasErrors();
  }

  private Set<String> blockFeatureIds(List<Feature> blockFeatures) {
    return blockFeatures.stream()
        .map(Feature::getId)
        .map(UUID::toString)
        .collect(Collectors.toSet());
  }
}
