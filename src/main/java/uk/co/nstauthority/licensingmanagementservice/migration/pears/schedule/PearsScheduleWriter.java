package uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule;

import jakarta.annotation.Nullable;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.PhaseType;
import uk.co.nstauthority.licensingmanagementservice.licence.TermType;
import uk.co.nstauthority.licensingmanagementservice.licence.crosslicenceeventtracker.CrossLicenceEventTrackerService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceSchedule;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.areaevent.LicenceScheduleAreaEvent;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.areaevent.LicenceScheduleAreaEventRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.calculation.LicenceScheduleCalculationService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventcomments.EventComment;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventcomments.EventCommentRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventcomments.EventCommentStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventreference.ScheduleEvent;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleexpiry.LicenceScheduleExpiry;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleexpiry.LicenceScheduleExpiryRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhase;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhaseService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate.LicenceScheduleRate;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate.LicenceScheduleRateService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTermService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencestartdate.LicenceStartDate;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencestartdate.LicenceStartDateService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent.OtherScheduleEvent;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent.OtherScheduleEventCategory;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent.OtherScheduleEventDateOption;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent.OtherScheduleEventService;
import uk.co.nstauthority.licensingmanagementservice.migration.MigrationUser;

/**
 * Saves a licence's mapped PEARS schedule as its LMS schedule, in the caller's transaction (plan, design section 3).
 * Every version becomes a schedule detail, oldest first, so events sharing an {@link MappedTerm#eventKey} can be given
 * the first one's {@code originalEventId}.
 *
 * <p>The expiry is stored as the day before PEARS's expiry date, where LMS ends the final term (Open question 8,
 * decided for now on 18 November 2026).
 */
@Service
class PearsScheduleWriter {

  private static final Set<String> AREA_EVENT_TYPES = Set.of("RD", "PA");

  private static final Map<Integer, TermType> TERM_TYPES = Map.of(
      1, TermType.INITIAL,
      2, TermType.SECOND,
      3, TermType.THIRD,
      4, TermType.FOURTH
  );

  private final LicenceScheduleService licenceScheduleService;
  private final LicenceScheduleDetailService licenceScheduleDetailService;
  private final LicenceStartDateService licenceStartDateService;
  private final LicenceScheduleTermService licenceScheduleTermService;
  private final LicenceSchedulePhaseService licenceSchedulePhaseService;
  private final LicenceScheduleRateService licenceScheduleRateService;
  private final OtherScheduleEventService otherScheduleEventService;
  private final LicenceScheduleExpiryRepository licenceScheduleExpiryRepository;
  private final EventCommentRepository eventCommentRepository;
  private final LicenceScheduleAreaEventRepository licenceScheduleAreaEventRepository;
  private final PearsScheduleEventMigrationLinkRepository eventMigrationLinkRepository;
  private final PearsScheduleMigratedLicenceRepository migratedLicenceRepository;
  private final LicenceScheduleCalculationService licenceScheduleCalculationService;
  private final CrossLicenceEventTrackerService crossLicenceEventTrackerService;
  private final EntityManager entityManager;
  private final Clock clock;

  @SuppressWarnings("java:S107") // One collaborator per part of the schedule it saves
  PearsScheduleWriter(
      LicenceScheduleService licenceScheduleService,
      LicenceScheduleDetailService licenceScheduleDetailService,
      LicenceStartDateService licenceStartDateService,
      LicenceScheduleTermService licenceScheduleTermService,
      LicenceSchedulePhaseService licenceSchedulePhaseService,
      LicenceScheduleRateService licenceScheduleRateService,
      OtherScheduleEventService otherScheduleEventService,
      LicenceScheduleExpiryRepository licenceScheduleExpiryRepository,
      EventCommentRepository eventCommentRepository,
      LicenceScheduleAreaEventRepository licenceScheduleAreaEventRepository,
      PearsScheduleEventMigrationLinkRepository eventMigrationLinkRepository,
      PearsScheduleMigratedLicenceRepository migratedLicenceRepository,
      LicenceScheduleCalculationService licenceScheduleCalculationService,
      CrossLicenceEventTrackerService crossLicenceEventTrackerService,
      EntityManager entityManager,
      Clock clock
  ) {
    this.licenceScheduleService = licenceScheduleService;
    this.licenceScheduleDetailService = licenceScheduleDetailService;
    this.licenceStartDateService = licenceStartDateService;
    this.licenceScheduleTermService = licenceScheduleTermService;
    this.licenceSchedulePhaseService = licenceSchedulePhaseService;
    this.licenceScheduleRateService = licenceScheduleRateService;
    this.otherScheduleEventService = otherScheduleEventService;
    this.licenceScheduleExpiryRepository = licenceScheduleExpiryRepository;
    this.eventCommentRepository = eventCommentRepository;
    this.licenceScheduleAreaEventRepository = licenceScheduleAreaEventRepository;
    this.eventMigrationLinkRepository = eventMigrationLinkRepository;
    this.migratedLicenceRepository = migratedLicenceRepository;
    this.licenceScheduleCalculationService = licenceScheduleCalculationService;
    this.crossLicenceEventTrackerService = crossLicenceEventTrackerService;
    this.entityManager = entityManager;
    this.clock = clock;
  }

