package uk.co.nstauthority.licensingmanagementservice.licence;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

public enum LicenceTypeGroup {
  PRODUCTION(EnumSet.of(LicenceType.SEAWARD_PRODUCTION, LicenceType.LANDWARD_PRODUCTION)),
  CARBON_STORAGE(LicenceType.CARBON_STORAGE),
  GAS_STORAGE(LicenceType.GAS_STORAGE),
  EXPLORATION(EnumSet.of(LicenceType.SEAWARD_EXPLORATION, LicenceType.LANDWARD_EXPLORATION)),
  METHANE_DRAINAGE(LicenceType.METHANE_DRAINAGE),
  ;

  private final EnumSet<LicenceType> licenceTypes;

  LicenceTypeGroup(EnumSet<LicenceType> licenceTypes) {
    this.licenceTypes = licenceTypes;
  }

  LicenceTypeGroup(LicenceType licenceType) {
    this(EnumSet.of(licenceType));
  }

  public Set<LicenceType> getLicenceTypes() {
    return licenceTypes;
  }

  public String getUrlSlugList() {
    return licenceTypes.stream()
        .map(LicenceType::getUrlSlug)
        .collect(Collectors.joining(","));
  }

  public static LicenceTypeGroup getGroupBy(LicenceType licenceType) {
    return Arrays.stream(values())
        .filter(group -> group.getLicenceTypes().contains(licenceType))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Licence type %s not found in any group".formatted(licenceType)));
  }

  public static Set<LicenceType> flattenLicenceTypeGroups(Set<LicenceTypeGroup> licenceTypeGroupEnumSet) {
    return licenceTypeGroupEnumSet.stream()
        .map(LicenceTypeGroup::getLicenceTypes)
        .flatMap(Set::stream)
        .collect(Collectors.toSet());
  }
}
