package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.licensee;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.action.ChangeAction;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.licensee.LicenseeChangeContext;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.licensee.UndoLicenseeChangeContext;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.ChangeViewUrls;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.LicenseeChangeView;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.LicencePositionStagedPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.LicencePositionStagedPayloadChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.LicencePositionStagedPayloadTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.payloads.UpdateLicencePositionPayloadTestUtil;

@ExtendWith(MockitoExtension.class)
class LicenseeChangeActionHandlerTest {

  private static final String TEMPLATE_NAME = "lms/licence/correction/change/removeLicenseeChange";
  private static final String CHANGE_ID = UUID.randomUUID().toString();
  private static final LicenceCorrection CORRECTION = LicenceCorrectionTestUtil.newBuilder().build();
  private static final LicencePosition POSITION = LicencePositionTestUtil.newBuilder().withId(UUID.randomUUID()).build();
  private static final LicencePositionStagedPayload POSITION_STAGED_PAYLOAD = LicencePositionStagedPayloadTestUtil.newBuilder()
      .withChangeType(LicencePositionStagedPayloadChangeType.UPDATE_POSITION)
      .withTargetLicencePosition(POSITION)
      .withPayload(UpdateLicencePositionPayloadTestUtil.newBuilder().build())
      .build();
  private static final UndoLicenseeChangeContext UNDO_CONTEXT = new UndoLicenseeChangeContext(
      List.of("Current withdrawing"),
      List.of("Current joining"),
      List.of("Restored withdrawing"),
      List.of("Restored joining"),
      List.of("Resulting licensee")
  );
  private static final LicenseeChangeView RESTORED_VIEW =
      new LicenseeChangeView(List.of("Restored withdrawing"), List.of("Restored joining"), "", ChangeViewUrls.none());

  @Mock
  private LicenseeChangeService licenseeChangeService;

  @InjectMocks
  private LicenseeChangeActionHandler licenseeChangeActionHandler;

  @Test
  void executedChangeConfirmation() {
    var context = new LicenseeChangeContext(
        List.of(3),
        List.of(1),
        List.of(1, 2),
        List.of(2, 3),
        Map.of(1, "Withdrawing", 2, "Staying", 3, "Joining")
    );
    when(licenseeChangeService.getLicenseeChangeContext(CORRECTION, POSITION.getId())).thenReturn(context);

    var result = licenseeChangeActionHandler.executedChangeConfirmation(
        CORRECTION,
        POSITION,
        LicencePositionChangeTestUtil.newBuilder().build()
    );

    var expected = new ModelAndView(TEMPLATE_NAME)
        .addObject("correctedLicenseeChangeView", new LicenseeChangeView(
            context.currentWithdrawingLicenseeNames(),
            context.currentJoiningLicenseeNames(),
            "",
            ChangeViewUrls.none()
        ))
        .addObject("resultingLicensees", context.previousLicenseeNames());
    assertThat(result).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void stagedChangeSummary_whenUndoing_thenShowsTheCorrectedAndRestoredChanges() {
    givenUndoContext();

    var result = licenseeChangeActionHandler.stagedChangeConfirmation(
        CORRECTION,
        POSITION_STAGED_PAYLOAD,
        CHANGE_ID,
        ChangeAction.UNDO
    );

    var expected = new ModelAndView(TEMPLATE_NAME)
        .addObject("correctedLicenseeChangeView",
            new LicenseeChangeView(List.of("Current withdrawing"), List.of("Current joining"), "", ChangeViewUrls.none()))
        .addObject("restoredLicenseeChangeView", RESTORED_VIEW)
        .addObject("resultingLicensees", List.of("Resulting licensee"));
    assertThat(result).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void stagedChangeSummary_whenReinstating_thenShowsOnlyTheRestoredChange() {
    givenUndoContext();

    var result = licenseeChangeActionHandler.stagedChangeConfirmation(
        CORRECTION,
        POSITION_STAGED_PAYLOAD,
        CHANGE_ID,
        ChangeAction.REINSTATE
    );

    var expected = new ModelAndView(TEMPLATE_NAME)
        .addObject("correctedLicenseeChangeView", null)
        .addObject("restoredLicenseeChangeView", RESTORED_VIEW)
        .addObject("resultingLicensees", List.of("Resulting licensee"));
    assertThat(result).usingRecursiveComparison().isEqualTo(expected);
  }

  private void givenUndoContext() {
    when(licenseeChangeService.getUndoLicenseeChangeContext(CORRECTION, POSITION_STAGED_PAYLOAD, POSITION.getId()))
        .thenReturn(UNDO_CONTEXT);
    when(licenseeChangeService.getRestoredLicenseeChangeView(List.of("Restored withdrawing"), List.of("Restored joining")))
        .thenReturn(RESTORED_VIEW);
  }
}
