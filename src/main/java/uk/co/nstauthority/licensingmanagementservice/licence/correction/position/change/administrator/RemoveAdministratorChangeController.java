package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.administrator;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.InvokingUserCanViewCorrection;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.change.LicencePositionChangeBelongsToPosition;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.change.administrator.ValidLicencePositionAdministratorChange;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionViewService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Controller
@RequestMapping("/licence-corrections/{correctionId}/position/{licencePositionId}/change/{changeId}/remove-administrator-change")
@Profile("enable-lms2")
@InvokingUserCanViewCorrection
@LicencePositionChangeBelongsToPosition
@ValidLicencePositionAdministratorChange
public class RemoveAdministratorChangeController {

  private static final String PAGE_TITLE = "Are you sure you want to remove this licence administrator change?";

  private final LicencePositionCorrectionService licencePositionCorrectionService;
  private final LicencePositionService licencePositionService;
  private final LicencePositionViewService licencePositionViewService;

  public RemoveAdministratorChangeController(
      LicencePositionCorrectionService licencePositionCorrectionService,
      LicencePositionService licencePositionService,
      LicencePositionViewService licencePositionViewService
  ) {
    this.licencePositionCorrectionService = licencePositionCorrectionService;
    this.licencePositionService = licencePositionService;
    this.licencePositionViewService = licencePositionViewService;
  }

  @GetMapping
  public ModelAndView renderRemoveExecutedAdminChange(
      @PathVariable UUID correctionId,
      @PathVariable UUID licencePositionId,
      @PathVariable String changeId,
      @RequestAttribute("validatedCorrection") LicenceCorrection correction
  ) {
    var licencePosition = licencePositionService
        .getPositionForLicence(correction.getLicence(), licencePositionId);

    return removeAdministratorChangeModelAndView(correction, licencePosition.getId());
  }

  @PostMapping
  public ModelAndView removeAdministratorChange(
      @PathVariable UUID correctionId,
      @PathVariable UUID licencePositionId,
      @PathVariable String changeId,
      @RequestAttribute("validatedCorrection") LicenceCorrection correction,
      RedirectAttributes redirectAttributes
  ) {
    var licencePosition = licencePositionService
        .getPositionForLicence(correction.getLicence(), licencePositionId);

    licencePositionCorrectionService.removeExistingAdministratorChange(licencePosition, correction, changeId);

    NotificationBanner.newSuccessBannerWithHeader("Licence administrator change removed", redirectAttributes);

    return ReverseRouter.redirect(on(LicenceCorrectionController.class)
        .renderLicencePosition(correction.getId(), licencePositionId, null));
  }

  private ModelAndView removeAdministratorChangeModelAndView(
      LicenceCorrection correction,
      UUID licencePositionId
  ) {
    var administratorChangeContext =
        licencePositionViewService.getAdministratorChangeContext(correction, licencePositionId);

    return new ModelAndView("lms/licence/correction/change/removeAdministratorChange")
        .addObject("pageTitle", PAGE_TITLE)
        .addObject("withdrawingAdministratorName", administratorChangeContext.previousAdministratorName())
        .addObject("joiningAdministratorName", administratorChangeContext.currentAdministratorName())
        .addObject("cancelUrl",
            ReverseRouter.route(on(LicenceCorrectionController.class)
                .renderLicencePosition(correction.getId(), licencePositionId, null)));
  }
}
