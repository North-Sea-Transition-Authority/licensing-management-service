package uk.co.nstauthority.licensingmanagementservice.migration.pears.workprogramme;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDuration;
import uk.co.nstauthority.licensingmanagementservice.licence.PhaseType;
import uk.co.nstauthority.licensingmanagementservice.licence.TermType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityCategory;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityCommitment;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.PearsLicenceReference;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.operation.MigrationNotes;

class PearsActivityDateMapperTest {

  private static final PearsLicenceReference P2244 = new PearsLicenceReference("P", 2244);

  private static final LocalDate START = LocalDate.of(2014, Month.DECEMBER, 1);

  private static final MigratedSchedule.Term INITIAL =
      new MigratedSchedule.Term(TermType.INITIAL, START, LocalDate.of(2018, Month.NOVEMBER, 30));
  private static final MigratedSchedule.Term SECOND =
      new MigratedSchedule.Term(TermType.SECOND, LocalDate.of(2018, Month.DECEMBER, 1), LocalDate.of(2022, Month.NOVEMBER, 30));

  private static final MigratedSchedule.Phase PHASE_A =
      new MigratedSchedule.Phase(PhaseType.PHASE_A, START, LocalDate.of(2016, Month.NOVEMBER, 30));
  private static final MigratedSchedule.Phase PHASE_B =
      new MigratedSchedule.Phase(PhaseType.PHASE_B, LocalDate.of(2016, Month.DECEMBER, 1), LocalDate.of(2018, Month.NOVEMBER, 30));

  private static final MigratedSchedule.DdEvent DD_EVENT = new MigratedSchedule.DdEvent(
      TermType.INITIAL, new ThreeFieldDuration(2, 9, 0), LocalDate.of(2017, Month.SEPTEMBER, 1));

  @Test
  void date_phaseOrganisedActivitySitsWithinItsPhase() {
    var notes = new MigrationNotes();

    var date = date(programme(null), phaseActivity("OBTAIN_3D_SEISMIC", "Phase B"), schedule(List.of()), notes);

    assertThat(date).isEqualTo(MappedActivityDate.withinPhase(PhaseType.PHASE_B));
    assertThat(notes.noteCountsByReason()).isEmpty();
  }

  @Test
  void date_whenTheScheduleHasNotGotTheActivitysPhase_thenWithinTheTermAndNoted() {
    var notes = new MigrationNotes();

    var date = date(programme(null), phaseActivity("FIRM_WELL", "Phase C"), schedule(List.of()), notes);

    assertThat(date).isEqualTo(MappedActivityDate.withinTerm(TermType.INITIAL));
    assertThat(notes.noteCountsByReason()).containsKey(PearsActivityDateMapper.PHASE_NOT_ON_SCHEDULE);
  }

  @Test
  void date_flatActivityWhoseDescriptionStartsWithAPhaseSitsWithinIt() {
    var notes = new MigrationNotes();
    var activity = flatActivity("SEISMIC", "Phase A: 3 years (TOTAL PHASE A: 3 YEARS)\nObtain 2000 kms 2D seismic data");

    var date = date(programme(null), activity, schedule(List.of()), notes);

    assertThat(date).isEqualTo(MappedActivityDate.withinPhase(PhaseType.PHASE_A));
    assertThat(notes.noteCountsByReason()).containsKey(PearsActivityDateMapper.PHASE_FROM_DESCRIPTION);
  }

  @Test
  void date_flatActivityNamingAPhaseOnlyLaterInItsDescriptionSitsWithinTheTerm() {
    var date = date(programme(null), flatActivity("OTHER", "Studies to be done in Phase A: geology"),
        schedule(List.of()), new MigrationNotes());

    assertThat(date).isEqualTo(MappedActivityDate.withinTerm(TermType.INITIAL));
  }

  @Test
  void date_flatActivitySitsWithinTheInitialTerm() {
    var notes = new MigrationNotes();

    var date = date(programme(null), flatActivity("LEGACY", "DorD 3 yrs 1500m"), schedule(List.of()), notes);

    assertThat(date).isEqualTo(MappedActivityDate.withinTerm(TermType.INITIAL));
    assertThat(notes.noteCountsByReason()).isEmpty();
  }

  @Test
  void date_whenTheScheduleHasNoInitialTerm_thenWithinItsFirstTermAndNoted() {
    var notes = new MigrationNotes();
    var schedule = new MigratedSchedule(START, List.of(SECOND), List.of(), List.of());

    var date = date(programme(null), flatActivity("OTHER", "Studies"), schedule, notes);

    assertThat(date).isEqualTo(MappedActivityDate.withinTerm(TermType.SECOND));
    assertThat(notes.noteCountsByReason()).containsKey(PearsActivityDateMapper.NO_INITIAL_TERM);
  }

