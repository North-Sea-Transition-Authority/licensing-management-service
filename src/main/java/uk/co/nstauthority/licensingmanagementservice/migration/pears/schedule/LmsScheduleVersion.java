package uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule;

import jakarta.annotation.Nullable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.areaevent.LicenceScheduleAreaEvent;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventreference.ScheduleEvent;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleexpiry.LicenceScheduleExpiry;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhase;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate.LicenceScheduleRate;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent.OtherScheduleEvent;

/**
 * One LMS schedule detail with everything the comparison report shows, as stored.
 *
 * @param links the PEARS event each migrated event came from, by schedule event id. Events added in LMS have none
 */
record LmsScheduleVersion(
    LicenceScheduleDetail detail,
    @Nullable LocalDate startDate,
    List<LicenceScheduleTerm> terms,
    List<LicenceSchedulePhase> phases,
    List<LicenceScheduleRate> rates,
    List<OtherScheduleEvent> otherEvents,
    @Nullable LicenceScheduleExpiry expiry,
    List<LicenceScheduleAreaEvent> areaEvents,
    Map<UUID, PearsScheduleEventMigrationLink> links
) {

  /**
   * The PEARS version the detail was migrated from: every link of one detail names the same one.
   */
  Optional<Long> pearsPlsdId() {
    return links.values().stream().map(PearsScheduleEventMigrationLink::getPearsPlsdId).findFirst();
  }

  /**
   * The detail's events that can have a link: everything but the start date and the area events.
   */
  List<ScheduleEvent> linkableEvents() {
    var events = new ArrayList<ScheduleEvent>();
    Stream.of(terms, phases, rates, otherEvents).forEach(events::addAll);
    if (expiry != null) {
      events.add(expiry);
    }
    return events;
  }
}
