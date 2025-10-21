package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
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
  private LicenceWorkProgrammeAmendmentService licenceWorkProgrammeAmendmentService;

  @Mock
  private LicenceWorkProgrammeAmendmentSummaryService licenceWorkProgrammeAmendmentSummaryService;

  @InjectMocks
  private LicenceWorkProgrammeAmendmentSubmissionService licenceWorkProgrammeAmendmentSubmissionService;

  @Test
  void IsSectionWorkProgrammeActivitySubmittable() {
    when(licenceWorkProgrammeAmendmentService.getAmendmentRequestsByScheduleWorkProgrammeApplicationDetail(
        any(ScheduleWorkProgrammeApplicationDetail.class)))
        .thenReturn(List.of(new LicenceWorkProgrammeAmendmentRequest()));

    boolean result = licenceWorkProgrammeAmendmentSubmissionService.isAmendmentSectionSubmittable(
        new ScheduleWorkProgrammeApplicationDetail(UUID.randomUUID()));
    assertTrue(result);
  }

  @Test
  void IsNotSectionWorkProgrammeActivitySubmittable() {
    when(licenceWorkProgrammeAmendmentService.getAmendmentRequestsByScheduleWorkProgrammeApplicationDetail(
        any(ScheduleWorkProgrammeApplicationDetail.class)))
        .thenReturn(List.of());

    boolean result = licenceWorkProgrammeAmendmentSubmissionService.isAmendmentSectionSubmittable(
        new ScheduleWorkProgrammeApplicationDetail(UUID.randomUUID()));
    assertFalse(result);
  }

  @Test
  void isAmendmentSectionComplete_whenSummaryPresentWithNoOption_returnsTrue() {

    ScheduleWorkProgrammeApplicationDetail detail = new ScheduleWorkProgrammeApplicationDetail(UUID.randomUUID());
    LicenceWorkProgrammeAmendmentSummary summary = new LicenceWorkProgrammeAmendmentSummary();
    summary.setLicenceWorkProgrammeAmendmentSummaryOptions(LicenceWorkProgrammeAmendmentSummaryOptions.NO);

    when(licenceWorkProgrammeAmendmentSummaryService.getLicenceWorkProgrammeAmendmentSummaryByScheduleWorkProgrammeApplicationDetail(
            detail))
        .thenReturn(Optional.of(summary));

    when(licenceWorkProgrammeAmendmentService.validateAllWorkProgrammeAmendments(any())).thenReturn(true);

    boolean result = licenceWorkProgrammeAmendmentSubmissionService.isAmendmentSectionComplete(detail);

    assertTrue(result);
  }

  @Test
  void isAmendmentSectionComplete_whenSummaryPresentWithYesOption_returnsFalse() {

    ScheduleWorkProgrammeApplicationDetail detail = new ScheduleWorkProgrammeApplicationDetail(UUID.randomUUID());
    LicenceWorkProgrammeAmendmentSummary summary = new LicenceWorkProgrammeAmendmentSummary();
    summary.setLicenceWorkProgrammeAmendmentSummaryOptions(LicenceWorkProgrammeAmendmentSummaryOptions.YES_NOW);

    when(licenceWorkProgrammeAmendmentSummaryService.getLicenceWorkProgrammeAmendmentSummaryByScheduleWorkProgrammeApplicationDetail(
            detail))
        .thenReturn(Optional.of(summary));

    boolean result = licenceWorkProgrammeAmendmentSubmissionService.isAmendmentSectionComplete(detail);

    assertFalse(result);
  }

  @Test
  void isAmendmentSectionComplete_whenSummaryNotPresent_returnsFalse() {

    ScheduleWorkProgrammeApplicationDetail detail = new ScheduleWorkProgrammeApplicationDetail(UUID.randomUUID());

    when(licenceWorkProgrammeAmendmentSummaryService.getLicenceWorkProgrammeAmendmentSummaryByScheduleWorkProgrammeApplicationDetail(
            detail))
        .thenReturn(Optional.empty());

    boolean result = licenceWorkProgrammeAmendmentSubmissionService.isAmendmentSectionComplete(detail);

    assertFalse(result);
  }

}