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
import uk.co.nstauthority.licensingmanagementservice.fds.searchselector.SearchSelectorService;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.internalapi.LicenceInternalApiRestController;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivity;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Controller
@RequestMapping(
    "licence/schedule-work-programme-application/{scheduleWorkProgrammeApplicationDetailId}" +
        "/work-programme-amendment-details/{workProgrammeActivityId}")
@ScheduleAmendmentApplicationHasStatus(value = ApplicationStatus.ISSUE_DECISION)
@InvokingUserCanAccessScheduleApplication
public class RecordWorkProgrammeAmendmentDetailsController {

  static final String PAGE_TITLE = "Work programme amendment details";

  private final RecordWorkProgrammeAmendmentDetailsService recordWorkProgrammeAmendmentDetailsService;
  private final RecordWorkProgrammeAmendmentDetailsFormValidator recordWorkProgrammeAmendmentDetailsFormValidator;
  private final WorkProgrammeActivityService workProgrammeActivityService;

  public RecordWorkProgrammeAmendmentDetailsController(
      RecordWorkProgrammeAmendmentDetailsService recordWorkProgrammeAmendmentDetailsService,
      RecordWorkProgrammeAmendmentDetailsFormValidator recordWorkProgrammeAmendmentDetailsFormValidator,
      WorkProgrammeActivityService workProgrammeActivityService
  ) {
    this.recordWorkProgrammeAmendmentDetailsService = recordWorkProgrammeAmendmentDetailsService;
    this.recordWorkProgrammeAmendmentDetailsFormValidator = recordWorkProgrammeAmendmentDetailsFormValidator;
    this.workProgrammeActivityService = workProgrammeActivityService;
  }

  @GetMapping
  public ModelAndView renderForm(
      @PathVariable UUID scheduleWorkProgrammeApplicationDetailId,
      @PathVariable UUID workProgrammeActivityId,
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail,
      WorkProgrammeActivity workProgrammeActivity) {
    return getModelAndView(
        recordWorkProgrammeAmendmentDetailsService
            .getFilledForm(scheduleWorkProgrammeApplicationDetail, workProgrammeActivity),
        scheduleWorkProgrammeApplicationDetail,
        workProgrammeActivity);
  }

  @PostMapping
  public ModelAndView submitForm(
      @PathVariable UUID scheduleWorkProgrammeApplicationDetailId,
      @PathVariable UUID workProgrammeActivityId,
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail,
      WorkProgrammeActivity workProgrammeActivity,
      @ModelAttribute("form") RecordWorkProgrammeAmendmentDetailsForm form,
      BindingResult bindingResult) {
    if (!recordWorkProgrammeAmendmentDetailsFormValidator.isValid(form, bindingResult)) {
      return getModelAndView(form, scheduleWorkProgrammeApplicationDetail, workProgrammeActivity);
    }

    recordWorkProgrammeAmendmentDetailsService.saveAmendmentDetails(
        form, scheduleWorkProgrammeApplicationDetail, workProgrammeActivity);

    return ReverseRouter.redirect(on(RecordOfDecisionTaskListController.class)
        .getTaskList(scheduleWorkProgrammeApplicationDetailId, null, null));
  }

  private ModelAndView getModelAndView(
      RecordWorkProgrammeAmendmentDetailsForm form,
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail,
      WorkProgrammeActivity workProgrammeActivity) {

    var taskListUrl = ReverseRouter.route(on(RecordOfDecisionTaskListController.class)
        .getTaskList(scheduleWorkProgrammeApplicationDetail.getId(), null, null));

    var licenceTypeSlug = workProgrammeActivity.getLicence().getType().getUrlSlug();

    var breadcrumbs = Breadcrumbs.builder(PAGE_TITLE)
        .addWorkAreaBreadcrumb()
        .addTaskListBreadcrumb(taskListUrl)
        .build();

    var modelAndView =
        new ModelAndView("lms/licence/scheduleWorkProgrammeApplication/recordWorkProgrammeAmendmentDetails")
            .addObject("form", form)
            .addObject("pageTitle", PAGE_TITLE)
            .addObject("workProgrammeActivityDetails",
                workProgrammeActivityService.createWorkProgrammeActivityView(workProgrammeActivity))
            .addObject("decisionOptions", WorkProgrammeAmendmentDecision.getOptions())
            .addObject("targetLicences",
                recordWorkProgrammeAmendmentDetailsService.getTargetLicenceSelections(form.getTargetLicenceIds()))
            .addObject("searchUrl",
                SearchSelectorService.route(on(LicenceInternalApiRestController.class)
                    .searchActiveLicenceSchedulesByReferenceAndType(licenceTypeSlug, null, null)))
            .addObject("cancelUrl", taskListUrl);

    BreadcrumbsUtil.addBreadcrumbsToModel(modelAndView, breadcrumbs);

    return modelAndView;
  }
}
