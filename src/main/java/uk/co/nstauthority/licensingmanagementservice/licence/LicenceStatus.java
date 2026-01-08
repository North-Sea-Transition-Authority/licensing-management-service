package uk.co.nstauthority.licensingmanagementservice.licence;

import java.util.Arrays;
import java.util.List;

public enum LicenceStatus {
  EXTANT("Extant", Arrays.asList(LicenceType.values())),
  REVOKED("Revoked", Arrays.asList(LicenceType.values())),
  SURRENDERED("Surrendered", Arrays.asList(LicenceType.values())),
  EXPIRED("Expired", Arrays.asList(LicenceType.values())),
  SPLIT_AND_TERMINATED("Split and terminated", List.of(LicenceType.CARBON_STORAGE));

  private final String displayText;
  private final List<LicenceType> applicableLicenceTypes;

  LicenceStatus(String displayText, List<LicenceType> applicableLicenceTypes) {
    this.displayText = displayText;
    this.applicableLicenceTypes = applicableLicenceTypes;
  }

  public String getDisplayText() {
    return displayText;
  }

  public List<LicenceType> getApplicableLicenceTypes() {
    return applicableLicenceTypes;
  }

  public static List<LicenceStatus> getApplicableStatusesForLicenceType(LicenceType licenceType) {
    return Arrays.stream(LicenceStatus.values())
        .filter(status -> status.getApplicableLicenceTypes().contains(licenceType))
        .toList();
  }
}
