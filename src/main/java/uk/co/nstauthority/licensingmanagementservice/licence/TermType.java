package uk.co.nstauthority.licensingmanagementservice.licence;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.Displayable;

public enum TermType implements Displayable {
  INITIAL(Set.of(LicenceType.LANDWARD_PRODUCTION, LicenceType.SEAWARD_PRODUCTION), "Initial Term", 10),
  SECOND(Set.of(LicenceType.LANDWARD_PRODUCTION, LicenceType.SEAWARD_PRODUCTION), "Second Term", 20),
  THIRD(Set.of(LicenceType.LANDWARD_PRODUCTION, LicenceType.SEAWARD_PRODUCTION), "Third Term", 30),
  FOURTH(Set.of(LicenceType.LANDWARD_PRODUCTION, LicenceType.SEAWARD_PRODUCTION), "Fourth Term", 40, true),
  APPRAISAL(Set.of(LicenceType.CARBON_STORAGE), "Appraisal Term", 10),
  INITIAL_CS(Set.of(LicenceType.CARBON_STORAGE), "Initial Term", 20),
  OPERATIONAL(Set.of(LicenceType.CARBON_STORAGE), "Operational Term", 30),
  POST_CLOSURE_PERIOD(Set.of(LicenceType.CARBON_STORAGE), "Post Closure Period", 40);

  private final Set<LicenceType> licenceTypes;
  private final String displayName;
  private final Integer displayOrder;
  private final boolean historic;

  TermType(
      Set<LicenceType> licenceTypes,
      String displayName,
      Integer displayOrder
  ) {
    this(licenceTypes, displayName, displayOrder, false);
  }

  TermType(
      Set<LicenceType> licenceTypes,
      String displayName,
      Integer displayOrder,
      boolean historic
  ) {
    this.licenceTypes = licenceTypes;
    this.displayName = displayName;
    this.displayOrder = displayOrder;
    this.historic = historic;
  }

  public Set<LicenceType> getLicenceTypes() {
    return licenceTypes;
  }

  public boolean isHistoric() {
    return historic;
  }

  public static Optional<TermType> fromDisplayName(String displayName) {
    return Stream.of(values())
        .filter(t -> t.displayName.equals(displayName))
        .findFirst();
  }

  public static Set<TermType> getTermsFor(LicenceType licenceType) {
    return Stream.of(values())
        .filter(termType -> termType.getLicenceTypes().contains(licenceType))
        .collect(Collectors.toSet());
  }

  public static Set<TermType> getSelectableTermsFor(LicenceType licenceType, Collection<TermType> termTypesInUse) {
    return getTermsFor(licenceType).stream()
        .filter(termType -> !termType.isHistoric() || termTypesInUse.contains(termType))
        .collect(Collectors.toSet());
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
