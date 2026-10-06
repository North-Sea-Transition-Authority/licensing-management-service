package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
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
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaDetails;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaSurrenderOutcome;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.PartialSurrenderChangeView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.feature.FeatureTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.spatial.LicencePositionSpatialService;

@ExtendWith(MockitoExtension.class)
class PartialSurrenderCorrectionServiceTest {

  private static final Licence LICENCE = LicenceTestUtil.builder().withLicenceType(LicenceType.CARBON_STORAGE).build();
  private static final LicenceCorrection LICENCE_CORRECTION = LicenceCorrectionTestUtil.newBuilder()
      .withLicence(LICENCE)
      .build();
  private static final LicencePosition LICENCE_POSITION = LicencePositionTestUtil.newBuilder()
      .withLicence(LICENCE)
      .build();
  private static final UUID FIRST_FEATURE_ID = UUID.randomUUID();
  private static final UUID SECOND_FEATURE_ID = UUID.randomUUID();
  private static final UUID THIRD_FEATURE_ID = UUID.randomUUID();
  private static final UUID FOURTH_FEATURE_ID = UUID.randomUUID();
  private static final UUID FIRST_COMMAND_JOURNEY_ID = UUID.randomUUID();
  private static final UUID SECOND_COMMAND_JOURNEY_ID = UUID.randomUUID();
  private static final UUID THIRD_COMMAND_JOURNEY_ID = UUID.randomUUID();
  private static final String LIVE_CHANGE_ID = UUID.randomUUID().toString();
  private static final LicencePosition LATER_POSITION = LicencePositionTestUtil.newBuilder()
      .withLicence(LICENCE)
      .build();
  private static final int ADMINISTRATOR_ID = 55;
  private static final LicenceCorrection PRODUCTION_CORRECTION = LicenceCorrectionTestUtil.newBuilder()
      .withLicence(LicenceTestUtil.builder().withLicenceType(LicenceType.SEAWARD_PRODUCTION).build())
      .build();
  private static final SubareaDetails SUBAREA = new SubareaDetails(UUID.randomUUID(), "Subarea A", "A");

  @Mock
  private LicencePositionCorrectionService licencePositionCorrectionService;

  @Mock
  private LicencePositionSpatialService licencePositionSpatialService;

  @Mock
  private LicencePositionChangeService licencePositionChangeService;

  @Mock
  private FeatureService featureService;

  @Mock
  private CommandJourneyService commandJourneyService;

  @Mock
  private PartialSurrenderSubareaService partialSurrenderSubareaService;

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
        .withLicenceCorrection(LICENCE_CORRECTION)
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
        .withLicenceCorrection(LICENCE_CORRECTION)
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
  void findSingleBlockNotOperatedOn_whenOneBlockNotOperatedOn_thenTheBlock() {
    var positionCorrection = executedPositionCorrection();
    var block = FeatureTestUtil.builder().withId(FIRST_FEATURE_ID).build();

    when(licencePositionSpatialService.getBlockFeaturesGoingIntoChange(
        LICENCE_CORRECTION,
        LICENCE_POSITION,
        LIVE_CHANGE_ID
    )).thenReturn(List.of(block));
    when(licencePositionCorrectionService.blockFeatureIdsAlreadyOperatedOnForExecutedPosition(
        LICENCE_CORRECTION,
        LICENCE_POSITION,
        positionCorrection,
        LIVE_CHANGE_ID
    )).thenReturn(Set.of(SECOND_FEATURE_ID));

    var result = partialSurrenderCorrectionService.findSingleBlockNotOperatedOn(
        LICENCE_CORRECTION, LICENCE_POSITION, positionCorrection, LIVE_CHANGE_ID);

    assertThat(result).contains(block);
  }

  @Test
  void findSingleBlockNotOperatedOn_whenNotExactlyOneBlock_thenEmpty() {
    when(licencePositionSpatialService.getBlockFeaturesGoingIntoChange(LICENCE_CORRECTION, LICENCE_POSITION, null))
        .thenReturn(List.of(
            FeatureTestUtil.builder().withId(FIRST_FEATURE_ID).build(),
            FeatureTestUtil.builder().withId(SECOND_FEATURE_ID).build()
        ));
    when(licencePositionCorrectionService.blockFeatureIdsAlreadyOperatedOnForExecutedPosition(
        LICENCE_CORRECTION,
        LICENCE_POSITION,
        null,
        null
    )).thenReturn(Set.of());

    var result = partialSurrenderCorrectionService.findSingleBlockNotOperatedOn(
        LICENCE_CORRECTION,
        LICENCE_POSITION,
        null,
        null
    );

    assertThat(result).isEmpty();
  }

  @Test
  void findSingleBlockNotOperatedOn_whenTheOnlyBlockIsOperatedOn_thenEmpty() {
    when(licencePositionSpatialService.getBlockFeaturesGoingIntoChange(LICENCE_CORRECTION, LICENCE_POSITION, null))
        .thenReturn(List.of(FeatureTestUtil.builder().withId(FIRST_FEATURE_ID).build()));
    when(licencePositionCorrectionService.blockFeatureIdsAlreadyOperatedOnForExecutedPosition(
        LICENCE_CORRECTION,
        LICENCE_POSITION,
        null,
        null
    )).thenReturn(Set.of(FIRST_FEATURE_ID));

    var result = partialSurrenderCorrectionService.findSingleBlockNotOperatedOn(
        LICENCE_CORRECTION, LICENCE_POSITION, null, null);

    assertThat(result).isEmpty();
  }

  @Test
  void findSingleBlockNotOperatedOn_whenUpdatePosition_thenChecksTheExecutedPositionGoingIntoTheStagedChange() {
    var stagedChange = AddChange.buildOperationsChange(List.of(partialSurrender(FIRST_FEATURE_ID)), 1);
    var positionCorrection = updatePositionCorrection(List.of(stagedChange));
    var block = FeatureTestUtil.builder().withId(FIRST_FEATURE_ID).build();

    when(licencePositionSpatialService.getBlockFeaturesGoingIntoChange(
        LICENCE_CORRECTION, LICENCE_POSITION, stagedChange.changeId()))
        .thenReturn(List.of(block));
    when(licencePositionCorrectionService.blockFeatureIdsAlreadyOperatedOnForExecutedPosition(
        LICENCE_CORRECTION,
        LICENCE_POSITION,
        positionCorrection,
        stagedChange.changeId()
    )).thenReturn(Set.of());

    var result = partialSurrenderCorrectionService.findSingleBlockNotOperatedOn(positionCorrection);

    assertThat(result).contains(block);
  }

  @Test
  void findSingleBlockNotOperatedOn_whenAddedPosition_thenChecksTheAddedPositionGoingIntoTheStagedChange() {
    var stagedChange = AddChange.buildOperationsChange(List.of(partialSurrender(FIRST_FEATURE_ID)), 1);
    var positionCorrection = positionCorrection(List.of(stagedChange));
    var block = FeatureTestUtil.builder().withId(FIRST_FEATURE_ID).build();

    when(licencePositionSpatialService.getBlockFeaturesGoingIntoChange(positionCorrection, stagedChange.changeId()))
        .thenReturn(List.of(block));
    when(licencePositionCorrectionService.blockFeatureIdsAlreadyOperatedOnForAddedPosition(
        positionCorrection,
        stagedChange.changeId()
    )).thenReturn(Set.of());

    var result = partialSurrenderCorrectionService.findSingleBlockNotOperatedOn(positionCorrection);

    assertThat(result).contains(block);
  }

