package uk.co.nstauthority.licensingmanagementservice.licence.continuation.requirementjourney;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BindingResult;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;

@ExtendWith(MockitoExtension.class)
class LicenceContinuationOtherRequirementSubmissionServiceTest {

  @Mock
  private LicenceContinuationOtherRequirementService requirementService;

  @Mock
  private LicenceContinuationOtherRequirementValidator validator;

  @Mock
  private OtherRequirementsVisibilityResolverService visibilityResolverService;

  @Mock
  private OtherRequirementsVisibility otherRequirementsVisibility;

  @InjectMocks
  private LicenceContinuationOtherRequirementSubmissionService submissionService;

  @Test
  void isSectionSubmittable_Valid() {
    var licenceContinuationApplicationDetail = new LicenceContinuationApplicationDetail();
    var licenceContinuationOtherRequirementForm = new LicenceContinuationOtherRequirementForm();

    when(requirementService.getLicenceContinuationOtherRequirementForm(licenceContinuationApplicationDetail))
        .thenReturn(licenceContinuationOtherRequirementForm);

    when(visibilityResolverService.resolveVisibility(any(LicenceContinuationApplicationDetail.class))).thenReturn(otherRequirementsVisibility);

    when(validator.isValid(eq(licenceContinuationOtherRequirementForm), any(BindingResult.class), eq(otherRequirementsVisibility)))
        .thenReturn(true);

    boolean result = submissionService.isSectionSubmittable(licenceContinuationApplicationDetail);

    assertThat(result).isTrue();
  }

  @Test
  void isSectionSubmittable_Invalid() {
    var licenceContinuationApplicationDetail = new LicenceContinuationApplicationDetail();
    var licenceContinuationOtherRequirementForm = new LicenceContinuationOtherRequirementForm();

    when(requirementService.getLicenceContinuationOtherRequirementForm(licenceContinuationApplicationDetail))
        .thenReturn(licenceContinuationOtherRequirementForm);

    when(visibilityResolverService.resolveVisibility(any(LicenceContinuationApplicationDetail.class))).thenReturn(otherRequirementsVisibility);

    when(validator.isValid(eq(licenceContinuationOtherRequirementForm), any(BindingResult.class), eq(otherRequirementsVisibility)))
        .thenReturn(false);

    boolean result = submissionService.isSectionSubmittable(licenceContinuationApplicationDetail);

    assertThat(result).isFalse();
  }
}