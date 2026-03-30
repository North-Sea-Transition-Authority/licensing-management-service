package uk.co.nstauthority.licensingmanagementservice.file;

public enum FileUsageType {
  APPLICATION_SUPPORTING_DOCUMENT("APPLICATION-SUPPORTING-DOCUMENT"),
  APPLICATION_CONTINUATION_LETTER("APPLICATION-CONTINUATION-LETTER");

  private final String usageType;

  FileUsageType(String usageType) {
    this.usageType = usageType;
  }

  public String getUsageType() {
    return usageType;
  }
}
