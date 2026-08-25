package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.tasklist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.PartialSurrenderCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.blocksurrendertype.BlockSurrenderType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.blocksurrendertype.BlockSurrenderTypeController;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.PartialSurrenderOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.feature.FeatureTestUtil;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListItem;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListLabel;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListSection;

@ExtendWith(MockitoExtension.class)
class PartialSurrenderBlockSurrenderTypeTaskListSectionServiceTest {

  private static final UUID CORRECTION_ID = UUID.randomUUID();
  private static final UUID POSITION_CORRECTION_ID = UUID.randomUUID();
  private static final LicenceCorrection CORRECTION = LicenceCorrectionTestUtil.newBuilder()
      .withId(CORRECTION_ID)
      .build();
  private static final UUID POSITION_ID = UUID.randomUUID();
  private static final LicencePosition POSITION = LicencePositionTestUtil.newBuilder()
      .withId(POSITION_ID)
      .build();
  private static final String LIVE_CHANGE_ID = UUID.randomUUID().toString();
  private static final ServiceUserDetail USER = ServiceUserDetailTestUtil.newBuilder().build();
  private static final Feature FIRST_BLOCK = FeatureTestUtil.blockFeature(UUID.randomUUID(), "30", 1);
  private static final Feature SECOND_BLOCK = FeatureTestUtil.blockFeature(UUID.randomUUID(), "30", 2);

  @Mock
  private PartialSurrenderCorrectionService partialSurrenderCorrectionService;

  @InjectMocks
  private PartialSurrenderBlockSurrenderTypeTaskListSectionService
      partialSurrenderBlockSurrenderTypeTaskListSectionService;

  @Test
  void getSection_whenNoSurrenderStaged_thenEmpty() {
    var positionCorrection = positionCorrection();
    var context = new PartialSurrenderTaskListContext.Staged(positionCorrection);
    when(partialSurrenderCorrectionService.getCommittedPartialSurrender(positionCorrection))
        .thenReturn(Optional.empty());

    var section = partialSurrenderBlockSurrenderTypeTaskListSectionService.getSection(context, USER);

    assertThat(section).isEmpty();
  }

  @Test
  void getSection_whenNoStagedBlockIsSurrenderable_thenEmpty() {
    var positionCorrection = positionCorrection();
    var context = new PartialSurrenderTaskListContext.Staged(positionCorrection);
    givenStagedSurrender(positionCorrection, operation(List.of(FIRST_BLOCK.getId()), Map.of()));
    when(partialSurrenderCorrectionService.getSurrenderableBlockFeatures(positionCorrection))
        .thenReturn(List.of(SECOND_BLOCK));

    var section = partialSurrenderBlockSurrenderTypeTaskListSectionService.getSection(context, USER);

    assertThat(section).isEmpty();
  }

  @Test
  void getSection_whenStagedBlocksSurrenderable_thenSectionItemsOrderedByBlock() {
    var positionCorrection = positionCorrection();
    var context = new PartialSurrenderTaskListContext.Staged(positionCorrection);
    givenStagedSurrender(positionCorrection,
        operation(List.of(FIRST_BLOCK.getId(), SECOND_BLOCK.getId()),
            Map.of(FIRST_BLOCK.getId(), BlockSurrenderType.FULL_SURRENDER)));
    when(partialSurrenderCorrectionService.getSurrenderableBlockFeatures(positionCorrection))
        .thenReturn(List.of(SECOND_BLOCK, FIRST_BLOCK));

    var section = partialSurrenderBlockSurrenderTypeTaskListSectionService.getSection(context, USER);

    assertThat(section).contains(new TaskListSection(
        PartialSurrenderBlockSurrenderTypeTaskListSectionService.SURRENDERED_BLOCKS,
        PartialSurrenderBlockSurrenderTypeTaskListSectionService.SECTION_ORDER,
        List.of(
            expectedItem(positionCorrection, FIRST_BLOCK, TaskListLabel.COMPLETE),
            expectedItem(positionCorrection, SECOND_BLOCK, TaskListLabel.NOT_COMPLETE))));
  }

  private static Stream<Arguments> blockSurrenderTypeToLabel() {
    return Stream.of(
        Arguments.of(Map.of(FIRST_BLOCK.getId(), BlockSurrenderType.FULL_SURRENDER), TaskListLabel.COMPLETE),
        Arguments.of(Map.of(FIRST_BLOCK.getId(), BlockSurrenderType.PARTIAL_SURRENDER), TaskListLabel.NOT_COMPLETE),
        Arguments.of(Map.<UUID, BlockSurrenderType>of(), TaskListLabel.NOT_COMPLETE));
  }

  @ParameterizedTest
  @MethodSource("blockSurrenderTypeToLabel")
  void getSection_whenBlockSurrenderType_thenLabelledCompleteOnlyForFullSurrender(
      Map<UUID, BlockSurrenderType> blockSurrenderTypeByFeatureId,
      TaskListLabel expectedLabel
  ) {
    var positionCorrection = positionCorrection();
    var context = new PartialSurrenderTaskListContext.Staged(positionCorrection);
    givenStagedSurrender(positionCorrection,
        operation(List.of(FIRST_BLOCK.getId()), blockSurrenderTypeByFeatureId));
    when(partialSurrenderCorrectionService.getSurrenderableBlockFeatures(positionCorrection))
        .thenReturn(List.of(FIRST_BLOCK));

    var section = partialSurrenderBlockSurrenderTypeTaskListSectionService.getSection(context, USER);

    assertThat(section).contains(new TaskListSection(
        PartialSurrenderBlockSurrenderTypeTaskListSectionService.SURRENDERED_BLOCKS,
        PartialSurrenderBlockSurrenderTypeTaskListSectionService.SECTION_ORDER,
        List.of(expectedItem(positionCorrection, FIRST_BLOCK, expectedLabel))));
  }

