package uk.co.nstauthority.licensingmanagementservice.licence.continuation.requirementjourney;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;

@ExtendWith(MockitoExtension.class)
class LicenceContinuationLicenceOperatorsSubmissionServiceTest {

  @Mock
  private LicenceContinuationLicenceOperatorsService service;

  @Mock
  private LicenceContinuationLicenceOperatorsValidator validator;

  @InjectMocks
  private LicenceContinuationLicenceOperatorsSubmissionService submissionService;

  @Test
  void isSectionSubmittable_returnsTrueWhenValidatorPasses() {
    var applicationDetail = new LicenceContinuationApplicationDetail();
    var form = new LicenceContinuationLicenceOperatorsForm();

    when(service.getSubareasForApplication(applicationDetail)).thenReturn(List.of());
    when(service.hasMissingOperators(any())).thenReturn(true);
    when(service.getLicenceContinuationLicenceOperatorsForm(applicationDetail)).thenReturn(form);
    when(validator.isValid(any(), anyBoolean())).thenReturn(true);

    assertThat(submissionService.isSectionSubmittable(applicationDetail)).isTrue();
  }

  @Test
  void isSectionSubmittable_returnsFalseWhenValidatorFails() {
    var applicationDetail = new LicenceContinuationApplicationDetail();
    var form = new LicenceContinuationLicenceOperatorsForm();

    when(service.getSubareasForApplication(applicationDetail)).thenReturn(List.of());
    when(service.hasMissingOperators(any())).thenReturn(false);
    when(service.getLicenceContinuationLicenceOperatorsForm(applicationDetail)).thenReturn(form);
    when(validator.isValid(any(), anyBoolean())).thenReturn(false);

    assertThat(submissionService.isSectionSubmittable(applicationDetail)).isFalse();
  }
}