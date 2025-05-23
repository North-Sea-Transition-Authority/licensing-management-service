package uk.co.nstauthority.licensingmanagementservice.file;

public enum XyzApplicationFileUsageType {
  SUPPORTING_DOCUMENT("SUPPORTING-DOCUMENT");
  private final String usageType;

  XyzApplicationFileUsageType(String usageType) {
    this.usageType = usageType;
  }

  public String getUsageType() {
    return usageType;
  }
}
