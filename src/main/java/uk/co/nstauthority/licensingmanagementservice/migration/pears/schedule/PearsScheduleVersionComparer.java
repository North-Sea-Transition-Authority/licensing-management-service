package uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule;

import jakarta.annotation.Nullable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDuration;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.areaevent.LicenceScheduleAreaEvent;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventreference.ScheduleEvent;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleexpiry.LicenceScheduleExpiry;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhase;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate.LicenceScheduleRate;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate.RateDefinitionOption;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate.RateRelativeDateOption;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent.OtherScheduleEvent;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent.OtherScheduleEventDateOption;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.PearsScheduleComparison.Result;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.PearsScheduleComparison.Row;

/**
 * Compares one PEARS schedule version with the LMS schedule detail migrated from it, a row per event. Events are
 * paired through the links the migration recorded, and what the mapper makes of the PEARS version says which events
 * were left out on purpose rather than lost.
 */
final class PearsScheduleVersionComparer {

  static final String UNTYPED = "No event type: ignored";
  static final String UNNUMBERED_TERM = "Term with no number: ignored";
  static final String LEFT_OUT_BY_RULES = "Left out by the migration's rules: see the run's notes";
  static final String ADDED_IN_LMS = "No PEARS event: added in LMS";
  static final String FROM_ANOTHER_VERSION = "From PEARS version %d, not this one";
  static final String NO_MATCHING_AREA_EVENT = "No PEARS area event holding the same";
  static final String START_DATE_BORROWED = "Another version's start date used";

  private static final Set<String> AREA_EVENT_TYPES = Set.of("RD", "PA");

  // Event names and captions used by both sides of a row
  private static final String TERM = "Term";
  private static final String PHASE = "Phase";
  private static final String RATE = "Rate";
  private static final String EXPIRY = "Expiry";
  private static final String LICENCE_EXPIRY = "Licence expiry";

  // Terms and phases first on a date, as a term that differs explains the events anchored to it
  private static final Map<String, Integer> EVENT_ORDER = Map.of("Start", 0, TERM, 1, PHASE, 2);

  private static final Comparator<Row> ROW_ORDER = Comparator
      .comparing(Row::sortingDate, Comparator.nullsLast(Comparator.naturalOrder()))
      .thenComparing(row -> EVENT_ORDER.getOrDefault(row.event(), EVENT_ORDER.size()))
      .thenComparing(Row::pearsIndex, Comparator.nullsLast(Comparator.naturalOrder()));

  @Nullable
  private final PearsScheduleVersion pears;
  @Nullable
  private final LmsScheduleVersion lms;
  @Nullable
  private final ExpectedEvents expected;
  private final Set<UUID> wrongOriginalEvents;

  private PearsScheduleVersionComparer(
      @Nullable PearsScheduleVersion pears,
      @Nullable LmsScheduleVersion lms,
      @Nullable ExpectedEvents expected,
      Set<UUID> wrongOriginalEvents
  ) {
    this.pears = pears;
    this.lms = lms;
    this.expected = expected;
    this.wrongOriginalEvents = wrongOriginalEvents;
  }

  /**
   * The rows for one pair. At least one side is present.
   *
   * @param expected            what the mapper makes of the PEARS version; null when it doesn't map it, because the
   *                            migration fails the licence or leaves the version out
   * @param wrongOriginalEvents LMS events whose {@code originalEventId} isn't that of the first event with their key
   */
  static List<Row> compare(
      @Nullable PearsScheduleVersion pears,
      @Nullable LmsScheduleVersion lms,
      @Nullable MappedScheduleVersion expected,
      Set<UUID> wrongOriginalEvents
  ) {
    return new PearsScheduleVersionComparer(
        pears, lms, expected == null ? null : ExpectedEvents.of(expected), wrongOriginalEvents).rows();
  }

  /**
   * The LMS events migrated from a PEARS version, by the index of the PEARS event each came from, in date order: a
   * yearly rate carried on into later terms has one LMS rate per term.
   */
  static Map<Integer, List<ScheduleEvent>> linkedEvents(LmsScheduleVersion lms, long plsdId) {
    return lms.linkableEvents().stream()
        .filter(event -> {
          var link = lms.links().get(event.getId());
          return link != null && link.getPearsPlsdId() == plsdId;
        })
        .sorted(Comparator.comparing(PearsScheduleVersionComparer::lmsDate, Comparator.nullsLast(Comparator.naturalOrder())))
        .collect(Collectors.groupingBy(event -> lms.links().get(event.getId()).getPearsEventIndex()));
  }

