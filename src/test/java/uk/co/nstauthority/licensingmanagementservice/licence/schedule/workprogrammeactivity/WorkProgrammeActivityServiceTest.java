package uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.exception.LmsEntityNotFoundException;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventreference.EventReference;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhase;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.status.WorkProgrammeActivityStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.status.WorkProgrammeActivityStatusService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.status.WorkProgrammeStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney.WorkProgrammeActivityView;

@ExtendWith(MockitoExtension.class)
class WorkProgrammeActivityServiceTest {

  @Mock
  private WorkProgrammeActivityRepository workProgrammeActivityRepository;

  @Mock
  private WorkProgrammeActivityStatusService workProgrammeActivityStatusService;

  @InjectMocks
  private WorkProgrammeActivityService workProgrammeActivityService;

  private static final String DUE_DATE_DISPLAY = "10 May 2026";
  private static final UUID ACTIVITY_ID = UUID.randomUUID();
  private WorkProgrammeActivityStatus workProgrammeActivityStatus;

  @BeforeEach
  void setup() {
    workProgrammeActivityStatus = new WorkProgrammeActivityStatus();
    workProgrammeActivityStatus.setStatus(WorkProgrammeStatus.OPEN);
  }

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
  void getWorkProgrammeActivities() {
    var licenceScheduleDetail = new LicenceScheduleDetail();

    workProgrammeActivityService.getWorkProgrammeActivities(licenceScheduleDetail);

    verify(workProgrammeActivityRepository).findAllByLicenceScheduleDetail(licenceScheduleDetail);
  }

  @Test
  void getWorkProgrammeActivitiesByTermAndDateOption() {
    var term = new LicenceScheduleTerm();

    workProgrammeActivityService.getWorkProgrammeActivitiesByTermAndDateOption(term, WorkProgrammeActivityDateOption.RELATIVE_DATE);

    verify(workProgrammeActivityRepository).findAllByLicenceScheduleTermAndDateOption(term, WorkProgrammeActivityDateOption.RELATIVE_DATE);
  }

  @Test
  void getWorkProgrammeActivitiesByPhaseAndDateOption() {
    var phase = new LicenceSchedulePhase();

    workProgrammeActivityService.getWorkProgrammeActivitiesByPhaseAndDateOption(phase, WorkProgrammeActivityDateOption.RELATIVE_DATE);

    verify(workProgrammeActivityRepository).findAllByLicenceSchedulePhaseAndDateOption(phase, WorkProgrammeActivityDateOption.RELATIVE_DATE);
  }

  @Test
  void saveWorkProgrammeActivities() {
    var activityList = List.of(new WorkProgrammeActivity());

    workProgrammeActivityService.saveWorkProgrammeActivities(activityList);

    verify(workProgrammeActivityRepository).saveAll(activityList);
  }

  @Test
  void getWorkProgrammeActivitiesByDateRangeFor_term() {
    var licenceScheduleDetail = new LicenceScheduleDetail();
    var startDate = LocalDate.now();
    var endDate = LocalDate.now();

    var term = new LicenceScheduleTerm();
    term.setLicenceScheduleDetail(licenceScheduleDetail);
    term.setStartDate(startDate);
    term.setEndDate(endDate);

    workProgrammeActivityService.getWorkProgrammeActivitiesByDateRangeFor(term);

    verify(workProgrammeActivityRepository).findAllByLicenceScheduleDetailAndDueDateBetween(
        licenceScheduleDetail,
        startDate,
        endDate
    );
  }

  @Test
  void getWorkProgrammeActivitiesByDateRangeFor_phase() {
    var licenceScheduleDetail = new LicenceScheduleDetail();
    var startDate = LocalDate.now();
    var endDate = LocalDate.now();

    var phase = new LicenceSchedulePhase();
    phase.setLicenceScheduleDetail(licenceScheduleDetail);
    phase.setStartDate(startDate);
    phase.setEndDate(endDate);

    workProgrammeActivityService.getWorkProgrammeActivitiesByDateRangeFor(phase);

    verify(workProgrammeActivityRepository).findAllByLicenceScheduleDetailAndDueDateBetween(
        licenceScheduleDetail,
        startDate,
        endDate
    );
  }

  @Test
  void getWorkProgrammeActivitiesAfterDate() {
    var detail = new LicenceScheduleDetail();
    var date = LocalDate.of(2026, 1, 1);

    workProgrammeActivityService.getWorkProgrammeActivitiesAfterDate(detail, date);

    verify(workProgrammeActivityRepository).findAllByLicenceScheduleDetailAndDueDateAfter(detail, date);
  }

