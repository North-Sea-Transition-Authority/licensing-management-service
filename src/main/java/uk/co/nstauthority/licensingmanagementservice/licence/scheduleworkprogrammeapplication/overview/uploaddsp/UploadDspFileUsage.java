package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overview.uploaddsp;

import uk.co.nstauthority.licensingmanagementservice.file.ApplicationFileUsage;
import uk.co.nstauthority.licensingmanagementservice.file.FileUsageType;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;

public record UploadDspFileUsage(
    String usageId,
    String usageType,
    String documentType
) implements ApplicationFileUsage {

  public static UploadDspFileUsage fromApplication(
      ScheduleWorkProgrammeApplicationDetail applicationDetail) {
    return new UploadDspFileUsage(
        applicationDetail.getId().toString(),
        FileUsageType.FINAL_DECISION_SUPPORT_PAPER.getUsageType(),
        "final-decision-support-paper"
    );
  }
}
