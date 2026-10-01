package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changeorder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.Month;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.OrderablePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.PositionMove;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.PositionMoveDirection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.SameTransactionPositionLookup;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changeoperation.LicencePositionChangeOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.AddChange;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.UpdateChangeOperations;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.UpdateChangeOrder;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.CreateLicencePositionPayloadTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.LicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionViewService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.PositionChange;

@ExtendWith(MockitoExtension.class)
class CorrectChangeOrderServiceTest {

  private static final Licence LICENCE = LicenceTestUtil.builder().build();
  private static final UUID POSITION_ID = UUID.randomUUID();
  private static final UUID CHANGE_A = UUID.randomUUID();
  private static final UUID CHANGE_B = UUID.randomUUID();
  private static final UUID CHANGE_C = UUID.randomUUID();

  private static final UUID TARGET_POSITION_ID = UUID.randomUUID();
  private static final LocalDate NEW_POSITION_DATE = LocalDate.of(2026, Month.JUNE, 15);
  private static final String SOURCE_REFERENCE = "REF-A";
  private static final UUID TRANSACTION_ID = UUID.randomUUID();
  private static final LicencePosition SOURCE_POSITION = LicencePositionTestUtil.newBuilder()
      .withId(POSITION_ID).withLicence(LICENCE).build();
  private static final LicencePosition TARGET_POSITION = LicencePositionTestUtil.newBuilder()
      .withId(TARGET_POSITION_ID).withLicence(LICENCE).build();

  private static final LicenceCorrection CORRECTION = LicenceCorrectionTestUtil.newBuilder()
      .withLicence(LICENCE).build();

  @Mock
  private LicencePositionViewService licencePositionViewService;

  @Mock
  private LicencePositionCorrectionService licencePositionCorrectionService;

  @Mock
  private LicencePositionService licencePositionService;

  @Mock
  private LicencePositionChangeService licencePositionChangeService;

  @InjectMocks
  private CorrectChangeOrderService correctChangeOrderService;

  @Test
  void getOrderableChanges_mapsLabelsToOrderableChangesPreservingOrder() {
    var labels = new LinkedHashMap<UUID, String>();
    labels.put(CHANGE_A, "Licence administrator change");
    labels.put(CHANGE_B, "Set equity change");
    when(licencePositionViewService.getOrderableChangeLabels(CORRECTION, POSITION_ID)).thenReturn(labels);

    var orderableChanges = correctChangeOrderService.getOrderableChanges(CORRECTION, POSITION_ID);

    assertThat(orderableChanges)
        .containsExactly(
            new OrderableChange(CHANGE_A, "Licence administrator change"),
            new OrderableChange(CHANGE_B, "Set equity change"));
  }

  @Test
  void correctChangeOrder_whenMovedChangeNotOnPosition_throws() {
    when(licencePositionViewService.getOrderableChangeLabels(CORRECTION, POSITION_ID))
        .thenReturn(orderableLabels(CHANGE_B));

    assertThatThrownBy(() -> correctChangeOrderService.correctChangeOrder(
        CORRECTION, POSITION_ID, CHANGE_A, CHANGE_B, PositionMoveDirection.BEFORE))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Cannot move change %s as it is not on licence position %s".formatted(CHANGE_A, POSITION_ID));
  }

  @Test
  void correctChangeOrder_whenTargetChangeNotOnPosition_throws() {
    when(licencePositionViewService.getOrderableChangeLabels(CORRECTION, POSITION_ID))
        .thenReturn(orderableLabels(CHANGE_A));

    assertThatThrownBy(() -> correctChangeOrderService.correctChangeOrder(
        CORRECTION, POSITION_ID, CHANGE_A, CHANGE_B, PositionMoveDirection.BEFORE))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(
            "Cannot move change %s relative to change %s as they are not on the same licence position %s"
                .formatted(CHANGE_A, CHANGE_B, POSITION_ID));
  }

  @Test
  void correctChangeOrder_onExecutedPosition_writesUpdateChangeOrdersOnlyForMovedChanges() {
    when(licencePositionViewService.getOrderableChangeLabels(CORRECTION, POSITION_ID))
        .thenReturn(orderableLabels(CHANGE_A, CHANGE_B, CHANGE_C));

    var position = LicencePositionTestUtil.newBuilder().withId(POSITION_ID).withLicence(LICENCE).build();
    when(licencePositionCorrectionService.findFirstAddedPositionCorrection(CORRECTION, POSITION_ID))
        .thenReturn(Optional.empty());
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(position);
    when(licencePositionChangeService.findByLicencePositionId(POSITION_ID)).thenReturn(List.of(
        liveChange(CHANGE_A, 1),
        liveChange(CHANGE_B, 2),
        liveChange(CHANGE_C, 3)));
    var positionCorrection = updatePositionCorrection(List.of());
    when(licencePositionCorrectionService.getOrBuildUpdatePositionCorrection(CORRECTION, position))
        .thenReturn(positionCorrection);

    correctChangeOrderService.correctChangeOrder(
        CORRECTION, POSITION_ID, CHANGE_B, CHANGE_C, PositionMoveDirection.AFTER);

    verify(licencePositionCorrectionService).saveOrDiscard(
        positionCorrection,
        List.of(new UpdateChangeOrder(CHANGE_C.toString(), 2), new UpdateChangeOrder(CHANGE_B.toString(), 3))
    );
  }

