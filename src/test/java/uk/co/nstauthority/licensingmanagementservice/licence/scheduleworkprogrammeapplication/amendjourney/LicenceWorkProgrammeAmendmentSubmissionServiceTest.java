package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;

@ExtendWith(MockitoExtension.class)
class LicenceWorkProgrammeAmendmentSubmissionServiceTest {

  @Mock
  private LicenceWorkProgrammeAmendmentRepository licenceWorkProgrammeAmendmentRepository;

  @InjectMocks
  private LicenceWorkProgrammeAmendmentSubmissionService licenceWorkProgrammeAmendmentSubmissionService;

  @Test
  void IsSectionWorkProgrammeActivitySubmittable() {
    when(licenceWorkProgrammeAmendmentRepository.findAllByScheduleWorkProgrammeApplicationDetails(
        any(ScheduleWorkProgrammeApplicationDetail.class)))
        .thenReturn(
        List.of(new LicenceWorkProgrammeAmendmentRequest()));

    boolean result = licenceWorkProgrammeAmendmentSubmissionService.isAmendmentSectionSubmittable(
        new ScheduleWorkProgrammeApplicationDetail(UUID.randomUUID()));
    assertTrue(result);
  }

  @Test
  void IsNotSectionWorkProgrammeActivitySubmittable() {
    when(licenceWorkProgrammeAmendmentRepository.findAllByScheduleWorkProgrammeApplicationDetails(
        any(ScheduleWorkProgrammeApplicationDetail.class)))
        .thenReturn(
        List.of());

    boolean result = licenceWorkProgrammeAmendmentSubmissionService.isAmendmentSectionSubmittable(
        new ScheduleWorkProgrammeApplicationDetail(UUID.randomUUID()));
    assertFalse(result);
  }

}