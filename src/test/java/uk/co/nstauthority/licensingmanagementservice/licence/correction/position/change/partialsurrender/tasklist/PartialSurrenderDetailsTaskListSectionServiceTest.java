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
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.LicencePositionPartialSurrenderController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.PartialSurrenderCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.PartialSurrenderDetailsFormValidator;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.feature.FeatureTestUtil;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListItem;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListLabel;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListSection;

@ExtendWith(MockitoExtension.class)
class PartialSurrenderDetailsTaskListSectionServiceTest {

  private static final UUID CORRECTION_ID = UUID.randomUUID();
  private static final UUID POSITION_CORRECTION_ID = UUID.randomUUID();
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

  private PartialSurrenderDetailsTaskListSectionService partialSurrenderDetailsTaskListSectionService;

  @BeforeEach
  void setUp() {
    partialSurrenderDetailsTaskListSectionService = new PartialSurrenderDetailsTaskListSectionService(
        partialSurrenderCorrectionService, new PartialSurrenderDetailsFormValidator());
  }

  @Test
  void getSection_whenAddedPosition_thenLinksToTheAddedPositionSurrenderDetails() {
    var positionCorrection = positionCorrection(LicencePositionCorrectionChangeType.ADD_POSITION);
    var context = new PartialSurrenderTaskListContext(positionCorrection);
    givenSurrenderDetails(positionCorrection, List.of(BLOCK.getId()), List.of(BLOCK));

    var section = partialSurrenderDetailsTaskListSectionService.getSection(context, USER);

    assertThat(section).contains(expectedSection(
        TaskListLabel.COMPLETE,
        ReverseRouter.route(on(LicencePositionPartialSurrenderController.class)
            .renderForAddedPosition(CORRECTION_ID, POSITION_CORRECTION_ID, null))));
  }

  @Test
  void getSection_whenExecutedPosition_thenLinksToTheExecutedPositionSurrenderDetails() {
    var positionCorrection = positionCorrection(LicencePositionCorrectionChangeType.UPDATE_POSITION);
    var context = new PartialSurrenderTaskListContext(positionCorrection);
    givenSurrenderDetails(positionCorrection, List.of(BLOCK.getId()), List.of(BLOCK));

    var section = partialSurrenderDetailsTaskListSectionService.getSection(context, USER);

    assertThat(section).contains(expectedSection(
        TaskListLabel.COMPLETE,
        ReverseRouter.route(on(LicencePositionPartialSurrenderController.class)
            .renderForExecutedPosition(CORRECTION_ID, POSITION_ID, null))));
  }

  @Test
  void getSection_whenNoSurrenderStaged_thenNotComplete() {
    var positionCorrection = positionCorrection(LicencePositionCorrectionChangeType.UPDATE_POSITION);
    var context = new PartialSurrenderTaskListContext(positionCorrection);
    givenSurrenderDetails(positionCorrection, List.of(), List.of(BLOCK));

    var section = partialSurrenderDetailsTaskListSectionService.getSection(context, USER);

    assertThat(section).contains(expectedSection(
        TaskListLabel.NOT_COMPLETE,
        ReverseRouter.route(on(LicencePositionPartialSurrenderController.class)
            .renderForExecutedPosition(CORRECTION_ID, POSITION_ID, null))));
  }

  @Test
  void getSection_whenStagedBlockIsNoLongerSurrenderable_thenNotComplete() {
    var positionCorrection = positionCorrection(LicencePositionCorrectionChangeType.UPDATE_POSITION);
    var context = new PartialSurrenderTaskListContext(positionCorrection);
    givenSurrenderDetails(positionCorrection, List.of(BLOCK.getId()), List.of(OTHER_BLOCK));

    var section = partialSurrenderDetailsTaskListSectionService.getSection(context, USER);

    assertThat(section).contains(expectedSection(
        TaskListLabel.NOT_COMPLETE,
        ReverseRouter.route(on(LicencePositionPartialSurrenderController.class)
            .renderForExecutedPosition(CORRECTION_ID, POSITION_ID, null))));
  }

  @Test
  void getSection_whenPositionIsBeingRemoved_thenThrows() {
    var context = new PartialSurrenderTaskListContext(
        positionCorrection(LicencePositionCorrectionChangeType.REMOVE_POSITION));

    assertThatThrownBy(() -> partialSurrenderDetailsTaskListSectionService.getSection(context, USER))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining(POSITION_CORRECTION_ID.toString());
  }

  private void givenSurrenderDetails(
      LicencePositionCorrection positionCorrection,
      List<UUID> stagedFeatureIds,
      List<Feature> surrenderableBlockFeatures
  ) {
    when(partialSurrenderCorrectionService.getCommittedPartialSurrender(positionCorrection)).thenReturn(
        stagedFeatureIds.isEmpty()
            ? Optional.empty()
            : Optional.of(LicenceOperation.newPartialSurrenderOperation()
                .withFeatureIds(stagedFeatureIds)
                .build()));
    when(partialSurrenderCorrectionService.getSurrenderableBlockFeatures(positionCorrection))
        .thenReturn(surrenderableBlockFeatures);
  }

  private LicencePositionCorrection positionCorrection(LicencePositionCorrectionChangeType changeType) {
    return LicencePositionCorrectionTestUtil.newBuilder()
        .withId(POSITION_CORRECTION_ID)
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
