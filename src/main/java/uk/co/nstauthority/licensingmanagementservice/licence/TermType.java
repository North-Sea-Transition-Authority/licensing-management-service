package uk.co.nstauthority.licensingmanagementservice.licence;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.Displayable;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.DisplayableEnumOptionUtil;

public enum TermType implements Displayable {
  INITIAL(LicenceTypeGroup.PRODUCTION, "Initial Term", 10),
  SECOND(LicenceTypeGroup.PRODUCTION, "Second Term", 20),
  THIRD(LicenceTypeGroup.PRODUCTION, "Third Term", 30),
  APPRAISAL(LicenceTypeGroup.CARBON_STORAGE, "Appraisal Term", 10),
  INITIAL_CS(LicenceTypeGroup.CARBON_STORAGE, "Initial Term", 20),
  OPERATIONAL(LicenceTypeGroup.CARBON_STORAGE, "Operational Term", 30),
  POST_CLOSURE_PERIOD(LicenceTypeGroup.CARBON_STORAGE, "Post Closure Period", 40);

  private final Set<LicenceType> licenceTypes;
  private final String displayName;
  private final Integer displayOrder;

  TermType(
      LicenceTypeGroup licenceTypeGroup,
      String displayName,
      Integer displayOrder
  ) {
    this.licenceTypes = licenceTypeGroup.getLicenceTypes();
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