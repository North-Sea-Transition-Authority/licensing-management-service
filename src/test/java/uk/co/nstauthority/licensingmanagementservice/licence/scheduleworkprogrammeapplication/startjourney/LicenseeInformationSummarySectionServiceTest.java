package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.startjourney;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryDataView;

@ExtendWith(MockitoExtension.class)
class LicenseeInformationSummarySectionServiceTest {
  @Mock
  private OrganisationUnitQueryService organisationUnitQueryService;

  @InjectMocks
  private LicenseeInformationSummarySectionService licenseeInformationSummarySectionService;

  @Test
  void getSummarySection_returnsSectionWithOrganisationName() {
    var scheduleDetail = new ScheduleWorkProgrammeApplicationDetail();
    scheduleDetail.setResponsibleOrganisationUnitId(1);
    scheduleDetail.setAllLicenseesPermissionConfirmed(true);

    when(organisationUnitQueryService.getOrganisationUnitNameById(1))
        .thenReturn(Optional.of("Test Organisation"));

    var result = licenseeInformationSummarySectionService.getSummarySection(scheduleDetail, null).get();

    assertThat(result.displayOrder()).isEqualTo(10);

    var summaryItem = result.summaryItems().getFirst();
    assertThat(summaryItem.displayName()).isEqualTo("General details");

    var summaryCard = summaryItem.summaryCards().getFirst();
    assertThat(summaryCard.displayName()).isEqualTo("Licensee information");
    assertThat(summaryCard.summaryData()).isEqualTo(
        SummaryDataView.newBuilder()
            .addStringValue("Who is the licensee for this application?", "Test Organisation")
            .addStringValue("Have you confirmed this request is made on behalf of all licensees?", true)
            .build()
    );
  }
}