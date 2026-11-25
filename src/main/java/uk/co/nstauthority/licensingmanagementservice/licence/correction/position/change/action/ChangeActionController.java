package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.action;

import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.CorrectionHasStatus;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.InvokingUserCanViewCorrection;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.LicencePositionIsNotRemovedInCorrection;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.change.LicencePositionChangeBelongsToPosition;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.position.LicencePositionBelongsToCorrectionLicence;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionStagedPayloadRouteUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionStagedPayloadService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Controller
@RequestMapping("/licence-corrections/{correctionId}")
@InvokingUserCanViewCorrection
@CorrectionHasStatus(LicenceCorrectionStatus.IN_PROGRESS)
public class ChangeActionController {

  private final ChangeActionService changeActionService;
  private final LicencePositionStagedPayloadService licencePositionStagedPayloadService;

  ChangeActionController(
      ChangeActionService changeActionService,
      LicencePositionStagedPayloadService licencePositionStagedPayloadService
  ) {
    this.changeActionService = changeActionService;
    this.licencePositionStagedPayloadService = licencePositionStagedPayloadService;
  }

  @RequestMapping(
      value = "/position/{licencePositionId}/change/{changeId}/remove",
      method = {RequestMethod.GET, RequestMethod.POST}
  )
  @LicencePositionIsNotRemovedInCorrection
  @LicencePositionChangeBelongsToPosition
  @LicencePositionBelongsToCorrectionLicence
  public ModelAndView removeChange(
      LicenceCorrection correction,
      LicencePosition licencePosition,
      LicencePositionChange change,
      HttpMethod httpMethod,
      RedirectAttributes redirectAttributes
  ) {
    var operation = changeActionService.getOperationOfLiveChange(change);
    var positionPageUrl = positionPageUrl(correction, licencePosition);

    if (HttpMethod.GET.equals(httpMethod)) {
      return withConfirmationObjects(
          changeActionService.getHandler(operation).executedChangeConfirmation(correction, licencePosition, change),
          operation.changeName(),
          ChangeAction.REMOVE,
          positionPageUrl
      );
    }

    changeActionService.removeLiveChange(correction, licencePosition, change);

    NotificationBanner.newSuccessBannerWithHeader(
        ChangeAction.REMOVE.successBannerHeading(operation.changeName()),
        redirectAttributes
    );

    return ReverseRouter.redirectToUrl(positionPageUrl);
  }

  @RequestMapping(value = "/change/{changeId}/undo", method = {RequestMethod.GET, RequestMethod.POST})
  public ModelAndView undoChange(
      LicenceCorrection correction,
      @PathVariable String changeId,
      HttpMethod httpMethod,
      RedirectAttributes redirectAttributes
  ) {
    var positionStagedPayload = licencePositionStagedPayloadService
        .getPositionStagedPayloadContainingChange(correction, changeId);
    var operation = changeActionService.getOperationOfStagedChange(positionStagedPayload, changeId);
    var action = changeActionService.getUndoAction(positionStagedPayload, changeId);
    // the position staged payload can be deleted by the undo, so the page to return to is worked out first
    var positionPageUrl = LicencePositionStagedPayloadRouteUtil.getPositionPageUrl(positionStagedPayload);

    if (HttpMethod.GET.equals(httpMethod)) {
      return withConfirmationObjects(
          changeActionService.getHandler(operation).stagedChangeConfirmation(correction, positionStagedPayload, changeId, action),
          operation.changeName(),
          action,
          positionPageUrl
      );
    }

    changeActionService.undoStagedChange(positionStagedPayload, changeId);

    NotificationBanner.newSuccessBannerWithHeader(action.successBannerHeading(operation.changeName()), redirectAttributes);

    return ReverseRouter.redirectToUrl(positionPageUrl);
  }

  private static ModelAndView withConfirmationObjects(
      ModelAndView confirmation,
      String changeName,
      ChangeAction action,
      String cancelUrl
  ) {
    return confirmation
        .addObject("pageTitle", action.pageTitle(changeName))
        .addObject("primaryButton", action.primaryButton(changeName))
        .addObject("cancelUrl", cancelUrl);
  }

  private static String positionPageUrl(LicenceCorrection correction, LicencePosition licencePosition) {
    return LicencePositionStagedPayloadRouteUtil.getPositionPageUrl(correction, licencePosition.getId());
  }
}
