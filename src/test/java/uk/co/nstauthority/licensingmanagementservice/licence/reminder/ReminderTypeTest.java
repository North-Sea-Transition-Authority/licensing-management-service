package uk.co.nstauthority.licensingmanagementservice.licence.reminder;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ReminderTypeTest {

  @ParameterizedTest
  @EnumSource(ReminderType.class)
  void getNoticePeriod_everyReminderTypeHasOne(ReminderType reminderType) {
    assertThat(reminderType.getNoticePeriod()).isEqualTo(NoticePeriod.SIX_MONTHS);
  }
}
