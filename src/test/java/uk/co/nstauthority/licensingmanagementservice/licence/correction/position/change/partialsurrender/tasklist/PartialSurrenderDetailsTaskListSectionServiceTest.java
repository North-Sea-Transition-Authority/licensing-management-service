package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.tasklist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.CorrectionPosition;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.LicencePositionPartialSurrenderController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.PartialSurrenderCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.PartialSurrenderDetailsFormValidator;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.feature.FeatureTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.spatial.LicencePositionSpatialService;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.LicencePositionStagedPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.LicencePositionStagedPayloadChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.LicencePositionStagedPayloadTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.changetypes.AddChange;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListItem;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListLabel;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListSection;

@ExtendWith(MockitoExtension.class)
class PartialSurrenderDetailsTaskListSectionServiceTest {

  private static final UUID CORRECTION_ID = UUID.randomUUID();
  private static final UUID POSITION_STAGED_PAYLOAD_ID = UUID.randomUUID();
  private static final UUID POSITION_ID = UUID.randomUUID();

  private static final LicenceCorrection CORRECTION = LicenceCorrectionTestUtil.newBuilder()
      .withId(CORRECTION_ID)
      .build();
  private static final LicencePosition POSITION = LicencePositionTestUtil.newBuilder()
      .withId(POSITION_ID)
      .build();
  private static final ServiceUserDetail USER = ServiceUserDetailTestUtil.newBuilder().build();
  private static final Feature BLOCK = FeatureTestUtil.builder().build();
  private static final Feature OTHER_BLOCK = FeatureTestUtil.builder().build();

  @Mock
  private PartialSurrenderCorrectionService partialSurrenderCorrectionService;

  @Mock
  private LicencePositionSpatialService licencePositionSpatialService;

  private PartialSurrenderDetailsTaskListSectionService partialSurrenderDetailsTaskListSectionService;

  @BeforeEach
  void setUp() {
    partialSurrenderDetailsTaskListSectionService = new PartialSurrenderDetailsTaskListSectionService(
        partialSurrenderCorrectionService, new PartialSurrenderDetailsFormValidator(), licencePositionSpatialService);
  }

  @Test
  void getSection_whenAddedPosition_thenLinksToTheAddedPositionSurrenderDetails() {
    var positionStagedPayload = positionStagedPayload(LicencePositionStagedPayloadChangeType.ADD_POSITION);
    var context = new PartialSurrenderTaskListContext.Staged(positionStagedPayload);
    givenSurrenderDetails(positionStagedPayload, List.of(BLOCK.getId()), List.of(BLOCK));

    var section = partialSurrenderDetailsTaskListSectionService.getSection(context, USER);

    assertThat(section).contains(expectedSection(
        TaskListLabel.COMPLETE,
        ReverseRouter.route(on(LicencePositionPartialSurrenderController.class)
            .renderForPosition(new CorrectionPosition.Staged(positionStagedPayload)))));
  }

  @Test
  void getSection_whenExecutedPosition_thenLinksToTheExecutedPositionSurrenderDetails() {
    var positionStagedPayload = positionStagedPayload(LicencePositionStagedPayloadChangeType.UPDATE_POSITION);
    var context = new PartialSurrenderTaskListContext.Staged(positionStagedPayload);
    givenSurrenderDetails(positionStagedPayload, List.of(BLOCK.getId()), List.of(BLOCK));

    var section = partialSurrenderDetailsTaskListSectionService.getSection(context, USER);

    assertThat(section).contains(expectedSection(
        TaskListLabel.COMPLETE,
        ReverseRouter.route(on(LicencePositionPartialSurrenderController.class)
            .renderForPosition(new CorrectionPosition.Executed(CORRECTION, POSITION)))));
  }

  @Test
  void getSection_whenNoSurrenderStaged_thenNotComplete() {
    var positionStagedPayload = positionStagedPayload(LicencePositionStagedPayloadChangeType.UPDATE_POSITION);
    var context = new PartialSurrenderTaskListContext.Staged(positionStagedPayload);
    givenSurrenderDetails(positionStagedPayload, List.of(), List.of(BLOCK));

    var section = partialSurrenderDetailsTaskListSectionService.getSection(context, USER);

    assertThat(section).contains(expectedSection(
        TaskListLabel.NOT_COMPLETE,
        ReverseRouter.route(on(LicencePositionPartialSurrenderController.class)
            .renderForPosition(new CorrectionPosition.Executed(CORRECTION, POSITION)))));
  }

