package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.list;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.gisframework.command.CommandJourney;
import uk.co.fivium.gisframework.command.CommandJourneyService;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.fivium.gisframework.feature.FeatureService;
import uk.co.nstauthority.licensingmanagementservice.exception.LmsEntityNotFoundException;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.blocksurrendertype.BlockSurrenderType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.AddChange;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.UpdateChangeOperations;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.CreateLicencePositionPayloadTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.LicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.UpdateLicencePositionPayloadTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.PartialSurrenderOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionViewService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.PositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.PartialSurrenderChangeView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.feature.FeatureTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.spatial.LicencePositionSpatialService;

@ExtendWith(MockitoExtension.class)
class PartialSurrenderCorrectionServiceTest {

  private static final Licence LICENCE = LicenceTestUtil.builder().build();
  private static final LicenceCorrection LICENCE_CORRECTION = LicenceCorrectionTestUtil.newBuilder()
      .withLicence(LICENCE)
      .build();
  private static final LicencePosition LICENCE_POSITION = LicencePositionTestUtil.newBuilder()
      .withLicence(LICENCE)
      .build();
  private static final UUID FIRST_FEATURE_ID = UUID.randomUUID();
  private static final UUID SECOND_FEATURE_ID = UUID.randomUUID();
  private static final UUID THIRD_FEATURE_ID = UUID.randomUUID();
  private static final UUID FIRST_COMMAND_JOURNEY_ID = UUID.randomUUID();
  private static final UUID SECOND_COMMAND_JOURNEY_ID = UUID.randomUUID();
  private static final String LIVE_CHANGE_ID = UUID.randomUUID().toString();
  private static final LicencePosition LATER_POSITION = LicencePositionTestUtil.newBuilder()
      .withLicence(LICENCE)
      .build();
  private static final String LATER_LIVE_CHANGE_ID = UUID.randomUUID().toString();
  private static final int ADMINISTRATOR_ID = 55;

  @Mock
  private LicencePositionCorrectionService licencePositionCorrectionService;

  @Mock
  private LicencePositionSpatialService licencePositionSpatialService;

  @Mock
  private LicencePositionChangeService licencePositionChangeService;

  @Mock
  private LicencePositionViewService licencePositionViewService;

  @Mock
  private FeatureService featureService;

  @Mock
  private CommandJourneyService commandJourneyService;

  @InjectMocks
  private PartialSurrenderCorrectionService partialSurrenderCorrectionService;

  @Captor
  private ArgumentCaptor<List<PartialSurrenderOperation>> partialSurrenderOperationCaptor;

  private static PartialSurrenderOperation.SurrenderDetails surrenderDetails(
      BlockSurrenderType type,
      UUID commandJourneyId
  ) {
    return new PartialSurrenderOperation.SurrenderDetails(type, commandJourneyId, List.of());
  }

  private static LicencePositionCorrection positionCorrection() {
    return positionCorrection(List.of());
  }

  private static LicencePositionCorrection positionCorrection(List<LicencePositionChangeType> changes) {
    return LicencePositionCorrectionTestUtil.newBuilder()
        .withTargetLicencePosition(null)
        .withPayload(CreateLicencePositionPayloadTestUtil.newBuilder().withChanges(changes).build())
        .build();
  }

