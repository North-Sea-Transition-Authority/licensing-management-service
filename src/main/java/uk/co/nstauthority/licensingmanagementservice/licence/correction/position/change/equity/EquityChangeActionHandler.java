package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.equity;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionStagedPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.action.ChangeAction;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.action.ChangeActionHandler;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;

@Component
public class EquityChangeActionHandler implements ChangeActionHandler {

  private static final String TEMPLATE_NAME = "lms/licence/correction/change/removeEquityChange";

  private final EquityChangeService equityChangeService;

  EquityChangeActionHandler(EquityChangeService equityChangeService) {
    this.equityChangeService = equityChangeService;
  }

  @Override
  public ModelAndView executedChangeConfirmation(
      LicenceCorrection licenceCorrection,
      LicencePosition licencePosition,
      LicencePositionChange liveChange
  ) {
    return confirmation(equityChangeService.getExecutedEquityChangeContext(liveChange.getId().toString()));
  }

  @Override
  public ModelAndView stagedChangeConfirmation(
      LicenceCorrection licenceCorrection,
      LicencePositionStagedPayload positionStagedPayload,
      String changeId,
      ChangeAction action
  ) {
    return confirmation(equityChangeService.getEquityChangeContext(licenceCorrection, changeId));
  }

  private static ModelAndView confirmation(EquityChangeContext equityChangeContext) {
    return new ModelAndView(TEMPLATE_NAME)
        .addObject("subareaLabel", equityChangeContext.subareaLabel())
        .addObject("setEquityRows", equityChangeContext.setEquityRows())
        .addObject("transferEquityRows", equityChangeContext.transferEquityRows());
  }
}
