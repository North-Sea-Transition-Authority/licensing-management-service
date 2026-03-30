package uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivity;

@ExtendWith(MockitoExtension.class)
class WorkProgrammeActivityStatusServiceTest {

  @Mock
  private WorkProgrammeActivityStatusRepository workProgrammeActivityStatusRepository;

  @Mock
  private Clock clock;

  @InjectMocks
  private WorkProgrammeActivityStatusService workProgrammeActivityStatusService;

  @Captor
  private ArgumentCaptor<WorkProgrammeActivityStatus> workProgrammeActivityStatusArgumentCaptor;

  private final Instant instant = Instant.parse("2026-01-01T10:00:00.00Z");

  @Test
  void createInitialStatusFor() {
    var activity = new WorkProgrammeActivity();
    activity.setEventReference(UUID.randomUUID());

    when(workProgrammeActivityStatusRepository.findAllByActivityEventReference(activity.getEventReference()))
        .thenReturn(List.of());

    when(clock.instant()).thenReturn(instant);

    workProgrammeActivityStatusService.createInitialStatusFor(activity);

    verify(workProgrammeActivityStatusRepository).save(workProgrammeActivityStatusArgumentCaptor.capture());

    assertThat(workProgrammeActivityStatusArgumentCaptor.getValue())
        .extracting(
            WorkProgrammeActivityStatus::getStatus,
            WorkProgrammeActivityStatus::getActivityEventReference,
            WorkProgrammeActivityStatus::getAppliedDatetime
        )
        .containsExactly(
            WorkProgrammeStatus.OPEN,
            activity.getEventReference(),
            instant
        );
  }

  @Test
  void saveStatusFromForm() {
    var activity = new WorkProgrammeActivity();
    activity.setEventReference(UUID.randomUUID());

    var form = new WorkProgrammeActivityStatusForm();
    form.setStatus(WorkProgrammeStatus.IN_PROGRESS);

    when(clock.instant()).thenReturn(instant);

    workProgrammeActivityStatusService.saveStatusFromForm(form, activity);

    verify(workProgrammeActivityStatusRepository).save(workProgrammeActivityStatusArgumentCaptor.capture());

    assertThat(workProgrammeActivityStatusArgumentCaptor.getValue())
        .extracting(
            WorkProgrammeActivityStatus::getStatus,
            WorkProgrammeActivityStatus::getActivityEventReference,
            WorkProgrammeActivityStatus::getAppliedDatetime
        )
        .containsExactly(
            form.getStatus(),
            activity.getEventReference(),
            instant
        );
  }

  @Test
  void getLatestStatusFor() {
    var activity = new WorkProgrammeActivity();

    var activityStatus = new WorkProgrammeActivityStatus();
    activityStatus.setAppliedDatetime(instant);

    var activityStatus2 = new WorkProgrammeActivityStatus();
    activityStatus2.setAppliedDatetime(instant.plusSeconds(100L));

    when(workProgrammeActivityStatusRepository.findAllByActivityEventReference(activity.getEventReference()))
        .thenReturn(List.of(activityStatus, activityStatus2));

    assertThat(workProgrammeActivityStatusService.getLatestStatusFor(activity)).isEqualTo(activityStatus2);
  }

  @Test
  void getStatusForm() {
    var activity = new WorkProgrammeActivity();

    var activityStatus = new WorkProgrammeActivityStatus();
    activityStatus.setStatus(WorkProgrammeStatus.IN_PROGRESS);
    activityStatus.setAppliedDatetime(instant);

    when(workProgrammeActivityStatusRepository.findAllByActivityEventReference(activity.getEventReference()))
        .thenReturn(List.of(activityStatus));

    assertThat(workProgrammeActivityStatusService.getStatusForm(activity))
        .extracting(
            WorkProgrammeActivityStatusForm::getStatus,
            WorkProgrammeActivityStatusForm::getTransferredToLicenceId
        )
        .containsExactly(
            activityStatus.getStatus(),
            null
        );
  }
}