  @Test
  void correctChangeOrder_whenMovedBackToItsExecutedOrder_thenLeavesNoChangesToSave() {
    when(licencePositionViewService.getOrderableChangeLabels(CORRECTION, POSITION_ID))
        .thenReturn(orderableLabels(CHANGE_B, CHANGE_A));

    var position = LicencePositionTestUtil.newBuilder().withId(POSITION_ID).withLicence(LICENCE).build();
    when(licencePositionCorrectionService.findFirstAddedPositionCorrection(CORRECTION, POSITION_ID))
        .thenReturn(Optional.empty());
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(position);
    when(licencePositionChangeService.findByLicencePositionId(POSITION_ID)).thenReturn(List.of(
        liveChange(CHANGE_A, 1),
        liveChange(CHANGE_B, 2)));

    var positionCorrection = updatePositionCorrection(List.of(
        new UpdateChangeOrder(CHANGE_B.toString(), 1),
        new UpdateChangeOrder(CHANGE_A.toString(), 2)));
    when(licencePositionCorrectionService.getOrBuildUpdatePositionCorrection(CORRECTION, position))
        .thenReturn(positionCorrection);

    correctChangeOrderService.correctChangeOrder(
        CORRECTION, POSITION_ID, CHANGE_A, CHANGE_B, PositionMoveDirection.BEFORE);

    verify(licencePositionCorrectionService).saveOrDiscard(positionCorrection, List.of());
  }

  @Test
  void correctChangeOrder_onAddedPosition_rewritesAddChangeOrders() {
    when(licencePositionViewService.getOrderableChangeLabels(CORRECTION, POSITION_ID))
        .thenReturn(orderableLabels(CHANGE_A, CHANGE_B));

    var addedCorrection = addedPositionCorrection(List.of(
        new AddChange(CHANGE_A.toString(), 1, List.of()),
        new AddChange(CHANGE_B.toString(), 2, List.of())));
    when(licencePositionCorrectionService.findFirstAddedPositionCorrection(CORRECTION, POSITION_ID))
        .thenReturn(Optional.of(addedCorrection));

    correctChangeOrderService.correctChangeOrder(
        CORRECTION, POSITION_ID, CHANGE_A, CHANGE_B, PositionMoveDirection.AFTER);

    var captor = ArgumentCaptor.forClass(LicencePositionCorrection.class);
    verify(licencePositionCorrectionService).save(captor.capture());

    assertThat(captor.getValue().getPayload().changes())
        .containsExactly(
            new AddChange(CHANGE_A.toString(), 2, List.of()),
            new AddChange(CHANGE_B.toString(), 1, List.of()));
  }

  @Test
  void correctChangeOrder_whenTwoSubareaChangesOnDifferentBlocks_reordersThemIndependently() {
    when(licencePositionViewService.getOrderableChangeLabels(CORRECTION, POSITION_ID))
        .thenReturn(orderableLabels(CHANGE_A, CHANGE_B));

    var subareaChangeA = new AddChange(CHANGE_A.toString(), 1, List.of(subareaAddOperation(UUID.randomUUID())));
    var subareaChangeB = new AddChange(CHANGE_B.toString(), 2, List.of(subareaAddOperation(UUID.randomUUID())));
    var addedCorrection = addedPositionCorrection(List.of(subareaChangeA, subareaChangeB));
    when(licencePositionCorrectionService.findFirstAddedPositionCorrection(CORRECTION, POSITION_ID))
        .thenReturn(Optional.of(addedCorrection));

    correctChangeOrderService.correctChangeOrder(
        CORRECTION, POSITION_ID, CHANGE_A, CHANGE_B, PositionMoveDirection.AFTER);

    var captor = ArgumentCaptor.forClass(LicencePositionCorrection.class);
    verify(licencePositionCorrectionService).save(captor.capture());

    assertThat(captor.getValue().getPayload().changes())
        .containsExactly(
            new AddChange(CHANGE_A.toString(), 2, subareaChangeA.operations()),
            new AddChange(CHANGE_B.toString(), 1, subareaChangeB.operations()));
  }

