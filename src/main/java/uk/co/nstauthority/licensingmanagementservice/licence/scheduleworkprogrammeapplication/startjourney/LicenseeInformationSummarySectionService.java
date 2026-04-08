package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.startjourney;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.energyportal.fox.FoxRedirectService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.summary.ExternalUrlView;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryCard;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryDataView;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryItem;
import uk.co.nstauthority.licensingmanagementservice.summary.SummarySection;
import uk.co.nstauthority.licensingmanagementservice.summary.SummarySectionService;

@Service
public class LicenseeInformationSummarySectionService implements SummarySectionService<ScheduleWorkProgrammeApplicationDetail> {

  public static final String SECTION_NAME = "General details";
  public static final int SECTION_DISPLAY_ORDER = 10;
  private final OrganisationUnitQueryService organisationUnitQueryService;
  private final FoxRedirectService foxRedirectService;

  public LicenseeInformationSummarySectionService(OrganisationUnitQueryService organisationUnitQueryService,
                                                  FoxRedirectService foxRedirectService
  ) {
    this.organisationUnitQueryService = organisationUnitQueryService;
    this.foxRedirectService = foxRedirectService;
  }

  @Override
  public Optional<SummarySection> getSummarySection(
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail,
      ServiceUserDetail user
  ) {
    var generalDetailsSummaryItem = getGeneralDetailsSummaryItem(scheduleWorkProgrammeApplicationDetail);

    var summarySection = new SummarySection(SECTION_DISPLAY_ORDER, List.of(generalDetailsSummaryItem));

    return Optional.of(summarySection);
  }

  private SummaryItem getGeneralDetailsSummaryItem(
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail
  ) {
    var licenceSummaryCard = getLicenceSummaryCard(scheduleWorkProgrammeApplicationDetail);
    var licenseeInformationSummaryCard = getLicenseeInformationSummaryCard(scheduleWorkProgrammeApplicationDetail);

    return SummaryItem.withCards(SECTION_NAME, List.of(licenceSummaryCard, licenseeInformationSummaryCard));
  }

  private SummaryCard getLicenceSummaryCard(ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail) {
    var licence = scheduleWorkProgrammeApplicationDetail.getScheduleWorkProgrammeApplication().getLicenceSchedule().getLicence();
    var viewPearsLicenceUrl = foxRedirectService.getViewPearsLicenceUrl(licence);
    var summaryDataView = SummaryDataView.newBuilder()
        .addStringValue("Licence reference", licence.getLicenceReference())
        .addExternalUrlValue("View licence", new ExternalUrlView("View licence in PEARS", viewPearsLicenceUrl))
        .build();

    return SummaryCard.simpleSummaryCardWithHeading("Licence information", summaryDataView);
  }

  private SummaryCard getLicenseeInformationSummaryCard(
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail
  ) {
    var responsibleOrganisationUnitName = organisationUnitQueryService.getOrganisationUnitNameById(
        scheduleWorkProgrammeApplicationDetail.getResponsibleOrganisationUnitId()
    ).orElse("");

    var summaryDataView = SummaryDataView.newBuilder()
        .addStringValue("Who is the licensee for this application?", responsibleOrganisationUnitName)
        .addStringValue("Have you confirmed this request is made on behalf of all licensees?",
            scheduleWorkProgrammeApplicationDetail.getAllLicenseesPermissionConfirmed())
        .build();

    return SummaryCard.simpleSummaryCardWithHeading("Licensee information", summaryDataView);
  }

}