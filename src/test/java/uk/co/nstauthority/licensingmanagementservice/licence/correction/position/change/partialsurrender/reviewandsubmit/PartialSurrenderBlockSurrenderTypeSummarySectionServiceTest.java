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
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.PartialSurrenderCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.blocksurrendertype.BlockSurrenderType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.AddChange;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.PartialSurrenderOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.feature.FeatureTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.spatial.LicencePositionSpatialService;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryCard;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryDataView;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryItem;
import uk.co.nstauthority.licensingmanagementservice.summary.SummarySection;

@ExtendWith(MockitoExtension.class)
class PartialSurrenderBlockSurrenderTypeSummarySectionServiceTest {

  private static final Feature FIRST_BLOCK = FeatureTestUtil.blockFeature(UUID.randomUUID(), "30", 1);
  private static final Feature SECOND_BLOCK = FeatureTestUtil.blockFeature(UUID.randomUUID(), "30", 2);

  @Mock
  private PartialSurrenderCorrectionService partialSurrenderCorrectionService;

  @Mock
  private LicencePositionSpatialService licencePositionSpatialService;

  @InjectMocks
  private PartialSurrenderBlockSurrenderTypeSummarySectionService partialSurrenderBlockSurrenderTypeSummarySectionService;

  @Test
  void getSummarySection_whenNoCommittedSurrender_thenEmpty() {
    var positionCorrection = positionCorrection();
    when(partialSurrenderCorrectionService.getCommittedPartialSurrender(positionCorrection))
        .thenReturn(Optional.empty());

    var result = partialSurrenderBlockSurrenderTypeSummarySectionService.getSummarySection(
        new PartialSurrenderSummaryContext.Staged(positionCorrection),
        null
    );

    assertThat(result).isEmpty();
  }

  @Test
  void getSummarySection_whenBlocksSurrendered_thenItemPerBlockOrderedByBlockShowingType() {
    var positionCorrection = positionCorrection();
    var staged = operation(
        List.of(FIRST_BLOCK.getId(), SECOND_BLOCK.getId()),
        Map.of(
            FIRST_BLOCK.getId(), BlockSurrenderType.FULL_SURRENDER,
            SECOND_BLOCK.getId(), BlockSurrenderType.PARTIAL_SURRENDER));

    when(partialSurrenderCorrectionService.getCommittedPartialSurrender(positionCorrection))
        .thenReturn(Optional.of(staged));
    var stagedChangeId = givenStagedSurrenderChangeId(positionCorrection, staged);
    when(licencePositionSpatialService.getBlockFeaturesGoingIntoChange(positionCorrection, stagedChangeId))
        .thenReturn(List.of(SECOND_BLOCK, FIRST_BLOCK));

    var result = partialSurrenderBlockSurrenderTypeSummarySectionService.getSummarySection(
        new PartialSurrenderSummaryContext.Staged(positionCorrection),
        null
    );

    var expected = new SummarySection(
        PartialSurrenderBlockSurrenderTypeSummarySectionService.SECTION_ORDER,
        List.of(
            expectedBlockItem(FIRST_BLOCK, BlockSurrenderType.FULL_SURRENDER),
            expectedBlockItem(SECOND_BLOCK, BlockSurrenderType.PARTIAL_SURRENDER)
        ));

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

    var result = partialSurrenderBlockSurrenderTypeSummarySectionService.getSummarySection(
        new PartialSurrenderSummaryContext.Staged(positionCorrection),
        null
    );

    var expected = new SummarySection(
        PartialSurrenderBlockSurrenderTypeSummarySectionService.SECTION_ORDER,
        List.of(expectedBlockItem(FIRST_BLOCK, null))
    );

    assertThat(result).get().usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void getSummarySection_whenStagedSurrenderBlockOnlyResolvableAnchoredOnTheStagedChange_thenResolves() {
    var positionCorrection = positionCorrection();
    var staged = operation(List.of(FIRST_BLOCK.getId()), Map.of(FIRST_BLOCK.getId(), BlockSurrenderType.FULL_SURRENDER));

    when(partialSurrenderCorrectionService.getCommittedPartialSurrender(positionCorrection))
        .thenReturn(Optional.of(staged));
    var stagedChangeId = givenStagedSurrenderChangeId(positionCorrection, staged);
    when(licencePositionSpatialService.getBlockFeaturesGoingIntoChange(positionCorrection, stagedChangeId))
        .thenReturn(List.of(FIRST_BLOCK));

    var result = partialSurrenderBlockSurrenderTypeSummarySectionService.getSummarySection(
        new PartialSurrenderSummaryContext.Staged(positionCorrection),
        null
    );

    var expected = new SummarySection(
        PartialSurrenderBlockSurrenderTypeSummarySectionService.SECTION_ORDER,
        List.of(expectedBlockItem(FIRST_BLOCK, BlockSurrenderType.FULL_SURRENDER)));

    assertThat(result).get().usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void getSummarySection_whenCorrectingALiveChange_thenItemPerSurrenderedBlockShowingType() {
    var correction = LicenceCorrectionTestUtil.newBuilder().build();
    var licencePosition = LicencePositionTestUtil.newBuilder().build();
    var changeId = UUID.randomUUID().toString();

    when(partialSurrenderCorrectionService.getSurrenderUnderCorrectionOrThrow(correction, licencePosition, changeId))
        .thenReturn(operation(
            List.of(FIRST_BLOCK.getId(), SECOND_BLOCK.getId()),
            Map.of(
                FIRST_BLOCK.getId(), BlockSurrenderType.FULL_SURRENDER,
                SECOND_BLOCK.getId(), BlockSurrenderType.PARTIAL_SURRENDER)));
    when(licencePositionSpatialService.getBlockFeaturesGoingIntoChange(correction, licencePosition, changeId))
        .thenReturn(List.of(SECOND_BLOCK, FIRST_BLOCK));

    var result = partialSurrenderBlockSurrenderTypeSummarySectionService.getSummarySection(
        new PartialSurrenderSummaryContext.LiveChange(correction, licencePosition, changeId),
        null
    );

    var expected = new SummarySection(
        PartialSurrenderBlockSurrenderTypeSummarySectionService.SECTION_ORDER,
        List.of(
            expectedBlockItem(FIRST_BLOCK, BlockSurrenderType.FULL_SURRENDER),
            expectedBlockItem(SECOND_BLOCK, BlockSurrenderType.PARTIAL_SURRENDER)
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

  private PartialSurrenderOperation operation(
      List<UUID> featureIds,
      Map<UUID, BlockSurrenderType> blockSurrenderTypeByFeatureId
  ) {
    var blockSurrenders = blockSurrenderTypeByFeatureId.entrySet().stream()
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            entry -> new PartialSurrenderOperation.SurrenderDetails(entry.getValue(), UUID.randomUUID(), List.of())));

    return LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(featureIds)
        .withSurrenderDetails(blockSurrenders)
        .build();
  }

  private LicencePositionCorrection positionCorrection() {
    return LicencePositionCorrectionTestUtil.newBuilder().build();
  }

  private SummaryItem expectedBlockItem(Feature block, BlockSurrenderType type) {
    return SummaryItem.withCard(
        "Block %s".formatted(block.getFeatureName()),
        SummaryCard.simpleSummaryCard(SummaryDataView.newBuilder()
            .addStringValue(
                PartialSurrenderBlockSurrenderTypeSummarySectionService.TYPE_OF_CHANGE,
                type != null ? type.getDisplayName() : null)
            .build()));
  }
}
