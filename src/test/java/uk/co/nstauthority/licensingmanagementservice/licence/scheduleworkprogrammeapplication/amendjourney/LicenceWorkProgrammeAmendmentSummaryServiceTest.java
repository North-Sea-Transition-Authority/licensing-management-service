package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDuration;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivity;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityCategory;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;

@ExtendWith(MockitoExtension.class)
class LicenceWorkProgrammeAmendmentSummaryServiceTest {

  @Mock
  private LicenceWorkProgrammeAmendmentSummaryRepository licenceWorkProgrammeAmendmentSummaryRepository;

  @Mock
  private LicenceWorkProgrammeAmendmentService licenceWorkProgrammeAmendmentService;

  @Mock
  private WorkProgrammeActivityService workProgrammeActivityService;


  @InjectMocks
  private LicenceWorkProgrammeAmendmentSummaryService licenceWorkProgrammeAmendmentSummaryService;

  private ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail;
  private LicenceWorkProgrammeAmendmentSummary licenceWorkProgrammeAmendmentSummary;
  private LicenceWorkProgrammeAmendmentRequest licenceWorkProgrammeAmendmentRequest;
  private WorkProgrammeActivity workProgrammeActivity;

  @BeforeEach
  void setUp() {
    workProgrammeActivity = new WorkProgrammeActivity();
    workProgrammeActivity.setId(UUID.randomUUID());
    scheduleWorkProgrammeApplicationDetail = new ScheduleWorkProgrammeApplicationDetail();
    scheduleWorkProgrammeApplicationDetail.setId(UUID.randomUUID());

    licenceWorkProgrammeAmendmentSummary = new LicenceWorkProgrammeAmendmentSummary();
    licenceWorkProgrammeAmendmentSummary.setId(UUID.randomUUID());
    licenceWorkProgrammeAmendmentSummary.setScheduleWorkProgrammeApplicationDetails(scheduleWorkProgrammeApplicationDetail);
    licenceWorkProgrammeAmendmentSummary.setLicenceWorkProgrammeAmendmentSummaryOptions(LicenceWorkProgrammeAmendmentSummaryOptions.YES_LATER);

    licenceWorkProgrammeAmendmentRequest = new LicenceWorkProgrammeAmendmentRequest();
    licenceWorkProgrammeAmendmentRequest.setWorkProgrammeActivity(workProgrammeActivity);
    licenceWorkProgrammeAmendmentRequest.setScheduleWorkProgrammeApplicationDetails(scheduleWorkProgrammeApplicationDetail);
    licenceWorkProgrammeAmendmentRequest.setWorkProgrammeExtensionDuration(new ThreeFieldDuration(1, 2, 3));
    licenceWorkProgrammeAmendmentRequest.setWorkProgrammeChangeRequested(true);
    licenceWorkProgrammeAmendmentRequest.setWorkProgrammeCompletionDateChangeRequested(false);
    licenceWorkProgrammeAmendmentRequest.setWorkProgrammeAmendmentInformation("Test amendment info");
  }

  @Test
  void getLicenceWorkProgrammeAmendmentSummaryByScheduleWorkProgrammeApplicationDetail() {
    when(licenceWorkProgrammeAmendmentSummaryRepository
        .findLicenceWorkProgrammeAmendmentSummaryByScheduleWorkProgrammeApplicationDetails(
            scheduleWorkProgrammeApplicationDetail))
        .thenReturn(Optional.of(licenceWorkProgrammeAmendmentSummary));

    Optional<LicenceWorkProgrammeAmendmentSummary> result =
        licenceWorkProgrammeAmendmentSummaryService.getLicenceWorkProgrammeAmendmentSummaryByScheduleWorkProgrammeApplicationDetail(
            scheduleWorkProgrammeApplicationDetail);

    assertThat(result).contains(licenceWorkProgrammeAmendmentSummary);
    verify(licenceWorkProgrammeAmendmentSummaryRepository).findLicenceWorkProgrammeAmendmentSummaryByScheduleWorkProgrammeApplicationDetails(
        scheduleWorkProgrammeApplicationDetail);

    when(licenceWorkProgrammeAmendmentSummaryRepository
        .findLicenceWorkProgrammeAmendmentSummaryByScheduleWorkProgrammeApplicationDetails(
            scheduleWorkProgrammeApplicationDetail))
        .thenReturn(Optional.empty());

    result = licenceWorkProgrammeAmendmentSummaryService.getLicenceWorkProgrammeAmendmentSummaryByScheduleWorkProgrammeApplicationDetail(
        scheduleWorkProgrammeApplicationDetail);

    assertThat(result).isEmpty();
  }

