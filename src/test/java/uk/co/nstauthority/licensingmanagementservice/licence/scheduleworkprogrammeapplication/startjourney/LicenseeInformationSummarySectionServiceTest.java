package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.startjourney;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.energyportal.fox.FoxRedirectService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceSchedule;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.summary.ExternalUrlView;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryDataView;

@ExtendWith(MockitoExtension.class)
class LicenseeInformationSummarySectionServiceTest {

  private static final String VIEW_PEARS_LICENCE_URL =
      "https://test.example.com/fox/nsta/LMS_REDIRECT/view-licence?LICENCE_TYPE=P&LICENCE_NO=123";

  private static final Licence LICENCE = LicenceTestUtil.builder()
      .withLicencePrefix("P")
      .withLicenceNumber("123")
      .withLicenceReference("P 123")
      .build();

  private static final LicenceSchedule LICENCE_SCHEDULE =
      LicenceScheduleTestUtil.createLicenceSchedule(LICENCE);

  private static final LicenceScheduleDetail LICENCE_SCHEDULE_DETAIL =
      LicenceScheduleTestUtil.createLicenceScheduleDetail(LICENCE_SCHEDULE);

  @Mock
  private OrganisationUnitQueryService organisationUnitQueryService;

  @Mock
  private FoxRedirectService foxRedirectService;

  @InjectMocks
  private LicenseeInformationSummarySectionService licenseeInformationSummarySectionService;

  @Test
  void getSummarySection_returnsSectionWithLicenceCardAndLicenseeCard() {
    var scheduleDetail = ScheduleWorkProgrammeApplicationDetailTestUtil
        .createScheduleWorkProgrammeApplicationDetail(LICENCE_SCHEDULE_DETAIL);
    scheduleDetail.setResponsibleOrganisationUnitId(1);
    scheduleDetail.setAllLicenseesPermissionConfirmed(true);

    when(foxRedirectService.getViewPearsLicenceUrl(LICENCE)).thenReturn(VIEW_PEARS_LICENCE_URL);
    when(organisationUnitQueryService.getOrganisationUnitNameById(1))
        .thenReturn(Optional.of("Test Organisation"));

    var result = licenseeInformationSummarySectionService.getSummarySection(scheduleDetail, null).get();

    assertThat(result.displayOrder()).isEqualTo(10);

    var summaryItem = result.summaryItems().getFirst();
    assertThat(summaryItem.displayName()).isEqualTo("General details");
    assertThat(summaryItem.summaryCards()).hasSize(2);

    var licenceSummaryCard = summaryItem.summaryCards().getFirst();
    assertThat(licenceSummaryCard.displayName()).isEqualTo("Licence information");
    assertThat(licenceSummaryCard.summaryData()).isEqualTo(
        SummaryDataView.newBuilder()
            .addStringValue("Licence reference", "P 123")
            .addExternalUrlValue("View licence", new ExternalUrlView("View licence in PEARS", VIEW_PEARS_LICENCE_URL))
            .build()
    );

    var licenseeInformationSummaryCard = summaryItem.summaryCards().get(1);
    assertThat(licenseeInformationSummaryCard.displayName()).isEqualTo("Licensee information");
    assertThat(licenseeInformationSummaryCard.summaryData()).isEqualTo(
        SummaryDataView.newBuilder()
            .addStringValue("Who is the licensee for this application?", "Test Organisation")
            .addStringValue("Have you confirmed this request is made on behalf of all licensees?", true)
            .build()
    );
  }
}