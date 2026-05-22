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
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.requirementjourney.LicenceContinuationOtherRequirementRequest;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.requirementjourney.LicenceContinuationOtherRequirementService;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.requirementjourney.OtherRequirementsVisibility;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.requirementjourney.OtherRequirementsVisibilityResolverService;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryCardType;
import uk.co.nstauthority.licensingmanagementservice.summary.SummarySection;

@ExtendWith(MockitoExtension.class)
class ContinuationRequirementSummarySectionServiceTest {

  @Mock
  private LicenceContinuationOtherRequirementService licenceContinuationOtherRequirementService;

  @Mock
  private OtherRequirementsVisibilityResolverService otherRequirementsVisibilityResolverService;

  @InjectMocks
  private ContinuationRequirementSummarySectionService continuationRequirementSummarySectionService;

  private LicenceContinuationApplicationDetail licenceContinuationApplicationDetail;
  private ServiceUserDetail user;

  @BeforeEach
  void setUp() {
    licenceContinuationApplicationDetail = new LicenceContinuationApplicationDetail();
    user = mock(ServiceUserDetail.class);
  }

  @Test
  void getSummarySection_whenDevelopmentConsentShown_assertAllThreeCardsReturned() {
    var otherRequest = createOtherRequirementRequest(true);

    when(licenceContinuationOtherRequirementService.getLicenceContinuationApplicationDetail(licenceContinuationApplicationDetail))
        .thenReturn(Optional.of(otherRequest));
    when(otherRequirementsVisibilityResolverService.resolveVisibility(licenceContinuationApplicationDetail))
        .thenReturn(new OtherRequirementsVisibility(true, true, true));

    Optional<SummarySection> result = continuationRequirementSummarySectionService.getSummarySection(
        licenceContinuationApplicationDetail,
        user
    );

    assertThat(result).isPresent();
    var summarySection = result.get();

    assertThat(summarySection.displayOrder()).isEqualTo(ContinuationRequirementSummarySectionService.SECTION_DISPLAY_ORDER);

    var summaryItem = summarySection.summaryItems().getFirst();
    assertThat(summaryItem.displayName()).isEqualTo(ContinuationRequirementSummarySectionService.SECTION_NAME);

    assertThat(summaryItem.summaryCards()).hasSize(3);
    assertThat(summaryItem.summaryCards().get(0).displayName()).isEqualTo("Financial Capacity");
    assertThat(summaryItem.summaryCards().get(1).displayName()).isEqualTo("Relinquishment");
    assertThat(summaryItem.summaryCards().get(2).displayName()).isEqualTo("Development Consent");
  }

  @Test
  void getSummarySection_whenDevelopmentConsentNotShown_assertTwoCardsReturnedWithoutDevConsent() {
    var otherRequest = createOtherRequirementRequest(false);

    when(licenceContinuationOtherRequirementService.getLicenceContinuationApplicationDetail(licenceContinuationApplicationDetail))
        .thenReturn(Optional.of(otherRequest));
    when(otherRequirementsVisibilityResolverService.resolveVisibility(licenceContinuationApplicationDetail))
        .thenReturn(new OtherRequirementsVisibility(true, true, false));

    Optional<SummarySection> result = continuationRequirementSummarySectionService.getSummarySection(
        licenceContinuationApplicationDetail,
        user
    );

    assertThat(result).isPresent();
    var summaryItem = result.get().summaryItems().getFirst();

    assertThat(summaryItem.summaryCards()).hasSize(2);
    assertThat(summaryItem.summaryCards().get(0).displayName()).isEqualTo("Financial Capacity");
    assertThat(summaryItem.summaryCards().get(1).displayName()).isEqualTo("Relinquishment");
  }

  @Test
  void getSummarySection_withNoRequirements_returnsEmptySummaryCardsList() {
    when(licenceContinuationOtherRequirementService.getLicenceContinuationApplicationDetail(licenceContinuationApplicationDetail))
        .thenReturn(Optional.empty());
    when(otherRequirementsVisibilityResolverService.resolveVisibility(licenceContinuationApplicationDetail))
        .thenReturn(new OtherRequirementsVisibility(true, true, true));

    Optional<SummarySection> result = continuationRequirementSummarySectionService.getSummarySection(
        licenceContinuationApplicationDetail,
        user
    );

    assertThat(result).isPresent();
    var summaryItem = result.get().summaryItems().getFirst();

    assertThat(summaryItem.summaryCards()).hasSize(1);
    assertThat(summaryItem.summaryCards().getFirst().summaryCardType()).isEqualTo(SummaryCardType.EMPTY_SUMMARY);
  }

  private LicenceContinuationOtherRequirementRequest createOtherRequirementRequest(boolean withDevConsent) {
    var request = mock(LicenceContinuationOtherRequirementRequest.class);
    when(request.getFinancialCapacityEvidenceSubmissionStatus()).thenReturn(true);
    when(request.getRelinquishmentRequirementStatus()).thenReturn(true);
    if (withDevConsent) {
      when(request.getDevelopmentConsentGrantStatus()).thenReturn(true);
    }
    return request;
  }
}