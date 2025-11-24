package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDuration;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhase;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivity;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityCategory;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityDateOption;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overallrequest.LicenceScheduleSupportingInformationService;

@ExtendWith(MockitoExtension.class)
class LicenceWorkProgrammeAmendmentServiceTest {

  @Mock
  private LicenceWorkProgrammeAmendmentRepository licenceWorkProgrammeAmendmentRepository;

  @Mock
  private WorkProgrammeActivityService workProgrammeActivityService;

  @Mock
  private LicenceWorkProgrammeAmendmentFormValidator licenceWorkProgrammeAmendmentFormValidator;

  @Mock
  private LicenceScheduleSupportingInformationService licenceScheduleSupportingInformationService;

  @InjectMocks
  private LicenceWorkProgrammeAmendmentService licenceWorkProgrammeAmendmentService;

  @Captor
  private ArgumentCaptor<LicenceWorkProgrammeAmendmentRequest> licenceWorkProgrammeAmendmentRequestArgumentCaptor;

  private static final String DUE_DATE_DISPLAY = "10 May 2026";
  private static final UUID ACTIVITY_ID = UUID.randomUUID();


  @Test
  void getLicenceScheduleExtensionExisting() {
    ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail = new ScheduleWorkProgrammeApplicationDetail();

    LicenceWorkProgrammeAmendmentRequest licenceWorkProgrammeAmendmentRequest = new LicenceWorkProgrammeAmendmentRequest();
    licenceWorkProgrammeAmendmentRequest.setWorkProgrammeExtensionDuration(new ThreeFieldDuration(1, 1, 1));
    licenceWorkProgrammeAmendmentRequest.setWorkProgrammeAmendmentInformation("testInformation");
    licenceWorkProgrammeAmendmentRequest.setId(UUID.randomUUID());
    licenceWorkProgrammeAmendmentRequest.setScheduleWorkProgrammeApplicationDetails(
        scheduleWorkProgrammeApplicationDetail);

    when(licenceWorkProgrammeAmendmentRepository.findByScheduleWorkProgrammeApplicationDetailsAndWorkProgrammeActivity(any(), any())).thenReturn(
        Optional.of(licenceWorkProgrammeAmendmentRequest));

    LicenceWorkProgrammeAmendmentForm actualForm = licenceWorkProgrammeAmendmentService.getLicenceWorkProgrammeActivityAmendmentForm(new WorkProgrammeActivity(),
        scheduleWorkProgrammeApplicationDetail);

    assertEquals(actualForm.getWorkProgrammeExtensionDuration().toThreeFieldDuration(),
        licenceWorkProgrammeAmendmentRequest.getWorkProgrammeExtensionDuration());
    assertEquals(("testInformation"), actualForm.getWorkProgrammeAmendmentInformation());
  }

  @Test
  void saveAmendmentFormYesThenNoScenario() {
    var scheduleWorkProgrammeApplicationDetail = new ScheduleWorkProgrammeApplicationDetail();

    var form = new LicenceWorkProgrammeAmendmentForm();
    form.setWorkProgrammeAmendmentInformation("testAmendmentInformation");
    form.getWorkProgrammeExtensionDuration().setYears("4");
    form.getWorkProgrammeExtensionDuration().setMonths("4");
    form.getWorkProgrammeExtensionDuration().setDays("4");
    form.setDurationExtensionRequired(true);
    form.setAdditionalInfoRequired(true);

    licenceWorkProgrammeAmendmentService.saveAmendmentForm(form, scheduleWorkProgrammeApplicationDetail,new WorkProgrammeActivity());

    verify(licenceWorkProgrammeAmendmentRepository).save(licenceWorkProgrammeAmendmentRequestArgumentCaptor.capture());

    var result = licenceWorkProgrammeAmendmentRequestArgumentCaptor.getValue();

    assertThat(result).extracting(
        LicenceWorkProgrammeAmendmentRequest::getWorkProgrammeExtensionDuration,
        LicenceWorkProgrammeAmendmentRequest::getWorkProgrammeAmendmentInformation,
        LicenceWorkProgrammeAmendmentRequest::getScheduleWorkProgrammeApplicationDetails
    ).containsExactly(
        form.getWorkProgrammeExtensionDuration().toThreeFieldDuration(),
        form.getWorkProgrammeAmendmentInformation(),
        scheduleWorkProgrammeApplicationDetail
    );

    form.setDurationExtensionRequired(false);
    form.setAdditionalInfoRequired(false);

    licenceWorkProgrammeAmendmentService.saveAmendmentForm(form, scheduleWorkProgrammeApplicationDetail,new WorkProgrammeActivity());

    verify(licenceScheduleSupportingInformationService).handleSupportingInformationExtensionRemoval(scheduleWorkProgrammeApplicationDetail);

    verify(licenceWorkProgrammeAmendmentRepository,times(2)).save(licenceWorkProgrammeAmendmentRequestArgumentCaptor.capture());

    var updatedResult = licenceWorkProgrammeAmendmentRequestArgumentCaptor.getValue();

    assertThat(updatedResult).extracting(
        LicenceWorkProgrammeAmendmentRequest::getWorkProgrammeExtensionDuration,
        LicenceWorkProgrammeAmendmentRequest::getWorkProgrammeAmendmentInformation,
        LicenceWorkProgrammeAmendmentRequest::getScheduleWorkProgrammeApplicationDetails
    ).containsNull();
  }

