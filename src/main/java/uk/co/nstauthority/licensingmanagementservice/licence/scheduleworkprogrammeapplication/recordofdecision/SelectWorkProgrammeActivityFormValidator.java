package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision;

import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney.WorkProgrammeActivityView;

@Service
public class SelectWorkProgrammeActivityFormValidator {

  static final String REQUIRED_ERROR_MESSAGE = "Select the work programme activity which forms part of the decision";
  static final String ALREADY_DECIDED_ERROR_MESSAGE =
      "A decision has already been recorded for this work programme activity";

  private final RecordWorkProgrammeAmendmentDetailsService recordWorkProgrammeAmendmentDetailsService;

  public SelectWorkProgrammeActivityFormValidator(
      RecordWorkProgrammeAmendmentDetailsService recordWorkProgrammeAmendmentDetailsService
  ) {
    this.recordWorkProgrammeAmendmentDetailsService = recordWorkProgrammeAmendmentDetailsService;
  }

  boolean isValid(
      SelectWorkProgrammeActivityForm form,
      Errors errors,
      ScheduleWorkProgrammeApplicationDetail applicationDetail
  ) {
    var selectableIds = recordWorkProgrammeAmendmentDetailsService.getSelectableActivityViews(applicationDetail)
        .stream()
        .map(WorkProgrammeActivityView::id)
        .toList();

    if (!selectableIds.contains(form.getWorkProgrammeActivityId())) {
      if (recordWorkProgrammeAmendmentDetailsService
          .isActivityAlreadyDecided(applicationDetail, form.getWorkProgrammeActivityId())) {
        errors.rejectValue("workProgrammeActivityId", "workProgrammeActivityId.alreadyDecided",
            ALREADY_DECIDED_ERROR_MESSAGE);
      } else {
        errors.rejectValue("workProgrammeActivityId", "workProgrammeActivityId.required", REQUIRED_ERROR_MESSAGE);
      }
    }

    return !errors.hasErrors();
  }
}
