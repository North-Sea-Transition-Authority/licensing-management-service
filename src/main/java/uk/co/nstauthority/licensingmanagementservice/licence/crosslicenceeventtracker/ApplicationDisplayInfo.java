package uk.co.nstauthority.licensingmanagementservice.licence.crosslicenceeventtracker;

import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.application.LatestApplicationDetailSummary;

record ApplicationDisplayInfo(ApplicationStatus status, String applicationReference, String overviewLink) {

  static ApplicationDisplayInfo from(LatestApplicationDetailSummary summary, String overviewLink) {
    return new ApplicationDisplayInfo(summary.status(), summary.applicationReference(), overviewLink);
  }
}
