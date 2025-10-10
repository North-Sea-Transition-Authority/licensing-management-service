package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDurationValidationUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.TermType;
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
    var existingTermTypes = licenceScheduleTermService.getActiveTermsByLicenceScheduleDetail(licenceScheduleDetail).stream()
        .map(LicenceScheduleTerm::getTermType)
        .toList();

    return doValidation(form, errors, existingTermTypes);
  }

  boolean isValidUpdate(
      LicenceScheduleTermForm form,
      Errors errors,
      LicenceScheduleDetail licenceScheduleDetail,
      LicenceScheduleTerm licenceScheduleTerm
  ) {
    var existingTermTypes = licenceScheduleTermService.getActiveTermsByLicenceScheduleDetail(licenceScheduleDetail).stream()
        .filter(term -> !licenceScheduleTerm.getId().equals(term.getId()))
        .map(LicenceScheduleTerm::getTermType)
        .toList();

    return doValidation(form, errors, existingTermTypes);
  }

  private boolean doValidation(
      LicenceScheduleTermForm form,
      Errors errors,
      List<TermType> existingTermTypes
  ) {
    ValidationUtils.rejectIfEmpty(errors, "termType", "termType.required", "Select a term type");
    if (form.getTermType() != null && existingTermTypes.contains(form.getTermType())) {
      errors.rejectValue(
          "termType",
          "termType.invalid",
          "%s already exists on the licence schedule".formatted(form.getTermType().getDisplayName())
      );
    }

    ThreeFieldDurationValidationUtil.validate(form.getTermDuration(), errors);

    return !errors.hasErrors();
  }
}
