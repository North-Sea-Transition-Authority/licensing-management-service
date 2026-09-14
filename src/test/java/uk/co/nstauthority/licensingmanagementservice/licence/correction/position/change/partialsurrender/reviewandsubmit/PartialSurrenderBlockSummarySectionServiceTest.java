package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.reviewandsubmit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.gisframework.command.CommandJourneyService;
import uk.co.fivium.gisframework.feature.CoordinateSystemUtils;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.PartialSurrenderCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.blocksurrendertype.BlockSurrenderType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.AddChange;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.PartialSurrenderOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.PartialSurrenderOperation.SurrenderDetails;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.feature.FeatureTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.spatial.LicencePositionSpatialService;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryCard;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryDataView;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryItem;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryMapView;
import uk.co.nstauthority.licensingmanagementservice.summary.SummarySection;

@ExtendWith(MockitoExtension.class)
class PartialSurrenderBlockSummarySectionServiceTest {

  private static final Feature FIRST_BLOCK = FeatureTestUtil.blockFeature(UUID.randomUUID(), "30", 1);
  private static final Feature SECOND_BLOCK = FeatureTestUtil.blockFeature(UUID.randomUUID(), "30", 2);

  private static final Feature RETAINED_PART = FeatureTestUtil.builder().withFeatureName("SHAPE 2A").build();
  private static final Feature SURRENDERED_PART = FeatureTestUtil.builder().withFeatureName("SHAPE 2B").build();

  private static final UUID COMMAND_JOURNEY_ID = UUID.randomUUID();

  @Mock
  private PartialSurrenderCorrectionService partialSurrenderCorrectionService;

  @Mock
  private LicencePositionSpatialService licencePositionSpatialService;

  @Mock
  private CommandJourneyService commandJourneyService;

  @InjectMocks
  private PartialSurrenderBlockSummarySectionService partialSurrenderBlockSummarySectionService;

  @Test
  void getSummarySection_whenNoCommittedSurrender_thenEmpty() {
    var positionCorrection = positionCorrection();
    when(partialSurrenderCorrectionService.getCommittedPartialSurrender(positionCorrection))
        .thenReturn(Optional.empty());

    var result = partialSurrenderBlockSummarySectionService.getSummarySection(
        new PartialSurrenderSummaryContext.Staged(positionCorrection),
        null
    );

    assertThat(result).isEmpty();
  }