  @Test
  void getWorkProgrammeActivityByScheduleDetailAndEventReferenceOrThrow() {
    var detail = new LicenceScheduleDetail();
    var eventReference = new EventReference();
    var activity = new WorkProgrammeActivity();

    when(workProgrammeActivityRepository.findByLicenceScheduleDetailAndEventReference(detail, eventReference))
        .thenReturn(Optional.of(activity));

    assertThat(workProgrammeActivityService.getWorkProgrammeActivityByScheduleDetailAndEventReferenceOrThrow(detail, eventReference))
        .isEqualTo(activity);
  }

  @Test
  void getWorkProgrammeActivityByScheduleDetailAndEventReferenceOrThrow_notFound() {
    var eventReference = new EventReference();
    eventReference.setId(UUID.randomUUID());

    when(workProgrammeActivityRepository.findByLicenceScheduleDetailAndEventReference(any(), any()))
        .thenReturn(Optional.empty());

    var detail = new LicenceScheduleDetail();
    assertThatThrownBy(() -> workProgrammeActivityService.getWorkProgrammeActivityByScheduleDetailAndEventReferenceOrThrow(detail, eventReference))
        .isInstanceOf(LmsEntityNotFoundException.class);
  }

  @Test
  void deleteWorkProgrammeActivity() {
    var workProgrammeActivity = new WorkProgrammeActivity();

    workProgrammeActivityService.deleteWorkProgrammeActivity(workProgrammeActivity);

    verify(workProgrammeActivityRepository).delete(workProgrammeActivity);
  }

  @Test
  void getAllActivitiesLinkedTo() {
    var term = new LicenceScheduleTerm();

    workProgrammeActivityService.getAllActivitiesLinkedTo(term);

    verify(workProgrammeActivityRepository).findByLicenceScheduleTerm(term);
  }

  @Test
  void hasActivitiesForPhase() {
    var phase = new LicenceSchedulePhase();
    when(workProgrammeActivityRepository.existsByLicenceSchedulePhase(phase)).thenReturn(true);

    assertThat(workProgrammeActivityService.hasActivitiesForPhase(phase)).isTrue();
  }

  @Test
  void hasActivitiesForTerm() {
    var term = new LicenceScheduleTerm();
    when(workProgrammeActivityRepository.existsByLicenceScheduleTerm(term)).thenReturn(false);

    assertThat(workProgrammeActivityService.hasActivitiesForTerm(term)).isFalse();
  }

  @Test
  void getLicenceWorkProgramActivitiesViews_mapsAllFieldsCorrectly() {
    LocalDate fixedDate = LocalDate.of(2026, 5, 10);
    WorkProgrammeActivity workProgrammeActivity = mock(WorkProgrammeActivity.class);
    var eventRef = new EventReference();
    eventRef.setId(UUID.randomUUID());

    when(workProgrammeActivity.getId()).thenReturn(ACTIVITY_ID);
    when(workProgrammeActivity.getEventReference()).thenReturn(eventRef);
    when(workProgrammeActivity.getDateOption()).thenReturn(WorkProgrammeActivityDateOption.RELATIVE_DATE);
    when(workProgrammeActivity.getCategory()).thenReturn(WorkProgrammeActivityCategory.WELL_TEST);
    when(workProgrammeActivity.getOtherCategoryName()).thenReturn(null);
    when(workProgrammeActivity.getDueDate()).thenReturn(fixedDate);
    when(workProgrammeActivity.getDescription()).thenReturn("Test Description");
    when(workProgrammeActivity.getCommitment()).thenReturn(WorkProgrammeActivityCommitment.FIRM);
    when(workProgrammeActivityStatusService.getLatestStatusesFor(List.of(workProgrammeActivity)))
        .thenReturn(Map.of(eventRef.getId(), workProgrammeActivityStatus));

    when(workProgrammeActivityRepository.findAllByLicenceScheduleDetail(any()))
        .thenReturn(List.of(workProgrammeActivity));

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
    when(workProgrammeActivityStatusService.getLatestStatusFor(workProgrammeActivity)).thenReturn(workProgrammeActivityStatus);

    WorkProgrammeActivityView result = workProgrammeActivityService.createWorkProgrammeActivityView(workProgrammeActivity);

    assertThat(result.id()).isEqualTo(targetId);
    assertThat(result.dueDate()).isEqualTo(DUE_DATE_DISPLAY);
  }
}