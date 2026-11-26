package uk.co.nstauthority.licensingmanagementservice.migration.pears.workprogramme;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhaseService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTermService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencestartdate.LicenceStartDateService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent.OtherScheduleEventService;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.PearsScheduleEventMigrationLink;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.PearsScheduleEventMigrationLinkRepository;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.PearsScheduleMigratedLicenceRepository;

/**
 * Reads a licence's schedule as the schedule migration wrote it. Only schedules the schedule migration created are read:
 * a work programme is never migrated onto a schedule LMS created itself.
 */
@Service
class MigratedScheduleReader {

  private static final String DD = "DD";

  private final PearsScheduleMigratedLicenceRepository migratedLicenceRepository;
  private final LicenceScheduleDetailService licenceScheduleDetailService;
  private final LicenceStartDateService licenceStartDateService;
  private final LicenceScheduleTermService licenceScheduleTermService;
  private final LicenceSchedulePhaseService licenceSchedulePhaseService;
  private final OtherScheduleEventService otherScheduleEventService;
  private final PearsScheduleEventMigrationLinkRepository eventMigrationLinkRepository;

  MigratedScheduleReader(
      PearsScheduleMigratedLicenceRepository migratedLicenceRepository,
      LicenceScheduleDetailService licenceScheduleDetailService,
      LicenceStartDateService licenceStartDateService,
      LicenceScheduleTermService licenceScheduleTermService,
      LicenceSchedulePhaseService licenceSchedulePhaseService,
      OtherScheduleEventService otherScheduleEventService,
      PearsScheduleEventMigrationLinkRepository eventMigrationLinkRepository
  ) {
    this.migratedLicenceRepository = migratedLicenceRepository;
    this.licenceScheduleDetailService = licenceScheduleDetailService;
    this.licenceStartDateService = licenceStartDateService;
    this.licenceScheduleTermService = licenceScheduleTermService;
    this.licenceSchedulePhaseService = licenceSchedulePhaseService;
    this.otherScheduleEventService = otherScheduleEventService;
    this.eventMigrationLinkRepository = eventMigrationLinkRepository;
  }

  /**
   * The licence's {@code ACTIVE} schedule detail, or empty when the schedule migration didn't create its schedule.
   */
  Optional<ActiveScheduleDetail> read(Licence licence) {
    if (migratedLicenceRepository.findByLicenceReference(licence.getLicenceReference()).isEmpty()) {
      return Optional.empty();
    }
    return licenceScheduleDetailService.getScheduleDetailByLicenceAndStatus(licence, LicenceScheduleDetailStatus.ACTIVE)
        .map(this::read);
  }

  private ActiveScheduleDetail read(LicenceScheduleDetail detail) {
    var terms = licenceScheduleTermService.getTermsByLicenceScheduleDetail(detail);
    var phases = licenceSchedulePhaseService.getPhasesByLicenceScheduleDetail(detail);
    var otherEvents = otherScheduleEventService.getOtherScheduleEvents(detail);

    // A copy in a later LMS version has no link of its own, but keeps the originalEventId of a migrated event that has
    var ddEventIds = ddEventIds(otherEvents.stream()
        .flatMap(event -> Stream.of(event.getId(), event.getOriginalEventId()))
        .collect(Collectors.toSet()));
    var ddEvents = otherEvents.stream()
        .filter(event -> ddEventIds.contains(event.getId()) || ddEventIds.contains(event.getOriginalEventId()))
        .map(event -> new MigratedSchedule.DdEvent(
            event.getLicenceScheduleTerm().getTermType(),
            event.getRelativeDuration(),
            event.getEventDate()
        ))
        .toList();

    var schedule = new MigratedSchedule(
        licenceStartDateService.getByLicenceScheduleDetailOrThrow(detail).getStartDate(),
        terms.stream()
            .map(term -> new MigratedSchedule.Term(term.getTermType(), term.getStartDate(), term.getEndDate()))
            .toList(),
        phases.stream()
            .map(phase -> new MigratedSchedule.Phase(phase.getPhaseType(), phase.getStartDate(), phase.getEndDate()))
            .toList(),
        ddEvents
    );
    return new ActiveScheduleDetail(detail, terms, phases, schedule);
  }

  private Set<UUID> ddEventIds(Set<UUID> otherEventIds) {
    return eventMigrationLinkRepository.findAllById(otherEventIds).stream()
        .filter(link -> DD.equals(link.getPearsEventMnem()))
        .map(PearsScheduleEventMigrationLink::getScheduleEventId)
        .collect(Collectors.toSet());
  }
}