  @Test
  void correctChangeOrder_onAddedPosition_rewritesTheStagedOrderOfAMovedLiveChange() {
    when(licencePositionViewService.getOrderableChangeLabels(CORRECTION, POSITION_ID))
        .thenReturn(orderableLabels(CHANGE_A, CHANGE_B));

    var movedLiveChange = updateChange(CHANGE_B, new SubareaOperation(UUID.randomUUID(), List.of(), List.of()));
    var addedCorrection = addedPositionCorrection(List.of(
        new AddChange(CHANGE_A.toString(), 1, List.of()),
        movedLiveChange,
        new UpdateChangeOrder(CHANGE_B.toString(), 2)
    ));
    when(licencePositionCorrectionService.findFirstAddedPositionCorrection(CORRECTION, POSITION_ID))
        .thenReturn(Optional.of(addedCorrection));

    correctChangeOrderService.correctChangeOrder(
        CORRECTION,
        POSITION_ID,
        CHANGE_B,
        CHANGE_A,
        PositionMoveDirection.BEFORE
    );

    verify(licencePositionCorrectionService).save(addedCorrection);
    assertThat(addedCorrection.getPayload().changes())
        .containsExactly(
            new AddChange(CHANGE_A.toString(), 2, List.of()),
            movedLiveChange,
            new UpdateChangeOrder(CHANGE_B.toString(), 1)
        );
  }

  @Test
  void moveChangeToPosition_whenTheTargetIsTheSourcePosition_thenThrows() {
    assertThatThrownBy(() -> correctChangeOrderService.moveChangeToPosition(
        CORRECTION,
        POSITION_ID,
        CHANGE_A,
        POSITION_ID
    ))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(
            "Cannot move change %s to licence position %s as it is already on it".formatted(CHANGE_A, POSITION_ID)
        );

    verifyNoInteractions(licencePositionCorrectionService);
  }

  @Test
  void moveChangeToPosition_whenTheChangeIsNotOnTheSourcePosition_thenThrows() {
    when(licencePositionViewService.getOrderableChangeLabels(CORRECTION, POSITION_ID))
        .thenReturn(orderableLabels(CHANGE_B));

    assertThatThrownBy(() -> correctChangeOrderService.moveChangeToPosition(
        CORRECTION,
        POSITION_ID,
        CHANGE_A,
        TARGET_POSITION_ID
    ))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Cannot move change %s as it is not on licence position %s".formatted(CHANGE_A, POSITION_ID));

    verifyNoInteractions(licencePositionCorrectionService);
  }

  @Test
  void moveChangeToPosition_whenTheChangeWasAddedInTheCorrection_thenMovesItAfterTheHighestTargetOrderKeepingItsId() {
    when(licencePositionViewService.getOrderableChangeLabels(CORRECTION, POSITION_ID))
        .thenReturn(orderableLabels(CHANGE_A));

    var movedChange = new AddChange(CHANGE_A.toString(), 1, List.of(subareaAddOperation(UUID.randomUUID())));
    var sourceCorrection = addedPositionCorrection(List.of(movedChange));
    when(licencePositionCorrectionService.getOrBuildPositionCorrection(CORRECTION, POSITION_ID))
        .thenReturn(sourceCorrection);
    when(licencePositionCorrectionService.findStagedChange(sourceCorrection, CHANGE_A.toString()))
        .thenReturn(Optional.of(movedChange));
    givenExecutedTargetChanges(
        TARGET_POSITION,
        List.of(
            new PositionChange(CHANGE_B.toString(), 1, null, List.of()),
            new PositionChange(CHANGE_C.toString(), 3, LicencePositionChangeType.REMOVE_CHANGE, List.of())
        )
    );

    correctChangeOrderService.moveChangeToPosition(CORRECTION, POSITION_ID, CHANGE_A, TARGET_POSITION_ID);

    verify(licencePositionCorrectionService).dropStagedChangeAndOrder(sourceCorrection, CHANGE_A.toString());
    verify(licencePositionCorrectionService).stageChangesOnPosition(
        CORRECTION,
        TARGET_POSITION_ID,
        List.of(new AddChange(CHANGE_A.toString(), 4, movedChange.operations()))
    );
  }

