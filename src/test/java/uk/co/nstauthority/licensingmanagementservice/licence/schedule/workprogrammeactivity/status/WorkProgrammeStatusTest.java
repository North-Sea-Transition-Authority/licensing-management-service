package uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.status;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityCategory;

class WorkProgrammeStatusTest {

  @Test
  void getRadioOptions_offersDroppedForDrillOrDropWellsAndOtherActivities() {
    assertThat(WorkProgrammeStatus.getRadioOptions(WorkProgrammeActivityCategory.DRILL_OR_DROP_WELL))
        .containsKey("DROPPED")
        .containsValue("Dropped (not drilled)");
    assertThat(WorkProgrammeStatus.getRadioOptions(WorkProgrammeActivityCategory.OTHER_ACTIVITY))
        .containsKey("DROPPED");
  }

  @Test
  void getRadioOptions_doesNotOfferDroppedForOtherCategories() {
    assertThat(WorkProgrammeStatus.getRadioOptions(WorkProgrammeActivityCategory.DRILL_WELL))
        .containsOnlyKeys("OPEN", "IN_PROGRESS", "COMPLETE", "FULL_WAIVER", "TRANSFERRED");
  }

  @Test
  void getRadioOptions_areInDisplayOrder() {
    assertThat(WorkProgrammeStatus.getRadioOptions(WorkProgrammeActivityCategory.DRILL_OR_DROP_WELL).keySet())
        .containsExactly("OPEN", "IN_PROGRESS", "COMPLETE", "FULL_WAIVER", "TRANSFERRED", "DROPPED");
  }
}
