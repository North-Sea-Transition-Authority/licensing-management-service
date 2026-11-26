package uk.co.nstauthority.licensingmanagementservice.migration.pears.workprogramme;

import jakarta.annotation.Nullable;
import java.time.LocalDate;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDuration;
import uk.co.nstauthority.licensingmanagementservice.licence.PhaseType;
import uk.co.nstauthority.licensingmanagementservice.licence.TermType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityDateOption;

/**
 * Where a migrated activity sits on the LMS schedule.
 *
 * @param termType         the term, for {@code WITHIN_A_TERM} and {@code RELATIVE_DATE}; otherwise null
 * @param phaseType        the phase, for {@code WITHIN_A_PHASE}; otherwise null
 * @param relativeDuration for {@code RELATIVE_DATE}, the offset from the term's start; otherwise null
 * @param dueDate          for {@code RELATIVE_DATE}, the date it lands on, which LMS stores; otherwise null
 */
public record MappedActivityDate(
    WorkProgrammeActivityDateOption dateOption,
    @Nullable TermType termType,
    @Nullable PhaseType phaseType,
    @Nullable ThreeFieldDuration relativeDuration,
    @Nullable LocalDate dueDate
) {

  static MappedActivityDate withinTerm(TermType termType) {
    return new MappedActivityDate(WorkProgrammeActivityDateOption.WITHIN_A_TERM, termType, null, null, null);
  }

  static MappedActivityDate withinPhase(PhaseType phaseType) {
    return new MappedActivityDate(WorkProgrammeActivityDateOption.WITHIN_A_PHASE, null, phaseType, null, null);
  }

  static MappedActivityDate relative(TermType termType, ThreeFieldDuration relativeDuration, LocalDate dueDate) {
    return new MappedActivityDate(WorkProgrammeActivityDateOption.RELATIVE_DATE, termType, null, relativeDuration,
        dueDate);
  }
}
