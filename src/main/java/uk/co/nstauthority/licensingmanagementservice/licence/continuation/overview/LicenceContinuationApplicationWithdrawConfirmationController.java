package uk.co.nstauthority.licensingmanagementservice.licence.continuation.overview;

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
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.continuationapplication.LicenceContinuationActionEndPointInterceptorRule;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceService;
import uk.co.nstauthority.licensingmanagementservice.licence.application.withdraw.ApplicationWithdrawReasonForm;
import uk.co.nstauthority.licensingmanagementservice.licence.application.withdraw.ApplicationWithdrawReasonValidator;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationService;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.overview.action.LicenceContinuationActionItem;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.workarea.WorkAreaController;

@Controller
@RequestMapping("licence/continuation-application/{licenceContinuationApplicationDetailId}/overview/withdraw")
@LicenceContinuationActionEndPointInterceptorRule.ActionEndPoint(
    LicenceContinuationActionItem.WITHDRAW_CONTINUATION)
public class LicenceContinuationApplicationWithdrawConfirmationController {

  private final LicenceContinuationService licenceContinuationService;
  private final ApplicationWithdrawReasonValidator applicationWithdrawReasonValidator;
  private final LicenceService licenceService;

  public LicenceContinuationApplicationWithdrawConfirmationController(
      LicenceContinuationService licenceContinuationService,
      ApplicationWithdrawReasonValidator applicationWithdrawReasonValidator,
      LicenceService licenceService
  ) {
    this.licenceContinuationService = licenceContinuationService;
    this.applicationWithdrawReasonValidator = applicationWithdrawReasonValidator;
    this.licenceService = licenceService;
  }

  @PostMapping
  public ModelAndView submitForm(
      @PathVariable UUID licenceContinuationApplicationDetailId,
      @ModelAttribute("form") ApplicationWithdrawReasonForm form,
      BindingResult bindingResult,
      LicenceContinuationApplicationDetail applicationDetail,
      RedirectAttributes redirectAttributes
  ) {

    if (!applicationWithdrawReasonValidator.isValid(bindingResult)) {
      return getModelAndView(licenceContinuationApplicationDetailId, form, applicationDetail);
    }

    licenceContinuationService.withdrawContinuationChangeStatus(applicationDetail, form.getReasonForWithdrawal());
    NotificationBanner.newSuccessBannerWithHeader(
        "Continuation application %s for licence %s has been withdrawn".formatted(
            applicationDetail.getLicenceContinuationApplication().getApplicationReference(),
            applicationDetail.getLicenceContinuationApplication().getLicenceSchedule().getLicence().getLicenceReference()
        ),
        redirectAttributes
    );

    return ReverseRouter.redirect(on(WorkAreaController.class).getWorkArea(null, null));
  }

  @GetMapping
  public ModelAndView renderWithdrawConfirmation(
      @PathVariable UUID licenceContinuationApplicationDetailId,
      LicenceContinuationApplicationDetail applicationDetail
  ) {
    return getModelAndView(licenceContinuationApplicationDetailId, new ApplicationWithdrawReasonForm(), applicationDetail);
  }

  private ModelAndView getModelAndView(
      UUID licenceContinuationApplicationDetailId,
      ApplicationWithdrawReasonForm form,
      LicenceContinuationApplicationDetail applicationDetail
  ) {
    var licence = applicationDetail.getLicenceContinuationApplication()
        .getLicenceSchedule()
        .getLicence();

    var pageCaption = "%s - %s".formatted(
        licenceService.getLicencePageCaption(licence),
        applicationDetail.getLicenceContinuationApplication().getApplicationReference()
    );

    return new ModelAndView("lms/licence/continuation/licenceContinuationWithdrawConfirmation")
        .addObject(
            "cancelUrl",
            ReverseRouter.route(on(LicenceContinuationApplicationOverviewController.class).renderOverview(
                licenceContinuationApplicationDetailId,
                null,
                null,
                null
            ))
    )
        .addObject("form", form)
        .addObject("pageCaption", pageCaption);
  }
}