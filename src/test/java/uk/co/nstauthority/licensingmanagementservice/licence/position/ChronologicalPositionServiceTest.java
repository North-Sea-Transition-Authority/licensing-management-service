package uk.co.nstauthority.licensingmanagementservice.licence.position;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.InstanceOfAssertFactories.list;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.UpdateChangeOperations;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.CreateLicencePositionPayloadTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.UpdateLicencePositionPayloadTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.AdministratorOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.PositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.transaction.LicenceTransactionTestUtil;

@ExtendWith(MockitoExtension.class)
class ChronologicalPositionServiceTest {

  private static final Licence LICENCE = LicenceTestUtil.builder().withLicenceType(LicenceType.SEAWARD_PRODUCTION).build();
  private static final UUID POSITION_ID = UUID.randomUUID();
  private static final AdministratorOperation ADMINISTRATOR_OPERATION = LicenceOperation.newAdministratorChange()
      .withOperator(1)
      .build();

  @Mock
  private LicencePositionService licencePositionService;

  @Mock
  private LicencePositionChangeService licencePositionChangeService;

  @Mock
  private LicencePositionCorrectionService licencePositionCorrectionService;

  @InjectMocks
  private ChronologicalPositionService chronologicalPositionService;

  @Test
  void getLiveChronologicalPositions_holdsEachChangeAgainstThePositionItWasExecutedOn() {
    var firstPosition = executedPosition(LocalDate.of(2026, Month.JULY, 26));
    var secondPosition = executedPosition(LocalDate.of(2026, Month.AUGUST, 1));
    var changeId = UUID.randomUUID();
    var change = liveAdminChange(changeId, firstPosition);

    when(licencePositionService.getExecutedChronologicalLicencePositions(LICENCE))
        .thenReturn(List.of(firstPosition, secondPosition));
    when(licencePositionChangeService.findByLicencePositionIn(List.of(firstPosition, secondPosition)))
        .thenReturn(List.of(change));

    var result = chronologicalPositionService.getLiveChronologicalPositions(LICENCE);

    assertThat(result).isEqualTo(List.of(
        ChronologicalPosition.fromLicencePosition(
            firstPosition,
            firstPosition.getLicenceTransaction().getRegulatorReference(),
            firstPosition.getPositionDate(),
            firstPosition.getPositionDateOrder(),
            List.of(new PositionChange(changeId.toString(), 1, null, List.of(ADMINISTRATOR_OPERATION)))),
        ChronologicalPosition.fromLicencePosition(
            secondPosition,
            secondPosition.getLicenceTransaction().getRegulatorReference(),
            secondPosition.getPositionDate(),
            secondPosition.getPositionDateOrder(),
            List.of())));
  }

  @Test
  void getInvolvedLegalEntitiesByLicenceId_whenNoLicences_thenEmpty() {
    var result = chronologicalPositionService.getInvolvedLegalEntityIdsByLicenceId(List.of());

    assertThat(result).isEmpty();
    verifyNoInteractions(licencePositionService, licencePositionChangeService);
  }

  @Test
  void getInvolvedLegalEntitiesByLicenceId_whenLicencesHaveMultiplePositions_thenStateFoldedFromFullHistoryPerLicence() {
    var firstLicence = LicenceTestUtil.builder().withId(1).build();
    var secondLicence = LicenceTestUtil.builder().withId(2).build();
    var firstLicenceEarlierPosition = executedPositionOn(firstLicence, LocalDate.of(2026, Month.JANUARY, 1));
    var secondLicencePosition = executedPositionOn(secondLicence, LocalDate.of(2026, Month.FEBRUARY, 1));
    var firstLicenceLaterPosition = executedPositionOn(firstLicence, LocalDate.of(2026, Month.MARCH, 1));
    var executedPositions = List.of(firstLicenceEarlierPosition, secondLicencePosition, firstLicenceLaterPosition);

    when(licencePositionService.getExecutedChronologicalLicencePositions(List.of(firstLicence, secondLicence)))
        .thenReturn(executedPositions);
    when(licencePositionChangeService.findByLicencePositionIn(executedPositions)).thenReturn(List.of(
        changeOn(firstLicenceEarlierPosition, 1, LicenceOperation.newAdministratorChange().withOperator(1).build()),
        changeOn(secondLicencePosition, 1, LicenceOperation.newAdministratorChange().withOperator(3).build()),
        changeOn(firstLicenceLaterPosition, 1, LicenceOperation.newLicenseeOperation().withLicenseesToAdd(List.of(2)).build())
    ));

    var result = chronologicalPositionService.getInvolvedLegalEntityIdsByLicenceId(List.of(firstLicence, secondLicence));

    assertThat(result).isEqualTo(Map.of(
        firstLicence.getId(), Set.of(1, 2),
        secondLicence.getId(), Set.of(3)
    ));
  }

