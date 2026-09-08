package uk.co.nstauthority.licensingmanagementservice.licence.position.spatial;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.fivium.gisframework.feature.FeatureService;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.CreateLicencePositionPayloadTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.UpdateLicencePositionPayloadTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionViewService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.PositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.PositionChangeTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.feature.FeatureTestUtil;

@ExtendWith(MockitoExtension.class)
class LicencePositionSpatialServiceTest {

  private static final Licence LICENCE = LicenceTestUtil.builder().build();
  private static final LicenceCorrection LICENCE_CORRECTION = LicenceCorrectionTestUtil.newBuilder()
      .withLicence(LICENCE)
      .build();

  private static final Feature BLOCK_30_1 = FeatureTestUtil.blockFeature(UUID.randomUUID(), "30", 1);
  private static final Feature BLOCK_30_2 = FeatureTestUtil.blockFeature(UUID.randomUUID(), "30", 2);
  private static final Feature BLOCK_30_3 = FeatureTestUtil.blockFeature(UUID.randomUUID(), "30", 3);
  private static final Feature SUBAREA = FeatureTestUtil.subareaFeature(UUID.randomUUID(), "30/1a");

  private static final UUID FIRST_POSITION_ID = UUID.randomUUID();
  private static final UUID SECOND_POSITION_ID = UUID.randomUUID();
  private static final UUID THIRD_POSITION_ID = UUID.randomUUID();

  @Mock
  private LicencePositionViewService licencePositionViewService;

  @Mock
  private FeatureService featureService;

  @InjectMocks
  private LicencePositionSpatialService licencePositionSpatialService;

  @Test
  void getBlockFeaturesGoingIntoChange_whenAnUpdatePositionCorrection_thenResolvesUpToTheNamedChange() {
    var targetPosition = LicencePositionTestUtil.newBuilder().withLicence(LICENCE).build();

    when(licencePositionViewService.getCorrectedChronologicalPositions(LICENCE_CORRECTION, targetPosition.getId()))
        .thenReturn(List.of(
            ChronologicalPositionTestUtil.newBuilder()
                .withId(targetPosition.getId())
                .withChanges(List.of(
                    PositionChangeTestUtil.newBuilder()
                        .withChangeId("earlier-change")
                        .withChangeOrder(1)
                        .withOperations(List.of(surrenderOutputting(Set.of(BLOCK_30_1.getId()))))
                        .build(),
                    PositionChangeTestUtil.newBuilder()
                        .withChangeId("later-change")
                        .withChangeOrder(2)
                        .withOperations(List.of(surrenderOutputting(Set.of(BLOCK_30_2.getId()))))
                        .build()))
                .build()));
    when(featureService.getFeaturesByIds(Set.of(BLOCK_30_1.getId()))).thenReturn(List.of(BLOCK_30_1));

    assertThat(licencePositionSpatialService
        .getBlockFeaturesGoingIntoChange(LICENCE_CORRECTION, targetPosition, "later-change"))
        .containsExactly(BLOCK_30_1);
  }

  @Test
  void getBlockFeaturesGoingIntoChange_whenAnAddedPositionCorrection_thenResolvesUpToTheNamedChange() {
    var addedPositionId = UUID.randomUUID();
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withLicenceCorrection(LICENCE_CORRECTION)
        .withTargetLicencePosition(null)
        .withPayload(CreateLicencePositionPayloadTestUtil.newBuilder()
            .withLicencePositionId(addedPositionId.toString())
            .build())
        .build();

    when(licencePositionViewService.getCorrectedChronologicalPositions(LICENCE_CORRECTION, addedPositionId))
        .thenReturn(List.of(
            ChronologicalPositionTestUtil.newBuilder()
                .withId(addedPositionId)
                .withChanges(List.of(
                    PositionChangeTestUtil.newBuilder()
                        .withChangeId("earlier-change")
                        .withChangeOrder(1)
                        .withOperations(List.of(surrenderOutputting(Set.of(BLOCK_30_1.getId()))))
                        .build(),
                    PositionChangeTestUtil.newBuilder()
                        .withChangeId("later-change")
                        .withChangeOrder(2)
                        .withOperations(List.of(surrenderOutputting(Set.of(BLOCK_30_2.getId()))))
                        .build()))
                .build()));
    when(featureService.getFeaturesByIds(Set.of(BLOCK_30_1.getId()))).thenReturn(List.of(BLOCK_30_1));

    assertThat(licencePositionSpatialService.getBlockFeaturesGoingIntoChange(positionCorrection, "later-change"))
        .containsExactly(BLOCK_30_1);
  }