  @Test
  void saveAmendmentForm() {
    var scheduleWorkProgrammeApplicationDetail = new ScheduleWorkProgrammeApplicationDetail();

    var form = new LicenceWorkProgrammeAmendmentForm();
    form.setWorkProgrammeAmendmentInformation("testAmendmentInformation");
    form.getWorkProgrammeExtensionDuration().setYears("4");
    form.getWorkProgrammeExtensionDuration().setMonths("4");
    form.getWorkProgrammeExtensionDuration().setDays("4");
    form.setDurationExtensionRequired(true);
    form.setAdditionalInfoRequired(true);

    licenceWorkProgrammeAmendmentService.saveAmendmentForm(form, scheduleWorkProgrammeApplicationDetail,new WorkProgrammeActivity());

    verify(licenceWorkProgrammeAmendmentRepository).save(licenceWorkProgrammeAmendmentRequestArgumentCaptor.capture());

    var result = licenceWorkProgrammeAmendmentRequestArgumentCaptor.getValue();

    assertThat(result).extracting(
        LicenceWorkProgrammeAmendmentRequest::getWorkProgrammeExtensionDuration,
        LicenceWorkProgrammeAmendmentRequest::getWorkProgrammeAmendmentInformation,
        LicenceWorkProgrammeAmendmentRequest::getScheduleWorkProgrammeApplicationDetails
    ).containsExactly(
        form.getWorkProgrammeExtensionDuration().toThreeFieldDuration(),
        form.getWorkProgrammeAmendmentInformation(),
        scheduleWorkProgrammeApplicationDetail
    );
  }

  @Test
  void saveAmendmentFormWithoutExtension() {
    var scheduleWorkProgrammeApplicationDetail = new ScheduleWorkProgrammeApplicationDetail();

    var form = new LicenceWorkProgrammeAmendmentForm();
    form.setWorkProgrammeAmendmentInformation("testAmendmentInformation");
    form.setAdditionalInfoRequired(true);

    licenceWorkProgrammeAmendmentService.saveAmendmentForm(form, scheduleWorkProgrammeApplicationDetail,new WorkProgrammeActivity());

    verify(licenceScheduleSupportingInformationService).handleSupportingInformationExtensionRemoval(scheduleWorkProgrammeApplicationDetail);

    verify(licenceWorkProgrammeAmendmentRepository).save(licenceWorkProgrammeAmendmentRequestArgumentCaptor.capture());

    var result = licenceWorkProgrammeAmendmentRequestArgumentCaptor.getValue();

    assertThat(result).extracting(
        LicenceWorkProgrammeAmendmentRequest::getWorkProgrammeAmendmentInformation,
        LicenceWorkProgrammeAmendmentRequest::getScheduleWorkProgrammeApplicationDetails
    ).containsExactly(
        form.getWorkProgrammeAmendmentInformation(),
        scheduleWorkProgrammeApplicationDetail
    );
  }

