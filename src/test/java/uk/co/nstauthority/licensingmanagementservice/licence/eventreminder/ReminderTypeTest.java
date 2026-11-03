package uk.co.nstauthority.licensingmanagementservice.licence.eventreminder;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

class ReminderTypeTest {

  @ParameterizedTest
  @CsvSource({
      "TERM_OR_PHASE_END, SIX_MONTHS",
      "LICENCE_EXPIRY, TWELVE_MONTHS",
      "WORK_PROGRAMME_ACTIVITY, SIX_MONTHS",
      "OTHER_SCHEDULE_EVENT, SIX_MONTHS"
  })
  void getNoticePeriod(ReminderType reminderType, NoticePeriod expectedNoticePeriod) {
    assertThat(reminderType.getNoticePeriod()).isEqualTo(expectedNoticePeriod);
  }

  @ParameterizedTest
  @EnumSource(ReminderType.class)
  void getNoticePeriod_noReminderTypeHasALongerNoticePeriodThanLicenceExpiry(ReminderType reminderType) {
    assertThat(reminderType.getNoticePeriod().getMonths())
        .isLessThanOrEqualTo(ReminderType.LICENCE_EXPIRY.getNoticePeriod().getMonths());
  }

  @ParameterizedTest
  @EnumSource(ReminderType.class)
  void getDisplayName_everyReminderTypeHasOne(ReminderType reminderType) {
    assertThat(reminderType.getDisplayName()).isNotBlank();
  }
}
