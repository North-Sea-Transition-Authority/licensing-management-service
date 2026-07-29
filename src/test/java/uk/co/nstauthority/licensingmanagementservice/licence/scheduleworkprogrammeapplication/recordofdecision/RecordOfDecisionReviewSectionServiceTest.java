package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListItem;

class RecordOfDecisionReviewSectionServiceTest {

  private final RecordOfDecisionReviewSectionService recordOfDecisionReviewSectionService =
      new RecordOfDecisionReviewSectionService();

  @Test
  void getSection_returnsReviewSectionWithReviewItem() {
    var applicationDetail = ScheduleWorkProgrammeApplicationDetailTestUtil.builder()
        .withId(UUID.randomUUID())
        .build();
    var context = new RecordOfDecisionTaskListContext(applicationDetail);

    var section = recordOfDecisionReviewSectionService.getSection(context, null).orElseThrow();

    assertThat(section.displayName()).isEqualTo(RecordOfDecisionReviewSectionService.SECTION_NAME);
    assertThat(section.displayOrder()).isEqualTo(RecordOfDecisionReviewSectionService.SECTION_ORDER);
    assertThat(section.items())
        .extracting(TaskListItem::displayName)
        .containsExactly(RecordOfDecisionReviewSectionService.REVIEW_RECORD_OF_DECISION);
  }
}
