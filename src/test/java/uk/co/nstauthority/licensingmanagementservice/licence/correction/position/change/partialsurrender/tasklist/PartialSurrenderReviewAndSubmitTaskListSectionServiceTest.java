package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.tasklist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListItem;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListSection;

@ExtendWith(MockitoExtension.class)
class PartialSurrenderReviewAndSubmitTaskListSectionServiceTest {

  private static final UUID CORRECTION_ID = UUID.randomUUID();
  private static final UUID POSITION_CORRECTION_ID = UUID.randomUUID();
  private static final UUID POSITION_ID = UUID.randomUUID();

  private final PartialSurrenderReviewAndSubmitTaskListSectionService
      partialSurrenderReviewAndSubmitTaskListSectionService = new PartialSurrenderReviewAndSubmitTaskListSectionService();

  @Test
  void getSection_whenCorrectingALiveChangeWithNothingStaged_thenLinksToTheReviewPageForThatChange() {
    var liveChangeId = UUID.randomUUID().toString();
    var context = new PartialSurrenderTaskListContext.LiveChange(
        LicenceCorrectionTestUtil.newBuilder().withId(CORRECTION_ID).build(),
        LicencePositionTestUtil.newBuilder().withId(POSITION_ID).build(),
        liveChangeId);

    var section = partialSurrenderReviewAndSubmitTaskListSectionService.getSection(
        context, ServiceUserDetailTestUtil.newBuilder().build());

    assertThat(section).contains(expectedSection(
        ReverseRouter.route(on(PartialSurrenderTaskListController.class)
            .renderReviewAndSubmitForCorrectingChange(CORRECTION_ID, POSITION_ID, liveChangeId, null, null))));
  }

  @Test
  void getSection() {
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withId(POSITION_CORRECTION_ID)
        .withLicenceCorrection(LicenceCorrectionTestUtil.newBuilder().withId(CORRECTION_ID).build())
        .build();
    var context = new PartialSurrenderTaskListContext.Staged(positionCorrection);

    var section = partialSurrenderReviewAndSubmitTaskListSectionService.getSection(
        context, ServiceUserDetailTestUtil.newBuilder().build());

    assertThat(section).contains(expectedSection(
        ReverseRouter.route(on(PartialSurrenderTaskListController.class)
            .renderReviewAndSubmit(CORRECTION_ID, POSITION_CORRECTION_ID, null, null))));
  }

  private TaskListSection expectedSection(String reviewAndSubmitUrl) {
    return new TaskListSection(
        PartialSurrenderReviewAndSubmitTaskListSectionService.REVIEW_AND_SUBMIT,
        PartialSurrenderReviewAndSubmitTaskListSectionService.SECTION_ORDER,
        List.of(new TaskListItem(
            PartialSurrenderReviewAndSubmitTaskListSectionService.REVIEW_AND_SUBMIT,
            reviewAndSubmitUrl)));
  }
}
