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
    "licence/schedule-work-programme-application/{scheduleWorkProgrammeApplicationDetailId}/extension-decision-details")
@ScheduleAmendmentApplicationHasStatus(value = ApplicationStatus.ISSUE_DECISION)
@InvokingUserCanAccessScheduleApplication
public class RecordExtensionDetailsController {

  static final String PAGE_TITLE = "Extension decision details";

  private final RecordExtensionDetailsService recordExtensionDetailsService;
  private final RecordExtensionDetailsFormValidator recordExtensionDetailsFormValidator;

  public RecordExtensionDetailsController(
      RecordExtensionDetailsService recordExtensionDetailsService,
      RecordExtensionDetailsFormValidator recordExtensionDetailsFormValidator
  ) {
    this.recordExtensionDetailsService = recordExtensionDetailsService;
    this.recordExtensionDetailsFormValidator = recordExtensionDetailsFormValidator;
  }

  @GetMapping
  public ModelAndView renderForm(
      @PathVariable UUID scheduleWorkProgrammeApplicationDetailId,
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail) {
    return getModelAndView(
        recordExtensionDetailsService.getFilledForm(scheduleWorkProgrammeApplicationDetail),
        scheduleWorkProgrammeApplicationDetail);
  }

  @PostMapping
  public ModelAndView submitForm(
      @PathVariable UUID scheduleWorkProgrammeApplicationDetailId,
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail,
      @ModelAttribute("form") RecordExtensionDetailsForm form,
      BindingResult bindingResult) {
    if (!recordExtensionDetailsFormValidator.isValid(form, bindingResult, scheduleWorkProgrammeApplicationDetail)) {
      return getModelAndView(form, scheduleWorkProgrammeApplicationDetail);
    }

    recordExtensionDetailsService.saveExtensionDetails(form, scheduleWorkProgrammeApplicationDetail);

    return ReverseRouter.redirect(on(RecordOfDecisionTaskListController.class)
        .getTaskList(scheduleWorkProgrammeApplicationDetailId, null, null));
  }

  private ModelAndView getModelAndView(
      RecordExtensionDetailsForm form,
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail) {

    var extensionDetailsViews = recordExtensionDetailsService
        .getExtensionDetailsViews(scheduleWorkProgrammeApplicationDetail);

    var taskListUrl = ReverseRouter.route(on(RecordOfDecisionTaskListController.class)
        .getTaskList(scheduleWorkProgrammeApplicationDetail.getId(), null, null));

    var breadcrumbs = Breadcrumbs.builder(PAGE_TITLE)
        .addWorkAreaBreadcrumb()
        .addTaskListBreadcrumb(taskListUrl)
        .build();

    var modelAndView = new ModelAndView("lms/licence/scheduleWorkProgrammeApplication/recordExtensionDetails")
        .addObject("form", form)
        .addObject("pageTitle", PAGE_TITLE)
        .addObject("extensionDetailsViews", extensionDetailsViews)
        .addObject("canExtendMoreThanOneOption", extensionDetailsViews.size() > 1)
        .addObject("cancelUrl", taskListUrl);

    BreadcrumbsUtil.addBreadcrumbsToModel(modelAndView, breadcrumbs);

    return modelAndView;
  }
}
