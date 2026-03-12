package uk.co.nstauthority.licensingmanagementservice.licence;

public enum LicenceScheduleFileUsageType {
  SCHEDULE_AMENDMENT_APP_SUPPORTING_DOCUMENT("SCHEDULE-AMENDMENT-APP-SUPPORTING-DOCUMENT"),
  FINAL_DECISION_SUPPORT_PAPER("FINAL-DECISION-SUPPORT-PAPER");
  private final String usageType;

  LicenceScheduleFileUsageType(String usageType) {
    this.usageType = usageType;
  }

  public String getUsageType() {
    return usageType;
  }
}