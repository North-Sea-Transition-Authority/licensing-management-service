package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
class LicenceWorkProgrammeAmendmentSubmissionServiceTest {

  @Mock
  private LicenceWorkProgrammeAmendmentFormService licenceWorkProgrammeAmendmentFormService;

  @Mock
  private LicenceWorkProgrammeAmendmentFormValidator licenceWorkProgrammeAmendmentFormValidator;

  @InjectMocks
  private LicenceWorkProgrammeAmendmentSubmissionService licenceWorkProgrammeAmendmentSubmissionService;

  @Test
  void IsSectionWorkProgrammeActivitySubmittable() {
    when(licenceWorkProgrammeAmendmentFormService.getLicenceWorkProgrammeActivityAmendmentForm(any())).thenReturn(
        new LicenceWorkProgrammeAmendmentForm());
    when(licenceWorkProgrammeAmendmentFormValidator.isValid(any(LicenceWorkProgrammeAmendmentForm.class),
        any(Errors.class))).thenReturn(true);

    boolean result = licenceWorkProgrammeAmendmentSubmissionService.isAmendmentSectionSubmittable(
        new ScheduleWorkProgrammeApplicationDetail(UUID.randomUUID()));
    assertTrue(result);
  }

  @Test
  void IsNotSectionWorkProgrammeActivitySubmittable() {
    when(licenceWorkProgrammeAmendmentFormService.getLicenceWorkProgrammeActivityAmendmentForm(any())).thenReturn(
        new LicenceWorkProgrammeAmendmentForm());
    when(licenceWorkProgrammeAmendmentFormValidator.isValid(any(LicenceWorkProgrammeAmendmentForm.class),
        any(Errors.class))).thenReturn(false);

    boolean result = licenceWorkProgrammeAmendmentSubmissionService.isAmendmentSectionSubmittable(
        new ScheduleWorkProgrammeApplicationDetail(UUID.randomUUID()));
    assertFalse(result);
  }

}