  @Test
  void createSummaryViewFromWorkProgrammeAmendments_ValidFields() {
    workProgrammeActivity.setId(UUID.randomUUID());
    workProgrammeActivity.setCategory(WorkProgrammeActivityCategory.WELL_TEST);

    when(workProgrammeActivityService.getWorkProgrammeActivityByIdOrThrow(any())).thenReturn(workProgrammeActivity);

    LicenceWorkProgrammeAmendmentSummaryView result =
        licenceWorkProgrammeAmendmentSummaryService.createSummaryViewFromWorkProgrammeAmendments(
            licenceWorkProgrammeAmendmentRequest, LicenceWorkProgrammeAmendmentSummaryMode.EDIT);

    assertThat(result.summaryMode()).isEqualTo(LicenceWorkProgrammeAmendmentSummaryMode.EDIT);
    assertTrue(result.workProgrammeChangeRequested());
    assertFalse(result.workProgrammeCompletionDateChangeRequested());
    assertThat(result.workProgrammeAmendmentLabel()).isEqualTo(WorkProgrammeActivityCategory.WELL_TEST.getDisplayName());
    assertThat(result.workProgrammeAmendmentInformation()).isEqualTo("Test amendment info");

    licenceWorkProgrammeAmendmentRequest.setWorkProgrammeChangeRequested(null);
    licenceWorkProgrammeAmendmentRequest.setWorkProgrammeCompletionDateChangeRequested(null);
    licenceWorkProgrammeAmendmentRequest.setWorkProgrammeAmendmentInformation(null);

    result = licenceWorkProgrammeAmendmentSummaryService.createSummaryViewFromWorkProgrammeAmendments(
        licenceWorkProgrammeAmendmentRequest, LicenceWorkProgrammeAmendmentSummaryMode.VIEW);

    assertNull(result.workProgrammeChangeRequested());
    assertNull(result.workProgrammeCompletionDateChangeRequested());
    assertThat(result.workProgrammeAmendmentInformation()).isEmpty();
    assertThat(result.summaryMode()).isEqualTo(LicenceWorkProgrammeAmendmentSummaryMode.VIEW);
  }

  @Test
  void getWorkProgrammeAmendmentResultSummaryViews_ReturnsValidViews() {
    LicenceWorkProgrammeAmendmentRequest workProgrammeAmendmentRequest = new LicenceWorkProgrammeAmendmentRequest();
    workProgrammeAmendmentRequest.setWorkProgrammeActivity(workProgrammeActivity);
    workProgrammeAmendmentRequest.setScheduleWorkProgrammeApplicationDetails(scheduleWorkProgrammeApplicationDetail);
    workProgrammeAmendmentRequest.setWorkProgrammeChangeRequested(false);
    workProgrammeAmendmentRequest.setWorkProgrammeCompletionDateChangeRequested(true);

    workProgrammeActivity.setId(UUID.randomUUID());
    workProgrammeActivity.setCategory(WorkProgrammeActivityCategory.WELL_TEST);

    when(workProgrammeActivityService.getWorkProgrammeActivityByIdOrThrow(any())).thenReturn(workProgrammeActivity);

    when(licenceWorkProgrammeAmendmentService.getAmendmentRequestsByScheduleWorkProgrammeApplicationDetail(
        scheduleWorkProgrammeApplicationDetail))
        .thenReturn(List.of(licenceWorkProgrammeAmendmentRequest, workProgrammeAmendmentRequest));

    List<LicenceWorkProgrammeAmendmentSummaryView> result =
        licenceWorkProgrammeAmendmentSummaryService.getWorkProgrammeAmendmentSummaryViewsFromScheduleWorkProgrammeApplicationDetail(
            scheduleWorkProgrammeApplicationDetail);

    assertThat(result).hasSize(2);
    assertTrue(result.get(0).workProgrammeChangeRequested());
    assertFalse(result.get(1).workProgrammeChangeRequested());

    when(licenceWorkProgrammeAmendmentService.getAmendmentRequestsByScheduleWorkProgrammeApplicationDetail(
        scheduleWorkProgrammeApplicationDetail))
        .thenReturn(new ArrayList<>());

    result = licenceWorkProgrammeAmendmentSummaryService.getWorkProgrammeAmendmentSummaryViewsFromScheduleWorkProgrammeApplicationDetail(
        scheduleWorkProgrammeApplicationDetail);

    assert(result).isEmpty();
  }

