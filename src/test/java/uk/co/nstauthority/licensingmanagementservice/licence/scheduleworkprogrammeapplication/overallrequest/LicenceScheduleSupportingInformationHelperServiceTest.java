package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overallrequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney.LicenceWorkProgrammeAmendmentRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.extendjourney.LicenceScheduleExtensionService;

@ExtendWith(MockitoExtension.class)
class LicenceScheduleSupportingInformationHelperServiceTest {

  @Mock
  private LicenceScheduleExtensionService licenceScheduleExtensionService;

  @Mock
  private LicenceWorkProgrammeAmendmentRepository licenceWorkProgrammeAmendmentRepository;

  @InjectMocks
  private LicenceScheduleSupportingInformationHelperService licenceScheduleSupportingInformationHelperService;

  @Test
  void isExtensionOrAmendment_OnlyExtensionRequested() {
    ScheduleWorkProgrammeApplicationDetail detail = new ScheduleWorkProgrammeApplicationDetail();
    when(licenceScheduleExtensionService.isExtensionRequested(detail)).thenReturn(true);

    boolean result = licenceScheduleSupportingInformationHelperService.isExtensionOrAmendment(detail);

    assertTrue(result);

    verify(licenceScheduleExtensionService, times(1)).isExtensionRequested(detail);
  }

  @Test
  void isExtensionOrAmendment_OnlyAmendmentRequested() {
    ScheduleWorkProgrammeApplicationDetail detail = new ScheduleWorkProgrammeApplicationDetail();

    when(licenceScheduleExtensionService.isExtensionRequested(detail)).thenReturn(false);

    when(licenceWorkProgrammeAmendmentRepository
             .existsByScheduleWorkProgrammeApplicationDetailsAndWorkProgrammeCompletionDateChangeRequestedTrue(detail))
        .thenReturn(true);

    boolean result = licenceScheduleSupportingInformationHelperService.isExtensionOrAmendment(detail);

    assertThat(result).isTrue();

    verify(licenceScheduleExtensionService, times(1)).isExtensionRequested(detail);
    verify(licenceWorkProgrammeAmendmentRepository, times(1))
        .existsByScheduleWorkProgrammeApplicationDetailsAndWorkProgrammeCompletionDateChangeRequestedTrue(detail);
  }

}