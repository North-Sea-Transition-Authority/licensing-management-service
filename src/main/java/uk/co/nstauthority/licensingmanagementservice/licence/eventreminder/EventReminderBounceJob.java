package uk.co.nstauthority.licensingmanagementservice.licence.eventreminder;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.phasedrelease.FeatureFlagService;
import uk.co.nstauthority.licensingmanagementservice.phasedrelease.ReleasePhase;

@Service
public class EventReminderBounceJob {

  private static final Logger LOGGER = LoggerFactory.getLogger(EventReminderBounceJob.class);

  private final EventReminderBounceService eventReminderBounceService;
  private final FeatureFlagService featureFlagService;

  public EventReminderBounceJob(EventReminderBounceService eventReminderBounceService,
                                FeatureFlagService featureFlagService) {
    this.eventReminderBounceService = eventReminderBounceService;
    this.featureFlagService = featureFlagService;
  }

  @Scheduled(cron = "0 0 8 * * *", zone = "Europe/London")
  @SchedulerLock(name = "licenceReminderBounces", lockAtMostFor = "PT1H", lockAtLeastFor = "PT1M")
  public void reportBounces() {
    if (!featureFlagService.isEnabled(ReleasePhase.LMS1)) {
      LOGGER.info("Skipping licence reminder bounce report as the LMS1 release phase is not enabled");
      return;
    }

    LOGGER.info("Starting licence reminder bounce report");

    var reported = eventReminderBounceService.reportBounces();

    LOGGER.info("Completed licence reminder bounce report: {} reminders reported", reported);
  }
}
