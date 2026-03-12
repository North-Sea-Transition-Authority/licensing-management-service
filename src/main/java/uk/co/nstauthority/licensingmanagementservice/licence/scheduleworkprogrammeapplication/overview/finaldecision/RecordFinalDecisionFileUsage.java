package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overview.finaldecision;

import uk.co.nstauthority.licensingmanagementservice.file.ApplicationFileUsage;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceScheduleFileUsageType;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;

public record RecordFinalDecisionFileUsage(
    String usageId,
    String usageType,
    String documentType
) implements ApplicationFileUsage {

  public static RecordFinalDecisionFileUsage fromApplication(
      ScheduleWorkProgrammeApplicationDetail applicationDetail) {
    return new RecordFinalDecisionFileUsage(
        applicationDetail.getId().toString(),
        LicenceScheduleFileUsageType.FINAL_DECISION_SUPPORT_PAPER.getUsageType(),
        "final-decision-support-paper"
    );
  }
}
