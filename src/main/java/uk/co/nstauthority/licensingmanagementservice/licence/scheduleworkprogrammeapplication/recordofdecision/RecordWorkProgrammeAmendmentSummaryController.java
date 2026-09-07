package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.scheduleworkprogrammeapplication.InvokingUserCanAccessScheduleApplication;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.scheduleworkprogrammeapplication.ScheduleAmendmentApplicationHasStatus;
import uk.co.nstauthority.licensingmanagementservice.breadcrumbs.Breadcrumbs;
import uk.co.nstauthority.licensingmanagementservice.breadcrumbs.BreadcrumbsUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Controller
@RequestMapping(
    "licence/schedule-work-programme-application/{scheduleWorkProgrammeApplicationDetailId}" +
        "/work-programme-amendment-details/summary")
@ScheduleAmendmentApplicationHasStatus(value = ApplicationStatus.ISSUE_DECISION)
@InvokingUserCanAccessScheduleApplication
public class RecordWorkProgrammeAmendmentSummaryController {

  static final String PAGE_TITLE = "Work programme amendments";

  private final RecordWorkProgrammeAmendmentDetailsService recordWorkProgrammeAmendmentDetailsService;
  private final RecordWorkProgrammeAmendmentSummaryFormValidator recordWorkProgrammeAmendmentSummaryFormValidator;
  private final RecordOfDecisionService recordOfDecisionService;

  public RecordWorkProgrammeAmendmentSummaryController(
      RecordWorkProgrammeAmendmentDetailsService recordWorkProgrammeAmendmentDetailsService,
      RecordWorkProgrammeAmendmentSummaryFormValidator recordWorkProgrammeAmendmentSummaryFormValidator,
      RecordOfDecisionService recordOfDecisionService
  ) {
    this.recordWorkProgrammeAmendmentDetailsService = recordWorkProgrammeAmendmentDetailsService;
    this.recordWorkProgrammeAmendmentSummaryFormValidator = recordWorkProgrammeAmendmentSummaryFormValidator;
    this.recordOfDecisionService = recordOfDecisionService;
  }

  @GetMapping
  public ModelAndView renderForm(
      @PathVariable UUID scheduleWorkProgrammeApplicationDetailId,
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail
  ) {
    if (!recordWorkProgrammeAmendmentDetailsService.hasAmendmentDetails(scheduleWorkProgrammeApplicationDetail)) {
      return ReverseRouter.redirect(on(SelectWorkProgrammeActivityController.class)
          .renderForm(scheduleWorkProgrammeApplicationDetailId, null));
    }

    return getModelAndView(
        recordOfDecisionService.getFilledWorkProgrammeSummaryForm(scheduleWorkProgrammeApplicationDetail),
        scheduleWorkProgrammeApplicationDetail);
  }

  @PostMapping
  public ModelAndView submitForm(
      @PathVariable UUID scheduleWorkProgrammeApplicationDetailId,
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail,
      @ModelAttribute("form") RecordWorkProgrammeAmendmentSummaryForm form,
      BindingResult bindingResult
  ) {
    if (!recordWorkProgrammeAmendmentDetailsService.hasAmendmentDetails(scheduleWorkProgrammeApplicationDetail)) {
      return ReverseRouter.redirect(on(SelectWorkProgrammeActivityController.class)
          .renderForm(scheduleWorkProgrammeApplicationDetailId, null));
    }

    if (!recordWorkProgrammeAmendmentSummaryFormValidator.isValid(bindingResult)) {
      return getModelAndView(form, scheduleWorkProgrammeApplicationDetail);
    }

    recordOfDecisionService.saveWorkProgrammeSummaryOption(scheduleWorkProgrammeApplicationDetail, form);

    if (form.getRecordWorkProgrammeAmendmentSummaryOptions()
        == RecordWorkProgrammeAmendmentSummaryOptions.YES_NOW) {
      return ReverseRouter.redirect(on(SelectWorkProgrammeActivityController.class)
          .renderForm(scheduleWorkProgrammeApplicationDetailId, null));
    }

    return ReverseRouter.redirect(on(RecordOfDecisionTaskListController.class)
        .getTaskList(scheduleWorkProgrammeApplicationDetailId, null, null));
  }

  private ModelAndView getModelAndView(
      RecordWorkProgrammeAmendmentSummaryForm form,
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail
  ) {
    var taskListUrl = ReverseRouter.route(on(RecordOfDecisionTaskListController.class)
        .getTaskList(scheduleWorkProgrammeApplicationDetail.getId(), null, null));

    var breadcrumbs = Breadcrumbs.builder(PAGE_TITLE)
        .addWorkAreaBreadcrumb()
        .addTaskListBreadcrumb(taskListUrl)
        .build();

    var modelAndView =
        new ModelAndView("lms/licence/scheduleWorkProgrammeApplication/recordWorkProgrammeAmendmentSummary")
            .addObject("form", form)
            .addObject("pageTitle", PAGE_TITLE)
            .addObject("workProgrammeAmendments", recordWorkProgrammeAmendmentDetailsService
                .getRecordedAmendmentViews(scheduleWorkProgrammeApplicationDetail))
            .addObject("summaryOptions", RecordWorkProgrammeAmendmentSummaryOptions.getOptions())
            .addObject("cancelUrl", taskListUrl);

    BreadcrumbsUtil.addBreadcrumbsToModel(modelAndView, breadcrumbs);

    return modelAndView;
  }
}
