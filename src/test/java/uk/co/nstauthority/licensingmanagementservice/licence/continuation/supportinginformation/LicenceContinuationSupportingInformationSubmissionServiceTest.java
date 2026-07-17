package uk.co.nstauthority.licensingmanagementservice.licence.continuation.supportinginformation;

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
class LicenceContinuationSupportingInformationSubmissionServiceTest {

  @Mock
  private LicenceContinuationSupportingInformationService supportingInformationService;

  @Mock
  private LicenceContinuationSupportingInformationValidator validator;

  @InjectMocks
  private LicenceContinuationSupportingInformationSubmissionService submissionService;

  @Test
  void isSectionSubmittable_whenFormValid_returnsTrue() {
    var licenceContinuationApplicationDetail = new LicenceContinuationApplicationDetail();
    var form = new LicenceContinuationSupportingInformationForm();

    when(supportingInformationService.getSupportingInformationForm(licenceContinuationApplicationDetail))
        .thenReturn(form);
    when(validator.isValid(eq(form), any(BindingResult.class))).thenReturn(true);

    assertThat(submissionService.isSectionSubmittable(licenceContinuationApplicationDetail)).isTrue();
  }

  @Test
  void isSectionSubmittable_whenFormInvalid_returnsFalse() {
    var licenceContinuationApplicationDetail = new LicenceContinuationApplicationDetail();
    var form = new LicenceContinuationSupportingInformationForm();

    when(supportingInformationService.getSupportingInformationForm(licenceContinuationApplicationDetail))
        .thenReturn(form);
    when(validator.isValid(eq(form), any(BindingResult.class))).thenReturn(false);

    assertThat(submissionService.isSectionSubmittable(licenceContinuationApplicationDetail)).isFalse();
  }
}
