package uk.co.nstauthority.licensingmanagementservice.migration.pears.workprogramme;

import jakarta.annotation.Nullable;
import java.time.LocalDate;
import java.util.Optional;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import uk.co.nstauthority.licensingmanagementservice.licence.PhaseType;
import uk.co.nstauthority.licensingmanagementservice.licence.TermType;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.operation.MigrationNotes;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.LmsScheduleDurations;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.PearsDates;

/**
 * Places a migrated activity on the licence's migrated schedule, following the plan's M3. PEARS activities have no
 * dates, so this is all rules, in this order:
 * <ol>
 *   <li>a drill-or-drop well is dated from the schedule's migrated {@code DD} event, or failing that from the
 *       programme's breakpoint;</li>
 *   <li>a phase-organised activity sits within its phase;</li>
 *   <li>a flat-list activity whose description starts "Phase A:" (B, C) sits within that phase;</li>
 *   <li>anything else sits within the Initial term, or the first term where there is no Initial term.</li>
 * </ol>
 * A phase the schedule doesn't have falls through to the next rule.
 */
final class PearsActivityDateMapper {

  static final String DATED_FROM_DD_EVENT = "Drill-or-drop well: dated from the schedule's DD event";
  static final String DATED_FROM_BREAKPOINT = "Drill-or-drop well: dated from the breakpoint (no DD event)";
  static final String BREAKPOINT_DISAGREES = "Drill-or-drop well: breakpoint disagrees with the DD event, which is used";
  static final String SEVERAL_DD_EVENTS = "Drill-or-drop well: several DD events, dated from the earliest";
  static final String BREAKPOINT_ZERO = "Breakpoint of zero: ignored";
  static final String BREAKPOINT_UNREADABLE = "Breakpoint that isn't whole numbers: ignored";
  static final String PHASE_NOT_ON_SCHEDULE = "Activity's phase isn't on the schedule: within the term instead";
  static final String PHASE_FROM_DESCRIPTION = "Flat-list activity whose description starts with its phase: within it";
  static final String FALLOW_OR_EXTENSION =
      "Fallow or extension programme: within the Initial term (provisional, WP-L12)";
  static final String NO_INITIAL_TERM = "Schedule with no Initial term: within its first term";

  private static final String DRILL_OR_DROP_WELL = "DRILL_OR_DROP_WELL";

  private static final Pattern PHASE_PREFIX = Pattern.compile("(?i)^phase\\s+([abc])\\s*:");
  private static final Pattern PHASE_TYPE = Pattern.compile("(?i)^phase\\s+([abc])$");
  /**
   * A breakpoint part PEARS could hold: a whole number of up to 4 digits. Anything longer is junk, and would overflow.
   */
  private static final Pattern WHOLE_NUMBER = Pattern.compile("\\d{1,4}");

  private PearsActivityDateMapper() {
  }

  /**
   * Where the activity sits on the schedule.
   *
   * @throws PearsWorkProgrammeMappingException if the schedule has no terms, which the schedule migration never writes
   */
  static MappedActivityDate date(
      PearsWorkProgramme workProgramme,
      MappedActivity activity,
      MigratedSchedule schedule,
      MigrationNotes notes
  ) {
    var source = activity.source();

    if (DRILL_OR_DROP_WELL.equals(StringUtils.strip(source.activityType()))) {
      var drillOrDropDate = drillOrDropDate(workProgramme, schedule, notes);
      if (drillOrDropDate.isPresent()) {
        return drillOrDropDate.get();
      }
    }

    var phase = phase(source, notes);
    if (phase != null) {
      if (schedule.phase(phase).isPresent()) {
        return MappedActivityDate.withinPhase(phase);
      }
      notes.note(PHASE_NOT_ON_SCHEDULE);
    }

    return withinTerm(workProgramme, schedule, notes);
  }

  /**
   * The phase the activity names: its enclosing phase on a phase-organised programme, or the phase its description
   * starts with on a flat list. Null when it names none.
   */
  @Nullable
  private static PhaseType phase(PearsActivity source, MigrationNotes notes) {
    if (source.listKind() == PearsActivity.ListKind.PHASE) {
      var phaseType = PHASE_TYPE.matcher(StringUtils.defaultString(source.phaseType()).strip());
      return phaseType.matches() ? phaseType(phaseType.group(1)) : null;
    }

    var prefix = PHASE_PREFIX.matcher(StringUtils.defaultString(source.description()).strip());
    if (prefix.find()) {
      notes.note(PHASE_FROM_DESCRIPTION);
      return phaseType(prefix.group(1));
    }
    return null;
  }

