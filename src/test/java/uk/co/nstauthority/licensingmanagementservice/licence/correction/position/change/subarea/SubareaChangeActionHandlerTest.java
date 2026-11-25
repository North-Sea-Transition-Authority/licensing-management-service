package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.subarea;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionStagedPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionStagedPayloadTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.action.ChangeAction;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeTestUtil;

@ExtendWith(MockitoExtension.class)
class SubareaChangeActionHandlerTest {

  private static final String TEMPLATE_NAME = "lms/licence/correction/change/subarea/removeSubareaChange";
  private static final String CHANGE_ID = UUID.randomUUID().toString();
  private static final LicenceCorrection CORRECTION = LicenceCorrectionTestUtil.newBuilder().build();
  private static final LicencePositionStagedPayload POSITION_STAGED_PAYLOAD = LicencePositionStagedPayloadTestUtil.newBuilder().build();
  private static final SubareaOperation SUBAREA_OPERATION = LicenceOperation.newSubAreaOperation()
      .withBlockFeatureId(UUID.randomUUID())
      .build();
  private static final ModelAndView EXPECTED_CONFIRMATION = new ModelAndView(TEMPLATE_NAME).addObject("blockName", "30/1");

  @Mock
  private SubareaChangeService subareaChangeService;

  @InjectMocks
  private SubareaChangeActionHandler subareaChangeActionHandler;

  @Test
  void executedChangeConfirmation() {
    var liveChange = LicencePositionChangeTestUtil.newBuilder().build();
    when(subareaChangeService.getLiveSubareaOperationOrThrow(liveChange)).thenReturn(SUBAREA_OPERATION);
    when(subareaChangeService.getBlockName(SUBAREA_OPERATION)).thenReturn("30/1");

    var result = subareaChangeActionHandler.executedChangeConfirmation(
        CORRECTION,
        LicencePositionTestUtil.newBuilder().build(),
        liveChange
    );

    assertThat(result).usingRecursiveComparison().isEqualTo(EXPECTED_CONFIRMATION);
  }

  @Test
  void stagedChangeConfirmation() {
    when(subareaChangeService.getStagedSubareaOperationOrThrow(POSITION_STAGED_PAYLOAD, CHANGE_ID))
        .thenReturn(SUBAREA_OPERATION);
    when(subareaChangeService.getBlockName(SUBAREA_OPERATION)).thenReturn("30/1");

    var result = subareaChangeActionHandler.stagedChangeConfirmation(
        CORRECTION,
        POSITION_STAGED_PAYLOAD,
        CHANGE_ID,
        ChangeAction.UNDO
    );

    assertThat(result).usingRecursiveComparison().isEqualTo(EXPECTED_CONFIRMATION);
  }

  @Test
  void beforeDroppingStagedChange() {
    var stagedChange = LicencePositionChangeType.removeChange().withChangeId(CHANGE_ID).build();

    subareaChangeActionHandler.beforeDroppingStagedChange(POSITION_STAGED_PAYLOAD, stagedChange);

    verify(subareaChangeService).deleteStagedSubareaArtefacts(stagedChange);
  }
}
