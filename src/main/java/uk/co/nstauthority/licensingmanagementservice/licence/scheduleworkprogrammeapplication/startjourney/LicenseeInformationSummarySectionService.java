package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.startjourney;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
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

  public LicenseeInformationSummarySectionService(OrganisationUnitQueryService organisationUnitQueryService) {
    this.organisationUnitQueryService = organisationUnitQueryService;
  }

  @Override
  public Optional<SummarySection> getSummarySection(
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail,
      ServiceUserDetail user
  ) {
    var appDetailsSummaryItem = getLicenseeInformationSummaryItem(scheduleWorkProgrammeApplicationDetail);

    var summarySection = new SummarySection(SECTION_DISPLAY_ORDER, List.of(appDetailsSummaryItem));

    return Optional.of(summarySection);
  }

  private SummaryItem getLicenseeInformationSummaryItem(
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

    var summaryCard = SummaryCard.simpleSummaryCardWithHeading("Licensee information", summaryDataView);

    return SummaryItem.withCard(SECTION_NAME, summaryCard);
  }

}