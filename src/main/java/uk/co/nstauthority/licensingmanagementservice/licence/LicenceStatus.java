package uk.co.nstauthority.licensingmanagementservice.licence;

public enum LicenceStatus {
  EXTANT("Extant"),
  REVOKED("Revoked"),
  SURRENDERED("Surrendered"),
  EXPIRED("Expired"),
  SPLIT_AND_TERMINATED("Split and terminated");

  private final String displayText;

  LicenceStatus(String displayText) {
    this.displayText = displayText;
  }

  public String getDisplayText() {
    return displayText;
  }
}
