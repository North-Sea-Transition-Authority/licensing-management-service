package uk.co.nstauthority.licensingmanagementservice.licence.continuation.overview.requestupdate;

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
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.continuationapplication.ContinuationApplicationHasStatus;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.continuationapplication.InvokingUserCanAccessContinuationApplication;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.continuationapplication.LicenceContinuationActionEndPointInterceptorRule;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.application.requestupdate.RequestUpdateForm;
import uk.co.nstauthority.licensingmanagementservice.licence.application.requestupdate.RequestUpdateFormValidator;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.overview.LicenceContinuationApplicationOverviewController;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.overview.action.LicenceContinuationActionItem;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Controller
@RequestMapping("licence/continuation-application/{licenceContinuationApplicationDetailId}/request-update")
@ContinuationApplicationHasStatus(ApplicationStatus.SUBMITTED)
@InvokingUserCanAccessContinuationApplication
@LicenceContinuationActionEndPointInterceptorRule.ActionEndPoint(LicenceContinuationActionItem.REQUEST_UPDATE)
public class LicenceContinuationRequestUpdateController {

  static final String PAGE_TITLE = "Request an update";

  private final LicenceContinuationRequestUpdateService requestUpdateService;
  private final RequestUpdateFormValidator requestUpdateFormValidator;

  public LicenceContinuationRequestUpdateController(
      LicenceContinuationRequestUpdateService requestUpdateService,
      RequestUpdateFormValidator requestUpdateFormValidator
  ) {
    this.requestUpdateService = requestUpdateService;
    this.requestUpdateFormValidator = requestUpdateFormValidator;
  }

  @GetMapping
  public ModelAndView render(
      @PathVariable UUID licenceContinuationApplicationDetailId,
      LicenceContinuationApplicationDetail applicationDetail
  ) {
    return getModelAndView(applicationDetail, requestUpdateService.getFormForApplication(applicationDetail));
  }

  @PostMapping
  ModelAndView save(
      @PathVariable UUID licenceContinuationApplicationDetailId,
      LicenceContinuationApplicationDetail applicationDetail,
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
            applicationDetail.getLicenceContinuationApplication().getApplicationReference()))
        .applyTo(redirectAttributes);

    return ReverseRouter.redirect(on(LicenceContinuationApplicationOverviewController.class)
        .renderOverview(applicationDetail.getId(), null, null, null));
  }

  private ModelAndView getModelAndView(
      LicenceContinuationApplicationDetail applicationDetail,
      RequestUpdateForm form
  ) {
    return new ModelAndView("lms/licence/application/requestUpdate")
        .addObject("pageTitle", PAGE_TITLE)
        .addObject("caption", applicationDetail.getLicence().getType().getDisplayName())
        .addObject("form", form)
        .addObject("backUrl", ReverseRouter.route(on(LicenceContinuationApplicationOverviewController.class)
            .renderOverview(applicationDetail.getId(), null, null, null)));
  }
}
