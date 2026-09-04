package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDuration;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDurationDisplayUtil;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDurationValidationUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;

@Service
public class RecordDurationChangesFormValidator {

  static final String CHANGE_TYPE_REQUIRED_ERROR_MESSAGE = "Select whether %s is maintained, reduced or extended";
  static final String REDUCE_NOT_AVAILABLE_ERROR_MESSAGE =
      "%s is already under way, so its duration cannot be reduced";
  static final String EXTEND_NOT_AVAILABLE_ERROR_MESSAGE =
      "%s is the final period of the schedule, so its duration cannot be extended";
  static final String TOTAL_MISMATCH_ERROR_MESSAGE =
      "The total reduction of %s must equal the total extension of %s";

  private final RecordDurationChangesService recordDurationChangesService;

  public RecordDurationChangesFormValidator(RecordDurationChangesService recordDurationChangesService) {
    this.recordDurationChangesService = recordDurationChangesService;
  }

  boolean isValid(
      RecordDurationChangesForm form,
      BindingResult bindingResult,
      ScheduleWorkProgrammeApplicationDetail applicationDetail
  ) {
    var views = recordDurationChangesService.getDurationChangeViews(applicationDetail);

    initialiseFormFromViews(form, views);

    views.forEach(view -> validateRow(form, bindingResult, view));

    if (!bindingResult.hasErrors()) {
      validateTotalsBalance(form, bindingResult, views);
    }

    return !bindingResult.hasErrors();
  }

  private void initialiseFormFromViews(RecordDurationChangesForm form, List<RecordDurationChangeView> views) {
    for (RecordDurationChangeView view : views) {
      form.getReduceDuration()
          .computeIfAbsent(view.id(), RecordDurationChangesForm::newReduceDurationInput);
      form.getExtendDuration()
          .computeIfAbsent(view.id(), RecordDurationChangesForm::newExtendDurationInput);
    }
  }

  private void validateRow(
      RecordDurationChangesForm form,
      BindingResult bindingResult,
      RecordDurationChangeView view
  ) {
    var changeType = form.getChangeType().get(view.id());

    if (changeType == null) {
      bindingResult.rejectValue(
          "changeType[%s]".formatted(view.id()),
          "changeType.required",
          CHANGE_TYPE_REQUIRED_ERROR_MESSAGE.formatted(view.displayName()));
      return;
    }

    if (changeType == DurationChangeType.REDUCE && !view.canReduce()) {
      rejectUnavailableOption(bindingResult, view, REDUCE_NOT_AVAILABLE_ERROR_MESSAGE);
      return;
    }

    if (changeType == DurationChangeType.EXTEND && !view.canExtend()) {
      rejectUnavailableOption(bindingResult, view, EXTEND_NOT_AVAILABLE_ERROR_MESSAGE);
      return;
    }

    if (changeType != DurationChangeType.MAINTAIN) {
      ThreeFieldDurationValidationUtil.validate(form.durationFor(view.id(), changeType), bindingResult);
    }
  }

  private void rejectUnavailableOption(
      BindingResult bindingResult,
      RecordDurationChangeView view,
      String errorMessage
  ) {
    bindingResult.rejectValue(
        "changeType[%s]".formatted(view.id()),
        "changeType.notAvailable",
        errorMessage.formatted(view.displayName()));
  }

  private void validateTotalsBalance(
      RecordDurationChangesForm form,
      Errors errors,
      List<RecordDurationChangeView> views
  ) {
    var totalExtension = totalFor(form, views, DurationChangeType.EXTEND);
    var totalReduction = totalFor(form, views, DurationChangeType.REDUCE);

    if (totalExtension.equals(totalReduction)) {
      return;
    }

    var firstChanged = views.stream()
        .filter(view -> form.getChangeType().get(view.id()) != null
            && form.getChangeType().get(view.id()) != DurationChangeType.MAINTAIN)
        .findFirst();

    if (firstChanged.isEmpty()) {
      return;
    }

    var firstChangedType = form.getChangeType().get(firstChanged.get().id());
    var fieldName = form.durationFor(firstChanged.get().id(), firstChangedType).getFieldName();

    errors.rejectValue(
        "%s.years".formatted(fieldName),
        "duration.total.mismatch",
        TOTAL_MISMATCH_ERROR_MESSAGE.formatted(
            ThreeFieldDurationDisplayUtil.convertToDisplayText(totalReduction),
            ThreeFieldDurationDisplayUtil.convertToDisplayText(totalExtension)));
  }

  private ThreeFieldDuration totalFor(
      RecordDurationChangesForm form,
      List<RecordDurationChangeView> views,
      DurationChangeType changeType
  ) {
    return ThreeFieldDuration.total(views.stream()
        .filter(view -> form.getChangeType().get(view.id()) == changeType)
        .map(view -> form.durationFor(view.id(), changeType).toThreeFieldDuration())
        .toList());
  }
}