  @Test
  void getBlockFeaturesGoingIntoChange_whenAnUpdatePositionCorrection_thenExcludesSubareasAndOrdersByBlock() {
    var targetPosition = LicencePositionTestUtil.newBuilder().withLicence(LICENCE).build();
    var heldFeatureIds = Set.of(SUBAREA.getId(), BLOCK_30_2.getId(), BLOCK_30_1.getId());

    when(licencePositionViewService.getCorrectedChronologicalPositions(LICENCE_CORRECTION, targetPosition.getId()))
        .thenReturn(List.of(
            ChronologicalPositionTestUtil.newBuilder()
                .withId(targetPosition.getId())
                .withChanges(List.of(PositionChangeTestUtil.newBuilder()
                    .withOperations(List.of(surrenderOutputting(heldFeatureIds)))
                    .build()))
                .build()));
    when(featureService.getFeaturesByIds(heldFeatureIds)).thenReturn(List.of(SUBAREA, BLOCK_30_2, BLOCK_30_1));

    assertThat(licencePositionSpatialService.getBlockFeaturesGoingIntoChange(LICENCE_CORRECTION, targetPosition, null))
        .containsExactly(BLOCK_30_1, BLOCK_30_2);
  }

  @Test
  void getBlockFeaturesGoingIntoChange_whenAPositionCorrection_thenExcludesSubareasAndOrdersByBlock() {
    var targetPosition = LicencePositionTestUtil.newBuilder().withLicence(LICENCE).build();
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withLicenceCorrection(LICENCE_CORRECTION)
        .withTargetLicencePosition(targetPosition)
        .withPayload(UpdateLicencePositionPayloadTestUtil.newBuilder().build())
        .build();
    var heldFeatureIds = Set.of(SUBAREA.getId(), BLOCK_30_2.getId(), BLOCK_30_1.getId());

    when(licencePositionViewService.getCorrectedChronologicalPositions(LICENCE_CORRECTION, targetPosition.getId()))
        .thenReturn(List.of(
            ChronologicalPositionTestUtil.newBuilder()
                .withId(targetPosition.getId())
                .withChanges(List.of(PositionChangeTestUtil.newBuilder()
                    .withOperations(List.of(surrenderOutputting(heldFeatureIds)))
                    .build()))
                .build()));
    when(featureService.getFeaturesByIds(heldFeatureIds)).thenReturn(List.of(SUBAREA, BLOCK_30_2, BLOCK_30_1));

    assertThat(licencePositionSpatialService.getBlockFeaturesGoingIntoChange(positionCorrection, null))
        .containsExactly(BLOCK_30_1, BLOCK_30_2);
  }

  @Test
  void getBlockFeaturesGoingIntoChange_whenSeveralSurrendersPrecedeThePosition_thenTheMostRecentOutputsWin() {
    var positions = List.of(
        position(FIRST_POSITION_ID, 1, surrenderOutputting(BLOCK_30_1.getId(), BLOCK_30_2.getId(), BLOCK_30_3.getId())),
        position(SECOND_POSITION_ID, 2, surrenderOutputting(BLOCK_30_2.getId(), BLOCK_30_3.getId())),
        position(THIRD_POSITION_ID, 3));

    when(licencePositionViewService.getCorrectedChronologicalPositions(LICENCE_CORRECTION, THIRD_POSITION_ID))
        .thenReturn(positions);
    when(featureService.getFeaturesByIds(Set.of(BLOCK_30_2.getId(), BLOCK_30_3.getId())))
        .thenReturn(List.of(BLOCK_30_2, BLOCK_30_3));

    assertThat(licencePositionSpatialService
        .getBlockFeaturesGoingIntoChange(LICENCE_CORRECTION, THIRD_POSITION_ID, null))
        .containsExactly(BLOCK_30_2, BLOCK_30_3);
  }

