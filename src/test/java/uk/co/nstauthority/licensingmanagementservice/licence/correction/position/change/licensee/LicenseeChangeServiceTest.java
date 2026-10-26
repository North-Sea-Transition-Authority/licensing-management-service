package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.licensee;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changeoperation.LicencePositionChangeOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.CreateLicencePositionPayloadTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.LicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.UpdateLicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.UpdateLicencePositionPayloadTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenseeOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionViewService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.licensee.LicenseeChangeContext;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.ChangeViewUrls;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.LicenseeChangeView;

@ExtendWith(MockitoExtension.class)
class LicenseeChangeServiceTest {

  @Captor
  private ArgumentCaptor<LicencePositionCorrection> licencePositionCorrectionCaptor;

  @Mock
  private LicencePositionCorrectionService licencePositionCorrectionService;

  @Mock
  private LicencePositionViewService licencePositionViewService;

  @InjectMocks
  private LicenseeChangeService licenseeChangeService;

  private static final Licence LICENCE = LicenceTestUtil.builder().build();
  private static final UUID POSITION_ID = UUID.randomUUID();

  @Test
  void addLicenseeChangeForAddedLicencePosition() {
    var payload = CreateLicencePositionPayloadTestUtil.newBuilder().build();
    var joiningLicenseeIds = List.of(2, 3, 4);
    var withdrawingLicenseeIds = List.of(1);
    var licencePositionCorrection = LicencePositionCorrectionTestUtil.newBuilder().withPayload(payload).build();

    licenseeChangeService.addLicenseeChangeForAddedLicencePosition(
        licencePositionCorrection,
        joiningLicenseeIds,
        withdrawingLicenseeIds
    );

    verify(licencePositionCorrectionService).save(licencePositionCorrection);
    var expectedLicenseeOperation = (LicenseeOperation)
        licencePositionCorrection.getPayload().changes().getFirst().operations().getFirst().operation();
    assertThat(licencePositionCorrection.getPayload().changes()).hasSize(1);
    assertThat(expectedLicenseeOperation.licenseesToAdd()).isEqualTo(joiningLicenseeIds);
    assertThat(expectedLicenseeOperation.licenseesToRemove()).isEqualTo(withdrawingLicenseeIds);
  }

  @Test
  void addLicenseeChangeForExistingLicencePosition() {
    var licencePosition = LicencePositionTestUtil.newBuilder().withPositionOrder(10).build();
    var licenceCorrection = LicenceCorrectionTestUtil.newBuilder().withCorrectionReference("correctionReference").build();
    var joiningLicenseeIds = List.of(2, 3, 4);
    var withdrawingLicenseeIds = List.of(1);
    var payload = UpdateLicencePositionPayloadTestUtil.newBuilder().build();
    var licencePositionCorrection = LicencePositionCorrectionTestUtil.newBuilder().withPayload(payload).build();

    when(licencePositionCorrectionService.getOrBuildUpdatePositionCorrection(licenceCorrection, licencePosition))
        .thenReturn(licencePositionCorrection);

    licenseeChangeService.addLicenseeChangeForExistingLicencePosition(
        licencePosition,
        licenceCorrection,
        joiningLicenseeIds,
        withdrawingLicenseeIds
    );

    ArgumentCaptor<LicencePositionCorrection> captor = ArgumentCaptor.forClass(LicencePositionCorrection.class);
    verify(licencePositionCorrectionService).save(captor.capture());

    var positionCorrection = captor.getValue();
    var expectedLicenseeOperation = (LicenseeOperation)
        positionCorrection.getPayload().changes().getFirst().operations().getFirst().operation();
    assertThat(positionCorrection.getPayload().changes()).hasSize(1);
    assertThat(expectedLicenseeOperation.licenseesToAdd()).isEqualTo(joiningLicenseeIds);
    assertThat(expectedLicenseeOperation.licenseesToRemove()).isEqualTo(withdrawingLicenseeIds);
  }

