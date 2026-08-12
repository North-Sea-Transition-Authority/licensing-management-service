package uk.co.nstauthority.licensingmanagementservice.licence.application;

import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import uk.co.nstauthority.licensingmanagementservice.phasedrelease.FeatureFlagService;

@Service
public class SelectApplicationTypeFormValidator {

  private final FeatureFlagService featureFlagService;

  public SelectApplicationTypeFormValidator(FeatureFlagService featureFlagService) {
    this.featureFlagService = featureFlagService;
  }

  boolean isValid(SelectApplicationTypeForm form, Errors errors) {
    ValidationUtils
        .rejectIfEmpty(errors, "selectedApplicationType", "selectedApplicationType.required", "Select what you are applying for");

    var selectedApplicationType = form.getSelectedApplicationType();
    // Backstop: an application type hidden from the radio list must also be rejected on submit
    if (selectedApplicationType != null && !featureFlagService.isEnabled(selectedApplicationType.getReleaseFeature())) {
      errors.rejectValue("selectedApplicationType", "selectedApplicationType.notAvailable", "Select a valid application type");
    }

    return !errors.hasErrors();
  }

}