  /**
   * Saves the schedule, every version's events and comments, and the records the comparison report and cleardown use.
   *
   * @param pearsSchedule the schedule as PEARS holds it, for the {@code RD} and {@code PA} area events
   * @param runId         the migration run, recorded against the licence for the cleardown
   * @return the schedule, or empty when the licence already has one. The licence's row is locked and the check made
   *     here, in the caller's transaction, so two migrations of the same licence at once can't both write a schedule:
   *     the second waits for the first and then finds its schedule
   */
  Optional<LicenceSchedule> write(
      Licence licence,
      MappedSchedule mappedSchedule,
      PearsSchedule pearsSchedule,
      UUID runId
  ) {
    entityManager.find(Licence.class, licence.getId(), LockModeType.PESSIMISTIC_WRITE);
    if (!licenceScheduleService.getIdsOfLicencesWithASchedule(List.of(licence)).isEmpty()) {
      return Optional.empty();
    }

    var licenceSchedule = new LicenceSchedule();
    licenceSchedule.setLicence(licence);
    licenceScheduleService.saveLicenceSchedules(List.of(licenceSchedule));

    var originalEventIds = new HashMap<Integer, UUID>();
    var details = new ArrayList<LicenceScheduleDetail>();

    for (var version : mappedSchedule.versions()) {
      details.add(writeVersion(licenceSchedule, version, pearsVersion(pearsSchedule, version.plsdId()), originalEventIds));
    }

    details.forEach(licenceScheduleCalculationService::calculateAndSaveLicenceScheduleDates);
    details.stream()
        .filter(detail -> detail.getStatus() == LicenceScheduleDetailStatus.ACTIVE)
        .forEach(crossLicenceEventTrackerService::refreshScheduleCache);

    migratedLicenceRepository.save(
        new PearsScheduleMigratedLicence(licenceSchedule.getId(), licence.getId(), runId, Instant.now(clock)));
    return Optional.of(licenceSchedule);
  }

  private LicenceScheduleDetail writeVersion(
      LicenceSchedule licenceSchedule,
      MappedScheduleVersion version,
      PearsScheduleVersion pearsVersion,
      Map<Integer, UUID> originalEventIds
  ) {
    var detail = new LicenceScheduleDetail();
    detail.setLicenceSchedule(licenceSchedule);
    detail.setStatus(version.status());
    detail.setCreatedInstant(version.createdInstant());
    detail.setAppliedInstant(version.createdInstant());
    detail.setCreatedByWuaId(MigrationUser.WUA_ID);
    detail.setAppliedByWuaId(MigrationUser.WUA_ID);
    licenceScheduleDetailService.saveLicenceScheduleDetails(List.of(detail));

    var startDate = new LicenceStartDate();
    startDate.setLicenceScheduleDetail(detail);
    startDate.setStartDate(version.startDate());
    licenceStartDateService.saveLicenceStartDates(List.of(startDate));

    var written = new VersionEvents(licenceSchedule, originalEventIds);
    var terms = writeTerms(detail, version, written);
    var phases = writePhases(detail, version, terms, written);
    writeRates(detail, version, terms, phases, written);
    writeOtherEvents(detail, version, terms, written);
    writeExpiry(detail, version, written);
    written.rememberOriginalEventIds();

    eventCommentRepository.saveAll(written.newComments(version.createdInstant()));
    eventMigrationLinkRepository.saveAll(written.links(version.plsdId()));
    licenceScheduleAreaEventRepository.saveAll(areaEvents(detail, pearsVersion));

    return detail;
  }

