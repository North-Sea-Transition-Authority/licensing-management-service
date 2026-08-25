package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.definearea;

import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import uk.co.fivium.gisframework.feature.Feature;

@Service
public class PartialSurrenderSelectAreasFormValidator {

  public boolean hasErrors(PartialSurrenderSelectAreasForm form, Errors errors, List<Feature> activeFeatures) {
    var featureIds = activeFeatures.stream().map(Feature::getId).collect(Collectors.toSet());
    var selected = form.getSurrenderedFeatureIds();

    if (CollectionUtils.isEmpty(selected) || !featureIds.containsAll(selected)) {
      errors.rejectValue(
          "surrenderedFeatureIds",
          "surrenderedFeatureIds.required",
          "Select the areas being surrendered"
      );
    } else if (selected.size() >= featureIds.size()) {
      errors.rejectValue(
          "surrenderedFeatureIds",
          "surrenderedFeatureIds.invalid",
          "You cannot surrender all the areas in a partial surrender"
      );
    }

    return errors.hasErrors();
  }
}