  @Test
  void moveChangeToPosition_whenALiveChangeHasBeenMovedOffTheTarget_thenDoesNotReuseItsLiveOrder() {
    when(licencePositionViewService.getOrderableChangeLabels(CORRECTION, POSITION_ID))
        .thenReturn(orderableLabels(CHANGE_A));

    var movedChange = new AddChange(CHANGE_A.toString(), 1, List.of(subareaAddOperation(UUID.randomUUID())));
    var sourceCorrection = addedPositionCorrection(List.of(movedChange));
    when(licencePositionCorrectionService.getOrBuildPositionCorrection(CORRECTION, POSITION_ID))
        .thenReturn(sourceCorrection);
    when(licencePositionCorrectionService.findStagedChange(sourceCorrection, CHANGE_A.toString()))
        .thenReturn(Optional.of(movedChange));
    givenExecutedTargetChanges(TARGET_POSITION, List.of(new PositionChange(CHANGE_B.toString(), 1, null, List.of())));
    when(licencePositionChangeService.findByLicencePositionId(TARGET_POSITION_ID))
        .thenReturn(List.of(liveChange(CHANGE_B, 1), liveChange(CHANGE_C, 2)));

    correctChangeOrderService.moveChangeToPosition(CORRECTION, POSITION_ID, CHANGE_A, TARGET_POSITION_ID);

    verify(licencePositionCorrectionService).stageChangesOnPosition(
        CORRECTION,
        TARGET_POSITION_ID,
        List.of(new AddChange(CHANGE_A.toString(), 3, movedChange.operations()))
    );
  }

  @Test
  void moveChangeToPosition_whenALiveChangeHasNoStagedEdits_thenStagesItsLiveOperationsAndANewOrderOnTheTarget() {
    when(licencePositionViewService.getOrderableChangeLabels(CORRECTION, POSITION_ID))
        .thenReturn(orderableLabels(CHANGE_A));

    var liveOperation = new SubareaOperation(UUID.randomUUID(), List.of(), List.of());
    var sourceCorrection = updatePositionCorrection(List.of());
    when(licencePositionCorrectionService.getOrBuildPositionCorrection(CORRECTION, POSITION_ID))
        .thenReturn(sourceCorrection);
    when(licencePositionCorrectionService.findStagedChange(sourceCorrection, CHANGE_A.toString()))
        .thenReturn(Optional.empty());
    when(licencePositionChangeService.getByIdOrThrow(CHANGE_A))
        .thenReturn(liveChangeOn(SOURCE_POSITION, CHANGE_A, 1, liveOperation));
    var targetCorrection = addedPositionCorrection(List.of());
    when(licencePositionCorrectionService.getOrBuildPositionCorrection(CORRECTION, TARGET_POSITION_ID))
        .thenReturn(targetCorrection);
    when(licencePositionCorrectionService.getChangesForAddedPosition(targetCorrection)).thenReturn(List.of());

    correctChangeOrderService.moveChangeToPosition(CORRECTION, POSITION_ID, CHANGE_A, TARGET_POSITION_ID);

    verify(licencePositionCorrectionService).stageChangesOnPosition(
        CORRECTION,
        TARGET_POSITION_ID,
        List.of(updateChange(CHANGE_A, liveOperation), new UpdateChangeOrder(CHANGE_A.toString(), 1))
    );
  }

  @Test
  void moveChangeToPosition_whenALiveChangeHasStagedEdits_thenCarriesTheEditsAndClearsTheSource() {
    when(licencePositionViewService.getOrderableChangeLabels(CORRECTION, POSITION_ID))
        .thenReturn(orderableLabels(CHANGE_A));

    var stagedEdit = updateChange(CHANGE_A, new SubareaOperation(UUID.randomUUID(), List.of(), List.of()));
    var sourceCorrection = updatePositionCorrection(List.of(stagedEdit, new UpdateChangeOrder(CHANGE_A.toString(), 3)));
    when(licencePositionCorrectionService.getOrBuildPositionCorrection(CORRECTION, POSITION_ID))
        .thenReturn(sourceCorrection);
    when(licencePositionCorrectionService.findStagedChange(sourceCorrection, CHANGE_A.toString()))
        .thenReturn(Optional.of(stagedEdit));
    when(licencePositionChangeService.getByIdOrThrow(CHANGE_A)).thenReturn(liveChangeOn(
        SOURCE_POSITION,
        CHANGE_A,
        1,
        new SubareaOperation(UUID.randomUUID(), List.of(), List.of())
    ));
    givenExecutedTargetChanges(TARGET_POSITION, List.of(new PositionChange(CHANGE_B.toString(), 2, null, List.of())));

    correctChangeOrderService.moveChangeToPosition(CORRECTION, POSITION_ID, CHANGE_A, TARGET_POSITION_ID);

    verify(licencePositionCorrectionService).dropStagedChangeAndOrder(sourceCorrection, CHANGE_A.toString());
    verify(licencePositionCorrectionService).stageChangesOnPosition(
        CORRECTION,
        TARGET_POSITION_ID,
        List.of(stagedEdit, new UpdateChangeOrder(CHANGE_A.toString(), 3))
    );
  }

