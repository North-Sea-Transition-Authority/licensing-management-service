package uk.co.nstauthority.licensingmanagementservice.licence;

import java.util.Set;
import java.util.stream.Collectors;

public enum PhaseType {
  PHASE_A(TermType.INITIAL),
  PHASE_B(TermType.INITIAL),
  PHASE_C(TermType.INITIAL);

  private final TermType termType;

  PhaseType(TermType termType) {
    this.termType = termType;
  }

  public TermType getTermType() {
    return termType;
  }

  public static Set<PhaseType> getPhasesFor(TermType termType) {
    return Set.of(PhaseType.values()).stream()
        .filter(phaseType -> phaseType.getTermType().equals(termType))
        .collect(Collectors.toSet());
  }
}