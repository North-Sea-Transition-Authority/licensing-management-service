package uk.co.nstauthority.licensingmanagementservice.file;

public enum FileUsageType {
  APPLICATION_SUPPORTING_DOCUMENT("APPLICATION-SUPPORTING-DOCUMENT"),
  APPLICATION_CONTINUATION_LETTER("APPLICATION-CONTINUATION-LETTER"),
  CONTINUATION_OTHER_REQUIREMENT_DOCUMENT("CONTINUATION-OTHER-REQUIREMENT-DOCUMENT"),
  CONTINUATION_ADDITIONAL_SUPPORTING_DOCUMENT("CONTINUATION-ADDITIONAL-SUPPORTING-DOCUMENT");

  private final String usageType;

  FileUsageType(String usageType) {
    this.usageType = usageType;
  }

  public String getUsageType() {
    return usageType;
  }
}
