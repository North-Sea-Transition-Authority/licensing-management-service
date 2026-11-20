package uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule;

import jakarta.annotation.Nullable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import uk.co.nstauthority.licensingmanagementservice.licence.PhaseType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate.RateDefinitionOption;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate.RateRelativeDateOption;

/**
 * Maps a version's PEARS rates ({@code R} events) to LMS rates, following the plan's "Rates and yearly recurrence".
 *
 * <p>PEARS can repeat a rate every year ({@code RECURRING=YEARLY}), for ever or a set number of times
 * ({@code RECURRANCES}). LMS has no recurrence: a rate is in force until the next one starts. So:
 * <ol>
 *   <li>Each PEARS rate is in force from its date until the next rate's date or the end of the final term. Where PEARS
 *       stops it earlier (a rate that doesn't recur, or recurs a set number of times), the years before the next rate
 *       have no rate in PEARS. Those gaps are taken to be errors (licensing item L12): the rate is carried on through
 *       them, and each gap is noted.</li>
 *   <li>Its yearly anniversaries are placed the way PEARS places recurrences, at the offset with the years raised by
 *       one each time ({@code PED012X}).</li>
 *   <li>It becomes one LMS rate in each term or phase it is in force in (Open question 7, option A): on its own date in
 *       the first, then on its first anniversary in each later one. So a written-back rate keeps PEARS's anniversaries.
 *       </li>
 * </ol>
 *
 * <p>Rates whose value can't be used, or that fall outside the terms, are left out and noted.
 */
final class PearsRateMapper {

  static final String RATE_NOT_A_VALUE = "Rate that is empty, not a number, or negative: not migrated";
  static final String RATE_ROUNDED = "Rate with more than 2 decimal places: rounded to the nearest penny";
  static final String RATE_OUTSIDE_TERMS = "Rate outside the schedule's terms: not migrated";
  static final String RATE_GAP_CARRIED_ON = "Years with no rate in PEARS: the previous rate is carried on through them";
  static final String COUNTED_RATE_RUNS_INTO_NEXT =
      "Counted yearly rate that runs into the next rate: it stops when the next rate starts";
  static final String INDEFINITE_RATE_NOT_LAST =
      "Yearly rate with no end, followed by another rate: it stops when the next rate starts";

  private static final String YEARLY = "YEARLY";

  /**
   * A rate PEARS can hold: digits with an optional decimal point, including PEARS's leading-dot values such as
   * {@code .5}. No sign, exponent, thousands separator or currency symbol (plan, "Rate values").
   */
  private static final Pattern RATE_VALUE = Pattern.compile("\\d+(\\.\\d*)?|\\.\\d+");

  private PearsRateMapper() {
  }

  /**
   * The version's LMS rates, in date order.
   *
   * @param rateEvents the version's {@code R} events
   * @param terms      the version's terms, in term order
   * @param phases     the initial term's phases, in phase order
   */
  static List<MappedRate> rates(
      VersionContext context,
      List<PearsScheduleEvent> rateEvents,
      List<MappedTerm> terms,
      List<MappedPhase> phases,
      EventKeys.VersionKeys keys
  ) {
    var periods = periods(terms, phases);
    var scheduleStart = terms.getFirst().startDate();
    var scheduleEnd = terms.getLast().boundary();
    var pearsRates = pearsRates(context, rateEvents, scheduleStart, scheduleEnd);

    var placedRates = new ArrayList<PlacedRate>();

    for (var i = 0; i < pearsRates.size(); i++) {
      var pearsRate = pearsRates.get(i);
      var nextRate = i + 1 < pearsRates.size() ? pearsRates.get(i + 1) : null;
      var inForceUntil = nextRate != null ? nextRate.date() : scheduleEnd;

      noteWherePearsStopsIt(context, pearsRate, nextRate, inForceUntil);
      placedRates.addAll(placed(pearsRate, inForceUntil, periods));
    }

    var rates = new ArrayList<MappedRate>();
    for (var i = 0; i < placedRates.size(); i++) {
      var nextStartDate = i + 1 < placedRates.size() ? placedRates.get(i + 1).date() : null;
      rates.add(anchored(placedRates.get(i), nextStartDate, scheduleEnd, keys));
    }
    return rates;
  }

  /**
   * The rate's first anniversary in each term or phase it is in force in, before {@code inForceUntil}: one LMS rate
   * each.
   */
  private static List<PlacedRate> placed(PearsRate pearsRate, LocalDate inForceUntil, List<RatePeriod> periods) {
    var placedRates = new ArrayList<PlacedRate>();
    RatePeriod previousPeriod = null;

    for (var year = 0; pearsRate.anniversary(year).isBefore(inForceUntil); year++) {
      var date = pearsRate.anniversary(year);
      var period = periodOf(periods, date);
      if (!period.equals(previousPeriod)) {
        placedRates.add(new PlacedRate(date, pearsRate, period, previousPeriod == null));
        previousPeriod = period;
      }
    }

    return placedRates;
  }