  @Test
  void moveChangeToPosition_whenMovedBackToItsLivePositionWithEdits_thenRestoresTheEditsAfterTheHighestOrder() {
    when(licencePositionViewService.getOrderableChangeLabels(CORRECTION, POSITION_ID))
        .thenReturn(orderableLabels(CHANGE_A));

    var stagedEdit = updateChange(CHANGE_A, new SubareaOperation(UUID.randomUUID(), List.of(), List.of()));
    var sourceCorrection = addedPositionCorrection(List.of(stagedEdit, new UpdateChangeOrder(CHANGE_A.toString(), 2)));
    when(licencePositionCorrectionService.getOrBuildPositionCorrection(CORRECTION, POSITION_ID))
        .thenReturn(sourceCorrection);
    when(licencePositionCorrectionService.findStagedChange(sourceCorrection, CHANGE_A.toString()))
        .thenReturn(Optional.of(stagedEdit));
    when(licencePositionChangeService.getByIdOrThrow(CHANGE_A)).thenReturn(liveChangeOn(
        TARGET_POSITION,
        CHANGE_A,
        2,
        new SubareaOperation(UUID.randomUUID(), List.of(), List.of())
    ));
    givenExecutedTargetChanges(
        TARGET_POSITION,
        List.of(
            new PositionChange(CHANGE_B.toString(), 1, null, List.of()),
            new PositionChange(CHANGE_C.toString(), 2, null, List.of())
        )
    );

    correctChangeOrderService.moveChangeToPosition(CORRECTION, POSITION_ID, CHANGE_A, TARGET_POSITION_ID);

    verify(licencePositionCorrectionService).dropStagedChangeAndOrder(sourceCorrection, CHANGE_A.toString());
    verify(licencePositionCorrectionService).stageChangesOnPosition(
        CORRECTION,
        TARGET_POSITION_ID,
        List.of(stagedEdit, new UpdateChangeOrder(CHANGE_A.toString(), 3))
    );
  }

  @Test
  void moveChangeToPosition_whenMovedBackUneditedBehindChangesReorderedOntoItsLiveOrder_thenStagesOnlyANewOrder() {
    when(licencePositionViewService.getOrderableChangeLabels(CORRECTION, POSITION_ID))
        .thenReturn(orderableLabels(CHANGE_A));

    var liveOperation = new SubareaOperation(UUID.randomUUID(), List.of(), List.of());
    var stagedMove = updateChange(CHANGE_A, liveOperation);
    var sourceCorrection = addedPositionCorrection(List.of(stagedMove, new UpdateChangeOrder(CHANGE_A.toString(), 1)));
    when(licencePositionCorrectionService.getOrBuildPositionCorrection(CORRECTION, POSITION_ID))
        .thenReturn(sourceCorrection);
    when(licencePositionCorrectionService.findStagedChange(sourceCorrection, CHANGE_A.toString()))
        .thenReturn(Optional.of(stagedMove));
    when(licencePositionChangeService.getByIdOrThrow(CHANGE_A))
        .thenReturn(liveChangeOn(TARGET_POSITION, CHANGE_A, 2, liveOperation));
    givenExecutedTargetChanges(
        TARGET_POSITION,
        List.of(
            new PositionChange(CHANGE_C.toString(), 1, null, List.of()),
            new PositionChange(CHANGE_B.toString(), 2, null, List.of()),
            new PositionChange(CHANGE_A.toString(), 2, null, List.of())
        )
    );

    correctChangeOrderService.moveChangeToPosition(CORRECTION, POSITION_ID, CHANGE_A, TARGET_POSITION_ID);

    verify(licencePositionCorrectionService).stageChangesOnPosition(
        CORRECTION,
        TARGET_POSITION_ID,
        List.of(new UpdateChangeOrder(CHANGE_A.toString(), 3))
    );
  }

  @Test
  void moveChangeToPosition_whenMovedBackUneditedAndStillLastAtItsLiveOrder_thenOnlyClearsTheMove() {
    when(licencePositionViewService.getOrderableChangeLabels(CORRECTION, POSITION_ID))
        .thenReturn(orderableLabels(CHANGE_A));

    var liveOperation = new SubareaOperation(UUID.randomUUID(), List.of(), List.of());
    var stagedMove = updateChange(CHANGE_A, liveOperation);
    var sourceCorrection = addedPositionCorrection(List.of(stagedMove, new UpdateChangeOrder(CHANGE_A.toString(), 2)));
    when(licencePositionCorrectionService.getOrBuildPositionCorrection(CORRECTION, POSITION_ID))
        .thenReturn(sourceCorrection);
    when(licencePositionCorrectionService.findStagedChange(sourceCorrection, CHANGE_A.toString()))
        .thenReturn(Optional.of(stagedMove));
    when(licencePositionChangeService.getByIdOrThrow(CHANGE_A))
        .thenReturn(liveChangeOn(TARGET_POSITION, CHANGE_A, 3, liveOperation));
    givenExecutedTargetChanges(
        TARGET_POSITION,
        List.of(
            new PositionChange(CHANGE_B.toString(), 1, null, List.of()),
            new PositionChange(CHANGE_C.toString(), 2, null, List.of()),
            new PositionChange(CHANGE_A.toString(), 3, null, List.of())
        )
    );

    correctChangeOrderService.moveChangeToPosition(CORRECTION, POSITION_ID, CHANGE_A, TARGET_POSITION_ID);

    verify(licencePositionCorrectionService).dropStagedChangeAndOrder(sourceCorrection, CHANGE_A.toString());
    verify(licencePositionCorrectionService, never()).stageChangesOnPosition(any(), any(), any());
  }

