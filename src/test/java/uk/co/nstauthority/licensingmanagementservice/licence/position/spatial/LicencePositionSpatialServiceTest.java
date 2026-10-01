package uk.co.nstauthority.licensingmanagementservice.licence.position.spatial;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
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
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.blocksurrendertype.BlockSurrenderType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.CreateLicencePositionPayloadTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.UpdateLicencePositionPayloadTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.PartialSurrenderOperation.SurrenderDetails;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaDetails;
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
  private static final Feature BLOCK_30_4 = FeatureTestUtil.blockFeature(UUID.randomUUID(), "30", 4);
  private static final Feature BLOCK_29_10 = FeatureTestUtil.blockFeature(UUID.randomUUID(), "29", 10);
  private static final Feature SUBAREA = FeatureTestUtil.subareaFeature(UUID.randomUUID(), "30/1a");
  private static final UUID UNHELD_BLOCK_ID = UUID.randomUUID();
  private static final SubareaDetails SUBAREA_A = new SubareaDetails(UUID.randomUUID(), "Subarea A", "A");
  private static final SubareaDetails SUBAREA_B = new SubareaDetails(UUID.randomUUID(), "Subarea B", "B");
  private static final SubareaDetails SUBAREA_C = new SubareaDetails(UUID.randomUUID(), "Subarea C", "C");

  private static final UUID FIRST_POSITION_ID = UUID.randomUUID();
  private static final UUID SECOND_POSITION_ID = UUID.randomUUID();
  private static final UUID THIRD_POSITION_ID = UUID.randomUUID();
  private static final UUID FOURTH_POSITION_ID = UUID.randomUUID();

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
                        .withOperations(List.of(blockCreateOf(BLOCK_30_1.getId())))
                        .build(),
                    PositionChangeTestUtil.newBuilder()
                        .withChangeId("later-change")
                        .withChangeOrder(2)
                        .withOperations(List.of(fullSurrenderOf(BLOCK_30_1.getId())))
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
                        .withOperations(List.of(blockCreateOf(BLOCK_30_1.getId())))
                        .build(),
                    PositionChangeTestUtil.newBuilder()
                        .withChangeId("later-change")
                        .withChangeOrder(2)
                        .withOperations(List.of(fullSurrenderOf(BLOCK_30_1.getId())))
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
                    .withOperations(List.of(blockCreateOf(SUBAREA.getId(), BLOCK_30_2.getId(), BLOCK_30_1.getId())))
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
                    .withOperations(List.of(blockCreateOf(SUBAREA.getId(), BLOCK_30_2.getId(), BLOCK_30_1.getId())))
                    .build()))
                .build()));
    when(featureService.getFeaturesByIds(heldFeatureIds)).thenReturn(List.of(SUBAREA, BLOCK_30_2, BLOCK_30_1));

    assertThat(licencePositionSpatialService.getBlockFeaturesGoingIntoChange(positionCorrection, null))
        .containsExactly(BLOCK_30_1, BLOCK_30_2);
  }

  @Test
  void getBlockFeaturesGoingIntoChange_whenSeveralSurrendersPrecedeThePosition_thenEachGivesUpItsBlocks() {
    var positions = List.of(
        position(FIRST_POSITION_ID, 1, blockCreateOf(BLOCK_30_1.getId(), BLOCK_30_2.getId(), BLOCK_30_3.getId())),
        position(SECOND_POSITION_ID, 2, fullSurrenderOf(BLOCK_30_1.getId())),
        position(THIRD_POSITION_ID, 3, fullSurrenderOf(BLOCK_30_2.getId())),
        position(FOURTH_POSITION_ID, 4)
    );

    when(licencePositionViewService.getCorrectedChronologicalPositions(LICENCE_CORRECTION, FOURTH_POSITION_ID))
        .thenReturn(positions);
    when(featureService.getFeaturesByIds(Set.of(BLOCK_30_3.getId()))).thenReturn(List.of(BLOCK_30_3));

    assertThat(licencePositionSpatialService
        .getBlockFeaturesGoingIntoChange(LICENCE_CORRECTION, FOURTH_POSITION_ID, null))
        .containsExactly(BLOCK_30_3);
  }

  @Test
  void getBlockFeaturesGoingIntoChange_whenAnEarlierSurrenderHasNotTakenEffect_thenItIsIgnored() {
    var positions = List.of(
        position(FIRST_POSITION_ID, 1, blockCreateOf(BLOCK_30_1.getId(), BLOCK_30_2.getId())),
        position(SECOND_POSITION_ID, 2, incompleteSurrenderOf(BLOCK_30_1.getId())),
        position(THIRD_POSITION_ID, 3)
    );

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
        .withOperations(List.of(fullSurrenderOf(BLOCK_30_1.getId())))
        .build();
    var positions = List.of(
        position(FIRST_POSITION_ID, 1, blockCreateOf(BLOCK_30_1.getId(), BLOCK_30_2.getId())),
        ChronologicalPositionTestUtil.newBuilder()
            .withId(SECOND_POSITION_ID)
            .withDate(LocalDate.of(2026, Month.JANUARY, 2))
            .withChanges(List.of(removedSurrender))
            .build(),
        position(THIRD_POSITION_ID, 3)
    );

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
        position(FIRST_POSITION_ID, 1, blockCreateOf(BLOCK_30_1.getId(), BLOCK_30_2.getId())),
        position(SECOND_POSITION_ID, 2, fullSurrenderOf(BLOCK_30_2.getId()))
    );

    when(licencePositionViewService.getCorrectedChronologicalPositions(LICENCE_CORRECTION, absentPositionId))
        .thenReturn(positions);
    when(featureService.getFeaturesByIds(Set.of(BLOCK_30_1.getId()))).thenReturn(List.of(BLOCK_30_1));

    assertThat(licencePositionSpatialService
        .getBlockFeaturesGoingIntoChange(LICENCE_CORRECTION, absentPositionId, null))
        .containsExactly(BLOCK_30_1);
  }

  @Test
  void getBlockFeaturesGoingIntoChange_whenASurrenderGivesUpABlockNoLongerHeld_thenTheRestOfItStillApplies() {
    var positions = List.of(
        position(FIRST_POSITION_ID, 1, blockCreateOf(BLOCK_30_1.getId(), BLOCK_30_2.getId(), BLOCK_30_3.getId())),
        position(SECOND_POSITION_ID, 2, fullSurrenderOf(BLOCK_30_1.getId())),
        position(THIRD_POSITION_ID, 3, fullSurrenderOf(BLOCK_30_1.getId(), BLOCK_30_2.getId())),
        position(FOURTH_POSITION_ID, 4)
    );

    when(licencePositionViewService.getCorrectedChronologicalPositions(LICENCE_CORRECTION, FOURTH_POSITION_ID))
        .thenReturn(positions);
    when(featureService.getFeaturesByIds(Set.of(BLOCK_30_3.getId()))).thenReturn(List.of(BLOCK_30_3));

    assertThat(licencePositionSpatialService
        .getBlockFeaturesGoingIntoChange(LICENCE_CORRECTION, FOURTH_POSITION_ID, null))
        .containsExactly(BLOCK_30_3);
  }

  @ParameterizedTest
  @MethodSource("subareaOperations")
  void getBlockFeaturesGoingIntoChange_whenASubareaOperationPrecedesThePosition_thenTheFeatureSetIsUnchanged(
      LicenceOperation subareaOperation
  ) {
    var positions = List.of(
        position(FIRST_POSITION_ID, 1, blockCreateOf(BLOCK_30_1.getId(), BLOCK_30_2.getId())),
        position(SECOND_POSITION_ID, 2, subareaOperation),
        position(THIRD_POSITION_ID, 3)
    );

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
                .withOperations(List.of(blockCreateOf(BLOCK_30_1.getId(), BLOCK_30_2.getId())))
                .build(),
            PositionChangeTestUtil.newBuilder()
                .withChangeOrder(2)
                .withOperations(List.of(fullSurrenderOf(BLOCK_30_2.getId())))
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
            .withOperations(List.of(blockCreateOf(BLOCK_30_1.getId())))
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
                .withOperations(List.of(blockCreateOf(BLOCK_30_1.getId())))
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
    var positions = List.of(positionWithChanges(
        FIRST_POSITION_ID,
        1,
        change("create-change", 1, blockCreateOf(BLOCK_30_1.getId(), BLOCK_30_2.getId())),
        change("surrender-change", 2, fullSurrenderOf(BLOCK_30_2.getId())),
        change("subarea-change", 3, subareaChangeFor(BLOCK_30_1.getId()))
    ));

    when(licencePositionViewService.getCorrectedChronologicalPositions(LICENCE_CORRECTION, FIRST_POSITION_ID))
        .thenReturn(positions);
    when(featureService.getFeaturesByIds(Set.of(BLOCK_30_1.getId()))).thenReturn(List.of(BLOCK_30_1));

    assertThat(licencePositionSpatialService
        .getBlockFeaturesGoingIntoChange(LICENCE_CORRECTION, FIRST_POSITION_ID, "subarea-change"))
        .containsExactly(BLOCK_30_1);
  }

  @Test
  void getBlockFeaturesGoingIntoChange_whenBlocksAreCreatedOnAnEmptyHolding_thenOnlyThoseBlocksAreHeld() {
    var positions = List.of(
        position(FIRST_POSITION_ID, 1, blockCreateOf(BLOCK_30_1.getId(), BLOCK_30_2.getId())),
        position(SECOND_POSITION_ID, 2)
    );

    when(licencePositionViewService.getCorrectedChronologicalPositions(LICENCE_CORRECTION, SECOND_POSITION_ID))
        .thenReturn(positions);
    when(featureService.getFeaturesByIds(Set.of(BLOCK_30_1.getId(), BLOCK_30_2.getId())))
        .thenReturn(List.of(BLOCK_30_1, BLOCK_30_2));

    assertThat(licencePositionSpatialService
        .getBlockFeaturesGoingIntoChange(LICENCE_CORRECTION, SECOND_POSITION_ID, null))
        .containsExactly(BLOCK_30_1, BLOCK_30_2);
  }

  @Test
  void getBlockFeaturesGoingIntoChange_whenBlocksAreCreatedOnTopOfAnExistingHolding_thenTheyAreAdded() {
    var positions = List.of(
        position(FIRST_POSITION_ID, 1, blockCreateOf(BLOCK_30_1.getId())),
        position(SECOND_POSITION_ID, 2, blockCreateOf(BLOCK_30_2.getId(), BLOCK_30_3.getId())),
        position(THIRD_POSITION_ID, 3)
    );

    when(licencePositionViewService.getCorrectedChronologicalPositions(LICENCE_CORRECTION, THIRD_POSITION_ID))
        .thenReturn(positions);
    when(featureService.getFeaturesByIds(Set.of(BLOCK_30_1.getId(), BLOCK_30_2.getId(), BLOCK_30_3.getId())))
        .thenReturn(List.of(BLOCK_30_1, BLOCK_30_2, BLOCK_30_3));

    assertThat(licencePositionSpatialService
        .getBlockFeaturesGoingIntoChange(LICENCE_CORRECTION, THIRD_POSITION_ID, null))
        .containsExactly(BLOCK_30_1, BLOCK_30_2, BLOCK_30_3);
  }

  @Test
  void getBlockFeaturesGoingIntoChange_whenACreatedBlockIsAlreadyHeld_thenItIsNotDuplicated() {
    var positions = List.of(
        position(FIRST_POSITION_ID, 1, blockCreateOf(BLOCK_30_1.getId(), BLOCK_30_2.getId())),
        position(SECOND_POSITION_ID, 2, blockCreateOf(BLOCK_30_2.getId())),
        position(THIRD_POSITION_ID, 3)
    );

    when(licencePositionViewService.getCorrectedChronologicalPositions(LICENCE_CORRECTION, THIRD_POSITION_ID))
        .thenReturn(positions);
    when(featureService.getFeaturesByIds(Set.of(BLOCK_30_1.getId(), BLOCK_30_2.getId())))
        .thenReturn(List.of(BLOCK_30_1, BLOCK_30_2));

    assertThat(licencePositionSpatialService
        .getBlockFeaturesGoingIntoChange(LICENCE_CORRECTION, THIRD_POSITION_ID, null))
        .containsExactly(BLOCK_30_1, BLOCK_30_2);
  }

  @Test
  void getBlockFeaturesGoingIntoChange_whenACreateHasNoFeatures_thenTheHoldingIsUnchanged() {
    var positions = List.of(
        position(FIRST_POSITION_ID, 1, blockCreateOf(BLOCK_30_1.getId())),
        position(SECOND_POSITION_ID, 2, blockCreateOf()),
        position(THIRD_POSITION_ID, 3)
    );

    when(licencePositionViewService.getCorrectedChronologicalPositions(LICENCE_CORRECTION, THIRD_POSITION_ID))
        .thenReturn(positions);
    when(featureService.getFeaturesByIds(Set.of(BLOCK_30_1.getId()))).thenReturn(List.of(BLOCK_30_1));

    assertThat(licencePositionSpatialService
        .getBlockFeaturesGoingIntoChange(LICENCE_CORRECTION, THIRD_POSITION_ID, null))
        .containsExactly(BLOCK_30_1);
  }

  @Test
  void getBlockFeaturesGoingIntoChange_whenBlocksAreRedefined_thenTheReplacedBlocksAreSwappedForSuccessors() {
    var positions = List.of(
        position(FIRST_POSITION_ID, 1, blockCreateOf(BLOCK_30_1.getId(), BLOCK_30_2.getId(), BLOCK_30_3.getId())),
        position(SECOND_POSITION_ID, 2, blockRedefinitionOf(Set.of(BLOCK_30_2.getId()), Set.of(BLOCK_30_4.getId()))),
        position(THIRD_POSITION_ID, 3)
    );

    when(licencePositionViewService.getCorrectedChronologicalPositions(LICENCE_CORRECTION, THIRD_POSITION_ID))
        .thenReturn(positions);
    when(featureService.getFeaturesByIds(Set.of(BLOCK_30_1.getId(), BLOCK_30_3.getId(), BLOCK_30_4.getId())))
        .thenReturn(List.of(BLOCK_30_1, BLOCK_30_3, BLOCK_30_4));

    assertThat(licencePositionSpatialService
        .getBlockFeaturesGoingIntoChange(LICENCE_CORRECTION, THIRD_POSITION_ID, null))
        .containsExactly(BLOCK_30_1, BLOCK_30_3, BLOCK_30_4);
  }

  @Test
  void getBlockFeaturesGoingIntoChange_whenARedefinitionReplacesABlockNotHeld_thenOnlyItsOutputsAreAdded() {
    var positions = List.of(
        position(FIRST_POSITION_ID, 1, blockCreateOf(BLOCK_30_1.getId())),
        position(SECOND_POSITION_ID, 2, blockRedefinitionOf(Set.of(UNHELD_BLOCK_ID), Set.of(BLOCK_30_4.getId()))),
        position(THIRD_POSITION_ID, 3)
    );

    when(licencePositionViewService.getCorrectedChronologicalPositions(LICENCE_CORRECTION, THIRD_POSITION_ID))
        .thenReturn(positions);
    when(featureService.getFeaturesByIds(Set.of(BLOCK_30_1.getId(), BLOCK_30_4.getId())))
        .thenReturn(List.of(BLOCK_30_1, BLOCK_30_4));

    assertThat(licencePositionSpatialService
        .getBlockFeaturesGoingIntoChange(LICENCE_CORRECTION, THIRD_POSITION_ID, null))
        .containsExactly(BLOCK_30_1, BLOCK_30_4);
  }

  @Test
  void getBlockFeaturesGoingIntoChange_whenARedefinitionReplacesNothing_thenItOnlyAdds() {
    var positions = List.of(
        position(FIRST_POSITION_ID, 1, blockCreateOf(BLOCK_30_1.getId())),
        position(SECOND_POSITION_ID, 2, blockRedefinitionOf(Set.of(), Set.of(BLOCK_30_4.getId()))),
        position(THIRD_POSITION_ID, 3)
    );

    when(licencePositionViewService.getCorrectedChronologicalPositions(LICENCE_CORRECTION, THIRD_POSITION_ID))
        .thenReturn(positions);
    when(featureService.getFeaturesByIds(Set.of(BLOCK_30_1.getId(), BLOCK_30_4.getId())))
        .thenReturn(List.of(BLOCK_30_1, BLOCK_30_4));

    assertThat(licencePositionSpatialService
        .getBlockFeaturesGoingIntoChange(LICENCE_CORRECTION, THIRD_POSITION_ID, null))
        .containsExactly(BLOCK_30_1, BLOCK_30_4);
  }

  @Test
  void getBlockFeaturesGoingIntoChange_whenARedefinitionOutputsNothing_thenItOnlyRemoves() {
    var positions = List.of(
        position(FIRST_POSITION_ID, 1, blockCreateOf(BLOCK_30_1.getId(), BLOCK_30_2.getId())),
        position(SECOND_POSITION_ID, 2, blockRedefinitionOf(Set.of(BLOCK_30_1.getId()), Set.of())),
        position(THIRD_POSITION_ID, 3)
    );

    when(licencePositionViewService.getCorrectedChronologicalPositions(LICENCE_CORRECTION, THIRD_POSITION_ID))
        .thenReturn(positions);
    when(featureService.getFeaturesByIds(Set.of(BLOCK_30_2.getId()))).thenReturn(List.of(BLOCK_30_2));

    assertThat(licencePositionSpatialService
        .getBlockFeaturesGoingIntoChange(LICENCE_CORRECTION, THIRD_POSITION_ID, null))
        .containsExactly(BLOCK_30_2);
  }

  @Test
  void getBlockFeaturesGoingIntoChange_whenARedefinitionOutputsABlockItAlsoReplaces_thenTheBlockIsStillHeld() {
    var positions = List.of(
        position(FIRST_POSITION_ID, 1, blockCreateOf(BLOCK_30_1.getId(), BLOCK_30_2.getId())),
        position(SECOND_POSITION_ID, 2, blockRedefinitionOf(
            Set.of(BLOCK_30_1.getId(), BLOCK_30_2.getId()), Set.of(BLOCK_30_1.getId()))),
        position(THIRD_POSITION_ID, 3)
    );

    when(licencePositionViewService.getCorrectedChronologicalPositions(LICENCE_CORRECTION, THIRD_POSITION_ID))
        .thenReturn(positions);
    when(featureService.getFeaturesByIds(Set.of(BLOCK_30_1.getId()))).thenReturn(List.of(BLOCK_30_1));

    assertThat(licencePositionSpatialService
        .getBlockFeaturesGoingIntoChange(LICENCE_CORRECTION, THIRD_POSITION_ID, null))
        .containsExactly(BLOCK_30_1);
  }

  @Test
  void getBlockFeaturesGoingIntoChange_whenBlocksAreEnded_thenOnlyThoseBlocksAreRemoved() {
    var positions = List.of(
        position(FIRST_POSITION_ID, 1, blockCreateOf(BLOCK_30_1.getId(), BLOCK_30_2.getId(), BLOCK_30_3.getId())),
        position(SECOND_POSITION_ID, 2, blockEndOf(BLOCK_30_2.getId())),
        position(THIRD_POSITION_ID, 3)
    );

    when(licencePositionViewService.getCorrectedChronologicalPositions(LICENCE_CORRECTION, THIRD_POSITION_ID))
        .thenReturn(positions);
    when(featureService.getFeaturesByIds(Set.of(BLOCK_30_1.getId(), BLOCK_30_3.getId())))
        .thenReturn(List.of(BLOCK_30_1, BLOCK_30_3));

    assertThat(licencePositionSpatialService
        .getBlockFeaturesGoingIntoChange(LICENCE_CORRECTION, THIRD_POSITION_ID, null))
        .containsExactly(BLOCK_30_1, BLOCK_30_3);
  }

  @Test
  void getBlockFeaturesGoingIntoChange_whenAnEndedBlockIsNotHeld_thenTheHoldingIsUnchanged() {
    var positions = List.of(
        position(FIRST_POSITION_ID, 1, blockCreateOf(BLOCK_30_1.getId())),
        position(SECOND_POSITION_ID, 2, blockEndOf(UNHELD_BLOCK_ID)),
        position(THIRD_POSITION_ID, 3)
    );

    when(licencePositionViewService.getCorrectedChronologicalPositions(LICENCE_CORRECTION, THIRD_POSITION_ID))
        .thenReturn(positions);
    when(featureService.getFeaturesByIds(Set.of(BLOCK_30_1.getId()))).thenReturn(List.of(BLOCK_30_1));

    assertThat(licencePositionSpatialService
        .getBlockFeaturesGoingIntoChange(LICENCE_CORRECTION, THIRD_POSITION_ID, null))
        .containsExactly(BLOCK_30_1);
  }

  @Test
  void getBlockFeaturesGoingIntoChange_whenEveryHeldBlockIsEnded_thenNoFeaturesAreLookedUp() {
    var positions = List.of(
        position(FIRST_POSITION_ID, 1, blockCreateOf(BLOCK_30_1.getId(), BLOCK_30_2.getId())),
        position(SECOND_POSITION_ID, 2, blockEndOf(BLOCK_30_1.getId(), BLOCK_30_2.getId())),
        position(THIRD_POSITION_ID, 3)
    );

    when(licencePositionViewService.getCorrectedChronologicalPositions(LICENCE_CORRECTION, THIRD_POSITION_ID))
        .thenReturn(positions);

    assertThat(licencePositionSpatialService
        .getBlockFeaturesGoingIntoChange(LICENCE_CORRECTION, THIRD_POSITION_ID, null))
        .isEmpty();
    verifyNoInteractions(featureService);
  }

  @Test
  void getBlockFeaturesGoingIntoChange_whenAnEndHasNoFeatures_thenTheHoldingIsUnchanged() {
    var positions = List.of(
        position(FIRST_POSITION_ID, 1, blockCreateOf(BLOCK_30_1.getId())),
        position(SECOND_POSITION_ID, 2, blockEndOf()),
        position(THIRD_POSITION_ID, 3)
    );

    when(licencePositionViewService.getCorrectedChronologicalPositions(LICENCE_CORRECTION, THIRD_POSITION_ID))
        .thenReturn(positions);
    when(featureService.getFeaturesByIds(Set.of(BLOCK_30_1.getId()))).thenReturn(List.of(BLOCK_30_1));

    assertThat(licencePositionSpatialService
        .getBlockFeaturesGoingIntoChange(LICENCE_CORRECTION, THIRD_POSITION_ID, null))
        .containsExactly(BLOCK_30_1);
  }

  @ParameterizedTest
  @MethodSource("nonSpatialOperations")
  void getBlockFeaturesGoingIntoChange_whenANonSpatialOperationPrecedesThePosition_thenTheHoldingIsUnchanged(
      LicenceOperation nonSpatialOperation
  ) {
    var positions = List.of(
        position(FIRST_POSITION_ID, 1, blockCreateOf(BLOCK_30_1.getId(), BLOCK_30_2.getId())),
        position(SECOND_POSITION_ID, 2, nonSpatialOperation),
        position(THIRD_POSITION_ID, 3)
    );

    when(licencePositionViewService.getCorrectedChronologicalPositions(LICENCE_CORRECTION, THIRD_POSITION_ID))
        .thenReturn(positions);
    when(featureService.getFeaturesByIds(Set.of(BLOCK_30_1.getId(), BLOCK_30_2.getId())))
        .thenReturn(List.of(BLOCK_30_1, BLOCK_30_2));

    assertThat(licencePositionSpatialService
        .getBlockFeaturesGoingIntoChange(LICENCE_CORRECTION, THIRD_POSITION_ID, null))
        .containsExactly(BLOCK_30_1, BLOCK_30_2);
  }

  @Test
  void getBlockFeaturesGoingIntoChange_whenBlocksAreCreatedRedefinedAndEndedAcrossPositions_thenSurvivorsRemain() {
    var positions = List.of(
        position(FIRST_POSITION_ID, 1, blockCreateOf(BLOCK_30_1.getId(), BLOCK_30_2.getId(), BLOCK_30_3.getId())),
        position(SECOND_POSITION_ID, 2, blockRedefinitionOf(Set.of(BLOCK_30_2.getId()), Set.of(BLOCK_30_4.getId()))),
        position(THIRD_POSITION_ID, 3, blockEndOf(BLOCK_30_1.getId())),
        position(FOURTH_POSITION_ID, 4)
    );

    when(licencePositionViewService.getCorrectedChronologicalPositions(LICENCE_CORRECTION, FOURTH_POSITION_ID))
        .thenReturn(positions);
    when(featureService.getFeaturesByIds(Set.of(BLOCK_30_3.getId(), BLOCK_30_4.getId())))
        .thenReturn(List.of(BLOCK_30_4, BLOCK_30_3));

    assertThat(licencePositionSpatialService
        .getBlockFeaturesGoingIntoChange(LICENCE_CORRECTION, FOURTH_POSITION_ID, null))
        .containsExactly(BLOCK_30_3, BLOCK_30_4);
  }

  @Test
  void getBlockFeaturesGoingIntoChange_whenThatSameLifecycleRunsAcrossChangesOnOnePosition_thenItFoldsTheSameWay() {
    var positions = List.of(
        positionWithChanges(FIRST_POSITION_ID, 1,
            change("create-change", 1, blockCreateOf(BLOCK_30_1.getId(), BLOCK_30_2.getId(), BLOCK_30_3.getId())),
            change("redefine-change", 2, blockRedefinitionOf(Set.of(BLOCK_30_2.getId()), Set.of(BLOCK_30_4.getId()))),
            change("end-change", 3, blockEndOf(BLOCK_30_1.getId()))
        ),
        position(SECOND_POSITION_ID, 2)
    );

    when(licencePositionViewService.getCorrectedChronologicalPositions(LICENCE_CORRECTION, SECOND_POSITION_ID))
        .thenReturn(positions);
    when(featureService.getFeaturesByIds(Set.of(BLOCK_30_3.getId(), BLOCK_30_4.getId())))
        .thenReturn(List.of(BLOCK_30_4, BLOCK_30_3));

    assertThat(licencePositionSpatialService
        .getBlockFeaturesGoingIntoChange(LICENCE_CORRECTION, SECOND_POSITION_ID, null))
        .containsExactly(BLOCK_30_3, BLOCK_30_4);
  }

  @Test
  void getBlockFeaturesGoingIntoChange_whenABlockIsCreatedThenEnded_thenItIsNotHeld() {
    var positions = List.of(
        positionWithChanges(FIRST_POSITION_ID, 1,
            change("create-change", 1, blockCreateOf(BLOCK_30_1.getId())),
            change("end-change", 2, blockEndOf(BLOCK_30_1.getId()))
        ),
        position(SECOND_POSITION_ID, 2)
    );

    when(licencePositionViewService.getCorrectedChronologicalPositions(LICENCE_CORRECTION, SECOND_POSITION_ID))
        .thenReturn(positions);

    assertThat(licencePositionSpatialService
        .getBlockFeaturesGoingIntoChange(LICENCE_CORRECTION, SECOND_POSITION_ID, null))
        .isEmpty();
  }

  @Test
  void getBlockFeaturesGoingIntoChange_whenABlockIsEndedThenCreated_thenItIsHeld() {
    var positions = List.of(
        positionWithChanges(FIRST_POSITION_ID, 1,
            change("end-change", 1, blockEndOf(BLOCK_30_1.getId())),
            change("create-change", 2, blockCreateOf(BLOCK_30_1.getId()))
        ),
        position(SECOND_POSITION_ID, 2)
    );

    when(licencePositionViewService.getCorrectedChronologicalPositions(LICENCE_CORRECTION, SECOND_POSITION_ID))
        .thenReturn(positions);
    when(featureService.getFeaturesByIds(Set.of(BLOCK_30_1.getId()))).thenReturn(List.of(BLOCK_30_1));

    assertThat(licencePositionSpatialService
        .getBlockFeaturesGoingIntoChange(LICENCE_CORRECTION, SECOND_POSITION_ID, null))
        .containsExactly(BLOCK_30_1);
  }

  @Test
  void getBlockFeaturesGoingIntoChange_whenOneChangeCarriesACreateAndAnEnd_thenTheyApplyInOperationOrder() {
    var positions = List.of(
        position(FIRST_POSITION_ID, 1,
            blockCreateOf(BLOCK_30_1.getId(), BLOCK_30_2.getId()),
            blockEndOf(BLOCK_30_1.getId())
        ),
        position(SECOND_POSITION_ID, 2)
    );

    when(licencePositionViewService.getCorrectedChronologicalPositions(LICENCE_CORRECTION, SECOND_POSITION_ID))
        .thenReturn(positions);
    when(featureService.getFeaturesByIds(Set.of(BLOCK_30_2.getId()))).thenReturn(List.of(BLOCK_30_2));

    assertThat(licencePositionSpatialService
        .getBlockFeaturesGoingIntoChange(LICENCE_CORRECTION, SECOND_POSITION_ID, null))
        .containsExactly(BLOCK_30_2);
  }

  @Test
  void getBlockFeaturesGoingIntoChange_whenTheTargetChangeFollowsABlockCreate_thenTheCreatedBlocksAreInTheInput() {
    var positions = List.of(
        positionWithChanges(FIRST_POSITION_ID, 1,
            change("create-change", 1, blockCreateOf(BLOCK_30_1.getId(), BLOCK_30_2.getId())),
            change("end-change", 2, blockEndOf(BLOCK_30_1.getId()))
        )
    );

    when(licencePositionViewService.getCorrectedChronologicalPositions(LICENCE_CORRECTION, FIRST_POSITION_ID))
        .thenReturn(positions);
    when(featureService.getFeaturesByIds(Set.of(BLOCK_30_1.getId(), BLOCK_30_2.getId())))
        .thenReturn(List.of(BLOCK_30_1, BLOCK_30_2));

    assertThat(licencePositionSpatialService
        .getBlockFeaturesGoingIntoChange(LICENCE_CORRECTION, FIRST_POSITION_ID, "end-change"))
        .containsExactly(BLOCK_30_1, BLOCK_30_2);
  }

  @Test
  void getBlockFeaturesGoingIntoChange_whenABlockCreateCarriesSubareas_thenOnlyTheCreatedBlocksAreHeld() {
    var blockCreate = LicenceOperation.newBlockCreateOperation()
        .withFeatureIds(List.of(BLOCK_30_1.getId()))
        .withCreatedSubareas(Map.of(BLOCK_30_1.getId(), List.of(new SubareaDetails(SUBAREA.getId(), "Subarea A", "A"))))
        .build();
    var positions = List.of(
        position(FIRST_POSITION_ID, 1, blockCreate),
        position(SECOND_POSITION_ID, 2)
    );

    when(licencePositionViewService.getCorrectedChronologicalPositions(LICENCE_CORRECTION, SECOND_POSITION_ID))
        .thenReturn(positions);
    when(featureService.getFeaturesByIds(Set.of(BLOCK_30_1.getId()))).thenReturn(List.of(BLOCK_30_1));

    assertThat(licencePositionSpatialService
        .getBlockFeaturesGoingIntoChange(LICENCE_CORRECTION, SECOND_POSITION_ID, null))
        .containsExactly(BLOCK_30_1);
  }

  @Test
  void getBlockFeaturesGoingIntoChange_whenABlockCreateIsStagedForRemoval_thenTheCreatedBlocksAreNotHeld() {
    var positions = List.of(
        position(FIRST_POSITION_ID, 1, blockCreateOf(BLOCK_30_1.getId())),
        positionWithChanges(SECOND_POSITION_ID, 2, removedChange(blockCreateOf(BLOCK_30_2.getId()))),
        position(THIRD_POSITION_ID, 3)
    );

    when(licencePositionViewService.getCorrectedChronologicalPositions(LICENCE_CORRECTION, THIRD_POSITION_ID))
        .thenReturn(positions);
    when(featureService.getFeaturesByIds(Set.of(BLOCK_30_1.getId()))).thenReturn(List.of(BLOCK_30_1));

    assertThat(licencePositionSpatialService
        .getBlockFeaturesGoingIntoChange(LICENCE_CORRECTION, THIRD_POSITION_ID, null))
        .containsExactly(BLOCK_30_1);
  }

  @Test
  void getBlockFeaturesGoingIntoChange_whenABlockEndIsStagedForRemoval_thenTheEndedBlocksSurvive() {
    var positions = List.of(
        position(FIRST_POSITION_ID, 1, blockCreateOf(BLOCK_30_1.getId(), BLOCK_30_2.getId())),
        positionWithChanges(SECOND_POSITION_ID, 2, removedChange(blockEndOf(BLOCK_30_1.getId()))),
        position(THIRD_POSITION_ID, 3)
    );

    when(licencePositionViewService.getCorrectedChronologicalPositions(LICENCE_CORRECTION, THIRD_POSITION_ID))
        .thenReturn(positions);
    when(featureService.getFeaturesByIds(Set.of(BLOCK_30_1.getId(), BLOCK_30_2.getId())))
        .thenReturn(List.of(BLOCK_30_1, BLOCK_30_2));

    assertThat(licencePositionSpatialService
        .getBlockFeaturesGoingIntoChange(LICENCE_CORRECTION, THIRD_POSITION_ID, null))
        .containsExactly(BLOCK_30_1, BLOCK_30_2);
  }

  @Test
  void getBlockFeaturesGoingIntoChange_whenABlockIsPartiallySurrendered_thenItsRetainedPartsReplaceIt() {
    var surrenderDetails = new SurrenderDetails(
        BlockSurrenderType.PARTIAL_SURRENDER,
        null,
        List.of(UUID.randomUUID()),
        List.of(BLOCK_30_3.getId())
    );
    var positions = List.of(
        position(FIRST_POSITION_ID, 1, blockCreateOf(BLOCK_30_1.getId(), BLOCK_30_2.getId())),
        position(SECOND_POSITION_ID, 2, surrenderOf(BLOCK_30_1.getId(), surrenderDetails)),
        position(THIRD_POSITION_ID, 3)
    );

    when(licencePositionViewService.getCorrectedChronologicalPositions(LICENCE_CORRECTION, THIRD_POSITION_ID))
        .thenReturn(positions);
    when(featureService.getFeaturesByIds(Set.of(BLOCK_30_2.getId(), BLOCK_30_3.getId())))
        .thenReturn(List.of(BLOCK_30_2, BLOCK_30_3));

    assertThat(licencePositionSpatialService
        .getBlockFeaturesGoingIntoChange(LICENCE_CORRECTION, THIRD_POSITION_ID, null))
        .containsExactly(BLOCK_30_2, BLOCK_30_3);
  }

  @Test
  void getBlockFeaturesGoingIntoChange_whenBlocksFromDifferentQuadrantsAreCreated_thenTheyAreInBlockOrder() {
    var positions = List.of(
        position(FIRST_POSITION_ID, 1, blockCreateOf(BLOCK_30_2.getId(), BLOCK_29_10.getId())),
        position(SECOND_POSITION_ID, 2)
    );

    when(licencePositionViewService.getCorrectedChronologicalPositions(LICENCE_CORRECTION, SECOND_POSITION_ID))
        .thenReturn(positions);
    when(featureService.getFeaturesByIds(Set.of(BLOCK_30_2.getId(), BLOCK_29_10.getId())))
        .thenReturn(List.of(BLOCK_30_2, BLOCK_29_10));

    assertThat(licencePositionSpatialService
        .getBlockFeaturesGoingIntoChange(LICENCE_CORRECTION, SECOND_POSITION_ID, null))
        .containsExactly(BLOCK_29_10, BLOCK_30_2);
  }

  @Test
  void getBlockFeaturesGoingIntoChange_whenACreateCarriesANonBlockFeature_thenItIsExcludedFromTheResult() {
    var positions = List.of(
        position(FIRST_POSITION_ID, 1, blockCreateOf(BLOCK_30_1.getId(), SUBAREA.getId())),
        position(SECOND_POSITION_ID, 2)
    );

    when(licencePositionViewService.getCorrectedChronologicalPositions(LICENCE_CORRECTION, SECOND_POSITION_ID))
        .thenReturn(positions);
    when(featureService.getFeaturesByIds(Set.of(BLOCK_30_1.getId(), SUBAREA.getId())))
        .thenReturn(List.of(BLOCK_30_1, SUBAREA));

    assertThat(licencePositionSpatialService
        .getBlockFeaturesGoingIntoChange(LICENCE_CORRECTION, SECOND_POSITION_ID, null))
        .containsExactly(BLOCK_30_1);
  }

  @Test
  void getBlockFeaturesGoingIntoChange_whenAnAddedPositionCorrectionCarriesBlockOperations_thenTheyAreApplied() {
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
            position(FIRST_POSITION_ID, 1, blockCreateOf(BLOCK_30_1.getId(), BLOCK_30_2.getId())),
            position(SECOND_POSITION_ID, 2, blockEndOf(BLOCK_30_1.getId())),
            ChronologicalPositionTestUtil.newBuilder()
                .withId(addedPositionId)
                .withDate(LocalDate.of(2026, Month.JANUARY, 3))
                .build()
        ));
    when(featureService.getFeaturesByIds(Set.of(BLOCK_30_2.getId()))).thenReturn(List.of(BLOCK_30_2));

    assertThat(licencePositionSpatialService.getBlockFeaturesGoingIntoChange(positionCorrection, null))
        .containsExactly(BLOCK_30_2);
  }

  @ParameterizedTest
  @MethodSource("subareaOperations")
  void getBlockFeaturesGoingIntoChange_whenASubareaOperationIsOnABlockNotHeld_thenNoBlockIsHeld(
      LicenceOperation subareaOperation
  ) {
    givenPositions(
        position(FIRST_POSITION_ID, 1, subareaOperation),
        position(SECOND_POSITION_ID, 2)
    );

    var result = licencePositionSpatialService
        .getBlockFeaturesGoingIntoChange(LICENCE_CORRECTION, SECOND_POSITION_ID, null);

    assertThat(result).isEmpty();
    verifyNoInteractions(featureService);
  }

  @Test
  void getSubareasGoingIntoChange_whenSubareasCreatedOnTheBlock_thenReturned() {
    givenPositions(position(
        FIRST_POSITION_ID,
        1,
        blockCreateOf(BLOCK_30_1.getId(), BLOCK_30_2.getId()),
        subareaCreateOn(BLOCK_30_1.getId(), SUBAREA_A, SUBAREA_B),
        subareaCreateOn(BLOCK_30_2.getId(), SUBAREA_C)
    ));

    var result = licencePositionSpatialService
        .getSubareasGoingIntoChange(LICENCE_CORRECTION, FIRST_POSITION_ID, BLOCK_30_1.getId(), null);

    assertThat(result).containsExactly(SUBAREA_A, SUBAREA_B);
    verifyNoInteractions(featureService);
  }

  @Test
  void getSubareasGoingIntoChange_whenSubareasCreatedOnABlockNotHeld_thenNotReturned() {
    givenPositions(position(FIRST_POSITION_ID, 1, subareaCreateOn(UNHELD_BLOCK_ID, SUBAREA_A)));

    var result = licencePositionSpatialService
        .getSubareasGoingIntoChange(LICENCE_CORRECTION, FIRST_POSITION_ID, UNHELD_BLOCK_ID, null);

    assertThat(result).isEmpty();
  }

  @Test
  void getSubareasGoingIntoChange_whenSubareaEnded_thenNoLongerReturned() {
    givenPositions(
        position(
            FIRST_POSITION_ID,
            1,
            blockCreateOf(BLOCK_30_1.getId()),
            subareaCreateOn(BLOCK_30_1.getId(), SUBAREA_A, SUBAREA_B)
        ),
        position(SECOND_POSITION_ID, 2, subareaEndOn(BLOCK_30_1.getId(), SUBAREA_A))
    );

    var result = licencePositionSpatialService
        .getSubareasGoingIntoChange(LICENCE_CORRECTION, SECOND_POSITION_ID, BLOCK_30_1.getId(), null);

    assertThat(result).containsExactly(SUBAREA_B);
  }

  @Test
  void getSubareasGoingIntoChange_whenSubareaChange_thenOutputsReplaceTheReplacedSubareas() {
    var subareaChange = LicenceOperation.newSubAreaOperation()
        .withBlockFeatureId(BLOCK_30_1.getId())
        .withReplacedSubareas(List.of(SUBAREA_A))
        .withOutputSubareas(List.of(SUBAREA_C))
        .build();
    givenPositions(
        position(
            FIRST_POSITION_ID,
            1,
            blockCreateOf(BLOCK_30_1.getId()),
            subareaCreateOn(BLOCK_30_1.getId(), SUBAREA_A, SUBAREA_B)
        ),
        position(SECOND_POSITION_ID, 2, subareaChange)
    );

    var result = licencePositionSpatialService
        .getSubareasGoingIntoChange(LICENCE_CORRECTION, SECOND_POSITION_ID, BLOCK_30_1.getId(), null);

    assertThat(result).containsExactly(SUBAREA_B, SUBAREA_C);
  }

  @Test
  void getSubareasGoingIntoChange_whenBlocksCreatedWithSubareas_thenSubareasMatchedToTheirBlock() {
    var blockCreate = LicenceOperation.newBlockCreateOperation()
        .withFeatureIds(List.of(BLOCK_30_1.getId(), BLOCK_30_2.getId()))
        .withCreatedSubareas(Map.of(
            BLOCK_30_1.getId(), List.of(SUBAREA_A, SUBAREA_B),
            BLOCK_30_2.getId(), List.of(SUBAREA_C)
        ))
        .build();
    givenPositions(position(FIRST_POSITION_ID, 1, blockCreate));

    var result = licencePositionSpatialService
        .getSubareasGoingIntoChange(LICENCE_CORRECTION, FIRST_POSITION_ID, BLOCK_30_1.getId(), null);

    assertThat(result).containsExactly(SUBAREA_A, SUBAREA_B);
    verifyNoInteractions(featureService);
  }

  @Test
  void getSubareasGoingIntoChange_whenBlockRedefined_thenOutputSubareasHeldBySuccessorBlock() {
    var blockRedefinition = LicenceOperation.newBlockRedefinitionOperation()
        .withReplacedFeatureIds(List.of(BLOCK_30_1.getId()))
        .withOutputFeatureIds(List.of(BLOCK_30_2.getId()))
        .withReplacedSubareas(List.of(SUBAREA_A))
        .withOutputSubareas(Map.of(BLOCK_30_2.getId(), List.of(SUBAREA_C)))
        .build();
    givenPositions(
        position(FIRST_POSITION_ID, 1, blockCreateOf(BLOCK_30_1.getId()), subareaCreateOn(BLOCK_30_1.getId(), SUBAREA_A)),
        position(SECOND_POSITION_ID, 2, blockRedefinition)
    );

    var replacedBlockSubareas = licencePositionSpatialService
        .getSubareasGoingIntoChange(LICENCE_CORRECTION, SECOND_POSITION_ID, BLOCK_30_1.getId(), null);
    var successorBlockSubareas = licencePositionSpatialService
        .getSubareasGoingIntoChange(LICENCE_CORRECTION, SECOND_POSITION_ID, BLOCK_30_2.getId(), null);

    assertThat(replacedBlockSubareas).isEmpty();
    assertThat(successorBlockSubareas).containsExactly(SUBAREA_C);
  }

  @Test
  void getSubareasGoingIntoChange_whenBlockEnded_thenNoSubareas() {
    givenPositions(
        position(FIRST_POSITION_ID, 1, blockCreateOf(BLOCK_30_1.getId()), subareaCreateOn(BLOCK_30_1.getId(), SUBAREA_A)),
        position(SECOND_POSITION_ID, 2, blockEndOf(BLOCK_30_1.getId()))
    );

    var result = licencePositionSpatialService
        .getSubareasGoingIntoChange(LICENCE_CORRECTION, SECOND_POSITION_ID, BLOCK_30_1.getId(), null);

    assertThat(result).isEmpty();
  }

  @Test
  void getSubareasGoingIntoChange_whenBlockFullySurrendered_thenNoSubareas() {
    var surrender = surrenderOf(
        BLOCK_30_1.getId(),
        new SurrenderDetails(BlockSurrenderType.FULL_SURRENDER, null, List.of(BLOCK_30_1.getId()))
    );
    givenPositions(
        position(
            FIRST_POSITION_ID,
            1,
            blockCreateOf(BLOCK_30_1.getId(), BLOCK_30_2.getId()),
            subareaCreateOn(BLOCK_30_1.getId(), SUBAREA_A)
        ),
        position(SECOND_POSITION_ID, 2, surrender),
        position(THIRD_POSITION_ID, 3)
    );

    var result = licencePositionSpatialService
        .getSubareasGoingIntoChange(LICENCE_CORRECTION, THIRD_POSITION_ID, BLOCK_30_1.getId(), null);

    assertThat(result).isEmpty();
  }

  @Test
  void getSubareasGoingIntoChange_whenSurrenderNotTakenEffect_thenSubareasUnchanged() {
    givenPositions(
        position(FIRST_POSITION_ID, 1, blockCreateOf(BLOCK_30_1.getId()), subareaCreateOn(BLOCK_30_1.getId(), SUBAREA_A)),
        position(SECOND_POSITION_ID, 2, incompleteSurrenderOf(BLOCK_30_1.getId())),
        position(THIRD_POSITION_ID, 3)
    );

    var result = licencePositionSpatialService
        .getSubareasGoingIntoChange(LICENCE_CORRECTION, THIRD_POSITION_ID, BLOCK_30_1.getId(), null);

    assertThat(result).containsExactly(SUBAREA_A);
  }

  @Test
  void getSubareasGoingIntoChange_whenChangeNamed_thenOnlyChangesBeforeItApplied() {
    givenPositions(positionWithChanges(
        FIRST_POSITION_ID,
        1,
        change("first-change", 1, blockCreateOf(BLOCK_30_1.getId()), subareaCreateOn(BLOCK_30_1.getId(), SUBAREA_A)),
        change("second-change", 2, subareaEndOn(BLOCK_30_1.getId(), SUBAREA_A))
    ));

    var result = licencePositionSpatialService
        .getSubareasGoingIntoChange(LICENCE_CORRECTION, FIRST_POSITION_ID, BLOCK_30_1.getId(), "second-change");

    assertThat(result).containsExactly(SUBAREA_A);
  }

  @Test
  void getSubareasGoingIntoChange_whenChangeRemoved_thenItIsNotApplied() {
    givenPositions(positionWithChanges(
        FIRST_POSITION_ID,
        1,
        change("first-change", 1, blockCreateOf(BLOCK_30_1.getId()), subareaCreateOn(BLOCK_30_1.getId(), SUBAREA_A)),
        removedChange(subareaEndOn(BLOCK_30_1.getId(), SUBAREA_A))
    ));

    var result = licencePositionSpatialService
        .getSubareasGoingIntoChange(LICENCE_CORRECTION, FIRST_POSITION_ID, BLOCK_30_1.getId(), null);

    assertThat(result).containsExactly(SUBAREA_A);
  }

  @ParameterizedTest
  @MethodSource("nonSpatialOperations")
  void getSubareasGoingIntoChange_whenNonSpatialOperation_thenSubareasUnchanged(LicenceOperation operation) {
    givenPositions(
        position(FIRST_POSITION_ID, 1, blockCreateOf(BLOCK_30_1.getId()), subareaCreateOn(BLOCK_30_1.getId(), SUBAREA_A)),
        position(SECOND_POSITION_ID, 2, operation)
    );

    var result = licencePositionSpatialService
        .getSubareasGoingIntoChange(LICENCE_CORRECTION, SECOND_POSITION_ID, BLOCK_30_1.getId(), null);

    assertThat(result).containsExactly(SUBAREA_A);
  }

  @Test
  void getSubareasGoingIntoChange_whenPositionCorrection_thenResolvedForItsPosition() {
    var targetPosition = LicencePositionTestUtil.newBuilder().withId(FIRST_POSITION_ID).withLicence(LICENCE).build();
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withLicenceCorrection(LICENCE_CORRECTION)
        .withTargetLicencePosition(targetPosition)
        .withPayload(UpdateLicencePositionPayloadTestUtil.newBuilder().build())
        .build();
    givenPositions(positionWithChanges(
        FIRST_POSITION_ID,
        1,
        change("first-change", 1, blockCreateOf(BLOCK_30_1.getId()), subareaCreateOn(BLOCK_30_1.getId(), SUBAREA_A)),
        change("second-change", 2, subareaCreateOn(BLOCK_30_1.getId(), SUBAREA_B))
    ));

    var result = licencePositionSpatialService
        .getSubareasGoingIntoChange(positionCorrection, BLOCK_30_1.getId(), "second-change");

    assertThat(result).containsExactly(SUBAREA_A);
  }

  private void givenPositions(ChronologicalPosition... positions) {
    var positionList = List.of(positions);
    when(licencePositionViewService.getCorrectedChronologicalPositions(LICENCE_CORRECTION, positionList.getLast().id()))
        .thenReturn(positionList);
  }

  private static LicenceOperation subareaCreateOn(
      UUID blockFeatureId,
      SubareaDetails... subareas
  ) {
    return LicenceOperation.newSubareaCreateOperation()
        .withBlockFeatureId(blockFeatureId)
        .withCreatedSubareas(List.of(subareas))
        .build();
  }

  private static LicenceOperation subareaEndOn(
      UUID blockFeatureId,
      SubareaDetails... subareas
  ) {
    return LicenceOperation.newSubareaEndOperation()
        .withBlockFeatureId(blockFeatureId)
        .withEndedSubareas(List.of(subareas))
        .build();
  }

  private static LicenceOperation surrenderOf(
      UUID blockFeatureId,
      SurrenderDetails surrenderDetails
  ) {
    return LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(blockFeatureId))
        .withSurrenderDetails(Map.of(blockFeatureId, surrenderDetails))
        .build();
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

  private static ChronologicalPosition positionWithChanges(UUID positionId, int dayOfMonth, PositionChange... changes) {
    return ChronologicalPositionTestUtil.newBuilder()
        .withId(positionId)
        .withDate(LocalDate.of(2026, Month.JANUARY, dayOfMonth))
        .withChanges(List.of(changes))
        .build();
  }

  private static PositionChange change(String changeId, int changeOrder, LicenceOperation... operations) {
    return PositionChangeTestUtil.newBuilder()
        .withChangeId(changeId)
        .withChangeOrder(changeOrder)
        .withOperations(List.of(operations))
        .build();
  }

  private static PositionChange removedChange(LicenceOperation... operations) {
    return PositionChangeTestUtil.newBuilder()
        .withChangeType(LicencePositionChangeType.REMOVE_CHANGE)
        .withOperations(List.of(operations))
        .build();
  }

  private static LicenceOperation fullSurrenderOf(UUID... blockFeatureIds) {
    return LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(blockFeatureIds))
        .withSurrenderDetails(Stream.of(blockFeatureIds).collect(Collectors.toMap(
            Function.identity(),
            blockFeatureId -> new SurrenderDetails(BlockSurrenderType.FULL_SURRENDER, null, List.of(blockFeatureId))
        )))
        .build();
  }

  private static LicenceOperation incompleteSurrenderOf(UUID blockFeatureId) {
    return surrenderOf(
        blockFeatureId,
        new SurrenderDetails(BlockSurrenderType.PARTIAL_SURRENDER, UUID.randomUUID(), List.of(UUID.randomUUID()))
    );
  }

  private static LicenceOperation subareaChangeFor(UUID blockFeatureId) {
    return LicenceOperation.newSubAreaOperation()
        .withBlockFeatureId(blockFeatureId)
        .build();
  }

  private static Stream<LicenceOperation> subareaOperations() {
    var subarea = new SubareaDetails(SUBAREA.getId(), "Subarea A", "A");
    return Stream.of(
        LicenceOperation.newSubAreaOperation()
            .withBlockFeatureId(BLOCK_30_1.getId())
            .withOutputSubareas(List.of(subarea))
            .build(),
        LicenceOperation.newSubareaCreateOperation()
            .withBlockFeatureId(BLOCK_30_1.getId())
            .withCreatedSubareas(List.of(subarea))
            .build(),
        LicenceOperation.newSubareaEndOperation()
            .withBlockFeatureId(BLOCK_30_1.getId())
            .withEndedSubareas(List.of(subarea))
            .build()
    );
  }

  private static LicenceOperation blockCreateOf(UUID... featureIds) {
    return LicenceOperation.newBlockCreateOperation()
        .withFeatureIds(List.of(featureIds))
        .build();
  }

  private static LicenceOperation blockEndOf(UUID... endedFeatureIds) {
    return LicenceOperation.newBlockEndOperation()
        .withEndedFeatureIds(List.of(endedFeatureIds))
        .build();
  }

  private static LicenceOperation blockRedefinitionOf(Set<UUID> replacedFeatureIds, Set<UUID> outputFeatureIds) {
    return LicenceOperation.newBlockRedefinitionOperation()
        .withReplacedFeatureIds(replacedFeatureIds)
        .withOutputFeatureIds(outputFeatureIds)
        .build();
  }

  private static Stream<LicenceOperation> nonSpatialOperations() {
    return Stream.of(
        LicenceOperation.newAdministratorChange().withOperator(1).build(),
        LicenceOperation.newSetEquityOperation().withTransferTo(1).withEquity(new BigDecimal("100")).build(),
        LicenceOperation.newTransferEquityOperation()
            .withTransferFrom(1)
            .withTransferTo(2)
            .withEquity(new BigDecimal("10"))
            .build()
    );
  }
}
