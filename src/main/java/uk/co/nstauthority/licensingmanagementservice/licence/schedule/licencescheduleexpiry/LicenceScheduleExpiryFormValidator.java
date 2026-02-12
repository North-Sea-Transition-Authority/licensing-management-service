package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleexpiry;

import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import uk.co.fivium.formlibrary.validator.date.ThreeFieldDateInputValidator;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencestartdate.LicenceStartDateService;

@Service
public class LicenceScheduleExpiryFormValidator {

  private final LicenceStartDateService licenceStartDateService;

  public LicenceScheduleExpiryFormValidator(
      LicenceStartDateService licenceStartDateService
  ) {
    this.licenceStartDateService = licenceStartDateService;
  }

  public boolean isValid(
      LicenceScheduleExpiryForm form,
      Errors errors,
      LicenceScheduleDetail licenceScheduleDetail
  ) {
    var startDate = licenceStartDateService.getByLicenceScheduleDetailOrThrow(licenceScheduleDetail);

    ThreeFieldDateInputValidator.builder()
        .isOptional()
        .mustBeAfterDate(startDate.getStartDate())
        .mustBeAfterDateErrorMessage("The expiry date must be after the licence start date")
        .validate(form.getExpiryDate(), errors);

    return !errors.hasErrors();
  }
}
