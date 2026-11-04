package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overview.requestupdate;

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
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationActionEndPointInterceptorRule;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.application.requestupdate.RequestUpdateForm;
import uk.co.nstauthority.licensingmanagementservice.licence.application.requestupdate.RequestUpdateFormValidator;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overview.ScheduleWorkProgrammeApplicationOverviewController;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overview.action.ScheduleWorkProgrammeApplicationActionItem;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Controller
@RequestMapping(
    "licence/schedule-work-programme-application/{scheduleWorkProgrammeApplicationDetailId}/request-update")
@ScheduleAmendmentApplicationHasStatus(ApplicationStatus.SUBMITTED)
@InvokingUserCanAccessScheduleApplication
@ScheduleWorkProgrammeApplicationActionEndPointInterceptorRule.ActionEndPoint(
    ScheduleWorkProgrammeApplicationActionItem.REQUEST_UPDATE)
public class RequestUpdateController {

  static final String PAGE_TITLE = "Request an update";

  private final RequestUpdateService requestUpdateService;
  private final RequestUpdateFormValidator requestUpdateFormValidator;

  public RequestUpdateController(
      RequestUpdateService requestUpdateService,
      RequestUpdateFormValidator requestUpdateFormValidator
  ) {
    this.requestUpdateService = requestUpdateService;
    this.requestUpdateFormValidator = requestUpdateFormValidator;
  }

  @GetMapping
  public ModelAndView render(
      @PathVariable UUID scheduleWorkProgrammeApplicationDetailId,
      ScheduleWorkProgrammeApplicationDetail applicationDetail
  ) {
    return getModelAndView(applicationDetail, requestUpdateService.getFormForApplication(applicationDetail));
  }

  @PostMapping
  ModelAndView save(
      @PathVariable UUID scheduleWorkProgrammeApplicationDetailId,
      ScheduleWorkProgrammeApplicationDetail applicationDetail,
      @ModelAttribute("form") RequestUpdateForm form,
      BindingResult bindingResult,
      ServiceUserDetail user,
      RedirectAttributes redirectAttributes
  ) {
    if (!requestUpdateFormValidator.isValid(form, bindingResult)) {
      return getModelAndView(applicationDetail, form);
    }

    requestUpdateService.requestUpdate(applicationDetail, form, user);

    NotificationBanner.newSuccessBanner()
        .withHeadingContent("Update requested on %s".formatted(
            applicationDetail.getScheduleWorkProgrammeApplication().getApplicationReference()))
        .applyTo(redirectAttributes);

    return ReverseRouter.redirect(on(ScheduleWorkProgrammeApplicationOverviewController.class)
        .renderOverview(applicationDetail.getId(), null, null, null));
  }

  private ModelAndView getModelAndView(
      ScheduleWorkProgrammeApplicationDetail applicationDetail,
      RequestUpdateForm form
  ) {
    return new ModelAndView("lms/licence/application/requestUpdate")
        .addObject("pageTitle", PAGE_TITLE)
        .addObject("caption", applicationDetail.getLicence().getType().getDisplayName())
        .addObject("form", form)
        .addObject("backUrl", ReverseRouter.route(on(ScheduleWorkProgrammeApplicationOverviewController.class)
            .renderOverview(applicationDetail.getId(), null, null, null)));
  }
}