  /**
   * The terms and phases a rate can be anchored to, in date order: the initial term's phases in place of the initial
   * term when it has them, and every other term.
   */
  private static List<RatePeriod> periods(List<MappedTerm> terms, List<MappedPhase> phases) {
    var periods = new ArrayList<RatePeriod>();

    for (var term : terms) {
      if (term.termNo() == 1 && !phases.isEmpty()) {
        phases.forEach(phase ->
            periods.add(new RatePeriod(1, phase.phaseType(), phase.startDate(), phase.boundary())));
      } else {
        periods.add(new RatePeriod(term.termNo(), null, term.startDate(), term.boundary()));
      }
    }

    return periods;
  }

  private static RatePeriod periodOf(List<RatePeriod> periods, LocalDate date) {
    return periods.stream()
        .filter(period -> !date.isBefore(period.startDate()) && date.isBefore(period.boundary()))
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("No term or phase holds %s".formatted(date)));
  }

  /**
   * The version's PEARS rates that can be migrated, in date order. Every {@code R} event is checked for data errors
   * (its recurrence, a missing date, two on one date) before any is left out for its value or date, so a left-out rate
   * can't hide one.
   */
  private static List<PearsRate> pearsRates(
      VersionContext context,
      List<PearsScheduleEvent> rateEvents,
      LocalDate scheduleStart,
      LocalDate scheduleEnd
  ) {
    var pearsRates = new ArrayList<PearsRate>();
    var rateDates = new HashSet<LocalDate>();

    for (var event : rateEvents) {
      var recursYearly = recursYearly(context, event);
      var recurrenceCount = recurrenceCount(context, event);
      var date = context.eventDate(event).orElseThrow(() -> context.cannotMap(noDateReason(event)));

      if (!rateDates.add(date)) {
        throw context.cannotMap("Two rates start on %s".formatted(date));
      }

      var rentalRate = rentalRate(context, event.rate());
      if (rentalRate.isPresent() && withinTerms(context, date, scheduleStart, scheduleEnd)) {
        pearsRates.add(new PearsRate(
            date,
            rentalRate.get(),
            recursYearly,
            recurrenceCount,
            firstAnniversaryAtOffset(context, event, date),
            context.startDate(),
            event
        ));
      }
    }

    pearsRates.sort(Comparator.comparing(PearsRate::date));
    return pearsRates;
  }

  private static boolean withinTerms(
      VersionContext context,
      LocalDate date,
      LocalDate scheduleStart,
      LocalDate scheduleEnd
  ) {
    if (date.isBefore(scheduleStart) || !date.isBefore(scheduleEnd)) {
      context.notes().note(RATE_OUTSIDE_TERMS);
      return false;
    }
    return true;
  }

  private static String noDateReason(PearsScheduleEvent event) {
    return StringUtils.isBlank(event.rate())
        ? "A rate has no date"
        : "A rate of %s has no date".formatted(event.rate().trim());
  }

  /**
   * The rate as LMS holds it: a number of 0 or more, to at most 2 decimal places. Whole numbers keep no decimal places
   * and anything with pence keeps 2 ({@code .5} is 0.50). Empty, and noted, when PEARS's value can't be used.
   */
  private static Optional<BigDecimal> rentalRate(VersionContext context, @Nullable String pearsRate) {
    var trimmed = StringUtils.trimToEmpty(pearsRate);

    if (!RATE_VALUE.matcher(trimmed).matches()) {
      context.notes().note(RATE_NOT_A_VALUE);
      return Optional.empty();
    }

    var rentalRate = new BigDecimal(trimmed);

    if (rentalRate.scale() > 2) {
      context.notes().note(RATE_ROUNDED);
      return Optional.of(rentalRate.setScale(2, RoundingMode.HALF_UP));
    }
    return Optional.of(rentalRate.scale() == 1 ? rentalRate.setScale(2, RoundingMode.UNNECESSARY) : rentalRate);
  }

  private static boolean recursYearly(VersionContext context, PearsScheduleEvent event) {
    var recurring = StringUtils.trimToNull(event.recurring());
    if (recurring == null) {
      return false;
    }
    if (YEARLY.equals(recurring.toUpperCase(Locale.ROOT))) {
      return true;
    }
    throw context.cannotMap("Rate recurrence '%s' is not YEARLY".formatted(recurring));
  }

  /**
   * How many more times a yearly rate recurs after its first year, or null when it recurs for ever (or doesn't recur).
   */
  @Nullable
  private static Integer recurrenceCount(VersionContext context, PearsScheduleEvent event) {
    var recurrances = StringUtils.trimToNull(event.recurrances());
    if (recurrances == null) {
      return null;
    }

    try {
      var count = Integer.parseInt(recurrances);
      if (count >= 0) {
        return count;
      }
    } catch (NumberFormatException e) {
      // Reported below, as a negative count is
    }
    throw context.cannotMap("Rate recurrence count '%s' is not a whole number".formatted(recurrances));
  }

  /**
   * The rate's offset, when its anniversaries can be placed from it the way PEARS places them: the rate's date is the
   * one its offset gives. Otherwise null, and its anniversaries are a year apart from its date.
   */
  @Nullable
  private static VersionContext.Offset firstAnniversaryAtOffset(
      VersionContext context,
      PearsScheduleEvent event,
      LocalDate date
  ) {
    return VersionContext.offset(event)
        .filter(offset -> offset.dateFrom(context.startDate(), 0).equals(date))
        .orElse(null);
  }

  /**
   * Notes the years PEARS has no rate for, where this rate stops before {@code inForceUntil}, and a counted or endless
   * yearly rate that the next rate cuts short.
   */
  private static void noteWherePearsStopsIt(
      VersionContext context,
      PearsRate pearsRate,
      @Nullable PearsRate nextRate,
      LocalDate inForceUntil
  ) {
    if (pearsRate.recursYearly() && pearsRate.recurrenceCount() == null) {
      if (nextRate != null) {
        context.notes().note(INDEFINITE_RATE_NOT_LAST);
      }
      return;
    }

    var lastYear = pearsRate.recursYearly() ? pearsRate.recurrenceCount() : 0;

    if (pearsRate.anniversary(lastYear + 1).isBefore(inForceUntil)) {
      context.notes().note(RATE_GAP_CARRIED_ON);
    } else if (nextRate != null && !pearsRate.anniversary(lastYear).isBefore(inForceUntil)) {
      context.notes().note(COUNTED_RATE_RUNS_INTO_NEXT);
    }
  }

  /**
   * The rate anchored to its term or phase the way LMS anchors rates. A {@code TERM} or {@code PHASE} rate starts and
   * ends with its period ({@code calculateRateEndDatesForDisplay}), so it is used only when it fits exactly: the rate
   * starts on the period's start and the next rate (or the schedule's end) comes on the period's end. Anything else is
   * a {@code CUSTOM_PERIOD} rate, which LMS keeps in force until the next rate.
   */
  private static MappedRate anchored(
      PlacedRate placedRate,
      @Nullable LocalDate nextStartDate,
      LocalDate scheduleEnd,
      EventKeys.VersionKeys keys
  ) {
    var period = placedRate.period();
    var date = placedRate.date();
    var onPeriodStart = date.equals(period.startDate());
    var endsWithPeriod = period.boundary().equals(nextStartDate != null ? nextStartDate : scheduleEnd);

    RateDefinitionOption definitionOption;
    RateRelativeDateOption relativeDateOption = null;
    var relativeDuration = LmsScheduleDurations.untilDate(period.startDate(), date);

    if (onPeriodStart && endsWithPeriod) {
      definitionOption = period.phaseType() != null ? RateDefinitionOption.PHASE : RateDefinitionOption.TERM;
      relativeDuration = null;
    } else {
      definitionOption = RateDefinitionOption.CUSTOM_PERIOD;
      if (onPeriodStart) {
        relativeDateOption = RateRelativeDateOption.ON_START_DATE;
        relativeDuration = null;
      } else {
        relativeDateOption = RateRelativeDateOption.RELATIVE_TO_START_DATE;
      }
    }

    var source = placedRate.pearsRate().source();
    var eventKey = keys.rate(source, period.termNo(), period.phaseType());

    return new MappedRate(
        eventKey,
        date,
        placedRate.pearsRate().rentalRate(),
        period.termNo(),
        period.phaseType(),
        definitionOption,
        relativeDateOption,
        relativeDuration,
        placedRate.first() ? keys.comment(eventKey, source.comments()) : null,
        source
    );
  }

  /**
   * A PEARS rate that can be migrated.
   *
   * @param recurrenceCount        how many more times a yearly rate recurs, or null for ever
   * @param offset                 the offset to place its anniversaries from, or null to place them a year apart from
   *                               {@code date}
   * @param scheduleStartDate      the version's start date, which the offset is from
   */
  private record PearsRate(
      LocalDate date,
      BigDecimal rentalRate,
      boolean recursYearly,
      @Nullable Integer recurrenceCount,
      @Nullable VersionContext.Offset offset,
      LocalDate scheduleStartDate,
      PearsScheduleEvent source
  ) {

    /**
     * The rate's date {@code year} years on, as PEARS places a yearly recurrence.
     */
    LocalDate anniversary(int year) {
      if (year == 0) {
        return date;
      }
      return offset != null ? offset.dateFrom(scheduleStartDate, year) : PearsDates.addMonths(date, year * 12);
    }
  }

  /**
   * A term, or a phase of the initial term, that a rate is anchored to.
   *
   * @param phaseType null for a term
   * @param boundary  the day after it ends, where the next term or phase starts
   */
  private record RatePeriod(int termNo, @Nullable PhaseType phaseType, LocalDate startDate, LocalDate boundary) {
  }

  /**
   * A PEARS rate's first date in a term or phase: one LMS rate.
   *
   * @param first whether it is the first LMS rate from the PEARS rate, which carries the PEARS rate's comments
   */
  private record PlacedRate(LocalDate date, PearsRate pearsRate, RatePeriod period, boolean first) {
  }
}