  @Test
  void date_fallowProgrammeActivitySitsWithinTheInitialTermAndIsNoted() {
    var notes = new MigrationNotes();
    var fallow = new PearsWorkProgramme(P2244, 12L, 201L, LocalDateTime.of(2011, Month.JULY, 12, 0, 0), "FALLOW", null,
        "NOT_STARTED", null, List.of(), List.of());

    var date = date(fallow, flatActivity("LEGACY", "Not Fallow - Block is Field"), schedule(List.of()), notes);

    assertThat(date).isEqualTo(MappedActivityDate.withinTerm(TermType.INITIAL));
    assertThat(notes.noteCountsByReason()).containsKey(PearsActivityDateMapper.FALLOW_OR_EXTENSION);
  }

  @Test
  void date_drillOrDropWellIsDatedFromTheDdEvent() {
    var notes = new MigrationNotes();

    var date = date(programme(breakpoint("2", "9", "0")), flatActivity("DRILL_OR_DROP_WELL", "One D&D well"),
        schedule(List.of(DD_EVENT)), notes);

    assertThat(date).isEqualTo(MappedActivityDate.relative(TermType.INITIAL, new ThreeFieldDuration(2, 9, 0),
        LocalDate.of(2017, Month.SEPTEMBER, 1)));
    assertThat(notes.noteCountsByReason())
        .containsKey(PearsActivityDateMapper.DATED_FROM_DD_EVENT)
        .doesNotContainKey(PearsActivityDateMapper.BREAKPOINT_DISAGREES);
  }

  @Test
  void date_drillOrDropWellOnAPhaseIsDatedFromTheDdEventRatherThanThePhase() {
    var date = date(programme(null), phaseActivity("DRILL_OR_DROP_WELL", "Phase C"), schedule(List.of(DD_EVENT)),
        new MigrationNotes());

    assertThat(date.dueDate()).isEqualTo(DD_EVENT.date());
  }

  @Test
  void date_whenTheBreakpointDisagreesWithTheDdEvent_thenTheDdEventIsUsedAndNoted() {
    var notes = new MigrationNotes();

    var date = date(programme(breakpoint("2", null, null)), flatActivity("DRILL_OR_DROP_WELL", "One D&D well"),
        schedule(List.of(DD_EVENT)), notes);

    assertThat(date.dueDate()).isEqualTo(DD_EVENT.date());
    assertThat(notes.noteCountsByReason()).containsKey(PearsActivityDateMapper.BREAKPOINT_DISAGREES);
  }

  @Test
  void date_whenThereAreSeveralDdEvents_thenTheEarliestIsUsedAndNoted() {
    var notes = new MigrationNotes();
    var later = new MigratedSchedule.DdEvent(TermType.INITIAL, new ThreeFieldDuration(3, 0, 0),
        LocalDate.of(2017, Month.DECEMBER, 1));

    var date = date(programme(null), flatActivity("DRILL_OR_DROP_WELL", "One D&D well"),
        schedule(List.of(later, DD_EVENT)), notes);

    assertThat(date.dueDate()).isEqualTo(DD_EVENT.date());
    assertThat(notes.noteCountsByReason()).containsKey(PearsActivityDateMapper.SEVERAL_DD_EVENTS);
  }

  @Test
  void date_whenThereIsNoDdEvent_thenTheDrillOrDropWellIsDatedFromTheBreakpoint() {
    var notes = new MigrationNotes();

    var date = date(programme(breakpoint("4", "0", "0")), flatActivity("DRILL_OR_DROP_WELL", "One D&D well"),
        schedule(List.of()), notes);

    assertThat(date).isEqualTo(MappedActivityDate.relative(TermType.INITIAL, new ThreeFieldDuration(4, 0, 0),
        LocalDate.of(2018, Month.DECEMBER, 1)));
    assertThat(notes.noteCountsByReason()).containsKey(PearsActivityDateMapper.DATED_FROM_BREAKPOINT);
  }

