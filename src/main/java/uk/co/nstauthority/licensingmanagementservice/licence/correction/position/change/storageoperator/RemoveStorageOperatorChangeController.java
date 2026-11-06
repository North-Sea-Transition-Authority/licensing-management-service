package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.storageoperator;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.InvokingUserCanViewCorrection;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.LicencePositionIsNotRemovedInCorrection;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.change.LicencePositionChangeBelongsToPosition;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.change.LicencePositionChangeIsOfType;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.position.LicencePositionBelongsToCorrectionLicence;
import uk.co.nstauthority.licensingmanagementservice.fds.button.Button;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.StorageOperatorOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionViewService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Controller
@RequestMapping("/licence-corrections/{correctionId}")
@InvokingUserCanViewCorrection
public class RemoveStorageOperatorChangeController {

  private static final String REMOVE_PAGE_TITLE = "Are you sure you want to remove this storage operator change?";
  private static final String CONFIRMATION_TEMPLATE = "lms/licence/correction/change/removeStorageOperatorChange";

  private final LicencePositionViewService licencePositionViewService;
  private final StorageOperatorChangeService storageOperatorChangeService;

  RemoveStorageOperatorChangeController(
      LicencePositionViewService licencePositionViewService,
      StorageOperatorChangeService storageOperatorChangeService
  ) {
    this.licencePositionViewService = licencePositionViewService;
    this.storageOperatorChangeService = storageOperatorChangeService;
  }

  @GetMapping("/position/{licencePositionId}/change/{changeId}/remove-storage-operator-change")
  @LicencePositionIsNotRemovedInCorrection
  @LicencePositionChangeBelongsToPosition
  @LicencePositionChangeIsOfType(StorageOperatorOperation.class)
  @LicencePositionBelongsToCorrectionLicence
  public ModelAndView renderRemoveExecutedStorageOperatorChange(
      LicenceCorrection correction,
      LicencePosition licencePosition,
      LicencePositionChange licencePositionChange
  ) {
    return removeStorageOperatorChangeModelAndView(correction, licencePosition);
  }

  @PostMapping("/position/{licencePositionId}/change/{changeId}/remove-storage-operator-change")
  @LicencePositionIsNotRemovedInCorrection
  @LicencePositionChangeBelongsToPosition
  @LicencePositionChangeIsOfType(StorageOperatorOperation.class)
  @LicencePositionBelongsToCorrectionLicence
  public ModelAndView removeStorageOperatorChange(
      LicenceCorrection correction,
      LicencePosition licencePosition,
      LicencePositionChange change,
      RedirectAttributes redirectAttributes
  ) {
    storageOperatorChangeService
        .removeExistingStorageOperatorChange(licencePosition, correction, change.getId().toString());

    NotificationBanner.newSuccessBannerWithHeader("Storage operator change removed", redirectAttributes);

    return ReverseRouter.redirect(on(LicenceCorrectionController.class)
        .renderLicencePosition(correction, licencePosition, null));
  }

  private ModelAndView removeStorageOperatorChangeModelAndView(
      LicenceCorrection correction,
      LicencePosition licencePosition
  ) {
    var storageOperatorChangeContext = licencePositionViewService
        .getStorageOperatorChangeContext(correction, licencePosition.getId());

    return new ModelAndView(CONFIRMATION_TEMPLATE)
        .addObject("pageTitle", REMOVE_PAGE_TITLE)
        .addObject("primaryButton", Button.warningButton("Remove storage operator change"))
        .addObject("withdrawingStorageOperatorName", storageOperatorChangeContext.previousOperatorName())
        .addObject("joiningStorageOperatorName", storageOperatorChangeContext.currentOperatorName())
        .addObject("cancelUrl", ReverseRouter.route(on(LicenceCorrectionController.class)
                .renderLicencePosition(correction, licencePosition, null)));
  }
}
