package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDurationValidationUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;

@Service
public class RecordExtensionDetailsFormValidator {

  private final RecordExtensionDetailsService recordExtensionDetailsService;

  public RecordExtensionDetailsFormValidator(RecordExtensionDetailsService recordExtensionDetailsService) {
    this.recordExtensionDetailsService = recordExtensionDetailsService;
  }

  boolean isValid(
      RecordExtensionDetailsForm form,
      BindingResult bindingResult,
      ScheduleWorkProgrammeApplicationDetail applicationDetail
  ) {
    var extensionDetailsViews = recordExtensionDetailsService.getExtensionDetailsViews(applicationDetail);

    initialiseFormFromViews(form, extensionDetailsViews);

    if (form.getExtensionDuration().size() > 1) {
      validateTermOrPhaseSelected(form, bindingResult, extensionDetailsViews);
    }

    form.getExtensionDuration().entrySet().stream()
        .filter(entry -> {
          if (entry.getValue() == null) {
            return false;
          }
          boolean isSelected = getSelectedStatus(form, entry.getKey());
          boolean isSingleValue = form.getExtensionDuration().size() == 1;
          return isSelected || isSingleValue;
        })
        .forEach(entry -> ThreeFieldDurationValidationUtil.validate(entry.getValue(), bindingResult));

    return !bindingResult.hasErrors();
  }

  private void initialiseFormFromViews(RecordExtensionDetailsForm form, List<RecordExtensionDetailsView> views) {
    if (form.getSelectedPhase() == null) {
      form.setSelectedPhase(new HashMap<>());
    }
    if (form.getSelectedTerm() == null) {
      form.setSelectedTerm(new HashMap<>());
    }
    if (form.getExtensionDuration() == null) {
      form.setExtensionDuration(new HashMap<>());
    }

    for (RecordExtensionDetailsView view : views) {
      if (view.isPhase()) {
        form.getSelectedPhase().putIfAbsent(view.id(), false);
      } else {
        form.getSelectedTerm().putIfAbsent(view.id(), false);
      }

      if (form.getExtensionDuration().get(view.id()) == null) {
        form.getExtensionDuration().put(view.id(), RecordExtensionDetailsForm.newDurationInput(view.id()));
      }
    }
  }

  private void validateTermOrPhaseSelected(
      RecordExtensionDetailsForm form,
      Errors errors,
      List<RecordExtensionDetailsView> views
  ) {
    var hasSelectedPhase = form.getSelectedPhase() != null
        && form.getSelectedPhase().values().stream().anyMatch(Boolean.TRUE::equals);
    var hasSelectedTerm = form.getSelectedTerm() != null
        && form.getSelectedTerm().values().stream().anyMatch(Boolean.TRUE::equals);

    if (hasSelectedTerm || hasSelectedPhase) {
      return;
    }

    var termOptionsAvailable = views.stream().anyMatch(view -> !view.isPhase());
    var phaseOptionsAvailable = views.stream().anyMatch(RecordExtensionDetailsView::isPhase);

    if (termOptionsAvailable && phaseOptionsAvailable) {
      errors.rejectValue("selectedTerm", "selectedTerm.required",
          "Select at least one phase or term being extended");
    } else if (termOptionsAvailable) {
      errors.rejectValue("selectedTerm", "selectedTerm.required",
          "Select at least one term being extended");
    } else if (phaseOptionsAvailable) {
      errors.rejectValue("selectedPhase", "selectedPhase.required",
          "Select at least one phase being extended");
    }
  }

  private boolean getSelectedStatus(RecordExtensionDetailsForm form, String key) {
    var possiblePhase = Optional.ofNullable(form.getSelectedPhase()).map(phase -> phase.get(key));
    var possibleTerm = Optional.ofNullable(form.getSelectedTerm()).map(term -> term.get(key));
    return possiblePhase.or(() -> possibleTerm).orElse(false);
  }
}
