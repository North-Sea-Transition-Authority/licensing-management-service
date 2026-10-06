package uk.co.nstauthority.licensingmanagementservice.licence.eventreminder;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleexpiry.LicenceScheduleExpiry;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleexpiry.LicenceScheduleExpiryRepository;

@Service
public class LicenceExpiryDeadlineServiceEvent implements EventReminderDeadlineSource {

  static final String DISPLAY_NAME = "Licence expiry";

  private final Clock clock;
  private final LicenceScheduleExpiryRepository licenceScheduleExpiryRepository;

  public LicenceExpiryDeadlineServiceEvent(
      Clock clock,
      LicenceScheduleExpiryRepository licenceScheduleExpiryRepository
  ) {
    this.clock = clock;
    this.licenceScheduleExpiryRepository = licenceScheduleExpiryRepository;
  }

  @Override
  public List<EventReminderDeadline> getDeadlinesDueReminder() {
    var noticePeriod = ReminderType.LICENCE_EXPIRY.getNoticePeriod();
    var today = LocalDate.now(clock);
    var latestExpiryDate = noticePeriod.getLatestDeadlineDate(today);

    return licenceScheduleExpiryRepository
        .findAllByExpiryDateBetweenAndLicenceScheduleDetail_Status(
            today, latestExpiryDate, LicenceScheduleDetailStatus.ACTIVE)
        .stream()
        .map(this::toDeadline)
        .toList();
  }

  private EventReminderDeadline toDeadline(LicenceScheduleExpiry expiry) {
    return new EventReminderDeadline(
        expiry,
        null,
        expiry.getLicenceSchedule().getLicence(),
        expiry.getExpiryDate(),
        DISPLAY_NAME,
        ReminderType.LICENCE_EXPIRY);
  }
}