  @Test
  void moveChangeToDate_whenAnotherPositionOnTheDateHasTheSameTransaction_thenMovesTheChangeOntoIt() {
    when(licencePositionCorrectionService.findSameTransactionPositionOnDate(CORRECTION, POSITION_ID, NEW_POSITION_DATE))
        .thenReturn(new SameTransactionPositionLookup(TRANSACTION_ID, TARGET_POSITION_ID));
    var movedChange = givenAnAddedChangeOnTheSource();
    givenEmptyAddedTarget();

    var result = correctChangeOrderService.moveChangeToDate(CORRECTION, POSITION_ID, CHANGE_A, NEW_POSITION_DATE, null);

    assertThat(result).isEqualTo(
        new MoveChangeToDateResult(MoveChangeToDateResult.Outcome.MOVED_TO_EXISTING_POSITION, TARGET_POSITION_ID));
    verify(licencePositionCorrectionService).stageChangesOnPosition(CORRECTION, TARGET_POSITION_ID, List.of(
        new AddChange(CHANGE_A.toString(), 1, movedChange.operations())));
    verify(licencePositionCorrectionService, never()).addNewPosition(any(), any(), any(), any());
  }

  @Test
  void moveChangeToDate_whenOnlyOtherTransactionsAreOnTheDateAndNoPlacementIsGiven_thenNeedsThePositionOrder() {
    givenNoSameTransactionPositionOnTheDate();
    when(licencePositionCorrectionService.getOrderablePositionsOnDate(CORRECTION, NEW_POSITION_DATE))
        .thenReturn(List.of(new OrderablePosition(UUID.randomUUID(), NEW_POSITION_DATE, 1, "REF-OTHER", false)));

    var result = correctChangeOrderService.moveChangeToDate(CORRECTION, POSITION_ID, CHANGE_A, NEW_POSITION_DATE, null);

    assertThat(result).isEqualTo(
        new MoveChangeToDateResult(MoveChangeToDateResult.Outcome.NEEDS_POSITION_ORDER, null));
    verify(licencePositionCorrectionService, never()).stageChangesOnPosition(any(), any(), any());
    verify(licencePositionCorrectionService, never()).addNewPosition(any(), any(), any(), any());
  }

  @Test
  void moveChangeToDate_whenTheDateIsEmptyAndTheChangeWasAddedInTheCorrection_thenMovesItOntoANewPosition() {
    givenNoSameTransactionPositionOnTheDate();
    when(licencePositionCorrectionService.getOrderablePositionsOnDate(CORRECTION, NEW_POSITION_DATE))
        .thenReturn(List.of());
    givenNewPositionAddedWithTheSourceReferenceAndTransaction();
    var movedChange = givenAnAddedChangeOnTheSource();
    givenEmptyAddedTarget();

    var result = correctChangeOrderService.moveChangeToDate(CORRECTION, POSITION_ID, CHANGE_A, NEW_POSITION_DATE, null);

    assertThat(result).isEqualTo(
        new MoveChangeToDateResult(MoveChangeToDateResult.Outcome.MOVED_TO_NEW_POSITION, TARGET_POSITION_ID));
    verify(licencePositionCorrectionService).stageChangesOnPosition(CORRECTION, TARGET_POSITION_ID, List.of(
        new AddChange(CHANGE_A.toString(), 1, movedChange.operations())));
    verify(licencePositionCorrectionService, never()).correctPositionOrder(any(), any(), any(), any());
  }