  @Test
  void getSection_whenCorrectingALiveChangeWithNothingStaged_thenSectionItemsFromTheLiveSurrender() {
    var context = liveChangeContext();
    givenSurrenderUnderCorrection(operation(
        List.of(FIRST_BLOCK.getId()), Map.of(FIRST_BLOCK.getId(), BlockSurrenderType.FULL_SURRENDER)));
    when(partialSurrenderCorrectionService.getSurrenderableBlockFeatures(POSITION))
        .thenReturn(List.of(FIRST_BLOCK, SECOND_BLOCK));

    var section = partialSurrenderBlockSurrenderTypeTaskListSectionService.getSection(context, USER);

    assertThat(section).contains(new TaskListSection(
        PartialSurrenderBlockSurrenderTypeTaskListSectionService.SURRENDERED_BLOCKS,
        PartialSurrenderBlockSurrenderTypeTaskListSectionService.SECTION_ORDER,
        List.of(expectedCorrectingChangeItem(FIRST_BLOCK, TaskListLabel.COMPLETE))));
  }

  @Test
  void getSection_whenCorrectingALiveChangeWithBlocksStaged_thenSectionItemsOrderedByBlock() {
    var context = liveChangeContext();
    givenSurrenderUnderCorrection(operation(
        List.of(SECOND_BLOCK.getId(), FIRST_BLOCK.getId()),
        Map.of(FIRST_BLOCK.getId(), BlockSurrenderType.PARTIAL_SURRENDER)));
    when(partialSurrenderCorrectionService.getSurrenderableBlockFeatures(POSITION))
        .thenReturn(List.of(SECOND_BLOCK, FIRST_BLOCK));

    var section = partialSurrenderBlockSurrenderTypeTaskListSectionService.getSection(context, USER);

    assertThat(section).contains(new TaskListSection(
        PartialSurrenderBlockSurrenderTypeTaskListSectionService.SURRENDERED_BLOCKS,
        PartialSurrenderBlockSurrenderTypeTaskListSectionService.SECTION_ORDER,
        List.of(
            expectedCorrectingChangeItem(FIRST_BLOCK, TaskListLabel.NOT_COMPLETE),
            expectedCorrectingChangeItem(SECOND_BLOCK, TaskListLabel.NOT_COMPLETE))));
  }

  @Test
  void getSection_whenCorrectingALiveChangeAndTheBlockIsNoLongerOnThePosition_thenEmpty() {
    var context = liveChangeContext();
    givenSurrenderUnderCorrection(operation(List.of(FIRST_BLOCK.getId()), Map.of()));
    when(partialSurrenderCorrectionService.getSurrenderableBlockFeatures(POSITION))
        .thenReturn(List.of(SECOND_BLOCK));

    var section = partialSurrenderBlockSurrenderTypeTaskListSectionService.getSection(context, USER);

    assertThat(section).isEmpty();
  }

  private PartialSurrenderTaskListContext.LiveChange liveChangeContext() {
    return new PartialSurrenderTaskListContext.LiveChange(CORRECTION, POSITION, LIVE_CHANGE_ID);
  }

  private void givenSurrenderUnderCorrection(PartialSurrenderOperation operation) {
    when(partialSurrenderCorrectionService.getSurrenderUnderCorrectionOrThrow(CORRECTION, POSITION, LIVE_CHANGE_ID))
        .thenReturn(operation);
  }

  private TaskListItem expectedCorrectingChangeItem(Feature block, TaskListLabel label) {
    return new TaskListItem(
        "Block %s".formatted(block.getFeatureName()),
        label,
        ReverseRouter.route(
            on(BlockSurrenderTypeController.class).renderSurrenderTypeFormForCorrectingChange(
                CORRECTION_ID, POSITION_ID, LIVE_CHANGE_ID, block.getId(), null)));
  }

  private void givenStagedSurrender(LicencePositionCorrection positionCorrection, PartialSurrenderOperation operation) {
    when(partialSurrenderCorrectionService.getCommittedPartialSurrender(positionCorrection))
        .thenReturn(Optional.of(operation));
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
        .withFeatureIds(featureIds)
        .withSurrenderDetails(blockSurrenders)
        .build();
  }

  private LicencePositionCorrection positionCorrection() {
    return LicencePositionCorrectionTestUtil.newBuilder()
        .withId(POSITION_CORRECTION_ID)
        .withLicenceCorrection(CORRECTION)
        .build();
  }

  private TaskListItem expectedItem(
      LicencePositionCorrection positionCorrection,
      Feature block,
      TaskListLabel label
  ) {
    return new TaskListItem(
        "Block %s".formatted(block.getFeatureName()),
        label,
        ReverseRouter.route(on(BlockSurrenderTypeController.class).renderSurrenderTypeForm(
            CORRECTION_ID, positionCorrection.getId(), block.getId(), null)));
  }
}
