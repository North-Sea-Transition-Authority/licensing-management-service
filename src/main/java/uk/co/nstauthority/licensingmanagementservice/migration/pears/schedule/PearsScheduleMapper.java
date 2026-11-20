package uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule;

import jakarta.annotation.Nullable;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import uk.co.nstauthority.licensingmanagementservice.licence.PhaseType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailStatus;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.PearsLicenceReference;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.operation.MigrationNotes;

/**
 * Maps a licence's PEARS schedule to what its LMS schedule will hold: one schedule detail per PEARS version, each with
 * its start date, terms, phases, expiry, rates and other events. A pure function of the schedule and the event type
 * catalogue, with no Spring or database, so it is tested from fixtures.
 *
 * <p>The rules are the plan's ("Term boundaries", "Phase boundaries", "Schedule history"). PEARS dates a term or phase
 * by its end: a {@code T} event with term number n is the date term n ends and the next term present starts. Durations
 * are worked out from those dates, so LMS lands on the same dates as PEARS. Rates are mapped by
 * {@link PearsRateMapper}, other events by {@link PearsOtherEventMapper}, and {@link EventKeys} matches events across
 * versions.
 *
 * <p>The {@code RD} and {@code PA} area events are copied by the write step; here they are only checked to be event
 * types the migration knows. A schedule whose active version this can't map throws a
 * {@link PearsScheduleMappingException}, which skips the licence rather than the run. A historical version this can't
 * map is left out of the licence's history and listed instead.
 */
public class PearsScheduleMapper {

  static final String NO_CURRENT_VERSION = "No current version in PEARS: the latest version is made active";
  static final String START_DATE_BORROWED = "Version with no start date in PEARS: another version's start date used";
  static final String STALE_DATES =
      "Version whose PEARS term dates are on or before its start date: its dates are worked out from the offsets";
  static final String EVENT_DATE_FROM_OFFSET = "Event with no date in PEARS: its date is worked out from its offset";
  static final String HISTORICAL_VERSION_LEFT_OUT = "Historical version that cannot be mapped: left out";
  static final String UNTYPED_EVENT_EMPTY = "Event with no type, empty: ignored";
  static final String UNTYPED_EVENT_WITH_DATA = "Event with no type, holding data: ignored";
  static final String UNNUMBERED_TERM = "Term event with no term number: ignored";
  static final String NO_EXPIRY = "No expiry event: migrated without an expiry";
  static final String EXPIRY_NOT_ON_FINAL_TERM_END = "Expiry not on the final term's end: migrated as it is";

  /**
   * The time zone PEARS saves version timestamps in.
   */
  static final ZoneId PEARS_ZONE = ZoneId.of("Europe/London");

  /**
   * The event types the migration maps by name. Any other type in the catalogue with a display sequence of 0 or more is
   * a user-defined type, which the migration maps as an "Other activity".
   */
  private static final Set<String> MAPPED_EVENT_TYPES = Set.of("T", "P", "E", "R", "MR", "BM", "RD", "PA");

  private static final String TERM = "T";
  private static final String PHASE = "P";
  private static final String EXPIRY = "E";
  private static final String RATE = "R";

  private static final Map<String, PhaseType> PHASES_BY_PEARS_NAME = Map.of(
      "A", PhaseType.PHASE_A,
      "B", PhaseType.PHASE_B,
      "C", PhaseType.PHASE_C
  );