  @Test
  void validateAllWorkProgrammeAmendments_withValidAmendments() {
    when(licenceWorkProgrammeAmendmentFormValidator.isValid(any(), any())).thenReturn(true);

    LicenceWorkProgrammeAmendmentRequest request = new LicenceWorkProgrammeAmendmentRequest();
    request.setWorkProgrammeChangeRequested(true);
    request.setWorkProgrammeAmendmentInformation("testAmendmentInformation");
    request.setWorkProgrammeCompletionDateChangeRequested(true);
    request.setWorkProgrammeExtensionDuration(new ThreeFieldDuration(2, 2, 2));

    boolean result = licenceWorkProgrammeAmendmentService.validateAllWorkProgrammeAmendments(List.of(request));

    assertTrue(result);
    verify(licenceWorkProgrammeAmendmentFormValidator).isValid(any(LicenceWorkProgrammeAmendmentForm.class), any());
  }

  @Test
  void validateAllWorkProgrammeAmendments_withInvalidAmendments() {
    when(licenceWorkProgrammeAmendmentFormValidator.isValid(any(), any())).thenReturn(false);

    LicenceWorkProgrammeAmendmentRequest request = new LicenceWorkProgrammeAmendmentRequest();
    request.setWorkProgrammeChangeRequested(false);
    request.setWorkProgrammeCompletionDateChangeRequested(false);

    assertFalse(licenceWorkProgrammeAmendmentService.validateAllWorkProgrammeAmendments(List.of(request)));
    verify(licenceWorkProgrammeAmendmentFormValidator).isValid(any(LicenceWorkProgrammeAmendmentForm.class), any());
  }

  @Test
  void getLicenceWorkProgramAmendmentViews_mapsAllFieldsCorrectly() {
    LocalDate fixedDate = LocalDate.of(2026, 5, 10);
    WorkProgrammeActivity workProgrammeActivity = mock(WorkProgrammeActivity.class);

    when(workProgrammeActivity.getId()).thenReturn(ACTIVITY_ID);
    when(workProgrammeActivity.getDateOption()).thenReturn(WorkProgrammeActivityDateOption.FIXED_DATE);
    when(workProgrammeActivity.getCategory()).thenReturn(WorkProgrammeActivityCategory.WELL_TEST);
    when(workProgrammeActivity.getOtherCategoryName()).thenReturn(null);
    when(workProgrammeActivity.getDueDate()).thenReturn(fixedDate);
    when(workProgrammeActivity.getDescription()).thenReturn("Test Description");
    when(workProgrammeActivityService.getWorkProgrammeActivities(any())).thenReturn(List.of(workProgrammeActivity));


    List<WorkProgrammeActivityAmendmentView> result = licenceWorkProgrammeAmendmentService.getLicenceWorkProgramAmendmentViews(any());

    assertThat(result).hasSize(1);
    WorkProgrammeActivityAmendmentView view = result.getFirst();

    assertThat(view.id()).isEqualTo(ACTIVITY_ID.toString());
    assertThat(view.dueDate()).isEqualTo(DUE_DATE_DISPLAY);
    assertThat(view.category()).isEqualTo(WorkProgrammeActivityCategory.WELL_TEST.getDisplayName());
    assertThat(view.description()).isEqualTo("Test Description");
  }

  @Test
  void resolveDueDate_whenWithinPhase_returnsPhaseEndDate() {
    LocalDate phaseEndDate = LocalDate.of(2027, 1, 1);
    WorkProgrammeActivity workProgrammeActivity = mock(WorkProgrammeActivity.class);
    LicenceSchedulePhase mockPhase = mock(LicenceSchedulePhase.class);

    when(workProgrammeActivity.getDateOption()).thenReturn(WorkProgrammeActivityDateOption.WITHIN_A_PHASE);
    when(mockPhase.getEndDate()).thenReturn(phaseEndDate);
    when(workProgrammeActivity.getLicenceSchedulePhase()).thenReturn(mockPhase);

    LocalDate result = licenceWorkProgrammeAmendmentService.resolveWorkProgrammeActivityDueDate(workProgrammeActivity);

    assertThat(result).isEqualTo(phaseEndDate);
  }