  @Test
  void getBlockFeaturesGoingIntoChange_whenAnEarlierSurrenderHasNoOutputFeatures_thenItIsIgnored() {
    var positions = List.of(
        position(FIRST_POSITION_ID, 1, surrenderOutputting(BLOCK_30_1.getId(), BLOCK_30_2.getId())),
        position(SECOND_POSITION_ID, 2, incompleteSurrender()),
        position(THIRD_POSITION_ID, 3));

    when(licencePositionViewService.getCorrectedChronologicalPositions(LICENCE_CORRECTION, THIRD_POSITION_ID))
        .thenReturn(positions);
    when(featureService.getFeaturesByIds(Set.of(BLOCK_30_1.getId(), BLOCK_30_2.getId())))
        .thenReturn(List.of(BLOCK_30_1, BLOCK_30_2));

    assertThat(licencePositionSpatialService
        .getBlockFeaturesGoingIntoChange(LICENCE_CORRECTION, THIRD_POSITION_ID, null))
        .containsExactly(BLOCK_30_1, BLOCK_30_2);
  }

  @Test
  void getBlockFeaturesGoingIntoChange_whenAnEarlierSurrenderIsStagedForRemoval_thenItDoesNotContribute() {
    var removedSurrender = PositionChangeTestUtil.newBuilder()
        .withChangeType(LicencePositionChangeType.REMOVE_CHANGE)
        .withOperations(List.of(surrenderOutputting(BLOCK_30_3.getId())))
        .build();
    var positions = List.of(
        position(FIRST_POSITION_ID, 1, surrenderOutputting(BLOCK_30_1.getId(), BLOCK_30_2.getId())),
        ChronologicalPositionTestUtil.newBuilder()
            .withId(SECOND_POSITION_ID)
            .withDate(LocalDate.of(2026, Month.JANUARY, 2))
            .withChanges(List.of(removedSurrender))
            .build(),
        position(THIRD_POSITION_ID, 3));

    when(licencePositionViewService.getCorrectedChronologicalPositions(LICENCE_CORRECTION, THIRD_POSITION_ID))
        .thenReturn(positions);
    when(featureService.getFeaturesByIds(Set.of(BLOCK_30_1.getId(), BLOCK_30_2.getId())))
        .thenReturn(List.of(BLOCK_30_1, BLOCK_30_2));

    assertThat(licencePositionSpatialService
        .getBlockFeaturesGoingIntoChange(LICENCE_CORRECTION, THIRD_POSITION_ID, null))
        .containsExactly(BLOCK_30_1, BLOCK_30_2);
  }

  @Test
  void getBlockFeaturesGoingIntoChange_whenNoPositionCarriesASpatialOperation_thenEmpty() {
    var positions = List.of(position(FIRST_POSITION_ID, 1), position(SECOND_POSITION_ID, 2));

    when(licencePositionViewService.getCorrectedChronologicalPositions(LICENCE_CORRECTION, SECOND_POSITION_ID))
        .thenReturn(positions);

    assertThat(licencePositionSpatialService
        .getBlockFeaturesGoingIntoChange(LICENCE_CORRECTION, SECOND_POSITION_ID, null))
        .isEmpty();
  }

  @Test
  void getBlockFeaturesGoingIntoChange_whenThePositionIsNotInTheStream_thenEveryOperationIsApplied() {
    var absentPositionId = UUID.randomUUID();
    var positions = List.of(
        position(FIRST_POSITION_ID, 1, surrenderOutputting(BLOCK_30_1.getId(), BLOCK_30_2.getId())),
        position(SECOND_POSITION_ID, 2, surrenderOutputting(BLOCK_30_1.getId())));

    when(licencePositionViewService.getCorrectedChronologicalPositions(LICENCE_CORRECTION, absentPositionId))
        .thenReturn(positions);
    when(featureService.getFeaturesByIds(Set.of(BLOCK_30_1.getId()))).thenReturn(List.of(BLOCK_30_1));

    assertThat(licencePositionSpatialService
        .getBlockFeaturesGoingIntoChange(LICENCE_CORRECTION, absentPositionId, null))
        .containsExactly(BLOCK_30_1);
  }

