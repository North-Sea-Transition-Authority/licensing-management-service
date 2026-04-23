package uk.co.nstauthority.licensingmanagementservice.licence.continuation.overview;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.continuationapplication.LicenceContinuationActionEndPointInterceptorRule;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
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

  public LicenceContinuationApplicationWithdrawConfirmationController(
      LicenceContinuationService licenceContinuationService
  ) {
    this.licenceContinuationService = licenceContinuationService;
  }

  @PostMapping
  public ModelAndView submitForm(
      @PathVariable UUID licenceContinuationApplicationDetailId,
      LicenceContinuationApplicationDetail applicationDetail,
      RedirectAttributes redirectAttributes
  ) {
    licenceContinuationService.withdrawContinuationChangeStatus(applicationDetail);
    NotificationBanner.newSuccessBannerWithHeader(
        "Continuation application has been withdrawn",
        redirectAttributes
    );

    return ReverseRouter.redirect(on(WorkAreaController.class).getWorkArea(null, null));
  }

  @GetMapping
  public ModelAndView renderWithdrawConfirmation(
      @PathVariable UUID licenceContinuationApplicationDetailId,
      LicenceContinuationApplicationDetail applicationDetail
  ) {
    return getModelAndView(licenceContinuationApplicationDetailId);
  }

  private ModelAndView getModelAndView(UUID licenceContinuationApplicationDetailId) {
    return new ModelAndView("lms/licence/continuation/licenceContinuationWithdrawConfirmation")
        .addObject(
            "cancelUrl",
            ReverseRouter.route(on(LicenceContinuationApplicationOverviewController.class).renderOverview(
                licenceContinuationApplicationDetailId,
                null,
                null,
                null
            ))
    );
  }
}