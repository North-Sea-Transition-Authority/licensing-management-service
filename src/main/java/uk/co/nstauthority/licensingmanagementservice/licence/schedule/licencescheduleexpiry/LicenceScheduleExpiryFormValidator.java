package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleexpiry;

import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import uk.co.fivium.formlibrary.validator.date.ThreeFieldDateInputValidator;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencestartdate.LicenceStartDateService;

@Service
public class LicenceScheduleExpiryFormValidator {

  private final LicenceScheduleExpiryService licenceScheduleExpiryService;
  private final LicenceStartDateService licenceStartDateService;

  public LicenceScheduleExpiryFormValidator(
      LicenceScheduleExpiryService licenceScheduleExpiryService,
      LicenceStartDateService licenceStartDateService
  ) {
    this.licenceScheduleExpiryService = licenceScheduleExpiryService;
    this.licenceStartDateService = licenceStartDateService;
  }

  public boolean isValid(
      LicenceScheduleExpiryForm form,
      Errors errors,
      LicenceScheduleDetail licenceScheduleDetail
  ) {
    var expiryDates = licenceScheduleExpiryService.getAllActiveExpiryDatesByLicenceScheduleDetail(licenceScheduleDetail);

    if (!expiryDates.isEmpty()) {
      errors.reject("licenceScheduleExpiry.invalid", "You cannot add an expiry as one already exists on the licence");
    }

    validateDateInput(
        form,
        errors,
        licenceScheduleDetail
    );

    return !errors.hasErrors();
  }

  public boolean isValidUpdate(
      LicenceScheduleExpiryForm form,
      Errors errors,
      LicenceScheduleDetail licenceScheduleDetail
  ) {
    validateDateInput(
        form,
        errors,
        licenceScheduleDetail
    );

    return !errors.hasErrors();
  }

  private void validateDateInput(
      LicenceScheduleExpiryForm form,
      Errors errors,
      LicenceScheduleDetail licenceScheduleDetail
  ) {
    var startDate = licenceStartDateService.getByLicenceScheduleDetailOrThrow(licenceScheduleDetail);

    ThreeFieldDateInputValidator.builder()
        .emptyInputErrorMessage("Provide the expiry date")
        .mustBeAfterDate(startDate.getStartDate())
        .mustBeAfterDateErrorMessage("The expiry date must be after the licence start date")
        .validate(form.getExpiryDate(), errors);
  }
}
