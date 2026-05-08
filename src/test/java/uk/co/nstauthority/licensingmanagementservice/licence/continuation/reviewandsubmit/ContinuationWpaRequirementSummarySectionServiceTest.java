package uk.co.nstauthority.licensingmanagementservice.licence.continuation.reviewandsubmit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.requirementjourney.LicenceContinuationWpaRequirementRequest;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.requirementjourney.LicenceContinuationWpaRequirementService;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryCardType;
import uk.co.nstauthority.licensingmanagementservice.summary.SummarySection;

@ExtendWith(MockitoExtension.class)
class ContinuationWpaRequirementSummarySectionServiceTest {

  @Mock
  private LicenceContinuationWpaRequirementService licenceContinuationWpaRequirementService;

  @InjectMocks
  private ContinuationWpaRequirementSummarySectionService continuationWpaRequirementSummarySectionService;

  private LicenceContinuationApplicationDetail licenceContinuationApplicationDetail;
  private ServiceUserDetail user;

  @BeforeEach
  void setUp() {
    licenceContinuationApplicationDetail = new LicenceContinuationApplicationDetail();
    user = mock(ServiceUserDetail.class);
  }

  @Test
  void getSummarySection_withWpaRequirement_returnsWpaSummaryCardOnly() {
    var wpaRequest = createWpaRequirementRequest();

    when(licenceContinuationWpaRequirementService.getWorkProgrammeActivitiesRequirementRequest(licenceContinuationApplicationDetail))
        .thenReturn(Optional.of(wpaRequest));

    Optional<SummarySection> result = continuationWpaRequirementSummarySectionService.getSummarySection(
        licenceContinuationApplicationDetail,
        user
    );

    assertThat(result).isPresent();
    var summarySection = result.get();

    assertThat(summarySection.displayOrder()).isEqualTo(ContinuationWpaRequirementSummarySectionService.SECTION_DISPLAY_ORDER);

    var summaryItem = summarySection.summaryItems().getFirst();
    assertThat(summaryItem.displayName()).isEqualTo(ContinuationWpaRequirementSummarySectionService.SECTION_NAME);

    assertThat(summaryItem.summaryCards()).hasSize(1);
    assertThat(summaryItem.summaryCards().getFirst().displayName()).isEqualTo("Completed and evidenced");
  }

  @Test
  void getSummarySection_withNoRequirements_returnsEmptySummaryCardsList() {
    when(licenceContinuationWpaRequirementService.getWorkProgrammeActivitiesRequirementRequest(licenceContinuationApplicationDetail))
        .thenReturn(Optional.empty());

    Optional<SummarySection> result = continuationWpaRequirementSummarySectionService.getSummarySection(
        licenceContinuationApplicationDetail,
        user
    );

    assertThat(result).isPresent();
    var summaryItem = result.get().summaryItems().getFirst();

    assertThat(summaryItem.summaryCards()).hasSize(1);
    assertThat(summaryItem.summaryCards().getFirst().summaryCardType()).isEqualTo(SummaryCardType.EMPTY_SUMMARY);
  }

  private LicenceContinuationWpaRequirementRequest createWpaRequirementRequest() {
    var request = mock(LicenceContinuationWpaRequirementRequest.class);
    when(request.getWorkProgrammeActivitiesCompletionStatus()).thenReturn(true);
    when(request.getFurtherInformation()).thenReturn("Further info");
    return request;
  }
}