  @Test
  void findSingleBlockNotOperatedOn_whenRemovePosition_thenThrows() {
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withLicenceCorrection(LICENCE_CORRECTION)
        .withChangeType(LicencePositionCorrectionChangeType.REMOVE_POSITION)
        .build();

    assertThatThrownBy(() -> partialSurrenderCorrectionService.findSingleBlockNotOperatedOn(positionCorrection))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Licence position correction %s removes a position so cannot carry a partial surrender"
            .formatted(positionCorrection.getId()));
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
  void stageSingleBlockSurrenderForExecutedPosition_whenOneBlock_thenStagesAPartialSurrenderOfIt() {
    var block = FeatureTestUtil.builder().withId(FIRST_FEATURE_ID).build();
    var positionCorrection = updatePositionCorrection(List.of());
    var committedPositionCorrection = updatePositionCorrection(List.of());
    var expectedOperation = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID))
        .withSurrenderDetails(Map.of(FIRST_FEATURE_ID,
            surrenderDetails(BlockSurrenderType.PARTIAL_SURRENDER, FIRST_COMMAND_JOURNEY_ID)))
        .build();
    when(licencePositionCorrectionService.findUpdatePositionCorrection(LICENCE_CORRECTION, LICENCE_POSITION))
        .thenReturn(Optional.empty());
    when(licencePositionSpatialService.getBlockFeaturesGoingIntoChange(LICENCE_CORRECTION, LICENCE_POSITION, null))
        .thenReturn(List.of(block));
    when(licencePositionCorrectionService.blockFeatureIdsAlreadyOperatedOnForExecutedPosition(
        LICENCE_CORRECTION, LICENCE_POSITION, null, null))
        .thenReturn(Set.of());
    when(licencePositionCorrectionService.getCommittedChangeOfType(null, PartialSurrenderOperation.class))
        .thenReturn(Optional.empty());
    when(licencePositionCorrectionService.getOrBuildUpdatePositionCorrection(LICENCE_CORRECTION, LICENCE_POSITION))
        .thenReturn(positionCorrection);
    when(licencePositionCorrectionService.getCommittedChangeOfType(positionCorrection, PartialSurrenderOperation.class))
        .thenReturn(Optional.empty());
    givenCommandJourneyCreatedFor(block, FIRST_COMMAND_JOURNEY_ID);
    when(licencePositionCorrectionService.replaceAddChangeFor(
        positionCorrection, PartialSurrenderOperation.class, List.of(expectedOperation)))
        .thenReturn(committedPositionCorrection);

    var result = partialSurrenderCorrectionService.stageSingleBlockSurrenderForExecutedPosition(
        LICENCE_CORRECTION, LICENCE_POSITION);

    assertThat(result).contains(new SingleBlockSurrender(committedPositionCorrection, block));
    verify(licencePositionCorrectionService).replaceAddChangeFor(
        eq(positionCorrection), eq(PartialSurrenderOperation.class), anyList());
  }

  @Test
  void stageSingleBlockSurrenderForExecutedPosition_whenTheBlockIsAlreadyPartiallySurrendered_thenReusesItsJourney() {
    var block = FeatureTestUtil.builder().withId(FIRST_FEATURE_ID).build();
    var stagedDetails = new PartialSurrenderOperation.SurrenderDetails(
        BlockSurrenderType.PARTIAL_SURRENDER, FIRST_COMMAND_JOURNEY_ID, List.of(SECOND_FEATURE_ID));
    var staged = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID))
        .withSurrenderDetails(Map.of(FIRST_FEATURE_ID, stagedDetails))
        .build();
    var stagedChange = AddChange.buildOperationsChange(List.of(staged), 1);
    var positionCorrection = updatePositionCorrection(List.of(stagedChange));
    var expectedOperation = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID))
        .withSurrenderDetails(Map.of(FIRST_FEATURE_ID, stagedDetails))
        .build();
    when(licencePositionCorrectionService.findUpdatePositionCorrection(LICENCE_CORRECTION, LICENCE_POSITION))
        .thenReturn(Optional.of(positionCorrection));
    when(licencePositionSpatialService.getBlockFeaturesGoingIntoChange(
        LICENCE_CORRECTION, LICENCE_POSITION, stagedChange.changeId()))
        .thenReturn(List.of(block));
    when(licencePositionCorrectionService.blockFeatureIdsAlreadyOperatedOnForExecutedPosition(
        LICENCE_CORRECTION, LICENCE_POSITION, positionCorrection, stagedChange.changeId()))
        .thenReturn(Set.of());
    givenCommittedPartialSurrender(positionCorrection, staged);
    when(licencePositionCorrectionService.getOrBuildUpdatePositionCorrection(LICENCE_CORRECTION, LICENCE_POSITION))
        .thenReturn(positionCorrection);
    when(licencePositionCorrectionService.replaceAddChangeFor(
        positionCorrection, PartialSurrenderOperation.class, List.of(expectedOperation)))
        .thenReturn(positionCorrection);

    var result = partialSurrenderCorrectionService.stageSingleBlockSurrenderForExecutedPosition(
        LICENCE_CORRECTION, LICENCE_POSITION);

    assertThat(result).contains(new SingleBlockSurrender(positionCorrection, block));
    verifyNoInteractions(commandJourneyService);
  }

  @Test
  void stageSingleBlockSurrenderForExecutedPosition_whenMoreThanOneBlock_thenStagesNothing() {
    when(licencePositionCorrectionService.findUpdatePositionCorrection(LICENCE_CORRECTION, LICENCE_POSITION))
        .thenReturn(Optional.empty());
    when(licencePositionSpatialService.getBlockFeaturesGoingIntoChange(LICENCE_CORRECTION, LICENCE_POSITION, null))
        .thenReturn(List.of(
            FeatureTestUtil.builder().withId(FIRST_FEATURE_ID).build(),
            FeatureTestUtil.builder().withId(SECOND_FEATURE_ID).build()
        ));
    when(licencePositionCorrectionService.blockFeatureIdsAlreadyOperatedOnForExecutedPosition(
        LICENCE_CORRECTION, LICENCE_POSITION, null, null))
        .thenReturn(Set.of());

    var result = partialSurrenderCorrectionService.stageSingleBlockSurrenderForExecutedPosition(
        LICENCE_CORRECTION, LICENCE_POSITION);

    assertThat(result).isEmpty();
    verify(licencePositionCorrectionService, never()).replaceAddChangeFor(any(), any(), anyList());
    verifyNoInteractions(commandJourneyService);
  }

  @Test
  void stageSingleBlockCorrectionOfLiveChange_whenNothingStaged_thenStagesAnUpdateWithANewJourney() {
    var block = FeatureTestUtil.builder().withId(FIRST_FEATURE_ID).build();
    var positionCorrection = updatePositionCorrection(List.of());
    var expectedOperation = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID))
        .withSurrenderDetails(Map.of(FIRST_FEATURE_ID,
            surrenderDetails(BlockSurrenderType.PARTIAL_SURRENDER, FIRST_COMMAND_JOURNEY_ID)))
        .build();
    when(licencePositionCorrectionService.findUpdatePositionCorrection(LICENCE_CORRECTION, LICENCE_POSITION))
        .thenReturn(Optional.empty());
    when(licencePositionSpatialService.getBlockFeaturesGoingIntoChange(
        LICENCE_CORRECTION, LICENCE_POSITION, LIVE_CHANGE_ID))
        .thenReturn(List.of(block));
    when(licencePositionCorrectionService.blockFeatureIdsAlreadyOperatedOnForExecutedPosition(
        LICENCE_CORRECTION, LICENCE_POSITION, null, LIVE_CHANGE_ID))
        .thenReturn(Set.of());
    when(licencePositionCorrectionService.getCommittedChangeOfType(null, PartialSurrenderOperation.class))
        .thenReturn(Optional.empty());
    givenCommandJourneyCreatedFor(block, FIRST_COMMAND_JOURNEY_ID);
    givenPositionCorrection(positionCorrection);

    var result = partialSurrenderCorrectionService.stageSingleBlockCorrectionOfLiveChange(
        LICENCE_CORRECTION, LICENCE_POSITION, LIVE_CHANGE_ID);

    assertThat(result).contains(new SingleBlockSurrender(positionCorrection, block));
    assertThat(positionCorrection.getPayload().changes())
        .containsExactly(UpdateChangeOperations.buildUpdateChange(LIVE_CHANGE_ID, expectedOperation));
    verifyNoInteractions(licencePositionChangeService);
  }

  @Test
  void stageSingleBlockCorrectionOfLiveChange_whenAlreadyStaged_thenReusesTheStagedJourney() {
    var block = FeatureTestUtil.builder().withId(FIRST_FEATURE_ID).build();
    var stagedDetails = new PartialSurrenderOperation.SurrenderDetails(
        BlockSurrenderType.PARTIAL_SURRENDER, FIRST_COMMAND_JOURNEY_ID, List.of(SECOND_FEATURE_ID));
    var staged = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID))
        .withSurrenderDetails(Map.of(FIRST_FEATURE_ID, stagedDetails))
        .build();
    var positionCorrection = updatePositionCorrection(
        List.of(UpdateChangeOperations.buildUpdateChange(LIVE_CHANGE_ID, staged)));
    when(licencePositionCorrectionService.findUpdatePositionCorrection(LICENCE_CORRECTION, LICENCE_POSITION))
        .thenReturn(Optional.of(positionCorrection));
    when(licencePositionSpatialService.getBlockFeaturesGoingIntoChange(
        LICENCE_CORRECTION, LICENCE_POSITION, LIVE_CHANGE_ID))
        .thenReturn(List.of(block));
    when(licencePositionCorrectionService.blockFeatureIdsAlreadyOperatedOnForExecutedPosition(
        LICENCE_CORRECTION, LICENCE_POSITION, positionCorrection, LIVE_CHANGE_ID))
        .thenReturn(Set.of());
    givenCommittedPartialSurrender(positionCorrection, staged);
    givenPositionCorrection(positionCorrection);

    var result = partialSurrenderCorrectionService.stageSingleBlockCorrectionOfLiveChange(
        LICENCE_CORRECTION, LICENCE_POSITION, LIVE_CHANGE_ID);

    assertThat(result).contains(new SingleBlockSurrender(positionCorrection, block));
    assertThat(positionCorrection.getPayload().changes())
        .containsExactly(UpdateChangeOperations.buildUpdateChange(LIVE_CHANGE_ID, staged));
    verifyNoInteractions(commandJourneyService);
  }

  @Test
  void stageSingleBlockCorrectionOfLiveChange_whenMoreThanOneBlock_thenStagesNothing() {
    when(licencePositionCorrectionService.findUpdatePositionCorrection(LICENCE_CORRECTION, LICENCE_POSITION))
        .thenReturn(Optional.empty());
    when(licencePositionSpatialService.getBlockFeaturesGoingIntoChange(
        LICENCE_CORRECTION, LICENCE_POSITION, LIVE_CHANGE_ID))
        .thenReturn(List.of(
            FeatureTestUtil.builder().withId(FIRST_FEATURE_ID).build(),
            FeatureTestUtil.builder().withId(SECOND_FEATURE_ID).build()
        ));
    when(licencePositionCorrectionService.blockFeatureIdsAlreadyOperatedOnForExecutedPosition(
        LICENCE_CORRECTION, LICENCE_POSITION, null, LIVE_CHANGE_ID))
        .thenReturn(Set.of());

    var result = partialSurrenderCorrectionService.stageSingleBlockCorrectionOfLiveChange(
        LICENCE_CORRECTION, LICENCE_POSITION, LIVE_CHANGE_ID);

    assertThat(result).isEmpty();
    verify(licencePositionCorrectionService, never()).save(any());
    verifyNoInteractions(commandJourneyService);
  }

  @Test
  void stageSingleBlockSurrenderForAddedPosition_whenOneBlock_thenStagesAPartialSurrenderOfIt() {
    var block = FeatureTestUtil.builder().withId(FIRST_FEATURE_ID).build();
    var positionCorrection = positionCorrection();
    var committedPositionCorrection = positionCorrection();
    var expectedOperation = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID))
        .withSurrenderDetails(Map.of(FIRST_FEATURE_ID,
            surrenderDetails(BlockSurrenderType.PARTIAL_SURRENDER, FIRST_COMMAND_JOURNEY_ID)))
        .build();
    when(licencePositionSpatialService.getBlockFeaturesGoingIntoChange(positionCorrection, null))
        .thenReturn(List.of(block));
    when(licencePositionCorrectionService.blockFeatureIdsAlreadyOperatedOnForAddedPosition(positionCorrection, null))
        .thenReturn(Set.of());
    when(licencePositionCorrectionService.getCommittedChangeOfType(positionCorrection, PartialSurrenderOperation.class))
        .thenReturn(Optional.empty());
    givenCommandJourneyCreatedFor(block, FIRST_COMMAND_JOURNEY_ID);
    when(licencePositionCorrectionService.replaceAddChangeFor(
        positionCorrection, PartialSurrenderOperation.class, List.of(expectedOperation)))
        .thenReturn(committedPositionCorrection);

    var result = partialSurrenderCorrectionService.stageSingleBlockSurrenderForAddedPosition(positionCorrection);

    assertThat(result).contains(new SingleBlockSurrender(committedPositionCorrection, block));
    verify(licencePositionCorrectionService).replaceAddChangeFor(
        eq(positionCorrection), eq(PartialSurrenderOperation.class), anyList());
  }

  @Test
  void stageSingleBlockSurrenderForAddedPosition_whenTheOnlyBlockIsOperatedOn_thenStagesNothing() {
    var positionCorrection = positionCorrection();
    when(licencePositionSpatialService.getBlockFeaturesGoingIntoChange(positionCorrection, null))
        .thenReturn(List.of(FeatureTestUtil.builder().withId(FIRST_FEATURE_ID).build()));
    when(licencePositionCorrectionService.blockFeatureIdsAlreadyOperatedOnForAddedPosition(positionCorrection, null))
        .thenReturn(Set.of(FIRST_FEATURE_ID));

    var result = partialSurrenderCorrectionService.stageSingleBlockSurrenderForAddedPosition(positionCorrection);

    assertThat(result).isEmpty();
    verify(licencePositionCorrectionService, never()).replaceAddChangeFor(any(), any(), anyList());
    verifyNoInteractions(commandJourneyService);
  }

  private void givenCommandJourneyCreatedFor(Feature block, UUID commandJourneyId) {
    when(featureService.getFeatureOrThrow(block.getId())).thenReturn(block);
    var commandJourney = new CommandJourney();
    commandJourney.setId(commandJourneyId);
    when(commandJourneyService.createAndAssignCommandJourney(List.of(block))).thenReturn(commandJourney);
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
  void correctExistingPartialSurrender_whenAlreadyCorrected_deletesTheStagedJourneysNoLongerHeld() {
    var staged = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID, SECOND_FEATURE_ID))
        .withSurrenderDetails(Map.of(
            FIRST_FEATURE_ID, surrenderDetails(BlockSurrenderType.PARTIAL_SURRENDER, FIRST_COMMAND_JOURNEY_ID),
            SECOND_FEATURE_ID, surrenderDetails(BlockSurrenderType.PARTIAL_SURRENDER, SECOND_COMMAND_JOURNEY_ID)))
        .build();
    var positionCorrection = updatePositionCorrection(
        List.of(UpdateChangeOperations.buildUpdateChange(LIVE_CHANGE_ID, staged)));
    var corrected = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID, THIRD_FEATURE_ID))
        .withSurrenderDetails(Map.of(
            FIRST_FEATURE_ID, surrenderDetails(BlockSurrenderType.PARTIAL_SURRENDER, FIRST_COMMAND_JOURNEY_ID),
            THIRD_FEATURE_ID, surrenderDetails(BlockSurrenderType.PARTIAL_SURRENDER, THIRD_COMMAND_JOURNEY_ID)))
        .build();
    givenPositionCorrection(positionCorrection);
    givenCommittedPartialSurrender(positionCorrection, staged);

    partialSurrenderCorrectionService.correctExistingPartialSurrender(
        LICENCE_CORRECTION, LICENCE_POSITION, LIVE_CHANGE_ID, corrected);

    verify(commandJourneyService).deleteCommandJourney(SECOND_COMMAND_JOURNEY_ID);
    verify(commandJourneyService, never()).deleteCommandJourney(FIRST_COMMAND_JOURNEY_ID);
    verify(commandJourneyService, never()).deleteCommandJourney(THIRD_COMMAND_JOURNEY_ID);
  }

  @Test
  void correctExistingPartialSurrender_whenNothingStaged_deletesNoJourneys() {
    var positionCorrection = updatePositionCorrection(List.of());
    givenPositionCorrection(positionCorrection);

    partialSurrenderCorrectionService.correctExistingPartialSurrender(
        LICENCE_CORRECTION, LICENCE_POSITION, LIVE_CHANGE_ID, partialSurrender(FIRST_FEATURE_ID));

    verify(commandJourneyService, never()).deleteCommandJourney(any());
  }

  @Test
  void revertPartialSurrenderCorrection_whenNoPositionCorrection_correctsNothing() {
    when(licencePositionCorrectionService.findUpdatePositionCorrection(LICENCE_CORRECTION, LICENCE_POSITION))
        .thenReturn(Optional.empty());

    partialSurrenderCorrectionService.revertPartialSurrenderCorrection(
        LICENCE_CORRECTION, LICENCE_POSITION, partialSurrender(FIRST_FEATURE_ID));

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

    partialSurrenderCorrectionService.revertPartialSurrenderCorrection(
        LICENCE_CORRECTION, LICENCE_POSITION, partialSurrender(FIRST_FEATURE_ID));

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

    partialSurrenderCorrectionService.revertPartialSurrenderCorrection(
        LICENCE_CORRECTION, LICENCE_POSITION, partialSurrender(FIRST_FEATURE_ID));

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

    partialSurrenderCorrectionService.revertPartialSurrenderCorrection(
        LICENCE_CORRECTION, LICENCE_POSITION, partialSurrender(FIRST_FEATURE_ID));

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
  void adjustPartialSurrenderBlocks_whenTheSurrenderIsStagedAsAnUpdate_thenReplacesItInPlaceKeepingItsChangeId() {
    var surrender = partialSurrender(FIRST_FEATURE_ID, SECOND_FEATURE_ID);
    var stagedChange = UpdateChangeOperations.buildUpdateChange(LIVE_CHANGE_ID, surrender);
    var positionCorrection = executedPositionCorrection(List.of(stagedChange));
    givenCommittedPartialSurrender(positionCorrection, surrender);
    when(licencePositionSpatialService.getBlockFeaturesGoingIntoChange(positionCorrection, LIVE_CHANGE_ID))
        .thenReturn(List.of(FeatureTestUtil.builder().withId(FIRST_FEATURE_ID).build()));
    when(licencePositionCorrectionService.save(positionCorrection)).thenReturn(positionCorrection);

    partialSurrenderCorrectionService.adjustPartialSurrenderBlocks(positionCorrection);

    var expected = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderDate(surrender.surrenderDate())
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID))
        .build();
    verify(licencePositionCorrectionService, never())
        .replaceAddChangeFor(eq(positionCorrection), eq(PartialSurrenderOperation.class), anyList());
    assertThat(positionCorrection.getPayload().changes())
        .containsExactly(UpdateChangeOperations.buildUpdateChange(LIVE_CHANGE_ID, expected));
  }

  @Test
  void adjustPartialSurrenderBlocks_whenNoBlocksStillSurrenderableOnALiveSurrenderMovedHere_thenStagesItsRemovalOnItsLivePosition() {
    var surrender = partialSurrender(FIRST_FEATURE_ID, SECOND_FEATURE_ID);
    var positionCorrection = updatePositionCorrectionFor(
        LATER_POSITION,
        List.of(UpdateChangeOperations.buildUpdateChange(LIVE_CHANGE_ID, surrender))
    );
    givenCommittedPartialSurrender(positionCorrection, surrender);
    when(licencePositionSpatialService.getBlockFeaturesGoingIntoChange(positionCorrection, LIVE_CHANGE_ID))
        .thenReturn(List.of(FeatureTestUtil.builder().withId(THIRD_FEATURE_ID).build()));
    when(licencePositionChangeService.getByIdOrThrow(UUID.fromString(LIVE_CHANGE_ID)))
        .thenReturn(LicencePositionChangeTestUtil.newBuilder()
            .withId(UUID.fromString(LIVE_CHANGE_ID))
            .withLicencePosition(LICENCE_POSITION)
            .withOperations(List.of(surrender))
            .build());

    partialSurrenderCorrectionService.adjustPartialSurrenderBlocks(positionCorrection);

    verify(licencePositionCorrectionService).dropStagedChange(positionCorrection, LIVE_CHANGE_ID);
    verify(licencePositionCorrectionService).stageChangesOnPosition(
        LICENCE_CORRECTION,
        LICENCE_POSITION.getId(),
        List.of(LicencePositionChangeType.removeChange().withChangeId(LIVE_CHANGE_ID).build())
    );
    verify(licencePositionCorrectionService, never())
        .replaceAddChangeFor(eq(positionCorrection), eq(PartialSurrenderOperation.class), anyList());
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
  void undoPartialSurrenderChange_whenTheSurrenderCorrectsALiveChange_thenDeletesItsCommandJourneys() {
    var updateChange = UpdateChangeOperations.buildUpdateChange(
        LIVE_CHANGE_ID, twoBlockSurrenderOf(FIRST_FEATURE_ID, SECOND_FEATURE_ID));
    var positionCorrection = givenStagedChange(updateChange);

    partialSurrenderCorrectionService.undoPartialSurrenderChange(LICENCE_CORRECTION, updateChange.changeId());

    verify(commandJourneyService).deleteCommandJourney(FIRST_COMMAND_JOURNEY_ID);
    verify(commandJourneyService).deleteCommandJourney(SECOND_COMMAND_JOURNEY_ID);
    verify(licencePositionCorrectionService).dropStagedChange(positionCorrection, updateChange.changeId());
  }


  @Test
  void undoPartialSurrenderChange_whenTheSurrenderWasRemovedByThisCorrection_thenDeletesNoCommandJourneys() {
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

  @Test
  void getSurrenderedBlockFeatures() {
    var surrender = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(SECOND_FEATURE_ID, FIRST_FEATURE_ID))
        .build();
    var firstBlock = FeatureTestUtil.blockFeature(FIRST_FEATURE_ID, "30", 1);
    var secondBlock = FeatureTestUtil.blockFeature(SECOND_FEATURE_ID, "30", 2);
    when(featureService.getFeaturesByIds(List.of(SECOND_FEATURE_ID, FIRST_FEATURE_ID)))
        .thenReturn(List.of(secondBlock, firstBlock));

    var result = partialSurrenderCorrectionService.getSurrenderedBlockFeatures(surrender);

    assertThat(result).containsExactly(firstBlock, secondBlock);
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
  void revertPartialSurrenderCorrection_whenTheStagedSurrenderMintedItsOwnJourney_deletesOnlyThatJourney() {
    var stagedSurrender = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID))
        .withSurrenderDetails(Map.of(
            FIRST_FEATURE_ID, surrenderDetails(BlockSurrenderType.PARTIAL_SURRENDER, SECOND_COMMAND_JOURNEY_ID)))
        .build();
    var positionCorrection = updatePositionCorrection(
        List.of(UpdateChangeOperations.buildUpdateChange(LIVE_CHANGE_ID, stagedSurrender)));
    when(licencePositionCorrectionService.findUpdatePositionCorrection(LICENCE_CORRECTION, LICENCE_POSITION))
        .thenReturn(Optional.of(positionCorrection));
    givenCommittedPartialSurrender(positionCorrection, stagedSurrender);

    partialSurrenderCorrectionService.revertPartialSurrenderCorrection(
        LICENCE_CORRECTION, LICENCE_POSITION, stagedSurrender);

    verify(commandJourneyService).deleteCommandJourney(SECOND_COMMAND_JOURNEY_ID);
    verify(commandJourneyService, never()).deleteCommandJourney(FIRST_COMMAND_JOURNEY_ID);
  }


  @Test
  void revertPartialSurrenderCorrection_whenNothingWasStaged_deletesTheJourneyTheCorrectionCreated() {
    var discardedSurrender = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID))
        .withSurrenderDetails(Map.of(
            FIRST_FEATURE_ID, surrenderDetails(BlockSurrenderType.FULL_SURRENDER, SECOND_COMMAND_JOURNEY_ID)))
        .build();
    when(licencePositionCorrectionService.findUpdatePositionCorrection(LICENCE_CORRECTION, LICENCE_POSITION))
        .thenReturn(Optional.empty());

    partialSurrenderCorrectionService.revertPartialSurrenderCorrection(
        LICENCE_CORRECTION, LICENCE_POSITION, discardedSurrender);

    verify(commandJourneyService).deleteCommandJourney(SECOND_COMMAND_JOURNEY_ID);
    verify(commandJourneyService, never()).deleteCommandJourney(FIRST_COMMAND_JOURNEY_ID);
  }

  @Test
  void revertPartialSurrenderCorrection_whenTheStagedAndDiscardedSurrendersShareAJourney_deletesItOnce() {
    var stagedSurrender = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID))
        .withSurrenderDetails(Map.of(
            FIRST_FEATURE_ID, surrenderDetails(BlockSurrenderType.PARTIAL_SURRENDER, SECOND_COMMAND_JOURNEY_ID)))
        .build();
    var positionCorrection = updatePositionCorrection(
        List.of(UpdateChangeOperations.buildUpdateChange(LIVE_CHANGE_ID, stagedSurrender)));
    when(licencePositionCorrectionService.findUpdatePositionCorrection(LICENCE_CORRECTION, LICENCE_POSITION))
        .thenReturn(Optional.of(positionCorrection));
    givenCommittedPartialSurrender(positionCorrection, stagedSurrender);

    partialSurrenderCorrectionService.revertPartialSurrenderCorrection(
        LICENCE_CORRECTION, LICENCE_POSITION, stagedSurrender);

    verify(commandJourneyService, times(1)).deleteCommandJourney(SECOND_COMMAND_JOURNEY_ID);
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
  void setSurrenderedFeatureIds_whenTheSurrenderCorrectsALiveChange_thenTheUpdateChangeIsReplacedInPlace() {
    var surrenderedId = UUID.randomUUID();
    var existingOperation = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID))
        .withSurrenderDetails(Map.of(FIRST_FEATURE_ID, surrenderDetails(
            BlockSurrenderType.PARTIAL_SURRENDER, FIRST_COMMAND_JOURNEY_ID)))
        .build();
    var positionCorrection = updatePositionCorrection(
        List.of(UpdateChangeOperations.buildUpdateChange(LIVE_CHANGE_ID, existingOperation)));
    givenCommittedPartialSurrender(positionCorrection, existingOperation);

    partialSurrenderCorrectionService.setSurrenderedFeatureIds(
        positionCorrection, FIRST_FEATURE_ID, List.of(surrenderedId));

    var expectedOperation = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID))
        .withSurrenderDetails(Map.of(FIRST_FEATURE_ID, new PartialSurrenderOperation.SurrenderDetails(
            BlockSurrenderType.PARTIAL_SURRENDER, FIRST_COMMAND_JOURNEY_ID, List.of(surrenderedId))))
        .build();
    assertThat(positionCorrection.getPayload().changes())
        .containsExactly(UpdateChangeOperations.buildUpdateChange(LIVE_CHANGE_ID, expectedOperation));
    verify(licencePositionCorrectionService).save(positionCorrection);
    verify(licencePositionCorrectionService, never()).replaceAddChangeFor(any(), any(), any());
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
  void getOrCreatePartialSurrenderDetails_whenExistingDetailsHaveAJourney_reusesJourneyWithoutCreating() {
    var existingSurrenderDetails = new PartialSurrenderOperation.SurrenderDetails(
        BlockSurrenderType.PARTIAL_SURRENDER, FIRST_COMMAND_JOURNEY_ID, List.of(FIRST_FEATURE_ID));
    var operation = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID))
        .withSurrenderDetails(Map.of(FIRST_FEATURE_ID, existingSurrenderDetails))
        .build();

    var result = partialSurrenderCorrectionService.getOrCreatePartialSurrenderDetails(
        operation,
        FIRST_FEATURE_ID,
        BlockSurrenderType.PARTIAL_SURRENDER);

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
        operation,
        FIRST_FEATURE_ID,
        BlockSurrenderType.FULL_SURRENDER);

    verify(commandJourneyService, never()).createAndAssignCommandJourney(anyList());

    var expected = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID))
        .withSurrenderDetails(Map.of(FIRST_FEATURE_ID, new PartialSurrenderOperation.SurrenderDetails(
            BlockSurrenderType.FULL_SURRENDER, FIRST_COMMAND_JOURNEY_ID, List.of(FIRST_FEATURE_ID))))
        .build();
    assertThat(result).isEqualTo(expected);
  }





  @Test
  void getOrCreatePartialSurrenderDetails_whenExistingDetailsHaveNoJourney_createsANewJourney() {
    var operation = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID))
        .withSurrenderDetails(Map.of(FIRST_FEATURE_ID, new PartialSurrenderOperation.SurrenderDetails(
            BlockSurrenderType.PARTIAL_SURRENDER, null, List.of(SECOND_FEATURE_ID), List.of(THIRD_FEATURE_ID))))
        .build();
    var feature = FeatureTestUtil.builder().withId(FIRST_FEATURE_ID).build();
    var createdJourney = new CommandJourney();
    createdJourney.setId(FIRST_COMMAND_JOURNEY_ID);
    when(featureService.getFeatureOrThrow(FIRST_FEATURE_ID)).thenReturn(feature);
    when(commandJourneyService.createAndAssignCommandJourney(List.of(feature))).thenReturn(createdJourney);

    var result = partialSurrenderCorrectionService.getOrCreatePartialSurrenderDetails(
        operation, FIRST_FEATURE_ID, BlockSurrenderType.PARTIAL_SURRENDER);

    var expected = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID))
        .withSurrenderDetails(Map.of(FIRST_FEATURE_ID, new PartialSurrenderOperation.SurrenderDetails(
            BlockSurrenderType.PARTIAL_SURRENDER, FIRST_COMMAND_JOURNEY_ID, List.of())))
        .build();
    assertThat(result).isEqualTo(expected);
  }

  @Test
  void getRetainedFeatureIds_whenNoJourney_thenTheRecordedRetainedFeatures() {
    var surrenderDetails = new PartialSurrenderOperation.SurrenderDetails(
        BlockSurrenderType.PARTIAL_SURRENDER, null, List.of(SECOND_FEATURE_ID), List.of(THIRD_FEATURE_ID));

    var result = partialSurrenderCorrectionService.getRetainedFeatureIds(surrenderDetails);

    assertThat(result).containsExactly(THIRD_FEATURE_ID);
    verify(commandJourneyService, never()).getActiveFeatures(any(UUID.class));
  }

  @Test
  void getRetainedFeatureIds_whenNothingSurrenderedYet_thenEmpty() {
    var surrenderDetails = surrenderDetails(BlockSurrenderType.PARTIAL_SURRENDER, FIRST_COMMAND_JOURNEY_ID);

    var result = partialSurrenderCorrectionService.getRetainedFeatureIds(surrenderDetails);

    assertThat(result).isEmpty();
    verify(commandJourneyService, never()).getActiveFeatures(any(UUID.class));
  }

  @Test
  void getRetainedFeatureIds_whenPartsSurrendered_thenTheOtherActiveFeaturesOrderedByName() {
    var surrenderedPart = FeatureTestUtil.builder().withId(FIRST_FEATURE_ID).withFeatureName("SHAPE 1A").build();
    var laterRetainedPart = FeatureTestUtil.builder().withId(SECOND_FEATURE_ID).withFeatureName("SHAPE 1C").build();
    var earlierRetainedPart = FeatureTestUtil.builder().withId(THIRD_FEATURE_ID).withFeatureName("SHAPE 1B").build();
    var surrenderDetails = new PartialSurrenderOperation.SurrenderDetails(
        BlockSurrenderType.PARTIAL_SURRENDER, FIRST_COMMAND_JOURNEY_ID, List.of(FIRST_FEATURE_ID));
    when(commandJourneyService.getActiveFeatures(FIRST_COMMAND_JOURNEY_ID))
        .thenReturn(List.of(surrenderedPart, laterRetainedPart, earlierRetainedPart));

    var result = partialSurrenderCorrectionService.getRetainedFeatureIds(surrenderDetails);

    assertThat(result).containsExactly(THIRD_FEATURE_ID, SECOND_FEATURE_ID);
  }

  @Test
  void toExecutedSurrender_resolvesJourneysIntoRecordedRetainedFeatures() {
    var surrenderDate = LocalDate.of(2026, Month.AUGUST, 1);
    var splitPart = FeatureTestUtil.builder().withId(SECOND_FEATURE_ID).withFeatureName("SHAPE 1A").build();
    var retainedPart = FeatureTestUtil.builder().withId(THIRD_FEATURE_ID).withFeatureName("SHAPE 1B").build();
    var recordedDetails = new PartialSurrenderOperation.SurrenderDetails(
        BlockSurrenderType.FULL_SURRENDER, null, List.of(FOURTH_FEATURE_ID));
    var relinquishedSubarea = new SubareaDetails(UUID.randomUUID(), "Subarea A", "A");
    var retainedFeatureIdToSubareas = Map.of(THIRD_FEATURE_ID, List.of(
        SubareaSurrenderOutcome.relinquished(relinquishedSubarea),
        SubareaSurrenderOutcome.kept(new SubareaDetails(UUID.randomUUID(), "Subarea B", "B"))));
    var staged = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderDate(surrenderDate)
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID, FOURTH_FEATURE_ID))
        .withSurrenderDetails(Map.of(
            FIRST_FEATURE_ID, new PartialSurrenderOperation.SurrenderDetails(
                BlockSurrenderType.PARTIAL_SURRENDER, FIRST_COMMAND_JOURNEY_ID, List.of(SECOND_FEATURE_ID))
                .withSubareas(retainedFeatureIdToSubareas),
            FOURTH_FEATURE_ID, recordedDetails))
        .build();
    when(commandJourneyService.getActiveFeatures(FIRST_COMMAND_JOURNEY_ID)).thenReturn(List.of(splitPart, retainedPart));

    var result = partialSurrenderCorrectionService.toExecutedSurrender(staged);

    var expected = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderDate(surrenderDate)
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID, FOURTH_FEATURE_ID))
        .withSurrenderDetails(Map.of(
            FIRST_FEATURE_ID, new PartialSurrenderOperation.SurrenderDetails(
                BlockSurrenderType.PARTIAL_SURRENDER,
                null,
                List.of(SECOND_FEATURE_ID),
                List.of(THIRD_FEATURE_ID),
                retainedFeatureIdToSubareas),
            FOURTH_FEATURE_ID, recordedDetails))
        .build();
    assertThat(result).isEqualTo(expected);
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

  private static PartialSurrenderOperation fullSurrenderOf(UUID featureId) {
    return LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(featureId))
        .withSurrenderDetails(Map.of(featureId, surrenderDetails(BlockSurrenderType.FULL_SURRENDER, FIRST_COMMAND_JOURNEY_ID)))
        .build();
  }

  @Test
  void setSurrenderedFeatureIds_whenProductionLicence_thenTheBlocksSubareasAreProcessed() {
    var surrenderedPart = FeatureTestUtil.builder().withFeatureName("SHAPE 1A").build();
    var retainedPart = FeatureTestUtil.builder().withFeatureName("SHAPE 1B").build();
    var existingOperation = surrenderOf(FIRST_FEATURE_ID, new PartialSurrenderOperation.SurrenderDetails(
        BlockSurrenderType.PARTIAL_SURRENDER, FIRST_COMMAND_JOURNEY_ID, List.of()));
    var stagedChange = AddChange.buildOperationsChange(List.of(existingOperation), 1);
    var positionCorrection = productionPositionCorrection(List.of(stagedChange));
    givenCommittedPartialSurrender(positionCorrection, existingOperation);
    when(commandJourneyService.getActiveFeatures(FIRST_COMMAND_JOURNEY_ID)).thenReturn(List.of(surrenderedPart, retainedPart));
    var selectedDetails = new PartialSurrenderOperation.SurrenderDetails(
        BlockSurrenderType.PARTIAL_SURRENDER, FIRST_COMMAND_JOURNEY_ID, List.of(surrenderedPart.getId()));
    var processedDetails = selectedDetails.withSubareas(Map.of(THIRD_FEATURE_ID, List.of(SubareaSurrenderOutcome.relinquished(SUBAREA))));
    when(partialSurrenderSubareaService.processSubareas(
        PRODUCTION_CORRECTION,
        positionCorrection.getPositionId(),
        stagedChange.changeId(),
        FIRST_FEATURE_ID,
        selectedDetails,
        List.of(retainedPart.getId()),
        null
    )).thenReturn(processedDetails);

    partialSurrenderCorrectionService.setSurrenderedFeatureIds(
        positionCorrection, FIRST_FEATURE_ID, List.of(surrenderedPart.getId()));

    verify(licencePositionCorrectionService)
        .replaceAddChangeFor(eq(positionCorrection), eq(PartialSurrenderOperation.class), partialSurrenderOperationCaptor.capture());
    assertThat(partialSurrenderOperationCaptor.getValue()).containsExactly(surrenderOf(FIRST_FEATURE_ID, processedDetails));
  }

  @Test
  void commitPartialSurrender_whenABlockIsSurrenderedAsItWasBefore_thenItsStagedSubareasAreOfferedForReuse() {
    var stagedDetails = new PartialSurrenderOperation.SurrenderDetails(
        BlockSurrenderType.FULL_SURRENDER, FIRST_COMMAND_JOURNEY_ID, List.of(FIRST_FEATURE_ID))
        .withSubareas(Map.of(THIRD_FEATURE_ID, List.of(SubareaSurrenderOutcome.relinquished(SUBAREA))));
    var staged = surrenderOf(FIRST_FEATURE_ID, stagedDetails);
    var stagedChange = AddChange.buildOperationsChange(List.of(staged), 1);
    var positionCorrection = productionPositionCorrection(List.of(stagedChange));
    givenCommittedPartialSurrender(positionCorrection, staged);
    var resubmittedDetails = new PartialSurrenderOperation.SurrenderDetails(
        BlockSurrenderType.FULL_SURRENDER, FIRST_COMMAND_JOURNEY_ID, List.of(FIRST_FEATURE_ID));
    when(partialSurrenderSubareaService.processSubareas(
        PRODUCTION_CORRECTION,
        positionCorrection.getPositionId(),
        stagedChange.changeId(),
        FIRST_FEATURE_ID,
        resubmittedDetails,
        List.of(),
        stagedDetails
    )).thenReturn(stagedDetails);

    partialSurrenderCorrectionService.commitPartialSurrender(
        positionCorrection, surrenderOf(FIRST_FEATURE_ID, resubmittedDetails));

    verify(licencePositionCorrectionService)
        .replaceAddChangeFor(eq(positionCorrection), eq(PartialSurrenderOperation.class), partialSurrenderOperationCaptor.capture());
    assertThat(partialSurrenderOperationCaptor.getValue()).containsExactly(staged);
  }

  @Test
  void commitPartialSurrender_whenAStagedCroppedSubareaIsNoLongerReferenced_thenItIsDeleted() {
    var croppedFeature = FeatureTestUtil.builder().build();
    var staged = croppedSurrenderOf(FIRST_FEATURE_ID, croppedFeature);
    var stagedChange = AddChange.buildOperationsChange(List.of(staged), 1);
    var positionCorrection = productionPositionCorrection(List.of(stagedChange));
    givenCommittedPartialSurrender(positionCorrection, staged);
    var resubmittedDetails = new PartialSurrenderOperation.SurrenderDetails(
        BlockSurrenderType.FULL_SURRENDER, FIRST_COMMAND_JOURNEY_ID, List.of(FIRST_FEATURE_ID));
    when(partialSurrenderSubareaService.processSubareas(
        PRODUCTION_CORRECTION,
        positionCorrection.getPositionId(),
        stagedChange.changeId(),
        FIRST_FEATURE_ID,
        resubmittedDetails,
        List.of(),
        null
    )).thenReturn(resubmittedDetails.withSubareas(Map.of(THIRD_FEATURE_ID, List.of(SubareaSurrenderOutcome.relinquished(SUBAREA)))));
    when(featureService.getFeaturesByIds(Set.of(croppedFeature.getId()))).thenReturn(List.of(croppedFeature));

    partialSurrenderCorrectionService.commitPartialSurrender(
        positionCorrection, surrenderOf(FIRST_FEATURE_ID, resubmittedDetails));

    verify(featureService).deleteAll(List.of(croppedFeature));
  }

  @Test
  void commitPartialSurrender_whenCarbonStorageLicence_thenSubareasAreNotProcessed() {
    var positionCorrection = positionCorrection();

    partialSurrenderCorrectionService.commitPartialSurrender(positionCorrection, fullSurrenderOf(FIRST_FEATURE_ID));

    verifyNoInteractions(partialSurrenderSubareaService);
  }

  @Test
  void correctExistingPartialSurrender_whenNothingStagedAndABlockIsSurrenderedAsItIsLive_thenTheLiveSubareasAreOfferedForReuse() {
    var liveDetails = new PartialSurrenderOperation.SurrenderDetails(
        BlockSurrenderType.FULL_SURRENDER, null, List.of(FIRST_FEATURE_ID))
        .withSubareas(Map.of(THIRD_FEATURE_ID, List.of(SubareaSurrenderOutcome.relinquished(SUBAREA))));
    givenLiveSurrenderChange(surrenderOf(FIRST_FEATURE_ID, liveDetails));
    var positionCorrection = productionUpdatePositionCorrection(List.of());
    givenProductionPositionCorrection(positionCorrection);
    var correctedDetails = new PartialSurrenderOperation.SurrenderDetails(
        BlockSurrenderType.FULL_SURRENDER, FIRST_COMMAND_JOURNEY_ID, List.of(FIRST_FEATURE_ID));
    var processedDetails = correctedDetails.withSubareas(liveDetails.retainedFeatureIdToSubareas());
    when(partialSurrenderSubareaService.processSubareas(
        PRODUCTION_CORRECTION,
        LICENCE_POSITION.getId(),
        LIVE_CHANGE_ID,
        FIRST_FEATURE_ID,
        correctedDetails,
        List.of(),
        liveDetails
    )).thenReturn(processedDetails);

    partialSurrenderCorrectionService.correctExistingPartialSurrender(
        PRODUCTION_CORRECTION, LICENCE_POSITION, LIVE_CHANGE_ID, surrenderOf(FIRST_FEATURE_ID, correctedDetails));

    assertThat(positionCorrection.getPayload().changes())
        .containsExactly(UpdateChangeOperations.buildUpdateChange(
            LIVE_CHANGE_ID, surrenderOf(FIRST_FEATURE_ID, processedDetails)));
  }

  @Test
  void correctExistingPartialSurrender_whenAStagedCroppedSubareaIsAlsoLive_thenItIsNotDeleted() {
    var croppedSurrender = croppedSurrenderOf(FIRST_FEATURE_ID, FeatureTestUtil.builder().build());
    givenLiveSurrenderChange(croppedSurrender);
    var positionCorrection = productionUpdatePositionCorrection(
        List.of(UpdateChangeOperations.buildUpdateChange(LIVE_CHANGE_ID, croppedSurrender)));
    givenProductionPositionCorrection(positionCorrection);
    givenCommittedPartialSurrender(positionCorrection, croppedSurrender);
    var correctedDetails = new PartialSurrenderOperation.SurrenderDetails(
        BlockSurrenderType.FULL_SURRENDER, FIRST_COMMAND_JOURNEY_ID, List.of(FIRST_FEATURE_ID));
    when(partialSurrenderSubareaService.processSubareas(
        PRODUCTION_CORRECTION,
        LICENCE_POSITION.getId(),
        LIVE_CHANGE_ID,
        FIRST_FEATURE_ID,
        correctedDetails,
        List.of(),
        null
    )).thenReturn(correctedDetails.withSubareas(Map.of(THIRD_FEATURE_ID, List.of(SubareaSurrenderOutcome.relinquished(SUBAREA)))));

    partialSurrenderCorrectionService.correctExistingPartialSurrender(
        PRODUCTION_CORRECTION, LICENCE_POSITION, LIVE_CHANGE_ID, surrenderOf(FIRST_FEATURE_ID, correctedDetails));

    verify(featureService, never()).deleteAll(any());
  }

  @Test
  void setSurrenderedFeatureIds_whenAStagedCroppedSubareaIsAlsoLive_thenItIsNotDeleted() {
    var croppedSurrender = croppedSurrenderOf(FIRST_FEATURE_ID, FeatureTestUtil.builder().build());
    givenLiveSurrenderChange(croppedSurrender);
    var positionCorrection = productionUpdatePositionCorrection(
        List.of(UpdateChangeOperations.buildUpdateChange(LIVE_CHANGE_ID, croppedSurrender)));
    givenCommittedPartialSurrender(positionCorrection, croppedSurrender);
    var selectedDetails = new PartialSurrenderOperation.SurrenderDetails(
        BlockSurrenderType.PARTIAL_SURRENDER, null, List.of(SECOND_FEATURE_ID));
    when(partialSurrenderSubareaService.processSubareas(
        PRODUCTION_CORRECTION,
        positionCorrection.getPositionId(),
        LIVE_CHANGE_ID,
        FIRST_FEATURE_ID,
        selectedDetails,
        List.of(),
        null
    )).thenReturn(selectedDetails.withSubareas(Map.of(THIRD_FEATURE_ID, List.of(SubareaSurrenderOutcome.relinquished(SUBAREA)))));

    partialSurrenderCorrectionService.setSurrenderedFeatureIds(
        positionCorrection, FIRST_FEATURE_ID, List.of(SECOND_FEATURE_ID));

    verify(featureService, never()).deleteAll(any());
  }

  @Test
  void undoPartialSurrenderChange_whenTheSurrenderWasAddedByThisCorrection_thenDeletesItsCroppedSubareas() {
    var croppedFeature = FeatureTestUtil.builder().build();
    var addChange = AddChange.buildOperationsChange(List.of(croppedSurrenderOf(FIRST_FEATURE_ID, croppedFeature)), 1);
    givenStagedChange(addChange);
    when(featureService.getFeaturesByIds(Set.of(croppedFeature.getId()))).thenReturn(List.of(croppedFeature));

    partialSurrenderCorrectionService.undoPartialSurrenderChange(LICENCE_CORRECTION, addChange.changeId());

    verify(featureService).deleteAll(List.of(croppedFeature));
  }

  @Test
  void undoPartialSurrenderChange_whenTheSurrenderCorrectsALiveChange_thenKeepsTheLiveCroppedSubareas() {
    var croppedSurrender = croppedSurrenderOf(FIRST_FEATURE_ID, FeatureTestUtil.builder().build());
    var updateChange = UpdateChangeOperations.buildUpdateChange(LIVE_CHANGE_ID, croppedSurrender);
    givenStagedChange(updateChange);
    givenLiveSurrenderChange(croppedSurrender);

    partialSurrenderCorrectionService.undoPartialSurrenderChange(LICENCE_CORRECTION, updateChange.changeId());

    verify(featureService, never()).deleteAll(any());
  }

  @Test
  void revertPartialSurrenderCorrection_whenTheStagedSurrenderCroppedSubareas_thenDeletesThoseNotLive() {
    var croppedFeature = FeatureTestUtil.builder().build();
    var staged = croppedSurrenderOf(FIRST_FEATURE_ID, croppedFeature);
    var positionCorrection = updatePositionCorrection(
        List.of(UpdateChangeOperations.buildUpdateChange(LIVE_CHANGE_ID, staged)));
    when(licencePositionCorrectionService.findUpdatePositionCorrection(LICENCE_CORRECTION, LICENCE_POSITION))
        .thenReturn(Optional.of(positionCorrection));
    givenCommittedPartialSurrender(positionCorrection, staged);
    givenLiveSurrenderChange(fullSurrenderOf(FIRST_FEATURE_ID));
    when(featureService.getFeaturesByIds(Set.of(croppedFeature.getId()))).thenReturn(List.of(croppedFeature));

    partialSurrenderCorrectionService.revertPartialSurrenderCorrection(LICENCE_CORRECTION, LICENCE_POSITION, staged);

    verify(featureService).deleteAll(List.of(croppedFeature));
  }

  @Test
  void adjustPartialSurrenderBlocks_whenABlockWithCroppedSubareasIsNoLongerSurrenderable_thenItsCroppedSubareasAreDeleted() {
    var croppedFeature = FeatureTestUtil.builder().build();
    var croppedDetails = croppedSurrenderOf(FIRST_FEATURE_ID, croppedFeature).featureIdToSurrenderDetails()
        .get(FIRST_FEATURE_ID);
    var surrender = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID, SECOND_FEATURE_ID))
        .withSurrenderDetails(Map.of(
            FIRST_FEATURE_ID, croppedDetails,
            SECOND_FEATURE_ID, surrenderDetails(BlockSurrenderType.FULL_SURRENDER, SECOND_COMMAND_JOURNEY_ID)))
        .build();
    var stagedChange = AddChange.buildOperationsChange(List.of(surrender), 1);
    var positionCorrection = executedPositionCorrection(List.of(stagedChange));
    givenCommittedPartialSurrender(positionCorrection, surrender);
    when(licencePositionSpatialService.getBlockFeaturesGoingIntoChange(positionCorrection, stagedChange.changeId()))
        .thenReturn(List.of(FeatureTestUtil.blockFeature(SECOND_FEATURE_ID, "30", 2)));
    when(featureService.getFeaturesByIds(Set.of(croppedFeature.getId()))).thenReturn(List.of(croppedFeature));

    partialSurrenderCorrectionService.adjustPartialSurrenderBlocks(positionCorrection);

    verify(featureService).deleteAll(List.of(croppedFeature));
  }

  @Test
  void adjustPartialSurrenderBlocks_whenARemovedBlocksCroppedSubareaIsAlsoLive_thenItIsNotDeleted() {
    var croppedDetails = croppedSurrenderOf(FIRST_FEATURE_ID, FeatureTestUtil.builder().build())
        .featureIdToSurrenderDetails()
        .get(FIRST_FEATURE_ID);
    var surrender = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID, SECOND_FEATURE_ID))
        .withSurrenderDetails(Map.of(
            FIRST_FEATURE_ID, croppedDetails,
            SECOND_FEATURE_ID, surrenderDetails(BlockSurrenderType.FULL_SURRENDER, SECOND_COMMAND_JOURNEY_ID)))
        .build();
    var positionCorrection = updatePositionCorrection(
        List.of(UpdateChangeOperations.buildUpdateChange(LIVE_CHANGE_ID, surrender)));
    givenCommittedPartialSurrender(positionCorrection, surrender);
    givenLiveSurrenderChange(surrender);
    when(licencePositionSpatialService.getBlockFeaturesGoingIntoChange(positionCorrection, LIVE_CHANGE_ID))
        .thenReturn(List.of(FeatureTestUtil.blockFeature(SECOND_FEATURE_ID, "30", 2)));

    partialSurrenderCorrectionService.adjustPartialSurrenderBlocks(positionCorrection);

    verify(featureService, never()).deleteAll(any());
  }

  private static PartialSurrenderOperation surrenderOf(
      UUID featureId,
      PartialSurrenderOperation.SurrenderDetails surrenderDetails
  ) {
    return LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(featureId))
        .withSurrenderDetails(Map.of(featureId, surrenderDetails))
        .build();
  }

  private static PartialSurrenderOperation croppedSurrenderOf(
      UUID featureId,
      Feature croppedFeature
  ) {
    var croppedSubarea = new SubareaDetails(croppedFeature.getId(), SUBAREA.name(), SUBAREA.shortName());
    var retainedFeatureId = UUID.randomUUID();
    return surrenderOf(featureId, new PartialSurrenderOperation.SurrenderDetails(
        BlockSurrenderType.PARTIAL_SURRENDER, null, List.of(UUID.randomUUID()), List.of(retainedFeatureId))
        .withSubareas(Map.of(retainedFeatureId, List.of(SubareaSurrenderOutcome.cropped(SUBAREA, croppedSubarea)))));
  }

  private static LicencePositionCorrection productionPositionCorrection(List<LicencePositionChangeType> changes) {
    return LicencePositionCorrectionTestUtil.newBuilder()
        .withLicenceCorrection(PRODUCTION_CORRECTION)
        .withTargetLicencePosition(null)
        .withPayload(CreateLicencePositionPayloadTestUtil.newBuilder().withChanges(changes).build())
        .build();
  }

  private static LicencePositionCorrection productionUpdatePositionCorrection(List<LicencePositionChangeType> changes) {
    return LicencePositionCorrectionTestUtil.newBuilder()
        .withLicenceCorrection(PRODUCTION_CORRECTION)
        .withChangeType(LicencePositionCorrectionChangeType.UPDATE_POSITION)
        .withTargetLicencePosition(LICENCE_POSITION)
        .withPayload(LicencePositionPayload.newUpdateLicencePositionPayload().withChanges(changes).build())
        .build();
  }

  private void givenProductionPositionCorrection(LicencePositionCorrection positionCorrection) {
    when(licencePositionCorrectionService.getOrBuildUpdatePositionCorrection(PRODUCTION_CORRECTION, LICENCE_POSITION))
        .thenReturn(positionCorrection);
    when(licencePositionCorrectionService.save(positionCorrection)).thenReturn(positionCorrection);
  }
}
