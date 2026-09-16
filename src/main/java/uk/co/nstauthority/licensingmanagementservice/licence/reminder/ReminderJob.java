package uk.co.nstauthority.licensingmanagementservice.licence.reminder;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class ReminderJob {

  private static final Logger LOGGER = LoggerFactory.getLogger(ReminderJob.class);

  private final ReminderService reminderService;

  public ReminderJob(ReminderService reminderService) {
    this.reminderService = reminderService;
  }

  @Scheduled(cron = "0 0 7 * * *", zone = "Europe/London")
  @SchedulerLock(name = "licenceReminders", lockAtMostFor = "PT1H", lockAtLeastFor = "PT1M")
  public void sendDueReminders() {
    LOGGER.info("Starting licence reminder run");

    var summary = reminderService.sendDueReminders();

    LOGGER.info("Completed licence reminder run: {}", summary);
  }
}
