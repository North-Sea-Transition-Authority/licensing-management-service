package uk.co.nstauthority.licensingmanagementservice.licence;

import java.time.Clock;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import uk.co.fivium.formlibrary.validator.date.ThreeFieldDateInputValidator;
import uk.co.nstauthority.licensingmanagementservice.formatting.DateFormatUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.status.LicenceStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.status.LicenceStatusService;

@Service
public class EditLicenceDetailsValidator {

  private final Clock clock;
  private final LicenceStatusService licenceStatusService;

  public EditLicenceDetailsValidator(Clock clock, LicenceStatusService licenceStatusService) {
    this.clock = clock;
    this.licenceStatusService = licenceStatusService;
  }

  boolean isValid(EditLicenceDetailsForm form, Licence licence, Errors errors) {

    ValidationUtils.rejectIfEmptyOrWhitespace(
        errors,
        "licenceStatus",
        "licenceStatus.required",
        "Select the status of the licence"
    );

    var dateValidator = ThreeFieldDateInputValidator.builder()
        .emptyInputErrorMessage("Enter the date the licence entered this status")
        .mustBeBeforeOrEqualTo(LocalDate.now(clock))
        .mustBeBeforeOrEqualToErrorMessage("The date the licence entered this status must not be in the future");

    licenceStatusService.getLatestLicenceStatus(licence)
        .map(LicenceStatus::getStatusDate)
        .ifPresent(previousStatusDate -> dateValidator
            .mustBeAfterDate(previousStatusDate)
            .mustBeAfterDateErrorMessage("The date the licence entered this status must be after %s"
                .formatted(DateFormatUtil.convertToDisplayText(previousStatusDate))));

    dateValidator.validate(form.getLicenceStatusDate(), errors);

    if (CollectionUtils.isEmpty(form.getOrganisationUnitIds())) {
      errors.rejectValue("organisationUnitSelector", "organisationUnitSelector.notEmpty",
          "You must add at least one licensee");
    }

    return !errors.hasErrors();
  }

}
