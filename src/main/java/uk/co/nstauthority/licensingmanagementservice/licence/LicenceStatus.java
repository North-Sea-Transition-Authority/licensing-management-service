package uk.co.nstauthority.licensingmanagementservice.licence;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.Displayable;

public enum LicenceStatus implements Displayable {
  EXTANT("Extant", 10, Arrays.asList(LicenceType.values())),
  REVOKED("Revoked", 20, Arrays.asList(LicenceType.values())),
  SURRENDERED("Surrendered", 30, Arrays.asList(LicenceType.values())),
  EXPIRED("Expired", 40, Arrays.asList(LicenceType.values())),
  SPLIT_AND_TERMINATED("Split and terminated", 50, List.of(LicenceType.CARBON_STORAGE));

  private final String displayName;
  private final int displayOrder;
  private final List<LicenceType> applicableLicenceTypes;

  LicenceStatus(String displayName, int displayOrder, List<LicenceType> applicableLicenceTypes) {
    this.displayName = displayName;
    this.displayOrder = displayOrder;
    this.applicableLicenceTypes = applicableLicenceTypes;
  }

  @Override
  public String getDisplayName() {
    return displayName;
  }

  @Override
  public int getDisplayOrder() {
    return displayOrder;
  }

  public List<LicenceType> getApplicableLicenceTypes() {
    return applicableLicenceTypes;
  }

  public static List<LicenceStatus> getApplicableStatusesForLicenceType(LicenceType licenceType) {
    return Arrays.stream(LicenceStatus.values())
        .filter(status -> status.getApplicableLicenceTypes().contains(licenceType))
        .sorted(Comparator.comparing(LicenceStatus::getDisplayOrder))
        .toList();
  }
}
