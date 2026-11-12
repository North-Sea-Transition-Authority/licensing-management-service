package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.reviewandsubmit;

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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.scheduleworkprogrammeapplication.InvokingUserCanAccessScheduleApplication;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.scheduleworkprogrammeapplication.ScheduleAmendmentApplicationHasStatus;
import uk.co.nstauthority.licensingmanagementservice.breadcrumbs.Breadcrumbs;
import uk.co.nstauthority.licensingmanagementservice.breadcrumbs.BreadcrumbsUtil;
import uk.co.nstauthority.licensingmanagementservice.feedback.FeedbackController;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceService;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.application.requestupdate.ConfirmOnlyRequestedChangesForm;
import uk.co.nstauthority.licensingmanagementservice.licence.application.requestupdate.ConfirmOnlyRequestedChangesFormValidator;
import uk.co.nstauthority.licensingmanagementservice.licence.contact.LicenceContactService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.tasklist.ScheduleWorkProgrammeApplicationTaskListController;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.tasklist.ScheduleWorkProgrammeApplicationTaskListService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.workarea.WorkAreaController;

@Controller
@RequestMapping("/licence/schedule-work-programme-application/{scheduleWorkProgrammeApplicationDetailId}/review-and-submit")
@ScheduleAmendmentApplicationHasStatus(value = ApplicationStatus.DRAFT)
@InvokingUserCanAccessScheduleApplication
public class ScheduleAmendmentApplicationReviewAndSubmitController {

  private final LicenceService licenceService;
  private final ScheduleWorkProgrammeApplicationService scheduleWorkProgrammeApplicationService;
  private final LicenceScheduleSummarySectionService licenceScheduleSummarySectionService;
  private final ScheduleWorkProgrammeApplicationTaskListService scheduleWorkProgrammeApplicationTaskListService;
  private final LicenceContactService licenceContactService;
  private final ConfirmOnlyRequestedChangesFormValidator confirmOnlyRequestedChangesFormValidator;

  public ScheduleAmendmentApplicationReviewAndSubmitController(
      LicenceService licenceService,
      ScheduleWorkProgrammeApplicationService scheduleWorkProgrammeApplicationService,
      LicenceScheduleSummarySectionService licenceScheduleSummarySectionService,
      ScheduleWorkProgrammeApplicationTaskListService scheduleWorkProgrammeApplicationTaskListService,
      LicenceContactService licenceContactService,
      ConfirmOnlyRequestedChangesFormValidator confirmOnlyRequestedChangesFormValidator
  ) {
    this.licenceService = licenceService;
    this.scheduleWorkProgrammeApplicationService = scheduleWorkProgrammeApplicationService;
    this.licenceScheduleSummarySectionService = licenceScheduleSummarySectionService;
    this.scheduleWorkProgrammeApplicationTaskListService = scheduleWorkProgrammeApplicationTaskListService;
    this.licenceContactService = licenceContactService;
    this.confirmOnlyRequestedChangesFormValidator = confirmOnlyRequestedChangesFormValidator;
  }

  @GetMapping
  public ModelAndView getReviewAndSubmit(
      @PathVariable UUID scheduleWorkProgrammeApplicationDetailId,
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail,
      @ModelAttribute("form") ConfirmOnlyRequestedChangesForm form,
      ServiceUserDetail user
  ) {
    var submittable = scheduleWorkProgrammeApplicationTaskListService.isSubmittable(
        scheduleWorkProgrammeApplicationDetail,
        user
    );

    return getReviewAndSubmitModelAndView(scheduleWorkProgrammeApplicationDetail, form, submittable, user);
  }

  private ModelAndView getReviewAndSubmitModelAndView(
      ScheduleWorkProgrammeApplicationDetail applicationDetail,
      ConfirmOnlyRequestedChangesForm form,
      boolean isSubmittable,
      ServiceUserDetail user
  ) {
    var taskListUrl = ReverseRouter.route(on(ScheduleWorkProgrammeApplicationTaskListController.class)
        .getTaskList(applicationDetail.getId(), null, null));

    var modelAndView = new ModelAndView("lms/licence/scheduleWorkProgrammeApplication/reviewAndSubmit")
        .addObject("cancelUrl", taskListUrl)
        .addObject("pageCaption", licenceService.getLicencePageCaption(applicationDetail.getLicence()))
        .addObject("summarySections", licenceScheduleSummarySectionService.getSummarySections(applicationDetail, null))
        .addObject("accordionId", applicationDetail.getId())
        .addObject("form", form)
        .addObject("isUpdateDraft", applicationDetail.isApplicationUpdate())
        .addObject("isSubmittable", isSubmittable)
        .addObject("hasLicenceContact", licenceContactService.hasContactForLicensee(applicationDetail))
        .addObject("userCanSubmit", scheduleWorkProgrammeApplicationService.userCanSubmitApplication(applicationDetail, user))
        .addObject("submitterRoleName", Role.APPLICATION_SUBMITTER.getName());

    var breadcrumbs = Breadcrumbs.builder("Review your application before submitting")
        .addWorkAreaBreadcrumb()
        .addTaskListBreadcrumb(taskListUrl)
        .build();

    BreadcrumbsUtil.addBreadcrumbsToModel(modelAndView, breadcrumbs);
    return modelAndView;
  }

  @PostMapping
  public ModelAndView submitApplication(
      @PathVariable UUID scheduleWorkProgrammeApplicationDetailId,
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail,
      @ModelAttribute("form") ConfirmOnlyRequestedChangesForm form,
      BindingResult bindingResult,
      ServiceUserDetail user,
      RedirectAttributes redirectAttributes
  ) {
    var submittable = scheduleWorkProgrammeApplicationTaskListService.isSubmittable(
        scheduleWorkProgrammeApplicationDetail,
        user
    );

    var changesConfirmed = !scheduleWorkProgrammeApplicationDetail.isApplicationUpdate()
        || confirmOnlyRequestedChangesFormValidator.isValid(form, bindingResult);

    if (!submittable
        || !changesConfirmed
        || !licenceContactService.hasContactForLicensee(scheduleWorkProgrammeApplicationDetail)) {
      return getReviewAndSubmitModelAndView(scheduleWorkProgrammeApplicationDetail, form, submittable, user);
    }

    var scheduleWorkProgrammeApplication
        = scheduleWorkProgrammeApplicationService.submitApplication(scheduleWorkProgrammeApplicationDetail, user);

    return new ModelAndView("lms/licence/application/submissionConfirmation")
        .addObject("feedbackUrl", ReverseRouter.route(on(FeedbackController.class).getFeedback(null)))
        .addObject("workAreaUrl", ReverseRouter.route(on(WorkAreaController.class).getWorkArea(null, null)))
        .addObject("applicationReference", scheduleWorkProgrammeApplication.getApplicationReference());
  }
}