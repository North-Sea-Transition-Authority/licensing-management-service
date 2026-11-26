package uk.co.nstauthority.licensingmanagementservice.migration.pears.workprogramme;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDuration;
import uk.co.nstauthority.licensingmanagementservice.licence.PhaseType;
import uk.co.nstauthority.licensingmanagementservice.licence.TermType;

/**
 * What the work programme migration needs of a licence's schedule, as the schedule migration wrote it: the
 * {@code ACTIVE} detail's start date, terms and phases, and the events it migrated from PEARS {@code DD} events.
 *
 * @param startDate the licence start date, which PEARS offsets such as the breakpoint are measured from
 * @param ddEvents  the migrated {@code DD} events, each anchored to a term as the schedule migration placed it
 */
public record MigratedSchedule(LocalDate startDate, List<Term> terms, List<Phase> phases, List<DdEvent> ddEvents) {

  public MigratedSchedule {
    terms = terms.stream().sorted(Comparator.comparing(Term::startDate)).toList();
    phases = List.copyOf(phases);
    ddEvents = ddEvents.stream().sorted(Comparator.comparing(DdEvent::date)).toList();
  }

  public Optional<Term> term(TermType type) {
    return terms.stream().filter(term -> term.type() == type).findFirst();
  }

  public Optional<Phase> phase(PhaseType type) {
    return phases.stream().filter(phase -> phase.type() == type).findFirst();
  }

  public record Term(TermType type, LocalDate startDate, LocalDate endDate) {
  }

  public record Phase(PhaseType type, LocalDate startDate, LocalDate endDate) {
  }

  /**
   * A migrated {@code DD} event: the term it is anchored to, its offset from that term's start, and the date it lands
   * on, which is the PEARS date.
   */
  public record DdEvent(TermType termType, ThreeFieldDuration relativeDuration, LocalDate date) {
  }
}
