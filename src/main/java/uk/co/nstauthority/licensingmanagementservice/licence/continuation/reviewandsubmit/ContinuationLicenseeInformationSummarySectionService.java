package uk.co.nstauthority.licensingmanagementservice.licence.continuation.reviewandsubmit;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;
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

  public ContinuationLicenseeInformationSummarySectionService(OrganisationUnitQueryService organisationUnitQueryService) {
    this.organisationUnitQueryService = organisationUnitQueryService;
  }

  @Override
  public Optional<SummarySection> getSummarySection(
      LicenceContinuationApplicationDetail licenceContinuationApplicationDetail,
      ServiceUserDetail user
  ) {
    var licenseeInformationSummaryItem = getLicenseeInformationSummaryItem(licenceContinuationApplicationDetail);

    var summarySection = new SummarySection(SECTION_DISPLAY_ORDER, List.of(licenseeInformationSummaryItem));

    return Optional.of(summarySection);
  }

  private SummaryItem getLicenseeInformationSummaryItem(
      LicenceContinuationApplicationDetail licenceContinuationApplicationDetail
  ) {
    var responsibleOrganisationUnitName = organisationUnitQueryService.getOrganisationUnitNameById(
        licenceContinuationApplicationDetail.getResponsibleOrganisationUnitId()
    ).orElse("");

    var summaryDataView = SummaryDataView.newBuilder()
        .addStringValue("Who is the licensee for this application?", responsibleOrganisationUnitName)
        .build();

    var summaryCard = SummaryCard.simpleSummaryCardWithHeading("Licensee information", summaryDataView);

    return SummaryItem.withCard(SECTION_NAME, summaryCard);
  }
}