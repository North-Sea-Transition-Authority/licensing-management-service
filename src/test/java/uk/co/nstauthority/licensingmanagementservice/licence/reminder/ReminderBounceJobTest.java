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
class ReminderBounceJobTest {

  @Mock
  private ReminderBounceService reminderBounceService;

  @InjectMocks
  private ReminderBounceJob reminderBounceJob;

  @Test
  void reportBounces_delegatesToTheBounceService() {
    when(reminderBounceService.reportBounces()).thenReturn(2);

    reminderBounceJob.reportBounces();

    verify(reminderBounceService).reportBounces();
  }

  @Test
  void reportBounces_isScheduledDailyAtEightUkTimeAfterTheReminderRun() throws NoSuchMethodException {
    var scheduled = jobMethod().getAnnotation(Scheduled.class);

    assertThat(scheduled).isNotNull();
    assertThat(scheduled.cron()).isEqualTo("0 0 8 * * *");
    assertThat(scheduled.zone()).isEqualTo("Europe/London");
  }

  @Test
  void reportBounces_isLockedSoOnlyOneInstanceReports() throws NoSuchMethodException {
    var schedulerLock = jobMethod().getAnnotation(SchedulerLock.class);

    assertThat(schedulerLock).isNotNull();
    assertThat(schedulerLock.name()).isEqualTo("licenceReminderBounces");
  }

  private Method jobMethod() throws NoSuchMethodException {
    return ReminderBounceJob.class.getMethod("reportBounces");
  }
}