  private static PhaseType phaseType(String letter) {
    return switch (letter.toUpperCase()) {
      case "A" -> PhaseType.PHASE_A;
      case "B" -> PhaseType.PHASE_B;
      default -> PhaseType.PHASE_C;
    };
  }

  /**
   * The drill-or-drop decision date. The schedule's {@code DD} event comes first: the schedule is maintained and the
   * breakpoint isn't, so where both exist the schedule is right (WP-S6b). The breakpoint dates the well only where the
   * schedule has no {@code DD} event.
   */
  private static Optional<MappedActivityDate> drillOrDropDate(
      PearsWorkProgramme workProgramme,
      MigratedSchedule schedule,
      MigrationNotes notes
  ) {
    var breakpointDate = breakpointDate(workProgramme.breakpoint(), schedule.startDate(), notes);

    if (!schedule.ddEvents().isEmpty()) {
      var ddEvent = schedule.ddEvents().getFirst();
      if (schedule.ddEvents().size() > 1) {
        notes.note(SEVERAL_DD_EVENTS);
      }
      if (breakpointDate != null && !breakpointDate.equals(ddEvent.date())) {
        notes.note(BREAKPOINT_DISAGREES);
      }
      notes.note(DATED_FROM_DD_EVENT);
      return Optional.of(MappedActivityDate.relative(ddEvent.termType(), ddEvent.relativeDuration(), ddEvent.date()));
    }

    if (breakpointDate != null) {
      var firstTerm = firstTerm(workProgramme, schedule);
      notes.note(DATED_FROM_BREAKPOINT);
      return Optional.of(MappedActivityDate.relative(
          firstTerm.type(),
          LmsScheduleDurations.untilDate(firstTerm.startDate(), breakpointDate),
          breakpointDate
      ));
    }

    return Optional.empty();
  }

  /**
   * The date PEARS gives the breakpoint, an offset from the licence start, worked out as PEARS works out schedule
   * dates. Null when there is no breakpoint, or one that can't be used.
   */
  @Nullable
  private static LocalDate breakpointDate(
      @Nullable PearsWorkProgramme.Breakpoint breakpoint,
      LocalDate startDate,
      MigrationNotes notes
  ) {
    if (breakpoint == null) {
      return null;
    }

    var parts = new int[3];
    var values = new String[] {breakpoint.years(), breakpoint.months(), breakpoint.days()};
    for (var index = 0; index < values.length; index++) {
      var value = StringUtils.strip(values[index]);
      if (StringUtils.isEmpty(value)) {
        continue;
      }
      if (!WHOLE_NUMBER.matcher(value).matches()) {
        notes.note(BREAKPOINT_UNREADABLE);
        return null;
      }
      parts[index] = Integer.parseInt(value);
    }

    if (parts[0] == 0 && parts[1] == 0 && parts[2] == 0) {
      notes.note(BREAKPOINT_ZERO);
      return null;
    }
    return PearsDates.eventDate(startDate, parts[0], parts[1], parts[2]);
  }

  private static MappedActivityDate withinTerm(
      PearsWorkProgramme workProgramme,
      MigratedSchedule schedule,
      MigrationNotes notes
  ) {
    if (!"INITIAL".equals(StringUtils.strip(workProgramme.type()))) {
      notes.note(FALLOW_OR_EXTENSION);
    }
    var term = firstTerm(workProgramme, schedule);
    if (term.type() != TermType.INITIAL) {
      notes.note(NO_INITIAL_TERM);
    }
    return MappedActivityDate.withinTerm(term.type());
  }

  /**
   * The Initial term, or the first term where the schedule starts at a later one (schedule survey patterns A and G).
   */
  private static MigratedSchedule.Term firstTerm(PearsWorkProgramme workProgramme, MigratedSchedule schedule) {
    return schedule.term(TermType.INITIAL)
        .or(() -> schedule.terms().stream().findFirst())
        .orElseThrow(() -> new PearsWorkProgrammeMappingException(workProgramme.licence(),
            "The migrated schedule has no terms"));
  }
}