  private List<Row> rows() {
    var rows = new ArrayList<Row>();
    rows.add(startRow());

    var shown = new HashSet<UUID>();
    if (pears != null) {
      var linked = lms == null ? Map.<Integer, List<ScheduleEvent>>of() : linkedEvents(lms, pears.plsdId());
      pears.events().forEach(event -> rows.addAll(pearsEventRows(event, linked.getOrDefault(event.index(), List.of()))));
      linked.values().forEach(events -> events.forEach(event -> shown.add(event.getId())));
    }
    if (lms != null) {
      lms.linkableEvents().stream()
          .filter(event -> !shown.contains(event.getId()))
          .forEach(event -> rows.add(lmsOnlyRow(event)));
    }
    rows.addAll(areaEventRows());

    var pearsHasExpiry = pears != null && pears.events().stream().anyMatch(event -> "E".equals(mnem(event)));
    if (pears != null && !pearsHasExpiry && (lms == null || lms.expiry() == null)) {
      rows.add(new Row(EXPIRY, LICENCE_EXPIRY, null, null, null, null, null, null, null, Result.NO_EXPIRY, null));
    }

    rows.sort(ROW_ORDER);
    return rows;
  }

  private Row startRow() {
    var pearsStart = pears == null ? null : pears.originDate();
    var lmsStart = lms == null ? null : lms.startDate();
    Result result;
    String detail = null;

    if (pears == null) {
      result = Result.LMS_ONLY;
    } else if (lms == null) {
      result = missingResult();
    } else if (pearsStart == null) {
      result = Result.NO_START_DATE;
      detail = START_DATE_BORROWED;
    } else {
      result = pearsStart.equals(lmsStart) ? Result.MATCH : Result.DIFFERS;
    }
    return new Row("Start", "Schedule start", null, null, pearsStart, null, "Schedule start date", lmsStart, null,
        result, detail);
  }

  private List<Row> pearsEventRows(PearsScheduleEvent event, List<ScheduleEvent> lmsEvents) {
    var mnem = mnem(event);
    if (mnem == null) {
      return PearsScheduleMapper.holdsData(event) ? List.of(pearsOnlyRow(event, Result.NOT_MIGRATED, UNTYPED)) : List.of();
    }
    if (AREA_EVENT_TYPES.contains(mnem)) {
      return List.of();
    }

    var expectedEvents = expected == null ? List.<ExpectedEvent>of() : expected.byIndex().getOrDefault(event.index(), List.of());
    if (lmsEvents.isEmpty()) {
      return List.of(expectedEvents.isEmpty()
          ? pearsOnlyRow(event, Result.NOT_MIGRATED, notMigratedReason(mnem, event))
          : pearsOnlyRow(event, Result.MISSING_IN_LMS, null));
    }

    var rows = new ArrayList<Row>();
    for (var i = 0; i < lmsEvents.size(); i++) {
      var expectedEvent = i < expectedEvents.size() ? expectedEvents.get(i) : null;
      rows.add(i == 0
          ? pairedRow(event, lmsEvents.get(i), expectedEvent)
          : carriedOnRow(event, lmsEvents.get(i), expectedEvent));
    }
    return rows;
  }

  private Row pairedRow(PearsScheduleEvent event, ScheduleEvent lmsEvent, @Nullable ExpectedEvent expectedEvent) {
    var lmsDate = lmsDate(lmsEvent);
    var lmsValue = lmsValue(lmsEvent);
    var result = dateResult(event.eventDate(), lmsDate, expectedEvent);
    if (result != Result.DIFFERS && expectedEvent != null && !sameValue(expectedEvent.value(), lmsValue)) {
      result = Result.VALUE_DIFFERS;
    }
    if (wrongOriginalEvents.contains(lmsEvent.getId())) {
      result = Result.ORIGINAL_EVENT_DIFFERS;
    }
    return new Row(eventName(lmsEvent), caption(lmsEvent), event.index(), offset(event), event.eventDate(),
        pearsValue(event), basis(lmsEvent), lmsDate, text(lmsValue), result, detail(lmsEvent),
        comparedWith(event.eventDate(), expectedEvent));
  }

