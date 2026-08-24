package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDuration;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDurationDisplayUtil;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDurationValidationUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;

@Service
public class RecordReductionDetailsFormValidator {

  static final String TOTAL_MISMATCH_ERROR_MESSAGE = "The total reduction must equal the total extension of %s";

  private final RecordReductionDetailsService recordReductionDetailsService;
  private final RecordExtensionDetailsService recordExtensionDetailsService;

  public RecordReductionDetailsFormValidator(
      RecordReductionDetailsService recordReductionDetailsService,
      RecordExtensionDetailsService recordExtensionDetailsService
  ) {
    this.recordReductionDetailsService = recordReductionDetailsService;
    this.recordExtensionDetailsService = recordExtensionDetailsService;
  }

  boolean isValid(
      RecordReductionDetailsForm form,
      BindingResult bindingResult,
      ScheduleWorkProgrammeApplicationDetail applicationDetail
  ) {
    var reductionDetailsViews = recordReductionDetailsService.getReductionDetailsViews(applicationDetail);

    initialiseFormFromViews(form, reductionDetailsViews);

    if (form.getReductionDuration().size() > 1) {
      validateTermOrPhaseSelected(form, bindingResult, reductionDetailsViews);
    }

    selectedIds(form, reductionDetailsViews)
        .forEach(id -> ThreeFieldDurationValidationUtil.validate(form.getReductionDuration().get(id), bindingResult));

    if (!bindingResult.hasErrors()) {
      validateTotalMatchesExtension(form, bindingResult, reductionDetailsViews, applicationDetail);
    }

    return !bindingResult.hasErrors();
  }

  private void initialiseFormFromViews(RecordReductionDetailsForm form, List<RecordReductionDetailsView> views) {
    if (form.getSelectedPhase() == null) {
      form.setSelectedPhase(new HashMap<>());
    }
    if (form.getSelectedTerm() == null) {
      form.setSelectedTerm(new HashMap<>());
    }
    if (form.getReductionDuration() == null) {
      form.setReductionDuration(new HashMap<>());
    }

    for (RecordReductionDetailsView view : views) {
      if (view.isPhase()) {
        form.getSelectedPhase().putIfAbsent(view.id(), false);
      } else {
        form.getSelectedTerm().putIfAbsent(view.id(), false);
      }

      if (form.getReductionDuration().get(view.id()) == null) {
        form.getReductionDuration().put(view.id(), RecordReductionDetailsForm.newDurationInput(view.id()));
      }
    }
  }

  private void validateTermOrPhaseSelected(
      RecordReductionDetailsForm form,
      Errors errors,
      List<RecordReductionDetailsView> views
  ) {
    var hasSelectedPhase = form.getSelectedPhase() != null
        && form.getSelectedPhase().values().stream().anyMatch(Boolean.TRUE::equals);
    var hasSelectedTerm = form.getSelectedTerm() != null
        && form.getSelectedTerm().values().stream().anyMatch(Boolean.TRUE::equals);

    if (hasSelectedTerm || hasSelectedPhase) {
      return;
    }

    var termOptionsAvailable = views.stream().anyMatch(view -> !view.isPhase());
    var phaseOptionsAvailable = views.stream().anyMatch(RecordReductionDetailsView::isPhase);

    if (termOptionsAvailable && phaseOptionsAvailable) {
      errors.rejectValue("selectedTerm", "selectedTerm.required",
          "Select at least one phase or term being reduced");
    } else if (termOptionsAvailable) {
      errors.rejectValue("selectedTerm", "selectedTerm.required",
          "Select at least one term being reduced");
    } else if (phaseOptionsAvailable) {
      errors.rejectValue("selectedPhase", "selectedPhase.required",
          "Select at least one phase being reduced");
    }
  }

  private void validateTotalMatchesExtension(
      RecordReductionDetailsForm form,
      Errors errors,
      List<RecordReductionDetailsView> views,
      ScheduleWorkProgrammeApplicationDetail applicationDetail
  ) {
    var selectedIds = selectedIds(form, views);

    if (selectedIds.isEmpty()) {
      return;
    }

    var totalReduction = ThreeFieldDuration.total(selectedIds.stream()
        .map(id -> form.getReductionDuration().get(id).toThreeFieldDuration())
        .toList());

    var totalExtension = recordExtensionDetailsService.getTotalExtensionDuration(applicationDetail);

    if (!totalReduction.equals(totalExtension)) {
      errors.rejectValue(
          form.getReductionDuration().get(selectedIds.getFirst()).getFieldName() + ".years",
          "reductionDuration.total.mismatch",
          TOTAL_MISMATCH_ERROR_MESSAGE.formatted(ThreeFieldDurationDisplayUtil.convertToDisplayText(totalExtension))
      );
    }
  }

  private List<String> selectedIds(RecordReductionDetailsForm form, List<RecordReductionDetailsView> views) {
    var isSingleValue = form.getReductionDuration().size() == 1;

    return views.stream()
        .map(RecordReductionDetailsView::id)
        .filter(id -> form.getReductionDuration().get(id) != null)
        .filter(id -> isSingleValue || getSelectedStatus(form, id))
        .toList();
  }

  private boolean getSelectedStatus(RecordReductionDetailsForm form, String key) {
    var possiblePhase = Optional.ofNullable(form.getSelectedPhase()).map(phase -> phase.get(key));
    var possibleTerm = Optional.ofNullable(form.getSelectedTerm()).map(term -> term.get(key));
    return possiblePhase.or(() -> possibleTerm).orElse(false);
  }

}