  /**
   * Maps every version of the licence's schedule.
   *
   * @param schedule   the licence's schedule, as {@link PearsScheduleService#licenceSchedule} reads it
   * @param eventTypes the PEARS event type catalogue, as {@link PearsScheduleService#eventTypes} reads it
   * @throws PearsScheduleMappingException if the active version cannot be mapped, or the licence has no version or no
   *                                       start date at all, so the licence is skipped as a whole
   */
  public MappedSchedule map(PearsSchedule schedule, List<PearsScheduleEventType> eventTypes) {
    var licence = schedule.licence();
    var versions = schedule.versions();
    var notes = new MigrationNotes();

    if (versions.isEmpty()) {
      throw new PearsScheduleMappingException(licence, "No schedule versions in PEARS");
    }
    if (versions.stream().allMatch(version -> version.originDate() == null)) {
      throw new PearsScheduleMappingException(licence, "No schedule start date in any PEARS version");
    }

    var catalogue = new Catalogue(knownEventTypes(eventTypes), shortTitles(eventTypes));
    var activeVersion = activeVersion(schedule, notes);
    var eventKeys = new EventKeys();
    var mappedVersions = new ArrayList<MappedScheduleVersion>();
    var leftOutVersions = new ArrayList<MappedSchedule.LeftOutVersion>();

    for (var i = 0; i < versions.size(); i++) {
      var version = versions.get(i);
      var active = version == activeVersion;
      var versionNotes = new MigrationNotes();

      try {
        var startDate = startDate(licence, versions, i);
        mappedVersions.add(mapVersion(
            new VersionContext(licence, version, startDate.startDate(), startDate.borrowed(), versionNotes),
            active,
            catalogue,
            eventKeys
        ));
        addNotes(notes, versionNotes);
      } catch (PearsScheduleMappingException e) {
        if (active) {
          throw e;
        }
        notes.note(HISTORICAL_VERSION_LEFT_OUT);
        leftOutVersions.add(new MappedSchedule.LeftOutVersion(version.plsdId(), e.reason()));
      }
    }

    return new MappedSchedule(licence, mappedVersions, leftOutVersions, notes);
  }

  private static MappedScheduleVersion mapVersion(
      VersionContext context,
      boolean active,
      Catalogue catalogue,
      EventKeys eventKeys
  ) {
    if (context.startDateBorrowed()) {
      context.notes().note(START_DATE_BORROWED);
    } else if (context.datesFromOffsets()) {
      context.notes().note(STALE_DATES);
    }

    var events = typedEvents(context, catalogue.knownEventTypes());
    var keys = eventKeys.forVersion(context, events);
    var terms = terms(context, events, keys);
    var phases = phases(context, events, terms, keys);
    var expiry = expiry(context, events, terms.getLast(), keys);
    var rates = PearsRateMapper.rates(context, ofType(events, RATE), terms, phases, keys);
    var otherEvents = PearsOtherEventMapper.otherEvents(context, events, terms, catalogue.shortTitles(), keys);

    var mappedVersion = new MappedScheduleVersion(
        context.version().plsdId(),
        active ? LicenceScheduleDetailStatus.ACTIVE : LicenceScheduleDetailStatus.REPLACED,
        context.version().start().atZone(PEARS_ZONE).toInstant(),
        context.startDate(),
        context.startDateBorrowed(),
        context.datesFromOffsets(),
        terms,
        phases,
        expiry,
        rates,
        otherEvents
    );
    keys.versionMapped(mappedVersion);
    return mappedVersion;
  }

  private static void addNotes(MigrationNotes notes, MigrationNotes versionNotes) {
    versionNotes.noteCountsByReason().forEach((reason, count) -> {
      for (var i = 0; i < count; i++) {
        notes.note(reason);
      }
    });
  }

  /**
   * The current version, or the latest when every version is end-dated (P1161 on live).
   */
  private static PearsScheduleVersion activeVersion(PearsSchedule schedule, MigrationNotes notes) {
    Optional<PearsScheduleVersion> currentVersion;
    try {
      currentVersion = schedule.currentVersion();
    } catch (IllegalStateException e) {
      throw new PearsScheduleMappingException(schedule.licence(), "More than one current schedule version in PEARS");
    }

    return currentVersion.orElseGet(() -> {
      notes.note(NO_CURRENT_VERSION);
      return schedule.versions().getLast();
    });
  }

  /**
   * The version's own start date, or when it has none (an early version, saved before the licence was raised), the
   * start date of the next version that has one, then of the earliest.
   */
  private static VersionStartDate startDate(PearsLicenceReference licence, List<PearsScheduleVersion> versions, int index) {
    var ownStartDate = versions.get(index).originDate();
    if (ownStartDate != null) {
      return new VersionStartDate(ownStartDate, false);
    }

    return Stream.concat(versions.subList(index + 1, versions.size()).stream(), versions.stream())
        .map(PearsScheduleVersion::originDate)
        .filter(Objects::nonNull)
        .findFirst()
        .map(startDate -> new VersionStartDate(startDate, true))
        .orElseThrow(() -> new PearsScheduleMappingException(licence, "No schedule start date in any PEARS version"));
  }

