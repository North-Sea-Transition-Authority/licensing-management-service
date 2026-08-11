package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.tasklist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListItem;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListLabel;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListSection;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListSectionService;

@ExtendWith(MockitoExtension.class)
class PartialSurrenderTaskListServiceTest {

  private static final ServiceUserDetail USER = ServiceUserDetailTestUtil.newBuilder().build();
  private static final PartialSurrenderTaskListContext CONTEXT =
      new PartialSurrenderTaskListContext(LicencePositionCorrectionTestUtil.newBuilder().build());

  private static final TaskListSection SURRENDER_DETAILS_SECTION = new TaskListSection("Surrender details", 10,
      List.of(new TaskListItem("Surrender details", TaskListLabel.COMPLETE, "/surrender-details")));
  private static final TaskListSection REVIEW_AND_SUBMIT_SECTION = new TaskListSection("Review and submit", 20,
      List.of(new TaskListItem("Review and submit", "/review-and-submit")));

  @Mock
  private PartialSurrenderDetailsTaskListSectionService partialSurrenderDetailsTaskListSectionService;

  @Mock
  private PartialSurrenderReviewAndSubmitTaskListSectionService partialSurrenderReviewAndSubmitTaskListSectionService;

  private PartialSurrenderTaskListService partialSurrenderTaskListService;

  @BeforeEach
  void setUp() {
    List<TaskListSectionService<PartialSurrenderTaskListContext>> taskListSectionServices = List.of(
        partialSurrenderReviewAndSubmitTaskListSectionService,
        partialSurrenderDetailsTaskListSectionService
    );
    partialSurrenderTaskListService = new PartialSurrenderTaskListService(taskListSectionServices);
  }

  @Test
  void getTaskListSections_whenSectionServicesOutOfOrder_thenSortedByDisplayOrder() {
    when(partialSurrenderReviewAndSubmitTaskListSectionService.getSection(CONTEXT, USER))
        .thenReturn(Optional.of(REVIEW_AND_SUBMIT_SECTION));
    when(partialSurrenderDetailsTaskListSectionService.getSection(CONTEXT, USER))
        .thenReturn(Optional.of(SURRENDER_DETAILS_SECTION));

    var sections = partialSurrenderTaskListService.getTaskListSections(CONTEXT, USER);

    assertThat(sections).containsExactly(SURRENDER_DETAILS_SECTION, REVIEW_AND_SUBMIT_SECTION);
  }

  @Test
  void getTaskListSections_whenSectionServiceHasNoSection_thenSectionOmitted() {
    when(partialSurrenderReviewAndSubmitTaskListSectionService.getSection(CONTEXT, USER))
        .thenReturn(Optional.empty());
    when(partialSurrenderDetailsTaskListSectionService.getSection(CONTEXT, USER))
        .thenReturn(Optional.of(SURRENDER_DETAILS_SECTION));

    var sections = partialSurrenderTaskListService.getTaskListSections(CONTEXT, USER);

    assertThat(sections).containsExactly(SURRENDER_DETAILS_SECTION);
  }
}
