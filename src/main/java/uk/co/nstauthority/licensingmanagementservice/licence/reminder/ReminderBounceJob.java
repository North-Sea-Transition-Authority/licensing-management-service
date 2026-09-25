package uk.co.nstauthority.licensingmanagementservice.licence.reminder;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class ReminderBounceJob {

  private static final Logger LOGGER = LoggerFactory.getLogger(ReminderBounceJob.class);

  private final ReminderBounceService reminderBounceService;

  public ReminderBounceJob(ReminderBounceService reminderBounceService) {
    this.reminderBounceService = reminderBounceService;
  }

  @Scheduled(cron = "0 0 8 * * *", zone = "Europe/London")
  @SchedulerLock(name = "licenceReminderBounces", lockAtMostFor = "PT1H", lockAtLeastFor = "PT1M")
  public void reportBounces() {
    LOGGER.info("Starting licence reminder bounce report");

    var reported = reminderBounceService.reportBounces();

    LOGGER.info("Completed licence reminder bounce report: {} reminders reported", reported);
  }
}
