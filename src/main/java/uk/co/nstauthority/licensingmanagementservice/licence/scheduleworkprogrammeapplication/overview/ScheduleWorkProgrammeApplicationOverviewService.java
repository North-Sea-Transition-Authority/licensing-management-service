package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overview;

import java.util.List;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.EnergyPortalUserService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.WebUserAccountId;
import uk.co.nstauthority.licensingmanagementservice.formatting.DateFormatUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryDataView;

@Service
class ScheduleWorkProgrammeApplicationOverviewService {

  static final String SUBMITTED_BY_USER_PURPOSE = "Fetch submitted by user for application overview";

  private final LicenceService licenceService;
  private final EnergyPortalUserService energyPortalUserService;

  ScheduleWorkProgrammeApplicationOverviewService(LicenceService licenceService,
                                                  EnergyPortalUserService energyPortalUserService) {
    this.licenceService = licenceService;
    this.energyPortalUserService = energyPortalUserService;
  }

  ScheduleWorkProgrammeApplicationContext getApplicationContext(
      ScheduleWorkProgrammeApplicationDetail applicationDetail,
      Licence licence
  ) {
    var submittedByUser = energyPortalUserService.getByWuaId(
        WebUserAccountId.from(applicationDetail.getSubmittedByWuaId()),
        SUBMITTED_BY_USER_PURPOSE
    );

    var summaryDataView = SummaryDataView.newBuilder()
        .addStringValue("Status", applicationDetail.getStatus().getDisplayName())
        .addStringValue("Licence reference", licence.getLicenceReference())
        .addStringValue("Submitted by", submittedByUser.displayName())
        .addStringValue("Submission date",
            DateFormatUtil.convertToDisplayTextWithTime(applicationDetail.getSubmittedDatetime()))
        .build();

    return new ScheduleWorkProgrammeApplicationContext(
        applicationDetail.getScheduleWorkProgrammeApplication().getApplicationReference(),
        licenceService.getLicencePageCaption(licence),
        List.of(summaryDataView)
    );
  }
}
