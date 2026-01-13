package uk.co.nstauthority.licensingmanagementservice.licence;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

public enum EventType {
  EXPIRY(EnumSet.of(
      LicenceType.LANDWARD_PRODUCTION,
      LicenceType.SEAWARD_PRODUCTION,
      LicenceType.CARBON_STORAGE,
      LicenceType.GAS_STORAGE,
      LicenceType.LANDWARD_EXPLORATION,
      LicenceType.SEAWARD_EXPLORATION,
      LicenceType.METHANE_DRAINAGE
  )),
  MANDATORY_SURRENDER(EnumSet.of(
      LicenceType.LANDWARD_PRODUCTION,
      LicenceType.SEAWARD_PRODUCTION
  )),
  BESPOKE(EnumSet.of(
      LicenceType.LANDWARD_PRODUCTION,
      LicenceType.SEAWARD_PRODUCTION
  )),
  RETENTION(EnumSet.of(
      LicenceType.LANDWARD_PRODUCTION,
      LicenceType.SEAWARD_PRODUCTION
  )),
  DEVELOPMENT(EnumSet.of(
      LicenceType.LANDWARD_PRODUCTION,
      LicenceType.SEAWARD_PRODUCTION
  )),
  PRE_PRODUCING(EnumSet.of(
      LicenceType.LANDWARD_PRODUCTION,
      LicenceType.SEAWARD_PRODUCTION
  )),
  PERMIT_CONDITIONS(EnumSet.of(
      LicenceType.CARBON_STORAGE
  ));

  private final Set<LicenceType> supportingLicenceTypes;

  EventType(Set<LicenceType> licenceTypes) {
    this.supportingLicenceTypes = licenceTypes;
  }

  public static Set<EventType> getEventsFor(LicenceType licenceType) {
    return Arrays.stream(values())
        .filter(eventType -> eventType.supportingLicenceTypes.contains(licenceType))
        .collect(Collectors.toSet());
  }
}