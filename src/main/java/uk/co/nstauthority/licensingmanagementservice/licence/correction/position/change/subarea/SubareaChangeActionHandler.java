package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.subarea;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.action.ChangeAction;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.action.ChangeActionHandler;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.LicencePositionStagedPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.changetypes.LicencePositionChangeType;

@Component
public class SubareaChangeActionHandler implements ChangeActionHandler {

  private static final String TEMPLATE_NAME = "lms/licence/correction/change/subarea/removeSubareaChange";

  private final SubareaChangeService subareaChangeService;

  SubareaChangeActionHandler(SubareaChangeService subareaChangeService) {
    this.subareaChangeService = subareaChangeService;
  }

  @Override
  public ModelAndView executedChangeConfirmation(
      LicenceCorrection licenceCorrection,
      LicencePosition licencePosition,
      LicencePositionChange liveChange
  ) {
    return confirmation(subareaChangeService.getLiveSubareaOperationOrThrow(liveChange));
  }

  @Override
  public ModelAndView stagedChangeConfirmation(
      LicenceCorrection licenceCorrection,
      LicencePositionStagedPayload positionStagedPayload,
      String changeId,
      ChangeAction action
  ) {
    return confirmation(subareaChangeService.getStagedSubareaOperationOrThrow(positionStagedPayload, changeId));
  }

  @Override
  public void beforeDroppingStagedChange(
      LicencePositionStagedPayload positionStagedPayload,
      LicencePositionChangeType stagedChange
  ) {
    subareaChangeService.deleteStagedSubareaArtefacts(stagedChange);
  }

  private ModelAndView confirmation(SubareaOperation subareaOperation) {
    return new ModelAndView(TEMPLATE_NAME).addObject("blockName", subareaChangeService.getBlockName(subareaOperation));
  }
}
