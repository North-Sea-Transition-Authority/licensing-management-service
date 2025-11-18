package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overallrequest;

import uk.co.nstauthority.licensingmanagementservice.file.ApplicationFileUsage;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceScheduleFileUsageType;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;

public record LicenceScheduleSupportingInformationFileUsages(
    String usageId,
    String usageType,
    String documentType
) implements ApplicationFileUsage {

  public static LicenceScheduleSupportingInformationFileUsages fromApplication(
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail) {
    return new LicenceScheduleSupportingInformationFileUsages(
        scheduleWorkProgrammeApplicationDetail.getId().toString(),
        LicenceScheduleFileUsageType.SCHEDULE_AMENDMENT_APP_SUPPORTING_DOCUMENT.getUsageType(),
        "licence-schedule-application-supporting-document"
    );
  }

}