  private Map<Integer, LicenceScheduleTerm> writeTerms(
      LicenceScheduleDetail detail,
      MappedScheduleVersion version,
      VersionEvents written
  ) {
    var terms = new HashMap<Integer, LicenceScheduleTerm>();

    for (var mappedTerm : version.terms()) {
      var term = new LicenceScheduleTerm();
      term.setLicenceScheduleDetail(detail);
      term.setTermType(termType(mappedTerm.termNo()));
      term.setTermDuration(mappedTerm.duration());
      written.add(term, mappedTerm.eventKey(), mappedTerm.source(), mappedTerm.comment());
      terms.put(mappedTerm.termNo(), term);
    }

    licenceScheduleTermService.saveTerms(List.copyOf(terms.values()));
    return terms;
  }

  private Map<PhaseType, LicenceSchedulePhase> writePhases(
      LicenceScheduleDetail detail,
      MappedScheduleVersion version,
      Map<Integer, LicenceScheduleTerm> terms,
      VersionEvents written
  ) {
    var phases = new EnumMap<PhaseType, LicenceSchedulePhase>(PhaseType.class);

    for (var mappedPhase : version.phases()) {
      var phase = new LicenceSchedulePhase();
      phase.setLicenceScheduleDetail(detail);
      phase.setLicenceScheduleTerm(terms.get(1));
      phase.setPhaseType(mappedPhase.phaseType());
      phase.setPhaseDuration(mappedPhase.duration());
      written.add(phase, mappedPhase.eventKey(), mappedPhase.source(), mappedPhase.comment());
      phases.put(mappedPhase.phaseType(), phase);
    }

    licenceSchedulePhaseService.saveLicenceSchedulePhases(List.copyOf(phases.values()));
    return phases;
  }

  private void writeRates(
      LicenceScheduleDetail detail,
      MappedScheduleVersion version,
      Map<Integer, LicenceScheduleTerm> terms,
      Map<PhaseType, LicenceSchedulePhase> phases,
      VersionEvents written
  ) {
    var rates = new ArrayList<LicenceScheduleRate>();

    for (var mappedRate : version.rates()) {
      var rate = new LicenceScheduleRate();
      rate.setLicenceScheduleDetail(detail);
      if (mappedRate.phaseType() != null) {
        rate.setLicenceSchedulePhase(phases.get(mappedRate.phaseType()));
      } else {
        rate.setLicenceScheduleTerm(terms.get(mappedRate.termNo()));
      }
      rate.setRateDefinitionOption(mappedRate.definitionOption());
      rate.setRateRelativeDateOption(mappedRate.relativeDateOption());
      rate.setRelativeDuration(mappedRate.relativeDuration());
      rate.setStartDate(mappedRate.startDate());
      rate.setRentalRate(mappedRate.rentalRate());
      written.add(rate, mappedRate.eventKey(), mappedRate.source(), mappedRate.comment());
      rates.add(rate);
    }

    licenceScheduleRateService.saveLicenceScheduleRates(rates);
  }

  private void writeOtherEvents(
      LicenceScheduleDetail detail,
      MappedScheduleVersion version,
      Map<Integer, LicenceScheduleTerm> terms,
      VersionEvents written
  ) {
    var otherEvents = new ArrayList<OtherScheduleEvent>();

    for (var mappedEvent : version.otherEvents()) {
      var otherEvent = new OtherScheduleEvent();
      otherEvent.setLicenceScheduleDetail(detail);
      otherEvent.setCategory(category(mappedEvent.category()));
      otherEvent.setOtherCategoryName(mappedEvent.otherCategoryName());
      otherEvent.setDescription(mappedEvent.description());
      otherEvent.setRelinquishmentPercentage(mappedEvent.relinquishmentPercentage());
      otherEvent.setDateOption(OtherScheduleEventDateOption.RELATIVE_DATE);
      otherEvent.setLicenceScheduleTerm(terms.get(mappedEvent.termNo()));
      otherEvent.setRelativeDuration(mappedEvent.relativeDuration());
      otherEvent.setEventDate(mappedEvent.date());
      written.add(otherEvent, mappedEvent.eventKey(), mappedEvent.source(), null);
      otherEvents.add(otherEvent);
    }

    otherScheduleEventService.saveScheduleEvents(otherEvents);
  }

  private void writeExpiry(LicenceScheduleDetail detail, MappedScheduleVersion version, VersionEvents written) {
    var mappedExpiry = version.expiry();
    if (mappedExpiry == null) {
      return;
    }

    var expiry = new LicenceScheduleExpiry();
    expiry.setLicenceScheduleDetail(detail);
    expiry.setExpiryDate(mappedExpiry.pearsDate().minusDays(1));
    expiry.setComments(mappedExpiry.comments());
    written.add(expiry, mappedExpiry.eventKey(), mappedExpiry.source(), null);
    licenceScheduleExpiryRepository.save(expiry);
  }

