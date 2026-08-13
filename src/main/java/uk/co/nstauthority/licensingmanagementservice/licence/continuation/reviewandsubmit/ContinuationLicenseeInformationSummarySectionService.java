package uk.co.nstauthority.licensingmanagementservice.licence.continuation.reviewandsubmit;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.energyportal.fox.FoxRedirectService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationService;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.externalcontributorjourney.LicenceContinuationExternalContributorService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleStateService;
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
  private final LicenceContinuationExternalContributorService licenceContinuationExternalContributorService;
  private final LicenceContinuationService licenceContinuationService;
  private final LicenceScheduleStateService licenceScheduleStateService;

  public ContinuationLicenseeInformationSummarySectionService(
      OrganisationUnitQueryService organisationUnitQueryService,
      FoxRedirectService foxRedirectService,
      LicenceContinuationExternalContributorService licenceContinuationExternalContributorService,
      LicenceContinuationService licenceContinuationService,
      LicenceScheduleStateService licenceScheduleStateService
  ) {
    this.organisationUnitQueryService = organisationUnitQueryService;
    this.foxRedirectService = foxRedirectService;
    this.licenceContinuationExternalContributorService = licenceContinuationExternalContributorService;
    this.licenceContinuationService = licenceContinuationService;
    this.licenceScheduleStateService = licenceScheduleStateService;
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
    var externalContributorSummaryCard = getExternalContributorSummaryCard(licenceContinuationApplicationDetail);

    return SummaryItem.withCards(
        SECTION_NAME,
        List.of(licenceSummaryCard, licenseeInformationSummaryCard, externalContributorSummaryCard)
    );
  }

  private SummaryCard getExternalContributorSummaryCard(
      LicenceContinuationApplicationDetail licenceContinuationApplicationDetail
  ) {
    var form = licenceContinuationExternalContributorService.getExternalContributorForm(
        licenceContinuationApplicationDetail
    );

    var summaryDataView = SummaryDataView.newBuilder()
        .addStringValue(
            "External contributors required",
            form.getAddExternalContributors()
        )
        .build();

    return SummaryCard.simpleSummaryCardWithHeading("External contributors", summaryDataView);
  }

  private SummaryCard getLicenceSummaryCard(LicenceContinuationApplicationDetail licenceContinuationApplicationDetail) {
    var licence = licenceContinuationApplicationDetail.getLicence();
    var scheduleState = licenceContinuationService.resolveScheduleState(licenceContinuationApplicationDetail);

    var currentTermPhaseDisplay = licenceScheduleStateService.formatTermPhaseDisplay(
        scheduleState.currentTerm(), scheduleState.currentPhase());
    var nextTermPhaseDisplay = licenceScheduleStateService.formatTermPhaseDisplay(
        scheduleState.nextTerm(), scheduleState.nextPhase());

    var summaryDataView = SummaryDataView.newBuilder()
        .addStringValue("Licence reference", licence.getLicenceReference());

    if (!licence.getType().isManagedByLms()) {
      summaryDataView.addExternalUrlValue(
          "View licence",
          new ExternalUrlView("View licence in PEARS", foxRedirectService.getViewPearsLicenceUrl(licence))
      );
    }

    if (currentTermPhaseDisplay != null) {
      summaryDataView.addStringValue("Current term/phase", currentTermPhaseDisplay);
    }
    if (nextTermPhaseDisplay != null) {
      summaryDataView.addStringValue("Next term/phase", nextTermPhaseDisplay);
    }

    return SummaryCard.simpleSummaryCardWithHeading("Licence information", summaryDataView.build());
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