  @Test
  void getInvolvedLegalEntitiesByLicenceId_whenLicenceHasNoExecutedPositions_thenLicenceNotIncluded() {
    var licence = LicenceTestUtil.builder().withId(1).build();

    when(licencePositionService.getExecutedChronologicalLicencePositions(List.of(licence))).thenReturn(List.of());

    var result = chronologicalPositionService.getInvolvedLegalEntityIdsByLicenceId(List.of(licence));

    assertThat(result).isEmpty();
  }

  @Test
  void getCorrectedChronologicalPositions_whenReferenceCorrected_usesTheCorrectedReference() {
    var correction = LicenceCorrectionTestUtil.newBuilder().withId(UUID.randomUUID()).withLicence(LICENCE).build();

    var executed = LicencePositionTestUtil.newBuilder()
        .withId(POSITION_ID).withLicence(LICENCE).withStatus(LicencePositionStatus.EXECUTED)
        .withLicenceTransaction(LicenceTransactionTestUtil.newBuilder().withRegulatorReference("EXEC-1").build())
        .withPositionDate(LocalDate.of(2026, Month.JANUARY, 1)).withPositionOrder(1).build();

    var payload = UpdateLicencePositionPayloadTestUtil.newBuilder().withCorrectionReference("COR-1").build();
    var referenceCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withLicenceCorrection(correction)
        .withChangeType(LicencePositionCorrectionChangeType.UPDATE_POSITION)
        .withTargetLicencePosition(executed)
        .withPayload(payload)
        .build();

    when(licencePositionService.getExecutedChronologicalLicencePositions(LICENCE)).thenReturn(List.of(executed));
    when(licencePositionChangeService.findByLicencePositionIn(List.of(executed))).thenReturn(List.of());
    when(licencePositionCorrectionService.getPositionCorrections(correction))
        .thenReturn(List.of(referenceCorrection));

    var result = chronologicalPositionService.getCorrectedChronologicalPositions(correction, POSITION_ID);

    assertThat(result).containsExactly(ChronologicalPosition.fromLicencePosition(
        executed,
        "COR-1",
        payload.effectiveDate(),
        payload.effectiveDateOrder(),
        List.of()));
  }

  @Test
  void getCorrectedChronologicalPositions_whenReferenceNotCorrected_usesTheExecutedReference() {
    var correction = LicenceCorrectionTestUtil.newBuilder().withId(UUID.randomUUID()).withLicence(LICENCE).build();

    var executed = LicencePositionTestUtil.newBuilder()
        .withId(POSITION_ID).withLicence(LICENCE).withStatus(LicencePositionStatus.EXECUTED)
        .withLicenceTransaction(LicenceTransactionTestUtil.newBuilder().withRegulatorReference("EXEC-1").build())
        .withPositionDate(LocalDate.of(2026, Month.JANUARY, 1)).withPositionOrder(1).build();

    when(licencePositionService.getExecutedChronologicalLicencePositions(LICENCE)).thenReturn(List.of(executed));
    when(licencePositionChangeService.findByLicencePositionIn(List.of(executed))).thenReturn(List.of());
    when(licencePositionCorrectionService.getPositionCorrections(correction)).thenReturn(List.of());

    var result = chronologicalPositionService.getCorrectedChronologicalPositions(correction, POSITION_ID);

    assertThat(result).containsExactly(ChronologicalPosition.fromLicencePosition(
        executed,
        "EXEC-1",
        executed.getPositionDate(),
        executed.getPositionDateOrder(),
        List.of()));
  }

