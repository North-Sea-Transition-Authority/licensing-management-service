package uk.co.nstauthority.licensingmanagementservice.licence;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

public enum EventType {
  EXPIRY(EnumSet.of(LicenceTypeGroup.PRODUCTION, LicenceTypeGroup.CARBON_STORAGE, LicenceTypeGroup.GAS_STORAGE,
      LicenceTypeGroup.EXPLORATION, LicenceTypeGroup.METHANE_DRAINAGE)),
  MANDATORY_SURRENDER(EnumSet.of(LicenceTypeGroup.PRODUCTION)),
  BESPOKE(EnumSet.of(LicenceTypeGroup.PRODUCTION)),
  RETENTION(EnumSet.of(LicenceTypeGroup.PRODUCTION)),
  DEVELOPMENT(EnumSet.of(LicenceTypeGroup.PRODUCTION)),
  PRE_PRODUCING(EnumSet.of(LicenceTypeGroup.PRODUCTION)),
  PERMIT_CONDITIONS(EnumSet.of(LicenceTypeGroup.CARBON_STORAGE));

  private final Set<LicenceType> supportingLicenceTypes;

  EventType(Set<LicenceTypeGroup> licenceTypeGroups) {
    this.supportingLicenceTypes = LicenceTypeGroup.flattenLicenceTypeGroups(licenceTypeGroups);
  }

  public static Set<EventType> getEventsFor(LicenceType licenceType) {
    return Arrays.stream(values())
        .filter(eventType -> eventType.supportingLicenceTypes.contains(licenceType))
        .collect(Collectors.toSet());
  }
}