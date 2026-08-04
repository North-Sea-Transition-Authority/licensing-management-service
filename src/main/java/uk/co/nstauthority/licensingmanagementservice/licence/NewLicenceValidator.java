package uk.co.nstauthority.licensingmanagementservice.licence;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;

@Service
public class NewLicenceValidator {

  private final LicenceService licenceService;

  public NewLicenceValidator(LicenceService licenceService) {
    this.licenceService = licenceService;
  }

  boolean isValid(NewLicenceForm form, Errors errors) {
    ValidationUtils.rejectIfEmptyOrWhitespace(
        errors,
        "licenceType",
        "licenceType.required",
        "Select the licence type"
    );

    ValidationUtils.rejectIfEmptyOrWhitespace(
        errors,
        "licenceNumber",
        "licenceNumber.required",
        "Enter the licence number"
    );

    if (StringUtils.isNotBlank(form.getLicenceNumber())
        && !form.getLicenceNumber().matches("^\\d.*")) {
      errors.rejectValue("licenceNumber", "licenceNumber.invalid", "The licence number must start with a digit");
    }

    if (form.getLicenceType() != null && StringUtils.isNotBlank(form.getLicenceNumber())
        && licenceService.licenceNumberExistsForType(form.getLicenceType(), form.getLicenceNumber())) {

      errors.rejectValue("licenceNumber", "licenceNumber.invalid",
          "The licence number already exists for the selected licence type");
    }

    if (CollectionUtils.isEmpty(form.getOrganisationUnitIds())) {
      errors.rejectValue("organisationUnitSelector", "organisationUnitSelector.notEmpty",
          "You must add at least one licensee");
    }

    return !errors.hasErrors();
  }

}
