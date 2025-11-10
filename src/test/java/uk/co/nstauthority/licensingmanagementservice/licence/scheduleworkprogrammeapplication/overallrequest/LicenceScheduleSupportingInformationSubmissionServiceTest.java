package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overallrequest;

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
import org.springframework.validation.BindingResult;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;

@ExtendWith(MockitoExtension.class)
class LicenceScheduleSupportingInformationSubmissionServiceTest {

    @Mock
    LicenceScheduleSupportingInformationService licenceScheduleSupportingInformationService;

    @Mock
    LicenceScheduleSupportingInformationFormValidator licenceScheduleSupportingInformationFormValidator;

    @InjectMocks
    LicenceScheduleSupportingInformationSubmissionService licenceScheduleSupportingInformationSubmissionService;

    @Test
    void IsSectionSubmittable() {
      when(licenceScheduleSupportingInformationService.getLicenceScheduleRequestForm(any()))
          .thenReturn(new LicenceScheduleSupportingInformationForm());

      when(licenceScheduleSupportingInformationFormValidator.isValid(
          any(BindingResult.class),
          any(ScheduleWorkProgrammeApplicationDetail.class)
      )).thenReturn(true);

      boolean result = licenceScheduleSupportingInformationSubmissionService.isSectionSubmittable(
          new ScheduleWorkProgrammeApplicationDetail(UUID.randomUUID()));

      assertTrue(result);
    }

  @Test
  void IsNotSectionSubmittable() {
    when(licenceScheduleSupportingInformationService.getLicenceScheduleRequestForm(any()))
        .thenReturn(new LicenceScheduleSupportingInformationForm());

    when(licenceScheduleSupportingInformationFormValidator.isValid(
        any(BindingResult.class),
        any(ScheduleWorkProgrammeApplicationDetail.class)
    )).thenReturn(false);

    boolean result = licenceScheduleSupportingInformationSubmissionService.isSectionSubmittable(
        new ScheduleWorkProgrammeApplicationDetail(UUID.randomUUID()));

    assertFalse(result);
  }
}