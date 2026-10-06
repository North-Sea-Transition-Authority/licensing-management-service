package uk.co.nstauthority.licensingmanagementservice.licence.eventreminder;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent.OtherScheduleEvent;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent.OtherScheduleEventDateOption;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent.OtherScheduleEventRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent.OtherScheduleEventService;

@Service
public class OtherScheduleEventDeadlineServiceEvent implements EventReminderDeadlineSource {

  private static final Logger LOGGER = LoggerFactory.getLogger(OtherScheduleEventDeadlineServiceEvent.class);

  private final Clock clock;
  private final OtherScheduleEventRepository otherScheduleEventRepository;
  private final OtherScheduleEventService otherScheduleEventService;

  public OtherScheduleEventDeadlineServiceEvent(
      Clock clock,
      OtherScheduleEventRepository otherScheduleEventRepository,
      OtherScheduleEventService otherScheduleEventService
  ) {
    this.clock = clock;
    this.otherScheduleEventRepository = otherScheduleEventRepository;
    this.otherScheduleEventService = otherScheduleEventService;
  }

  @Override
  public List<EventReminderDeadline> getDeadlinesDueReminder() {
    var noticePeriod = ReminderType.OTHER_SCHEDULE_EVENT.getNoticePeriod();
    var today = LocalDate.now(clock);
    var latestEventDate = noticePeriod.getLatestDeadlineDate(today);

    return otherScheduleEventRepository
        .findAllByDateOptionAndEventDateBetweenAndLicenceScheduleDetail_Status(
            OtherScheduleEventDateOption.RELATIVE_DATE, today, latestEventDate, LicenceScheduleDetailStatus.ACTIVE)
        .stream()
        .filter(this::hasCategory)
        .filter(event -> noticePeriod.isWithinNoticeWindow(getEventDate(event), today))
        .map(this::toDeadline)
        .toList();
  }

  private boolean hasCategory(OtherScheduleEvent event) {
    if (event.getCategory() == null) {
      LOGGER.warn("Skipping other schedule event {} for reminders as it has no category", event.getId());
      return false;
    }

    return true;
  }

  private LocalDate getEventDate(OtherScheduleEvent event) {
    return otherScheduleEventService.resolveOtherScheduleEventDate(event);
  }

  private String getDisplayName(OtherScheduleEvent event) {
    var caption = event.getEventCaption();

    if (StringUtils.isBlank(event.getDescription())) {
      return caption;
    }

    return DISPLAY_NAME_FORMAT.formatted(caption, event.getDescription());
  }

  private EventReminderDeadline toDeadline(OtherScheduleEvent event) {
    return new EventReminderDeadline(
        event,
        event.getOriginalEventId(),
        event.getLicenceSchedule().getLicence(),
        getEventDate(event),
        getDisplayName(event),
        ReminderType.OTHER_SCHEDULE_EVENT);
  }
}
