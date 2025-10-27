package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.reviewandsubmit;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.tasklist.ScheduleWorkProgrammeApplicationTaskListController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Controller
@RequestMapping("/licence/schedule-work-programme-application/{scheduleWorkProgrammeApplicationDetailId}/review-and-submit")
public class LicenceScheduleReviewAndSubmitController {

  private final LicenceService licenceService;
  private final ScheduleWorkProgrammeApplicationService scheduleWorkProgrammeApplicationService;
  private final LicenceScheduleSummarySectionService licenceScheduleSummarySectionService;

  public LicenceScheduleReviewAndSubmitController(
      LicenceService licenceService,
      ScheduleWorkProgrammeApplicationService scheduleWorkProgrammeApplicationService,
      LicenceScheduleSummarySectionService licenceScheduleSummarySectionService
  ) {
    this.licenceService = licenceService;
    this.scheduleWorkProgrammeApplicationService = scheduleWorkProgrammeApplicationService;
    this.licenceScheduleSummarySectionService = licenceScheduleSummarySectionService;
  }

  @GetMapping
  public ModelAndView getReviewAndSubmit(
      @PathVariable UUID scheduleWorkProgrammeApplicationDetailId,
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail
  ) {

    return getModelAndView(scheduleWorkProgrammeApplicationDetail);
  }

  private ModelAndView getModelAndView(
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail
  ) {

    return new ModelAndView("lms/licence/scheduleWorkProgrammeApplication/reviewAndSubmit")
        .addObject("cancelUrl", ReverseRouter.route(on(ScheduleWorkProgrammeApplicationTaskListController.class).getTaskList(
                scheduleWorkProgrammeApplicationDetail.getId(),
                null,
                null
            )))
        .addObject("pageCaption", licenceService.getLicencePageCaption(
            scheduleWorkProgrammeApplicationService.getLicenceFromScheduleWorkProgrammeApplicationDetail(
                scheduleWorkProgrammeApplicationDetail
            )))
        .addObject("summarySections", licenceScheduleSummarySectionService.getSummarySections(
                scheduleWorkProgrammeApplicationDetail,
                null
            ))
        .addObject("accordionId", scheduleWorkProgrammeApplicationDetail.getId());
  }
}