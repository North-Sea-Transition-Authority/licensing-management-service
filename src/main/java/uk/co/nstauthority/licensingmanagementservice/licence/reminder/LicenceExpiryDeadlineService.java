package uk.co.nstauthority.licensingmanagementservice.licence.reminder;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleexpiry.LicenceScheduleExpiry;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleexpiry.LicenceScheduleExpiryRepository;

@Service
public class LicenceExpiryDeadlineService implements ReminderDeadlineSource {

  static final String DISPLAY_NAME = "Licence expiry";

  private static final int PREFILTER_BUFFER_DAYS = 3;

  private final Clock clock;
  private final LicenceScheduleExpiryRepository licenceScheduleExpiryRepository;

  public LicenceExpiryDeadlineService(
      Clock clock,
      LicenceScheduleExpiryRepository licenceScheduleExpiryRepository
  ) {
    this.clock = clock;
    this.licenceScheduleExpiryRepository = licenceScheduleExpiryRepository;
  }

  @Override
  public List<ReminderDeadline> getDeadlinesDueReminder() {
    var noticePeriod = ReminderType.LICENCE_EXPIRY.getNoticePeriod();
    var today = LocalDate.now(clock);
    var latestExpiryDate = today.plusMonths(noticePeriod.getMonths()).plusDays(PREFILTER_BUFFER_DAYS);

    return licenceScheduleExpiryRepository
        .findAllByExpiryDateBetweenAndLicenceScheduleDetail_Status(
            today, latestExpiryDate, LicenceScheduleDetailStatus.ACTIVE)
        .stream()
        .filter(expiry -> noticePeriod.isDueBy(expiry.getExpiryDate(), today))
        .map(this::toDeadline)
        .toList();
  }

  private ReminderDeadline toDeadline(LicenceScheduleExpiry expiry) {
    return new ReminderDeadline(
        expiry,
        null,
        expiry.getLicenceSchedule().getLicence(),
        expiry.getExpiryDate(),
        DISPLAY_NAME,
        ReminderType.LICENCE_EXPIRY);
  }
}
