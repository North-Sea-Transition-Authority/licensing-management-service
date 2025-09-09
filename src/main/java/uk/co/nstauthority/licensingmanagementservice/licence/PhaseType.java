package uk.co.nstauthority.licensingmanagementservice.licence;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.Displayable;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.DisplayableEnumOptionUtil;

public enum PhaseType implements Displayable {
  PHASE_A(TermType.INITIAL, "Phase A", 1),
  PHASE_B(TermType.INITIAL, "Phase B", 2),
  PHASE_C(TermType.INITIAL, "Phase C", 3);

  private final TermType termType;
  private final String displayName;
  private final Integer displayOrder;

  PhaseType(
      TermType termType,
      String displayName,
      Integer displayOrder
  ) {
    this.termType = termType;
    this.displayName = displayName;
    this.displayOrder = displayOrder;
  }

  public TermType getTermType() {
    return termType;
  }

  public static Set<PhaseType> getPhasesFor(TermType termType) {
    return Set.of(PhaseType.values()).stream()
        .filter(phaseType -> phaseType.getTermType().equals(termType))
        .collect(Collectors.toSet());
  }

  public static Map<String, String> getPhaseRadioOptionsFor(LicenceType licenceType) {
    var phaseTypes = TermType.getTermsFor(licenceType).stream()
        .map(PhaseType::getPhasesFor)
        .flatMap(Collection::stream)
        .collect(Collectors.toSet());

    return DisplayableEnumOptionUtil.getDisplayableOptions(phaseTypes);
  }

  @Override
  public String getDisplayName() {
    return displayName;
  }

  @Override
  public int getDisplayOrder() {
    return displayOrder;
  }
}