package uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.areaevent.LicenceScheduleAreaEventService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventreference.ScheduleEvent;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleexpiry.LicenceScheduleExpiryService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhaseService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate.LicenceScheduleRateService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTermService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencestartdate.LicenceStartDate;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencestartdate.LicenceStartDateRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent.OtherScheduleEventService;

/**
 * Reads an LMS schedule detail for the comparison report, through the services LMS reads it with. No dates are
 * recalculated: the report shows what is stored.
 */
@Service
@Profile("migration")
class LmsScheduleVersionReader {

  private final LicenceStartDateRepository licenceStartDateRepository;
  private final LicenceScheduleTermService licenceScheduleTermService;
  private final LicenceSchedulePhaseService licenceSchedulePhaseService;
  private final LicenceScheduleRateService licenceScheduleRateService;
  private final OtherScheduleEventService otherScheduleEventService;
  private final LicenceScheduleExpiryService licenceScheduleExpiryService;
  private final LicenceScheduleAreaEventService licenceScheduleAreaEventService;
  private final PearsScheduleEventMigrationLinkRepository eventMigrationLinkRepository;

  @SuppressWarnings("java:S107") // One collaborator per part of the schedule it reads
  LmsScheduleVersionReader(
      LicenceStartDateRepository licenceStartDateRepository,
      LicenceScheduleTermService licenceScheduleTermService,
      LicenceSchedulePhaseService licenceSchedulePhaseService,
      LicenceScheduleRateService licenceScheduleRateService,
      OtherScheduleEventService otherScheduleEventService,
      LicenceScheduleExpiryService licenceScheduleExpiryService,
      LicenceScheduleAreaEventService licenceScheduleAreaEventService,
      PearsScheduleEventMigrationLinkRepository eventMigrationLinkRepository
  ) {
    this.licenceStartDateRepository = licenceStartDateRepository;
    this.licenceScheduleTermService = licenceScheduleTermService;
    this.licenceSchedulePhaseService = licenceSchedulePhaseService;
    this.licenceScheduleRateService = licenceScheduleRateService;
    this.otherScheduleEventService = otherScheduleEventService;
    this.licenceScheduleExpiryService = licenceScheduleExpiryService;
    this.licenceScheduleAreaEventService = licenceScheduleAreaEventService;
    this.eventMigrationLinkRepository = eventMigrationLinkRepository;
  }

  LmsScheduleVersion read(LicenceScheduleDetail detail) {
    var terms = licenceScheduleTermService.getTermsByLicenceScheduleDetail(detail);
    var phases = licenceSchedulePhaseService.getPhasesByLicenceScheduleDetail(detail);
    var rates = licenceScheduleRateService.getLicenceScheduleRates(detail);
    var otherEvents = otherScheduleEventService.getOtherScheduleEvents(detail);
    var expiry = licenceScheduleExpiryService.getExpiryForLicenceScheduleDetail(detail);

    var eventIds = Stream.of(terms, phases, rates, otherEvents, expiry.stream().toList())
        .flatMap(List::stream)
        .map(ScheduleEvent::getId)
        .toList();
    var links = eventMigrationLinkRepository.findAllById(eventIds).stream()
        .collect(Collectors.toMap(PearsScheduleEventMigrationLink::getScheduleEventId, Function.identity()));

    return new LmsScheduleVersion(
        detail,
        licenceStartDateRepository.findByLicenceScheduleDetail(detail).map(LicenceStartDate::getStartDate).orElse(null),
        terms,
        phases,
        rates,
        otherEvents,
        expiry.orElse(null),
        licenceScheduleAreaEventService.getAreaEvents(detail),
        links
    );
  }
}