  @Test
  void moveChangeToDate_whenAPlacementIsGiven_thenPlacesTheNewPositionBeforeMovingTheChangeOntoIt() {
    var placementTargetId = UUID.randomUUID();
    givenNoSameTransactionPositionOnTheDate();
    givenNewPositionAddedWithTheSourceReferenceAndTransaction();
    var movedChange = givenAnAddedChangeOnTheSource();
    givenEmptyAddedTarget();

    var result = correctChangeOrderService.moveChangeToDate(CORRECTION, POSITION_ID, CHANGE_A, NEW_POSITION_DATE,
        new PositionMove(PositionMoveDirection.BEFORE, placementTargetId));

    assertThat(result).isEqualTo(
        new MoveChangeToDateResult(MoveChangeToDateResult.Outcome.MOVED_TO_NEW_POSITION, TARGET_POSITION_ID));

    var inOrder = inOrder(licencePositionCorrectionService);
    inOrder.verify(licencePositionCorrectionService)
        .correctPositionOrder(CORRECTION, TARGET_POSITION_ID, placementTargetId, PositionMoveDirection.BEFORE);
    inOrder.verify(licencePositionCorrectionService).stageChangesOnPosition(CORRECTION, TARGET_POSITION_ID, List.of(
        new AddChange(CHANGE_A.toString(), 1, movedChange.operations())));
  }

  @Test
  void moveChangeToDate_whenTheDateIsTheSourcePositionsDate_thenMovesItOntoANewPositionWithANewTransaction() {
    givenNoSameTransactionPositionOnTheDate();
    when(licencePositionCorrectionService.getOrderableDatePosition(CORRECTION, POSITION_ID))
        .thenReturn(new OrderablePosition(POSITION_ID, NEW_POSITION_DATE, 1, SOURCE_REFERENCE, false));
    when(licencePositionCorrectionService.addNewPosition(CORRECTION, NEW_POSITION_DATE, SOURCE_REFERENCE))
        .thenReturn(TARGET_POSITION_ID);
    var movedChange = givenAnAddedChangeOnTheSource();
    givenEmptyAddedTarget();

    var result = correctChangeOrderService.moveChangeToDate(CORRECTION, POSITION_ID, CHANGE_A, NEW_POSITION_DATE,
        new PositionMove(PositionMoveDirection.AFTER, POSITION_ID));

    assertThat(result).isEqualTo(
        new MoveChangeToDateResult(MoveChangeToDateResult.Outcome.MOVED_TO_NEW_POSITION, TARGET_POSITION_ID));
    verify(licencePositionCorrectionService, never()).addNewPosition(any(), any(), any(), any());
    verify(licencePositionCorrectionService)
        .correctPositionOrder(CORRECTION, TARGET_POSITION_ID, POSITION_ID, PositionMoveDirection.AFTER);
    verify(licencePositionCorrectionService).stageChangesOnPosition(CORRECTION, TARGET_POSITION_ID, List.of(
        new AddChange(CHANGE_A.toString(), 1, movedChange.operations())));
  }

  @Test
  void moveChangeToDate_whenTheDateIsEmptyAndALiveChangeHasNoStagedEdits_thenMovesItOntoANewPosition() {
    givenNoSameTransactionPositionOnTheDate();
    when(licencePositionCorrectionService.getOrderablePositionsOnDate(CORRECTION, NEW_POSITION_DATE))
        .thenReturn(List.of());
    when(licencePositionViewService.getOrderableChangeLabels(CORRECTION, POSITION_ID))
        .thenReturn(orderableLabels(CHANGE_A));
    givenNewPositionAddedWithTheSourceReferenceAndTransaction();

    var liveOperation = new SubareaOperation(UUID.randomUUID(), List.of(), List.of());
    var sourceCorrection = updatePositionCorrection(List.of());
    when(licencePositionCorrectionService.getOrBuildPositionCorrection(CORRECTION, POSITION_ID))
        .thenReturn(sourceCorrection);
    when(licencePositionCorrectionService.findStagedChange(sourceCorrection, CHANGE_A.toString()))
        .thenReturn(Optional.empty());
    when(licencePositionChangeService.getByIdOrThrow(CHANGE_A))
        .thenReturn(liveChangeOn(SOURCE_POSITION, CHANGE_A, 1, liveOperation));
    givenEmptyAddedTarget();

    var result = correctChangeOrderService.moveChangeToDate(CORRECTION, POSITION_ID, CHANGE_A, NEW_POSITION_DATE, null);

    assertThat(result).isEqualTo(
        new MoveChangeToDateResult(MoveChangeToDateResult.Outcome.MOVED_TO_NEW_POSITION, TARGET_POSITION_ID));
    verify(licencePositionCorrectionService).stageChangesOnPosition(CORRECTION, TARGET_POSITION_ID, List.of(
        updateChange(CHANGE_A, liveOperation),
        new UpdateChangeOrder(CHANGE_A.toString(), 1)));
  }

  private void givenNoSameTransactionPositionOnTheDate() {
    when(licencePositionCorrectionService.findSameTransactionPositionOnDate(CORRECTION, POSITION_ID, NEW_POSITION_DATE))
        .thenReturn(new SameTransactionPositionLookup(TRANSACTION_ID, null));
  }

