package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.licensee;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.action.ChangeAction;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.action.ChangeActionHandler;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.ChangeViewUrls;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.LicenseeChangeView;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.LicencePositionStagedPayload;

@Component
public class LicenseeChangeActionHandler implements ChangeActionHandler {

  private static final String TEMPLATE_NAME = "lms/licence/correction/change/removeLicenseeChange";

  private final LicenseeChangeService licenseeChangeService;

  LicenseeChangeActionHandler(LicenseeChangeService licenseeChangeService) {
    this.licenseeChangeService = licenseeChangeService;
  }

  @Override
  public ModelAndView executedChangeConfirmation(
      LicenceCorrection licenceCorrection,
      LicencePosition licencePosition,
      LicencePositionChange liveChange
  ) {
    var licenseeChangeContext =
        licenseeChangeService.getLicenseeChangeContext(licenceCorrection, licencePosition.getId());

    var correctedLicenseeChangeView = new LicenseeChangeView(
        licenseeChangeContext.currentWithdrawingLicenseeNames(),
        licenseeChangeContext.currentJoiningLicenseeNames(),
        "",
        ChangeViewUrls.none()
    );

    return new ModelAndView(TEMPLATE_NAME)
        .addObject("correctedLicenseeChangeView", correctedLicenseeChangeView)
        .addObject("resultingLicensees", licenseeChangeContext.previousLicenseeNames());
  }

  @Override
  public ModelAndView stagedChangeConfirmation(
      LicenceCorrection licenceCorrection,
      LicencePositionStagedPayload positionStagedPayload,
      String changeId,
      ChangeAction action
  ) {
    var undoLicenseeChangeContext = licenseeChangeService.getUndoLicenseeChangeContext(
        licenceCorrection,
        positionStagedPayload,
        positionStagedPayload.getPositionId()
    );

    var restoredLicenseeChangeView = licenseeChangeService.getRestoredLicenseeChangeView(
        undoLicenseeChangeContext.restoredWithdrawingLicenseeNames(),
        undoLicenseeChangeContext.restoredJoiningLicenseeNames()
    );

    // a reinstated change has no corrected version to show, only the live one coming back
    var correctedLicenseeChangeView = action == ChangeAction.REINSTATE
        ? null
        : new LicenseeChangeView(
            undoLicenseeChangeContext.currentWithdrawingLicenseeNames(),
            undoLicenseeChangeContext.currentJoiningLicenseeNames(),
            "",
            ChangeViewUrls.none()
        );

    return new ModelAndView(TEMPLATE_NAME)
        .addObject("correctedLicenseeChangeView", correctedLicenseeChangeView)
        .addObject("restoredLicenseeChangeView", restoredLicenseeChangeView)
        .addObject("resultingLicensees", undoLicenseeChangeContext.resultingLicenseeNames());
  }
}
