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
class EventReminderBounceJobTest {

  @Mock
  private EventReminderBounceService eventReminderBounceService;

  @Mock
  private FeatureFlagService featureFlagService;

  @InjectMocks
  private EventReminderBounceJob eventReminderBounceJob;

  @Test
  void reportBounces_whenLms1Enabled_thenDelegatesToTheBounceService() {
    when(featureFlagService.isEnabled(ReleasePhase.LMS1)).thenReturn(true);
    when(eventReminderBounceService.reportBounces()).thenReturn(2);

    eventReminderBounceJob.reportBounces();

    verify(eventReminderBounceService).reportBounces();
  }

  @Test
  void reportBounces_whenLms1NotEnabled_thenNoBouncesReported() {
    when(featureFlagService.isEnabled(ReleasePhase.LMS1)).thenReturn(false);

    eventReminderBounceJob.reportBounces();

    verifyNoInteractions(eventReminderBounceService);
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
    return EventReminderBounceJob.class.getMethod("reportBounces");
  }
}
