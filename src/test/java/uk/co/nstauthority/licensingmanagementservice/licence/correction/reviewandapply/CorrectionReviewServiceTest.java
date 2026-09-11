package uk.co.nstauthority.licensingmanagementservice.licence.correction.reviewandapply;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.CorrectionMarker;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.UpdateChangeOperations;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.CreateLicencePositionPayloadTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.UpdateLicencePositionPayloadTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionViewService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.PositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.PositionChangeTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.AdministratorChangeView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.ChangeViewUrls;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.LicencePositionChangeView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.transaction.LicenceTransactionTestUtil;

@ExtendWith(MockitoExtension.class)
class CorrectionReviewServiceTest {

  private static final String EXECUTED_REFERENCE = "EXEC-1";
  private static final int JOINING_ID = 100;
  private static final String JOINING_NAME = "Joining Org Ltd";
  private static final int CORRECTED_JOINING_ID = 200;
  private static final String CORRECTED_JOINING_NAME = "Corrected Org Ltd";
  private static final Map<Integer, String> ORGANISATION_NAMES =
      Map.of(JOINING_ID, JOINING_NAME, CORRECTED_JOINING_ID, CORRECTED_JOINING_NAME);

  @Mock
  private LicencePositionCorrectionService licencePositionCorrectionService;

  @Mock
  private LicencePositionViewService licencePositionViewService;

  @InjectMocks
  private CorrectionReviewService correctionReviewService;

  private final LicenceCorrection correction = LicenceCorrectionTestUtil.newBuilder().build();

  @Test
  void getReviewPositions_returnsOnlyThePositionsTouchedByTheCorrection() {
    var updatedPosition = executedPosition();
    var addedPositionId = UUID.randomUUID();

    var updateCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withChangeType(LicencePositionCorrectionChangeType.UPDATE_POSITION)
        .withTargetLicencePosition(updatedPosition)
        .withPayload(UpdateLicencePositionPayloadTestUtil.newBuilder()
            .withCorrectionReference("COR-1")
            .build())
        .build();
    var addCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withChangeType(LicencePositionCorrectionChangeType.ADD_POSITION)
        .withTargetLicencePosition(null)
        .withPayload(CreateLicencePositionPayloadTestUtil.newBuilder()
            .withLicencePositionId(addedPositionId.toString())
            .withCorrectionReference("COR-2")
            .build())
        .build();

    var chronologicalPositions = List.of(
        chronologicalPosition(UUID.randomUUID(), LocalDate.of(2026, Month.JANUARY, 1)),
        chronologicalPosition(updatedPosition.getId(), LocalDate.of(2026, Month.FEBRUARY, 1)),
        chronologicalPosition(addedPositionId, LocalDate.of(2026, Month.MARCH, 1)));

    stubTimelines(List.of(updateCorrection, addCorrection), Set.of(), chronologicalPositions, List.of());

    var result = correctionReviewService.getReviewPositions(correction);

    assertThat(result).isEqualTo(List.of(
        new ReviewPositionView(
            updatedPosition.getId(), "1 February 2026", "COR-1", CorrectionMarker.POSITION_CORRECTED, List.of()),
        new ReviewPositionView(
            addedPositionId, "1 March 2026", "COR-2", CorrectionMarker.POSITION_ADDED, List.of())));
  }

  @Test
  void getReviewPositions_whenUpdatedPositionHasNoCorrectedReference_usesTheExecutedReference() {
    var updatedPosition = executedPosition();

    var updateCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withChangeType(LicencePositionCorrectionChangeType.UPDATE_POSITION)
        .withTargetLicencePosition(updatedPosition)
        .withPayload(UpdateLicencePositionPayloadTestUtil.newBuilder().build())
        .build();

    var chronologicalPositions =
        List.of(chronologicalPosition(updatedPosition.getId(), LocalDate.of(2026, Month.FEBRUARY, 1)));

    stubTimelines(List.of(updateCorrection), Set.of(), chronologicalPositions, List.of());

    var result = correctionReviewService.getReviewPositions(correction);

    assertThat(result).isEqualTo(List.of(new ReviewPositionView(
        updatedPosition.getId(), "1 February 2026", EXECUTED_REFERENCE, CorrectionMarker.POSITION_CORRECTED, List.of())));
  }

  @Test
  void getReviewPositions_whenPositionRemoved_retainsThePositionAndTheChangesExecutedOnIt() {
    var removedPosition = executedPosition();

    var removeCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withChangeType(LicencePositionCorrectionChangeType.REMOVE_POSITION)
        .withTargetLicencePosition(removedPosition)
        .withPayload(null)
        .build();

    var changeId = UUID.randomUUID().toString();
    var chronologicalPositions = List.of(ChronologicalPositionTestUtil.newBuilder()
        .withId(removedPosition.getId())
        .withDate(LocalDate.of(2026, Month.FEBRUARY, 1))
        .withChanges(List.of(administratorChange(changeId, 1, JOINING_ID, null)))
        .build());

    stubTimelines(List.of(removeCorrection), Set.of(removedPosition.getId()), chronologicalPositions, List.of());

    var result = correctionReviewService.getReviewPositions(correction);

    assertThat(result).isEqualTo(List.of(new ReviewPositionView(
        removedPosition.getId(),
        "1 February 2026",
        EXECUTED_REFERENCE,
        CorrectionMarker.POSITION_REMOVED,
        List.of(unchanged(administratorChangeView(changeId, JOINING_NAME, null), 1)))));
  }