  private static Set<String> knownEventTypes(List<PearsScheduleEventType> eventTypes) {
    return eventTypes.stream()
        .filter(eventType -> MAPPED_EVENT_TYPES.contains(eventType.mnem())
            || (eventType.displaySeq() != null && eventType.displaySeq() >= 0))
        .map(PearsScheduleEventType::mnem)
        .collect(Collectors.toSet());
  }

  private static Map<String, String> shortTitles(List<PearsScheduleEventType> eventTypes) {
    return eventTypes.stream()
        .filter(eventType -> StringUtils.isNotBlank(eventType.shortTitle()))
        .collect(Collectors.toMap(PearsScheduleEventType::mnem, PearsScheduleEventType::shortTitle, (first, second) -> first));
  }

  /**
   * The version's events that have a type, each checked to be a type the migration knows. Events with no type are
   * ignored, as the PEARS schedule screen ignores them, and only counted.
   */
  private static List<PearsScheduleEvent> typedEvents(VersionContext context, Set<String> knownEventTypes) {
    var typedEvents = new ArrayList<PearsScheduleEvent>();

    for (var event : context.version().events()) {
      if (StringUtils.isBlank(event.eventMnem())) {
        context.notes().note(holdsData(event) ? UNTYPED_EVENT_WITH_DATA : UNTYPED_EVENT_EMPTY);
      } else if (knownEventTypes.contains(event.eventMnem())) {
        typedEvents.add(event);
      } else {
        throw context.cannotMap("Event type '%s' is not known to the migration".formatted(event.eventMnem()));
      }
    }

    return typedEvents;
  }

  static boolean holdsData(PearsScheduleEvent event) {
    return event.area() != null || Stream.of(
        event.termNo(), event.phase(), event.recurring(), event.recurrances(), event.recurEndDate(), event.rate(),
        event.percentage(), event.comments(), event.eventSubtype(), event.eventId()
    ).anyMatch(StringUtils::isNotBlank);
  }

  /**
   * The terms with a numbered {@code T} event, in term order. The first starts on the schedule start date and each
   * ends the day before its {@code T} event's date, where the next term present starts. Terms may be missing (a
   * schedule with only the second and third, for example), but must be in date order.
   */
  private static List<MappedTerm> terms(
      VersionContext context,
      List<PearsScheduleEvent> events,
      EventKeys.VersionKeys keys
  ) {
    var termEvents = new TreeMap<Integer, PearsScheduleEvent>();

    for (var event : ofType(events, TERM)) {
      if (StringUtils.isBlank(event.termNo())) {
        context.notes().note(UNNUMBERED_TERM);
      } else {
        var termNo = termNo(context, event.termNo());
        if (termEvents.putIfAbsent(termNo, event) != null) {
          throw context.cannotMap("Term %d appears more than once".formatted(termNo));
        }
      }
    }

    if (termEvents.isEmpty()) {
      throw new PearsScheduleMappingException(context.licence(), PearsScheduleMappingException.NO_TERMS);
    }

    var terms = new ArrayList<MappedTerm>();
    var startDate = context.startDate();

    for (var termEvent : termEvents.entrySet()) {
      var termNo = termEvent.getKey();
      var event = termEvent.getValue();
      var boundary = context.eventDate(event)
          .orElseThrow(() -> context.cannotMap("Term %d has no date".formatted(termNo)));

      if (!boundary.isAfter(startDate)) {
        throw context.cannotMap("Term %d ends on %s, on or before it starts on %s".formatted(termNo, boundary, startDate));
      }

      var duration = LmsScheduleDurations.untilBoundary(startDate, boundary);
      var eventKey = keys.term(termNo);
      terms.add(new MappedTerm(
          eventKey,
          termNo,
          startDate,
          boundary.minusDays(1),
          duration,
          keys.comment(eventKey, event.comments()),
          event
      ));
      startDate = boundary;
    }

    return terms;
  }

  private static int termNo(VersionContext context, String pearsTermNo) {
    try {
      var termNo = Integer.parseInt(pearsTermNo.trim());
      if (termNo >= 1 && termNo <= 4) {
        return termNo;
      }
    } catch (NumberFormatException e) {
      // Reported below, as any other term number outside 1 to 4 is
    }
    throw context.cannotMap("Term number '%s' is not 1 to 4".formatted(pearsTermNo));
  }

