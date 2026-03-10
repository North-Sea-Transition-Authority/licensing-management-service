package uk.co.nstauthority.licensingmanagementservice.licence.continuation.reviewandsubmit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryDataView;

@ExtendWith(MockitoExtension.class)
class ContinuationLicenseeInformationSummarySectionServiceTest {

  @Mock
  private OrganisationUnitQueryService organisationUnitQueryService;

  @InjectMocks
  private ContinuationLicenseeInformationSummarySectionService continuationLicenseeInformationSummarySectionService;

  @Test
  void getSummarySection_returnsSectionWithOrganisationName() {
    var licenceContinuationApplicationDetail = new LicenceContinuationApplicationDetail();
    licenceContinuationApplicationDetail.setResponsibleOrganisationUnitId(1);

    when(organisationUnitQueryService.getOrganisationUnitNameById(1)).thenReturn(Optional.of("Test Organisation"));

    var result = continuationLicenseeInformationSummarySectionService.getSummarySection(licenceContinuationApplicationDetail, null).get();

    assertThat(result.displayOrder()).isEqualTo(10);

    var summaryItem = result.summaryItems().getFirst();
    assertThat(summaryItem.displayName()).isEqualTo("General details");

    var summaryCard = summaryItem.summaryCards().getFirst();
    assertThat(summaryCard.displayName()).isEqualTo("Licensee information");
    assertThat(summaryCard.summaryData()).isEqualTo(
        SummaryDataView.newBuilder()
            .addStringValue("Who is the licensee for this application?", "Test Organisation")
            .build()
    );
  }
}