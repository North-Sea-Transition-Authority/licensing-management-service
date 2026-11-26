package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changeorder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.gisframework.feature.FeatureService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionStagedPayloadService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.OrderablePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.PositionMove;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.PositionMoveDirection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.SameTransactionPositionLookup;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.PartialSurrenderOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.ChronologicalPositionService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionNameService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.PositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.PositionChangeTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.feature.FeatureTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.LicencePositionStagedPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.LicencePositionStagedPayloadChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.LicencePositionStagedPayloadTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.changeoperation.LicencePositionChangeOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.changetypes.AddChange;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.changetypes.LicencePositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.changetypes.UpdateChangeOperations;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.changetypes.UpdateChangeOrder;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.payloads.CreateLicencePositionPayloadTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.payloads.LicencePositionPayload;

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
  private ChronologicalPositionService chronologicalPositionService;

  @Mock
  private FeatureService featureService;

  @Mock
  private OrganisationUnitQueryService organisationUnitQueryService;

  @Mock
  private LicencePositionStagedPayloadService licencePositionStagedPayloadService;

  @Mock
  private LicencePositionService licencePositionService;

  @Mock
  private LicencePositionChangeService licencePositionChangeService;

  private CorrectChangeOrderService correctChangeOrderService;

  @BeforeEach
  void setUp() {
    correctChangeOrderService = new CorrectChangeOrderService(
        chronologicalPositionService,
        new LicencePositionNameService(organisationUnitQueryService, featureService),
        licencePositionStagedPayloadService,
        licencePositionService,
        licencePositionChangeService
    );
  }

  @Test
  void getOrderableChanges_mapsLabelsToOrderableChangesPreservingOrder() {
    var administratorOperation = LicenceOperation.newAdministratorChange().withOperator(1).build();
    var setEquityOperation = LicenceOperation.newSetEquityOperation().withTransferTo(2).withEquity(BigDecimal.TEN).build();
    givenCorrectedPosition(ChronologicalPositionTestUtil.newBuilder()
        .withId(POSITION_ID)
        .withChanges(List.of(
            PositionChangeTestUtil.newBuilder()
                .withChangeId(CHANGE_A.toString())
                .withOperations(List.of(administratorOperation))
                .build(),
            PositionChangeTestUtil.newBuilder()
                .withChangeId(CHANGE_B.toString())
                .withOperations(List.of(setEquityOperation))
                .build()))
        .build());

    var orderableChanges = correctChangeOrderService.getOrderableChanges(CORRECTION, POSITION_ID);

    assertThat(orderableChanges)
        .containsExactly(
            new OrderableChange(CHANGE_A, administratorOperation.displayName()),
            new OrderableChange(CHANGE_B, setEquityOperation.displayName()));
  }

  @Test
  void getOrderableChanges_whenTwoSubareaChangesOnDifferentBlocks_labelsEachByChangeIdWithFeatureName() {
    var firstBlock = FeatureTestUtil.blockFeature(UUID.randomUUID(), "30", 1);
    var secondBlock = FeatureTestUtil.blockFeature(UUID.randomUUID(), "30", 2);
    givenCorrectedPosition(ChronologicalPositionTestUtil.newBuilder()
        .withId(POSITION_ID)
        .withChanges(List.of(
            PositionChangeTestUtil.newBuilder()
                .withChangeId(CHANGE_A.toString())
                .withOperations(List.of(new SubareaOperation(firstBlock.getId(), List.of(), List.of())))
                .build(),
            PositionChangeTestUtil.newBuilder()
                .withChangeId(CHANGE_B.toString())
                .withChangeOrder(2)
                .withOperations(List.of(new SubareaOperation(secondBlock.getId(), List.of(), List.of())))
                .build()))
        .build());
    when(featureService.getFeaturesByIds(Set.of(firstBlock.getId(), secondBlock.getId())))
        .thenReturn(List.of(firstBlock, secondBlock));

    var orderableChanges = correctChangeOrderService.getOrderableChanges(CORRECTION, POSITION_ID);

    assertThat(orderableChanges).containsExactly(
        new OrderableChange(CHANGE_A, "Subarea change – %s".formatted(firstBlock.getFeatureName())),
        new OrderableChange(CHANGE_B, "Subarea change – %s".formatted(secondBlock.getFeatureName())));
  }

  @Test
  void getOrderableChanges_whenNonSubareaChange_labelsByDisplayNameWithoutFeature() {
    var administratorOperation = LicenceOperation.newAdministratorChange().withOperator(1).build();
    givenCorrectedPosition(ChronologicalPositionTestUtil.newBuilder()
        .withId(POSITION_ID)
        .withChanges(List.of(PositionChangeTestUtil.newBuilder()
            .withChangeId(CHANGE_A.toString())
            .withOperations(List.of(administratorOperation))
            .build()))
        .build());

    var orderableChanges = correctChangeOrderService.getOrderableChanges(CORRECTION, POSITION_ID);

    assertThat(orderableChanges).containsExactly(new OrderableChange(CHANGE_A, administratorOperation.displayName()));
    verifyNoInteractions(featureService);
  }

  @Test
  void correctChangeOrder_whenMovedChangeNotOnPosition_throws() {
    givenOrderableChangesOn(CHANGE_B);

    assertThatThrownBy(() -> correctChangeOrderService.correctChangeOrder(
        CORRECTION, POSITION_ID, CHANGE_A, CHANGE_B, PositionMoveDirection.BEFORE))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Cannot move change %s as it is not on licence position %s".formatted(CHANGE_A, POSITION_ID));
  }

  @Test
  void correctChangeOrder_whenTargetChangeNotOnPosition_throws() {
    givenOrderableChangesOn(CHANGE_A);

    assertThatThrownBy(() -> correctChangeOrderService.correctChangeOrder(
        CORRECTION, POSITION_ID, CHANGE_A, CHANGE_B, PositionMoveDirection.BEFORE))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(
            "Cannot move change %s relative to change %s as they are not on the same licence position %s"
                .formatted(CHANGE_A, CHANGE_B, POSITION_ID));
  }

  @Test
  void correctChangeOrder_onExecutedPosition_writesUpdateChangeOrdersOnlyForMovedChanges() {
    givenOrderableChangesOn(CHANGE_A, CHANGE_B, CHANGE_C);

    var position = LicencePositionTestUtil.newBuilder().withId(POSITION_ID).withLicence(LICENCE).build();
    when(licencePositionStagedPayloadService.findFirstAddedPositionStagedPayload(CORRECTION, POSITION_ID))
        .thenReturn(Optional.empty());
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(position);
    when(licencePositionChangeService.findByLicencePositionId(POSITION_ID)).thenReturn(List.of(
        liveChange(CHANGE_A, 1),
        liveChange(CHANGE_B, 2),
        liveChange(CHANGE_C, 3)));
    var positionStagedPayload = updatePositionStagedPayload(List.of());
    when(licencePositionStagedPayloadService.getOrBuildUpdatePositionStagedPayload(CORRECTION, position))
        .thenReturn(positionStagedPayload);

    correctChangeOrderService.correctChangeOrder(
        CORRECTION, POSITION_ID, CHANGE_B, CHANGE_C, PositionMoveDirection.AFTER);

    verify(licencePositionStagedPayloadService).saveOrDiscard(
        positionStagedPayload,
        List.of(new UpdateChangeOrder(CHANGE_C.toString(), 2), new UpdateChangeOrder(CHANGE_B.toString(), 3))
    );
  }

  @Test
  void correctChangeOrder_whenMovedBackToItsExecutedOrder_thenLeavesNoChangesToSave() {
    givenOrderableChangesOn(CHANGE_B, CHANGE_A);

    var position = LicencePositionTestUtil.newBuilder().withId(POSITION_ID).withLicence(LICENCE).build();
    when(licencePositionStagedPayloadService.findFirstAddedPositionStagedPayload(CORRECTION, POSITION_ID))
        .thenReturn(Optional.empty());
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(position);
    when(licencePositionChangeService.findByLicencePositionId(POSITION_ID)).thenReturn(List.of(
        liveChange(CHANGE_A, 1),
        liveChange(CHANGE_B, 2)));

    var positionStagedPayload = updatePositionStagedPayload(List.of(
        new UpdateChangeOrder(CHANGE_B.toString(), 1),
        new UpdateChangeOrder(CHANGE_A.toString(), 2)));
    when(licencePositionStagedPayloadService.getOrBuildUpdatePositionStagedPayload(CORRECTION, position))
        .thenReturn(positionStagedPayload);

    correctChangeOrderService.correctChangeOrder(
        CORRECTION, POSITION_ID, CHANGE_A, CHANGE_B, PositionMoveDirection.BEFORE);

    verify(licencePositionStagedPayloadService).saveOrDiscard(positionStagedPayload, List.of());
  }

  @Test
  void correctChangeOrder_onAddedPosition_rewritesAddChangeOrders() {
    givenOrderableChangesOn(CHANGE_A, CHANGE_B);

    var addedStagedPayload = addedPositionStagedPayload(List.of(
        new AddChange(CHANGE_A.toString(), 1, List.of()),
        new AddChange(CHANGE_B.toString(), 2, List.of())));
    when(licencePositionStagedPayloadService.findFirstAddedPositionStagedPayload(CORRECTION, POSITION_ID))
        .thenReturn(Optional.of(addedStagedPayload));

    correctChangeOrderService.correctChangeOrder(
        CORRECTION, POSITION_ID, CHANGE_A, CHANGE_B, PositionMoveDirection.AFTER);

    var captor = ArgumentCaptor.forClass(LicencePositionStagedPayload.class);
    verify(licencePositionStagedPayloadService).save(captor.capture());

    assertThat(captor.getValue().getPayload().changes())
        .containsExactly(
            new AddChange(CHANGE_A.toString(), 2, List.of()),
            new AddChange(CHANGE_B.toString(), 1, List.of()));
  }

  @Test
  void correctChangeOrder_whenTwoSubareaChangesOnDifferentBlocks_reordersThemIndependently() {
    givenOrderableChangesOn(CHANGE_A, CHANGE_B);

    var subareaChangeA = new AddChange(CHANGE_A.toString(), 1, List.of(subareaAddOperation(UUID.randomUUID())));
    var subareaChangeB = new AddChange(CHANGE_B.toString(), 2, List.of(subareaAddOperation(UUID.randomUUID())));
    var addedStagedPayload = addedPositionStagedPayload(List.of(subareaChangeA, subareaChangeB));
    when(licencePositionStagedPayloadService.findFirstAddedPositionStagedPayload(CORRECTION, POSITION_ID))
        .thenReturn(Optional.of(addedStagedPayload));

    correctChangeOrderService.correctChangeOrder(
        CORRECTION, POSITION_ID, CHANGE_A, CHANGE_B, PositionMoveDirection.AFTER);

    var captor = ArgumentCaptor.forClass(LicencePositionStagedPayload.class);
    verify(licencePositionStagedPayloadService).save(captor.capture());

    assertThat(captor.getValue().getPayload().changes())
        .containsExactly(
            new AddChange(CHANGE_A.toString(), 2, subareaChangeA.operations()),
            new AddChange(CHANGE_B.toString(), 1, subareaChangeB.operations()));
  }

  @Test
  void correctChangeOrder_onAddedPosition_rewritesTheStagedOrderOfAMovedLiveChange() {
    givenOrderableChangesOn(CHANGE_A, CHANGE_B);

    var movedLiveChange = updateChange(CHANGE_B, new SubareaOperation(UUID.randomUUID(), List.of(), List.of()));
    var addedStagedPayload = addedPositionStagedPayload(List.of(
        new AddChange(CHANGE_A.toString(), 1, List.of()),
        movedLiveChange,
        new UpdateChangeOrder(CHANGE_B.toString(), 2)
    ));
    when(licencePositionStagedPayloadService.findFirstAddedPositionStagedPayload(CORRECTION, POSITION_ID))
        .thenReturn(Optional.of(addedStagedPayload));

    correctChangeOrderService.correctChangeOrder(
        CORRECTION,
        POSITION_ID,
        CHANGE_B,
        CHANGE_A,
        PositionMoveDirection.BEFORE
    );

    verify(licencePositionStagedPayloadService).save(addedStagedPayload);
    assertThat(addedStagedPayload.getPayload().changes())
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

    verifyNoInteractions(licencePositionStagedPayloadService);
  }

  @Test
  void moveChangeToPosition_whenTheChangeIsNotOnTheSourcePosition_thenThrows() {
    givenOrderableChangesOn(CHANGE_B);

    assertThatThrownBy(() -> correctChangeOrderService.moveChangeToPosition(
        CORRECTION,
        POSITION_ID,
        CHANGE_A,
        TARGET_POSITION_ID
    ))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Cannot move change %s as it is not on licence position %s".formatted(CHANGE_A, POSITION_ID));

    verifyNoInteractions(licencePositionStagedPayloadService);
  }

  @Test
  void moveChangeToPosition_whenTheChangeWasAddedInTheCorrection_thenMovesItAfterTheHighestTargetOrderKeepingItsId() {
    givenOrderableChangesOn(CHANGE_A);

    var movedChange = new AddChange(CHANGE_A.toString(), 1, List.of(subareaAddOperation(UUID.randomUUID())));
    var sourceStagedPayload = addedPositionStagedPayload(List.of(movedChange));
    when(licencePositionStagedPayloadService.getOrBuildPositionStagedPayload(CORRECTION, POSITION_ID))
        .thenReturn(sourceStagedPayload);
    when(licencePositionStagedPayloadService.findStagedChange(sourceStagedPayload, CHANGE_A.toString()))
        .thenReturn(Optional.of(movedChange));
    givenExecutedTargetChanges(
        TARGET_POSITION,
        List.of(
            new PositionChange(CHANGE_B.toString(), 1, null, List.of()),
            new PositionChange(CHANGE_C.toString(), 3, LicencePositionChangeType.REMOVE_CHANGE, List.of())
        )
    );

    correctChangeOrderService.moveChangeToPosition(CORRECTION, POSITION_ID, CHANGE_A, TARGET_POSITION_ID);

    verify(licencePositionStagedPayloadService).dropStagedChangeAndOrder(sourceStagedPayload, CHANGE_A.toString());
    verify(licencePositionStagedPayloadService).stageChangesOnPosition(
        CORRECTION,
        TARGET_POSITION_ID,
        List.of(new AddChange(CHANGE_A.toString(), 4, movedChange.operations()))
    );
  }

  @Test
  void moveChangeToPosition_whenALiveChangeHasBeenMovedOffTheTarget_thenDoesNotReuseItsLiveOrder() {
    givenOrderableChangesOn(CHANGE_A);

    var movedChange = new AddChange(CHANGE_A.toString(), 1, List.of(subareaAddOperation(UUID.randomUUID())));
    var sourceStagedPayload = addedPositionStagedPayload(List.of(movedChange));
    when(licencePositionStagedPayloadService.getOrBuildPositionStagedPayload(CORRECTION, POSITION_ID))
        .thenReturn(sourceStagedPayload);
    when(licencePositionStagedPayloadService.findStagedChange(sourceStagedPayload, CHANGE_A.toString()))
        .thenReturn(Optional.of(movedChange));
    givenExecutedTargetChanges(TARGET_POSITION, List.of(new PositionChange(CHANGE_B.toString(), 1, null, List.of())));
    when(licencePositionChangeService.findByLicencePositionId(TARGET_POSITION_ID))
        .thenReturn(List.of(liveChange(CHANGE_B, 1), liveChange(CHANGE_C, 2)));

    correctChangeOrderService.moveChangeToPosition(CORRECTION, POSITION_ID, CHANGE_A, TARGET_POSITION_ID);

    verify(licencePositionStagedPayloadService).stageChangesOnPosition(
        CORRECTION,
        TARGET_POSITION_ID,
        List.of(new AddChange(CHANGE_A.toString(), 3, movedChange.operations()))
    );
  }

  @Test
  void moveChangeToPosition_whenALiveChangeHasNoStagedEdits_thenStagesItsLiveOperationsAndANewOrderOnTheTarget() {
    givenOrderableChangesOn(CHANGE_A);

    var liveOperation = new SubareaOperation(UUID.randomUUID(), List.of(), List.of());
    var sourceStagedPayload = updatePositionStagedPayload(List.of());
    when(licencePositionStagedPayloadService.getOrBuildPositionStagedPayload(CORRECTION, POSITION_ID))
        .thenReturn(sourceStagedPayload);
    when(licencePositionStagedPayloadService.findStagedChange(sourceStagedPayload, CHANGE_A.toString()))
        .thenReturn(Optional.empty());
    when(licencePositionChangeService.getByIdOrThrow(CHANGE_A))
        .thenReturn(liveChangeOn(SOURCE_POSITION, CHANGE_A, 1, liveOperation));
    var targetStagedPayload = addedPositionStagedPayload(List.of());
    when(licencePositionStagedPayloadService.getOrBuildPositionStagedPayload(CORRECTION, TARGET_POSITION_ID))
        .thenReturn(targetStagedPayload);
    when(licencePositionStagedPayloadService.getChangesForAddedPosition(targetStagedPayload)).thenReturn(List.of());

    correctChangeOrderService.moveChangeToPosition(CORRECTION, POSITION_ID, CHANGE_A, TARGET_POSITION_ID);

    verify(licencePositionStagedPayloadService).stageChangesOnPosition(
        CORRECTION,
        TARGET_POSITION_ID,
        List.of(updateChange(CHANGE_A, liveOperation), new UpdateChangeOrder(CHANGE_A.toString(), 1))
    );
  }

  @Test
  void moveChangeToPosition_whenALiveChangeHasStagedEdits_thenCarriesTheEditsAndClearsTheSource() {
    givenOrderableChangesOn(CHANGE_A);

    var stagedEdit = updateChange(CHANGE_A, new SubareaOperation(UUID.randomUUID(), List.of(), List.of()));
    var sourceStagedPayload = updatePositionStagedPayload(List.of(stagedEdit, new UpdateChangeOrder(CHANGE_A.toString(), 3)));
    when(licencePositionStagedPayloadService.getOrBuildPositionStagedPayload(CORRECTION, POSITION_ID))
        .thenReturn(sourceStagedPayload);
    when(licencePositionStagedPayloadService.findStagedChange(sourceStagedPayload, CHANGE_A.toString()))
        .thenReturn(Optional.of(stagedEdit));
    when(licencePositionChangeService.getByIdOrThrow(CHANGE_A)).thenReturn(liveChangeOn(
        SOURCE_POSITION,
        CHANGE_A,
        1,
        new SubareaOperation(UUID.randomUUID(), List.of(), List.of())
    ));
    givenExecutedTargetChanges(TARGET_POSITION, List.of(new PositionChange(CHANGE_B.toString(), 2, null, List.of())));

    correctChangeOrderService.moveChangeToPosition(CORRECTION, POSITION_ID, CHANGE_A, TARGET_POSITION_ID);

    verify(licencePositionStagedPayloadService).dropStagedChangeAndOrder(sourceStagedPayload, CHANGE_A.toString());
    verify(licencePositionStagedPayloadService).stageChangesOnPosition(
        CORRECTION,
        TARGET_POSITION_ID,
        List.of(stagedEdit, new UpdateChangeOrder(CHANGE_A.toString(), 3))
    );
  }

  @Test
  void moveChangeToPosition_whenMovedBackToItsLivePositionWithEdits_thenRestoresTheEditsAfterTheHighestOrder() {
    givenOrderableChangesOn(CHANGE_A);

    var stagedEdit = updateChange(CHANGE_A, new SubareaOperation(UUID.randomUUID(), List.of(), List.of()));
    var sourceStagedPayload = addedPositionStagedPayload(List.of(stagedEdit, new UpdateChangeOrder(CHANGE_A.toString(), 2)));
    when(licencePositionStagedPayloadService.getOrBuildPositionStagedPayload(CORRECTION, POSITION_ID))
        .thenReturn(sourceStagedPayload);
    when(licencePositionStagedPayloadService.findStagedChange(sourceStagedPayload, CHANGE_A.toString()))
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

    verify(licencePositionStagedPayloadService).dropStagedChangeAndOrder(sourceStagedPayload, CHANGE_A.toString());
    verify(licencePositionStagedPayloadService).stageChangesOnPosition(
        CORRECTION,
        TARGET_POSITION_ID,
        List.of(stagedEdit, new UpdateChangeOrder(CHANGE_A.toString(), 3))
    );
  }

  @Test
  void moveChangeToPosition_whenMovedBackUneditedBehindChangesReorderedOntoItsLiveOrder_thenStagesOnlyANewOrder() {
    givenOrderableChangesOn(CHANGE_A);

    var liveOperation = new SubareaOperation(UUID.randomUUID(), List.of(), List.of());
    var stagedMove = updateChange(CHANGE_A, liveOperation);
    var sourceStagedPayload = addedPositionStagedPayload(List.of(stagedMove, new UpdateChangeOrder(CHANGE_A.toString(), 1)));
    when(licencePositionStagedPayloadService.getOrBuildPositionStagedPayload(CORRECTION, POSITION_ID))
        .thenReturn(sourceStagedPayload);
    when(licencePositionStagedPayloadService.findStagedChange(sourceStagedPayload, CHANGE_A.toString()))
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

    verify(licencePositionStagedPayloadService).stageChangesOnPosition(
        CORRECTION,
        TARGET_POSITION_ID,
        List.of(new UpdateChangeOrder(CHANGE_A.toString(), 3))
    );
  }

  @Test
  void moveChangeToPosition_whenMovedBackUneditedAndStillLastAtItsLiveOrder_thenOnlyClearsTheMove() {
    givenOrderableChangesOn(CHANGE_A);

    var liveOperation = new SubareaOperation(UUID.randomUUID(), List.of(), List.of());
    var stagedMove = updateChange(CHANGE_A, liveOperation);
    var sourceStagedPayload = addedPositionStagedPayload(List.of(stagedMove, new UpdateChangeOrder(CHANGE_A.toString(), 2)));
    when(licencePositionStagedPayloadService.getOrBuildPositionStagedPayload(CORRECTION, POSITION_ID))
        .thenReturn(sourceStagedPayload);
    when(licencePositionStagedPayloadService.findStagedChange(sourceStagedPayload, CHANGE_A.toString()))
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

    verify(licencePositionStagedPayloadService).dropStagedChangeAndOrder(sourceStagedPayload, CHANGE_A.toString());
    verify(licencePositionStagedPayloadService, never()).stageChangesOnPosition(any(), any(), any());
  }

  @Test
  void moveChangeToDate_whenAnotherPositionOnTheDateHasTheSameTransaction_thenMovesTheChangeOntoIt() {
    when(licencePositionStagedPayloadService.findSameTransactionPositionOnDate(CORRECTION, POSITION_ID, NEW_POSITION_DATE))
        .thenReturn(new SameTransactionPositionLookup(TRANSACTION_ID, TARGET_POSITION_ID));
    var movedChange = givenAnAddedChangeOnTheSource();
    givenEmptyAddedTarget();

    var result = correctChangeOrderService.moveChangeToDate(CORRECTION, POSITION_ID, CHANGE_A, NEW_POSITION_DATE, null);

    assertThat(result).isEqualTo(
        new MoveChangeToDateResult(MoveChangeToDateResult.Outcome.MOVED_TO_EXISTING_POSITION, TARGET_POSITION_ID));
    verify(licencePositionStagedPayloadService).stageChangesOnPosition(CORRECTION, TARGET_POSITION_ID, List.of(
        new AddChange(CHANGE_A.toString(), 1, movedChange.operations())));
    verify(licencePositionStagedPayloadService, never()).addNewPosition(any(), any(), any(), any());
  }

  @Test
  void moveChangeToDate_whenOnlyOtherTransactionsAreOnTheDateAndNoPlacementIsGiven_thenNeedsThePositionOrder() {
    givenNoSameTransactionPositionOnTheDate();
    when(licencePositionStagedPayloadService.getOrderablePositionsOnDate(CORRECTION, NEW_POSITION_DATE))
        .thenReturn(List.of(new OrderablePosition(UUID.randomUUID(), NEW_POSITION_DATE, 1, "REF-OTHER", false)));

    var result = correctChangeOrderService.moveChangeToDate(CORRECTION, POSITION_ID, CHANGE_A, NEW_POSITION_DATE, null);

    assertThat(result).isEqualTo(
        new MoveChangeToDateResult(MoveChangeToDateResult.Outcome.NEEDS_POSITION_ORDER, null));
    verify(licencePositionStagedPayloadService, never()).stageChangesOnPosition(any(), any(), any());
    verify(licencePositionStagedPayloadService, never()).addNewPosition(any(), any(), any(), any());
  }

  @Test
  void moveChangeToDate_whenTheDateIsEmptyAndTheChangeWasAddedInTheCorrection_thenMovesItOntoANewPosition() {
    givenNoSameTransactionPositionOnTheDate();
    when(licencePositionStagedPayloadService.getOrderablePositionsOnDate(CORRECTION, NEW_POSITION_DATE))
        .thenReturn(List.of());
    givenNewPositionAddedWithTheSourceReferenceAndTransaction();
    var movedChange = givenAnAddedChangeOnTheSource();
    givenEmptyAddedTarget();

    var result = correctChangeOrderService.moveChangeToDate(CORRECTION, POSITION_ID, CHANGE_A, NEW_POSITION_DATE, null);

    assertThat(result).isEqualTo(
        new MoveChangeToDateResult(MoveChangeToDateResult.Outcome.MOVED_TO_NEW_POSITION, TARGET_POSITION_ID));
    verify(licencePositionStagedPayloadService).stageChangesOnPosition(CORRECTION, TARGET_POSITION_ID, List.of(
        new AddChange(CHANGE_A.toString(), 1, movedChange.operations())));
    verify(licencePositionStagedPayloadService, never()).correctPositionOrder(any(), any(), any(), any());
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

    var inOrder = inOrder(licencePositionStagedPayloadService);
    inOrder.verify(licencePositionStagedPayloadService)
        .correctPositionOrder(CORRECTION, TARGET_POSITION_ID, placementTargetId, PositionMoveDirection.BEFORE);
    inOrder.verify(licencePositionStagedPayloadService).stageChangesOnPosition(CORRECTION, TARGET_POSITION_ID, List.of(
        new AddChange(CHANGE_A.toString(), 1, movedChange.operations())));
  }

  @Test
  void moveChangeToDate_whenTheDateIsTheSourcePositionsDate_thenMovesItOntoANewPositionWithANewTransaction() {
    givenNoSameTransactionPositionOnTheDate();
    when(licencePositionStagedPayloadService.getOrderableDatePosition(CORRECTION, POSITION_ID))
        .thenReturn(new OrderablePosition(POSITION_ID, NEW_POSITION_DATE, 1, SOURCE_REFERENCE, false));
    when(licencePositionStagedPayloadService.addNewPosition(CORRECTION, NEW_POSITION_DATE, SOURCE_REFERENCE))
        .thenReturn(TARGET_POSITION_ID);
    var movedChange = givenAnAddedChangeOnTheSource();
    givenEmptyAddedTarget();

    var result = correctChangeOrderService.moveChangeToDate(CORRECTION, POSITION_ID, CHANGE_A, NEW_POSITION_DATE,
        new PositionMove(PositionMoveDirection.AFTER, POSITION_ID));

    assertThat(result).isEqualTo(
        new MoveChangeToDateResult(MoveChangeToDateResult.Outcome.MOVED_TO_NEW_POSITION, TARGET_POSITION_ID));
    verify(licencePositionStagedPayloadService, never()).addNewPosition(any(), any(), any(), any());
    verify(licencePositionStagedPayloadService)
        .correctPositionOrder(CORRECTION, TARGET_POSITION_ID, POSITION_ID, PositionMoveDirection.AFTER);
    verify(licencePositionStagedPayloadService).stageChangesOnPosition(CORRECTION, TARGET_POSITION_ID, List.of(
        new AddChange(CHANGE_A.toString(), 1, movedChange.operations())));
  }

  @Test
  void moveChangeToDate_whenTheDateIsEmptyAndALiveChangeHasNoStagedEdits_thenMovesItOntoANewPosition() {
    givenNoSameTransactionPositionOnTheDate();
    when(licencePositionStagedPayloadService.getOrderablePositionsOnDate(CORRECTION, NEW_POSITION_DATE))
        .thenReturn(List.of());
    givenOrderableChangesOn(CHANGE_A);
    givenNewPositionAddedWithTheSourceReferenceAndTransaction();

    var liveOperation = new SubareaOperation(UUID.randomUUID(), List.of(), List.of());
    var sourceStagedPayload = updatePositionStagedPayload(List.of());
    when(licencePositionStagedPayloadService.getOrBuildPositionStagedPayload(CORRECTION, POSITION_ID))
        .thenReturn(sourceStagedPayload);
    when(licencePositionStagedPayloadService.findStagedChange(sourceStagedPayload, CHANGE_A.toString()))
        .thenReturn(Optional.empty());
    when(licencePositionChangeService.getByIdOrThrow(CHANGE_A))
        .thenReturn(liveChangeOn(SOURCE_POSITION, CHANGE_A, 1, liveOperation));
    givenEmptyAddedTarget();

    var result = correctChangeOrderService.moveChangeToDate(CORRECTION, POSITION_ID, CHANGE_A, NEW_POSITION_DATE, null);

    assertThat(result).isEqualTo(
        new MoveChangeToDateResult(MoveChangeToDateResult.Outcome.MOVED_TO_NEW_POSITION, TARGET_POSITION_ID));
    verify(licencePositionStagedPayloadService).stageChangesOnPosition(CORRECTION, TARGET_POSITION_ID, List.of(
        updateChange(CHANGE_A, liveOperation),
        new UpdateChangeOrder(CHANGE_A.toString(), 1)));
  }

  private void givenNoSameTransactionPositionOnTheDate() {
    when(licencePositionStagedPayloadService.findSameTransactionPositionOnDate(CORRECTION, POSITION_ID, NEW_POSITION_DATE))
        .thenReturn(new SameTransactionPositionLookup(TRANSACTION_ID, null));
  }

  private void givenNewPositionAddedWithTheSourceReferenceAndTransaction() {
    when(licencePositionStagedPayloadService.getOrderableDatePosition(CORRECTION, POSITION_ID))
        .thenReturn(new OrderablePosition(POSITION_ID, NEW_POSITION_DATE.minusMonths(1), 1, SOURCE_REFERENCE, false));
    when(licencePositionStagedPayloadService.addNewPosition(CORRECTION, NEW_POSITION_DATE, SOURCE_REFERENCE, TRANSACTION_ID))
        .thenReturn(TARGET_POSITION_ID);
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {LicencePositionChangeType.ADD_CHANGE, LicencePositionChangeType.REMOVE_CHANGE})
  void canMoveChangeToPosition_whenMovingAPartialSurrenderOntoAPositionWithOne_thenFalse(String targetChangeType) {
    givenAddedSourceChanges(List.of(new PositionChange(CHANGE_A.toString(), 1, null, List.of(partialSurrender()))));
    givenExecutedTargetChanges(
        TARGET_POSITION,
        List.of(new PositionChange(CHANGE_B.toString(), 1, targetChangeType, List.of(partialSurrender())))
    );

    var result = correctChangeOrderService.canMoveChangeToPosition(CORRECTION, POSITION_ID, CHANGE_A, TARGET_POSITION_ID);

    assertThat(result).isFalse();
  }

  @Test
  void canMoveChangeToPosition_whenMovingAPartialSurrenderOntoAPositionWithoutOne_thenTrue() {
    givenAddedSourceChanges(List.of(new PositionChange(CHANGE_A.toString(), 1, null, List.of(partialSurrender()))));
    givenExecutedTargetChanges(
        TARGET_POSITION,
        List.of(new PositionChange(CHANGE_B.toString(), 1, null, List.of(new SubareaOperation(UUID.randomUUID(), List.of(), List.of()))))
    );

    var result = correctChangeOrderService.canMoveChangeToPosition(CORRECTION, POSITION_ID, CHANGE_A, TARGET_POSITION_ID);

    assertThat(result).isTrue();
  }

  @Test
  void canMoveChangeToPosition_whenMovingAPartialSurrenderBackOntoItsLivePosition_thenTrue() {
    var surrender = partialSurrender();
    givenAddedSourceChanges(List.of(new PositionChange(CHANGE_A.toString(), 1, null, List.of(surrender))));
    givenExecutedTargetChanges(TARGET_POSITION, List.of(new PositionChange(CHANGE_A.toString(), 1, null, List.of(surrender))));

    var result = correctChangeOrderService.canMoveChangeToPosition(CORRECTION, POSITION_ID, CHANGE_A, TARGET_POSITION_ID);

    assertThat(result).isTrue();
  }

  @Test
  void canMoveChangeToPosition_whenMovingAChangeThatIsNotAPartialSurrender_thenTrue() {
    givenAddedSourceChanges(List.of(
        new PositionChange(CHANGE_A.toString(), 1, null, List.of(new SubareaOperation(UUID.randomUUID(), List.of(), List.of()))),
        new PositionChange(CHANGE_B.toString(), 2, null, List.of(partialSurrender()))
    ));

    var result = correctChangeOrderService.canMoveChangeToPosition(CORRECTION, POSITION_ID, CHANGE_A, TARGET_POSITION_ID);

    assertThat(result).isTrue();
    verify(licencePositionStagedPayloadService, never()).getOrBuildPositionStagedPayload(CORRECTION, TARGET_POSITION_ID);
  }

  private void givenAddedSourceChanges(List<PositionChange> changes) {
    var sourceStagedPayload = addedPositionStagedPayload(List.of());
    when(licencePositionStagedPayloadService.getOrBuildPositionStagedPayload(CORRECTION, POSITION_ID))
        .thenReturn(sourceStagedPayload);
    when(licencePositionStagedPayloadService.getChangesForAddedPosition(sourceStagedPayload)).thenReturn(changes);
  }

  private static PartialSurrenderOperation partialSurrender() {
    return LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(UUID.randomUUID()))
        .build();
  }

  private AddChange givenAnAddedChangeOnTheSource() {
    givenOrderableChangesOn(CHANGE_A);

    var movedChange = new AddChange(CHANGE_A.toString(), 2, List.of(subareaAddOperation(UUID.randomUUID())));
    var sourceStagedPayload = addedPositionStagedPayload(List.of(movedChange));
    when(licencePositionStagedPayloadService.getOrBuildPositionStagedPayload(CORRECTION, POSITION_ID))
        .thenReturn(sourceStagedPayload);
    when(licencePositionStagedPayloadService.findStagedChange(sourceStagedPayload, CHANGE_A.toString()))
        .thenReturn(Optional.of(movedChange));
    return movedChange;
  }

  private void givenEmptyAddedTarget() {
    var targetStagedPayload = addedPositionStagedPayload(List.of());
    when(licencePositionStagedPayloadService.getOrBuildPositionStagedPayload(CORRECTION, TARGET_POSITION_ID))
        .thenReturn(targetStagedPayload);
    when(licencePositionStagedPayloadService.getChangesForAddedPosition(targetStagedPayload)).thenReturn(List.of());
  }

  private void givenExecutedTargetChanges(LicencePosition targetPosition, List<PositionChange> changes) {
    var targetStagedPayload = LicencePositionStagedPayloadTestUtil.newBuilder()
        .withChangeType(LicencePositionStagedPayloadChangeType.UPDATE_POSITION)
        .withTargetLicencePosition(targetPosition)
        .withPayload(LicencePositionPayload.newUpdateLicencePositionPayload()
            .withCorrectionReference("CORRECTION-REF")
            .withChanges(List.of())
            .build())
        .build();
    when(licencePositionStagedPayloadService.getOrBuildPositionStagedPayload(CORRECTION, targetPosition.getId()))
        .thenReturn(targetStagedPayload);
    when(licencePositionStagedPayloadService.getChangesForExecutedPosition(CORRECTION, targetPosition, targetStagedPayload))
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

  private void givenOrderableChangesOn(UUID... changeIds) {
    var changes = Arrays.stream(changeIds)
        .map(changeId -> PositionChangeTestUtil.newBuilder().withChangeId(changeId.toString()).build())
        .toList();

    givenCorrectedPosition(ChronologicalPositionTestUtil.newBuilder().withId(POSITION_ID).withChanges(changes).build());
  }

  private void givenCorrectedPosition(ChronologicalPosition chronologicalPosition) {
    when(chronologicalPositionService.getCorrectedChronologicalPositions(CORRECTION, POSITION_ID))
        .thenReturn(List.of(chronologicalPosition));
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

  private static LicencePositionStagedPayload updatePositionStagedPayload(List<LicencePositionChangeType> changes) {
    return LicencePositionStagedPayloadTestUtil.newBuilder()
        .withChangeType(LicencePositionStagedPayloadChangeType.UPDATE_POSITION)
        .withPayload(LicencePositionPayload.newUpdateLicencePositionPayload()
            .withCorrectionReference("CORRECTION-REF")
            .withChanges(changes)
            .build())
        .build();
  }

  private static LicencePositionStagedPayload addedPositionStagedPayload(List<LicencePositionChangeType> changes) {
    return LicencePositionStagedPayloadTestUtil.newBuilder()
        .withChangeType(LicencePositionStagedPayloadChangeType.ADD_POSITION)
        .withPayload(CreateLicencePositionPayloadTestUtil.newBuilder()
            .withLicencePositionId(POSITION_ID.toString())
            .withChanges(changes)
            .build())
        .build();
  }
}
