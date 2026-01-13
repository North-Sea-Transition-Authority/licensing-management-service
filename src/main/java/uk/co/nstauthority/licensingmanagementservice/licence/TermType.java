package uk.co.nstauthority.licensingmanagementservice.licence;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.Displayable;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.DisplayableEnumOptionUtil;

public enum TermType implements Displayable {
  INITIAL(Set.of(LicenceType.LANDWARD_PRODUCTION, LicenceType.SEAWARD_PRODUCTION), "Initial Term", 10),
  SECOND(Set.of(LicenceType.LANDWARD_PRODUCTION, LicenceType.SEAWARD_PRODUCTION), "Second Term", 20),
  THIRD(Set.of(LicenceType.LANDWARD_PRODUCTION, LicenceType.SEAWARD_PRODUCTION), "Third Term", 30),
  APPRAISAL(Set.of(LicenceType.CARBON_STORAGE), "Appraisal Term", 10),
  INITIAL_CS(Set.of(LicenceType.CARBON_STORAGE), "Initial Term", 20),
  OPERATIONAL(Set.of(LicenceType.CARBON_STORAGE), "Operational Term", 30),
  POST_CLOSURE_PERIOD(Set.of(LicenceType.CARBON_STORAGE), "Post Closure Period", 40);

  private final Set<LicenceType> licenceTypes;
  private final String displayName;
  private final Integer displayOrder;

  TermType(
      Set<LicenceType> licenceTypes,
      String displayName,
      Integer displayOrder
  ) {
    this.licenceTypes = licenceTypes;
    this.displayName = displayName;
    this.displayOrder = displayOrder;
  }

  public Set<LicenceType> getLicenceTypes() {
    return licenceTypes;
  }

  public static Set<TermType> getTermsFor(LicenceType licenceType) {
    return Stream.of(values())
        .filter(termType -> termType.getLicenceTypes().contains(licenceType))
        .collect(Collectors.toSet());
  }

  public static Map<String, String> getTermRadioOptionsFor(LicenceType licenceType) {
    return DisplayableEnumOptionUtil.getDisplayableOptions(getTermsFor(licenceType));
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