package uk.co.nstauthority.template.file;

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
