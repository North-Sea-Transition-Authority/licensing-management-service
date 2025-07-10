package uk.co.nstauthority.licensingmanagementservice.licence;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum TermType {
  INITIAL(LicenceTypeGroup.PRODUCTION),
  SECOND(LicenceTypeGroup.PRODUCTION),
  THIRD(LicenceTypeGroup.PRODUCTION),
  APPRAISAL(LicenceTypeGroup.CARBON_STORAGE);

  private final Set<LicenceType> licenceTypes;

  TermType(LicenceTypeGroup licenceTypeGroup) {
    this.licenceTypes = licenceTypeGroup.getLicenceTypes();
  }

  public Set<LicenceType> getLicenceTypes() {
    return licenceTypes;
  }

  public static Set<TermType> getTermsFor(LicenceType licenceType) {
    return Stream.of(values())
        .filter(termType -> termType.getLicenceTypes().contains(licenceType))
        .collect(Collectors.toSet());
  }
}