  @Test
  void getCorrectedChronologicalPositions_whenChangeOrderCorrected_appliesNewOrderKeepingTypeAndOperations() {
    var correction = LicenceCorrectionTestUtil.newBuilder().withId(UUID.randomUUID()).withLicence(LICENCE).build();
    var adminChangeId = UUID.randomUUID();
    var setEquityChangeId = UUID.randomUUID();

    var executed = LicencePositionTestUtil.newBuilder()
        .withId(POSITION_ID).withLicence(LICENCE).withStatus(LicencePositionStatus.EXECUTED)
        .withPositionDate(LocalDate.of(2026, Month.JANUARY, 1)).withPositionOrder(1).build();

    var liveAdminChange = LicencePositionChangeTestUtil.newBuilder()
        .withId(adminChangeId).withLicencePosition(executed).withChangeOrder(1)
        .withOperations(List.of(LicenceOperation.newAdministratorChange().withOperator(5).build()))
        .build();
    var liveSetEquityChange = LicencePositionChangeTestUtil.newBuilder()
        .withId(setEquityChangeId).withLicencePosition(executed).withChangeOrder(2)
        .withOperations(List.of(
            LicenceOperation.newSetEquityOperation().withTransferTo(5).withEquity(BigDecimal.valueOf(100)).build()))
        .build();

    var orderCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withLicenceCorrection(correction)
        .withChangeType(LicencePositionCorrectionChangeType.UPDATE_POSITION)
        .withTargetLicencePosition(executed)
        .withPayload(UpdateLicencePositionPayloadTestUtil.newBuilder()
            .withChanges(List.of(
                LicencePositionChangeType.updateChangeOrder()
                    .withChangeId(setEquityChangeId.toString()).withChangeOrder(1).build(),
                LicencePositionChangeType.updateChangeOrder()
                    .withChangeId(adminChangeId.toString()).withChangeOrder(2).build()))
            .build())
        .build();

    when(licencePositionService.getExecutedChronologicalLicencePositions(LICENCE)).thenReturn(List.of(executed));
    when(licencePositionChangeService.findByLicencePositionIn(List.of(executed)))
        .thenReturn(List.of(liveAdminChange, liveSetEquityChange));
    when(licencePositionCorrectionService.getPositionCorrections(correction)).thenReturn(List.of(orderCorrection));

    var result = chronologicalPositionService.getCorrectedChronologicalPositions(correction, POSITION_ID);

    assertThat(result)
        .singleElement()
        .extracting(ChronologicalPosition::changes, list(PositionChange.class))
        .extracting(PositionChange::changeId, PositionChange::changeOrder, PositionChange::changeType)
        .containsExactly(
            tuple(setEquityChangeId.toString(), 1, null),
            tuple(adminChangeId.toString(), 2, null));
  }

  @Test
  void getCorrectedChronologicalPositions_whenALiveChangeIsMovedToAnotherExecutedPosition_thenShowsItAsMovedAwayOnTheSource() {
    var correction = LicenceCorrectionTestUtil.newBuilder().withId(UUID.randomUUID()).withLicence(LICENCE).build();
    var movedChangeId = UUID.randomUUID();

    var source = LicencePositionTestUtil.newBuilder()
        .withId(POSITION_ID).withLicence(LICENCE).withStatus(LicencePositionStatus.EXECUTED)
        .withLicenceTransaction(LicenceTransactionTestUtil.newBuilder().withRegulatorReference("EXEC-1").build())
        .withPositionDate(LocalDate.of(2026, Month.JANUARY, 1)).withPositionOrder(1).build();
    var target = LicencePositionTestUtil.newBuilder()
        .withId(UUID.randomUUID()).withLicence(LICENCE).withStatus(LicencePositionStatus.EXECUTED)
        .withLicenceTransaction(LicenceTransactionTestUtil.newBuilder().withRegulatorReference("EXEC-2").build())
        .withPositionDate(LocalDate.of(2026, Month.FEBRUARY, 1)).withPositionOrder(1).build();

    var liveChange = LicencePositionChangeTestUtil.newBuilder()
        .withId(movedChangeId).withLicencePosition(source).withChangeOrder(1)
        .withOperations(List.of(ADMINISTRATOR_OPERATION))
        .build();

    var targetPayload = UpdateLicencePositionPayloadTestUtil.newBuilder()
        .withCorrectionReference("EXEC-2")
        .withChanges(List.of(
            UpdateChangeOperations.buildUpdateChange(movedChangeId.toString(), ADMINISTRATOR_OPERATION),
            LicencePositionChangeType.updateChangeOrder()
                .withChangeId(movedChangeId.toString()).withChangeOrder(4).build()
        ))
        .build();
    var moveCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withLicenceCorrection(correction)
        .withChangeType(LicencePositionCorrectionChangeType.UPDATE_POSITION)
        .withTargetLicencePosition(target)
        .withPayload(targetPayload)
        .build();

    when(licencePositionService.getExecutedChronologicalLicencePositions(LICENCE)).thenReturn(List.of(source, target));
    when(licencePositionChangeService.findByLicencePositionIn(List.of(source, target))).thenReturn(List.of(liveChange));
    when(licencePositionCorrectionService.getPositionCorrections(correction)).thenReturn(List.of(moveCorrection));

    var result = chronologicalPositionService.getCorrectedChronologicalPositions(correction, POSITION_ID);

    assertThat(result).containsExactlyInAnyOrder(
        ChronologicalPosition.fromLicencePosition(
            source,
            "EXEC-1",
            source.getPositionDate(),
            source.getPositionDateOrder(),
            List.of(new PositionChange(
                movedChangeId.toString(),
                1,
                LicencePositionChangeType.REMOVE_CHANGE,
                List.of(ADMINISTRATOR_OPERATION),
                true
            ))
        ),
        ChronologicalPosition.fromLicencePosition(
            target,
            "EXEC-2",
            targetPayload.effectiveDate(),
            targetPayload.effectiveDateOrder(),
            List.of(new PositionChange(
                movedChangeId.toString(),
                4,
                LicencePositionChangeType.UPDATE_CHANGE_OPERATIONS,
                List.of(ADMINISTRATOR_OPERATION)
            ))
        )
    );
  }

