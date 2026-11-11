package uk.co.nstauthority.licensingmanagementservice.licence.continuation.overview;

import java.util.List;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.EnergyPortalUserService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.WebUserAccountId;
import uk.co.nstauthority.licensingmanagementservice.formatting.DateFormatUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceService;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryDataView;

@Service
public class LicenceContinuationApplicationOverviewService {

  static final String SUBMITTED_BY_USER_PURPOSE = "Fetch submitted by user for application overview";

  private final LicenceService licenceService;
  private final EnergyPortalUserService energyPortalUserService;

  LicenceContinuationApplicationOverviewService(
      LicenceService licenceService,
      EnergyPortalUserService energyPortalUserService
  ) {
    this.licenceService = licenceService;
    this.energyPortalUserService = energyPortalUserService;
  }

  LicenceContinuationApplicationContext getApplicationContext(
      LicenceContinuationApplicationDetail applicationDetail,
      Licence licence
  ) {
    if (applicationDetail.getStatus() == ApplicationStatus.DRAFT) {
      return getDraftApplicationContext(applicationDetail, licence);
    }

    var submittedByUser = energyPortalUserService.getByWuaId(
        WebUserAccountId.from(applicationDetail.getSubmittedByWuaId()),
        SUBMITTED_BY_USER_PURPOSE
    );

    var summaryDataView = SummaryDataView.newBuilder()
        .addStringValue("Submitted by", submittedByUser.displayNameAndEmail())
        .addStringValue("Submission date",
            DateFormatUtil.convertToDisplayTextWithTime(applicationDetail.getSubmittedDatetime()))
        .build();

    return new LicenceContinuationApplicationContext(
        applicationDetail.getLicenceContinuationApplication().getApplicationReference(),
        licenceService.getLicencePageCaption(licence),
        List.of(summaryDataView)
    );
  }

  private LicenceContinuationApplicationContext getDraftApplicationContext(
      LicenceContinuationApplicationDetail applicationDetail,
      Licence licence
  ) {
    var summaryDataView = SummaryDataView.newBuilder()
        .addStringValue("Created", DateFormatUtil.convertToDisplayTextWithTime(applicationDetail.getCreatedDatetime()))
        .build();

    return new LicenceContinuationApplicationContext(
        licence.getLicenceReference(),
        licenceService.getLicencePageCaption(licence),
        List.of(summaryDataView)
    );
  }
}