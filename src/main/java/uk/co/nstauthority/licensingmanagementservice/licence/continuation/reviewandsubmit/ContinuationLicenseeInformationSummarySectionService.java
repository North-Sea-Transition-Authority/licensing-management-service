package uk.co.nstauthority.licensingmanagementservice.licence.continuation.reviewandsubmit;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.energyportal.fox.FoxRedirectService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.summary.ExternalUrlView;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryCard;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryDataView;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryItem;
import uk.co.nstauthority.licensingmanagementservice.summary.SummarySection;
import uk.co.nstauthority.licensingmanagementservice.summary.SummarySectionService;

@Service
public class ContinuationLicenseeInformationSummarySectionService
    implements SummarySectionService<LicenceContinuationApplicationDetail> {

  public static final String SECTION_NAME = "General details";
  public static final int SECTION_DISPLAY_ORDER = 10;
  private final OrganisationUnitQueryService organisationUnitQueryService;
  private final FoxRedirectService foxRedirectService;

  public ContinuationLicenseeInformationSummarySectionService(OrganisationUnitQueryService organisationUnitQueryService,
                                                              FoxRedirectService foxRedirectService
  ) {
    this.organisationUnitQueryService = organisationUnitQueryService;
    this.foxRedirectService = foxRedirectService;
  }

  @Override
  public Optional<SummarySection> getSummarySection(
      LicenceContinuationApplicationDetail licenceContinuationApplicationDetail,
      ServiceUserDetail user
  ) {
    var generalDetailsSummaryItem = getGeneralDetailsSummaryItem(licenceContinuationApplicationDetail);

    var summarySection = new SummarySection(SECTION_DISPLAY_ORDER, List.of(generalDetailsSummaryItem));

    return Optional.of(summarySection);
  }

  private SummaryItem getGeneralDetailsSummaryItem(
      LicenceContinuationApplicationDetail licenceContinuationApplicationDetail
  ) {
    var licenceSummaryCard = getLicenceSummaryCard(licenceContinuationApplicationDetail);
    var licenseeInformationSummaryCard = getLicenseeInformationSummaryCard(licenceContinuationApplicationDetail);

    return SummaryItem.withCards(SECTION_NAME, List.of(licenceSummaryCard, licenseeInformationSummaryCard));
  }

  private SummaryCard getLicenceSummaryCard(LicenceContinuationApplicationDetail licenceContinuationApplicationDetail) {
    var licence = licenceContinuationApplicationDetail.getLicenceContinuationApplication().getLicenceSchedule().getLicence();
    var viewPearsLicenceUrl = foxRedirectService.getViewPearsLicenceUrl(licence);
    var summaryDataView = SummaryDataView.newBuilder()
        .addStringValue("Licence reference", licence.getLicenceReference())
        .addExternalUrlValue("View licence", new ExternalUrlView("View licence in PEARS", viewPearsLicenceUrl))
        .build();

    return SummaryCard.simpleSummaryCardWithHeading("Licence information", summaryDataView);
  }

  private SummaryCard getLicenseeInformationSummaryCard(
      LicenceContinuationApplicationDetail licenceContinuationApplicationDetail
  ) {
    var responsibleOrganisationUnitName = organisationUnitQueryService.getOrganisationUnitNameById(
        licenceContinuationApplicationDetail.getResponsibleOrganisationUnitId()
    ).orElse("");

    var summaryDataView = SummaryDataView.newBuilder()
        .addStringValue("Who is the licensee for this application?", responsibleOrganisationUnitName)
        .build();

    return SummaryCard.simpleSummaryCardWithHeading("Licensee information", summaryDataView);
  }
}