  private void givenNewPositionAddedWithTheSourceReferenceAndTransaction() {
    when(licencePositionCorrectionService.getOrderableDatePosition(CORRECTION, POSITION_ID))
        .thenReturn(new OrderablePosition(POSITION_ID, NEW_POSITION_DATE.minusMonths(1), 1, SOURCE_REFERENCE, false));
    when(licencePositionCorrectionService.addNewPosition(CORRECTION, NEW_POSITION_DATE, SOURCE_REFERENCE, TRANSACTION_ID))
        .thenReturn(TARGET_POSITION_ID);
  }

  private AddChange givenAnAddedChangeOnTheSource() {
    when(licencePositionViewService.getOrderableChangeLabels(CORRECTION, POSITION_ID))
        .thenReturn(orderableLabels(CHANGE_A));

    var movedChange = new AddChange(CHANGE_A.toString(), 2, List.of(subareaAddOperation(UUID.randomUUID())));
    var sourceCorrection = addedPositionCorrection(List.of(movedChange));
    when(licencePositionCorrectionService.getOrBuildPositionCorrection(CORRECTION, POSITION_ID))
        .thenReturn(sourceCorrection);
    when(licencePositionCorrectionService.findStagedChange(sourceCorrection, CHANGE_A.toString()))
        .thenReturn(Optional.of(movedChange));
    return movedChange;
  }

  private void givenEmptyAddedTarget() {
    var targetCorrection = addedPositionCorrection(List.of());
    when(licencePositionCorrectionService.getOrBuildPositionCorrection(CORRECTION, TARGET_POSITION_ID))
        .thenReturn(targetCorrection);
    when(licencePositionCorrectionService.getChangesForAddedPosition(targetCorrection)).thenReturn(List.of());
  }

  private void givenExecutedTargetChanges(LicencePosition targetPosition, List<PositionChange> changes) {
    var targetCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withChangeType(LicencePositionCorrectionChangeType.UPDATE_POSITION)
        .withTargetLicencePosition(targetPosition)
        .withPayload(LicencePositionPayload.newUpdateLicencePositionPayload()
            .withCorrectionReference("CORRECTION-REF")
            .withChanges(List.of())
            .build())
        .build();
    when(licencePositionCorrectionService.getOrBuildPositionCorrection(CORRECTION, targetPosition.getId()))
        .thenReturn(targetCorrection);
    when(licencePositionCorrectionService.getChangesForExecutedPosition(CORRECTION, targetPosition, targetCorrection))
        .thenReturn(changes);
  }

  private static UpdateChangeOperations updateChange(UUID changeId, SubareaOperation operation) {
    return UpdateChangeOperations.buildUpdateChange(changeId.toString(), operation);
  }

  private static LicencePositionChange liveChangeOn(
      LicencePosition position,
      UUID id,
      int changeOrder,
      SubareaOperation operation
  ) {
    return LicencePositionChangeTestUtil.newBuilder()
        .withId(id)
        .withLicencePosition(position)
        .withChangeOrder(changeOrder)
        .withOperations(List.of(operation))
        .build();
  }

  private static LinkedHashMap<UUID, String> orderableLabels(UUID... changeIds) {
    var labels = new LinkedHashMap<UUID, String>();
    for (var changeId : changeIds) {
      labels.put(changeId, "Change " + changeId);
    }
    return labels;
  }

  private static LicencePositionChangeOperation subareaAddOperation(UUID featureId) {
    var operation = new SubareaOperation(featureId, List.of(), List.of());
    return LicencePositionChangeOperation.newLicencePositionAddOperation()
        .withOperationId(operation.id())
        .withOperation(operation)
        .build();
  }

  private static LicencePositionChange liveChange(UUID id, int order) {
    return LicencePositionChangeTestUtil.newBuilder().withId(id).withChangeOrder(order).build();
  }

  private static LicencePositionCorrection updatePositionCorrection(List<LicencePositionChangeType> changes) {
    return LicencePositionCorrectionTestUtil.newBuilder()
        .withChangeType(LicencePositionCorrectionChangeType.UPDATE_POSITION)
        .withPayload(LicencePositionPayload.newUpdateLicencePositionPayload()
            .withCorrectionReference("CORRECTION-REF")
            .withChanges(changes)
            .build())
        .build();
  }

  private static LicencePositionCorrection addedPositionCorrection(List<LicencePositionChangeType> changes) {
    return LicencePositionCorrectionTestUtil.newBuilder()
        .withChangeType(LicencePositionCorrectionChangeType.ADD_POSITION)
        .withPayload(CreateLicencePositionPayloadTestUtil.newBuilder()
            .withLicencePositionId(POSITION_ID.toString())
            .withChanges(changes)
            .build())
        .build();
  }
}
