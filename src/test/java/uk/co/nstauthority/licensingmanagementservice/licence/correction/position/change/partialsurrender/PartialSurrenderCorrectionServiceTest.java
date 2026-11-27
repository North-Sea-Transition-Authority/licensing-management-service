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
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
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
import uk.co.fivium.gisframework.operator.merge.MergeOperatorService;
import uk.co.nstauthority.licensingmanagementservice.exception.LmsEntityNotFoundException;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.ownedgisartefact.CorrectionOwnedGisArtefactService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.CorrectionPosition;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionStagedPayloadService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.blocksurrendertype.BlockSurrenderType;
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
import uk.co.nstauthority.licensingmanagementservice.licence.staged.LicencePositionStagedPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.LicencePositionStagedPayloadChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.LicencePositionStagedPayloadTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.changetypes.AddChange;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.changetypes.LicencePositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.changetypes.UpdateChangeOperations;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.payloads.CreateLicencePositionPayloadTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.payloads.LicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.payloads.UpdateLicencePositionPayloadTestUtil;

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
  private LicencePositionStagedPayloadService licencePositionStagedPayloadService;

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

  @Mock
  private MergeOperatorService mergeOperatorService;

  @Mock
  private CorrectionOwnedGisArtefactService correctionOwnedGisArtefactService;

  @InjectMocks
  private PartialSurrenderCorrectionService partialSurrenderCorrectionService;

  @Captor
  private ArgumentCaptor<List<PartialSurrenderOperation>> partialSurrenderOperationCaptor;

  @Test
  void getCommittedPartialSurrender_whenNoPartialSurrenderChange_returnsEmpty() {
    assertThat(partialSurrenderCorrectionService.getCommittedPartialSurrender(positionStagedPayload())).isEmpty();
  }

  @Test
  void getCommittedPartialSurrender_whenPartialSurrenderChange_returnsTheOperation() {
    var operation = partialSurrender(FIRST_FEATURE_ID, SECOND_FEATURE_ID);
    var positionStagedPayload = positionStagedPayload(
        List.of(AddChange.buildOperationsChange(List.of(operation), 1)));
    givenCommittedPartialSurrender(positionStagedPayload, operation);

    assertThat(partialSurrenderCorrectionService.getCommittedPartialSurrender(positionStagedPayload))
        .contains(operation);
  }

  @Test
  void getCommittedPartialSurrender_whenNoPositionStagedPayload_returnsEmpty() {
    assertThat(partialSurrenderCorrectionService.getCommittedPartialSurrender(null)).isEmpty();
  }

  @Test
  void getCommittedPartialSurrenderChangeId_whenSurrenderStaged_returnsTheStagedChangeId() {
    var change = AddChange.buildOperationsChange(List.of(partialSurrender(FIRST_FEATURE_ID)), 1);
    var positionStagedPayload = positionStagedPayload(List.of(change));

    assertThat(partialSurrenderCorrectionService.getCommittedPartialSurrenderChangeId(positionStagedPayload))
        .contains(change.changeId());
  }

  @Test
  void getCommittedPartialSurrenderChangeId_whenNoPositionStagedPayload_returnsEmpty() {
    assertThat(partialSurrenderCorrectionService.getCommittedPartialSurrenderChangeId(null)).isEmpty();
  }

  @Test
  void getCommittedPartialSurrenderChangeId_whenNoSurrenderStaged_returnsEmpty() {
    assertThat(partialSurrenderCorrectionService.getCommittedPartialSurrenderChangeId(positionStagedPayload())).isEmpty();
  }

  @Test
  void findSingleBlockNotOperatedOn_whenOneBlockNotOperatedOn_thenTheBlock() {
    var positionStagedPayload = executedPositionStagedPayload();
    var block = FeatureTestUtil.builder().withId(FIRST_FEATURE_ID).build();

    when(licencePositionSpatialService.getBlockFeaturesGoingIntoChange(
        LICENCE_CORRECTION,
        LICENCE_POSITION,
        LIVE_CHANGE_ID
    )).thenReturn(List.of(block));
    when(licencePositionStagedPayloadService.blockFeatureIdsAlreadyOperatedOnForExecutedPosition(
        LICENCE_CORRECTION,
        LICENCE_POSITION,
        positionStagedPayload,
        LIVE_CHANGE_ID
    )).thenReturn(Set.of(SECOND_FEATURE_ID));

    var result = partialSurrenderCorrectionService.findSingleBlockNotOperatedOn(
        LICENCE_CORRECTION, LICENCE_POSITION, positionStagedPayload, LIVE_CHANGE_ID);

    assertThat(result).contains(block);
  }

  @Test
  void findSingleBlockNotOperatedOn_whenNotExactlyOneBlock_thenEmpty() {
    when(licencePositionSpatialService.getBlockFeaturesGoingIntoChange(LICENCE_CORRECTION, LICENCE_POSITION, null))
        .thenReturn(List.of(
            FeatureTestUtil.builder().withId(FIRST_FEATURE_ID).build(),
            FeatureTestUtil.builder().withId(SECOND_FEATURE_ID).build()
        ));
    when(licencePositionStagedPayloadService.blockFeatureIdsAlreadyOperatedOnForExecutedPosition(
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
    when(licencePositionStagedPayloadService.blockFeatureIdsAlreadyOperatedOnForExecutedPosition(
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
    var positionStagedPayload = updatePositionStagedPayload(List.of(stagedChange));
    var block = FeatureTestUtil.builder().withId(FIRST_FEATURE_ID).build();

    when(licencePositionSpatialService.getBlockFeaturesGoingIntoChange(
        LICENCE_CORRECTION, LICENCE_POSITION, stagedChange.changeId()))
        .thenReturn(List.of(block));
    when(licencePositionStagedPayloadService.blockFeatureIdsAlreadyOperatedOnForExecutedPosition(
        LICENCE_CORRECTION,
        LICENCE_POSITION,
        positionStagedPayload,
        stagedChange.changeId()
    )).thenReturn(Set.of());

    var result = partialSurrenderCorrectionService.findSingleBlockNotOperatedOn(positionStagedPayload);

    assertThat(result).contains(block);
  }

  @Test
  void findSingleBlockNotOperatedOn_whenAddedPosition_thenChecksTheAddedPositionGoingIntoTheStagedChange() {
    var stagedChange = AddChange.buildOperationsChange(List.of(partialSurrender(FIRST_FEATURE_ID)), 1);
    var positionStagedPayload = positionStagedPayload(List.of(stagedChange));
    var block = FeatureTestUtil.builder().withId(FIRST_FEATURE_ID).build();

    when(licencePositionSpatialService.getBlockFeaturesGoingIntoChange(positionStagedPayload, stagedChange.changeId()))
        .thenReturn(List.of(block));
    when(licencePositionStagedPayloadService.blockFeatureIdsAlreadyOperatedOnForAddedPosition(
        positionStagedPayload,
        stagedChange.changeId()
    )).thenReturn(Set.of());

    var result = partialSurrenderCorrectionService.findSingleBlockNotOperatedOn(positionStagedPayload);

    assertThat(result).contains(block);
  }

  @Test
  void findSingleBlockNotOperatedOn_whenRemovePosition_thenThrows() {
    var positionStagedPayload = LicencePositionStagedPayloadTestUtil.newBuilder()
        .withLicenceCorrection(LICENCE_CORRECTION)
        .withChangeType(LicencePositionStagedPayloadChangeType.REMOVE_POSITION)
        .build();

    assertThatThrownBy(() -> partialSurrenderCorrectionService.findSingleBlockNotOperatedOn(positionStagedPayload))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Licence position staged payload %s removes a position so cannot carry a partial surrender"
            .formatted(positionStagedPayload.getId()));
  }

  @Test
  void getCommittedPartialSurrenderOrThrow_whenStaged_returnsTheOperation() {
    var operation = partialSurrender(FIRST_FEATURE_ID);
    var positionStagedPayload = positionStagedPayload(
        List.of(AddChange.buildOperationsChange(List.of(operation), 1)));
    givenCommittedPartialSurrender(positionStagedPayload, operation);

    assertThat(partialSurrenderCorrectionService.getCommittedPartialSurrenderOrThrow(positionStagedPayload))
        .isEqualTo(operation);
  }

  @Test
  void getCommittedPartialSurrenderOrThrow_whenNotStaged_throws() {
    var positionStagedPayload = positionStagedPayload();

    assertThatThrownBy(() ->
        partialSurrenderCorrectionService.getCommittedPartialSurrenderOrThrow(positionStagedPayload))
        .isInstanceOf(LmsEntityNotFoundException.class)
        .hasMessageContaining(positionStagedPayload.getId().toString());
  }

  @Test
  void commitPartialSurrender_whenFeatureIds_replacesAddChangeWithTheOperation() {
    var positionStagedPayload = positionStagedPayload();
    var operation = partialSurrender(FIRST_FEATURE_ID);
    when(licencePositionStagedPayloadService.replaceAddChangeFor(
        positionStagedPayload, PartialSurrenderOperation.class, List.of(operation)))
        .thenReturn(positionStagedPayload);

    assertThat(partialSurrenderCorrectionService.commitPartialSurrender(positionStagedPayload, operation))
        .isEqualTo(positionStagedPayload);
  }

  @Test
  void commitPartialSurrender_whenExecutedPosition_replacesAddChangeOnTheResolvedPositionStagedPayload() {
    var positionStagedPayload = positionStagedPayload();
    var operation = partialSurrender(FIRST_FEATURE_ID);
    when(licencePositionStagedPayloadService.getOrBuildUpdatePositionStagedPayload(LICENCE_CORRECTION, LICENCE_POSITION))
        .thenReturn(positionStagedPayload);
    when(licencePositionStagedPayloadService.replaceAddChangeFor(
        positionStagedPayload, PartialSurrenderOperation.class, List.of(operation)))
        .thenReturn(positionStagedPayload);

    assertThat(partialSurrenderCorrectionService.commitPartialSurrender(new CorrectionPosition.Executed(LICENCE_CORRECTION, LICENCE_POSITION), operation))
        .isEqualTo(positionStagedPayload);
  }

  @Test
  void stageSingleBlockSurrender_whenExecutedPositionAndOneBlock_thenStagesAPartialSurrenderOfIt() {
    var block = FeatureTestUtil.builder().withId(FIRST_FEATURE_ID).build();
    var positionStagedPayload = updatePositionStagedPayload(List.of());
    var committedPositionStagedPayload = updatePositionStagedPayload(List.of());
    var expectedOperation = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID))
        .withSurrenderDetails(Map.of(FIRST_FEATURE_ID,
            surrenderDetails(BlockSurrenderType.PARTIAL_SURRENDER, FIRST_COMMAND_JOURNEY_ID)))
        .build();
    when(licencePositionStagedPayloadService.findUpdatePositionStagedPayload(LICENCE_CORRECTION, LICENCE_POSITION))
        .thenReturn(Optional.empty());
    when(licencePositionSpatialService.getBlockFeaturesGoingIntoChange(LICENCE_CORRECTION, LICENCE_POSITION, null))
        .thenReturn(List.of(block));
    when(licencePositionStagedPayloadService.blockFeatureIdsAlreadyOperatedOnForExecutedPosition(
        LICENCE_CORRECTION, LICENCE_POSITION, null, null))
        .thenReturn(Set.of());
    when(licencePositionStagedPayloadService.getCommittedChangeOfType(null, PartialSurrenderOperation.class))
        .thenReturn(Optional.empty());
    when(licencePositionStagedPayloadService.getOrBuildUpdatePositionStagedPayload(LICENCE_CORRECTION, LICENCE_POSITION))
        .thenReturn(positionStagedPayload);
    when(licencePositionStagedPayloadService.getCommittedChangeOfType(positionStagedPayload, PartialSurrenderOperation.class))
        .thenReturn(Optional.empty());
    givenCommandJourneyCreatedFor(block, FIRST_COMMAND_JOURNEY_ID);
    when(licencePositionStagedPayloadService.replaceAddChangeFor(
        positionStagedPayload, PartialSurrenderOperation.class, List.of(expectedOperation)))
        .thenReturn(committedPositionStagedPayload);

    var result = partialSurrenderCorrectionService.stageSingleBlockSurrender(new CorrectionPosition.Executed(LICENCE_CORRECTION, LICENCE_POSITION));

    assertThat(result).contains(new SingleBlockSurrender(committedPositionStagedPayload, block));
    verify(licencePositionStagedPayloadService).replaceAddChangeFor(
        eq(positionStagedPayload), eq(PartialSurrenderOperation.class), anyList());
  }

  @Test
  void stageSingleBlockSurrender_whenExecutedPositionAndTheBlockIsAlreadyPartiallySurrendered_thenReusesItsJourney() {
    var block = FeatureTestUtil.builder().withId(FIRST_FEATURE_ID).build();
    var stagedDetails = new PartialSurrenderOperation.SurrenderDetails(
        BlockSurrenderType.PARTIAL_SURRENDER,
        FIRST_COMMAND_JOURNEY_ID,
        List.of(SECOND_FEATURE_ID),
        List.of(THIRD_FEATURE_ID)
    );
    var staged = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID))
        .withSurrenderDetails(Map.of(FIRST_FEATURE_ID, stagedDetails))
        .build();
    var stagedChange = AddChange.buildOperationsChange(List.of(staged), 1);
    var positionStagedPayload = updatePositionStagedPayload(List.of(stagedChange));
    var expectedOperation = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID))
        .withSurrenderDetails(Map.of(FIRST_FEATURE_ID, stagedDetails))
        .build();
    when(licencePositionStagedPayloadService.findUpdatePositionStagedPayload(LICENCE_CORRECTION, LICENCE_POSITION))
        .thenReturn(Optional.of(positionStagedPayload));
    when(licencePositionSpatialService.getBlockFeaturesGoingIntoChange(
        LICENCE_CORRECTION, LICENCE_POSITION, stagedChange.changeId()))
        .thenReturn(List.of(block));
    when(licencePositionStagedPayloadService.blockFeatureIdsAlreadyOperatedOnForExecutedPosition(
        LICENCE_CORRECTION, LICENCE_POSITION, positionStagedPayload, stagedChange.changeId()))
        .thenReturn(Set.of());
    givenCommittedPartialSurrender(positionStagedPayload, staged);
    givenActiveFeaturesOnFirstJourney();
    when(licencePositionStagedPayloadService.getOrBuildUpdatePositionStagedPayload(LICENCE_CORRECTION, LICENCE_POSITION))
        .thenReturn(positionStagedPayload);
    when(licencePositionStagedPayloadService.replaceAddChangeFor(
        positionStagedPayload, PartialSurrenderOperation.class, List.of(expectedOperation)))
        .thenReturn(positionStagedPayload);

    var result = partialSurrenderCorrectionService.stageSingleBlockSurrender(new CorrectionPosition.Executed(LICENCE_CORRECTION, LICENCE_POSITION));

    assertThat(result).contains(new SingleBlockSurrender(positionStagedPayload, block));
    verify(commandJourneyService).getActiveFeatures(FIRST_COMMAND_JOURNEY_ID);
    verifyNoMoreInteractions(commandJourneyService);
  }

  @Test
  void stageSingleBlockSurrender_whenExecutedPositionAndMoreThanOneBlock_thenStagesNothing() {
    when(licencePositionStagedPayloadService.findUpdatePositionStagedPayload(LICENCE_CORRECTION, LICENCE_POSITION))
        .thenReturn(Optional.empty());
    when(licencePositionSpatialService.getBlockFeaturesGoingIntoChange(LICENCE_CORRECTION, LICENCE_POSITION, null))
        .thenReturn(List.of(
            FeatureTestUtil.builder().withId(FIRST_FEATURE_ID).build(),
            FeatureTestUtil.builder().withId(SECOND_FEATURE_ID).build()
        ));
    when(licencePositionStagedPayloadService.blockFeatureIdsAlreadyOperatedOnForExecutedPosition(
        LICENCE_CORRECTION, LICENCE_POSITION, null, null))
        .thenReturn(Set.of());

    var result = partialSurrenderCorrectionService.stageSingleBlockSurrender(new CorrectionPosition.Executed(LICENCE_CORRECTION, LICENCE_POSITION));

    assertThat(result).isEmpty();
    verify(licencePositionStagedPayloadService, never()).replaceAddChangeFor(any(), any(), anyList());
    verifyNoInteractions(commandJourneyService);
  }

  @Test
  void stageSingleBlockCorrectionOfLiveChange_whenNothingStaged_thenStagesAnUpdateWithANewJourney() {
    var block = FeatureTestUtil.builder().withId(FIRST_FEATURE_ID).build();
    var positionStagedPayload = updatePositionStagedPayload(List.of());
    var expectedOperation = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID))
        .withSurrenderDetails(Map.of(FIRST_FEATURE_ID,
            surrenderDetails(BlockSurrenderType.PARTIAL_SURRENDER, FIRST_COMMAND_JOURNEY_ID)))
        .build();
    when(licencePositionStagedPayloadService.findUpdatePositionStagedPayload(LICENCE_CORRECTION, LICENCE_POSITION))
        .thenReturn(Optional.empty());
    when(licencePositionSpatialService.getBlockFeaturesGoingIntoChange(
        LICENCE_CORRECTION, LICENCE_POSITION, LIVE_CHANGE_ID))
        .thenReturn(List.of(block));
    when(licencePositionStagedPayloadService.blockFeatureIdsAlreadyOperatedOnForExecutedPosition(
        LICENCE_CORRECTION, LICENCE_POSITION, null, LIVE_CHANGE_ID))
        .thenReturn(Set.of());
    when(licencePositionStagedPayloadService.getCommittedChangeOfType(null, PartialSurrenderOperation.class))
        .thenReturn(Optional.empty());
    givenCommandJourneyCreatedFor(block, FIRST_COMMAND_JOURNEY_ID);
    givenPositionStagedPayload(positionStagedPayload);
    givenLiveSurrenderChange(partialSurrender(FIRST_FEATURE_ID));

    var result = partialSurrenderCorrectionService.stageSingleBlockCorrectionOfLiveChange(
        LICENCE_CORRECTION, LICENCE_POSITION, LIVE_CHANGE_ID);

    assertThat(result).contains(new SingleBlockSurrender(positionStagedPayload, block));
    assertThat(positionStagedPayload.getPayload().changes())
        .containsExactly(UpdateChangeOperations.buildUpdateChange(LIVE_CHANGE_ID, expectedOperation));
  }

  @Test
  void stageSingleBlockCorrectionOfLiveChange_whenAlreadyStaged_thenReusesTheStagedJourney() {
    var block = FeatureTestUtil.builder().withId(FIRST_FEATURE_ID).build();
    var stagedDetails = new PartialSurrenderOperation.SurrenderDetails(
        BlockSurrenderType.PARTIAL_SURRENDER,
        FIRST_COMMAND_JOURNEY_ID,
        List.of(SECOND_FEATURE_ID),
        List.of(THIRD_FEATURE_ID)
    );
    var staged = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID))
        .withSurrenderDetails(Map.of(FIRST_FEATURE_ID, stagedDetails))
        .build();
    var positionStagedPayload = updatePositionStagedPayload(
        List.of(UpdateChangeOperations.buildUpdateChange(LIVE_CHANGE_ID, staged)));
    when(licencePositionStagedPayloadService.findUpdatePositionStagedPayload(LICENCE_CORRECTION, LICENCE_POSITION))
        .thenReturn(Optional.of(positionStagedPayload));
    when(licencePositionSpatialService.getBlockFeaturesGoingIntoChange(
        LICENCE_CORRECTION, LICENCE_POSITION, LIVE_CHANGE_ID))
        .thenReturn(List.of(block));
    when(licencePositionStagedPayloadService.blockFeatureIdsAlreadyOperatedOnForExecutedPosition(
        LICENCE_CORRECTION, LICENCE_POSITION, positionStagedPayload, LIVE_CHANGE_ID))
        .thenReturn(Set.of());
    givenCommittedPartialSurrender(positionStagedPayload, staged);
    givenPositionStagedPayload(positionStagedPayload);
    givenActiveFeaturesOnFirstJourney();

    var result = partialSurrenderCorrectionService.stageSingleBlockCorrectionOfLiveChange(
        LICENCE_CORRECTION, LICENCE_POSITION, LIVE_CHANGE_ID);

    assertThat(result).contains(new SingleBlockSurrender(positionStagedPayload, block));
    assertThat(positionStagedPayload.getPayload().changes())
        .containsExactly(UpdateChangeOperations.buildUpdateChange(LIVE_CHANGE_ID, staged));
    verify(commandJourneyService).getActiveFeatures(FIRST_COMMAND_JOURNEY_ID);
    verifyNoMoreInteractions(commandJourneyService);
  }

  @Test
  void stageSingleBlockCorrectionOfLiveChange_whenMoreThanOneBlock_thenStagesNothing() {
    when(licencePositionStagedPayloadService.findUpdatePositionStagedPayload(LICENCE_CORRECTION, LICENCE_POSITION))
        .thenReturn(Optional.empty());
    when(licencePositionSpatialService.getBlockFeaturesGoingIntoChange(
        LICENCE_CORRECTION, LICENCE_POSITION, LIVE_CHANGE_ID))
        .thenReturn(List.of(
            FeatureTestUtil.builder().withId(FIRST_FEATURE_ID).build(),
            FeatureTestUtil.builder().withId(SECOND_FEATURE_ID).build()
        ));
    when(licencePositionStagedPayloadService.blockFeatureIdsAlreadyOperatedOnForExecutedPosition(
        LICENCE_CORRECTION, LICENCE_POSITION, null, LIVE_CHANGE_ID))
        .thenReturn(Set.of());

    var result = partialSurrenderCorrectionService.stageSingleBlockCorrectionOfLiveChange(
        LICENCE_CORRECTION, LICENCE_POSITION, LIVE_CHANGE_ID);

    assertThat(result).isEmpty();
    verify(licencePositionStagedPayloadService, never()).save(any());
    verifyNoInteractions(commandJourneyService);
  }

  @Test
  void stageSingleBlockSurrender_whenStagedPositionAndOneBlock_thenStagesAPartialSurrenderOfIt() {
    var block = FeatureTestUtil.builder().withId(FIRST_FEATURE_ID).build();
    var positionStagedPayload = positionStagedPayload();
    var committedPositionStagedPayload = positionStagedPayload();
    var expectedOperation = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID))
        .withSurrenderDetails(Map.of(FIRST_FEATURE_ID,
            surrenderDetails(BlockSurrenderType.PARTIAL_SURRENDER, FIRST_COMMAND_JOURNEY_ID)))
        .build();
    when(licencePositionSpatialService.getBlockFeaturesGoingIntoChange(positionStagedPayload, null))
        .thenReturn(List.of(block));
    when(licencePositionStagedPayloadService.blockFeatureIdsAlreadyOperatedOnForAddedPosition(positionStagedPayload, null))
        .thenReturn(Set.of());
    when(licencePositionStagedPayloadService.getCommittedChangeOfType(positionStagedPayload, PartialSurrenderOperation.class))
        .thenReturn(Optional.empty());
    givenCommandJourneyCreatedFor(block, FIRST_COMMAND_JOURNEY_ID);
    when(licencePositionStagedPayloadService.replaceAddChangeFor(
        positionStagedPayload, PartialSurrenderOperation.class, List.of(expectedOperation)))
        .thenReturn(committedPositionStagedPayload);

    var result = partialSurrenderCorrectionService.stageSingleBlockSurrender(new CorrectionPosition.Staged(positionStagedPayload));

    assertThat(result).contains(new SingleBlockSurrender(committedPositionStagedPayload, block));
    verify(licencePositionStagedPayloadService).replaceAddChangeFor(
        eq(positionStagedPayload), eq(PartialSurrenderOperation.class), anyList());
  }

  @Test
  void stageSingleBlockSurrender_whenStagedPositionAndTheOnlyBlockIsOperatedOn_thenStagesNothing() {
    var positionStagedPayload = positionStagedPayload();
    when(licencePositionSpatialService.getBlockFeaturesGoingIntoChange(positionStagedPayload, null))
        .thenReturn(List.of(FeatureTestUtil.builder().withId(FIRST_FEATURE_ID).build()));
    when(licencePositionStagedPayloadService.blockFeatureIdsAlreadyOperatedOnForAddedPosition(positionStagedPayload, null))
        .thenReturn(Set.of(FIRST_FEATURE_ID));

    var result = partialSurrenderCorrectionService.stageSingleBlockSurrender(new CorrectionPosition.Staged(positionStagedPayload));

    assertThat(result).isEmpty();
    verify(licencePositionStagedPayloadService, never()).replaceAddChangeFor(any(), any(), anyList());
    verifyNoInteractions(commandJourneyService);
  }

  @Test
  void correctExistingPartialSurrender_whenNothingStaged_addsUpdateChangeKeyedOnTheLiveChange() {
    var positionStagedPayload = updatePositionStagedPayload(List.of());
    var operation = partialSurrender(FIRST_FEATURE_ID);
    givenPositionStagedPayload(positionStagedPayload);
    givenLiveSurrenderChange(partialSurrender(FIRST_FEATURE_ID));

    partialSurrenderCorrectionService.correctExistingPartialSurrender(
        LICENCE_CORRECTION, LICENCE_POSITION, LIVE_CHANGE_ID, operation);

    assertThat(positionStagedPayload.getPayload().changes())
        .containsExactly(UpdateChangeOperations.buildUpdateChange(LIVE_CHANGE_ID, operation));
  }

  @Test
  void correctExistingPartialSurrender_whenStagedAsAnAddChange_replacesItKeepingTheChangeOrder() {
    var staged = partialSurrender(FIRST_FEATURE_ID);
    var positionStagedPayload = updatePositionStagedPayload(
        List.of(AddChange.buildOperationsChange(List.of(staged), 3)));
    var corrected = partialSurrender(SECOND_FEATURE_ID);
    givenPositionStagedPayload(positionStagedPayload);
    givenLiveSurrenderChange(partialSurrender(FIRST_FEATURE_ID));

    partialSurrenderCorrectionService.correctExistingPartialSurrender(
        LICENCE_CORRECTION, LICENCE_POSITION, LIVE_CHANGE_ID, corrected);

    assertThat(positionStagedPayload.getPayload().changes())
        .singleElement()
        .usingRecursiveComparison()
        .ignoringFields("changeId")
        .isEqualTo(AddChange.buildOperationsChange(List.of(corrected), 3));
  }

  @Test
  void correctExistingPartialSurrender_whenAlreadyCorrected_replacesTheStagedCorrection() {
    var staged = partialSurrender(FIRST_FEATURE_ID);
    var positionStagedPayload = updatePositionStagedPayload(
        List.of(UpdateChangeOperations.buildUpdateChange(LIVE_CHANGE_ID, staged)));
    var corrected = partialSurrender(SECOND_FEATURE_ID);
    givenPositionStagedPayload(positionStagedPayload);
    givenLiveSurrenderChange(partialSurrender(FIRST_FEATURE_ID));

    partialSurrenderCorrectionService.correctExistingPartialSurrender(
        LICENCE_CORRECTION, LICENCE_POSITION, LIVE_CHANGE_ID, corrected);

    assertThat(positionStagedPayload.getPayload().changes())
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
    var positionStagedPayload = updatePositionStagedPayload(
        List.of(UpdateChangeOperations.buildUpdateChange(LIVE_CHANGE_ID, staged)));
    var corrected = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID, THIRD_FEATURE_ID))
        .withSurrenderDetails(Map.of(
            FIRST_FEATURE_ID, surrenderDetails(BlockSurrenderType.PARTIAL_SURRENDER, FIRST_COMMAND_JOURNEY_ID),
            THIRD_FEATURE_ID, surrenderDetails(BlockSurrenderType.PARTIAL_SURRENDER, THIRD_COMMAND_JOURNEY_ID)))
        .build();
    givenPositionStagedPayload(positionStagedPayload);
    givenCommittedPartialSurrender(positionStagedPayload, staged);

    partialSurrenderCorrectionService.correctExistingPartialSurrender(
        LICENCE_CORRECTION, LICENCE_POSITION, LIVE_CHANGE_ID, corrected);

    verify(commandJourneyService).deleteCommandJourney(SECOND_COMMAND_JOURNEY_ID);
    verify(commandJourneyService, never()).deleteCommandJourney(FIRST_COMMAND_JOURNEY_ID);
    verify(commandJourneyService, never()).deleteCommandJourney(THIRD_COMMAND_JOURNEY_ID);
  }

  @Test
  void correctExistingPartialSurrender_whenNothingStaged_deletesNoJourneys() {
    var positionStagedPayload = updatePositionStagedPayload(List.of());
    givenPositionStagedPayload(positionStagedPayload);
    givenLiveSurrenderChange(partialSurrender(FIRST_FEATURE_ID));

    partialSurrenderCorrectionService.correctExistingPartialSurrender(
        LICENCE_CORRECTION, LICENCE_POSITION, LIVE_CHANGE_ID, partialSurrender(FIRST_FEATURE_ID));

    verify(commandJourneyService, never()).deleteCommandJourney(any());
  }

  @Test
  void revertPartialSurrenderCorrection_whenNoPositionStagedPayload_correctsNothing() {
    when(licencePositionStagedPayloadService.findUpdatePositionStagedPayload(LICENCE_CORRECTION, LICENCE_POSITION))
        .thenReturn(Optional.empty());

    partialSurrenderCorrectionService.revertPartialSurrenderCorrection(
        LICENCE_CORRECTION, LICENCE_POSITION, partialSurrender(FIRST_FEATURE_ID));

    verify(licencePositionStagedPayloadService, never()).save(any());
    verify(licencePositionStagedPayloadService, never()).delete(any());
  }

  @Test
  void revertPartialSurrenderCorrection_whenOtherChangesRemain_dropsOnlyThePartialSurrenderChange() {
    var administratorChange = AddChange.buildOperationsChange(
        List.of(LicenceOperation.newAdministratorChange().withOperator(ADMINISTRATOR_ID).build()), 1);
    var positionStagedPayload = updatePositionStagedPayload(List.of(
        administratorChange,
        UpdateChangeOperations.buildUpdateChange(LIVE_CHANGE_ID, partialSurrender(FIRST_FEATURE_ID))));
    when(licencePositionStagedPayloadService.findUpdatePositionStagedPayload(LICENCE_CORRECTION, LICENCE_POSITION))
        .thenReturn(Optional.of(positionStagedPayload));

    partialSurrenderCorrectionService.revertPartialSurrenderCorrection(
        LICENCE_CORRECTION, LICENCE_POSITION, partialSurrender(FIRST_FEATURE_ID));

    verify(licencePositionStagedPayloadService, never()).delete(any());
    verify(licencePositionStagedPayloadService).save(positionStagedPayload);
    assertThat(positionStagedPayload.getPayload().changes()).containsExactly(administratorChange);
  }

  @Test
  void revertPartialSurrenderCorrection_whenThePartialSurrenderIsTheOnlyChange_deletesThePositionStagedPayload() {
    var positionStagedPayload = updatePositionStagedPayload(
        List.of(UpdateChangeOperations.buildUpdateChange(LIVE_CHANGE_ID, partialSurrender(FIRST_FEATURE_ID))));
    when(licencePositionStagedPayloadService.findUpdatePositionStagedPayload(LICENCE_CORRECTION, LICENCE_POSITION))
        .thenReturn(Optional.of(positionStagedPayload));

    partialSurrenderCorrectionService.revertPartialSurrenderCorrection(
        LICENCE_CORRECTION, LICENCE_POSITION, partialSurrender(FIRST_FEATURE_ID));

    verify(licencePositionStagedPayloadService).delete(positionStagedPayload);
    verify(licencePositionStagedPayloadService, never()).save(any());
  }

  @Test
  void revertPartialSurrenderCorrection_whenThePositionDateIsAlsoCorrected_keepsThePositionStagedPayload() {
    var positionStagedPayload = LicencePositionStagedPayloadTestUtil.newBuilder()
        .withChangeType(LicencePositionStagedPayloadChangeType.UPDATE_POSITION)
        .withTargetLicencePosition(LICENCE_POSITION)
        .withPayload(UpdateLicencePositionPayloadTestUtil.newBuilder()
            .withEffectiveDate(LICENCE_POSITION.getPositionDate().plusDays(1))
            .withChanges(List.of(
                UpdateChangeOperations.buildUpdateChange(LIVE_CHANGE_ID, partialSurrender(FIRST_FEATURE_ID))))
            .build())
        .build();
    when(licencePositionStagedPayloadService.findUpdatePositionStagedPayload(LICENCE_CORRECTION, LICENCE_POSITION))
        .thenReturn(Optional.of(positionStagedPayload));

    partialSurrenderCorrectionService.revertPartialSurrenderCorrection(
        LICENCE_CORRECTION, LICENCE_POSITION, partialSurrender(FIRST_FEATURE_ID));

    verify(licencePositionStagedPayloadService, never()).delete(any());
    verify(licencePositionStagedPayloadService).save(positionStagedPayload);
    assertThat(positionStagedPayload.getPayload().changes()).isEmpty();
  }

  @Test
  void hasStagedPartialSurrender_whenStaged_returnsTrue() {
    var operation = partialSurrender(FIRST_FEATURE_ID);
    var positionStagedPayload = positionStagedPayload(
        List.of(AddChange.buildOperationsChange(List.of(operation), 1)));
    givenCommittedPartialSurrender(positionStagedPayload, operation);

    assertThat(partialSurrenderCorrectionService.hasStagedPartialSurrender(positionStagedPayload)).isTrue();
  }

  @Test
  void hasStagedPartialSurrender_whenNotStaged_returnsFalse() {
    assertThat(partialSurrenderCorrectionService.hasStagedPartialSurrender(positionStagedPayload())).isFalse();
  }

  @Test
  void adjustPartialSurrenderBlocks_whenNoCommittedSurrender_doesNothing() {
    var positionStagedPayload = executedPositionStagedPayload();

    partialSurrenderCorrectionService.adjustPartialSurrenderBlocks(positionStagedPayload);

    verify(licencePositionStagedPayloadService, never())
        .replaceAddChangeFor(eq(positionStagedPayload), eq(PartialSurrenderOperation.class), anyList());
  }

  @Test
  void adjustPartialSurrenderBlocks_whenAllBlocksStillSurrenderable_doesNothing() {
    var surrender = partialSurrender(FIRST_FEATURE_ID, SECOND_FEATURE_ID);
    var stagedChange = AddChange.buildOperationsChange(List.of(surrender), 1);
    var positionStagedPayload = executedPositionStagedPayload(List.of(stagedChange));
    givenCommittedPartialSurrender(positionStagedPayload, surrender);
    when(licencePositionSpatialService.getBlockFeaturesGoingIntoChange(positionStagedPayload, stagedChange.changeId()))
        .thenReturn(List.of(
            FeatureTestUtil.builder().withId(FIRST_FEATURE_ID).build(),
            FeatureTestUtil.builder().withId(SECOND_FEATURE_ID).build()
        ));

    partialSurrenderCorrectionService.adjustPartialSurrenderBlocks(positionStagedPayload);

    verify(licencePositionStagedPayloadService, never())
        .replaceAddChangeFor(eq(positionStagedPayload), eq(PartialSurrenderOperation.class), anyList());
  }

  @Test
  void adjustPartialSurrenderBlocks_whenSomeBlocksNoLongerSurrenderable_retainsOnlySurrenderableBlocks() {
    var surrender = partialSurrender(FIRST_FEATURE_ID, SECOND_FEATURE_ID);
    var stagedChange = AddChange.buildOperationsChange(List.of(surrender), 1);
    var positionStagedPayload = executedPositionStagedPayload(List.of(stagedChange));
    givenCommittedPartialSurrender(positionStagedPayload, surrender);
    when(licencePositionSpatialService.getBlockFeaturesGoingIntoChange(positionStagedPayload, stagedChange.changeId()))
        .thenReturn(List.of(
            FeatureTestUtil.builder().withId(FIRST_FEATURE_ID).build()
        ));

    partialSurrenderCorrectionService.adjustPartialSurrenderBlocks(positionStagedPayload);

    var expected = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderDate(surrender.surrenderDate())
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID))
        .build();
    verify(licencePositionStagedPayloadService)
        .replaceAddChangeFor(positionStagedPayload, PartialSurrenderOperation.class, List.of(expected));
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
    var positionStagedPayload = executedPositionStagedPayload(List.of(stagedChange));
    givenCommittedPartialSurrender(positionStagedPayload, surrender);
    when(licencePositionSpatialService.getBlockFeaturesGoingIntoChange(positionStagedPayload, stagedChange.changeId()))
        .thenReturn(List.of(
            FeatureTestUtil.builder().withId(FIRST_FEATURE_ID).build(),
            FeatureTestUtil.builder().withId(THIRD_FEATURE_ID).build()
        ));

    partialSurrenderCorrectionService.adjustPartialSurrenderBlocks(positionStagedPayload);

    verify(commandJourneyService).deleteCommandJourney(SECOND_COMMAND_JOURNEY_ID);

    var expected = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderDate(surrender.surrenderDate())
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID))
        .withSurrenderDetails(Map.of(FIRST_FEATURE_ID, retainedSurrenderDetails))
        .build();
    verify(licencePositionStagedPayloadService)
        .replaceAddChangeFor(positionStagedPayload, PartialSurrenderOperation.class, List.of(expected));
  }


  @Test
  void adjustPartialSurrenderBlocks_whenNoBlocksStillSurrenderable_removesTheSurrender() {
    var surrender = partialSurrender(FIRST_FEATURE_ID, SECOND_FEATURE_ID);
    var stagedChange = AddChange.buildOperationsChange(List.of(surrender), 1);
    var positionStagedPayload = executedPositionStagedPayload(List.of(stagedChange));
    givenCommittedPartialSurrender(positionStagedPayload, surrender);
    when(licencePositionSpatialService.getBlockFeaturesGoingIntoChange(positionStagedPayload, stagedChange.changeId()))
        .thenReturn(List.of(
            FeatureTestUtil.builder().withId(UUID.randomUUID()).build()
        ));

    partialSurrenderCorrectionService.adjustPartialSurrenderBlocks(positionStagedPayload);

    verify(licencePositionStagedPayloadService)
        .replaceAddChangeFor(positionStagedPayload, PartialSurrenderOperation.class, List.of());
  }

  @Test
  void adjustPartialSurrenderBlocks_whenTheSurrenderIsStagedAsAnUpdate_thenReplacesItInPlaceKeepingItsChangeId() {
    var surrender = partialSurrender(FIRST_FEATURE_ID, SECOND_FEATURE_ID);
    var stagedChange = UpdateChangeOperations.buildUpdateChange(LIVE_CHANGE_ID, surrender);
    var positionStagedPayload = executedPositionStagedPayload(List.of(stagedChange));
    givenCommittedPartialSurrender(positionStagedPayload, surrender);
    when(licencePositionSpatialService.getBlockFeaturesGoingIntoChange(positionStagedPayload, LIVE_CHANGE_ID))
        .thenReturn(List.of(FeatureTestUtil.builder().withId(FIRST_FEATURE_ID).build()));
    when(licencePositionStagedPayloadService.save(positionStagedPayload)).thenReturn(positionStagedPayload);

    partialSurrenderCorrectionService.adjustPartialSurrenderBlocks(positionStagedPayload);

    var expected = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderDate(surrender.surrenderDate())
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID))
        .build();
    verify(licencePositionStagedPayloadService, never())
        .replaceAddChangeFor(eq(positionStagedPayload), eq(PartialSurrenderOperation.class), anyList());
    assertThat(positionStagedPayload.getPayload().changes())
        .containsExactly(UpdateChangeOperations.buildUpdateChange(LIVE_CHANGE_ID, expected));
  }

  @Test
  void adjustPartialSurrenderBlocks_whenNoBlocksStillSurrenderableOnALiveSurrenderMovedHere_thenStagesItsRemovalOnItsLivePosition() {
    var surrender = partialSurrender(FIRST_FEATURE_ID, SECOND_FEATURE_ID);
    var positionStagedPayload = updatePositionStagedPayloadFor(
        LATER_POSITION,
        List.of(UpdateChangeOperations.buildUpdateChange(LIVE_CHANGE_ID, surrender))
    );
    givenCommittedPartialSurrender(positionStagedPayload, surrender);
    when(licencePositionSpatialService.getBlockFeaturesGoingIntoChange(positionStagedPayload, LIVE_CHANGE_ID))
        .thenReturn(List.of(FeatureTestUtil.builder().withId(THIRD_FEATURE_ID).build()));
    when(licencePositionChangeService.getByIdOrThrow(UUID.fromString(LIVE_CHANGE_ID)))
        .thenReturn(LicencePositionChangeTestUtil.newBuilder()
            .withId(UUID.fromString(LIVE_CHANGE_ID))
            .withLicencePosition(LICENCE_POSITION)
            .withOperations(List.of(surrender))
            .build());

    partialSurrenderCorrectionService.adjustPartialSurrenderBlocks(positionStagedPayload);

    verify(licencePositionStagedPayloadService).stageChangesOnPosition(
        LICENCE_CORRECTION,
        LICENCE_POSITION.getId(),
        List.of(LicencePositionChangeType.removeChange().withChangeId(LIVE_CHANGE_ID).build())
    );
    verify(licencePositionStagedPayloadService, never())
        .replaceAddChangeFor(eq(positionStagedPayload), eq(PartialSurrenderOperation.class), anyList());
  }

  @Test
  void getSurrenderedBlockFeatureOrThrow_whenStaged_returnsTheFeature() {
    var operation = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID))
        .build();
    var positionStagedPayload = positionStagedPayload(
        List.of(AddChange.buildOperationsChange(List.of(operation), 1)));
    var feature = FeatureTestUtil.builder().withId(FIRST_FEATURE_ID).build();
    givenCommittedPartialSurrender(positionStagedPayload, operation);
    when(featureService.getFeatureOrThrow(FIRST_FEATURE_ID)).thenReturn(feature);

    assertThat(partialSurrenderCorrectionService.getSurrenderedBlockFeatureOrThrow(positionStagedPayload, FIRST_FEATURE_ID))
        .isEqualTo(feature);
  }

  @Test
  void getSurrenderedBlockFeatureOrThrow_whenNotStaged_throws() {
    var operation = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID))
        .build();
    var positionStagedPayload = positionStagedPayload(
        List.of(AddChange.buildOperationsChange(List.of(operation), 1)));
    givenCommittedPartialSurrender(positionStagedPayload, operation);

    assertThatThrownBy(() ->
        partialSurrenderCorrectionService.getSurrenderedBlockFeatureOrThrow(positionStagedPayload, SECOND_FEATURE_ID))
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
    var positionStagedPayload = positionStagedPayload(
        List.of(AddChange.buildOperationsChange(List.of(existingOperation), 1)));
    givenCommittedPartialSurrender(positionStagedPayload, existingOperation);
    var secondFeature = FeatureTestUtil.builder().withId(SECOND_FEATURE_ID).build();
    when(featureService.getFeatureOrThrow(SECOND_FEATURE_ID)).thenReturn(secondFeature);
    var createdJourney = new CommandJourney();
    createdJourney.setId(SECOND_COMMAND_JOURNEY_ID);
    when(commandJourneyService.createAndAssignCommandJourney(List.of(secondFeature)))
        .thenReturn(createdJourney);

    partialSurrenderCorrectionService.setBlockSurrenderType(
        positionStagedPayload, SECOND_FEATURE_ID,
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

    verify(licencePositionStagedPayloadService).replaceAddChangeFor(
        eq(positionStagedPayload), eq(PartialSurrenderOperation.class), partialSurrenderOperationCaptor.capture());
    assertThat(partialSurrenderOperationCaptor.getValue()).containsExactly(expectedOperation);
  }

  @Test
  void getSurrenderUnderCorrectionOrThrow_whenStagedOnThisCorrection_returnsTheStagedSurrender() {
    var staged = partialSurrender(FIRST_FEATURE_ID);
    var positionStagedPayload = updatePositionStagedPayload(
        List.of(UpdateChangeOperations.buildUpdateChange(LIVE_CHANGE_ID, staged)));
    when(licencePositionStagedPayloadService.findUpdatePositionStagedPayload(LICENCE_CORRECTION, LICENCE_POSITION))
        .thenReturn(Optional.of(positionStagedPayload));
    givenCommittedPartialSurrender(positionStagedPayload, staged);

    assertThat(partialSurrenderCorrectionService.getSurrenderUnderCorrectionOrThrow(
        LICENCE_CORRECTION, LICENCE_POSITION, LIVE_CHANGE_ID))
        .isEqualTo(staged);
  }

  @Test
  void getSurrenderUnderCorrectionOrThrow_whenNothingStaged_returnsTheLiveSurrender() {
    var live = partialSurrender(SECOND_FEATURE_ID);
    when(licencePositionStagedPayloadService.findUpdatePositionStagedPayload(LICENCE_CORRECTION, LICENCE_POSITION))
        .thenReturn(Optional.empty());
    givenLiveSurrenderChange(live);

    assertThat(partialSurrenderCorrectionService.getSurrenderUnderCorrectionOrThrow(
        LICENCE_CORRECTION, LICENCE_POSITION, LIVE_CHANGE_ID))
        .isEqualTo(live);
  }

  @Test
  void getSurrenderUnderCorrectionOrThrow_whenTheLiveChangeIsNotAPartialSurrender_throws() {
    when(licencePositionStagedPayloadService.findUpdatePositionStagedPayload(LICENCE_CORRECTION, LICENCE_POSITION))
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
    var positionStagedPayload = updatePositionStagedPayload(List.of(
        UpdateChangeOperations.buildUpdateChange(LIVE_CHANGE_ID, partialSurrender(FIRST_FEATURE_ID))));

    assertThat(partialSurrenderCorrectionService.findCorrectedLiveChangeId(positionStagedPayload))
        .contains(LIVE_CHANGE_ID);
  }

  @Test
  void findCorrectedLiveChangeId_whenTheStagedSurrenderIsNewlyAdded_thenEmpty() {
    var positionStagedPayload = updatePositionStagedPayload(List.of(
        AddChange.buildOperationsChange(List.of(partialSurrender(FIRST_FEATURE_ID)), 1)));

    assertThat(partialSurrenderCorrectionService.findCorrectedLiveChangeId(positionStagedPayload)).isEmpty();
  }

  @Test
  void deleteStagedSurrenderArtefacts_whenTheSurrenderWasAddedByThisCorrection_thenDeletesItsCommandJourneys() {
    var surrender = fullSurrenderOf(FIRST_FEATURE_ID);
    var addChange = AddChange.buildOperationsChange(List.of(surrender), 1);
    givenStagedChange(addChange, surrender);

    partialSurrenderCorrectionService.deleteStagedSurrenderArtefacts(addChange);

    verify(commandJourneyService).deleteCommandJourney(FIRST_COMMAND_JOURNEY_ID);
  }

  @Test
  void deleteStagedSurrenderArtefacts_whenTheSurrenderCorrectsALiveChange_thenDeletesItsCommandJourneys() {
    var surrender = twoBlockSurrenderOf(FIRST_FEATURE_ID, SECOND_FEATURE_ID);
    var updateChange = UpdateChangeOperations.buildUpdateChange(LIVE_CHANGE_ID, surrender);
    givenStagedChange(updateChange, surrender);

    partialSurrenderCorrectionService.deleteStagedSurrenderArtefacts(updateChange);

    verify(commandJourneyService).deleteCommandJourney(FIRST_COMMAND_JOURNEY_ID);
    verify(commandJourneyService).deleteCommandJourney(SECOND_COMMAND_JOURNEY_ID);
  }

  @Test
  void deleteStagedSurrenderArtefacts_whenTheSurrenderWasRemovedByThisCorrection_thenDeletesNoCommandJourneys() {
    var removeChange = LicencePositionChangeType.removeChange().withChangeId(LIVE_CHANGE_ID).build();
    givenStagedChange(removeChange, fullSurrenderOf(FIRST_FEATURE_ID));

    partialSurrenderCorrectionService.deleteStagedSurrenderArtefacts(removeChange);

    verify(commandJourneyService, never()).deleteCommandJourney(any());
  }

  @Test
  void deleteStagedSurrenderArtefacts_whenTheChangeIsNotAPartialSurrender_thenThrowsAndDeletesNothing() {
    var administratorChange = AddChange.buildOperationsChange(
        List.of(LicenceOperation.newAdministratorChange().withOperator(ADMINISTRATOR_ID).build()), 1);
    var changeId = administratorChange.changeId();
    givenStagedChange(administratorChange, Optional.empty());

    assertThatThrownBy(() ->
        partialSurrenderCorrectionService.deleteStagedSurrenderArtefacts(administratorChange))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining(changeId);

    verify(commandJourneyService, never()).deleteCommandJourney(any());
  }

  @Test
  void getStagedPartialSurrenderOrThrow_whenTheChangeIsARemoveChange_thenReturnsTheLiveSurrender() {
    var live = fullSurrenderOf(FIRST_FEATURE_ID);
    var removeChange = LicencePositionChangeType.removeChange().withChangeId(LIVE_CHANGE_ID).build();
    var positionStagedPayload = updatePositionStagedPayload(List.of(removeChange));
    when(licencePositionStagedPayloadService.getStagedChangeOrThrow(positionStagedPayload, LIVE_CHANGE_ID))
        .thenReturn(removeChange);
    when(licencePositionStagedPayloadService.findStagedChangeOperation(removeChange, PartialSurrenderOperation.class))
        .thenReturn(Optional.of(live));

    var result = partialSurrenderCorrectionService
        .getStagedPartialSurrenderOrThrow(positionStagedPayload, LIVE_CHANGE_ID);

    assertThat(result).isEqualTo(live);
  }

  @Test
  void getStagedPartialSurrenderOrThrow_whenTheChangeIsAnAddChange_thenReturnsTheStagedSurrender() {
    var staged = partialSurrender(SECOND_FEATURE_ID);
    var addChange = AddChange.buildOperationsChange(List.of(staged), 1);
    var positionStagedPayload = updatePositionStagedPayload(List.of(addChange));
    when(licencePositionStagedPayloadService.getStagedChangeOrThrow(positionStagedPayload, addChange.changeId()))
        .thenReturn(addChange);
    when(licencePositionStagedPayloadService.findStagedChangeOperation(addChange, PartialSurrenderOperation.class))
        .thenReturn(Optional.of(staged));

    var result = partialSurrenderCorrectionService
        .getStagedPartialSurrenderOrThrow(positionStagedPayload, addChange.changeId());

    assertThat(result).isEqualTo(staged);
  }

  @Test
  void getBlockRows() {
    var retainedFeatureId = UUID.randomUUID();
    var surrender = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID))
        .withSurrenderDetails(Map.of(FIRST_FEATURE_ID, new PartialSurrenderOperation.SurrenderDetails(
            BlockSurrenderType.PARTIAL_SURRENDER, null, List.of(), List.of(retainedFeatureId))))
        .build();
    var block = FeatureTestUtil.builder()
        .withId(FIRST_FEATURE_ID)
        .withFeatureName("21/5")
        .withFeatureArea(new BigDecimal("219100000"))
        .build();
    var retainedPart = FeatureTestUtil.builder()
        .withId(retainedFeatureId)
        .withFeatureName("21/5a")
        .withFeatureArea(new BigDecimal("164200000"))
        .build();
    when(featureService.getFeaturesByIds(Set.of(FIRST_FEATURE_ID, retainedFeatureId)))
        .thenReturn(List.of(block, retainedPart));

    var result = partialSurrenderCorrectionService.getBlockRows(surrender);

    assertThat(result).containsExactly(new PartialSurrenderChangeView.BlockRow(
        "Block 21/5 (219.10\u00A0km²)",
        List.of("Block 21/5a retained (164.20\u00A0km²)", "54.90\u00A0km² surrendered")
    ));
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

  @Test
  void setBlockSurrenderType_whenTypeUnchanged_reusesJourneyAndSurrenderedAndRetainedFeatures() {
    var surrenderedPart = FeatureTestUtil.builder().build();
    var retainedPart = FeatureTestUtil.builder().build();
    var existingSurrenderDetails = new PartialSurrenderOperation.SurrenderDetails(
        BlockSurrenderType.PARTIAL_SURRENDER,
        FIRST_COMMAND_JOURNEY_ID,
        List.of(surrenderedPart.getId()),
        List.of(retainedPart.getId())
    );
    var existingOperation = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID))
        .withSurrenderDetails(Map.of(FIRST_FEATURE_ID, existingSurrenderDetails))
        .build();
    var positionStagedPayload = positionStagedPayload(
        List.of(AddChange.buildOperationsChange(List.of(existingOperation), 1)));
    givenCommittedPartialSurrender(positionStagedPayload, existingOperation);
    when(commandJourneyService.getActiveFeatures(FIRST_COMMAND_JOURNEY_ID))
        .thenReturn(List.of(surrenderedPart, retainedPart));

    partialSurrenderCorrectionService.setBlockSurrenderType(
        positionStagedPayload, FIRST_FEATURE_ID, BlockSurrenderType.PARTIAL_SURRENDER);

    verify(commandJourneyService, never()).createAndAssignCommandJourney(anyList());

    var expectedOperation = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID))
        .withSurrenderDetails(Map.of(FIRST_FEATURE_ID, existingSurrenderDetails))
        .build();
    verify(licencePositionStagedPayloadService)
        .replaceAddChangeFor(eq(positionStagedPayload), eq(PartialSurrenderOperation.class), partialSurrenderOperationCaptor.capture());
    assertThat(partialSurrenderOperationCaptor.getValue()).containsExactly(expectedOperation);
  }

  @Test
  void setBlockSurrenderType_whenTypeChanged_deletesOldJourneyAndCreatesNewWithNothingRetained() {
    var existingSurrenderDetails = new PartialSurrenderOperation.SurrenderDetails(
        BlockSurrenderType.PARTIAL_SURRENDER,
        FIRST_COMMAND_JOURNEY_ID,
        List.of(THIRD_FEATURE_ID),
        List.of(FOURTH_FEATURE_ID)
    );
    var existingOperation = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID))
        .withSurrenderDetails(Map.of(FIRST_FEATURE_ID, existingSurrenderDetails))
        .build();
    var positionStagedPayload = positionStagedPayload(
        List.of(AddChange.buildOperationsChange(List.of(existingOperation), 1)));
    givenCommittedPartialSurrender(positionStagedPayload, existingOperation);
    var feature = FeatureTestUtil.builder().withId(FIRST_FEATURE_ID).build();
    var createdJourney = new CommandJourney();
    createdJourney.setId(SECOND_COMMAND_JOURNEY_ID);

    when(featureService.getFeatureOrThrow(FIRST_FEATURE_ID)).thenReturn(feature);
    when(commandJourneyService.createAndAssignCommandJourney(List.of(feature))).thenReturn(createdJourney);

    partialSurrenderCorrectionService.setBlockSurrenderType(
        positionStagedPayload, FIRST_FEATURE_ID, BlockSurrenderType.FULL_SURRENDER);

    verify(commandJourneyService).deleteCommandJourney(FIRST_COMMAND_JOURNEY_ID);

    var expectedOperation = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID))
        .withSurrenderDetails(Map.of(FIRST_FEATURE_ID, new PartialSurrenderOperation.SurrenderDetails(
            BlockSurrenderType.FULL_SURRENDER, SECOND_COMMAND_JOURNEY_ID, List.of(FIRST_FEATURE_ID))))
        .build();
    verify(licencePositionStagedPayloadService)
        .replaceAddChangeFor(eq(positionStagedPayload), eq(PartialSurrenderOperation.class), partialSurrenderOperationCaptor.capture());
    assertThat(partialSurrenderOperationCaptor.getValue()).containsExactly(expectedOperation);
  }


  @Test
  void revertPartialSurrenderCorrection_whenTheStagedSurrenderMintedItsOwnJourney_deletesOnlyThatJourney() {
    var stagedSurrender = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID))
        .withSurrenderDetails(Map.of(
            FIRST_FEATURE_ID, surrenderDetails(BlockSurrenderType.PARTIAL_SURRENDER, SECOND_COMMAND_JOURNEY_ID)))
        .build();
    var positionStagedPayload = updatePositionStagedPayload(
        List.of(UpdateChangeOperations.buildUpdateChange(LIVE_CHANGE_ID, stagedSurrender)));
    when(licencePositionStagedPayloadService.findUpdatePositionStagedPayload(LICENCE_CORRECTION, LICENCE_POSITION))
        .thenReturn(Optional.of(positionStagedPayload));
    givenCommittedPartialSurrender(positionStagedPayload, stagedSurrender);

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
    when(licencePositionStagedPayloadService.findUpdatePositionStagedPayload(LICENCE_CORRECTION, LICENCE_POSITION))
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
    var positionStagedPayload = updatePositionStagedPayload(
        List.of(UpdateChangeOperations.buildUpdateChange(LIVE_CHANGE_ID, stagedSurrender)));
    when(licencePositionStagedPayloadService.findUpdatePositionStagedPayload(LICENCE_CORRECTION, LICENCE_POSITION))
        .thenReturn(Optional.of(positionStagedPayload));
    givenCommittedPartialSurrender(positionStagedPayload, stagedSurrender);

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
    var positionStagedPayload = positionStagedPayload(
        List.of(AddChange.buildOperationsChange(List.of(operation), 1)));
    givenCommittedPartialSurrender(positionStagedPayload, operation);

    assertThat(partialSurrenderCorrectionService.getSurrenderDetailsOrThrow(positionStagedPayload, FIRST_FEATURE_ID))
        .isEqualTo(surrenderDetails);
  }

  @Test
  void setSurrenderedFeatureIds_whenSeveralPartsKept_thenTheyAreMergedIntoOneRetainedFeature() {
    var firstSurrenderedPart = FeatureTestUtil.builder().withFeatureName("SHAPE 1A").build();
    var secondSurrenderedPart = FeatureTestUtil.builder().withFeatureName("SHAPE 1B").build();
    var firstRetainedPart = FeatureTestUtil.builder().withFeatureName("SHAPE 1D").build();
    var secondRetainedPart = FeatureTestUtil.builder().withFeatureName("SHAPE 1C").build();
    var mergedFeature = FeatureTestUtil.builder().build();
    var existingBlockSurrender = new PartialSurrenderOperation.SurrenderDetails(
        BlockSurrenderType.PARTIAL_SURRENDER, FIRST_COMMAND_JOURNEY_ID, List.of());
    var existingOperation = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID))
        .withSurrenderDetails(Map.of(FIRST_FEATURE_ID, existingBlockSurrender))
        .build();
    var positionStagedPayload = positionStagedPayload(
        List.of(AddChange.buildOperationsChange(List.of(existingOperation), 1)));
    givenCommittedPartialSurrender(positionStagedPayload, existingOperation);
    when(commandJourneyService.getActiveFeatures(FIRST_COMMAND_JOURNEY_ID)).thenReturn(List.of(
        firstSurrenderedPart,
        firstRetainedPart,
        secondSurrenderedPart,
        secondRetainedPart
    ));
    when(featureService.getFeaturesByIds(List.of(secondRetainedPart.getId(), firstRetainedPart.getId())))
        .thenReturn(List.of(secondRetainedPart, firstRetainedPart));
    when(mergeOperatorService.mergePolygons(List.of(secondRetainedPart, firstRetainedPart))).thenReturn(mergedFeature);

    partialSurrenderCorrectionService.setSurrenderedFeatureIds(
        positionStagedPayload,
        FIRST_FEATURE_ID,
        List.of(firstSurrenderedPart.getId(), secondSurrenderedPart.getId())
    );

    verify(commandJourneyService, never()).deleteCommandJourney(FIRST_COMMAND_JOURNEY_ID);

    var expectedOperation = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID))
        .withSurrenderDetails(Map.of(FIRST_FEATURE_ID, new PartialSurrenderOperation.SurrenderDetails(
            BlockSurrenderType.PARTIAL_SURRENDER,
            FIRST_COMMAND_JOURNEY_ID,
            List.of(firstSurrenderedPart.getId(), secondSurrenderedPart.getId()),
            List.of(secondRetainedPart.getId(), firstRetainedPart.getId()),
            Map.of(),
            mergedFeature.getId()
        )))
        .build();
    verify(licencePositionStagedPayloadService)
        .replaceAddChangeFor(eq(positionStagedPayload), eq(PartialSurrenderOperation.class), partialSurrenderOperationCaptor.capture());
    assertThat(partialSurrenderOperationCaptor.getValue()).containsExactly(expectedOperation);
  }

  @Test
  void setSurrenderedFeatureIds_whenOnePartKept_thenNothingIsMerged() {
    var surrenderedPart = FeatureTestUtil.builder().withFeatureName("SHAPE 1A").build();
    var retainedPart = FeatureTestUtil.builder().withFeatureName("SHAPE 1B").build();
    var existingOperation = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID))
        .withSurrenderDetails(Map.of(FIRST_FEATURE_ID,
            surrenderDetails(BlockSurrenderType.PARTIAL_SURRENDER, FIRST_COMMAND_JOURNEY_ID)))
        .build();
    var positionStagedPayload = positionStagedPayload(
        List.of(AddChange.buildOperationsChange(List.of(existingOperation), 1)));
    givenCommittedPartialSurrender(positionStagedPayload, existingOperation);
    when(commandJourneyService.getActiveFeatures(FIRST_COMMAND_JOURNEY_ID))
        .thenReturn(List.of(surrenderedPart, retainedPart));

    partialSurrenderCorrectionService.setSurrenderedFeatureIds(
        positionStagedPayload, FIRST_FEATURE_ID, List.of(surrenderedPart.getId()));

    var expectedOperation = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID))
        .withSurrenderDetails(Map.of(FIRST_FEATURE_ID, new PartialSurrenderOperation.SurrenderDetails(
            BlockSurrenderType.PARTIAL_SURRENDER,
            FIRST_COMMAND_JOURNEY_ID,
            List.of(surrenderedPart.getId()),
            List.of(retainedPart.getId())
        )))
        .build();
    verify(licencePositionStagedPayloadService)
        .replaceAddChangeFor(eq(positionStagedPayload), eq(PartialSurrenderOperation.class), partialSurrenderOperationCaptor.capture());
    assertThat(partialSurrenderOperationCaptor.getValue()).containsExactly(expectedOperation);
    verifyNoInteractions(mergeOperatorService);
  }

  @Test
  void setSurrenderedFeatureIds_whenOnePartKept_thenTheJourneyAndBothSplitPartsAreRecorded() {
    var surrenderedPart = FeatureTestUtil.builder().withFeatureName("SHAPE 1A").build();
    var retainedPart = FeatureTestUtil.builder().withFeatureName("SHAPE 1B").build();
    var existingOperation = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID))
        .withSurrenderDetails(Map.of(FIRST_FEATURE_ID,
            surrenderDetails(BlockSurrenderType.PARTIAL_SURRENDER, FIRST_COMMAND_JOURNEY_ID)))
        .build();
    var positionStagedPayload = positionStagedPayload(
        List.of(AddChange.buildOperationsChange(List.of(existingOperation), 1)));
    givenCommittedPartialSurrender(positionStagedPayload, existingOperation);
    when(commandJourneyService.getActiveFeatures(FIRST_COMMAND_JOURNEY_ID))
        .thenReturn(List.of(surrenderedPart, retainedPart));

    partialSurrenderCorrectionService.setSurrenderedFeatureIds(
        positionStagedPayload, FIRST_FEATURE_ID, List.of(surrenderedPart.getId()));

    verify(correctionOwnedGisArtefactService)
        .recordCommandJourneys(LICENCE_CORRECTION, Set.of(FIRST_COMMAND_JOURNEY_ID));
    verify(correctionOwnedGisArtefactService)
        .recordFeatures(LICENCE_CORRECTION, Set.of(surrenderedPart.getId(), retainedPart.getId()));
  }

  @Test
  void correctExistingPartialSurrender_whenFeaturesComeFromTheLiveSurrender_thenOnlyTheNewOnesAreRecorded() {
    var liveSurrenderedPartId = UUID.randomUUID();
    var liveRetainedPartId = UUID.randomUUID();
    var newSurrenderedPartId = UUID.randomUUID();
    var liveSurrender = surrenderOf(FIRST_FEATURE_ID, new PartialSurrenderOperation.SurrenderDetails(
        BlockSurrenderType.PARTIAL_SURRENDER, null, List.of(liveSurrenderedPartId), List.of(liveRetainedPartId)));
    var correctedSurrender = surrenderOf(FIRST_FEATURE_ID, new PartialSurrenderOperation.SurrenderDetails(
        BlockSurrenderType.PARTIAL_SURRENDER,
        null,
        List.of(liveSurrenderedPartId, newSurrenderedPartId),
        List.of(liveRetainedPartId)
    ));
    givenPositionStagedPayload(updatePositionStagedPayload(List.of()));
    givenLiveSurrenderChange(liveSurrender);

    partialSurrenderCorrectionService.correctExistingPartialSurrender(
        LICENCE_CORRECTION, LICENCE_POSITION, LIVE_CHANGE_ID, correctedSurrender);

    verify(correctionOwnedGisArtefactService).recordCommandJourneys(LICENCE_CORRECTION, Set.of());
    verify(correctionOwnedGisArtefactService).recordFeatures(LICENCE_CORRECTION, Set.of(newSurrenderedPartId));
  }

  @Test
  void setSurrenderedFeatureIds_whenKeptPartsUnchanged_thenThePreviousMergeIsReused() {
    var surrenderedPart = FeatureTestUtil.builder().withFeatureName("SHAPE 1A").build();
    var firstRetainedPart = FeatureTestUtil.builder().withFeatureName("SHAPE 1B").build();
    var secondRetainedPart = FeatureTestUtil.builder().withFeatureName("SHAPE 1C").build();
    var mergedFeatureId = UUID.randomUUID();
    var mergedDetails = new PartialSurrenderOperation.SurrenderDetails(
        BlockSurrenderType.PARTIAL_SURRENDER,
        FIRST_COMMAND_JOURNEY_ID,
        List.of(surrenderedPart.getId()),
        List.of(firstRetainedPart.getId(), secondRetainedPart.getId()),
        Map.of(),
        mergedFeatureId
    );
    var existingOperation = surrenderOf(FIRST_FEATURE_ID, mergedDetails);
    var positionStagedPayload = positionStagedPayload(
        List.of(AddChange.buildOperationsChange(List.of(existingOperation), 1)));
    givenCommittedPartialSurrender(positionStagedPayload, existingOperation);
    when(commandJourneyService.getActiveFeatures(FIRST_COMMAND_JOURNEY_ID))
        .thenReturn(List.of(surrenderedPart, firstRetainedPart, secondRetainedPart));

    partialSurrenderCorrectionService.setSurrenderedFeatureIds(
        positionStagedPayload, FIRST_FEATURE_ID, List.of(surrenderedPart.getId()));

    verify(licencePositionStagedPayloadService)
        .replaceAddChangeFor(eq(positionStagedPayload), eq(PartialSurrenderOperation.class), partialSurrenderOperationCaptor.capture());
    assertThat(partialSurrenderOperationCaptor.getValue()).containsExactly(existingOperation);
    verifyNoInteractions(mergeOperatorService);
    verify(commandJourneyService, never()).deleteFeaturesNotHeldByAJourney(any());
  }

  @Test
  void setSurrenderedFeatureIds_whenThePreviouslyMergedPartsChange_thenTheOldMergedFeatureIsDeleted() {
    var firstSurrenderedPart = FeatureTestUtil.builder().withFeatureName("SHAPE 1A").build();
    var secondSurrenderedPart = FeatureTestUtil.builder().withFeatureName("SHAPE 1B").build();
    var retainedPart = FeatureTestUtil.builder().withFeatureName("SHAPE 1C").build();
    var oldMergedFeature = FeatureTestUtil.builder().build();
    var existingOperation = surrenderOf(FIRST_FEATURE_ID, new PartialSurrenderOperation.SurrenderDetails(
        BlockSurrenderType.PARTIAL_SURRENDER,
        FIRST_COMMAND_JOURNEY_ID,
        List.of(firstSurrenderedPart.getId()),
        List.of(secondSurrenderedPart.getId(), retainedPart.getId()),
        Map.of(),
        oldMergedFeature.getId()
    ));
    var positionStagedPayload = positionStagedPayload(
        List.of(AddChange.buildOperationsChange(List.of(existingOperation), 1)));
    givenCommittedPartialSurrender(positionStagedPayload, existingOperation);
    when(commandJourneyService.getActiveFeatures(FIRST_COMMAND_JOURNEY_ID))
        .thenReturn(List.of(firstSurrenderedPart, secondSurrenderedPart, retainedPart));
    when(featureService.getFeaturesByIds(Set.of(oldMergedFeature.getId()))).thenReturn(List.of(oldMergedFeature));

    partialSurrenderCorrectionService.setSurrenderedFeatureIds(
        positionStagedPayload, FIRST_FEATURE_ID, List.of(firstSurrenderedPart.getId(), secondSurrenderedPart.getId()));

    var expectedOperation = surrenderOf(FIRST_FEATURE_ID, new PartialSurrenderOperation.SurrenderDetails(
        BlockSurrenderType.PARTIAL_SURRENDER,
        FIRST_COMMAND_JOURNEY_ID,
        List.of(firstSurrenderedPart.getId(), secondSurrenderedPart.getId()),
        List.of(retainedPart.getId())
    ));
    verify(licencePositionStagedPayloadService)
        .replaceAddChangeFor(eq(positionStagedPayload), eq(PartialSurrenderOperation.class), partialSurrenderOperationCaptor.capture());
    assertThat(partialSurrenderOperationCaptor.getValue()).containsExactly(expectedOperation);
    verify(commandJourneyService).deleteFeaturesNotHeldByAJourney(List.of(oldMergedFeature));
  }

  @Test
  void correctExistingPartialSurrender_whenABlockHasNoJourney_thenItsRetainedFeaturesAreNotMerged() {
    var positionStagedPayload = updatePositionStagedPayload(List.of());
    var recordedSurrender = surrenderOf(FIRST_FEATURE_ID, new PartialSurrenderOperation.SurrenderDetails(
        BlockSurrenderType.PARTIAL_SURRENDER, null, List.of(), List.of(SECOND_FEATURE_ID, THIRD_FEATURE_ID)));
    givenPositionStagedPayload(positionStagedPayload);
    givenLiveSurrenderChange(recordedSurrender);

    partialSurrenderCorrectionService.correctExistingPartialSurrender(
        LICENCE_CORRECTION, LICENCE_POSITION, LIVE_CHANGE_ID, recordedSurrender);

    assertThat(positionStagedPayload.getPayload().changes())
        .containsExactly(UpdateChangeOperations.buildUpdateChange(LIVE_CHANGE_ID, recordedSurrender));
    verifyNoInteractions(mergeOperatorService);
  }

  @Test
  void setSurrenderedFeatureIds_whenTheSurrenderCorrectsALiveChange_thenTheUpdateChangeIsReplacedInPlace() {
    var surrenderedId = UUID.randomUUID();
    var existingOperation = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID))
        .withSurrenderDetails(Map.of(FIRST_FEATURE_ID, surrenderDetails(
            BlockSurrenderType.PARTIAL_SURRENDER, FIRST_COMMAND_JOURNEY_ID)))
        .build();
    var positionStagedPayload = updatePositionStagedPayload(
        List.of(UpdateChangeOperations.buildUpdateChange(LIVE_CHANGE_ID, existingOperation)));
    givenCommittedPartialSurrender(positionStagedPayload, existingOperation);

    partialSurrenderCorrectionService.setSurrenderedFeatureIds(
        positionStagedPayload, FIRST_FEATURE_ID, List.of(surrenderedId));

    var expectedOperation = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID))
        .withSurrenderDetails(Map.of(FIRST_FEATURE_ID, new PartialSurrenderOperation.SurrenderDetails(
            BlockSurrenderType.PARTIAL_SURRENDER, FIRST_COMMAND_JOURNEY_ID, List.of(surrenderedId))))
        .build();
    assertThat(positionStagedPayload.getPayload().changes())
        .containsExactly(UpdateChangeOperations.buildUpdateChange(LIVE_CHANGE_ID, expectedOperation));
    verify(licencePositionStagedPayloadService).save(positionStagedPayload);
    verify(licencePositionStagedPayloadService, never()).replaceAddChangeFor(any(), any(), any());
  }

  @Test
  void clearSurrenderedIds_whenSomeSurrenderedIdsNoLongerActive_clearsSurrenderedAndRetainedIds() {
    var staleSurrenderedId = UUID.randomUUID();
    var existingBlockSurrender = new PartialSurrenderOperation.SurrenderDetails(
        BlockSurrenderType.PARTIAL_SURRENDER,
        FIRST_COMMAND_JOURNEY_ID,
        List.of(staleSurrenderedId),
        List.of(SECOND_FEATURE_ID)
    );
    var existingOperation = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID))
        .withSurrenderDetails(Map.of(FIRST_FEATURE_ID, existingBlockSurrender))
        .build();
    var positionStagedPayload = positionStagedPayload(
        List.of(AddChange.buildOperationsChange(List.of(existingOperation), 1)));
    givenCommittedPartialSurrender(positionStagedPayload, existingOperation);

    partialSurrenderCorrectionService.clearSurrenderedIds(
        positionStagedPayload, FIRST_FEATURE_ID, List.of(UUID.randomUUID()));

    var expectedOperation = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID))
        .withSurrenderDetails(Map.of(FIRST_FEATURE_ID, new PartialSurrenderOperation.SurrenderDetails(
            BlockSurrenderType.PARTIAL_SURRENDER, FIRST_COMMAND_JOURNEY_ID, List.of())))
        .build();
    verify(licencePositionStagedPayloadService)
        .replaceAddChangeFor(eq(positionStagedPayload), eq(PartialSurrenderOperation.class), partialSurrenderOperationCaptor.capture());
    assertThat(partialSurrenderOperationCaptor.getValue()).containsExactly(expectedOperation);
  }

  @Test
  void clearSurrenderedIds_whenTheSelectionStillMatchesTheActiveAreas_doesNothing() {
    var surrenderedId = UUID.randomUUID();
    var retainedId = UUID.randomUUID();
    var existingBlockSurrender = new PartialSurrenderOperation.SurrenderDetails(
        BlockSurrenderType.PARTIAL_SURRENDER,
        FIRST_COMMAND_JOURNEY_ID,
        List.of(surrenderedId),
        List.of(retainedId)
    );
    var existingOperation = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID))
        .withSurrenderDetails(Map.of(FIRST_FEATURE_ID, existingBlockSurrender))
        .build();
    var positionStagedPayload = positionStagedPayload(
        List.of(AddChange.buildOperationsChange(List.of(existingOperation), 1)));
    givenCommittedPartialSurrender(positionStagedPayload, existingOperation);

    partialSurrenderCorrectionService.clearSurrenderedIds(
        positionStagedPayload,
        FIRST_FEATURE_ID,
        List.of(surrenderedId, retainedId)
    );

    verify(licencePositionStagedPayloadService, never())
        .replaceAddChangeFor(eq(positionStagedPayload), eq(PartialSurrenderOperation.class), anyList());
  }

  @Test
  void clearSurrenderedIds_whenOnlyAnAreaKeptWasSplitAgain_clearsSurrenderedAndRetainedIds() {
    var surrenderedId = UUID.randomUUID();
    var existingBlockSurrender = new PartialSurrenderOperation.SurrenderDetails(
        BlockSurrenderType.PARTIAL_SURRENDER,
        FIRST_COMMAND_JOURNEY_ID,
        List.of(surrenderedId),
        List.of(UUID.randomUUID())
    );
    var existingOperation = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID))
        .withSurrenderDetails(Map.of(FIRST_FEATURE_ID, existingBlockSurrender))
        .build();
    var positionStagedPayload = positionStagedPayload(
        List.of(AddChange.buildOperationsChange(List.of(existingOperation), 1)));
    givenCommittedPartialSurrender(positionStagedPayload, existingOperation);

    partialSurrenderCorrectionService.clearSurrenderedIds(
        positionStagedPayload,
        FIRST_FEATURE_ID,
        List.of(surrenderedId, UUID.randomUUID(), UUID.randomUUID())
    );

    var expectedOperation = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID))
        .withSurrenderDetails(Map.of(FIRST_FEATURE_ID, new PartialSurrenderOperation.SurrenderDetails(
            BlockSurrenderType.PARTIAL_SURRENDER,
            FIRST_COMMAND_JOURNEY_ID,
            List.of()
        )))
        .build();
    verify(licencePositionStagedPayloadService).replaceAddChangeFor(
        eq(positionStagedPayload),
        eq(PartialSurrenderOperation.class),
        partialSurrenderOperationCaptor.capture()
    );
    assertThat(partialSurrenderOperationCaptor.getValue()).containsExactly(expectedOperation);
  }

  @Test
  void clearSurrenderedIds_whenNoSurrenderedIds_doesNothing() {
    var existingBlockSurrender = new PartialSurrenderOperation.SurrenderDetails(
        BlockSurrenderType.PARTIAL_SURRENDER, FIRST_COMMAND_JOURNEY_ID, List.of());
    var existingOperation = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID))
        .withSurrenderDetails(Map.of(FIRST_FEATURE_ID, existingBlockSurrender))
        .build();
    var positionStagedPayload = positionStagedPayload(
        List.of(AddChange.buildOperationsChange(List.of(existingOperation), 1)));
    givenCommittedPartialSurrender(positionStagedPayload, existingOperation);

    partialSurrenderCorrectionService.clearSurrenderedIds(
        positionStagedPayload, FIRST_FEATURE_ID, List.of(UUID.randomUUID()));

    verify(licencePositionStagedPayloadService, never())
        .replaceAddChangeFor(eq(positionStagedPayload), eq(PartialSurrenderOperation.class), anyList());
  }

  @Test
  void clearSurrenderedIds_whenNoBlockSurrenderForFeature_doesNothing() {
    var existingOperation = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID))
        .build();
    var positionStagedPayload = positionStagedPayload(
        List.of(AddChange.buildOperationsChange(List.of(existingOperation), 1)));
    givenCommittedPartialSurrender(positionStagedPayload, existingOperation);

    partialSurrenderCorrectionService.clearSurrenderedIds(
        positionStagedPayload, FIRST_FEATURE_ID, List.of(UUID.randomUUID()));

    verify(licencePositionStagedPayloadService, never())
        .replaceAddChangeFor(eq(positionStagedPayload), eq(PartialSurrenderOperation.class), anyList());
  }

  @Test
  void getBlockSurrenderOrThrow_whenNoSurrenderForFeature_throws() {
    var operation = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID))
        .build();
    var positionStagedPayload = positionStagedPayload(
        List.of(AddChange.buildOperationsChange(List.of(operation), 1)));
    givenCommittedPartialSurrender(positionStagedPayload, operation);

    assertThatThrownBy(() ->
        partialSurrenderCorrectionService.getSurrenderDetailsOrThrow(positionStagedPayload, FIRST_FEATURE_ID))
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
            BlockSurrenderType.PARTIAL_SURRENDER,
            null,
            List.of(SECOND_FEATURE_ID),
            List.of(THIRD_FEATURE_ID)
        )))
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
  void toExecutedSurrender_whenRetainedPartsNotMerged_thenRecordsTheRetainedPartsWithoutJourneys() {
    var surrenderDate = LocalDate.of(2026, Month.AUGUST, 1);
    var recordedDetails = new PartialSurrenderOperation.SurrenderDetails(
        BlockSurrenderType.FULL_SURRENDER, null, List.of(FOURTH_FEATURE_ID));
    var retainedFeatureIdToSubareas = Map.of(THIRD_FEATURE_ID, List.of(
        SubareaSurrenderOutcome.relinquished(new SubareaDetails(UUID.randomUUID(), "Subarea A", "A")),
        SubareaSurrenderOutcome.kept(new SubareaDetails(UUID.randomUUID(), "Subarea B", "B"))));
    var staged = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderDate(surrenderDate)
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID, FOURTH_FEATURE_ID))
        .withSurrenderDetails(Map.of(
            FIRST_FEATURE_ID, new PartialSurrenderOperation.SurrenderDetails(
                BlockSurrenderType.PARTIAL_SURRENDER,
                FIRST_COMMAND_JOURNEY_ID,
                List.of(SECOND_FEATURE_ID),
                List.of(THIRD_FEATURE_ID),
                retainedFeatureIdToSubareas,
                null
            ),
            FOURTH_FEATURE_ID, recordedDetails))
        .build();

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
                retainedFeatureIdToSubareas,
                null
            ),
            FOURTH_FEATURE_ID, recordedDetails))
        .build();
    assertThat(result).isEqualTo(expected);
  }

  @Test
  void toExecutedSurrender_whenRetainedPartsMerged_thenRecordsTheMergedFeatureAsTheOneRetainedFeature() {
    var mergedFeatureId = UUID.randomUUID();
    var retainedFeatureIdToSubareas = Map.of(mergedFeatureId, List.of(SubareaSurrenderOutcome.kept(SUBAREA)));
    var staged = surrenderOf(FIRST_FEATURE_ID, new PartialSurrenderOperation.SurrenderDetails(
        BlockSurrenderType.PARTIAL_SURRENDER,
        FIRST_COMMAND_JOURNEY_ID,
        List.of(SECOND_FEATURE_ID),
        List.of(THIRD_FEATURE_ID, FOURTH_FEATURE_ID),
        retainedFeatureIdToSubareas,
        mergedFeatureId
    ));

    var result = partialSurrenderCorrectionService.toExecutedSurrender(staged);

    var expected = surrenderOf(FIRST_FEATURE_ID, new PartialSurrenderOperation.SurrenderDetails(
        BlockSurrenderType.PARTIAL_SURRENDER,
        null,
        List.of(SECOND_FEATURE_ID),
        List.of(mergedFeatureId),
        retainedFeatureIdToSubareas,
        null
    ));
    assertThat(result).isEqualTo(expected);
  }

  @Test
  void setFeatureDates_thenWhatTheSurrenderCreatesStartsAndWhatItEndsEndsOnThePositionDate() {
    var positionDate = LocalDate.of(2026, Month.AUGUST, 1);
    var block = FeatureTestUtil.builder().withId(FIRST_FEATURE_ID).build();
    var surrenderedPart = FeatureTestUtil.builder().withId(SECOND_FEATURE_ID).build();
    var retainedPart = FeatureTestUtil.builder().withId(THIRD_FEATURE_ID).build();
    var surrender = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID))
        .withSurrenderDetails(Map.of(FIRST_FEATURE_ID, new PartialSurrenderOperation.SurrenderDetails(
            BlockSurrenderType.PARTIAL_SURRENDER,
            null,
            List.of(SECOND_FEATURE_ID),
            List.of(THIRD_FEATURE_ID)
        )))
        .build();
    when(featureService.getFeaturesByIds(Set.of(FIRST_FEATURE_ID, SECOND_FEATURE_ID, THIRD_FEATURE_ID)))
        .thenReturn(List.of(block, surrenderedPart, retainedPart));

    partialSurrenderCorrectionService.setFeatureDates(surrender, positionDate);

    var expectedBlock = FeatureTestUtil.builder().withId(FIRST_FEATURE_ID).build();
    expectedBlock.setEndDate(positionDate);
    var expectedSurrenderedPart = FeatureTestUtil.builder().withId(SECOND_FEATURE_ID).build();
    expectedSurrenderedPart.setStartDate(positionDate);
    expectedSurrenderedPart.setEndDate(positionDate);
    var expectedRetainedPart = FeatureTestUtil.builder().withId(THIRD_FEATURE_ID).build();
    expectedRetainedPart.setStartDate(positionDate);
    assertThat(List.of(block, surrenderedPart, retainedPart))
        .usingRecursiveComparison()
        .isEqualTo(List.of(expectedBlock, expectedSurrenderedPart, expectedRetainedPart));
    verify(featureService).saveFeature(block);
    verify(featureService).saveFeature(surrenderedPart);
    verify(featureService).saveFeature(retainedPart);
  }

  @Test
  void revertFeatures_thenWhatOnlyTheSurrenderCreatedIsDeletedAndWhatItEndedIsReopened() {
    var positionDate = LocalDate.of(2026, Month.AUGUST, 1);
    var block = FeatureTestUtil.builder().withId(FIRST_FEATURE_ID).build();
    block.setEndDate(positionDate);
    var surrenderedPart = FeatureTestUtil.builder().withId(SECOND_FEATURE_ID).build();
    var stillCreatedPart = FeatureTestUtil.builder().withId(FOURTH_FEATURE_ID).build();
    stillCreatedPart.setStartDate(positionDate);
    stillCreatedPart.setEndDate(positionDate);
    var surrender = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID))
        .withSurrenderDetails(Map.of(FIRST_FEATURE_ID, new PartialSurrenderOperation.SurrenderDetails(
            BlockSurrenderType.PARTIAL_SURRENDER,
            null,
            List.of(SECOND_FEATURE_ID, FOURTH_FEATURE_ID),
            List.of(THIRD_FEATURE_ID)
        )))
        .build();
    when(featureService.getFeaturesByIds(Set.of(SECOND_FEATURE_ID, THIRD_FEATURE_ID)))
        .thenReturn(List.of(surrenderedPart));
    when(featureService.getFeaturesByIds(Set.of(FIRST_FEATURE_ID, FOURTH_FEATURE_ID)))
        .thenReturn(List.of(block, stillCreatedPart));

    partialSurrenderCorrectionService.revertFeatures(surrender, Set.of(FOURTH_FEATURE_ID));

    verify(featureService).deleteAll(List.of(surrenderedPart));
    var expectedBlock = FeatureTestUtil.builder().withId(FIRST_FEATURE_ID).build();
    var expectedStillCreatedPart = FeatureTestUtil.builder().withId(FOURTH_FEATURE_ID).build();
    expectedStillCreatedPart.setStartDate(positionDate);
    assertThat(List.of(block, stillCreatedPart))
        .usingRecursiveComparison()
        .isEqualTo(List.of(expectedBlock, expectedStillCreatedPart));
    verify(featureService).saveFeature(block);
    verify(featureService).saveFeature(stillCreatedPart);
  }

  @Test
  void revertFeatures_whenEverythingCreatedIsStillCreated_thenNothingIsDeleted() {
    var surrender = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID))
        .withSurrenderDetails(Map.of(FIRST_FEATURE_ID, new PartialSurrenderOperation.SurrenderDetails(
            BlockSurrenderType.FULL_SURRENDER,
            null,
            List.of(FIRST_FEATURE_ID)
        )))
        .build();
    when(featureService.getFeaturesByIds(Set.of(FIRST_FEATURE_ID))).thenReturn(List.of());

    partialSurrenderCorrectionService.revertFeatures(surrender, Set.of());

    verify(featureService, never()).deleteAll(anyList());
  }

  @Test
  void setSurrenderedFeatureIds_whenProductionLicence_thenTheBlocksSubareasAreProcessed() {
    var surrenderedPart = FeatureTestUtil.builder().withFeatureName("SHAPE 1A").build();
    var retainedPart = FeatureTestUtil.builder().withFeatureName("SHAPE 1B").build();
    var existingOperation = surrenderOf(FIRST_FEATURE_ID, new PartialSurrenderOperation.SurrenderDetails(
        BlockSurrenderType.PARTIAL_SURRENDER, FIRST_COMMAND_JOURNEY_ID, List.of()));
    var stagedChange = AddChange.buildOperationsChange(List.of(existingOperation), 1);
    var positionStagedPayload = productionPositionStagedPayload(List.of(stagedChange));
    givenCommittedPartialSurrender(positionStagedPayload, existingOperation);
    when(commandJourneyService.getActiveFeatures(FIRST_COMMAND_JOURNEY_ID)).thenReturn(List.of(surrenderedPart, retainedPart));
    var selectedDetails = new PartialSurrenderOperation.SurrenderDetails(
        BlockSurrenderType.PARTIAL_SURRENDER,
        FIRST_COMMAND_JOURNEY_ID,
        List.of(surrenderedPart.getId()),
        List.of(retainedPart.getId())
    );
    var processedDetails = selectedDetails.withSubareas(Map.of(THIRD_FEATURE_ID, List.of(SubareaSurrenderOutcome.relinquished(SUBAREA))));
    when(partialSurrenderSubareaService.processSubareas(
        PRODUCTION_CORRECTION,
        positionStagedPayload.getPositionId(),
        stagedChange.changeId(),
        FIRST_FEATURE_ID,
        selectedDetails,
        List.of(retainedPart.getId()),
        null
    )).thenReturn(processedDetails);

    partialSurrenderCorrectionService.setSurrenderedFeatureIds(
        positionStagedPayload, FIRST_FEATURE_ID, List.of(surrenderedPart.getId()));

    verify(licencePositionStagedPayloadService)
        .replaceAddChangeFor(eq(positionStagedPayload), eq(PartialSurrenderOperation.class), partialSurrenderOperationCaptor.capture());
    assertThat(partialSurrenderOperationCaptor.getValue()).containsExactly(surrenderOf(FIRST_FEATURE_ID, processedDetails));
  }

  @Test
  void commitPartialSurrender_whenABlockIsSurrenderedAsItWasBefore_thenItsStagedSubareasAreOfferedForReuse() {
    var stagedDetails = new PartialSurrenderOperation.SurrenderDetails(
        BlockSurrenderType.FULL_SURRENDER, FIRST_COMMAND_JOURNEY_ID, List.of(FIRST_FEATURE_ID))
        .withSubareas(Map.of(THIRD_FEATURE_ID, List.of(SubareaSurrenderOutcome.relinquished(SUBAREA))));
    var staged = surrenderOf(FIRST_FEATURE_ID, stagedDetails);
    var stagedChange = AddChange.buildOperationsChange(List.of(staged), 1);
    var positionStagedPayload = productionPositionStagedPayload(List.of(stagedChange));
    givenCommittedPartialSurrender(positionStagedPayload, staged);
    var resubmittedDetails = new PartialSurrenderOperation.SurrenderDetails(
        BlockSurrenderType.FULL_SURRENDER, FIRST_COMMAND_JOURNEY_ID, List.of(FIRST_FEATURE_ID));
    when(partialSurrenderSubareaService.processSubareas(
        PRODUCTION_CORRECTION,
        positionStagedPayload.getPositionId(),
        stagedChange.changeId(),
        FIRST_FEATURE_ID,
        resubmittedDetails,
        List.of(),
        stagedDetails
    )).thenReturn(stagedDetails);

    partialSurrenderCorrectionService.commitPartialSurrender(
        positionStagedPayload, surrenderOf(FIRST_FEATURE_ID, resubmittedDetails));

    verify(licencePositionStagedPayloadService)
        .replaceAddChangeFor(eq(positionStagedPayload), eq(PartialSurrenderOperation.class), partialSurrenderOperationCaptor.capture());
    assertThat(partialSurrenderOperationCaptor.getValue()).containsExactly(staged);
  }

  @Test
  void commitPartialSurrender_whenAStagedCroppedSubareaIsNoLongerReferenced_thenItIsDeleted() {
    var croppedFeature = FeatureTestUtil.builder().build();
    var staged = croppedSurrenderOf(FIRST_FEATURE_ID, croppedFeature);
    var stagedChange = AddChange.buildOperationsChange(List.of(staged), 1);
    var positionStagedPayload = productionPositionStagedPayload(List.of(stagedChange));
    givenCommittedPartialSurrender(positionStagedPayload, staged);
    var resubmittedDetails = new PartialSurrenderOperation.SurrenderDetails(
        BlockSurrenderType.FULL_SURRENDER, FIRST_COMMAND_JOURNEY_ID, List.of(FIRST_FEATURE_ID));
    when(partialSurrenderSubareaService.processSubareas(
        PRODUCTION_CORRECTION,
        positionStagedPayload.getPositionId(),
        stagedChange.changeId(),
        FIRST_FEATURE_ID,
        resubmittedDetails,
        List.of(),
        null
    )).thenReturn(resubmittedDetails.withSubareas(Map.of(THIRD_FEATURE_ID, List.of(SubareaSurrenderOutcome.relinquished(SUBAREA)))));
    when(featureService.getFeaturesByIds(Set.of(croppedFeature.getId()))).thenReturn(List.of(croppedFeature));

    partialSurrenderCorrectionService.commitPartialSurrender(
        positionStagedPayload, surrenderOf(FIRST_FEATURE_ID, resubmittedDetails));

    verify(commandJourneyService).deleteFeaturesNotHeldByAJourney(List.of(croppedFeature));
  }

  @Test
  void commitPartialSurrender_whenCarbonStorageLicence_thenSubareasAreNotProcessed() {
    var positionStagedPayload = positionStagedPayload();

    partialSurrenderCorrectionService.commitPartialSurrender(positionStagedPayload, fullSurrenderOf(FIRST_FEATURE_ID));

    verifyNoInteractions(partialSurrenderSubareaService);
  }

  @Test
  void correctExistingPartialSurrender_whenNothingStagedAndABlockIsSurrenderedAsItIsLive_thenTheLiveSubareasAreOfferedForReuse() {
    var liveDetails = new PartialSurrenderOperation.SurrenderDetails(
        BlockSurrenderType.FULL_SURRENDER, null, List.of(FIRST_FEATURE_ID))
        .withSubareas(Map.of(THIRD_FEATURE_ID, List.of(SubareaSurrenderOutcome.relinquished(SUBAREA))));
    givenLiveSurrenderChange(surrenderOf(FIRST_FEATURE_ID, liveDetails));
    var positionStagedPayload = productionUpdatePositionStagedPayload(List.of());
    givenProductionPositionStagedPayload(positionStagedPayload);
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

    assertThat(positionStagedPayload.getPayload().changes())
        .containsExactly(UpdateChangeOperations.buildUpdateChange(
            LIVE_CHANGE_ID, surrenderOf(FIRST_FEATURE_ID, processedDetails)));
  }

  @Test
  void correctExistingPartialSurrender_whenAStagedCroppedSubareaIsAlsoLive_thenItIsNotDeleted() {
    var croppedSurrender = croppedSurrenderOf(FIRST_FEATURE_ID, FeatureTestUtil.builder().build());
    givenLiveSurrenderChange(croppedSurrender);
    var positionStagedPayload = productionUpdatePositionStagedPayload(
        List.of(UpdateChangeOperations.buildUpdateChange(LIVE_CHANGE_ID, croppedSurrender)));
    givenProductionPositionStagedPayload(positionStagedPayload);
    givenCommittedPartialSurrender(positionStagedPayload, croppedSurrender);
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

    verify(commandJourneyService, never()).deleteFeaturesNotHeldByAJourney(any());
  }

  @Test
  void setSurrenderedFeatureIds_whenAStagedCroppedSubareaIsAlsoLive_thenItIsNotDeleted() {
    var croppedSurrender = croppedSurrenderOf(FIRST_FEATURE_ID, FeatureTestUtil.builder().build());
    givenLiveSurrenderChange(croppedSurrender);
    var positionStagedPayload = productionUpdatePositionStagedPayload(
        List.of(UpdateChangeOperations.buildUpdateChange(LIVE_CHANGE_ID, croppedSurrender)));
    givenCommittedPartialSurrender(positionStagedPayload, croppedSurrender);
    var selectedDetails = new PartialSurrenderOperation.SurrenderDetails(
        BlockSurrenderType.PARTIAL_SURRENDER, null, List.of(SECOND_FEATURE_ID));
    when(partialSurrenderSubareaService.processSubareas(
        PRODUCTION_CORRECTION,
        positionStagedPayload.getPositionId(),
        LIVE_CHANGE_ID,
        FIRST_FEATURE_ID,
        selectedDetails,
        List.of(),
        null
    )).thenReturn(selectedDetails.withSubareas(Map.of(THIRD_FEATURE_ID, List.of(SubareaSurrenderOutcome.relinquished(SUBAREA)))));

    partialSurrenderCorrectionService.setSurrenderedFeatureIds(
        positionStagedPayload, FIRST_FEATURE_ID, List.of(SECOND_FEATURE_ID));

    verify(commandJourneyService, never()).deleteFeaturesNotHeldByAJourney(any());
  }

  @Test
  void deleteStagedSurrenderArtefacts_whenTheSurrenderWasAddedByThisCorrection_thenDeletesItsCroppedSubareas() {
    var croppedFeature = FeatureTestUtil.builder().build();
    var croppedSurrender = croppedSurrenderOf(FIRST_FEATURE_ID, croppedFeature);
    var addChange = AddChange.buildOperationsChange(List.of(croppedSurrender), 1);
    givenStagedChange(addChange, croppedSurrender);
    when(featureService.getFeaturesByIds(Set.of(croppedFeature.getId()))).thenReturn(List.of(croppedFeature));

    partialSurrenderCorrectionService.deleteStagedSurrenderArtefacts(addChange);

    verify(commandJourneyService).deleteFeaturesNotHeldByAJourney(List.of(croppedFeature));
  }

  @Test
  void deleteStagedSurrenderArtefacts_whenTheSurrenderCorrectsALiveChange_thenKeepsTheLiveCroppedSubareas() {
    var croppedSurrender = croppedSurrenderOf(FIRST_FEATURE_ID, FeatureTestUtil.builder().build());
    var updateChange = UpdateChangeOperations.buildUpdateChange(LIVE_CHANGE_ID, croppedSurrender);
    givenStagedChange(updateChange, croppedSurrender);
    givenLiveSurrenderChange(croppedSurrender);

    partialSurrenderCorrectionService.deleteStagedSurrenderArtefacts(updateChange);

    verify(commandJourneyService, never()).deleteFeaturesNotHeldByAJourney(any());
  }

  @Test
  void revertPartialSurrenderCorrection_whenTheStagedSurrenderCroppedSubareas_thenDeletesThoseNotLive() {
    var croppedFeature = FeatureTestUtil.builder().build();
    var staged = croppedSurrenderOf(FIRST_FEATURE_ID, croppedFeature);
    var positionStagedPayload = updatePositionStagedPayload(
        List.of(UpdateChangeOperations.buildUpdateChange(LIVE_CHANGE_ID, staged)));
    when(licencePositionStagedPayloadService.findUpdatePositionStagedPayload(LICENCE_CORRECTION, LICENCE_POSITION))
        .thenReturn(Optional.of(positionStagedPayload));
    givenCommittedPartialSurrender(positionStagedPayload, staged);
    givenLiveSurrenderChange(fullSurrenderOf(FIRST_FEATURE_ID));
    when(featureService.getFeaturesByIds(Set.of(croppedFeature.getId()))).thenReturn(List.of(croppedFeature));

    partialSurrenderCorrectionService.revertPartialSurrenderCorrection(LICENCE_CORRECTION, LICENCE_POSITION, staged);

    verify(commandJourneyService).deleteFeaturesNotHeldByAJourney(List.of(croppedFeature));
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
    var positionStagedPayload = executedPositionStagedPayload(List.of(stagedChange));
    givenCommittedPartialSurrender(positionStagedPayload, surrender);
    when(licencePositionSpatialService.getBlockFeaturesGoingIntoChange(positionStagedPayload, stagedChange.changeId()))
        .thenReturn(List.of(FeatureTestUtil.blockFeature(SECOND_FEATURE_ID, "30", 2)));
    when(featureService.getFeaturesByIds(Set.of(croppedFeature.getId()))).thenReturn(List.of(croppedFeature));

    partialSurrenderCorrectionService.adjustPartialSurrenderBlocks(positionStagedPayload);

    verify(commandJourneyService).deleteFeaturesNotHeldByAJourney(List.of(croppedFeature));
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
    var positionStagedPayload = updatePositionStagedPayload(
        List.of(UpdateChangeOperations.buildUpdateChange(LIVE_CHANGE_ID, surrender)));
    givenCommittedPartialSurrender(positionStagedPayload, surrender);
    givenLiveSurrenderChange(surrender);
    when(licencePositionSpatialService.getBlockFeaturesGoingIntoChange(positionStagedPayload, LIVE_CHANGE_ID))
        .thenReturn(List.of(FeatureTestUtil.blockFeature(SECOND_FEATURE_ID, "30", 2)));

    partialSurrenderCorrectionService.adjustPartialSurrenderBlocks(positionStagedPayload);

    verify(commandJourneyService, never()).deleteFeaturesNotHeldByAJourney(any());
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

  private static LicencePositionStagedPayload productionPositionStagedPayload(List<LicencePositionChangeType> changes) {
    return LicencePositionStagedPayloadTestUtil.newBuilder()
        .withLicenceCorrection(PRODUCTION_CORRECTION)
        .withTargetLicencePosition(null)
        .withPayload(CreateLicencePositionPayloadTestUtil.newBuilder().withChanges(changes).build())
        .build();
  }

  private static LicencePositionStagedPayload productionUpdatePositionStagedPayload(List<LicencePositionChangeType> changes) {
    return LicencePositionStagedPayloadTestUtil.newBuilder()
        .withLicenceCorrection(PRODUCTION_CORRECTION)
        .withChangeType(LicencePositionStagedPayloadChangeType.UPDATE_POSITION)
        .withTargetLicencePosition(LICENCE_POSITION)
        .withPayload(LicencePositionPayload.newUpdateLicencePositionPayload().withChanges(changes).build())
        .build();
  }

  private void givenProductionPositionStagedPayload(LicencePositionStagedPayload positionStagedPayload) {
    when(licencePositionStagedPayloadService.getOrBuildUpdatePositionStagedPayload(PRODUCTION_CORRECTION, LICENCE_POSITION))
        .thenReturn(positionStagedPayload);
    when(licencePositionStagedPayloadService.save(positionStagedPayload)).thenReturn(positionStagedPayload);
  }

  private static PartialSurrenderOperation.SurrenderDetails surrenderDetails(
      BlockSurrenderType type,
      UUID commandJourneyId
  ) {
    return new PartialSurrenderOperation.SurrenderDetails(type, commandJourneyId, List.of());
  }

  private static LicencePositionStagedPayload positionStagedPayload() {
    return positionStagedPayload(List.of());
  }

  private static LicencePositionStagedPayload positionStagedPayload(List<LicencePositionChangeType> changes) {
    return LicencePositionStagedPayloadTestUtil.newBuilder()
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
      LicencePositionStagedPayload positionStagedPayload,
      PartialSurrenderOperation operation
  ) {
    when(licencePositionStagedPayloadService.getCommittedChangeOfType(positionStagedPayload, PartialSurrenderOperation.class))
        .thenReturn(Optional.of(operation));
  }

  private void givenActiveFeaturesOnFirstJourney() {
    when(commandJourneyService.getActiveFeatures(FIRST_COMMAND_JOURNEY_ID)).thenReturn(List.of(
        FeatureTestUtil.builder().withId(SECOND_FEATURE_ID).build(),
        FeatureTestUtil.builder().withId(THIRD_FEATURE_ID).build()
    ));
  }

  private void givenPositionStagedPayload(LicencePositionStagedPayload positionStagedPayload) {
    when(licencePositionStagedPayloadService.getOrBuildUpdatePositionStagedPayload(LICENCE_CORRECTION, LICENCE_POSITION))
        .thenReturn(positionStagedPayload);
    when(licencePositionStagedPayloadService.save(positionStagedPayload)).thenReturn(positionStagedPayload);
  }

  private static LicencePositionStagedPayload updatePositionStagedPayload(List<LicencePositionChangeType> changes) {
    return LicencePositionStagedPayloadTestUtil.newBuilder()
        .withLicenceCorrection(LICENCE_CORRECTION)
        .withChangeType(LicencePositionStagedPayloadChangeType.UPDATE_POSITION)
        .withTargetLicencePosition(LICENCE_POSITION)
        .withPayload(LicencePositionPayload.newUpdateLicencePositionPayload().withChanges(changes).build())
        .build();
  }

  private static LicencePositionStagedPayload executedPositionStagedPayload() {
    return executedPositionStagedPayload(List.of());
  }

  private static LicencePositionStagedPayload executedPositionStagedPayload(List<LicencePositionChangeType> changes) {
    return LicencePositionStagedPayloadTestUtil.newBuilder()
        .withLicenceCorrection(LICENCE_CORRECTION)
        .withTargetLicencePosition(LICENCE_POSITION)
        .withPayload(UpdateLicencePositionPayloadTestUtil.newBuilder().withChanges(changes).build())
        .build();
  }

  private void givenCommandJourneyCreatedFor(
      Feature block,
      UUID commandJourneyId
  ) {
    when(featureService.getFeatureOrThrow(block.getId())).thenReturn(block);
    var commandJourney = new CommandJourney();
    commandJourney.setId(commandJourneyId);
    when(commandJourneyService.createAndAssignCommandJourney(List.of(block))).thenReturn(commandJourney);
  }

  private static PartialSurrenderOperation twoBlockSurrenderOf(
      UUID firstFeatureId,
      UUID secondFeatureId
  ) {
    return LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(firstFeatureId, secondFeatureId))
        .withSurrenderDetails(Map.of(
            firstFeatureId, surrenderDetails(BlockSurrenderType.FULL_SURRENDER, FIRST_COMMAND_JOURNEY_ID),
            secondFeatureId, surrenderDetails(BlockSurrenderType.FULL_SURRENDER, SECOND_COMMAND_JOURNEY_ID)
        ))
        .build();
  }

  private void givenStagedChange(LicencePositionChangeType change, PartialSurrenderOperation resolvedSurrender) {
    givenStagedChange(change, Optional.of(resolvedSurrender));
  }

  private void givenStagedChange(
      LicencePositionChangeType change,
      Optional<PartialSurrenderOperation> resolvedSurrender
  ) {
    when(licencePositionStagedPayloadService.findStagedChangeOperation(change, PartialSurrenderOperation.class))
        .thenReturn(resolvedSurrender);
  }

  private void givenLiveSurrenderChange(PartialSurrenderOperation liveSurrender) {
    when(licencePositionChangeService.getByIdOrThrow(UUID.fromString(LIVE_CHANGE_ID)))
        .thenReturn(LicencePositionChangeTestUtil.newBuilder()
            .withId(UUID.fromString(LIVE_CHANGE_ID))
            .withOperations(List.of(liveSurrender))
            .build());
  }

  private static LicencePositionStagedPayload updatePositionStagedPayloadFor(
      LicencePosition licencePosition,
      List<LicencePositionChangeType> changes
  ) {
    return LicencePositionStagedPayloadTestUtil.newBuilder()
        .withLicenceCorrection(LICENCE_CORRECTION)
        .withChangeType(LicencePositionStagedPayloadChangeType.UPDATE_POSITION)
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
}
