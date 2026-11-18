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

  private static final String TERM_TYPE_FIELD = "termType";

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

    doValidation(form, errors, licenceScheduleDetail, existingTermTypes);
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

    doValidation(form, errors, licenceScheduleDetail, existingTermTypes);

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
      LicenceScheduleDetail licenceScheduleDetail,
      List<TermType> existingTermTypes
  ) {
    ValidationUtils.rejectIfEmpty(errors, TERM_TYPE_FIELD, "termType.required", "Select a term type");
    if (form.getTermType() != null
        && !licenceScheduleTermService.getSelectableTermTypes(licenceScheduleDetail).contains(form.getTermType())) {
      errors.rejectValue(TERM_TYPE_FIELD, "termType.invalid", "Select a term type");
    } else if (form.getTermType() != null && existingTermTypes.contains(form.getTermType())) {
      errors.rejectValue(
          TERM_TYPE_FIELD,
          "termType.invalid",
          "%s already exists on the licence schedule".formatted(form.getTermType().getDisplayName())
      );
    }

    ThreeFieldDurationValidationUtil.validate(form.getTermDuration(), errors);


  }
}
