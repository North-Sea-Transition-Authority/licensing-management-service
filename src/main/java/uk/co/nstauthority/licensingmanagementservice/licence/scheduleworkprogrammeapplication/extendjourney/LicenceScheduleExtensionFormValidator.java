package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.extendjourney;

import java.util.HashMap;
import java.util.List;
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

  private final LicenceScheduleExtensionService licenceScheduleExtensionService;

  public LicenceScheduleExtensionFormValidator(LicenceScheduleExtensionService licenceScheduleExtensionService) {
    this.licenceScheduleExtensionService = licenceScheduleExtensionService;
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

    List<LicenceScheduleExtensionRequestView> views = licenceScheduleExtensionService
        .getLicenceScheduleExtensionViews(scheduleWorkProgrammeApplicationDetail);

    if (form.getSelectedPhase() == null) {
      form.setSelectedPhase(new HashMap<>());
    }
    if (form.getSelectedTerm() == null) {
      form.setSelectedTerm(new HashMap<>());
    }

    for (LicenceScheduleExtensionRequestView view : views) {
      if (view.isPhase()) {
        form.getSelectedPhase().putIfAbsent(view.id(), false);
      } else {
        form.getSelectedTerm().putIfAbsent(view.id(), false);
      }
    }

    form.setExtensionDuration(submittedDurationMap);
  }

  private void validateTermOrPhaseSelected(
      LicenceScheduleExtensionForm form, Errors errors,
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail
  ) {

    var extensionRequestViews = licenceScheduleExtensionService
        .getLicenceScheduleExtensionViews(scheduleWorkProgrammeApplicationDetail);

    var hasSelectedPhase = form.getSelectedPhase() != null
        && form.getSelectedPhase().values().stream().anyMatch(Boolean.TRUE::equals);
    var hasSelectedTerm = form.getSelectedTerm() != null
        && form.getSelectedTerm().values().stream().anyMatch(Boolean.TRUE::equals);

    var onlyTermsOptionsAvailable = extensionRequestViews.stream().anyMatch(view -> !view.isPhase());
    var onlyPhaseOptionsAvailable = extensionRequestViews.stream().anyMatch(LicenceScheduleExtensionRequestView::isPhase);

    var selectionMade = hasSelectedTerm || hasSelectedPhase;
    var bothOptionsAvailable = onlyTermsOptionsAvailable && onlyPhaseOptionsAvailable;


    if (selectionMade) {
      return;
    }

    if (bothOptionsAvailable) {
      errors.rejectValue(
          "selectedTerm",
          "selectedTerm.required",
          "Select at least one Phase or Term to request extension"
      );

    } else if (onlyTermsOptionsAvailable) {
      errors.rejectValue(
          "selectedTerm",
          "selectedTerm.required",
          "Select at least one term to request extension"
      );
    } else if (onlyPhaseOptionsAvailable) {
      errors.rejectValue(
          "selectedPhase",
          "selectedPhase.required",
          "Select at least one phase to request extension"
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