package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm;

import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDurationValidationUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;

@Service
public class LicenceScheduleTermFormValidator {

  private final LicenceScheduleTermService licenceScheduleTermService;

  public LicenceScheduleTermFormValidator(LicenceScheduleTermService licenceScheduleTermService) {
    this.licenceScheduleTermService = licenceScheduleTermService;
  }

  boolean isValid(
      LicenceScheduleTermForm form,
      Errors errors,
      LicenceScheduleDetail licenceScheduleDetail
  ) {
    ValidationUtils.rejectIfEmpty(errors, "termType", "termType.required", "Select a term type");

    if (form.getTermType() != null) {
      var existingTermTypes = licenceScheduleTermService.getTermsByLicenceScheduleDetail(licenceScheduleDetail).stream()
           .map(LicenceScheduleTerm::getTermType)
           .toList();

      if (existingTermTypes.contains(form.getTermType())) {
        errors.rejectValue(
            "termType",
            "termType.invalid",
            "%s already exists on the schedule".formatted(form.getTermType().getDisplayName())
        );
      }
    }

    ThreeFieldDurationValidationUtil.validate(form.getTermDuration(), errors);

    return !errors.hasErrors();
  }
}