  @Test
  void getSection_whenStagedBlockIsNoLongerSurrenderable_thenNotComplete() {
    var positionStagedPayload = positionStagedPayload(LicencePositionStagedPayloadChangeType.UPDATE_POSITION);
    var context = new PartialSurrenderTaskListContext.Staged(positionStagedPayload);
    givenSurrenderDetails(positionStagedPayload, List.of(BLOCK.getId()), List.of(OTHER_BLOCK));

    var section = partialSurrenderDetailsTaskListSectionService.getSection(context, USER);

    assertThat(section).contains(expectedSection(
        TaskListLabel.NOT_COMPLETE,
        ReverseRouter.route(on(LicencePositionPartialSurrenderController.class)
            .renderForPosition(new CorrectionPosition.Executed(CORRECTION, POSITION)))));
  }

  @Test
  void getSection_whenCorrectingALiveChangeWithNothingStaged_thenLinksToTheCorrectChangePage() {
    var liveChange = LicencePositionChangeTestUtil.newBuilder().build();
    var context = new PartialSurrenderTaskListContext.LiveChange(CORRECTION, POSITION, liveChange);

    var section = partialSurrenderDetailsTaskListSectionService.getSection(context, USER);

    assertThat(section).contains(expectedSection(
        TaskListLabel.COMPLETE,
        ReverseRouter.route(on(LicencePositionPartialSurrenderController.class)
            .renderForCorrectingChange(CORRECTION, POSITION, liveChange))));
  }

  @Test
  void getSection_whenPositionIsBeingRemoved_thenThrows() {
    var context = new PartialSurrenderTaskListContext.Staged(
        positionStagedPayload(LicencePositionStagedPayloadChangeType.REMOVE_POSITION));

    assertThatThrownBy(() -> partialSurrenderDetailsTaskListSectionService.getSection(context, USER))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining(POSITION_STAGED_PAYLOAD_ID.toString());
  }

  private void givenSurrenderDetails(
      LicencePositionStagedPayload positionStagedPayload,
      List<UUID> stagedFeatureIds,
      List<Feature> surrenderableBlockFeatures
  ) {
    if (stagedFeatureIds.isEmpty()) {
      when(partialSurrenderCorrectionService.getCommittedPartialSurrender(positionStagedPayload))
          .thenReturn(Optional.empty());
      when(licencePositionSpatialService.getBlockFeaturesGoingIntoChange(positionStagedPayload, null))
          .thenReturn(surrenderableBlockFeatures);
      return;
    }

    var staged = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(stagedFeatureIds)
        .build();
    when(partialSurrenderCorrectionService.getCommittedPartialSurrender(positionStagedPayload))
        .thenReturn(Optional.of(staged));

    var stagedChange = AddChange.buildOperationsChange(List.of(staged), 1);
    when(partialSurrenderCorrectionService.getCommittedPartialSurrenderChangeId(positionStagedPayload))
        .thenReturn(Optional.of(stagedChange.changeId()));
    when(licencePositionSpatialService.getBlockFeaturesGoingIntoChange(positionStagedPayload, stagedChange.changeId()))
        .thenReturn(surrenderableBlockFeatures);
  }

  private LicencePositionStagedPayload positionStagedPayload(LicencePositionStagedPayloadChangeType changeType) {
    return LicencePositionStagedPayloadTestUtil.newBuilder()
        .withId(POSITION_STAGED_PAYLOAD_ID)
        .withLicenceCorrection(CORRECTION)
        .withChangeType(changeType)
        .withTargetLicencePosition(POSITION)
        .build();
  }

  private TaskListSection expectedSection(TaskListLabel label, String surrenderDetailsUrl) {
    return new TaskListSection(
        PartialSurrenderDetailsTaskListSectionService.SURRENDER_DETAILS,
        PartialSurrenderDetailsTaskListSectionService.SECTION_ORDER,
        List.of(new TaskListItem(
            PartialSurrenderDetailsTaskListSectionService.SURRENDER_DETAILS,
            label,
            surrenderDetailsUrl)));
  }
}
