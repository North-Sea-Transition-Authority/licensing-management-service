package uk.co.nstauthority.licensingmanagementservice.workarea.workareaitemview;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class WorkAreaDataItemTypeTest {

  @Test
  void getDisplayName_assertCorrectDisplayNames() {
    assertThat(WorkAreaDataItemType.LICENCE_CONTINUATION_APPLICATION.getDisplayName())
        .isEqualTo("Licence continuation application");
    assertThat(WorkAreaDataItemType.SCHEDULE_WORK_PROGRAMME_APPLICATION.getDisplayName())
        .isEqualTo("Schedule and work programme application");
  }
}