  @Test
  void correctExistingLicenseeChange() {
    var licencePosition = LicencePositionTestUtil.newBuilder().withPositionOrder(10).build();
    var licenceCorrection = LicenceCorrectionTestUtil.newBuilder().withCorrectionReference("correctionReference").build();
    var joiningLicenseeIds = List.of(2, 3, 4);
    var withdrawingLicenseeIds = List.of(1);
    var changeId = UUID.randomUUID().toString();
    var payload = UpdateLicencePositionPayloadTestUtil.newBuilder().build();
    var licencePositionCorrection = LicencePositionCorrectionTestUtil.newBuilder().withPayload(payload).build();

    when(licencePositionCorrectionService.getOrBuildUpdatePositionCorrection(licenceCorrection, licencePosition))
        .thenReturn(licencePositionCorrection);

    licenseeChangeService.correctExistingLicenseeChange(
        licencePosition,
        licenceCorrection,
        joiningLicenseeIds,
        withdrawingLicenseeIds,
        changeId
    );

    ArgumentCaptor<LicencePositionCorrection> captor = ArgumentCaptor.forClass(LicencePositionCorrection.class);
    verify(licencePositionCorrectionService).save(captor.capture());

    var positionCorrection = captor.getValue();
    var expectedLicenseeOperation = (LicenseeOperation)
        positionCorrection.getPayload().changes().getFirst().operations().getFirst().operation();
    assertThat(positionCorrection.getPayload().changes()).hasSize(1);
    assertThat(expectedLicenseeOperation.licenseesToAdd()).isEqualTo(joiningLicenseeIds);
    assertThat(expectedLicenseeOperation.licenseesToRemove()).isEqualTo(withdrawingLicenseeIds);
  }

  @Test
  void removeExistingLicenseeChange_whenNoExistingUpdateCorrection_thenOnlyStagesTheRemoval() {
    var licencePosition = LicencePositionTestUtil.newBuilder().build();
    var licenceCorrection = LicenceCorrectionTestUtil.newBuilder().withCorrectionReference("correctionReference").build();
    var originalChangeId = UUID.randomUUID().toString();

    when(licencePositionCorrectionService.findUpdatePositionCorrection(licenceCorrection, licencePosition))
        .thenReturn(Optional.empty());

    licenseeChangeService.removeExistingLicenseeChange(licencePosition, licenceCorrection, originalChangeId);

    verify(licencePositionCorrectionService)
        .stageRemovalOfExecutedChange(licenceCorrection, licencePosition, originalChangeId);
    verify(licencePositionCorrectionService, never()).save(any());
  }

  @Test
  void removeExistingLicenseeChange_whenExistingUpdateCorrection_thenDropsLicenseeChangeAndStagesTheRemoval() {
    var licencePosition = LicencePositionTestUtil.newBuilder().withPositionOrder(10).build();
    var licenceCorrection = LicenceCorrectionTestUtil.newBuilder().withCorrectionReference("correctionReference").build();
    var originalChangeId = UUID.randomUUID().toString();

    var existing = LicencePositionCorrectionTestUtil.newBuilder()
        .withPayload(LicencePositionPayload.newUpdateLicencePositionPayload()
            .withChanges(List.of(licenseeUpdateChange(originalChangeId, List.of(1), List.of(3))))
            .build())
        .build();

    when(licencePositionCorrectionService.findUpdatePositionCorrection(licenceCorrection, licencePosition))
        .thenReturn(Optional.of(existing));

    licenseeChangeService.removeExistingLicenseeChange(licencePosition, licenceCorrection, originalChangeId);

    verify(licencePositionCorrectionService).save(licencePositionCorrectionCaptor.capture());
    var payload = (UpdateLicencePositionPayload) licencePositionCorrectionCaptor.getValue().getPayload();

    assertThat(payload.changes()).isEmpty();
    verify(licencePositionCorrectionService)
        .stageRemovalOfExecutedChange(licenceCorrection, licencePosition, originalChangeId);
  }