  @Test
  void getBlockFeaturesGoingIntoChange_whenALaterSurrenderIsInvalidatedByAnEarlierOne_thenItsOutputsStillApply() {
    var positions = List.of(
        position(FIRST_POSITION_ID, 1, surrenderOutputting(BLOCK_30_3.getId())),
        position(SECOND_POSITION_ID, 2, surrenderOutputting(BLOCK_30_1.getId(), BLOCK_30_2.getId())),
        position(THIRD_POSITION_ID, 3));

    when(licencePositionViewService.getCorrectedChronologicalPositions(LICENCE_CORRECTION, THIRD_POSITION_ID))
        .thenReturn(positions);
    when(featureService.getFeaturesByIds(Set.of(BLOCK_30_1.getId(), BLOCK_30_2.getId())))
        .thenReturn(List.of(BLOCK_30_1, BLOCK_30_2));

    assertThat(licencePositionSpatialService
        .getBlockFeaturesGoingIntoChange(LICENCE_CORRECTION, THIRD_POSITION_ID, null))
        .containsExactly(BLOCK_30_1, BLOCK_30_2);
  }

  @Test
  void getBlockFeaturesGoingIntoChange_whenASubareaOperationPrecedesThePosition_thenTheFeatureSetIsUnchanged() {
    var positions = List.of(
        position(FIRST_POSITION_ID, 1, surrenderOutputting(BLOCK_30_1.getId(), BLOCK_30_2.getId())),
        position(SECOND_POSITION_ID, 2, subareaChangeFor(BLOCK_30_1.getId())),
        position(THIRD_POSITION_ID, 3));

    when(licencePositionViewService.getCorrectedChronologicalPositions(LICENCE_CORRECTION, THIRD_POSITION_ID))
        .thenReturn(positions);
    when(featureService.getFeaturesByIds(Set.of(BLOCK_30_1.getId(), BLOCK_30_2.getId())))
        .thenReturn(List.of(BLOCK_30_1, BLOCK_30_2));

    assertThat(licencePositionSpatialService
        .getBlockFeaturesGoingIntoChange(LICENCE_CORRECTION, THIRD_POSITION_ID, null))
        .containsExactly(BLOCK_30_1, BLOCK_30_2);
  }

  @Test
  void getBlockFeaturesGoingIntoChange_whenChangeIdIsNull_thenFoldsTheWholePosition() {
    var positions = List.of(ChronologicalPositionTestUtil.newBuilder()
        .withId(FIRST_POSITION_ID)
        .withChanges(List.of(
            PositionChangeTestUtil.newBuilder()
                .withChangeOrder(1)
                .withOperations(List.of(surrenderOutputting(BLOCK_30_1.getId(), BLOCK_30_2.getId())))
                .build(),
            PositionChangeTestUtil.newBuilder()
                .withChangeOrder(2)
                .withOperations(List.of(surrenderOutputting(BLOCK_30_1.getId())))
                .build()))
        .build());

    when(licencePositionViewService.getCorrectedChronologicalPositions(LICENCE_CORRECTION, FIRST_POSITION_ID))
        .thenReturn(positions);
    when(featureService.getFeaturesByIds(Set.of(BLOCK_30_1.getId()))).thenReturn(List.of(BLOCK_30_1));

    assertThat(licencePositionSpatialService
        .getBlockFeaturesGoingIntoChange(LICENCE_CORRECTION, FIRST_POSITION_ID, null))
        .containsExactly(BLOCK_30_1);
  }

  @Test
  void getBlockFeaturesGoingIntoChange_whenChangeIdDoesNotMatchAnyChange_thenFoldsTheWholePosition() {
    var positions = List.of(ChronologicalPositionTestUtil.newBuilder()
        .withId(FIRST_POSITION_ID)
        .withChanges(List.of(PositionChangeTestUtil.newBuilder()
            .withChangeOrder(1)
            .withOperations(List.of(surrenderOutputting(BLOCK_30_1.getId())))
            .build()))
        .build());

    when(licencePositionViewService.getCorrectedChronologicalPositions(LICENCE_CORRECTION, FIRST_POSITION_ID))
        .thenReturn(positions);
    when(featureService.getFeaturesByIds(Set.of(BLOCK_30_1.getId()))).thenReturn(List.of(BLOCK_30_1));

    assertThat(licencePositionSpatialService
        .getBlockFeaturesGoingIntoChange(LICENCE_CORRECTION, FIRST_POSITION_ID, UUID.randomUUID().toString()))
        .containsExactly(BLOCK_30_1);
  }