  @Test
  void date_breakpointWithMonthsAndDaysLandsOnThePearsDate() {
    // PEARS adds the days first, then the months, keeping a month end: 30-Jan-2015 + 1 day = 31-Jan-2015, a month
    // end, + 1 month = 28-Feb-2015. LMS, adding the month first, would give 01-Mar-2015.
    var schedule = new MigratedSchedule(LocalDate.of(2015, Month.JANUARY, 30),
        List.of(new MigratedSchedule.Term(TermType.INITIAL, LocalDate.of(2015, Month.JANUARY, 30),
            LocalDate.of(2019, Month.JANUARY, 29))),
        List.of(), List.of());

    var date = date(programme(breakpoint("0", "1", "1")), flatActivity("DRILL_OR_DROP_WELL", "One D&D well"), schedule,
        new MigrationNotes());

    assertThat(date.dueDate()).isEqualTo(LocalDate.of(2015, Month.FEBRUARY, 28));
    assertThat(date.relativeDuration()).isEqualTo(new ThreeFieldDuration(0, 0, 29));
  }

  @Test
  void date_zeroOrUnreadableBreakpointIsIgnored() {
    var notes = new MigrationNotes();

    var zero = date(programme(breakpoint("0", "0", "0")), flatActivity("DRILL_OR_DROP_WELL", "One D&D well"),
        schedule(List.of()), notes);
    var unreadable = date(programme(breakpoint("3 yrs", null, null)), flatActivity("DRILL_OR_DROP_WELL", "A well"),
        schedule(List.of()), notes);
    var tooLong = date(programme(breakpoint("99999999999", null, null)), flatActivity("DRILL_OR_DROP_WELL", "A well"),
        schedule(List.of()), notes);

    assertThat(zero).isEqualTo(MappedActivityDate.withinTerm(TermType.INITIAL));
    assertThat(unreadable).isEqualTo(MappedActivityDate.withinTerm(TermType.INITIAL));
    assertThat(tooLong).isEqualTo(MappedActivityDate.withinTerm(TermType.INITIAL));
    assertThat(notes.noteCountsByReason())
        .containsKey(PearsActivityDateMapper.BREAKPOINT_ZERO)
        .containsKey(PearsActivityDateMapper.BREAKPOINT_UNREADABLE);
  }

  @Test
  void date_onlyDrillOrDropWellsTakeTheDdEvent() {
    var date = date(programme(breakpoint("2", "9", "0")), flatActivity("FIRM_WELL", "One firm well"),
        schedule(List.of(DD_EVENT)), new MigrationNotes());

    assertThat(date).isEqualTo(MappedActivityDate.withinTerm(TermType.INITIAL));
  }

  @Test
  void date_whenTheScheduleHasNoTerms_thenTheLicenceFails() {
    var schedule = new MigratedSchedule(START, List.of(), List.of(), List.of());
    var programme = programme(null);
    var activity = flatActivity("OTHER", "Studies");
    var notes = new MigrationNotes();

    assertThatThrownBy(() -> date(programme, activity, schedule, notes))
        .isInstanceOf(PearsWorkProgrammeMappingException.class)
        .hasMessage("P2244: The migrated schedule has no terms");
  }

  private static MappedActivityDate date(
      PearsWorkProgramme programme,
      PearsActivity activity,
      MigratedSchedule schedule,
      MigrationNotes notes
  ) {
    var mapped = new MappedActivity(activity.activityId(), WorkProgrammeActivityCategory.OTHER_ACTIVITY, "Other",
        WorkProgrammeActivityCommitment.CONDITIONAL, Objects.toString(activity.description(), ""), false, activity);
    return PearsActivityDateMapper.date(programme, mapped, schedule, notes);
  }

  private static MigratedSchedule schedule(List<MigratedSchedule.DdEvent> ddEvents) {
    return new MigratedSchedule(START, List.of(INITIAL, SECOND), List.of(PHASE_A, PHASE_B), ddEvents);
  }

  private static PearsActivity flatActivity(String activityType, String description) {
    return new PearsActivity(1, PearsActivity.ListKind.FLAT, null, "501", activityType, null, "NOT_STARTED", null,
        description, null);
  }

  private static PearsActivity phaseActivity(String activityType, String phaseType) {
    return new PearsActivity(1, PearsActivity.ListKind.PHASE, phaseType, "502", activityType, null, "NOT_STARTED",
        null, "Some work", null);
  }

  private static PearsWorkProgramme.Breakpoint breakpoint(String years, String months, String days) {
    return new PearsWorkProgramme.Breakpoint(years, months, days);
  }

  private static PearsWorkProgramme programme(PearsWorkProgramme.Breakpoint breakpoint) {
    return new PearsWorkProgramme(P2244, 11L, 101L, LocalDateTime.of(2019, Month.MARCH, 4, 10, 0), "INITIAL", null,
        "NOT_STARTED", breakpoint, List.of(), List.of());
  }
}
