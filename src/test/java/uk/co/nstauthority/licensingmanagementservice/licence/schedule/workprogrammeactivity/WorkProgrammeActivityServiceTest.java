package uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.exception.LmsEntityNotFoundException;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhase;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;

@ExtendWith(MockitoExtension.class)
class WorkProgrammeActivityServiceTest {

  @Mock
  private WorkProgrammeActivityRepository workProgrammeActivityRepository;

  @InjectMocks
  private WorkProgrammeActivityService workProgrammeActivityService;

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

}