  @Test
  void getExistingOrEmptyForm() {
    when(licenceWorkProgrammeAmendmentSummaryRepository
        .findLicenceWorkProgrammeAmendmentSummaryByScheduleWorkProgrammeApplicationDetails(
            scheduleWorkProgrammeApplicationDetail))
        .thenReturn(Optional.of(licenceWorkProgrammeAmendmentSummary));

    LicenceWorkProgrammeAmendmentSummaryForm result =
        licenceWorkProgrammeAmendmentSummaryService.getWorkProgrammeAmendmentByScheduleWorkProgrammeApplicationDetail(scheduleWorkProgrammeApplicationDetail);

    assertNotNull(result);
    assertThat(result.getLicenceWorkProgrammeAmendmentSummaryOptions()).isEqualTo(LicenceWorkProgrammeAmendmentSummaryOptions.YES_LATER);

    when(licenceWorkProgrammeAmendmentSummaryRepository
        .findLicenceWorkProgrammeAmendmentSummaryByScheduleWorkProgrammeApplicationDetails(
            scheduleWorkProgrammeApplicationDetail))
        .thenReturn(Optional.empty());

    result = licenceWorkProgrammeAmendmentSummaryService.getWorkProgrammeAmendmentByScheduleWorkProgrammeApplicationDetail(scheduleWorkProgrammeApplicationDetail);

    assertNotNull(result);
    assertThat(result.getLicenceWorkProgrammeAmendmentSummaryOptions()).isNull();
  }

  @Test
  void saveWorkProgrammeAmendmentSummaryForm_withYesNowOption() {
    LicenceWorkProgrammeAmendmentSummaryForm form = new LicenceWorkProgrammeAmendmentSummaryForm();
    form.setLicenceWorkProgrammeAmendmentSummaryOptions(
        LicenceWorkProgrammeAmendmentSummaryOptions.YES_NOW);

    when(licenceWorkProgrammeAmendmentSummaryRepository
        .findLicenceWorkProgrammeAmendmentSummaryByScheduleWorkProgrammeApplicationDetails(
            scheduleWorkProgrammeApplicationDetail))
        .thenReturn(Optional.of(licenceWorkProgrammeAmendmentSummary));

    licenceWorkProgrammeAmendmentSummaryService.saveWorkProgrammeAmendmentSummaryForm(
        form, scheduleWorkProgrammeApplicationDetail);

    ArgumentCaptor<LicenceWorkProgrammeAmendmentSummary> licenceWorkProgrammeAmendmentSummaryArgumentCaptor = ArgumentCaptor.forClass(LicenceWorkProgrammeAmendmentSummary.class);

    verify(licenceWorkProgrammeAmendmentSummaryRepository).save(
        licenceWorkProgrammeAmendmentSummaryArgumentCaptor.capture());

    LicenceWorkProgrammeAmendmentSummary savedLicenceWorkProgrammeAmendmentSummary = licenceWorkProgrammeAmendmentSummaryArgumentCaptor.getValue();

    assertThat(savedLicenceWorkProgrammeAmendmentSummary.getLicenceWorkProgrammeAmendmentSummaryOptions())
        .isEqualTo(LicenceWorkProgrammeAmendmentSummaryOptions.YES_NOW);

    assertThat(savedLicenceWorkProgrammeAmendmentSummary.getScheduleWorkProgrammeApplicationDetails())
        .isEqualTo(scheduleWorkProgrammeApplicationDetail);
  }