  /**
   * A further LMS rate from one PEARS rate: the yearly rate carried on into a later term (rate option A).
   */
  private Row carriedOnRow(PearsScheduleEvent event, ScheduleEvent lmsEvent, @Nullable ExpectedEvent expectedEvent) {
    var lmsDate = lmsDate(lmsEvent);
    var lmsValue = lmsValue(lmsEvent);
    var result = Result.CARRIED_ON;
    if (expectedEvent != null && !Objects.equals(expectedEvent.date(), lmsDate)) {
      result = Result.DIFFERS;
    } else if (expectedEvent != null && !sameValue(expectedEvent.value(), lmsValue)) {
      result = Result.VALUE_DIFFERS;
    }
    if (wrongOriginalEvents.contains(lmsEvent.getId())) {
      result = Result.ORIGINAL_EVENT_DIFFERS;
    }
    return new Row(eventName(lmsEvent), caption(lmsEvent), event.index(), null, null, null, basis(lmsEvent), lmsDate,
        text(lmsValue), result, "Carried on from PEARS event %d".formatted(event.index()),
        expectedEvent == null ? null : expectedEvent.date());
  }

  /**
   * An LMS event with no PEARS event in this version: added in LMS, or migrated from another PEARS version, as when
   * PEARS has saved a version since the migration.
   */
  private Row lmsOnlyRow(ScheduleEvent lmsEvent) {
    var lmsValue = lmsValue(lmsEvent);
    var link = lms == null ? null : lms.links().get(lmsEvent.getId());
    var detail = link == null ? ADDED_IN_LMS : FROM_ANOTHER_VERSION.formatted(link.getPearsPlsdId());
    return new Row(eventName(lmsEvent), caption(lmsEvent), null, null, null, null, basis(lmsEvent), lmsDate(lmsEvent),
        text(lmsValue), Result.LMS_ONLY, detail);
  }

  private Row pearsOnlyRow(PearsScheduleEvent event, Result result, @Nullable String detail) {
    return new Row(pearsEventName(mnem(event)), pearsCaption(event), event.index(), offset(event), event.eventDate(),
        pearsValue(event), null, null, null, result, detail);
  }

  /**
   * PEARS's {@code RD} and {@code PA} events against the LMS area events, which have no links: each is paired with an
   * LMS area event holding exactly what it holds.
   */
  private List<Row> areaEventRows() {
    var unmatched = new HashMap<AreaEvent, LinkedList<LicenceScheduleAreaEvent>>();
    if (lms != null) {
      lms.areaEvents().forEach(areaEvent ->
          unmatched.computeIfAbsent(AreaEvent.of(areaEvent), key -> new LinkedList<>()).add(areaEvent));
    }

    var rows = new ArrayList<Row>();
    if (pears != null) {
      pears.events().stream()
          .filter(PearsScheduleVersionComparer::isAreaEvent)
          .forEach(event -> rows.add(pearsAreaEventRow(event, unmatched)));
    }
    unmatched.values().stream().flatMap(List::stream).forEach(areaEvent -> rows.add(new Row(
        "Area event",
        areaCaption(areaEvent.getEventMnem(), new PearsScheduleEvent.Area(areaEvent.getAreaName(),
            areaEvent.getAreaShortName(), areaEvent.getAreaType(), areaEvent.getAreaVersion())),
        null, null, null, null, null, areaEvent.getEventDate(), null, Result.LMS_ONLY, lmsOnlyAreaEventDetail())));
    return rows;
  }

  /**
   * A PEARS area event, paired with the first unpaired LMS area event holding exactly what it holds, which it takes.
   */
  private Row pearsAreaEventRow(
      PearsScheduleEvent event,
      Map<AreaEvent, LinkedList<LicenceScheduleAreaEvent>> unmatched
  ) {
    var candidates = unmatched.get(AreaEvent.of(event));
    var match = candidates == null ? null : candidates.poll();
    return new Row("Area event", areaCaption(mnem(event), event.area()), event.index(), offset(event),
        event.eventDate(), null, null, match == null ? null : match.getEventDate(), null, areaEventResult(match),
        StringUtils.trimToNull(event.comments()));
  }

  private Result areaEventResult(@Nullable LicenceScheduleAreaEvent match) {
    if (match != null) {
      return Result.MATCH;
    }
    return lms == null ? missingResult() : Result.MISSING_IN_LMS;
  }

  /**
   * LMS can't add an area event, so one with no PEARS counterpart came from the PEARS version the detail was migrated
   * from, when that's another.
   */
  private String lmsOnlyAreaEventDetail() {
    var migratedFrom = lms == null ? null : lms.pearsPlsdId().orElse(null);
    if (migratedFrom == null || (pears != null && migratedFrom == pears.plsdId())) {
      return NO_MATCHING_AREA_EVENT;
    }
    return FROM_ANOTHER_VERSION.formatted(migratedFrom);
  }

