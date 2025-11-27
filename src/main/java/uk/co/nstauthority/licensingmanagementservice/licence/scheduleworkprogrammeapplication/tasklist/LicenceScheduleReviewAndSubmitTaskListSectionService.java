package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.tasklist;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.reviewandsubmit.ScheduleAmendmentApplicationReviewAndSubmitController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListItem;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListSection;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListSectionService;

@Service
public class LicenceScheduleReviewAndSubmitTaskListSectionService
    implements TaskListSectionService<ScheduleWorkProgrammeApplicationDetail> {

  static final String REVIEW_AND_SUBMIT = "Review and submit";
  static final int SECTION_ORDER = 20;

  @Override
  public Optional<TaskListSection> getSection(
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail,
      ServiceUserDetail user
  ) {

    var items = new ArrayList<>(List.of(new TaskListItem(
        REVIEW_AND_SUBMIT,
        ReverseRouter.route(on(ScheduleAmendmentApplicationReviewAndSubmitController.class).getReviewAndSubmit(
            scheduleWorkProgrammeApplicationDetail.getId(),
            null,
            user
        ))
    )));
    return Optional.of(new TaskListSection(
        REVIEW_AND_SUBMIT,
        SECTION_ORDER,
        items
    ));
  }
}