  @Test
  void undoLicenseeChange_isLicenseeChange_dropsChange() {
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder().build();
    var changeId = "1";
    var changeToUndo = LicencePositionChangeType.addChange().build();

    when(licencePositionCorrectionService.getStagedChangeOrThrow(positionCorrection, changeId))
        .thenReturn(changeToUndo);
    when(licencePositionCorrectionService.resolveStagedChangeOperations(changeToUndo))
        .thenReturn(List.of(LicenceOperation.newLicenseeOperation().build()));

    licenseeChangeService.undoLicenseeChange(positionCorrection, changeId);

    verify(licencePositionCorrectionService).dropStagedChange(positionCorrection, changeId);
  }

  @Test
  void undoLicenseeChange_isNotLicenseeChange_throwsException() {
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder().build();
    var changeId = "1";
    var changeToUndo = LicencePositionChangeType.addChange().build();

    when(licencePositionCorrectionService.getStagedChangeOrThrow(positionCorrection, changeId))
        .thenReturn(changeToUndo);
    when(licencePositionCorrectionService.resolveStagedChangeOperations(changeToUndo))
        .thenReturn(List.of(LicenceOperation.newAdministratorChange().withOperator(2).build()));

    assertThatThrownBy(() -> licenseeChangeService.undoLicenseeChange(positionCorrection, changeId))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Change with id %s is not a licensee change".formatted(changeId));
  }

  @Test
  void getUndoLicenseeChangeContext_whenPositionHasBeenChanged_restoresPreviousVersion() {
    var correction = LicenceCorrectionTestUtil.newBuilder().withLicence(LICENCE).build();

    var correctionToUndo = LicencePositionCorrectionTestUtil.newBuilder()
        .withId(UUID.randomUUID())
        .build();
    var otherCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withId(UUID.randomUUID())
        .build();

    var correctedChangeContext = new LicenseeChangeContext(
        List.of(),
        List.of(1),
        List.of(1, 2),
        List.of(2),
        Map.of(1, "BP", 2, "FLAMSTONE")
    );

    var uncorrectedChangeContext = new LicenseeChangeContext(
        List.of(3),
        List.of(1),
        List.of(1, 2),
        List.of(2, 3),
        Map.of(1, "BP", 2, "FLAMSTONE", 3, "SHELL")
    );

    var targetPosition = mock(ChronologicalPosition.class);
    when(targetPosition.id()).thenReturn(POSITION_ID);
    var targetPositions = List.of(targetPosition);

    when(licencePositionViewService.getLicenseeChangeContext(correction, POSITION_ID)).thenReturn(correctedChangeContext);
    when(licencePositionCorrectionService.getPositionCorrections(correction))
        .thenReturn(List.of(correctionToUndo, otherCorrection));
    when(licencePositionViewService.getCorrectedChronologicalPositions(correction, List.of(otherCorrection), Set.of(POSITION_ID)))
        .thenReturn(targetPositions);
    when(licencePositionViewService.buildLicenseeChangeContext(targetPositions, POSITION_ID))
        .thenReturn(uncorrectedChangeContext);

    var result = licenseeChangeService.getUndoLicenseeChangeContext(
        correction, correctionToUndo, POSITION_ID);

    assertThat(result.currentWithdrawingLicenseeNames()).containsExactly("BP");
    assertThat(result.currentJoiningLicenseeNames()).isEmpty();
    assertThat(result.restoredWithdrawingLicenseeNames()).containsExactly("BP");
    assertThat(result.restoredJoiningLicenseeNames()).containsExactly("SHELL");
    assertThat(result.resultingLicenseeNames()).containsExactlyInAnyOrder("FLAMSTONE", "SHELL");
  }

