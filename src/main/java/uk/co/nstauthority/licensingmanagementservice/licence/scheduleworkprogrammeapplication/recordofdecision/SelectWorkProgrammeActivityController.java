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
    "licence/schedule-work-programme-application/{scheduleWorkProgrammeApplicationDetailId}/work-programme-amendment-details")
@ScheduleAmendmentApplicationHasStatus(value = ApplicationStatus.ISSUE_DECISION)
@InvokingUserCanAccessScheduleApplication
public class SelectWorkProgrammeActivityController {

  static final String PAGE_TITLE = "Which work programme activity forms part of the decision?";

  private final RecordWorkProgrammeAmendmentDetailsService recordWorkProgrammeAmendmentDetailsService;
  private final SelectWorkProgrammeActivityFormValidator selectWorkProgrammeActivityFormValidator;

  public SelectWorkProgrammeActivityController(
      RecordWorkProgrammeAmendmentDetailsService recordWorkProgrammeAmendmentDetailsService,
      SelectWorkProgrammeActivityFormValidator selectWorkProgrammeActivityFormValidator
  ) {
    this.recordWorkProgrammeAmendmentDetailsService = recordWorkProgrammeAmendmentDetailsService;
    this.selectWorkProgrammeActivityFormValidator = selectWorkProgrammeActivityFormValidator;
  }

  @GetMapping
  public ModelAndView renderForm(
      @PathVariable UUID scheduleWorkProgrammeApplicationDetailId,
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail) {
    return getModelAndView(new SelectWorkProgrammeActivityForm(), scheduleWorkProgrammeApplicationDetail);
  }

  @PostMapping
  public ModelAndView submitForm(
      @PathVariable UUID scheduleWorkProgrammeApplicationDetailId,
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail,
      @ModelAttribute("form") SelectWorkProgrammeActivityForm form,
      BindingResult bindingResult) {
    if (!selectWorkProgrammeActivityFormValidator.isValid(
        form, bindingResult, scheduleWorkProgrammeApplicationDetail)) {
      return getModelAndView(form, scheduleWorkProgrammeApplicationDetail);
    }

    return ReverseRouter.redirect(on(RecordWorkProgrammeAmendmentDetailsController.class)
        .renderForm(
            scheduleWorkProgrammeApplicationDetailId,
            UUID.fromString(form.getWorkProgrammeActivityId()),
            null,
            null));
  }

  private ModelAndView getModelAndView(
      SelectWorkProgrammeActivityForm form,
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail) {

    var taskListUrl = ReverseRouter.route(on(RecordOfDecisionTaskListController.class)
        .getTaskList(scheduleWorkProgrammeApplicationDetail.getId(), null, null));

    var breadcrumbs = Breadcrumbs.builder(PAGE_TITLE)
        .addWorkAreaBreadcrumb()
        .addTaskListBreadcrumb(taskListUrl)
        .build();

    var modelAndView = new ModelAndView("lms/licence/scheduleWorkProgrammeApplication/selectWorkProgrammeActivity")
        .addObject("form", form)
        .addObject("pageTitle", PAGE_TITLE)
        .addObject("workProgrammeActivityViews",
            recordWorkProgrammeAmendmentDetailsService
                .getSelectableActivityViews(scheduleWorkProgrammeApplicationDetail))
        .addObject("allActivitiesActioned",
            recordWorkProgrammeAmendmentDetailsService
                .hasAmendmentDetails(scheduleWorkProgrammeApplicationDetail))
        .addObject("cancelUrl", taskListUrl);

    BreadcrumbsUtil.addBreadcrumbsToModel(modelAndView, breadcrumbs);

    return modelAndView;
  }
}
