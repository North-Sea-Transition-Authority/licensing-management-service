package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDurationValidationUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.PhaseType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTermService;

@Service
public class LicenceSchedulePhaseFormValidator {

  private final LicenceSchedulePhaseService licenceSchedulePhaseService;
  private final LicenceScheduleTermService licenceScheduleTermService;

  public LicenceSchedulePhaseFormValidator(
      LicenceSchedulePhaseService licenceSchedulePhaseService,
      LicenceScheduleTermService licenceScheduleTermService
  ) {
    this.licenceSchedulePhaseService = licenceSchedulePhaseService;
    this.licenceScheduleTermService = licenceScheduleTermService;
  }

  boolean isValid(
      LicenceSchedulePhaseForm form,
      Errors errors,
      LicenceScheduleDetail licenceScheduleDetail
  ) {
    var existingPhaseTypes = licenceSchedulePhaseService.getActivePhasesByLicenceScheduleDetail(licenceScheduleDetail).stream()
        .map(LicenceSchedulePhase::getPhaseType)
        .toList();

    return doValidation(
        form,
        errors,
        licenceScheduleDetail,
        existingPhaseTypes
    );
  }

  boolean isValidUpdate(
      LicenceSchedulePhaseForm form,
      Errors errors,
      LicenceScheduleDetail licenceScheduleDetail,
      LicenceSchedulePhase licenceSchedulePhase
  ) {
    var existingPhaseTypes = licenceSchedulePhaseService.getActivePhasesByLicenceScheduleDetail(licenceScheduleDetail).stream()
        .filter(phase -> !licenceSchedulePhase.getId().equals(phase.getId()))
        .map(LicenceSchedulePhase::getPhaseType)
        .toList();

    return doValidation(
        form,
        errors,
        licenceScheduleDetail,
        existingPhaseTypes
    );
  }

  boolean doValidation(
      LicenceSchedulePhaseForm form,
      Errors errors,
      LicenceScheduleDetail licenceScheduleDetail,
      List<PhaseType> existingPhaseTypes
  ) {
    ValidationUtils.rejectIfEmpty(errors, "phaseType", "phaseType.required", "Select a phase");

    if (form.getPhaseType() != null) {
      var existingTermTypes = licenceScheduleTermService.getActiveTermsByLicenceScheduleDetail(licenceScheduleDetail).stream()
          .map(LicenceScheduleTerm::getTermType)
          .toList();

      if (!existingTermTypes.contains(form.getPhaseType().getTermType())) {
        errors.rejectValue(
            "phaseType",
            "phaseType.invalid",
            "%s cannot be added to a licence schedule without an existing %s".formatted(
                form.getPhaseType().getDisplayName(),
                form.getPhaseType().getTermType().getDisplayName()
            )
        );
      }

      if (existingPhaseTypes.contains(form.getPhaseType())) {
        errors.rejectValue(
            "phaseType",
            "phaseType.invalid",
            "%s already exists on the licence schedule".formatted(form.getPhaseType().getDisplayName())
        );
      }
    }

    ThreeFieldDurationValidationUtil.validate(form.getPhaseDuration(), errors);

    return !errors.hasErrors();
  }
}