  @Test
  void getUndoLicenseeChangeContext_whenPositionHasNotBeenChanged_returnsRemovalToExistingPosition() {
    var correction = LicenceCorrectionTestUtil.newBuilder().withLicence(LICENCE).build();

    var correctionToUndo = LicencePositionCorrectionTestUtil.newBuilder()
        .withId(UUID.randomUUID())
        .build();
    var otherCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withId(UUID.randomUUID())
        .build();

    var correctedChangeContext = new LicenseeChangeContext(
        List.of(3),
        List.of(1),
        List.of(1, 2),
        List.of(2, 3),
        Map.of(1, "BP", 2, "FLAMSTONE", 3, "SHELL")

    );

    when(licencePositionViewService.getLicenseeChangeContext(correction, POSITION_ID)).thenReturn(correctedChangeContext);
    when(licencePositionCorrectionService.getPositionCorrections(correction))
        .thenReturn(List.of(correctionToUndo, otherCorrection));
    when(licencePositionViewService.getCorrectedChronologicalPositions(correction, List.of(otherCorrection), Set.of(POSITION_ID)))
        .thenReturn(List.of());

    var result = licenseeChangeService.getUndoLicenseeChangeContext(
        correction, correctionToUndo, POSITION_ID);

    assertThat(result.currentWithdrawingLicenseeNames()).containsExactly("BP");
    assertThat(result.currentJoiningLicenseeNames()).containsExactly("SHELL");
    assertThat(result.restoredWithdrawingLicenseeNames()).isEmpty();
    assertThat(result.restoredJoiningLicenseeNames()).isEmpty();
    assertThat(result.resultingLicenseeNames()).containsExactlyInAnyOrder("BP", "FLAMSTONE");
  }

  @ParameterizedTest(name = "{argumentSetName}")
  @MethodSource("getRestoredLicenseeChangeView")
  void getRestoredLicenseeChangeView(
      List<String> joiningNames,
      List<String> withdrawingNames,
      LicenseeChangeView expectedView
  ) {
    assertThat(licenseeChangeService.getRestoredLicenseeChangeView(joiningNames, withdrawingNames))
        .usingRecursiveComparison()
        .isEqualTo(expectedView);
  }

  private static Stream<Arguments> getRestoredLicenseeChangeView() {
    return Stream.of(
        Arguments.argumentSet("No names given",
            List.of(),
            List.of(),
            null),
        Arguments.argumentSet("Only joining names",
            List.of(),
            List.of("joiningOrg1"),
            new LicenseeChangeView(
                List.of(),
                List.of("joiningOrg1"),
                "",
                ChangeViewUrls.none()
            )),
        Arguments.argumentSet("Only withdrawing names",
            List.of("withdrawingOrg1"),
            List.of(),
            new LicenseeChangeView(
                List.of("withdrawingOrg1"),
                List.of(),
                "",
                ChangeViewUrls.none()
            )),
        Arguments.argumentSet("Joining and withdrawing names",
            List.of("withdrawingOrg1"),
            List.of("joiningOrg1"),
            new LicenseeChangeView(
                List.of("withdrawingOrg1"),
                List.of("joiningOrg1"),
                "",
                ChangeViewUrls.none()
            ))
    );
  }

  private LicencePositionChangeType licenseeUpdateChange(
      String changeId,
      List<Integer> licenseesToAdd,
      List<Integer> licenseesToRemove
  ) {
    var licenseeOperation = LicenceOperation.newLicenseeOperation()
        .withLicenseesToAdd(licenseesToAdd)
        .withLicenseesToRemove(licenseesToRemove)
        .build();
    var operation = LicencePositionChangeOperation.newLicencePositionUpdateOperation()
        .withOperationId(licenseeOperation.id())
        .withOperation(licenseeOperation)
        .build();
    return LicencePositionChangeType.updateChangeOperations()
        .withChangeId(changeId)
        .withOperations(List.of(operation))
        .build();
  }
}