  @Test
  void resolveDueDate_whenWithinTerm_returnsTermEndDate() {
    LocalDate termEndDate = LocalDate.of(2028, 6, 15);
    WorkProgrammeActivity workProgrammeActivity = mock(WorkProgrammeActivity.class);
    LicenceScheduleTerm mockTerm = mock(LicenceScheduleTerm.class);

    when(mockTerm.getEndDate()).thenReturn(termEndDate);
    when(workProgrammeActivity.getLicenceScheduleTerm()).thenReturn(mockTerm);
    when(workProgrammeActivity.getDateOption()).thenReturn(WorkProgrammeActivityDateOption.WITHIN_A_TERM);

    LocalDate result = licenceWorkProgrammeAmendmentService.resolveWorkProgrammeActivityDueDate(workProgrammeActivity);

    assertThat(result).isEqualTo(termEndDate);
  }

  @Test
  void resolveDueDate_whenFixedDate_returnsDueDate() {
    LocalDate fixedDate = LocalDate.of(2026, 12, 31);
    WorkProgrammeActivity workProgrammeActivity = mock(WorkProgrammeActivity.class);

    when(workProgrammeActivity.getDateOption()).thenReturn(WorkProgrammeActivityDateOption.FIXED_DATE);
    when(workProgrammeActivity.getDueDate()).thenReturn(fixedDate);

    LocalDate result = licenceWorkProgrammeAmendmentService.resolveWorkProgrammeActivityDueDate(workProgrammeActivity);

    assertThat(result).isEqualTo(fixedDate);
  }

  @Test
  void resolveCategory_whenOtherCategoryNameIsPresent_returnsOtherCategoryName() {
    String customName = "Custom Reporting Requirement";
    WorkProgrammeActivity workProgrammeActivity = mock(WorkProgrammeActivity.class);

    when(workProgrammeActivity.getOtherCategoryName()).thenReturn(customName);


    String result = licenceWorkProgrammeAmendmentService.resolveCategory(workProgrammeActivity);

    assertThat(result).isEqualTo(customName);
  }

  @Test
  void resolveCategory_whenOtherCategoryNameIsNull_returnsCategoryDisplayName() {
    WorkProgrammeActivity workProgrammeActivity = mock(WorkProgrammeActivity.class);
    when(workProgrammeActivity.getCategory()).thenReturn(WorkProgrammeActivityCategory.WELL_TEST);

    String result = licenceWorkProgrammeAmendmentService.resolveCategory(workProgrammeActivity);

    assertThat(result).isEqualTo(WorkProgrammeActivityCategory.WELL_TEST.getDisplayName());
  }

  @Test
  void getLicenceWorkProgramAmendmentView_returnsMatchingView_whenIdExists() {
    String targetId = ACTIVITY_ID.toString();

    WorkProgrammeActivity workProgrammeActivity = mock(WorkProgrammeActivity.class);
    when(workProgrammeActivity.getId()).thenReturn(ACTIVITY_ID);
    when(workProgrammeActivity.getDateOption()).thenReturn(WorkProgrammeActivityDateOption.FIXED_DATE);
    when(workProgrammeActivity.getCategory()).thenReturn(WorkProgrammeActivityCategory.WELL_TEST);
    when(workProgrammeActivity.getOtherCategoryName()).thenReturn(null);
    when(workProgrammeActivity.getDueDate()).thenReturn(LocalDate.of(2026, 5, 10));
    when(workProgrammeActivity.getDescription()).thenReturn("Test Description");

    when(workProgrammeActivityService.getWorkProgrammeActivities(any())).thenReturn(List.of(workProgrammeActivity));

    WorkProgrammeActivityAmendmentView result = licenceWorkProgrammeAmendmentService.getLicenceWorkProgramAmendmentView(
        any(), targetId);

    assertThat(result.id()).isEqualTo(targetId);
    assertThat(result.dueDate()).isEqualTo(DUE_DATE_DISPLAY);
  }
}