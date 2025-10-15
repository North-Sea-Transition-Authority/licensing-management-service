package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.extendjourney;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.Errors;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;

@ExtendWith(MockitoExtension.class)
class LicenceScheduleExtensionSubmissionServiceTest {

  @Mock
  LicenceScheduleExtensionFormService licenceScheduleExtensionFormService;

  @Mock
  LicenceScheduleExtensionFormValidator licenceScheduleExtensionFormValidator;

  @InjectMocks
  LicenceScheduleExtensionSubmissionService licenceScheduleExtensionSubmissionService;

  @Test
  void IsSectionSubmittable() {
    when(licenceScheduleExtensionFormService.getLicenceScheduleExtensionForm(any())).thenReturn(new LicenceScheduleExtensionForm());
    when(licenceScheduleExtensionFormValidator.isValid(any(LicenceScheduleExtensionForm.class),
        any(Errors.class))).thenReturn(true);

    boolean result = licenceScheduleExtensionSubmissionService.isSectionSubmittable(
        new ScheduleWorkProgrammeApplicationDetail(UUID.randomUUID()));
    assertTrue(result);
  }

  @Test
  void IsNotSectionSubmittable() {
    when(licenceScheduleExtensionFormService.getLicenceScheduleExtensionForm(any())).thenReturn(new LicenceScheduleExtensionForm());
    when(licenceScheduleExtensionFormValidator.isValid(any(LicenceScheduleExtensionForm.class),
        any(Errors.class))).thenReturn(false);

    boolean result = licenceScheduleExtensionSubmissionService.isSectionSubmittable(
        new ScheduleWorkProgrammeApplicationDetail(UUID.randomUUID()));
    assertFalse(result);
  }
}