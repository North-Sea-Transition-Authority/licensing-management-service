package uk.co.nstauthority.licensingmanagementservice.licence.eventreminder;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.phasedrelease.FeatureFlagService;
import uk.co.nstauthority.licensingmanagementservice.phasedrelease.ReleasePhase;

@Service
public class EventReminderJob {

  private static final Logger LOGGER = LoggerFactory.getLogger(EventReminderJob.class);

  private final EventReminderService eventReminderService;
  private final FeatureFlagService featureFlagService;

  public EventReminderJob(EventReminderService eventReminderService, FeatureFlagService featureFlagService) {
    this.eventReminderService = eventReminderService;
    this.featureFlagService = featureFlagService;
  }

  @Scheduled(cron = "0 0 7 * * *", zone = "Europe/London")
  @SchedulerLock(name = "licenceReminders", lockAtMostFor = "PT1H", lockAtLeastFor = "PT1M")
  public void sendDueReminders() {
    if (!featureFlagService.isEnabled(ReleasePhase.LMS1)) {
      LOGGER.info("Skipping licence reminder run as the LMS1 release phase is not enabled");
      return;
    }

    LOGGER.info("Starting licence reminder run");

    var summary = eventReminderService.sendDueReminders();

    LOGGER.info("Completed licence reminder run: {}", summary);
  }
}
