package uk.co.nstauthority.licensingmanagementservice.licence.eventreminder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.annotation.Scheduled;
import uk.co.nstauthority.licensingmanagementservice.phasedrelease.FeatureFlagService;
import uk.co.nstauthority.licensingmanagementservice.phasedrelease.ReleasePhase;

@ExtendWith(MockitoExtension.class)
class EventReminderJobTest {

  @Mock
  private EventReminderService eventReminderService;

  @Mock
  private FeatureFlagService featureFlagService;

  @InjectMocks
  private EventReminderJob eventReminderJob;

  @Test
  void sendDueReminders_whenLms1Enabled_thenDelegatesToTheReminderService() {
    when(featureFlagService.isEnabled(ReleasePhase.LMS1)).thenReturn(true);
    when(eventReminderService.sendDueReminders()).thenReturn(new EventReminderRunSummary(1, 0, 1, 0));

    eventReminderJob.sendDueReminders();

    verify(eventReminderService).sendDueReminders();
  }

  @Test
  void sendDueReminders_whenLms1NotEnabled_thenNoRemindersSent() {
    when(featureFlagService.isEnabled(ReleasePhase.LMS1)).thenReturn(false);

    eventReminderJob.sendDueReminders();

    verifyNoInteractions(eventReminderService);
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
    return EventReminderJob.class.getMethod("sendDueReminders");
  }
}
