package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.extendjourney;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDurationInput;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDurationValidationUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;

@Service
public class LicenceScheduleExtensionFormValidator {

  private final LicenceScheduleExtensionService licenceScheduleExtensionFormService;

  public LicenceScheduleExtensionFormValidator(LicenceScheduleExtensionService licenceScheduleExtensionFormService) {
    this.licenceScheduleExtensionFormService = licenceScheduleExtensionFormService;
  }

  boolean isValid(
      LicenceScheduleExtensionForm form,
      BindingResult bindingResult,
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail
  ) {

    initializeCheckboxes(form, scheduleWorkProgrammeApplicationDetail);

    if (form.getExtensionDuration().size() > 1) {
      validateTermOrPhaseSelected(form, bindingResult, scheduleWorkProgrammeApplicationDetail);
    }

    form.getExtensionDuration().entrySet().stream()
        .filter(entry -> {

          if (entry.getValue() == null) {
            return false;
          }

          String key = entry.getKey();
          boolean isSelected = getSelectedStatus(form, key);
          boolean isSingleValue = form.getExtensionDuration().size() == 1;
          return isSelected || isSingleValue;
        })
        .forEach(entry -> ThreeFieldDurationValidationUtil.validate(entry.getValue(), bindingResult));

    return !bindingResult.hasErrors();
  }

  private void initializeCheckboxes(
      LicenceScheduleExtensionForm form,
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail
  ) {

    Map<String, ThreeFieldDurationInput> submittedDurationMap = form.getExtensionDuration();

    var validSchedule = licenceScheduleExtensionFormService.getExtendableTermAndPhases(
        scheduleWorkProgrammeApplicationDetail.getScheduleWorkProgrammeApplication().getLicenceScheduleDetail());

    var newLicenceScheduleExtensionForm = licenceScheduleExtensionFormService.getNewLicenceScheduleExtensionForm(validSchedule);

    if (form.getSelectedPhase() == null) {
      form.setSelectedPhase(new HashMap<>());
    }
    if (form.getSelectedTerm() == null) {
      form.setSelectedTerm(new HashMap<>());
    }

    newLicenceScheduleExtensionForm.getSelectedPhase()
                                   .forEach((key, defaultValue) -> form.getSelectedPhase().putIfAbsent(key, defaultValue));

    newLicenceScheduleExtensionForm.getSelectedTerm()
                                   .forEach((key, defaultValue) -> form.getSelectedTerm().putIfAbsent(key, defaultValue));

    form.setExtensionDuration(submittedDurationMap);
  }

  private void validateTermOrPhaseSelected(
      LicenceScheduleExtensionForm form, Errors errors,
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail
  ) {

    var validSchedule = licenceScheduleExtensionFormService.getExtendableTermAndPhases(
        scheduleWorkProgrammeApplicationDetail.getScheduleWorkProgrammeApplication().getLicenceScheduleDetail());

    var hasSelectedPhase = form.getSelectedPhase() != null
        && form.getSelectedPhase().values().stream().anyMatch(Boolean.TRUE::equals);
    var hasSelectedTerm = form.getSelectedTerm() != null
        && form.getSelectedTerm().values().stream().anyMatch(Boolean.TRUE::equals);

    var onlyTermsOptionsAvailable = validSchedule.stream().anyMatch(term -> term.termId() != null);
    var onlyPhaseOptionsAvailable = validSchedule.stream().anyMatch(term -> !term.phases().isEmpty());

    var selectionMade = hasSelectedTerm || hasSelectedPhase;
    var bothOptionsAvailable = onlyTermsOptionsAvailable && onlyPhaseOptionsAvailable;


    if (selectionMade) {
      return;
    }

    if (bothOptionsAvailable) {
      errors.rejectValue(
          "selectedTerm",
          "selectedTerm.required",
          "You must select at least one Phase or Term to request extension."
      );

    } else if (onlyTermsOptionsAvailable) {
      errors.rejectValue(
          "selectedTerm",
          "selectedTerm.required",
          "You must select at least one term to request extension."
      );
    } else if (onlyPhaseOptionsAvailable) {
      errors.rejectValue(
          "selectedPhase",
          "selectedPhase.required",
          "You must select at least one phase to request extension."
      );
    }
  }

  private boolean getSelectedStatus(LicenceScheduleExtensionForm form, String key) {
    var possiblePhase = Optional.ofNullable(form.getSelectedPhase())
                                .map(phase -> phase.get(key));

    var possibleTerm = Optional.ofNullable(form.getSelectedTerm())
                               .map(term -> term.get(key));

    return possiblePhase
        .or(() -> possibleTerm)
        .orElse(false);
  }
}