  @Test
  void getSummarySection_whenBlocksSurrendered_thenItemPerBlockOrderedByBlockShowingType() {
    var positionCorrection = positionCorrection();
    var staged = operation(Map.of(
        FIRST_BLOCK.getId(), surrenderDetails(BlockSurrenderType.FULL_SURRENDER, List.of()),
        SECOND_BLOCK.getId(), surrenderDetails(BlockSurrenderType.PARTIAL_SURRENDER, List.of())));

    when(partialSurrenderCorrectionService.getCommittedPartialSurrender(positionCorrection))
        .thenReturn(Optional.of(staged));
    var stagedChangeId = givenStagedSurrenderChangeId(positionCorrection, staged);
    when(licencePositionSpatialService.getBlockFeaturesGoingIntoChange(positionCorrection, stagedChangeId))
        .thenReturn(List.of(SECOND_BLOCK, FIRST_BLOCK));

    var result = partialSurrenderBlockSummarySectionService.getSummarySection(
        new PartialSurrenderSummaryContext.Staged(positionCorrection),
        null
    );

    var expected = new SummarySection(
        PartialSurrenderBlockSummarySectionService.SECTION_ORDER,
        List.of(
            expectedBlockItem(FIRST_BLOCK, expectedFullSurrenderCard(FIRST_BLOCK)),
            expectedBlockItem(SECOND_BLOCK, expectedCardWithMaps(
                BlockSurrenderType.PARTIAL_SURRENDER,
                List.of(expectedMap(
                    PartialSurrenderBlockSummarySectionService.BEFORE,
                    List.of(SECOND_BLOCK.getId())))))
        ));

    assertThat(result).get().usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void getSummarySection_whenBlockPartiallySurrendered_thenBeforeAndAfterMapsShowingRetainedParts() {
    var positionCorrection = positionCorrection();
    var staged = operation(Map.of(
        SECOND_BLOCK.getId(),
        surrenderDetails(BlockSurrenderType.PARTIAL_SURRENDER, List.of(SURRENDERED_PART.getId()))));

    when(partialSurrenderCorrectionService.getCommittedPartialSurrender(positionCorrection))
        .thenReturn(Optional.of(staged));
    var stagedChangeId = givenStagedSurrenderChangeId(positionCorrection, staged);
    when(licencePositionSpatialService.getBlockFeaturesGoingIntoChange(positionCorrection, stagedChangeId))
        .thenReturn(List.of(SECOND_BLOCK));
    when(commandJourneyService.getActiveFeatures(COMMAND_JOURNEY_ID))
        .thenReturn(List.of(SURRENDERED_PART, RETAINED_PART));

    var result = partialSurrenderBlockSummarySectionService.getSummarySection(
        new PartialSurrenderSummaryContext.Staged(positionCorrection),
        null
    );

    var expected = new SummarySection(
        PartialSurrenderBlockSummarySectionService.SECTION_ORDER,
        List.of(expectedBlockItem(SECOND_BLOCK, expectedCardWithMaps(
            BlockSurrenderType.PARTIAL_SURRENDER,
            List.of(
                expectedMap(PartialSurrenderBlockSummarySectionService.BEFORE, List.of(SECOND_BLOCK.getId())),
                expectedMap(PartialSurrenderBlockSummarySectionService.AFTER, List.of(RETAINED_PART.getId())))))));

    assertThat(result).get().usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void getSummarySection_whenEveryPartOfABlockIsSurrendered_thenNoAfterMap() {
    var positionCorrection = positionCorrection();
    var staged = operation(Map.of(
        SECOND_BLOCK.getId(),
        surrenderDetails(
            BlockSurrenderType.PARTIAL_SURRENDER,
            List.of(SURRENDERED_PART.getId(), RETAINED_PART.getId()))));

    when(partialSurrenderCorrectionService.getCommittedPartialSurrender(positionCorrection))
        .thenReturn(Optional.of(staged));
    var stagedChangeId = givenStagedSurrenderChangeId(positionCorrection, staged);
    when(licencePositionSpatialService.getBlockFeaturesGoingIntoChange(positionCorrection, stagedChangeId))
        .thenReturn(List.of(SECOND_BLOCK));
    when(commandJourneyService.getActiveFeatures(COMMAND_JOURNEY_ID))
        .thenReturn(List.of(SURRENDERED_PART, RETAINED_PART));

    var result = partialSurrenderBlockSummarySectionService.getSummarySection(
        new PartialSurrenderSummaryContext.Staged(positionCorrection),
        null
    );

    var expected = new SummarySection(
        PartialSurrenderBlockSummarySectionService.SECTION_ORDER,
        List.of(expectedBlockItem(SECOND_BLOCK, expectedCardWithMaps(
            BlockSurrenderType.PARTIAL_SURRENDER,
            List.of(expectedMap(
                PartialSurrenderBlockSummarySectionService.BEFORE,
                List.of(SECOND_BLOCK.getId())))))));

    assertThat(result).get().usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void getSummarySection_whenBlockFullySurrendered_thenSingleUnlabelledMapOfTheBlock() {
    var positionCorrection = positionCorrection();
    var staged = operation(Map.of(
        SECOND_BLOCK.getId(), surrenderDetails(BlockSurrenderType.FULL_SURRENDER, List.of())));

    when(partialSurrenderCorrectionService.getCommittedPartialSurrender(positionCorrection))
        .thenReturn(Optional.of(staged));
    var stagedChangeId = givenStagedSurrenderChangeId(positionCorrection, staged);
    when(licencePositionSpatialService.getBlockFeaturesGoingIntoChange(positionCorrection, stagedChangeId))
        .thenReturn(List.of(SECOND_BLOCK));

    var result = partialSurrenderBlockSummarySectionService.getSummarySection(
        new PartialSurrenderSummaryContext.Staged(positionCorrection),
        null
    );

    var expected = new SummarySection(
        PartialSurrenderBlockSummarySectionService.SECTION_ORDER,
        List.of(expectedBlockItem(SECOND_BLOCK, expectedCardWithMaps(
            BlockSurrenderType.FULL_SURRENDER,
            List.of(expectedMap(
                PartialSurrenderBlockSummarySectionService.UNLABELLED_MAP,
                List.of(SECOND_BLOCK.getId())))))));

    assertThat(result).get().usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void getSummarySection_whenBlockSurrenderTypeNotYetSelected_thenSurrenderTypeHasNoValue() {
    var positionCorrection = positionCorrection();
    var staged = operation(List.of(FIRST_BLOCK.getId()), Map.of());

    when(partialSurrenderCorrectionService.getCommittedPartialSurrender(positionCorrection))
        .thenReturn(Optional.of(staged));
    var stagedChangeId = givenStagedSurrenderChangeId(positionCorrection, staged);
    when(licencePositionSpatialService.getBlockFeaturesGoingIntoChange(positionCorrection, stagedChangeId))
        .thenReturn(List.of(FIRST_BLOCK));

    var result = partialSurrenderBlockSummarySectionService.getSummarySection(
        new PartialSurrenderSummaryContext.Staged(positionCorrection),
        null
    );

    var expected = new SummarySection(
        PartialSurrenderBlockSummarySectionService.SECTION_ORDER,
        List.of(expectedBlockItem(FIRST_BLOCK, expectedTypeOnlyCard(null)))
    );

    assertThat(result).get().usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void getSummarySection_whenStagedSurrenderBlockOnlyResolvableAnchoredOnTheStagedChange_thenResolves() {
    var positionCorrection = positionCorrection();
    var staged = operation(
        Map.of(FIRST_BLOCK.getId(), surrenderDetails(BlockSurrenderType.FULL_SURRENDER, List.of())));

    when(partialSurrenderCorrectionService.getCommittedPartialSurrender(positionCorrection))
        .thenReturn(Optional.of(staged));
    var stagedChangeId = givenStagedSurrenderChangeId(positionCorrection, staged);
    when(licencePositionSpatialService.getBlockFeaturesGoingIntoChange(positionCorrection, stagedChangeId))
        .thenReturn(List.of(FIRST_BLOCK));

    var result = partialSurrenderBlockSummarySectionService.getSummarySection(
        new PartialSurrenderSummaryContext.Staged(positionCorrection),
        null
    );

    var expected = new SummarySection(
        PartialSurrenderBlockSummarySectionService.SECTION_ORDER,
        List.of(expectedBlockItem(FIRST_BLOCK, expectedFullSurrenderCard(FIRST_BLOCK))));

    assertThat(result).get().usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void getSummarySection_whenCorrectingALiveChange_thenItemPerSurrenderedBlockShowingTypeAndMaps() {
    var correction = LicenceCorrectionTestUtil.newBuilder().build();
    var licencePosition = LicencePositionTestUtil.newBuilder().build();
    var changeId = UUID.randomUUID().toString();

    when(partialSurrenderCorrectionService.getSurrenderUnderCorrectionOrThrow(correction, licencePosition, changeId))
        .thenReturn(operation(Map.of(
            FIRST_BLOCK.getId(), surrenderDetails(BlockSurrenderType.FULL_SURRENDER, List.of()),
            SECOND_BLOCK.getId(),
            surrenderDetails(BlockSurrenderType.PARTIAL_SURRENDER, List.of(SURRENDERED_PART.getId())))));
    when(licencePositionSpatialService.getBlockFeaturesGoingIntoChange(correction, licencePosition, changeId))
        .thenReturn(List.of(SECOND_BLOCK, FIRST_BLOCK));
    when(commandJourneyService.getActiveFeatures(COMMAND_JOURNEY_ID))
        .thenReturn(List.of(SURRENDERED_PART, RETAINED_PART));

    var result = partialSurrenderBlockSummarySectionService.getSummarySection(
        new PartialSurrenderSummaryContext.LiveChange(correction, licencePosition, changeId),
        null
    );

    var expected = new SummarySection(
        PartialSurrenderBlockSummarySectionService.SECTION_ORDER,
        List.of(
            expectedBlockItem(FIRST_BLOCK, expectedFullSurrenderCard(FIRST_BLOCK)),
            expectedBlockItem(SECOND_BLOCK, expectedCardWithMaps(
                BlockSurrenderType.PARTIAL_SURRENDER,
                List.of(
                    expectedMap(PartialSurrenderBlockSummarySectionService.BEFORE, List.of(SECOND_BLOCK.getId())),
                    expectedMap(PartialSurrenderBlockSummarySectionService.AFTER, List.of(RETAINED_PART.getId())))))
        ));

    assertThat(result).get().usingRecursiveComparison().isEqualTo(expected);
  }

  private String givenStagedSurrenderChangeId(
      LicencePositionCorrection positionCorrection,
      PartialSurrenderOperation staged
  ) {
    var stagedChange = AddChange.buildOperationsChange(List.of(staged), 1);
    when(partialSurrenderCorrectionService.getCommittedPartialSurrenderChangeId(positionCorrection))
        .thenReturn(Optional.of(stagedChange.changeId()));
    return stagedChange.changeId();
  }

  private SurrenderDetails surrenderDetails(BlockSurrenderType type, List<UUID> surrenderedFeatureIds) {
    return new SurrenderDetails(type, COMMAND_JOURNEY_ID, surrenderedFeatureIds);
  }

  private PartialSurrenderOperation operation(Map<UUID, SurrenderDetails> surrenderDetailsByFeatureId) {
    return LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(surrenderDetailsByFeatureId.keySet())
        .withSurrenderDetails(surrenderDetailsByFeatureId)
        .build();
  }

  private PartialSurrenderOperation operation(
      List<UUID> featureIds,
      Map<UUID, BlockSurrenderType> blockSurrenderTypeByFeatureId
  ) {
    var blockSurrenders = blockSurrenderTypeByFeatureId.entrySet().stream()
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            entry -> surrenderDetails(entry.getValue(), List.of())));

    return LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(featureIds)
        .withSurrenderDetails(blockSurrenders)
        .build();
  }

  private LicencePositionCorrection positionCorrection() {
    return LicencePositionCorrectionTestUtil.newBuilder().build();
  }

  private SummaryItem expectedBlockItem(Feature block, SummaryCard summaryCard) {
    return SummaryItem.withCard("Block %s".formatted(block.getFeatureName()), summaryCard);
  }

  private SummaryCard expectedTypeOnlyCard(BlockSurrenderType type) {
    return expectedCardWithMaps(type, List.of());
  }

  private SummaryCard expectedFullSurrenderCard(Feature block) {
    return expectedCardWithMaps(
        BlockSurrenderType.FULL_SURRENDER,
        List.of(expectedMap(
            PartialSurrenderBlockSummarySectionService.UNLABELLED_MAP,
            List.of(block.getId()))));
  }

  private SummaryCard expectedCardWithMaps(BlockSurrenderType type, List<ExpectedMap> maps) {
    var details = SummaryDataView.newBuilder()
        .addStringValue(
            PartialSurrenderBlockSummarySectionService.TYPE_OF_CHANGE,
            type != null ? type.getDisplayName() : null);

    maps.forEach(map -> details.addMapValue(
        map.label(),
        new SummaryMapView(map.featureIds(), CoordinateSystemUtils.getWkid(SECOND_BLOCK.getCoordinateSystem()))));

    return SummaryCard.simpleSummaryCard(details.build());
  }

  private ExpectedMap expectedMap(String label, List<UUID> featureIds) {
    return new ExpectedMap(label, featureIds);
  }

  private record ExpectedMap(String label, List<UUID> featureIds) {
  }
}
