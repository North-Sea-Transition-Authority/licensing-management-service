package uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

@ExtendWith(MockitoExtension.class)
class WorkProgrammeActivityServiceTest {

  @Mock
  private WorkProgrammeActivityRepository workProgrammeActivityRepository;

  @InjectMocks
  private WorkProgrammeActivityService workProgrammeActivityService;

  @Captor
  private ArgumentCaptor<WorkProgrammeActivity> workProgrammeActivityArgumentCaptor;

  @Test
  void getWorkProgrammeActivityByIdOrThrow() {
    var activity = new WorkProgrammeActivity();
    activity.setId(UUID.randomUUID());

    when(workProgrammeActivityRepository.findById(activity.getId())).thenReturn(Optional.of(activity));

    assertThat(workProgrammeActivityService.getWorkProgrammeActivityByIdOrThrow(activity.getId())).isEqualTo(activity);
  }

  @Test
  void getWorkProgrammeActivityByIdOrThrow_termNotFound() {
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
  void deleteWorkProgrammeActivity() {
    var workProgrammeActivity = new WorkProgrammeActivity();
    workProgrammeActivity.setStatus(LicenceScheduleEventStatus.ACTIVE);

    workProgrammeActivityService.deleteWorkProgrammeActivity(workProgrammeActivity);

    verify(workProgrammeActivityRepository).save(workProgrammeActivityArgumentCaptor.capture());

    assertThat(workProgrammeActivityArgumentCaptor.getValue())
        .extracting(WorkProgrammeActivity::getStatus)
        .isEqualTo(LicenceScheduleEventStatus.DELETED);
  }

}