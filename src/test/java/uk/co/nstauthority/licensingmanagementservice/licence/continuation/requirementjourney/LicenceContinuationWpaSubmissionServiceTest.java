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
class LicenceContinuationWpaSubmissionServiceTest {

  @Mock
  private LicenceContinuationWpaRequirementService requirementService;

  @Mock
  private LicenceContinuationWpaRequirementValidator validator;

  @InjectMocks
  private LicenceContinuationWpaSubmissionService submissionService;

  @Test
  void isSectionSubmittable_Valid() {
    var detail = new LicenceContinuationApplicationDetail();
    var form = new LicenceContinuationWpaRequirementForm();

    when(requirementService.getLicenceContinuationWorkProgrammeActivitiesRequirementForm(detail))
        .thenReturn(form);
    when(validator.isValid(eq(form), any(BindingResult.class)))
        .thenReturn(true);

    boolean result = submissionService.isSectionSubmittable(detail);

    assertThat(result).isTrue();
  }

  @Test
  void isSectionSubmittable_Invalid() {
    var detail = new LicenceContinuationApplicationDetail();
    var form = new LicenceContinuationWpaRequirementForm();

    when(requirementService.getLicenceContinuationWorkProgrammeActivitiesRequirementForm(detail))
        .thenReturn(form);
    when(validator.isValid(eq(form), any(BindingResult.class)))
        .thenReturn(false);

    boolean result = submissionService.isSectionSubmittable(detail);

    assertThat(result).isFalse();
  }
}