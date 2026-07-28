package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDurationValidationUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.TermType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.common.ScheduleRelativeDateValidationService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;

@Service
public class LicenceScheduleTermFormValidator {

  private final LicenceScheduleTermService licenceScheduleTermService;
  private final ScheduleRelativeDateValidationService scheduleRelativeDateValidationService;

  public LicenceScheduleTermFormValidator(LicenceScheduleTermService licenceScheduleTermService,
                                          ScheduleRelativeDateValidationService scheduleRelativeDateValidationService
  ) {
    this.licenceScheduleTermService = licenceScheduleTermService;
    this.scheduleRelativeDateValidationService = scheduleRelativeDateValidationService;
  }

  boolean isValid(
      LicenceScheduleTermForm form,
      Errors errors,
      LicenceScheduleDetail licenceScheduleDetail
  ) {
    var existingTermTypes = licenceScheduleTermService.getTermsByLicenceScheduleDetail(licenceScheduleDetail).stream()
        .map(LicenceScheduleTerm::getTermType)
        .toList();

    doValidation(form, errors, existingTermTypes);
    return !errors.hasErrors();
  }

  boolean isValidUpdate(
      LicenceScheduleTermForm form,
      Errors errors,
      LicenceScheduleDetail licenceScheduleDetail,
      LicenceScheduleTerm licenceScheduleTerm
  ) {
    var existingTermTypes = licenceScheduleTermService.getTermsByLicenceScheduleDetail(licenceScheduleDetail).stream()
        .filter(term -> !licenceScheduleTerm.getId().equals(term.getId()))
        .map(LicenceScheduleTerm::getTermType)
        .toList();

    doValidation(form, errors, existingTermTypes);

    if (!errors.hasErrors()) {
      scheduleRelativeDateValidationService.validateTermLengthUpdate(
          licenceScheduleTerm,
          form.getTermDuration(),
          errors
      );
    }

    return !errors.hasErrors();
  }

  private void doValidation(
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


  }
}