  private Result dateResult(@Nullable LocalDate pearsDate, @Nullable LocalDate lmsDate, @Nullable ExpectedEvent expectedEvent) {
    if (lmsDate != null && lmsDate.equals(pearsDate)) {
      return Result.MATCH;
    }
    if (isDatedFromOffset(pearsDate) && expectedEvent != null && Objects.equals(expectedEvent.date(), lmsDate)) {
      return Result.FROM_OFFSET;
    }
    return Result.DIFFERS;
  }

  /**
   * The date an LMS date is compared with: PEARS's, or what the migration works out from the offset where PEARS's isn't
   * used.
   */
  @Nullable
  private LocalDate comparedWith(@Nullable LocalDate pearsDate, @Nullable ExpectedEvent expectedEvent) {
    return isDatedFromOffset(pearsDate) && expectedEvent != null ? expectedEvent.date() : pearsDate;
  }

  /**
   * Whether the migration dates the event from its offset: PEARS holds no date for it, or the whole version's dates are
   * from PEARS's 1900 base.
   */
  private boolean isDatedFromOffset(@Nullable LocalDate pearsDate) {
    return pearsDate == null || (expected != null && expected.datesFromOffsets());
  }

  /**
   * For a PEARS version with no LMS detail: lost if the migration maps it, otherwise left out on purpose.
   */
  private Result missingResult() {
    return expected == null ? Result.NOT_MIGRATED : Result.MISSING_IN_LMS;
  }

  @Nullable
  private String notMigratedReason(String mnem, PearsScheduleEvent event) {
    if ("T".equals(mnem) && StringUtils.isBlank(event.termNo())) {
      return UNNUMBERED_TERM;
    }
    return expected == null ? null : LEFT_OUT_BY_RULES;
  }

  @Nullable
  static LocalDate lmsDate(ScheduleEvent event) {
    return switch (event) {
      case LicenceScheduleTerm term -> dayAfter(term.getEndDate());
      case LicenceSchedulePhase phase -> dayAfter(phase.getEndDate());
      case LicenceScheduleRate rate -> rateStart(rate);
      case OtherScheduleEvent otherEvent -> otherEvent.getEventDate();
      case LicenceScheduleExpiry expiry -> dayAfter(expiry.getExpiryDate());
      default -> null;
    };
  }

  /**
   * A term or phase rate holds no start date of its own: it starts with its period.
   */
  @Nullable
  private static LocalDate rateStart(LicenceScheduleRate rate) {
    if (rate.getStartDate() != null) {
      return rate.getStartDate();
    }
    if (rate.getLicenceSchedulePhase() != null) {
      return rate.getLicenceSchedulePhase().getStartDate();
    }
    return rate.getLicenceScheduleTerm() == null ? null : rate.getLicenceScheduleTerm().getStartDate();
  }

  @Nullable
  private static LocalDate dayAfter(@Nullable LocalDate date) {
    return date == null ? null : date.plusDays(1);
  }

  @Nullable
  private static BigDecimal lmsValue(ScheduleEvent event) {
    if (event instanceof LicenceScheduleRate rate) {
      return rate.getRentalRate();
    }
    return event instanceof OtherScheduleEvent otherEvent ? otherEvent.getRelinquishmentPercentage() : null;
  }

  private static boolean sameValue(@Nullable BigDecimal expectedValue, @Nullable BigDecimal lmsValue) {
    return expectedValue == null || lmsValue == null
        ? expectedValue == lmsValue
        : expectedValue.compareTo(lmsValue) == 0;
  }

  private static String eventName(ScheduleEvent event) {
    return switch (event) {
      case LicenceScheduleTerm ignored -> TERM;
      case LicenceSchedulePhase ignored -> PHASE;
      case LicenceScheduleRate ignored -> RATE;
      case OtherScheduleEvent otherEvent -> otherEvent.getCategory().getDisplayName();
      case LicenceScheduleExpiry ignored -> EXPIRY;
      default -> event.getClass().getSimpleName();
    };
  }