  private static List<LicenceScheduleAreaEvent> areaEvents(LicenceScheduleDetail detail, PearsScheduleVersion pearsVersion) {
    return pearsVersion.events().stream()
        // Events with no type are left out, as the mapper leaves them out
        .filter(event -> event.eventMnem() != null && AREA_EVENT_TYPES.contains(event.eventMnem()))
        .map(event -> {
          var areaEvent = new LicenceScheduleAreaEvent();
          areaEvent.setLicenceScheduleDetail(detail);
          areaEvent.setEventMnem(event.eventMnem());
          areaEvent.setSubtype(event.eventSubtype());
          areaEvent.setEventDate(event.eventDate());
          areaEvent.setComments(event.comments());
          areaEvent.setPearsEventId(event.eventId());
          if (event.area() != null) {
            areaEvent.setAreaName(event.area().name());
            areaEvent.setAreaShortName(event.area().shortName());
            areaEvent.setAreaType(event.area().type());
            areaEvent.setAreaVersion(event.area().version());
          }
          return areaEvent;
        })
        .toList();
  }

  private static PearsScheduleVersion pearsVersion(PearsSchedule pearsSchedule, long plsdId) {
    return pearsSchedule.versions().stream()
        .filter(version -> version.plsdId() == plsdId)
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("No PEARS version %d".formatted(plsdId)));
  }

  private static TermType termType(int termNo) {
    var termType = TERM_TYPES.get(termNo);
    if (termType == null) {
      throw new IllegalStateException("No LMS term type for term %d".formatted(termNo));
    }
    return termType;
  }

  /**
   * The LMS category. A bespoke milestone needs {@code BESPOKE_MILESTONE}, which LMS doesn't have yet, so
   * {@link PearsScheduleMigrationService} skips those licences before they get here.
   */
  private static OtherScheduleEventCategory category(MappedOtherEvent.Category category) {
    return switch (category) {
      case MANDATORY_RELINQUISHMENT -> OtherScheduleEventCategory.MANDATORY_RELINQUISHMENT;
      case OTHER_ACTIVITY -> OtherScheduleEventCategory.OTHER_ACTIVITY;
      case BESPOKE_MILESTONE -> throw new IllegalStateException("LMS has no bespoke milestone category yet");
    };
  }

  /**
   * One version's events as they are saved: each event's key gets the {@code originalEventId} of the first event saved
   * with it, and its new comment and PEARS source are kept for after the events are saved.
   */
  private static final class VersionEvents {

    private final LicenceSchedule licenceSchedule;
    private final Map<Integer, UUID> originalEventIds;
    private final List<WrittenEvent> events = new ArrayList<>();

    private VersionEvents(LicenceSchedule licenceSchedule, Map<Integer, UUID> originalEventIds) {
      this.licenceSchedule = licenceSchedule;
      this.originalEventIds = originalEventIds;
    }

    void add(ScheduleEvent event, int eventKey, PearsScheduleEvent source, @Nullable MappedComment comment) {
      event.setLicenceSchedule(licenceSchedule);
      event.setOriginalEventId(originalEventIds.get(eventKey));
      events.add(new WrittenEvent(event, eventKey, source, comment));
    }

    /**
     * Records the {@code originalEventId} of events seen for the first time, which LMS sets to their own id when they
     * are saved.
     */
    void rememberOriginalEventIds() {
      events.forEach(written -> originalEventIds.putIfAbsent(written.eventKey(), written.event().getOriginalEventId()));
    }

    List<EventComment> newComments(Instant versionSaved) {
      return events.stream()
          .filter(written -> written.comment() != null && written.comment().isNew())
          .map(written -> {
            var eventComment = new EventComment();
            eventComment.setScheduleEvent(written.event());
            eventComment.setComment(written.comment().text());
            eventComment.setStatus(EventCommentStatus.PUBLISHED);
            eventComment.setTimestamp(versionSaved);
            return eventComment;
          })
          .toList();
    }

    List<PearsScheduleEventMigrationLink> links(long plsdId) {
      return events.stream()
          .map(written -> new PearsScheduleEventMigrationLink(written.event().getId(), plsdId, written.source()))
          .toList();
    }
  }

  private record WrittenEvent(
      ScheduleEvent event,
      int eventKey,
      PearsScheduleEvent source,
      @Nullable MappedComment comment
  ) {
  }
}