  @Test
  void getBlockFeaturesGoingIntoChange_whenAnEarlierChangeIsStagedForRemoval_thenItDoesNotContribute() {
    var positions = List.of(ChronologicalPositionTestUtil.newBuilder()
        .withId(FIRST_POSITION_ID)
        .withChanges(List.of(
            PositionChangeTestUtil.newBuilder()
                .withChangeId("removed-change")
                .withChangeOrder(1)
                .withChangeType(LicencePositionChangeType.REMOVE_CHANGE)
                .withOperations(List.of(surrenderOutputting(BLOCK_30_1.getId())))
                .build(),
            PositionChangeTestUtil.newBuilder()
                .withChangeId("target-change")
                .withChangeOrder(2)
                .withOperations(List.of(subareaChangeFor(BLOCK_30_2.getId())))
                .build()))
        .build());

    when(licencePositionViewService.getCorrectedChronologicalPositions(LICENCE_CORRECTION, FIRST_POSITION_ID))
        .thenReturn(positions);

    assertThat(licencePositionSpatialService
        .getBlockFeaturesGoingIntoChange(LICENCE_CORRECTION, FIRST_POSITION_ID, "target-change"))
        .isEmpty();
  }

  @Test
  void getBlockFeaturesGoingIntoChange_whenASurrenderOnTheSamePositionPrecedesTheChange_thenItIsInTheInput() {
    var positions = List.of(ChronologicalPositionTestUtil.newBuilder()
        .withId(FIRST_POSITION_ID)
        .withChanges(List.of(
            PositionChangeTestUtil.newBuilder()
                .withChangeId("surrender-change")
                .withChangeOrder(1)
                .withOperations(List.of(surrenderOutputting(BLOCK_30_1.getId())))
                .build(),
            PositionChangeTestUtil.newBuilder()
                .withChangeId("subarea-change")
                .withChangeOrder(2)
                .withOperations(List.of(subareaChangeFor(BLOCK_30_1.getId())))
                .build()))
        .build());

    when(licencePositionViewService.getCorrectedChronologicalPositions(LICENCE_CORRECTION, FIRST_POSITION_ID))
        .thenReturn(positions);
    when(featureService.getFeaturesByIds(Set.of(BLOCK_30_1.getId()))).thenReturn(List.of(BLOCK_30_1));

    assertThat(licencePositionSpatialService
        .getBlockFeaturesGoingIntoChange(LICENCE_CORRECTION, FIRST_POSITION_ID, "subarea-change"))
        .containsExactly(BLOCK_30_1);
  }

  private static ChronologicalPosition position(UUID positionId, int dayOfMonth, LicenceOperation... operations) {
    var changes = operations.length == 0
        ? List.<PositionChange>of()
        : List.of(PositionChangeTestUtil.newBuilder().withOperations(List.of(operations)).build());

    return ChronologicalPositionTestUtil.newBuilder()
        .withId(positionId)
        .withDate(LocalDate.of(2026, Month.JANUARY, dayOfMonth))
        .withChanges(changes)
        .build();
  }

  private static LicenceOperation surrenderOutputting(Set<UUID> outputFeatureIds) {
    return LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(UUID.randomUUID()))
        .withOutputFeatureIds(outputFeatureIds)
        .build();
  }

  private static LicenceOperation surrenderOutputting(UUID... outputFeatureIds) {
    return surrenderOutputting(Set.of(outputFeatureIds));
  }

  private static LicenceOperation incompleteSurrender() {
    return LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(UUID.randomUUID()))
        .build();
  }

  private static LicenceOperation subareaChangeFor(UUID featureId) {
    return LicenceOperation.newSubAreaOperation()
        .withFeatureId(featureId)
        .build();
  }
}