  @Test
  void getReviewPositions_whenChangeOperationsCorrected_thenTheChangeItWasCorrectedFromIsShown() {
    var updatedPosition = executedPosition();
    var changeId = UUID.randomUUID().toString();

    var updateCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withChangeType(LicencePositionCorrectionChangeType.UPDATE_POSITION)
        .withTargetLicencePosition(updatedPosition)
        .withPayload(UpdateLicencePositionPayloadTestUtil.newBuilder()
            .withChanges(List.of(UpdateChangeOperations.buildUpdateChange(
                changeId,
                LicenceOperation.newAdministratorChange().withOperator(CORRECTED_JOINING_ID).build())))
            .build())
        .build();

    var correctedPositions = List.of(ChronologicalPositionTestUtil.newBuilder()
        .withId(updatedPosition.getId())
        .withDate(LocalDate.of(2026, Month.FEBRUARY, 1))
        .withChanges(List.of(administratorChange(
            changeId, 1, CORRECTED_JOINING_ID, LicencePositionChangeType.UPDATE_CHANGE_OPERATIONS)))
        .build());

    var executedPositions = List.of(ChronologicalPositionTestUtil.newBuilder()
        .withId(updatedPosition.getId())
        .withDate(LocalDate.of(2026, Month.FEBRUARY, 1))
        .withChanges(List.of(administratorChange(changeId, 1, JOINING_ID, null)))
        .build());

    stubTimelines(List.of(updateCorrection), Set.of(), correctedPositions, executedPositions);

    var result = correctionReviewService.getReviewPositions(correction);

    assertThat(result).isEqualTo(List.of(new ReviewPositionView(
        updatedPosition.getId(),
        "1 February 2026",
        EXECUTED_REFERENCE,
        CorrectionMarker.POSITION_CORRECTED,
        List.of(new ReviewChangeView(
            administratorChangeView(changeId, CORRECTED_JOINING_NAME, LicencePositionChangeType.UPDATE_CHANGE_OPERATIONS),
            administratorChangeView(changeId, JOINING_NAME, null),
            1)))));
  }

  private void stubTimelines(
      List<LicencePositionCorrection> positionCorrections,
      Set<UUID> retainedRemovedPositionIds,
      List<ChronologicalPosition> correctedPositions,
      List<ChronologicalPosition> executedPositions
  ) {
    var bothTimelines = Stream.concat(correctedPositions.stream(), executedPositions.stream()).toList();

    when(licencePositionCorrectionService.getPositionCorrections(correction)).thenReturn(positionCorrections);
    when(licencePositionViewService.getCorrectedChronologicalPositions(correction, retainedRemovedPositionIds))
        .thenReturn(correctedPositions);
    when(licencePositionViewService.getLiveChronologicalPositions(correction.getLicence()))
        .thenReturn(executedPositions);
    when(licencePositionViewService.resolveOrganisationNames(bothTimelines)).thenReturn(ORGANISATION_NAMES);
    when(licencePositionViewService.resolveFeatureNames(bothTimelines)).thenReturn(Map.of());
  }

  private static LicencePosition executedPosition() {
    return LicencePositionTestUtil.newBuilder()
        .withLicenceTransaction(LicenceTransactionTestUtil.newBuilder()
            .withRegulatorReference(EXECUTED_REFERENCE)
            .build())
        .build();
  }

  private static ChronologicalPosition chronologicalPosition(UUID positionId, LocalDate date) {
    return ChronologicalPositionTestUtil.newBuilder()
        .withId(positionId)
        .withDate(date)
        .build();
  }

  private static PositionChange administratorChange(
      String changeId,
      int changeOrder,
      int operatorId,
      String changeType
  ) {
    return PositionChangeTestUtil.newBuilder()
        .withChangeId(changeId)
        .withChangeOrder(changeOrder)
        .withChangeType(changeType)
        .withAdministratorOperation(operatorId)
        .build();
  }

  private static ReviewChangeView unchanged(LicencePositionChangeView change, int changeOrder) {
    return new ReviewChangeView(change, null, changeOrder);
  }

  private static AdministratorChangeView administratorChangeView(
      String changeId,
      String joiningOrganisationName,
      String changeType
  ) {
    return new AdministratorChangeView(null, joiningOrganisationName, changeId, changeType, ChangeViewUrls.none());
  }
}