  private static PartialSurrenderOperation partialSurrender(UUID... featureIds) {
    return LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(featureIds))
        .build();
  }

  private void givenCommittedPartialSurrender(
      LicencePositionCorrection positionCorrection,
      PartialSurrenderOperation operation
  ) {
    when(licencePositionCorrectionService.getCommittedChangeOfType(positionCorrection, PartialSurrenderOperation.class))
        .thenReturn(Optional.of(operation));
  }

  private void givenPositionCorrection(LicencePositionCorrection positionCorrection) {
    when(licencePositionCorrectionService.getOrBuildUpdatePositionCorrection(LICENCE_CORRECTION, LICENCE_POSITION))
        .thenReturn(positionCorrection);
    when(licencePositionCorrectionService.save(positionCorrection)).thenReturn(positionCorrection);
  }

  private static LicencePositionCorrection updatePositionCorrection(List<LicencePositionChangeType> changes) {
    return LicencePositionCorrectionTestUtil.newBuilder()
        .withChangeType(LicencePositionCorrectionChangeType.UPDATE_POSITION)
        .withTargetLicencePosition(LICENCE_POSITION)
        .withPayload(LicencePositionPayload.newUpdateLicencePositionPayload().withChanges(changes).build())
        .build();
  }

  private static LicencePositionCorrection executedPositionCorrection() {
    return executedPositionCorrection(List.of());
  }

  private static LicencePositionCorrection executedPositionCorrection(List<LicencePositionChangeType> changes) {
    return LicencePositionCorrectionTestUtil.newBuilder()
        .withLicenceCorrection(LICENCE_CORRECTION)
        .withTargetLicencePosition(LICENCE_POSITION)
        .withPayload(UpdateLicencePositionPayloadTestUtil.newBuilder().withChanges(changes).build())
        .build();
  }

  @Test
  void getCommittedPartialSurrender_whenNoPartialSurrenderChange_returnsEmpty() {
    assertThat(partialSurrenderCorrectionService.getCommittedPartialSurrender(positionCorrection())).isEmpty();
  }

  @Test
  void getCommittedPartialSurrender_whenPartialSurrenderChange_returnsTheOperation() {
    var operation = partialSurrender(FIRST_FEATURE_ID, SECOND_FEATURE_ID);
    var positionCorrection = positionCorrection(
        List.of(AddChange.buildOperationsChange(List.of(operation), 1)));
    givenCommittedPartialSurrender(positionCorrection, operation);

    assertThat(partialSurrenderCorrectionService.getCommittedPartialSurrender(positionCorrection))
        .contains(operation);
  }

  @Test
  void getCommittedPartialSurrender_whenNoPositionCorrection_returnsEmpty() {
    assertThat(partialSurrenderCorrectionService.getCommittedPartialSurrender(null)).isEmpty();
  }

  @Test
  void getCommittedPartialSurrenderChangeId_whenSurrenderStaged_returnsTheStagedChangeId() {
    var change = AddChange.buildOperationsChange(List.of(partialSurrender(FIRST_FEATURE_ID)), 1);
    var positionCorrection = positionCorrection(List.of(change));

    assertThat(partialSurrenderCorrectionService.getCommittedPartialSurrenderChangeId(positionCorrection))
        .contains(change.changeId());
  }

  @Test
  void getCommittedPartialSurrenderChangeId_whenNoPositionCorrection_returnsEmpty() {
    assertThat(partialSurrenderCorrectionService.getCommittedPartialSurrenderChangeId(null)).isEmpty();
  }

  @Test
  void getCommittedPartialSurrenderChangeId_whenNoSurrenderStaged_returnsEmpty() {
    assertThat(partialSurrenderCorrectionService.getCommittedPartialSurrenderChangeId(positionCorrection())).isEmpty();
  }

  @Test
  void getCommittedPartialSurrenderOrThrow_whenStaged_returnsTheOperation() {
    var operation = partialSurrender(FIRST_FEATURE_ID);
    var positionCorrection = positionCorrection(
        List.of(AddChange.buildOperationsChange(List.of(operation), 1)));
    givenCommittedPartialSurrender(positionCorrection, operation);

    assertThat(partialSurrenderCorrectionService.getCommittedPartialSurrenderOrThrow(positionCorrection))
        .isEqualTo(operation);
  }

  @Test
  void getCommittedPartialSurrenderOrThrow_whenNotStaged_throws() {
    var positionCorrection = positionCorrection();

    assertThatThrownBy(() ->
        partialSurrenderCorrectionService.getCommittedPartialSurrenderOrThrow(positionCorrection))
        .isInstanceOf(LmsEntityNotFoundException.class)
        .hasMessageContaining(positionCorrection.getId().toString());
  }

  @Test
  void allSurrenderedBlocksAreFull_whenEveryBlockIsFullSurrender_returnsTrue() {
    var operation = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID, SECOND_FEATURE_ID))
        .withSurrenderDetails(Map.of(
            FIRST_FEATURE_ID, surrenderDetails(BlockSurrenderType.FULL_SURRENDER, FIRST_COMMAND_JOURNEY_ID),
            SECOND_FEATURE_ID, surrenderDetails(BlockSurrenderType.FULL_SURRENDER, SECOND_COMMAND_JOURNEY_ID)))
        .build();
    var positionCorrection = positionCorrection(
        List.of(AddChange.buildOperationsChange(List.of(operation), 1)));
    givenCommittedPartialSurrender(positionCorrection, operation);

    assertThat(partialSurrenderCorrectionService.allSurrenderedBlocksAreFull(positionCorrection)).isTrue();
  }

  @Test
  void allSurrenderedBlocksAreFull_whenAnyBlockIsPartialSurrender_returnsFalse() {
    var operation = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID, SECOND_FEATURE_ID))
        .withSurrenderDetails(Map.of(
            FIRST_FEATURE_ID, surrenderDetails(BlockSurrenderType.FULL_SURRENDER, FIRST_COMMAND_JOURNEY_ID),
            SECOND_FEATURE_ID, surrenderDetails(BlockSurrenderType.PARTIAL_SURRENDER, SECOND_COMMAND_JOURNEY_ID)))
        .build();
    var positionCorrection = positionCorrection(
        List.of(AddChange.buildOperationsChange(List.of(operation), 1)));
    givenCommittedPartialSurrender(positionCorrection, operation);

    assertThat(partialSurrenderCorrectionService.allSurrenderedBlocksAreFull(positionCorrection)).isFalse();
  }

  @Test
  void allSurrenderedBlocksAreFull_whenOperationHasOnlyFullSurrenders_returnsTrue() {
    var operation = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID))
        .withSurrenderDetails(Map.of(FIRST_FEATURE_ID, surrenderDetails(BlockSurrenderType.FULL_SURRENDER, FIRST_COMMAND_JOURNEY_ID)))
        .build();

    assertThat(partialSurrenderCorrectionService.allSurrenderedBlocksAreFull(operation)).isTrue();
  }

  @Test
  void allSurrenderedBlocksAreFull_whenOperationHasAPartialSurrender_returnsFalse() {
    var operation = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID, SECOND_FEATURE_ID))
        .withSurrenderDetails(Map.of(
            FIRST_FEATURE_ID, surrenderDetails(BlockSurrenderType.FULL_SURRENDER, FIRST_COMMAND_JOURNEY_ID),
            SECOND_FEATURE_ID, surrenderDetails(BlockSurrenderType.PARTIAL_SURRENDER, SECOND_COMMAND_JOURNEY_ID)))
        .build();

    assertThat(partialSurrenderCorrectionService.allSurrenderedBlocksAreFull(operation)).isFalse();
  }

  @Test
  void allSurrenderedBlocksAreFull_whenNoSurrenderStaged_returnsFalse() {
    var positionCorrection = positionCorrection();

    assertThat(partialSurrenderCorrectionService.allSurrenderedBlocksAreFull(positionCorrection)).isFalse();
  }

  @Test
  void commitPartialSurrender_whenFeatureIds_replacesAddChangeWithTheOperation() {
    var positionCorrection = positionCorrection();
    var operation = partialSurrender(FIRST_FEATURE_ID);
    when(licencePositionCorrectionService.replaceAddChangeFor(
        positionCorrection, PartialSurrenderOperation.class, List.of(operation)))
        .thenReturn(positionCorrection);

    assertThat(partialSurrenderCorrectionService.commitPartialSurrender(positionCorrection, operation))
        .isEqualTo(positionCorrection);
  }

  @Test
  void commitPartialSurrender_whenNothingStaged_thenOutputsAreAnchoredOnTheWholePosition() {
    var positionCorrection = positionCorrection();
    var operation = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID))
        .withSurrenderDetails(Map.of(
            FIRST_FEATURE_ID, surrenderDetails(BlockSurrenderType.FULL_SURRENDER, FIRST_COMMAND_JOURNEY_ID)))
        .build();
    when(licencePositionSpatialService.getBlockFeaturesGoingIntoChange(positionCorrection, null)).thenReturn(List.of(
        FeatureTestUtil.blockFeature(FIRST_FEATURE_ID, "30", 1),
        FeatureTestUtil.blockFeature(SECOND_FEATURE_ID, "30", 2)));

    partialSurrenderCorrectionService.commitPartialSurrender(positionCorrection, operation);

    verify(licencePositionCorrectionService).replaceAddChangeFor(
        eq(positionCorrection), eq(PartialSurrenderOperation.class), partialSurrenderOperationCaptor.capture());

    var expected = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID))
        .withSurrenderDetails(Map.of(
            FIRST_FEATURE_ID, surrenderDetails(BlockSurrenderType.FULL_SURRENDER, FIRST_COMMAND_JOURNEY_ID)))
        .withOutputFeatureIds(List.of(SECOND_FEATURE_ID))
        .build();
    assertThat(partialSurrenderOperationCaptor.getValue()).containsExactly(expected);
  }

  @Test
  void commitPartialSurrender_whenRewritingAnAlreadyStagedSurrender_thenOutputsAreAnchoredOnTheStagedChange() {
    var stagedOperation = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID))
        .withSurrenderDetails(Map.of(
            FIRST_FEATURE_ID, surrenderDetails(BlockSurrenderType.FULL_SURRENDER, FIRST_COMMAND_JOURNEY_ID)))
        .build();
    var stagedChange = AddChange.buildOperationsChange(List.of(stagedOperation), 1);
    var positionCorrection = positionCorrection(List.of(stagedChange));
    givenCommittedPartialSurrender(positionCorrection, stagedOperation);
    when(licencePositionSpatialService.getBlockFeaturesGoingIntoChange(positionCorrection, stagedChange.changeId()))
        .thenReturn(List.of(
            FeatureTestUtil.blockFeature(FIRST_FEATURE_ID, "30", 1),
            FeatureTestUtil.blockFeature(SECOND_FEATURE_ID, "30", 2)));

    partialSurrenderCorrectionService.commitPartialSurrender(positionCorrection, stagedOperation);

    verify(licencePositionCorrectionService).replaceAddChangeFor(
        eq(positionCorrection), eq(PartialSurrenderOperation.class), partialSurrenderOperationCaptor.capture());

    var expected = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID))
        .withSurrenderDetails(Map.of(
            FIRST_FEATURE_ID, surrenderDetails(BlockSurrenderType.FULL_SURRENDER, FIRST_COMMAND_JOURNEY_ID)))
        .withOutputFeatureIds(List.of(SECOND_FEATURE_ID))
        .build();
    assertThat(partialSurrenderOperationCaptor.getValue()).containsExactly(expected);
  }

  @Test
  void commitPartialSurrender_whenABlockIsNotFullySurrendered_thenTheSurrenderIsIncompleteAndHasNoOutputs() {
    var positionCorrection = positionCorrection();
    var operation = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID, SECOND_FEATURE_ID))
        .withSurrenderDetails(Map.of(
            FIRST_FEATURE_ID, surrenderDetails(BlockSurrenderType.FULL_SURRENDER, FIRST_COMMAND_JOURNEY_ID),
            SECOND_FEATURE_ID, surrenderDetails(BlockSurrenderType.PARTIAL_SURRENDER, SECOND_COMMAND_JOURNEY_ID)))
        .build();

    partialSurrenderCorrectionService.commitPartialSurrender(positionCorrection, operation);

    verify(licencePositionCorrectionService).replaceAddChangeFor(
        eq(positionCorrection), eq(PartialSurrenderOperation.class), partialSurrenderOperationCaptor.capture());

    assertThat(partialSurrenderOperationCaptor.getValue())
        .singleElement()
        .extracting(PartialSurrenderOperation::outputFeatureIds, list(UUID.class))
        .isEmpty();
    verify(licencePositionSpatialService, never()).getBlockFeaturesGoingIntoChange(eq(positionCorrection), any());
  }

  @Test
  void commitPartialSurrenderForExecutedPosition_replacesAddChangeOnTheResolvedPositionCorrection() {
    var positionCorrection = positionCorrection();
    var operation = partialSurrender(FIRST_FEATURE_ID);
    when(licencePositionCorrectionService.getOrBuildUpdatePositionCorrection(LICENCE_CORRECTION, LICENCE_POSITION))
        .thenReturn(positionCorrection);
    when(licencePositionCorrectionService.replaceAddChangeFor(
        positionCorrection, PartialSurrenderOperation.class, List.of(operation)))
        .thenReturn(positionCorrection);

    assertThat(partialSurrenderCorrectionService.commitPartialSurrenderForExecutedPosition(
        LICENCE_CORRECTION, LICENCE_POSITION, operation))
        .isEqualTo(positionCorrection);
  }

  @Test
  void correctExistingPartialSurrender_whenNothingStaged_addsUpdateChangeKeyedOnTheLiveChange() {
    var positionCorrection = updatePositionCorrection(List.of());
    var operation = partialSurrender(FIRST_FEATURE_ID);
    givenPositionCorrection(positionCorrection);

    partialSurrenderCorrectionService.correctExistingPartialSurrender(
        LICENCE_CORRECTION, LICENCE_POSITION, LIVE_CHANGE_ID, operation);

    assertThat(positionCorrection.getPayload().changes())
        .containsExactly(UpdateChangeOperations.buildUpdateChange(LIVE_CHANGE_ID, operation));
  }

  @Test
  void correctExistingPartialSurrender_whenNothingStaged_thenOutputsAreAnchoredOnTheOriginalChangeId() {
    var positionCorrection = updatePositionCorrection(List.of());
    var operation = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID))
        .withSurrenderDetails(Map.of(
            FIRST_FEATURE_ID, surrenderDetails(BlockSurrenderType.FULL_SURRENDER, FIRST_COMMAND_JOURNEY_ID)))
        .build();
    givenPositionCorrection(positionCorrection);
    when(licencePositionSpatialService.getBlockFeaturesGoingIntoChange(positionCorrection, LIVE_CHANGE_ID))
        .thenReturn(List.of(
            FeatureTestUtil.blockFeature(FIRST_FEATURE_ID, "30", 1),
            FeatureTestUtil.blockFeature(SECOND_FEATURE_ID, "30", 2)));

    partialSurrenderCorrectionService.correctExistingPartialSurrender(
        LICENCE_CORRECTION, LICENCE_POSITION, LIVE_CHANGE_ID, operation);

    var expectedOperation = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID))
        .withSurrenderDetails(Map.of(
            FIRST_FEATURE_ID, surrenderDetails(BlockSurrenderType.FULL_SURRENDER, FIRST_COMMAND_JOURNEY_ID)))
        .withOutputFeatureIds(List.of(SECOND_FEATURE_ID))
        .build();
    assertThat(positionCorrection.getPayload().changes())
        .containsExactly(UpdateChangeOperations.buildUpdateChange(LIVE_CHANGE_ID, expectedOperation));
  }

  @Test
  void correctExistingPartialSurrender_whenStagedAsAnAddChange_replacesItKeepingTheChangeOrder() {
    var staged = partialSurrender(FIRST_FEATURE_ID);
    var positionCorrection = updatePositionCorrection(
        List.of(AddChange.buildOperationsChange(List.of(staged), 3)));
    var corrected = partialSurrender(SECOND_FEATURE_ID);
    givenPositionCorrection(positionCorrection);

    partialSurrenderCorrectionService.correctExistingPartialSurrender(
        LICENCE_CORRECTION, LICENCE_POSITION, LIVE_CHANGE_ID, corrected);

    assertThat(positionCorrection.getPayload().changes())
        .singleElement()
        .usingRecursiveComparison()
        .ignoringFields("changeId")
        .isEqualTo(AddChange.buildOperationsChange(List.of(corrected), 3));
  }

  @Test
  void correctExistingPartialSurrender_whenAlreadyCorrected_replacesTheStagedCorrection() {
    var staged = partialSurrender(FIRST_FEATURE_ID);
    var positionCorrection = updatePositionCorrection(
        List.of(UpdateChangeOperations.buildUpdateChange(LIVE_CHANGE_ID, staged)));
    var corrected = partialSurrender(SECOND_FEATURE_ID);
    givenPositionCorrection(positionCorrection);

    partialSurrenderCorrectionService.correctExistingPartialSurrender(
        LICENCE_CORRECTION, LICENCE_POSITION, LIVE_CHANGE_ID, corrected);

    assertThat(positionCorrection.getPayload().changes())
        .containsExactly(UpdateChangeOperations.buildUpdateChange(LIVE_CHANGE_ID, corrected));
  }

  @Test
  void revertPartialSurrenderCorrection_whenNoPositionCorrection_correctsNothing() {
    when(licencePositionCorrectionService.findUpdatePositionCorrection(LICENCE_CORRECTION, LICENCE_POSITION))
        .thenReturn(Optional.empty());

    partialSurrenderCorrectionService.revertPartialSurrenderCorrection(LICENCE_CORRECTION, LICENCE_POSITION);

    verify(licencePositionCorrectionService, never()).save(any());
    verify(licencePositionCorrectionService, never()).delete(any());
  }

  @Test
  void revertPartialSurrenderCorrection_whenOtherChangesRemain_dropsOnlyThePartialSurrenderChange() {
    var administratorChange = AddChange.buildOperationsChange(
        List.of(LicenceOperation.newAdministratorChange().withOperator(ADMINISTRATOR_ID).build()), 1);
    var positionCorrection = updatePositionCorrection(List.of(
        administratorChange,
        UpdateChangeOperations.buildUpdateChange(LIVE_CHANGE_ID, partialSurrender(FIRST_FEATURE_ID))));
    when(licencePositionCorrectionService.findUpdatePositionCorrection(LICENCE_CORRECTION, LICENCE_POSITION))
        .thenReturn(Optional.of(positionCorrection));

    partialSurrenderCorrectionService.revertPartialSurrenderCorrection(LICENCE_CORRECTION, LICENCE_POSITION);

    verify(licencePositionCorrectionService, never()).delete(any());
    verify(licencePositionCorrectionService).save(positionCorrection);
    assertThat(positionCorrection.getPayload().changes()).containsExactly(administratorChange);
  }

  @Test
  void revertPartialSurrenderCorrection_whenThePartialSurrenderIsTheOnlyChange_deletesThePositionCorrection() {
    var positionCorrection = updatePositionCorrection(
        List.of(UpdateChangeOperations.buildUpdateChange(LIVE_CHANGE_ID, partialSurrender(FIRST_FEATURE_ID))));
    when(licencePositionCorrectionService.findUpdatePositionCorrection(LICENCE_CORRECTION, LICENCE_POSITION))
        .thenReturn(Optional.of(positionCorrection));

    partialSurrenderCorrectionService.revertPartialSurrenderCorrection(LICENCE_CORRECTION, LICENCE_POSITION);

    verify(licencePositionCorrectionService).delete(positionCorrection);
    verify(licencePositionCorrectionService, never()).save(any());
  }

  @Test
  void revertPartialSurrenderCorrection_whenThePositionDateIsAlsoCorrected_keepsThePositionCorrection() {
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withChangeType(LicencePositionCorrectionChangeType.UPDATE_POSITION)
        .withTargetLicencePosition(LICENCE_POSITION)
        .withPayload(UpdateLicencePositionPayloadTestUtil.newBuilder()
            .withEffectiveDate(LICENCE_POSITION.getPositionDate().plusDays(1))
            .withChanges(List.of(
                UpdateChangeOperations.buildUpdateChange(LIVE_CHANGE_ID, partialSurrender(FIRST_FEATURE_ID))))
            .build())
        .build();
    when(licencePositionCorrectionService.findUpdatePositionCorrection(LICENCE_CORRECTION, LICENCE_POSITION))
        .thenReturn(Optional.of(positionCorrection));

    partialSurrenderCorrectionService.revertPartialSurrenderCorrection(LICENCE_CORRECTION, LICENCE_POSITION);

    verify(licencePositionCorrectionService, never()).delete(any());
    verify(licencePositionCorrectionService).save(positionCorrection);
    assertThat(positionCorrection.getPayload().changes()).isEmpty();
  }

  @Test
  void hasStagedPartialSurrender_whenStaged_returnsTrue() {
    var operation = partialSurrender(FIRST_FEATURE_ID);
    var positionCorrection = positionCorrection(
        List.of(AddChange.buildOperationsChange(List.of(operation), 1)));
    givenCommittedPartialSurrender(positionCorrection, operation);

    assertThat(partialSurrenderCorrectionService.hasStagedPartialSurrender(positionCorrection)).isTrue();
  }

  @Test
  void hasStagedPartialSurrender_whenNotStaged_returnsFalse() {
    assertThat(partialSurrenderCorrectionService.hasStagedPartialSurrender(positionCorrection())).isFalse();
  }

  @Test
  void adjustPartialSurrenderBlocks_whenNoCommittedSurrender_doesNothing() {
    var positionCorrection = executedPositionCorrection();

    partialSurrenderCorrectionService.adjustPartialSurrenderBlocks(positionCorrection);

    verify(licencePositionCorrectionService, never())
        .replaceAddChangeFor(eq(positionCorrection), eq(PartialSurrenderOperation.class), anyList());
  }

  @Test
  void adjustPartialSurrenderBlocks_whenAllBlocksStillSurrenderable_doesNothing() {
    var surrender = partialSurrender(FIRST_FEATURE_ID, SECOND_FEATURE_ID);
    var stagedChange = AddChange.buildOperationsChange(List.of(surrender), 1);
    var positionCorrection = executedPositionCorrection(List.of(stagedChange));
    givenCommittedPartialSurrender(positionCorrection, surrender);
    when(licencePositionSpatialService.getBlockFeaturesGoingIntoChange(positionCorrection, stagedChange.changeId()))
        .thenReturn(List.of(
            FeatureTestUtil.builder().withId(FIRST_FEATURE_ID).build(),
            FeatureTestUtil.builder().withId(SECOND_FEATURE_ID).build()
        ));

    partialSurrenderCorrectionService.adjustPartialSurrenderBlocks(positionCorrection);

    verify(licencePositionCorrectionService, never())
        .replaceAddChangeFor(eq(positionCorrection), eq(PartialSurrenderOperation.class), anyList());
    verify(licencePositionViewService, never())
        .getCorrectedChronologicalPositions(LICENCE_CORRECTION, positionCorrection.getPositionId());
  }

  @Test
  void adjustPartialSurrenderBlocks_whenSomeBlocksNoLongerSurrenderable_retainsOnlySurrenderableBlocks() {
    var surrender = partialSurrender(FIRST_FEATURE_ID, SECOND_FEATURE_ID);
    var stagedChange = AddChange.buildOperationsChange(List.of(surrender), 1);
    var positionCorrection = executedPositionCorrection(List.of(stagedChange));
    givenCommittedPartialSurrender(positionCorrection, surrender);
    when(licencePositionSpatialService.getBlockFeaturesGoingIntoChange(positionCorrection, stagedChange.changeId()))
        .thenReturn(List.of(
            FeatureTestUtil.builder().withId(FIRST_FEATURE_ID).build()
        ));

    partialSurrenderCorrectionService.adjustPartialSurrenderBlocks(positionCorrection);

    var expected = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderDate(surrender.surrenderDate())
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID))
        .build();
    verify(licencePositionCorrectionService)
        .replaceAddChangeFor(positionCorrection, PartialSurrenderOperation.class, List.of(expected));
  }

  @Test
  void adjustPartialSurrenderBlocks_whenSomeBlocksNoLongerSurrenderable_preservesSurrenderTypesForRetainedBlocks() {
    var retainedSurrenderDetails = surrenderDetails(BlockSurrenderType.FULL_SURRENDER, FIRST_COMMAND_JOURNEY_ID);
    var surrender = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID, SECOND_FEATURE_ID))
        .withSurrenderDetails(Map.of(
            FIRST_FEATURE_ID, retainedSurrenderDetails,
            SECOND_FEATURE_ID, surrenderDetails(BlockSurrenderType.PARTIAL_SURRENDER, SECOND_COMMAND_JOURNEY_ID)
        ))
        .build();
    var stagedChange = AddChange.buildOperationsChange(List.of(surrender), 1);
    var positionCorrection = executedPositionCorrection(List.of(stagedChange));
    givenCommittedPartialSurrender(positionCorrection, surrender);
    when(licencePositionSpatialService.getBlockFeaturesGoingIntoChange(positionCorrection, stagedChange.changeId()))
        .thenReturn(List.of(
            FeatureTestUtil.builder().withId(FIRST_FEATURE_ID).build(),
            FeatureTestUtil.builder().withId(THIRD_FEATURE_ID).build()
        ));

    partialSurrenderCorrectionService.adjustPartialSurrenderBlocks(positionCorrection);

    verify(commandJourneyService).deleteCommandJourney(SECOND_COMMAND_JOURNEY_ID);

    var expected = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderDate(surrender.surrenderDate())
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID))
        .withSurrenderDetails(Map.of(FIRST_FEATURE_ID, retainedSurrenderDetails))
        .withOutputFeatureIds(List.of(THIRD_FEATURE_ID))
        .build();
    verify(licencePositionCorrectionService)
        .replaceAddChangeFor(positionCorrection, PartialSurrenderOperation.class, List.of(expected));
  }

  @Test
  void adjustPartialSurrenderBlocks_whenNoBlocksStillSurrenderable_removesTheSurrender() {
    var surrender = partialSurrender(FIRST_FEATURE_ID, SECOND_FEATURE_ID);
    var stagedChange = AddChange.buildOperationsChange(List.of(surrender), 1);
    var positionCorrection = executedPositionCorrection(List.of(stagedChange));
    givenCommittedPartialSurrender(positionCorrection, surrender);
    when(licencePositionSpatialService.getBlockFeaturesGoingIntoChange(positionCorrection, stagedChange.changeId()))
        .thenReturn(List.of(
            FeatureTestUtil.builder().withId(UUID.randomUUID()).build()
        ));

    partialSurrenderCorrectionService.adjustPartialSurrenderBlocks(positionCorrection);

    verify(licencePositionCorrectionService)
        .replaceAddChangeFor(positionCorrection, PartialSurrenderOperation.class, List.of());
  }

  @Test
  void adjustPartialSurrenderBlocks_whenSomeBlocksNoLongerSurrenderable_thenALaterSurrendersOutputsAreRestaged() {
    var surrender = partialSurrender(FIRST_FEATURE_ID, SECOND_FEATURE_ID);
    var stagedChange = AddChange.buildOperationsChange(List.of(surrender), 1);
    var positionCorrection = executedPositionCorrection(List.of(stagedChange));
    givenCommittedPartialSurrender(positionCorrection, surrender);
    when(licencePositionSpatialService.getBlockFeaturesGoingIntoChange(positionCorrection, stagedChange.changeId()))
        .thenReturn(List.of(blockFeature(FIRST_FEATURE_ID, 1)));
    var laterSurrender = staleFullSurrenderOf(SECOND_FEATURE_ID);
    var laterPositionCorrection = updatePositionCorrectionFor(LATER_POSITION, List.of());
    givenLaterExecutedSurrender(positionCorrection, laterSurrender, laterPositionCorrection);
    when(licencePositionSpatialService
        .getBlockFeaturesGoingIntoChange(LICENCE_CORRECTION, LATER_POSITION.getId(), LATER_LIVE_CHANGE_ID))
        .thenReturn(List.of(blockFeature(SECOND_FEATURE_ID, 2), blockFeature(THIRD_FEATURE_ID, 3)));

    partialSurrenderCorrectionService.adjustPartialSurrenderBlocks(positionCorrection);

    verify(licencePositionCorrectionService).save(laterPositionCorrection);
    assertThat(laterPositionCorrection.getPayload().changes()).containsExactly(
        UpdateChangeOperations.buildUpdateChange(
            LATER_LIVE_CHANGE_ID, withOutputs(laterSurrender, THIRD_FEATURE_ID)));
  }

  @Test
  void getSurrenderedBlockFeatureOrThrow_whenStaged_returnsTheFeature() {
    var operation = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID))
        .build();
    var positionCorrection = positionCorrection(
        List.of(AddChange.buildOperationsChange(List.of(operation), 1)));
    var feature = FeatureTestUtil.builder().withId(FIRST_FEATURE_ID).build();
    givenCommittedPartialSurrender(positionCorrection, operation);
    when(featureService.getFeatureOrThrow(FIRST_FEATURE_ID)).thenReturn(feature);

    assertThat(partialSurrenderCorrectionService.getSurrenderedBlockFeatureOrThrow(positionCorrection, FIRST_FEATURE_ID))
        .isEqualTo(feature);
  }

  @Test
  void getSurrenderedBlockFeatureOrThrow_whenNotStaged_throws() {
    var operation = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID))
        .build();
    var positionCorrection = positionCorrection(
        List.of(AddChange.buildOperationsChange(List.of(operation), 1)));
    givenCommittedPartialSurrender(positionCorrection, operation);

    assertThatThrownBy(() ->
        partialSurrenderCorrectionService.getSurrenderedBlockFeatureOrThrow(positionCorrection, SECOND_FEATURE_ID))
        .isInstanceOf(LmsEntityNotFoundException.class);
  }

  @Test
  void setBlockSurrenderType_replacesTheOperationWithTheTypeSetForTheFeaturePreservingOtherState() {
    var surrenderDate = LocalDate.of(2026, Month.AUGUST, 1);
    var existingBlockSurrender = surrenderDetails(BlockSurrenderType.PARTIAL_SURRENDER, FIRST_COMMAND_JOURNEY_ID);
    var existingOperation = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderDate(surrenderDate)
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID, SECOND_FEATURE_ID))
        .withSurrenderDetails(Map.of(FIRST_FEATURE_ID, existingBlockSurrender))
        .build();
    var positionCorrection = positionCorrection(
        List.of(AddChange.buildOperationsChange(List.of(existingOperation), 1)));
    givenCommittedPartialSurrender(positionCorrection, existingOperation);
    var secondFeature = FeatureTestUtil.builder().withId(SECOND_FEATURE_ID).build();
    when(featureService.getFeatureOrThrow(SECOND_FEATURE_ID)).thenReturn(secondFeature);
    var createdJourney = new CommandJourney();
    createdJourney.setId(SECOND_COMMAND_JOURNEY_ID);
    when(commandJourneyService.createAndAssignCommandJourney(List.of(secondFeature)))
        .thenReturn(createdJourney);

    partialSurrenderCorrectionService.setBlockSurrenderType(
        positionCorrection, SECOND_FEATURE_ID,
        BlockSurrenderType.FULL_SURRENDER
    );

    var expectedOperation = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderDate(surrenderDate)
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID, SECOND_FEATURE_ID))
        .withSurrenderDetails(Map.of(
            FIRST_FEATURE_ID, existingBlockSurrender,
            SECOND_FEATURE_ID, new PartialSurrenderOperation.SurrenderDetails(
                BlockSurrenderType.FULL_SURRENDER, SECOND_COMMAND_JOURNEY_ID, List.of(SECOND_FEATURE_ID))
        ))
        .build();

    verify(licencePositionCorrectionService).replaceAddChangeFor(
        eq(positionCorrection), eq(PartialSurrenderOperation.class), partialSurrenderOperationCaptor.capture());
    assertThat(partialSurrenderOperationCaptor.getValue()).containsExactly(expectedOperation);
  }

  @Test
  void getSurrenderUnderCorrectionOrThrow_whenStagedOnThisCorrection_returnsTheStagedSurrender() {
    var staged = partialSurrender(FIRST_FEATURE_ID);
    var positionCorrection = updatePositionCorrection(
        List.of(UpdateChangeOperations.buildUpdateChange(LIVE_CHANGE_ID, staged)));
    when(licencePositionCorrectionService.findUpdatePositionCorrection(LICENCE_CORRECTION, LICENCE_POSITION))
        .thenReturn(Optional.of(positionCorrection));
    givenCommittedPartialSurrender(positionCorrection, staged);

    assertThat(partialSurrenderCorrectionService.getSurrenderUnderCorrectionOrThrow(
        LICENCE_CORRECTION, LICENCE_POSITION, LIVE_CHANGE_ID))
        .isEqualTo(staged);
  }

  @Test
  void getSurrenderUnderCorrectionOrThrow_whenNothingStaged_returnsTheLiveSurrender() {
    var live = partialSurrender(SECOND_FEATURE_ID);
    when(licencePositionCorrectionService.findUpdatePositionCorrection(LICENCE_CORRECTION, LICENCE_POSITION))
        .thenReturn(Optional.empty());
    givenLiveSurrenderChange(live);

    assertThat(partialSurrenderCorrectionService.getSurrenderUnderCorrectionOrThrow(
        LICENCE_CORRECTION, LICENCE_POSITION, LIVE_CHANGE_ID))
        .isEqualTo(live);
  }

  @Test
  void getSurrenderUnderCorrectionOrThrow_whenTheLiveChangeIsNotAPartialSurrender_throws() {
    when(licencePositionCorrectionService.findUpdatePositionCorrection(LICENCE_CORRECTION, LICENCE_POSITION))
        .thenReturn(Optional.empty());
    when(licencePositionChangeService.getByIdOrThrow(UUID.fromString(LIVE_CHANGE_ID)))
        .thenReturn(LicencePositionChangeTestUtil.newBuilder()
            .withId(UUID.fromString(LIVE_CHANGE_ID))
            .withOperations(List.of(LicenceOperation.newAdministratorChange().withOperator(1).build()))
            .build());

    assertThatThrownBy(() -> partialSurrenderCorrectionService.getSurrenderUnderCorrectionOrThrow(
        LICENCE_CORRECTION, LICENCE_POSITION, LIVE_CHANGE_ID))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining(LIVE_CHANGE_ID);
  }

  @Test
  void findCorrectedLiveChangeId_whenTheStagedSurrenderCorrectsALiveChange_thenTheLiveChangeId() {
    var positionCorrection = updatePositionCorrection(List.of(
        UpdateChangeOperations.buildUpdateChange(LIVE_CHANGE_ID, partialSurrender(FIRST_FEATURE_ID))));

    assertThat(partialSurrenderCorrectionService.findCorrectedLiveChangeId(positionCorrection))
        .contains(LIVE_CHANGE_ID);
  }

  @Test
  void findCorrectedLiveChangeId_whenTheStagedSurrenderIsNewlyAdded_thenEmpty() {
    var positionCorrection = updatePositionCorrection(List.of(
        AddChange.buildOperationsChange(List.of(partialSurrender(FIRST_FEATURE_ID)), 1)));

    assertThat(partialSurrenderCorrectionService.findCorrectedLiveChangeId(positionCorrection)).isEmpty();
  }

  @Test
  void removeExistingPartialSurrender() {
    partialSurrenderCorrectionService
        .removeExistingPartialSurrender(LICENCE_POSITION, LICENCE_CORRECTION, LIVE_CHANGE_ID);

    verify(licencePositionCorrectionService)
        .stageRemovalOfExecutedChange(LICENCE_CORRECTION, LICENCE_POSITION, LIVE_CHANGE_ID);
  }

  @Test
  void undoPartialSurrenderChange_whenTheSurrenderWasAddedByThisCorrection_thenDeletesItsCommandJourneys() {
    var addChange = AddChange.buildOperationsChange(List.of(fullSurrenderOf(FIRST_FEATURE_ID)), 1);
    var positionCorrection = givenStagedChange(addChange);

    partialSurrenderCorrectionService.undoPartialSurrenderChange(LICENCE_CORRECTION, addChange.changeId());

    verify(commandJourneyService).deleteCommandJourney(FIRST_COMMAND_JOURNEY_ID);
    verify(licencePositionCorrectionService).dropStagedChange(positionCorrection, addChange.changeId());
  }

  @Test
  void undoPartialSurrenderChange_whenTheSurrenderCorrectsALiveChange_thenKeepsTheSharedCommandJourneys() {
    var updateChange =
        UpdateChangeOperations.buildUpdateChange(LIVE_CHANGE_ID, fullSurrenderOf(FIRST_FEATURE_ID));
    var positionCorrection = givenStagedChange(updateChange);
    givenLiveSurrenderChange(fullSurrenderOf(FIRST_FEATURE_ID));

    partialSurrenderCorrectionService.undoPartialSurrenderChange(LICENCE_CORRECTION, updateChange.changeId());

    verify(commandJourneyService, never()).deleteCommandJourney(any());
    verify(licencePositionCorrectionService).dropStagedChange(positionCorrection, updateChange.changeId());
  }

  @Test
  void undoPartialSurrenderChange_whenTheCorrectionSelectedABlockTheLiveSurrenderDidNotHold_thenDeletesOnlyTheJourneyItCreated() {
    var updateChange = UpdateChangeOperations.buildUpdateChange(
        LIVE_CHANGE_ID, twoBlockSurrenderOf(FIRST_FEATURE_ID, SECOND_FEATURE_ID));
    var positionCorrection = givenStagedChange(updateChange);
    givenLiveSurrenderChange(fullSurrenderOf(FIRST_FEATURE_ID));

    partialSurrenderCorrectionService.undoPartialSurrenderChange(LICENCE_CORRECTION, updateChange.changeId());

    verify(commandJourneyService).deleteCommandJourney(SECOND_COMMAND_JOURNEY_ID);
    verify(commandJourneyService, never()).deleteCommandJourney(FIRST_COMMAND_JOURNEY_ID);
    verify(licencePositionCorrectionService).dropStagedChange(positionCorrection, updateChange.changeId());
  }

  @Test
  void undoPartialSurrenderChange_whenTheSurrenderWasRemovedByThisCorrection_thenKeepsTheLiveCommandJourneys() {
    var removeChange = LicencePositionChangeType.removeChange().withChangeId(LIVE_CHANGE_ID).build();
    var positionCorrection = givenStagedChange(removeChange, fullSurrenderOf(FIRST_FEATURE_ID));

    partialSurrenderCorrectionService.undoPartialSurrenderChange(LICENCE_CORRECTION, LIVE_CHANGE_ID);

    verify(commandJourneyService, never()).deleteCommandJourney(any());
    verify(licencePositionCorrectionService).dropStagedChange(positionCorrection, LIVE_CHANGE_ID);
  }

  @Test
  void undoPartialSurrenderChange_whenTheChangeIsNotAPartialSurrender_thenThrowsAndDropsNothing() {
    var administratorChange = AddChange.buildOperationsChange(
        List.of(LicenceOperation.newAdministratorChange().withOperator(ADMINISTRATOR_ID).build()), 1);
    var changeId = administratorChange.changeId();
    givenStagedChange(administratorChange);

    assertThatThrownBy(() ->
        partialSurrenderCorrectionService.undoPartialSurrenderChange(LICENCE_CORRECTION, changeId))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining(changeId);

    verify(licencePositionCorrectionService, never()).dropStagedChange(any(), any());
    verify(commandJourneyService, never()).deleteCommandJourney(any());
  }

  @Test
  void getStagedPartialSurrenderOrThrow_whenTheChangeIsARemoveChange_thenReturnsTheLiveSurrender() {
    var live = fullSurrenderOf(FIRST_FEATURE_ID);
    var removeChange = LicencePositionChangeType.removeChange().withChangeId(LIVE_CHANGE_ID).build();
    var positionCorrection = updatePositionCorrection(List.of(removeChange));
    when(licencePositionCorrectionService.getStagedChangeOrThrow(positionCorrection, LIVE_CHANGE_ID))
        .thenReturn(removeChange);
    when(licencePositionCorrectionService.resolveStagedChangeOperations(removeChange))
        .thenReturn(List.of(live));

    var result = partialSurrenderCorrectionService
        .getStagedPartialSurrenderOrThrow(positionCorrection, LIVE_CHANGE_ID);

    assertThat(result).isEqualTo(live);
  }

  @Test
  void getStagedPartialSurrenderOrThrow_whenTheChangeIsAnAddChange_thenReturnsTheStagedSurrender() {
    var staged = partialSurrender(SECOND_FEATURE_ID);
    var addChange = AddChange.buildOperationsChange(List.of(staged), 1);
    var positionCorrection = updatePositionCorrection(List.of(addChange));
    when(licencePositionCorrectionService.getStagedChangeOrThrow(positionCorrection, addChange.changeId()))
        .thenReturn(addChange);
    when(licencePositionCorrectionService.resolveStagedChangeOperations(addChange))
        .thenReturn(List.of(staged));

    var result = partialSurrenderCorrectionService
        .getStagedPartialSurrenderOrThrow(positionCorrection, addChange.changeId());

    assertThat(result).isEqualTo(staged);
  }

  private static PartialSurrenderOperation twoBlockSurrenderOf(UUID firstFeatureId, UUID secondFeatureId) {
    return LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(firstFeatureId, secondFeatureId))
        .withSurrenderDetails(Map.of(
            firstFeatureId, surrenderDetails(BlockSurrenderType.FULL_SURRENDER, FIRST_COMMAND_JOURNEY_ID),
            secondFeatureId, surrenderDetails(BlockSurrenderType.FULL_SURRENDER, SECOND_COMMAND_JOURNEY_ID)))
        .build();
  }

  private LicencePositionCorrection givenStagedChange(LicencePositionChangeType change) {
    return givenStagedChange(change, LicencePositionChangeType.operationsOf(change).toArray(LicenceOperation[]::new));
  }

  private LicencePositionCorrection givenStagedChange(
      LicencePositionChangeType change,
      LicenceOperation... resolvedOperations
  ) {
    var positionCorrection = updatePositionCorrection(List.of(change));

    when(licencePositionCorrectionService
        .getPositionCorrectionContainingChange(LICENCE_CORRECTION, change.changeId()))
        .thenReturn(positionCorrection);
    when(licencePositionCorrectionService.getStagedChangeOrThrow(positionCorrection, change.changeId()))
        .thenReturn(change);
    when(licencePositionCorrectionService.resolveStagedChangeOperations(change))
        .thenReturn(List.of(resolvedOperations));

    return positionCorrection;
  }

  @Test
  void getBlockRows() {
    var surrender = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID, SECOND_FEATURE_ID))
        .withSurrenderDetails(Map.of(FIRST_FEATURE_ID,
            surrenderDetails(BlockSurrenderType.FULL_SURRENDER, FIRST_COMMAND_JOURNEY_ID)))
        .build();
    when(featureService.getFeaturesByIds(List.of(FIRST_FEATURE_ID, SECOND_FEATURE_ID))).thenReturn(List.of(
        FeatureTestUtil.blockFeature(FIRST_FEATURE_ID, "30", 1),
        FeatureTestUtil.blockFeature(SECOND_FEATURE_ID, "30", 2)));

    var result = partialSurrenderCorrectionService.getBlockRows(surrender);

    assertThat(result).containsExactly(
        new PartialSurrenderChangeView.BlockRow("SHAPE 1", BlockSurrenderType.FULL_SURRENDER.getDisplayName()),
        new PartialSurrenderChangeView.BlockRow("SHAPE 2", null));
  }

  @Test
  void getBlockRows_whenAFeatureCannotBeResolved_thenTheBlockLabelIsNotAvailable() {
    var surrender = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID, SECOND_FEATURE_ID))
        .withSurrenderDetails(Map.of(FIRST_FEATURE_ID,
            surrenderDetails(BlockSurrenderType.FULL_SURRENDER, FIRST_COMMAND_JOURNEY_ID)))
        .build();
    when(featureService.getFeaturesByIds(List.of(FIRST_FEATURE_ID, SECOND_FEATURE_ID)))
        .thenReturn(List.of(FeatureTestUtil.blockFeature(FIRST_FEATURE_ID, "30", 1)));

    var result = partialSurrenderCorrectionService.getBlockRows(surrender);

    assertThat(result).containsExactly(
        new PartialSurrenderChangeView.BlockRow("SHAPE 1", BlockSurrenderType.FULL_SURRENDER.getDisplayName()),
        new PartialSurrenderChangeView.BlockRow("Not available", null));
  }

  private void givenLiveSurrenderChange(PartialSurrenderOperation liveSurrender) {
    when(licencePositionChangeService.getByIdOrThrow(UUID.fromString(LIVE_CHANGE_ID)))
        .thenReturn(LicencePositionChangeTestUtil.newBuilder()
            .withId(UUID.fromString(LIVE_CHANGE_ID))
            .withOperations(List.of(liveSurrender))
            .build());
  }

  @Test
  void setBlockSurrenderType_whenTypeUnchanged_reusesJourneyAndSurrenderedFeatures() {
    var existingSurrenderDetails = new PartialSurrenderOperation.SurrenderDetails(
        BlockSurrenderType.PARTIAL_SURRENDER, FIRST_COMMAND_JOURNEY_ID, List.of(FIRST_FEATURE_ID));
    var existingOperation = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID))
        .withSurrenderDetails(Map.of(FIRST_FEATURE_ID, existingSurrenderDetails))
        .build();
    var positionCorrection = positionCorrection(
        List.of(AddChange.buildOperationsChange(List.of(existingOperation), 1)));
    givenCommittedPartialSurrender(positionCorrection, existingOperation);

    partialSurrenderCorrectionService.setBlockSurrenderType(
        positionCorrection, FIRST_FEATURE_ID, BlockSurrenderType.PARTIAL_SURRENDER);

    verify(commandJourneyService, never()).createAndAssignCommandJourney(anyList());

    var expectedOperation = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID))
        .withSurrenderDetails(Map.of(FIRST_FEATURE_ID, existingSurrenderDetails))
        .build();
    verify(licencePositionCorrectionService)
        .replaceAddChangeFor(eq(positionCorrection), eq(PartialSurrenderOperation.class), partialSurrenderOperationCaptor.capture());
    assertThat(partialSurrenderOperationCaptor.getValue()).containsExactly(expectedOperation);
  }

  @Test
  void setBlockSurrenderType_whenTypeChanged_deletesOldJourneyAndCreatesNew() {
    var existingSurrenderDetails = new PartialSurrenderOperation.SurrenderDetails(
        BlockSurrenderType.PARTIAL_SURRENDER, FIRST_COMMAND_JOURNEY_ID, List.of(FIRST_FEATURE_ID));
    var existingOperation = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID))
        .withSurrenderDetails(Map.of(FIRST_FEATURE_ID, existingSurrenderDetails))
        .build();
    var positionCorrection = positionCorrection(
        List.of(AddChange.buildOperationsChange(List.of(existingOperation), 1)));
    givenCommittedPartialSurrender(positionCorrection, existingOperation);
    var feature = FeatureTestUtil.builder().withId(FIRST_FEATURE_ID).build();
    var createdJourney = new CommandJourney();
    createdJourney.setId(SECOND_COMMAND_JOURNEY_ID);

    when(featureService.getFeatureOrThrow(FIRST_FEATURE_ID)).thenReturn(feature);
    when(commandJourneyService.createAndAssignCommandJourney(List.of(feature))).thenReturn(createdJourney);

    partialSurrenderCorrectionService.setBlockSurrenderType(
        positionCorrection, FIRST_FEATURE_ID, BlockSurrenderType.FULL_SURRENDER);

    verify(commandJourneyService).deleteCommandJourney(FIRST_COMMAND_JOURNEY_ID);

    var expectedOperation = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID))
        .withSurrenderDetails(Map.of(FIRST_FEATURE_ID, new PartialSurrenderOperation.SurrenderDetails(
            BlockSurrenderType.FULL_SURRENDER, SECOND_COMMAND_JOURNEY_ID, List.of(FIRST_FEATURE_ID))))
        .build();
    verify(licencePositionCorrectionService)
        .replaceAddChangeFor(eq(positionCorrection), eq(PartialSurrenderOperation.class), partialSurrenderOperationCaptor.capture());
    assertThat(partialSurrenderOperationCaptor.getValue()).containsExactly(expectedOperation);
  }

  @Test
  void getBlockSurrenderOrThrow_whenStaged_returnsTheBlockSurrender() {
    var surrenderDetails = surrenderDetails(BlockSurrenderType.PARTIAL_SURRENDER, FIRST_COMMAND_JOURNEY_ID);
    var operation = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID))
        .withSurrenderDetails(Map.of(FIRST_FEATURE_ID, surrenderDetails))
        .build();
    var positionCorrection = positionCorrection(
        List.of(AddChange.buildOperationsChange(List.of(operation), 1)));
    givenCommittedPartialSurrender(positionCorrection, operation);

    assertThat(partialSurrenderCorrectionService.getSurrenderDetailsOrThrow(positionCorrection, FIRST_FEATURE_ID))
        .isEqualTo(surrenderDetails);
  }

  @Test
  void setSurrenderedFeatureIds_replacesTheOperationWithSurrenderedIdsForFeaturePreservingOtherState() {
    var firstSurrenderedId = UUID.randomUUID();
    var secondSurrenderedId = UUID.randomUUID();
    var existingBlockSurrender = new PartialSurrenderOperation.SurrenderDetails(
        BlockSurrenderType.PARTIAL_SURRENDER, FIRST_COMMAND_JOURNEY_ID, List.of());
    var existingOperation = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID))
        .withSurrenderDetails(Map.of(FIRST_FEATURE_ID, existingBlockSurrender))
        .build();
    var positionCorrection = positionCorrection(
        List.of(AddChange.buildOperationsChange(List.of(existingOperation), 1)));
    givenCommittedPartialSurrender(positionCorrection, existingOperation);

    partialSurrenderCorrectionService.setSurrenderedFeatureIds(
        positionCorrection, FIRST_FEATURE_ID, List.of(firstSurrenderedId, secondSurrenderedId));

    verify(commandJourneyService, never()).deleteCommandJourney(FIRST_COMMAND_JOURNEY_ID);

    var expectedOperation = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID))
        .withSurrenderDetails(Map.of(FIRST_FEATURE_ID, new PartialSurrenderOperation.SurrenderDetails(
            BlockSurrenderType.PARTIAL_SURRENDER, FIRST_COMMAND_JOURNEY_ID,
            List.of(firstSurrenderedId, secondSurrenderedId))))
        .build();
    verify(licencePositionCorrectionService)
        .replaceAddChangeFor(eq(positionCorrection), eq(PartialSurrenderOperation.class), partialSurrenderOperationCaptor.capture());
    assertThat(partialSurrenderOperationCaptor.getValue()).containsExactly(expectedOperation);
  }

  @Test
  void clearSurrenderedIds_whenSomeSurrenderedIdsNoLongerActive_clearsSurrenderedIds() {
    var staleSurrenderedId = UUID.randomUUID();
    var existingBlockSurrender = new PartialSurrenderOperation.SurrenderDetails(
        BlockSurrenderType.PARTIAL_SURRENDER, FIRST_COMMAND_JOURNEY_ID, List.of(staleSurrenderedId));
    var existingOperation = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID))
        .withSurrenderDetails(Map.of(FIRST_FEATURE_ID, existingBlockSurrender))
        .build();
    var positionCorrection = positionCorrection(
        List.of(AddChange.buildOperationsChange(List.of(existingOperation), 1)));
    givenCommittedPartialSurrender(positionCorrection, existingOperation);

    partialSurrenderCorrectionService.clearSurrenderedIds(
        positionCorrection, FIRST_FEATURE_ID, List.of(UUID.randomUUID()));

    var expectedOperation = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID))
        .withSurrenderDetails(Map.of(FIRST_FEATURE_ID, new PartialSurrenderOperation.SurrenderDetails(
            BlockSurrenderType.PARTIAL_SURRENDER, FIRST_COMMAND_JOURNEY_ID, List.of())))
        .build();
    verify(licencePositionCorrectionService)
        .replaceAddChangeFor(eq(positionCorrection), eq(PartialSurrenderOperation.class), partialSurrenderOperationCaptor.capture());
    assertThat(partialSurrenderOperationCaptor.getValue()).containsExactly(expectedOperation);
  }

  @Test
  void clearSurrenderedIds_whenAllSurrenderedIdsStillActive_doesNothing() {
    var surrenderedId = UUID.randomUUID();
    var existingBlockSurrender = new PartialSurrenderOperation.SurrenderDetails(
        BlockSurrenderType.PARTIAL_SURRENDER, FIRST_COMMAND_JOURNEY_ID, List.of(surrenderedId));
    var existingOperation = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID))
        .withSurrenderDetails(Map.of(FIRST_FEATURE_ID, existingBlockSurrender))
        .build();
    var positionCorrection = positionCorrection(
        List.of(AddChange.buildOperationsChange(List.of(existingOperation), 1)));
    givenCommittedPartialSurrender(positionCorrection, existingOperation);

    partialSurrenderCorrectionService.clearSurrenderedIds(
        positionCorrection, FIRST_FEATURE_ID, List.of(surrenderedId, UUID.randomUUID()));

    verify(licencePositionCorrectionService, never())
        .replaceAddChangeFor(eq(positionCorrection), eq(PartialSurrenderOperation.class), anyList());
  }

  @Test
  void clearSurrenderedIds_whenNoSurrenderedIds_doesNothing() {
    var existingBlockSurrender = new PartialSurrenderOperation.SurrenderDetails(
        BlockSurrenderType.PARTIAL_SURRENDER, FIRST_COMMAND_JOURNEY_ID, List.of());
    var existingOperation = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID))
        .withSurrenderDetails(Map.of(FIRST_FEATURE_ID, existingBlockSurrender))
        .build();
    var positionCorrection = positionCorrection(
        List.of(AddChange.buildOperationsChange(List.of(existingOperation), 1)));
    givenCommittedPartialSurrender(positionCorrection, existingOperation);

    partialSurrenderCorrectionService.clearSurrenderedIds(
        positionCorrection, FIRST_FEATURE_ID, List.of(UUID.randomUUID()));

    verify(licencePositionCorrectionService, never())
        .replaceAddChangeFor(eq(positionCorrection), eq(PartialSurrenderOperation.class), anyList());
  }

  @Test
  void clearSurrenderedIds_whenNoBlockSurrenderForFeature_doesNothing() {
    var existingOperation = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID))
        .build();
    var positionCorrection = positionCorrection(
        List.of(AddChange.buildOperationsChange(List.of(existingOperation), 1)));
    givenCommittedPartialSurrender(positionCorrection, existingOperation);

    partialSurrenderCorrectionService.clearSurrenderedIds(
        positionCorrection, FIRST_FEATURE_ID, List.of(UUID.randomUUID()));

    verify(licencePositionCorrectionService, never())
        .replaceAddChangeFor(eq(positionCorrection), eq(PartialSurrenderOperation.class), anyList());
  }

  @Test
  void getBlockSurrenderOrThrow_whenNoSurrenderForFeature_throws() {
    var operation = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID))
        .build();
    var positionCorrection = positionCorrection(
        List.of(AddChange.buildOperationsChange(List.of(operation), 1)));
    givenCommittedPartialSurrender(positionCorrection, operation);

    assertThatThrownBy(() ->
        partialSurrenderCorrectionService.getSurrenderDetailsOrThrow(positionCorrection, FIRST_FEATURE_ID))
        .isInstanceOf(LmsEntityNotFoundException.class);
  }

  @Test
  void getOrCreatePartialSurrenderDetails_whenNoExistingDetails_createsJourneyAndReturnsOperationWithDetails() {
    var surrenderDate = LocalDate.of(2026, Month.AUGUST, 1);
    var operation = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderDate(surrenderDate)
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID))
        .build();
    var feature = FeatureTestUtil.builder().withId(FIRST_FEATURE_ID).build();
    var createdJourney = new CommandJourney();
    createdJourney.setId(FIRST_COMMAND_JOURNEY_ID);
    when(featureService.getFeatureOrThrow(FIRST_FEATURE_ID)).thenReturn(feature);
    when(commandJourneyService.createAndAssignCommandJourney(List.of(feature))).thenReturn(createdJourney);

    var result = partialSurrenderCorrectionService.getOrCreatePartialSurrenderDetails(
        operation, FIRST_FEATURE_ID, BlockSurrenderType.FULL_SURRENDER);

    var expected = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderDate(surrenderDate)
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID))
        .withSurrenderDetails(Map.of(FIRST_FEATURE_ID, new PartialSurrenderOperation.SurrenderDetails(
            BlockSurrenderType.FULL_SURRENDER, FIRST_COMMAND_JOURNEY_ID, List.of(FIRST_FEATURE_ID))))
        .build();
    assertThat(result).isEqualTo(expected);
  }

  @Test
  void getOrCreatePartialSurrenderDetails_whenExistingDetails_reusesJourneyWithoutCreating() {
    var existingSurrenderDetails = new PartialSurrenderOperation.SurrenderDetails(
        BlockSurrenderType.PARTIAL_SURRENDER, FIRST_COMMAND_JOURNEY_ID, List.of(FIRST_FEATURE_ID));
    var operation = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID))
        .withSurrenderDetails(Map.of(FIRST_FEATURE_ID, existingSurrenderDetails))
        .build();

    var result = partialSurrenderCorrectionService.getOrCreatePartialSurrenderDetails(
        operation, FIRST_FEATURE_ID, BlockSurrenderType.PARTIAL_SURRENDER);

    verify(commandJourneyService, never()).createAndAssignCommandJourney(anyList());

    var expected = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID))
        .withSurrenderDetails(Map.of(FIRST_FEATURE_ID, existingSurrenderDetails))
        .build();
    assertThat(result).isEqualTo(expected);
  }

  @Test
  void getOrCreatePartialSurrenderDetails_whenExistingDetailsOfDifferentType_reusesJourneyAndResetsSurrenderedFeatures() {
    var existingSurrenderDetails = new PartialSurrenderOperation.SurrenderDetails(
        BlockSurrenderType.PARTIAL_SURRENDER, FIRST_COMMAND_JOURNEY_ID, List.of(SECOND_FEATURE_ID));
    var operation = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID))
        .withSurrenderDetails(Map.of(FIRST_FEATURE_ID, existingSurrenderDetails))
        .build();

    var result = partialSurrenderCorrectionService.getOrCreatePartialSurrenderDetails(
        operation, FIRST_FEATURE_ID, BlockSurrenderType.FULL_SURRENDER);

    verify(commandJourneyService, never()).createAndAssignCommandJourney(anyList());

    var expected = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID))
        .withSurrenderDetails(Map.of(FIRST_FEATURE_ID, new PartialSurrenderOperation.SurrenderDetails(
            BlockSurrenderType.FULL_SURRENDER, FIRST_COMMAND_JOURNEY_ID, List.of(FIRST_FEATURE_ID))))
        .build();
    assertThat(result).isEqualTo(expected);
  }

  @Test
  void commitPartialSurrender_whenALaterExecutedSurrenderExists_thenItsOutputsAreRestagedAsAnUpdateChange() {
    var positionCorrection = addedPositionCorrection();
    var operation = fullSurrenderOf(FIRST_FEATURE_ID);
    when(licencePositionSpatialService.getBlockFeaturesGoingIntoChange(positionCorrection, null))
        .thenReturn(List.of(blockFeature(FIRST_FEATURE_ID, 1), blockFeature(SECOND_FEATURE_ID, 2)));
    var laterSurrender = staleFullSurrenderOf(SECOND_FEATURE_ID);
    var laterPositionCorrection = updatePositionCorrectionFor(LATER_POSITION, List.of());
    givenLaterExecutedSurrender(positionCorrection, laterSurrender, laterPositionCorrection);
    when(licencePositionSpatialService
        .getBlockFeaturesGoingIntoChange(LICENCE_CORRECTION, LATER_POSITION.getId(), LATER_LIVE_CHANGE_ID))
        .thenReturn(List.of(blockFeature(SECOND_FEATURE_ID, 2), blockFeature(THIRD_FEATURE_ID, 3)));

    partialSurrenderCorrectionService.commitPartialSurrender(positionCorrection, operation);

    verify(licencePositionCorrectionService).save(laterPositionCorrection);
    assertThat(laterPositionCorrection.getPayload().changes()).containsExactly(
        UpdateChangeOperations.buildUpdateChange(
            LATER_LIVE_CHANGE_ID, withOutputs(laterSurrender, THIRD_FEATURE_ID)));
  }

  @Test
  void commitPartialSurrender_whenALaterSurrenderWasAddedByThisCorrection_thenItsOutputsAreUpdatedInPlace() {
    var positionCorrection = addedPositionCorrection();
    var operation = fullSurrenderOf(FIRST_FEATURE_ID);
    when(licencePositionSpatialService.getBlockFeaturesGoingIntoChange(positionCorrection, null))
        .thenReturn(List.of(blockFeature(FIRST_FEATURE_ID, 1), blockFeature(SECOND_FEATURE_ID, 2)));
    var laterSurrender = staleFullSurrenderOf(SECOND_FEATURE_ID);
    var laterAddChange = AddChange.buildOperationsChange(List.of(laterSurrender), 3);
    var laterPositionCorrection = updatePositionCorrectionFor(LATER_POSITION, List.of(laterAddChange));
    when(licencePositionViewService.getCorrectedChronologicalPositions(LICENCE_CORRECTION, positionId(positionCorrection)))
        .thenReturn(List.of(
            ChronologicalPositionTestUtil.newBuilder().withId(positionId(positionCorrection)).build(),
            laterChronologicalPosition(laterAddChange.changeId(), LicencePositionChangeType.ADD_CHANGE, laterSurrender)));
    when(licencePositionCorrectionService
        .getPositionCorrectionContainingChange(LICENCE_CORRECTION, laterAddChange.changeId()))
        .thenReturn(laterPositionCorrection);
    when(licencePositionSpatialService
        .getBlockFeaturesGoingIntoChange(LICENCE_CORRECTION, LATER_POSITION.getId(), laterAddChange.changeId()))
        .thenReturn(List.of(blockFeature(SECOND_FEATURE_ID, 2), blockFeature(THIRD_FEATURE_ID, 3)));

    partialSurrenderCorrectionService.commitPartialSurrender(positionCorrection, operation);

    verify(licencePositionCorrectionService).save(laterPositionCorrection);
    assertThat(laterPositionCorrection.getPayload().changes())
        .singleElement()
        .usingRecursiveComparison()
        .ignoringFields("changeId")
        .isEqualTo(AddChange.buildOperationsChange(
            List.of(withOutputs(laterSurrender, THIRD_FEATURE_ID)), 3));
  }

  @Test
  void commitPartialSurrender_whenALaterSurrenderKeepsTheSameOutputs_thenItIsLeftAlone() {
    var positionCorrection = addedPositionCorrection();
    var operation = fullSurrenderOf(FIRST_FEATURE_ID);
    when(licencePositionSpatialService.getBlockFeaturesGoingIntoChange(positionCorrection, null))
        .thenReturn(List.of(blockFeature(FIRST_FEATURE_ID, 1), blockFeature(SECOND_FEATURE_ID, 2)));
    var laterSurrender = withOutputs(fullSurrenderOf(SECOND_FEATURE_ID), THIRD_FEATURE_ID);
    givenLaterExecutedSurrender(positionCorrection, laterSurrender, null);
    when(licencePositionSpatialService
        .getBlockFeaturesGoingIntoChange(LICENCE_CORRECTION, LATER_POSITION.getId(), LATER_LIVE_CHANGE_ID))
        .thenReturn(List.of(blockFeature(SECOND_FEATURE_ID, 2), blockFeature(THIRD_FEATURE_ID, 3)));

    partialSurrenderCorrectionService.commitPartialSurrender(positionCorrection, operation);

    verify(licencePositionCorrectionService, never())
        .getOrBuildUpdatePositionCorrection(LICENCE_CORRECTION, LATER_POSITION);
  }

  @Test
  void commitPartialSurrender_whenALaterChangeOnTheSamePositionHasASurrender_thenItsOutputsAreRestaged() {
    var positionCorrection = addedPositionCorrection();
    var operation = fullSurrenderOf(FIRST_FEATURE_ID);
    when(licencePositionSpatialService.getBlockFeaturesGoingIntoChange(positionCorrection, null))
        .thenReturn(List.of(blockFeature(FIRST_FEATURE_ID, 1), blockFeature(SECOND_FEATURE_ID, 2)));
    var editedChange = AddChange.buildOperationsChange(List.of(operation), 1);
    when(licencePositionCorrectionService
        .replaceAddChangeFor(eq(positionCorrection), eq(PartialSurrenderOperation.class), anyList()))
        .thenReturn(updatePositionCorrectionFor(LICENCE_POSITION, List.of(editedChange)));
    var laterSurrender = staleFullSurrenderOf(SECOND_FEATURE_ID);
    var laterChange = AddChange.buildOperationsChange(List.of(laterSurrender), 2);
    var laterPositionCorrection = updatePositionCorrectionFor(LICENCE_POSITION, List.of(laterChange));
    when(licencePositionViewService
        .getCorrectedChronologicalPositions(LICENCE_CORRECTION, positionId(positionCorrection)))
        .thenReturn(List.of(chronologicalPositionWith(
            positionId(positionCorrection),
            positionChange(editedChange.changeId(), 1, operation),
            positionChange(laterChange.changeId(), 2, laterSurrender))));
    when(licencePositionCorrectionService
        .getPositionCorrectionContainingChange(LICENCE_CORRECTION, laterChange.changeId()))
        .thenReturn(laterPositionCorrection);
    when(licencePositionSpatialService.getBlockFeaturesGoingIntoChange(
        LICENCE_CORRECTION, positionId(positionCorrection), laterChange.changeId()))
        .thenReturn(List.of(blockFeature(SECOND_FEATURE_ID, 2), blockFeature(THIRD_FEATURE_ID, 3)));

    partialSurrenderCorrectionService.commitPartialSurrender(positionCorrection, operation);

    verify(licencePositionCorrectionService).save(laterPositionCorrection);
    assertThat(laterPositionCorrection.getPayload().changes())
        .singleElement()
        .usingRecursiveComparison()
        .ignoringFields("changeId")
        .isEqualTo(AddChange.buildOperationsChange(
            List.of(withOutputs(laterSurrender, THIRD_FEATURE_ID)), 2));
  }

  @Test
  void commitPartialSurrender_whenAnEarlierChangeOnTheSamePositionHasASurrender_thenItIsLeftAlone() {
    var positionCorrection = addedPositionCorrection();
    var operation = fullSurrenderOf(SECOND_FEATURE_ID);
    when(licencePositionSpatialService.getBlockFeaturesGoingIntoChange(positionCorrection, null))
        .thenReturn(List.of(blockFeature(SECOND_FEATURE_ID, 2), blockFeature(THIRD_FEATURE_ID, 3)));
    var editedChange = AddChange.buildOperationsChange(List.of(operation), 2);
    when(licencePositionCorrectionService
        .replaceAddChangeFor(eq(positionCorrection), eq(PartialSurrenderOperation.class), anyList()))
        .thenReturn(updatePositionCorrectionFor(LICENCE_POSITION, List.of(editedChange)));
    var earlierSurrender = staleFullSurrenderOf(FIRST_FEATURE_ID);
    var earlierChange = AddChange.buildOperationsChange(List.of(earlierSurrender), 1);
    when(licencePositionViewService
        .getCorrectedChronologicalPositions(LICENCE_CORRECTION, positionId(positionCorrection)))
        .thenReturn(List.of(chronologicalPositionWith(
            positionId(positionCorrection),
            positionChange(earlierChange.changeId(), 1, earlierSurrender),
            positionChange(editedChange.changeId(), 2, operation))));

    partialSurrenderCorrectionService.commitPartialSurrender(positionCorrection, operation);

    verify(licencePositionCorrectionService, never())
        .getPositionCorrectionContainingChange(LICENCE_CORRECTION, earlierChange.changeId());
  }

  @Test
  void removeExistingPartialSurrender_whenALaterSurrenderExists_thenItsOutputsAreRestaged() {
    var laterSurrender = staleFullSurrenderOf(SECOND_FEATURE_ID);
    var laterPositionCorrection = updatePositionCorrectionFor(LATER_POSITION, List.of());
    when(licencePositionViewService.getCorrectedChronologicalPositions(LICENCE_CORRECTION, LICENCE_POSITION.getId()))
        .thenReturn(List.of(
            ChronologicalPositionTestUtil.newBuilder().withId(LICENCE_POSITION.getId()).build(),
            laterChronologicalPosition(LATER_LIVE_CHANGE_ID, null, laterSurrender)));
    givenLaterLiveChange(laterSurrender);
    when(licencePositionCorrectionService.getOrBuildUpdatePositionCorrection(LICENCE_CORRECTION, LATER_POSITION))
        .thenReturn(laterPositionCorrection);
    when(licencePositionSpatialService
        .getBlockFeaturesGoingIntoChange(LICENCE_CORRECTION, LATER_POSITION.getId(), LATER_LIVE_CHANGE_ID))
        .thenReturn(List.of(blockFeature(SECOND_FEATURE_ID, 2), blockFeature(THIRD_FEATURE_ID, 3)));

    partialSurrenderCorrectionService
        .removeExistingPartialSurrender(LICENCE_POSITION, LICENCE_CORRECTION, LIVE_CHANGE_ID);

    assertThat(laterPositionCorrection.getPayload().changes()).containsExactly(
        UpdateChangeOperations.buildUpdateChange(
            LATER_LIVE_CHANGE_ID, withOutputs(laterSurrender, THIRD_FEATURE_ID)));
  }

  private void givenLaterExecutedSurrender(
      LicencePositionCorrection positionCorrection,
      PartialSurrenderOperation laterSurrender,
      LicencePositionCorrection laterPositionCorrection
  ) {
    when(licencePositionViewService.getCorrectedChronologicalPositions(LICENCE_CORRECTION, positionId(positionCorrection)))
        .thenReturn(List.of(
            ChronologicalPositionTestUtil.newBuilder().withId(positionId(positionCorrection)).build(),
            laterChronologicalPosition(LATER_LIVE_CHANGE_ID, null, laterSurrender)));
    givenLaterLiveChange(laterSurrender);

    if (laterPositionCorrection != null) {
      when(licencePositionCorrectionService.getOrBuildUpdatePositionCorrection(LICENCE_CORRECTION, LATER_POSITION))
          .thenReturn(laterPositionCorrection);
    }
  }

  private void givenLaterLiveChange(PartialSurrenderOperation laterSurrender) {
    when(licencePositionChangeService.findByLicencePositionId(LATER_POSITION.getId()))
        .thenReturn(List.of(LicencePositionChangeTestUtil.newBuilder()
            .withId(UUID.fromString(LATER_LIVE_CHANGE_ID))
            .withLicencePosition(LATER_POSITION)
            .withOperations(List.of(laterSurrender))
            .build()));
  }

  private static ChronologicalPosition chronologicalPositionWith(UUID positionId, PositionChange... changes) {
    return ChronologicalPositionTestUtil.newBuilder()
        .withId(positionId)
        .withChanges(List.of(changes))
        .build();
  }

  private static PositionChange positionChange(String changeId, int changeOrder, PartialSurrenderOperation surrender) {
    return new PositionChange(changeId, changeOrder, LicencePositionChangeType.ADD_CHANGE, List.of(surrender));
  }

  private static ChronologicalPosition laterChronologicalPosition(
      String changeId,
      String changeType,
      PartialSurrenderOperation laterSurrender
  ) {
    return ChronologicalPositionTestUtil.newBuilder()
        .withId(LATER_POSITION.getId())
        .withChanges(List.of(new PositionChange(changeId, 1, changeType, List.of(laterSurrender))))
        .build();
  }

  private static LicencePositionCorrection addedPositionCorrection() {
    return LicencePositionCorrectionTestUtil.newBuilder()
        .withLicenceCorrection(LICENCE_CORRECTION)
        .withTargetLicencePosition(null)
        .withPayload(CreateLicencePositionPayloadTestUtil.newBuilder().build())
        .build();
  }

  private static LicencePositionCorrection updatePositionCorrectionFor(
      LicencePosition licencePosition,
      List<LicencePositionChangeType> changes
  ) {
    return LicencePositionCorrectionTestUtil.newBuilder()
        .withLicenceCorrection(LICENCE_CORRECTION)
        .withChangeType(LicencePositionCorrectionChangeType.UPDATE_POSITION)
        .withTargetLicencePosition(licencePosition)
        .withPayload(UpdateLicencePositionPayloadTestUtil.newBuilder().withChanges(changes).build())
        .build();
  }

  private static UUID positionId(LicencePositionCorrection licencePositionCorrection) {
    return licencePositionCorrection.getPositionId();
  }

  private static Feature blockFeature(UUID featureId, int shapeNumber) {
    return FeatureTestUtil.blockFeature(featureId, "30", shapeNumber);
  }

  private static PartialSurrenderOperation fullSurrenderOf(UUID featureId) {
    return LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(featureId))
        .withSurrenderDetails(Map.of(featureId, surrenderDetails(BlockSurrenderType.FULL_SURRENDER, FIRST_COMMAND_JOURNEY_ID)))
        .build();
  }

  private static PartialSurrenderOperation staleFullSurrenderOf(UUID featureId) {
    return withOutputs(fullSurrenderOf(featureId), FIRST_FEATURE_ID, THIRD_FEATURE_ID);
  }

  private static PartialSurrenderOperation withOutputs(PartialSurrenderOperation operation, UUID... outputFeatureIds) {
    return LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderDate(operation.surrenderDate())
        .withSurrenderedFeatureIds(operation.surrenderedFeatureIds())
        .withSurrenderDetails(operation.featureIdToSurrenderDetails())
        .withOutputFeatureIds(List.of(outputFeatureIds))
        .build();
  }
}