  private static String caption(ScheduleEvent event) {
    return switch (event) {
      case LicenceScheduleTerm term -> "%s ends".formatted(term.getTermType().getDisplayName());
      case LicenceSchedulePhase phase -> "%s ends".formatted(phase.getPhaseType().getDisplayName());
      case LicenceScheduleRate rate -> "In %s".formatted(rate.getLicenceSchedulePhase() != null
          ? rate.getLicenceSchedulePhase().getPhaseType().getDisplayName()
          : periodName(rate.getLicenceScheduleTerm()));
      case OtherScheduleEvent otherEvent -> otherEvent.getOtherCategoryName() != null
          ? otherEvent.getOtherCategoryName()
          : otherEvent.getCategory().getDisplayName();
      case LicenceScheduleExpiry ignored -> LICENCE_EXPIRY;
      default -> "";
    };
  }

  @Nullable
  private static String detail(ScheduleEvent event) {
    if (event instanceof OtherScheduleEvent otherEvent) {
      return otherEvent.getDescription();
    }
    return event instanceof LicenceScheduleExpiry expiry ? expiry.getComments() : null;
  }

  /**
   * What LMS dates the event from, as it stores it.
   */
  private static String basis(ScheduleEvent event) {
    return switch (event) {
      case LicenceScheduleTerm term -> "%s, %s".formatted(term.getTermType().getDisplayName(),
          duration(term.getTermDuration()));
      case LicenceSchedulePhase phase -> "%s, %s".formatted(phase.getPhaseType().getDisplayName(),
          duration(phase.getPhaseDuration()));
      case LicenceScheduleRate rate -> rateBasis(rate);
      case OtherScheduleEvent otherEvent -> otherEvent.getDateOption() == OtherScheduleEventDateOption.RELATIVE_DATE
          ? "%s start + %s".formatted(otherEventAnchor(otherEvent), duration(otherEvent.getRelativeDuration()))
          : "Its own date";
      case LicenceScheduleExpiry ignored -> "Expiry date, held as the day before";
      default -> "";
    };
  }

  private static String rateBasis(LicenceScheduleRate rate) {
    var anchor = rate.getLicenceSchedulePhase() != null
        ? rate.getLicenceSchedulePhase().getPhaseType().getDisplayName()
        : periodName(rate.getLicenceScheduleTerm());
    if (rate.getRateDefinitionOption() != RateDefinitionOption.CUSTOM_PERIOD
        || rate.getRateRelativeDateOption() != RateRelativeDateOption.RELATIVE_TO_START_DATE) {
      return "%s start".formatted(anchor);
    }
    return "%s start + %s".formatted(anchor, duration(rate.getRelativeDuration()));
  }

  private static String otherEventAnchor(OtherScheduleEvent otherEvent) {
    return otherEvent.getLicenceSchedulePhase() != null
        ? otherEvent.getLicenceSchedulePhase().getPhaseType().getDisplayName()
        : periodName(otherEvent.getLicenceScheduleTerm());
  }

  private static String periodName(@Nullable LicenceScheduleTerm term) {
    return term == null ? "No term" : term.getTermType().getDisplayName();
  }

  static String duration(@Nullable ThreeFieldDuration duration) {
    if (duration == null) {
      return "no offset";
    }
    return "%dy %dm %dd".formatted(zeroIfNull(duration.years()), zeroIfNull(duration.months()),
        zeroIfNull(duration.days()));
  }

  private static int zeroIfNull(@Nullable Integer value) {
    return value == null ? 0 : value;
  }

  @Nullable
  private static String mnem(PearsScheduleEvent event) {
    return StringUtils.trimToNull(event.eventMnem());
  }

  private static boolean isAreaEvent(PearsScheduleEvent event) {
    var mnem = mnem(event);
    return mnem != null && AREA_EVENT_TYPES.contains(mnem);
  }

  private static String pearsEventName(@Nullable String mnem) {
    if (mnem == null) {
      return "Untyped";
    }
    return switch (mnem) {
      case "T" -> TERM;
      case "P" -> PHASE;
      case "R" -> RATE;
      case "E" -> EXPIRY;
      case "MR" -> "Mandatory relinquishment";
      case "BM" -> "Bespoke milestone";
      default -> "Other activity";
    };
  }

  private static String pearsCaption(PearsScheduleEvent event) {
    var mnem = mnem(event);
    if (mnem == null) {
      return "";
    }
    return switch (mnem) {
      case "T" -> StringUtils.isBlank(event.termNo()) ? "Term ends" : "Term %s ends".formatted(event.termNo().trim());
      case "P" -> "Phase %s ends".formatted(StringUtils.trimToEmpty(event.phase()));
      case "R" -> RATE;
      case "E" -> LICENCE_EXPIRY;
      default -> mnem;
    };
  }

