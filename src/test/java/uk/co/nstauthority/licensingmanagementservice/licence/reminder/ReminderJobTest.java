package uk.co.nstauthority.licensingmanagementservice.licence.reminder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.annotation.Scheduled;

@ExtendWith(MockitoExtension.class)
class ReminderJobTest {

  @Mock
  private ReminderService reminderService;

  @InjectMocks
  private ReminderJob reminderJob;

  @Test
  void sendDueReminders_delegatesWithTheSixMonthNoticePeriod() {
    when(reminderService.sendDueReminders(NoticePeriod.SIX_MONTHS))
        .thenReturn(new ReminderRunSummary(1, 0, 1, 0));

    reminderJob.sendDueReminders();

    verify(reminderService).sendDueReminders(NoticePeriod.SIX_MONTHS);
  }

  @Test
  void sendDueReminders_isScheduledDailyAtSevenUkTime() throws NoSuchMethodException {
    var scheduled = jobMethod().getAnnotation(Scheduled.class);

    assertThat(scheduled).isNotNull();
    assertThat(scheduled.cron()).isEqualTo("0 0 7 * * *");
    assertThat(scheduled.zone()).isEqualTo("Europe/London");
  }

  @Test
  void sendDueReminders_isLockedSoOnlyOneInstanceSends() throws NoSuchMethodException {
    var schedulerLock = jobMethod().getAnnotation(SchedulerLock.class);

    assertThat(schedulerLock).isNotNull();
    assertThat(schedulerLock.name()).isEqualTo("licenceReminders");
  }

  private Method jobMethod() throws NoSuchMethodException {
    return ReminderJob.class.getMethod("sendDueReminders");
  }
}
