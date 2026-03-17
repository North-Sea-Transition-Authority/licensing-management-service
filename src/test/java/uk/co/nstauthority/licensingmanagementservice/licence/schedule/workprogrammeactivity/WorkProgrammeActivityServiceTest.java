package uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
import uk.co.nstauthority.licensingmanagementservice.exception.LmsEntityNotFoundException;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleEventStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhase;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney.WorkProgrammeActivityView;

@ExtendWith(MockitoExtension.class)
class WorkProgrammeActivityServiceTest {

  @Mock
  private WorkProgrammeActivityRepository workProgrammeActivityRepository;

  @InjectMocks
  private WorkProgrammeActivityService workProgrammeActivityService;

  @Captor
  private ArgumentCaptor<WorkProgrammeActivity> workProgrammeActivityArgumentCaptor;

  private static final String DUE_DATE_DISPLAY = "10 May 2026";
  private static final UUID ACTIVITY_ID = UUID.randomUUID();

  @Test
  void getWorkProgrammeActivityByIdOrThrow() {
    var activity = new WorkProgrammeActivity();
    activity.setId(UUID.randomUUID());

    when(workProgrammeActivityRepository.findById(activity.getId())).thenReturn(Optional.of(activity));

    assertThat(workProgrammeActivityService.getWorkProgrammeActivityByIdOrThrow(activity.getId())).isEqualTo(activity);
  }

  @Test
  void getWorkProgrammeActivityByIdOrThrow_activityNotFound() {
    when(workProgrammeActivityRepository.findById(any())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> workProgrammeActivityService.getWorkProgrammeActivityByIdOrThrow(UUID.randomUUID()))
        .isInstanceOf(LmsEntityNotFoundException.class);
  }

  @Test
  void getActiveWorkProgrammeActivities() {
    var licenceScheduleDetail = new LicenceScheduleDetail();

    workProgrammeActivityService.getActiveWorkProgrammeActivities(licenceScheduleDetail);

    verify(workProgrammeActivityRepository).findAllByLicenceScheduleDetailAndStatus(licenceScheduleDetail, LicenceScheduleEventStatus.ACTIVE);
  }

  @Test
  void getActiveWorkProgrammeActivitiesByTermAndDateOption() {
    var term = new LicenceScheduleTerm();

    workProgrammeActivityService.getActiveWorkProgrammeActivitiesByTermAndDateOption(term, WorkProgrammeActivityDateOption.RELATIVE_DATE);

    verify(workProgrammeActivityRepository).findAllByLicenceScheduleTermAndDateOptionAndStatus(term, WorkProgrammeActivityDateOption.RELATIVE_DATE, LicenceScheduleEventStatus.ACTIVE);
  }

  @Test
  void getActiveWorkProgrammeActivitiesByPhaseAndDateOption() {
    var phase = new LicenceSchedulePhase();

    workProgrammeActivityService.getActiveWorkProgrammeActivitiesByPhaseAndDateOption(phase, WorkProgrammeActivityDateOption.RELATIVE_DATE);

    verify(workProgrammeActivityRepository).findAllByLicenceSchedulePhaseAndDateOptionAndStatus(phase, WorkProgrammeActivityDateOption.RELATIVE_DATE, LicenceScheduleEventStatus.ACTIVE);
  }

  @Test
  void saveWorkProgrammeActivities() {
    var activityList = List.of(new WorkProgrammeActivity());

    workProgrammeActivityService.saveWorkProgrammeActivities(activityList);

    verify(workProgrammeActivityRepository).saveAll(activityList);
  }

  @Test
  void getActiveWorkProgrammeActivitiesByDateRangeFor_term() {
    var licenceScheduleDetail = new LicenceScheduleDetail();
    var startDate = LocalDate.now();
    var endDate = LocalDate.now();

    var term = new LicenceScheduleTerm();
    term.setLicenceScheduleDetail(licenceScheduleDetail);
    term.setStartDate(startDate);
    term.setEndDate(endDate);

    workProgrammeActivityService.getActiveWorkProgrammeActivitiesByDateRangeFor(term);

    verify(workProgrammeActivityRepository).findAllByLicenceScheduleDetailAndDueDateBetweenAndStatus(
        licenceScheduleDetail,
        startDate,
        endDate,
        LicenceScheduleEventStatus.ACTIVE
    );
  }

  @Test
  void getActiveWorkProgrammeActivitiesByDateRangeFor_phase() {
    var licenceScheduleDetail = new LicenceScheduleDetail();
    var startDate = LocalDate.now();
    var endDate = LocalDate.now();

    var phase = new LicenceSchedulePhase();
    phase.setLicenceScheduleDetail(licenceScheduleDetail);
    phase.setStartDate(startDate);
    phase.setEndDate(endDate);

    workProgrammeActivityService.getActiveWorkProgrammeActivitiesByDateRangeFor(phase);

    verify(workProgrammeActivityRepository).findAllByLicenceScheduleDetailAndDueDateBetweenAndStatus(
        licenceScheduleDetail,
        startDate,
        endDate,
        LicenceScheduleEventStatus.ACTIVE
    );
  }

  @Test
  void getActiveWorkProgrammeActivitiesAfterDate() {
    var detail = new LicenceScheduleDetail();
    var date = LocalDate.of(2026, 1, 1);

    workProgrammeActivityService.getActiveWorkProgrammeActivitiesAfterDate(detail, date);

    verify(workProgrammeActivityRepository).findAllByLicenceScheduleDetailAndDueDateAfterAndStatus(
        detail,
        date,
        LicenceScheduleEventStatus.ACTIVE
    );
  }