  @Test
  void saveWorkProgrammeAmendmentSummaryForm_withYesLaterOption() {
    LicenceWorkProgrammeAmendmentSummaryForm form = new LicenceWorkProgrammeAmendmentSummaryForm();
    form.setLicenceWorkProgrammeAmendmentSummaryOptions(
        LicenceWorkProgrammeAmendmentSummaryOptions.YES_LATER);

    when(licenceWorkProgrammeAmendmentSummaryRepository
        .findLicenceWorkProgrammeAmendmentSummaryByScheduleWorkProgrammeApplicationDetails(
            scheduleWorkProgrammeApplicationDetail))
        .thenReturn(Optional.of(licenceWorkProgrammeAmendmentSummary));

    licenceWorkProgrammeAmendmentSummaryService.saveWorkProgrammeAmendmentSummaryForm(
        form, scheduleWorkProgrammeApplicationDetail);

    ArgumentCaptor<LicenceWorkProgrammeAmendmentSummary> licenceWorkProgrammeAmendmentSummaryArgumentCaptor = ArgumentCaptor.forClass(LicenceWorkProgrammeAmendmentSummary.class);

    verify(licenceWorkProgrammeAmendmentSummaryRepository).save(licenceWorkProgrammeAmendmentSummaryArgumentCaptor.capture());

    LicenceWorkProgrammeAmendmentSummary savedLicenceWorkProgrammeAmendmentSummary = licenceWorkProgrammeAmendmentSummaryArgumentCaptor.getValue();

    assertThat(savedLicenceWorkProgrammeAmendmentSummary.getLicenceWorkProgrammeAmendmentSummaryOptions())
        .isEqualTo(LicenceWorkProgrammeAmendmentSummaryOptions.YES_LATER);
    assertThat(savedLicenceWorkProgrammeAmendmentSummary.getScheduleWorkProgrammeApplicationDetails())
        .isEqualTo(scheduleWorkProgrammeApplicationDetail);
  }

  @Test
  void saveWorkProgrammeAmendmentSummaryForm_withNoOption() {
    LicenceWorkProgrammeAmendmentSummaryForm form = new LicenceWorkProgrammeAmendmentSummaryForm();
    form.setLicenceWorkProgrammeAmendmentSummaryOptions(
        LicenceWorkProgrammeAmendmentSummaryOptions.NO);

    when(licenceWorkProgrammeAmendmentSummaryRepository
        .findLicenceWorkProgrammeAmendmentSummaryByScheduleWorkProgrammeApplicationDetails(
            scheduleWorkProgrammeApplicationDetail))
        .thenReturn(Optional.of(licenceWorkProgrammeAmendmentSummary));

    licenceWorkProgrammeAmendmentSummaryService.saveWorkProgrammeAmendmentSummaryForm(
        form, scheduleWorkProgrammeApplicationDetail);

    verify(licenceWorkProgrammeAmendmentSummaryRepository).save(
        licenceWorkProgrammeAmendmentSummary);

    assertThat(licenceWorkProgrammeAmendmentSummary.getLicenceWorkProgrammeAmendmentSummaryOptions())
        .isEqualTo(LicenceWorkProgrammeAmendmentSummaryOptions.NO);
    assertThat(licenceWorkProgrammeAmendmentSummary.getScheduleWorkProgrammeApplicationDetails())
        .isEqualTo(scheduleWorkProgrammeApplicationDetail);
  }

  @Test
  void saveWorkProgrammeAmendmentSummaryForm_withNewSummaryOption() {
    LicenceWorkProgrammeAmendmentSummaryForm form = new LicenceWorkProgrammeAmendmentSummaryForm();
    form.setLicenceWorkProgrammeAmendmentSummaryOptions(LicenceWorkProgrammeAmendmentSummaryOptions.NO);

    when(licenceWorkProgrammeAmendmentSummaryRepository
        .findLicenceWorkProgrammeAmendmentSummaryByScheduleWorkProgrammeApplicationDetails(
            scheduleWorkProgrammeApplicationDetail))
        .thenReturn(Optional.empty());

    licenceWorkProgrammeAmendmentSummaryService.saveWorkProgrammeAmendmentSummaryForm(
        form, scheduleWorkProgrammeApplicationDetail);

    ArgumentCaptor<LicenceWorkProgrammeAmendmentSummary> licenceWorkProgrammeAmendmentSummaryArgumentCaptor =
        ArgumentCaptor.forClass(LicenceWorkProgrammeAmendmentSummary.class);

    verify(licenceWorkProgrammeAmendmentSummaryRepository).save(licenceWorkProgrammeAmendmentSummaryArgumentCaptor.capture());

    LicenceWorkProgrammeAmendmentSummary savedLicenceWorkProgrammeAmendmentSummary = licenceWorkProgrammeAmendmentSummaryArgumentCaptor.getValue();

    assertThat(savedLicenceWorkProgrammeAmendmentSummary.getLicenceWorkProgrammeAmendmentSummaryOptions())
        .isEqualTo(LicenceWorkProgrammeAmendmentSummaryOptions.NO);

    assertThat(savedLicenceWorkProgrammeAmendmentSummary.getScheduleWorkProgrammeApplicationDetails())
        .isEqualTo(scheduleWorkProgrammeApplicationDetail);
  }
}