  /**
   * The initial term's phases with a {@code P} event, in phase order, chained the same way as the terms from the
   * initial term's start. Phases may be missing (A and C with no B, for example), but must be in date order and end
   * with the initial term.
   */
  private static List<MappedPhase> phases(
      VersionContext context,
      List<PearsScheduleEvent> events,
      List<MappedTerm> terms,
      EventKeys.VersionKeys keys
  ) {
    var phaseEvents = new EnumMap<PhaseType, PearsScheduleEvent>(PhaseType.class);

    for (var event : ofType(events, PHASE)) {
      var phaseType = PHASES_BY_PEARS_NAME.get(StringUtils.trimToEmpty(event.phase()));
      if (phaseType == null) {
        throw context.cannotMap("Phase '%s' is not A, B or C".formatted(Objects.toString(event.phase(), "")));
      }
      if (phaseEvents.putIfAbsent(phaseType, event) != null) {
        throw context.cannotMap("%s appears more than once".formatted(phaseType.getDisplayName()));
      }
    }

    if (phaseEvents.isEmpty()) {
      return List.of();
    }

    var initialTerm = terms.stream()
        .filter(term -> term.termNo() == 1)
        .findFirst()
        .orElseThrow(() -> context.cannotMap("Phases with no initial term"));

    var phases = new ArrayList<MappedPhase>();
    var startDate = initialTerm.startDate();

    for (var phaseEvent : phaseEvents.entrySet()) {
      var phaseType = phaseEvent.getKey();
      var event = phaseEvent.getValue();
      var boundary = context.eventDate(event)
          .orElseThrow(() -> context.cannotMap("%s has no date".formatted(phaseType.getDisplayName())));

      if (!boundary.isAfter(startDate)) {
        throw context.cannotMap("%s ends on %s, on or before it starts on %s"
            .formatted(phaseType.getDisplayName(), boundary, startDate));
      }

      var duration = LmsScheduleDurations.untilBoundary(startDate, boundary);
      var eventKey = keys.phase(phaseType);
      phases.add(new MappedPhase(
          eventKey,
          phaseType,
          startDate,
          boundary.minusDays(1),
          duration,
          keys.comment(eventKey, event.comments()),
          event
      ));
      startDate = boundary;
    }

    if (!startDate.equals(initialTerm.boundary())) {
      throw context.cannotMap("The last phase ends on %s, not with the initial term on %s"
          .formatted(startDate, initialTerm.boundary()));
    }

    return phases;
  }

  /**
   * The expiry from the {@code E} event. A schedule without one is migrated without an expiry, and one whose date
   * isn't the final term's end is migrated as it is. Both are noted for the report.
   */
  @Nullable
  private static MappedExpiry expiry(
      VersionContext context,
      List<PearsScheduleEvent> events,
      MappedTerm finalTerm,
      EventKeys.VersionKeys keys
  ) {
    var expiryEvents = ofType(events, EXPIRY);

    if (expiryEvents.isEmpty()) {
      context.notes().note(NO_EXPIRY);
      return null;
    }
    if (expiryEvents.size() > 1) {
      throw context.cannotMap("More than one expiry event");
    }

    var event = expiryEvents.getFirst();
    var expiryDate = context.eventDate(event)
        .orElseThrow(() -> context.cannotMap("The expiry event has no date"));

    if (!expiryDate.isAfter(finalTerm.startDate())) {
      throw context.cannotMap("The expiry on %s is on or before the final term starts on %s"
          .formatted(expiryDate, finalTerm.startDate()));
    }
    if (!expiryDate.equals(finalTerm.boundary())) {
      context.notes().note(EXPIRY_NOT_ON_FINAL_TERM_END);
    }

    return new MappedExpiry(keys.expiry(), expiryDate, StringUtils.trimToNull(event.comments()), event);
  }

  private static List<PearsScheduleEvent> ofType(List<PearsScheduleEvent> events, String eventMnem) {
    return events.stream().filter(event -> eventMnem.equals(event.eventMnem())).toList();
  }

  private record VersionStartDate(LocalDate startDate, boolean borrowed) {
  }

  private record Catalogue(Set<String> knownEventTypes, Map<String, String> shortTitles) {
  }
}