  @Test
  void deleteWorkProgrammeActivity() {
    var workProgrammeActivity = new WorkProgrammeActivity();
    workProgrammeActivity.setStatus(LicenceScheduleEventStatus.ACTIVE);

    workProgrammeActivityService.deleteWorkProgrammeActivity(workProgrammeActivity);

    verify(workProgrammeActivityRepository).save(workProgrammeActivityArgumentCaptor.capture());

    assertThat(workProgrammeActivityArgumentCaptor.getValue())
        .extracting(WorkProgrammeActivity::getStatus)
        .isEqualTo(LicenceScheduleEventStatus.DELETED);
  }

  @Test
  void getLicenceWorkProgramActivitiesViews_mapsAllFieldsCorrectly() {
    LocalDate fixedDate = LocalDate.of(2026, 5, 10);
    WorkProgrammeActivity workProgrammeActivity = mock(WorkProgrammeActivity.class);

    when(workProgrammeActivity.getId()).thenReturn(ACTIVITY_ID);
    when(workProgrammeActivity.getDateOption()).thenReturn(WorkProgrammeActivityDateOption.RELATIVE_DATE);
    when(workProgrammeActivity.getCategory()).thenReturn(WorkProgrammeActivityCategory.WELL_TEST);
    when(workProgrammeActivity.getOtherCategoryName()).thenReturn(null);
    when(workProgrammeActivity.getDueDate()).thenReturn(fixedDate);
    when(workProgrammeActivity.getDescription()).thenReturn("Test Description");
    when(workProgrammeActivity.getCommitment()).thenReturn(WorkProgrammeActivityCommitment.FIRM);
    when( workProgrammeActivityRepository.findAllByLicenceScheduleDetailAndStatus(any(), any())).thenReturn(List.of(workProgrammeActivity));

    List<WorkProgrammeActivityView> result = workProgrammeActivityService.getLicenceWorkProgramActivitiesViews(new LicenceScheduleDetail());

    assertThat(result).hasSize(1);
    WorkProgrammeActivityView view = result.getFirst();

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

    LocalDate result = workProgrammeActivityService.resolveWorkProgrammeActivityDueDate(workProgrammeActivity);

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

    LocalDate result = workProgrammeActivityService.resolveWorkProgrammeActivityDueDate(workProgrammeActivity);

    assertThat(result).isEqualTo(termEndDate);
  }

  @Test
  void resolveDueDate_whenRelativeDate_returnsDueDate() {
    LocalDate fixedDate = LocalDate.of(2026, 12, 31);
    WorkProgrammeActivity workProgrammeActivity = mock(WorkProgrammeActivity.class);

    when(workProgrammeActivity.getDateOption()).thenReturn(WorkProgrammeActivityDateOption.RELATIVE_DATE);
    when(workProgrammeActivity.getDueDate()).thenReturn(fixedDate);

    LocalDate result = workProgrammeActivityService.resolveWorkProgrammeActivityDueDate(workProgrammeActivity);

    assertThat(result).isEqualTo(fixedDate);
  }

  @Test
  void resolveCategory_whenOtherCategoryNameIsPresent_returnsOtherCategoryName() {
    String customName = "Custom Reporting Requirement";
    WorkProgrammeActivity workProgrammeActivity = mock(WorkProgrammeActivity.class);

    when(workProgrammeActivity.getOtherCategoryName()).thenReturn(customName);

    String result = workProgrammeActivityService.resolveCategory(workProgrammeActivity);

    assertThat(result).isEqualTo(customName);
  }

  @Test
  void resolveCategory_whenOtherCategoryNameIsNull_returnsCategoryDisplayName() {
    WorkProgrammeActivity workProgrammeActivity = mock(WorkProgrammeActivity.class);
    when(workProgrammeActivity.getCategory()).thenReturn(WorkProgrammeActivityCategory.WELL_TEST);

    String result = workProgrammeActivityService.resolveCategory(workProgrammeActivity);

    assertThat(result).isEqualTo(WorkProgrammeActivityCategory.WELL_TEST.getDisplayName());
  }

  @Test
  void getLicenceWorkProgramAmendmentView_returnsMatchingView_whenIdExists() {
    String targetId = ACTIVITY_ID.toString();

    WorkProgrammeActivity workProgrammeActivity = mock(WorkProgrammeActivity.class);
    when(workProgrammeActivity.getId()).thenReturn(ACTIVITY_ID);
    when(workProgrammeActivity.getDateOption()).thenReturn(WorkProgrammeActivityDateOption.RELATIVE_DATE);
    when(workProgrammeActivity.getCategory()).thenReturn(WorkProgrammeActivityCategory.WELL_TEST);
    when(workProgrammeActivity.getOtherCategoryName()).thenReturn(null);
    when(workProgrammeActivity.getDueDate()).thenReturn(LocalDate.of(2026, 5, 10));
    when(workProgrammeActivity.getDescription()).thenReturn("Test Description");
    when(workProgrammeActivity.getCommitment()).thenReturn(WorkProgrammeActivityCommitment.FIRM);

    WorkProgrammeActivityView result = workProgrammeActivityService.createWorkProgrammeActivityView(workProgrammeActivity);

    assertThat(result.id()).isEqualTo(targetId);
    assertThat(result.dueDate()).isEqualTo(DUE_DATE_DISPLAY);
  }

}