  @Nullable
  private static String offset(PearsScheduleEvent event) {
    if (Stream.of(event.years(), event.months(), event.days()).allMatch(StringUtils::isBlank)) {
      return null;
    }
    return "%sy %sm %sd".formatted(number(event.years()), number(event.months()), number(event.days()));
  }

  private static String number(@Nullable String value) {
    return StringUtils.defaultIfBlank(StringUtils.trim(value), "0");
  }

  /**
   * The rate or percentage, with the recurrence PEARS holds, for example {@code 1200 YEARLY}.
   */
  @Nullable
  private static String pearsValue(PearsScheduleEvent event) {
    var value = StringUtils.isNotBlank(event.rate()) ? event.rate() : event.percentage();
    var text = Stream.of(value, event.recurring(), event.recurrances())
        .map(StringUtils::trimToNull)
        .filter(Objects::nonNull)
        .collect(Collectors.joining(" "));
    return StringUtils.trimToNull(text);
  }

  @Nullable
  private static String text(@Nullable BigDecimal value) {
    return value == null ? null : value.stripTrailingZeros().toPlainString();
  }

  /**
   * An area's caption as the timeline heads it: its name and short name, with its version from the second on, or what
   * kind of area it is when PEARS holds no name.
   */
  private static String areaCaption(@Nullable String mnem, @Nullable PearsScheduleEvent.Area area) {
    if (area == null || StringUtils.isBlank(area.name())) {
      return "PA".equals(StringUtils.trim(mnem)) ? "Pre-producing area" : "Retention or development area";
    }
    var version = StringUtils.trimToEmpty(area.version());
    var suffix = version.isEmpty() || "1".equals(version) ? "" : "v" + version;
    return "%s (%s%s)".formatted(area.name().trim(), StringUtils.trimToEmpty(area.shortName()), suffix);
  }

  /**
   * Everything an area event holds, so a PEARS event and the LMS event written from it are equal.
   */
  private record AreaEvent(
      @Nullable String mnem,
      @Nullable String subtype,
      @Nullable LocalDate date,
      @Nullable String comments,
      @Nullable String eventId,
      @Nullable PearsScheduleEvent.Area area
  ) {

    static AreaEvent of(PearsScheduleEvent event) {
      return new AreaEvent(event.eventMnem(), event.eventSubtype(), event.eventDate(), event.comments(),
          event.eventId(), event.area());
    }

    static AreaEvent of(LicenceScheduleAreaEvent event) {
      return new AreaEvent(event.getEventMnem(), event.getSubtype(), event.getEventDate(), event.getComments(),
          event.getPearsEventId(), PearsScheduleEvent.Area.of(event.getAreaName(), event.getAreaShortName(),
          event.getAreaType(), event.getAreaVersion()));
    }
  }

  /**
   * What the mapper makes of a PEARS version, by the index of the PEARS event each LMS event comes from.
   */
  record ExpectedEvents(boolean datesFromOffsets, Map<Integer, List<ExpectedEvent>> byIndex) {

    static ExpectedEvents of(MappedScheduleVersion version) {
      var byIndex = new HashMap<Integer, List<ExpectedEvent>>();
      version.terms().forEach(term -> add(byIndex, term.source(), term.eventKey(), term.boundary(), null));
      version.phases().forEach(phase -> add(byIndex, phase.source(), phase.eventKey(), phase.boundary(), null));
      version.rates().forEach(rate -> add(byIndex, rate.source(), rate.eventKey(), rate.startDate(), rate.rentalRate()));
      version.otherEvents().forEach(otherEvent -> add(byIndex, otherEvent.source(), otherEvent.eventKey(),
          otherEvent.date(), otherEvent.relinquishmentPercentage()));
      if (version.expiry() != null) {
        add(byIndex, version.expiry().source(), version.expiry().eventKey(), version.expiry().pearsDate(), null);
      }
      byIndex.values().forEach(events -> events.sort(Comparator.comparing(ExpectedEvent::date)));
      return new ExpectedEvents(version.datesFromOffsets(), byIndex);
    }

    private static void add(
        Map<Integer, List<ExpectedEvent>> byIndex,
        PearsScheduleEvent source,
        int eventKey,
        LocalDate date,
        @Nullable BigDecimal value
    ) {
      byIndex.computeIfAbsent(source.index(), index -> new ArrayList<>()).add(new ExpectedEvent(eventKey, date, value));
    }
  }

  record ExpectedEvent(int eventKey, LocalDate date, @Nullable BigDecimal value) {
  }
}