  @Test
  void getCorrectedChronologicalPositions_whenALiveChangeIsMovedToAnAddedPosition_thenShowsItAsMovedAwayOnTheSource() {
    var correction = LicenceCorrectionTestUtil.newBuilder().withId(UUID.randomUUID()).withLicence(LICENCE).build();
    var movedChangeId = UUID.randomUUID();

    var source = LicencePositionTestUtil.newBuilder()
        .withId(POSITION_ID).withLicence(LICENCE).withStatus(LicencePositionStatus.EXECUTED)
        .withLicenceTransaction(LicenceTransactionTestUtil.newBuilder().withRegulatorReference("EXEC-1").build())
        .withPositionDate(LocalDate.of(2026, Month.JANUARY, 1)).withPositionOrder(1).build();

    var liveChange = LicencePositionChangeTestUtil.newBuilder()
        .withId(movedChangeId).withLicencePosition(source).withChangeOrder(1)
        .withOperations(List.of(ADMINISTRATOR_OPERATION))
        .build();

    var addedPayload = CreateLicencePositionPayloadTestUtil.newBuilder()
        .withEffectiveDate(LocalDate.of(2026, Month.JUNE, 1))
        .withCorrectionReference("ADD-REF")
        .withChanges(List.of(
            UpdateChangeOperations.buildUpdateChange(movedChangeId.toString(), ADMINISTRATOR_OPERATION),
            LicencePositionChangeType.updateChangeOrder()
                .withChangeId(movedChangeId.toString()).withChangeOrder(1).build()
        ))
        .build();
    var addedPositionCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withLicenceCorrection(correction)
        .withChangeType(LicencePositionCorrectionChangeType.ADD_POSITION)
        .withPayload(addedPayload)
        .build();

    when(licencePositionService.getExecutedChronologicalLicencePositions(LICENCE)).thenReturn(List.of(source));
    when(licencePositionChangeService.findByLicencePositionIn(List.of(source))).thenReturn(List.of(liveChange));
    when(licencePositionCorrectionService.getPositionCorrections(correction))
        .thenReturn(List.of(addedPositionCorrection));

    var result = chronologicalPositionService.getCorrectedChronologicalPositions(correction, POSITION_ID);

    assertThat(result).containsExactly(
        ChronologicalPosition.fromLicencePosition(
            source,
            "EXEC-1",
            source.getPositionDate(),
            source.getPositionDateOrder(),
            List.of(new PositionChange(
                movedChangeId.toString(),
                1,
                LicencePositionChangeType.REMOVE_CHANGE,
                List.of(ADMINISTRATOR_OPERATION),
                true
            ))
        ),
        ChronologicalPosition.fromPayload(addedPayload)
    );
    assertThat(result.get(1).changes()).containsExactly(new PositionChange(
        movedChangeId.toString(),
        1,
        LicencePositionChangeType.UPDATE_CHANGE_OPERATIONS,
        List.of(ADMINISTRATOR_OPERATION)
    ));
  }

  private static LicencePosition executedPositionOn(Licence licence, LocalDate positionDate) {
    return LicencePositionTestUtil.newBuilder()
        .withLicence(licence)
        .withStatus(LicencePositionStatus.EXECUTED)
        .withPositionDate(positionDate)
        .withPositionOrder(1)
        .build();
  }

  private static LicencePosition executedPosition(LocalDate positionDate) {
    return LicencePositionTestUtil.newBuilder()
        .withLicence(LICENCE)
        .withStatus(LicencePositionStatus.EXECUTED)
        .withPositionDate(positionDate)
        .withPositionOrder(1)
        .build();
  }

  private static LicencePositionChange liveAdminChange(UUID changeId, LicencePosition licencePosition) {
    return LicencePositionChangeTestUtil.newBuilder()
        .withId(changeId)
        .withLicencePosition(licencePosition)
        .withOperations(List.of(ADMINISTRATOR_OPERATION))
        .withChangeOrder(1)
        .build();
  }

  private static LicencePositionChange changeOn(LicencePosition position, int changeOrder, LicenceOperation operation) {
    return LicencePositionChangeTestUtil.newBuilder()
        .withLicencePosition(position)
        .withChangeOrder(changeOrder)
        .withOperations(List.of(operation))
        .build();
  }
}
