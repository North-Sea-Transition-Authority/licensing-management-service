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
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.LicencePositionIsNotRemovedInCorrection;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.change.LicencePositionChangeBelongsToPosition;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.change.LicencePositionChangeIsOfType;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.CreateLicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.AdministratorOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionViewService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Controller
@RequestMapping("/licence-corrections/{correctionId}")
@Profile("enable-lms2")
@InvokingUserCanViewCorrection
public class RemoveAdministratorChangeController {

  private static final String REMOVE_PAGE_TITLE = "Are you sure you want to remove this licence administrator change?";
  private static final String UNDO_PAGE_TITLE = "Are you sure you want to undo this licence administrator change?";
  private static final String CONFIRMATION_TEMPLATE = "lms/licence/correction/change/removeAdministratorChange";

  private final LicencePositionCorrectionService licencePositionCorrectionService;
  private final AdministratorChangeService administratorChangeService;
  private final LicencePositionService licencePositionService;
  private final LicencePositionViewService licencePositionViewService;

  public RemoveAdministratorChangeController(
      LicencePositionCorrectionService licencePositionCorrectionService,
      AdministratorChangeService administratorChangeService,
      LicencePositionService licencePositionService,
      LicencePositionViewService licencePositionViewService
  ) {
    this.licencePositionCorrectionService = licencePositionCorrectionService;
    this.administratorChangeService = administratorChangeService;
    this.licencePositionService = licencePositionService;
    this.licencePositionViewService = licencePositionViewService;
  }

  @GetMapping("/position/{licencePositionId}/change/{changeId}/remove-administrator-change")
  @LicencePositionIsNotRemovedInCorrection
  @LicencePositionChangeBelongsToPosition
  @LicencePositionChangeIsOfType(AdministratorOperation.class)
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

  @PostMapping("/position/{licencePositionId}/change/{changeId}/remove-administrator-change")
  @LicencePositionIsNotRemovedInCorrection
  @LicencePositionChangeBelongsToPosition
  @LicencePositionChangeIsOfType(AdministratorOperation.class)
  public ModelAndView removeAdministratorChange(
      @PathVariable UUID correctionId,
      @PathVariable UUID licencePositionId,
      @PathVariable String changeId,
      @RequestAttribute("validatedCorrection") LicenceCorrection correction,
      RedirectAttributes redirectAttributes
  ) {
    var licencePosition = licencePositionService
        .getPositionForLicence(correction.getLicence(), licencePositionId);

    administratorChangeService.removeExistingAdministratorChange(licencePosition, correction, changeId);

    NotificationBanner.newSuccessBannerWithHeader("Licence administrator change removed", redirectAttributes);

    return ReverseRouter.redirect(on(LicenceCorrectionController.class)
        .renderLicencePosition(correction.getId(), licencePositionId, null));
  }

  @GetMapping("/change/{changeId}/undo-administrator-change")
  public ModelAndView renderUndoAdminChange(
      @PathVariable UUID correctionId,
      @PathVariable String changeId,
      @RequestAttribute("validatedCorrection") LicenceCorrection correction
  ) {
    var positionCorrection = licencePositionCorrectionService
        .getPositionCorrectionContainingChange(correction, changeId);

    return undoAdministratorChangeModelAndView(correction, positionCorrection);
  }

  @PostMapping("/change/{changeId}/undo-administrator-change")
  public ModelAndView undoAdminChange(
      @PathVariable UUID correctionId,
      @PathVariable String changeId,
      @RequestAttribute("validatedCorrection") LicenceCorrection correction,
      RedirectAttributes redirectAttributes
  ) {
    var positionCorrection = licencePositionCorrectionService
        .getPositionCorrectionContainingChange(correction, changeId);

    administratorChangeService.undoAdministratorChange(correction, changeId);

    NotificationBanner.newSuccessBannerWithHeader("Licence administrator change undone", redirectAttributes);

    if (positionCorrection.getChangeType() == LicencePositionCorrectionChangeType.ADD_POSITION) {
      return ReverseRouter.redirect(on(LicenceCorrectionController.class)
          .renderAddedPosition(correction.getId(), positionCorrection.getId(), null));
    }
    return ReverseRouter.redirect(on(LicenceCorrectionController.class)
        .renderLicencePosition(correction.getId(), positionCorrection.getTargetLicencePosition().getId(), null));
  }

  private ModelAndView removeAdministratorChangeModelAndView(
      LicenceCorrection correction,
      UUID licencePositionId
  ) {
    var administratorChangeContext =
        licencePositionViewService.getAdministratorChangeContext(correction, licencePositionId);

    return new ModelAndView(CONFIRMATION_TEMPLATE)
        .addObject("pageTitle", REMOVE_PAGE_TITLE)
        .addObject("primaryButtonText", "Remove administrator change")
        .addObject("withdrawingAdministratorName", administratorChangeContext.previousAdministratorName())
        .addObject("joiningAdministratorName", administratorChangeContext.currentAdministratorName())
        .addObject("cancelUrl",
            ReverseRouter.route(on(LicenceCorrectionController.class)
                .renderLicencePosition(correction.getId(), licencePositionId, null)));
  }

  private ModelAndView undoAdministratorChangeModelAndView(
      LicenceCorrection correction,
      LicencePositionCorrection positionCorrection
  ) {
    var administratorChangeContext =
        licencePositionViewService.getAdministratorChangeContext(correction, getPositionId(positionCorrection));

    return new ModelAndView(CONFIRMATION_TEMPLATE)
        .addObject("pageTitle", UNDO_PAGE_TITLE)
        .addObject("primaryButtonText", "Undo administrator change")
        .addObject("withdrawingAdministratorName", administratorChangeContext.previousAdministratorName())
        .addObject("joiningAdministratorName", administratorChangeContext.currentAdministratorName())
        .addObject("cancelUrl", positionPageRoute(correction, positionCorrection));
  }

  private String positionPageRoute(LicenceCorrection correction, LicencePositionCorrection positionCorrection) {
    if (positionCorrection.getChangeType() == LicencePositionCorrectionChangeType.ADD_POSITION) {
      return ReverseRouter.route(on(LicenceCorrectionController.class)
          .renderAddedPosition(correction.getId(), positionCorrection.getId(), null));
    }
    return ReverseRouter.route(on(LicenceCorrectionController.class)
        .renderLicencePosition(correction.getId(), positionCorrection.getTargetLicencePosition().getId(), null));
  }

  private UUID getPositionId(LicencePositionCorrection positionCorrection) {
    if (positionCorrection.getChangeType() == LicencePositionCorrectionChangeType.ADD_POSITION) {
      var payload = (CreateLicencePositionPayload) positionCorrection.getPayload();
      return UUID.fromString(payload.licencePositionId());
    }
    return positionCorrection.getTargetLicencePosition().getId();
  }
}
