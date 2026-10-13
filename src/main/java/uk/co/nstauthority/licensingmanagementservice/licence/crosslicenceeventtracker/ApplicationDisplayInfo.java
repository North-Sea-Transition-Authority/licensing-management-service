package uk.co.nstauthority.licensingmanagementservice.licence.crosslicenceeventtracker;

import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;

record ApplicationDisplayInfo(ApplicationStatus status, String applicationReference, String overviewLink) {

  static ApplicationDisplayInfo fromScheduleWorkProgrammeApplicationDetail(
      ScheduleWorkProgrammeApplicationDetail detail,
      String overviewLink
  ) {
    return new ApplicationDisplayInfo(
        detail.getStatus(),
        detail.getScheduleWorkProgrammeApplication().getApplicationReference(),
        overviewLink
    );
  }

  static ApplicationDisplayInfo fromLicenceContinuationApplicationDetail(
      LicenceContinuationApplicationDetail detail,
      String overviewLink
  ) {
    return new ApplicationDisplayInfo(
        detail.getStatus(),
        detail.getLicenceContinuationApplication().getApplicationReference(),
        overviewLink
    );
  }
}
