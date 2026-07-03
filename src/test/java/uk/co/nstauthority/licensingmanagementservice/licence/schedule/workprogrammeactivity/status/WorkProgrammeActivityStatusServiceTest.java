package uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivity;

@ExtendWith(MockitoExtension.class)
class WorkProgrammeActivityStatusServiceTest {

  @Mock
  private WorkProgrammeActivityStatusRepository workProgrammeActivityStatusRepository;

  @Mock
  private Clock clock;

  @Mock
  private LicenceService licenceService;

  @InjectMocks
  private WorkProgrammeActivityStatusService workProgrammeActivityStatusService;

  @Captor
  private ArgumentCaptor<WorkProgrammeActivityStatus> workProgrammeActivityStatusArgumentCaptor;

  private final Instant instant = Instant.parse("2026-01-01T10:00:00.00Z");

  @Test
  void createInitialStatusFor() {
    var activity = new WorkProgrammeActivity();
    activity.setId(UUID.randomUUID());
    activity.setOriginalEventId(activity.getId());

    when(workProgrammeActivityStatusRepository.findAllByScheduleEvent_OriginalEventId(activity.getOriginalEventId()))
        .thenReturn(List.of());

    when(clock.instant()).thenReturn(instant);

    workProgrammeActivityStatusService.createInitialStatusFor(activity);

    verify(workProgrammeActivityStatusRepository).save(workProgrammeActivityStatusArgumentCaptor.capture());

    assertThat(workProgrammeActivityStatusArgumentCaptor.getValue())
        .extracting(
            WorkProgrammeActivityStatus::getStatus,
            WorkProgrammeActivityStatus::getScheduleEvent,
            WorkProgrammeActivityStatus::getAppliedDatetime
        )
        .containsExactly(
            WorkProgrammeStatus.OPEN,
            activity,
            instant
        );
  }

  @Test
  void saveStatusFromForm() {
    var activity = new WorkProgrammeActivity();
    activity.setId(UUID.randomUUID());

    var form = new WorkProgrammeActivityStatusForm();
    form.setStatus(WorkProgrammeStatus.IN_PROGRESS);

    when(clock.instant()).thenReturn(instant);

    workProgrammeActivityStatusService.saveStatusFromForm(form, activity);

    verify(workProgrammeActivityStatusRepository).save(workProgrammeActivityStatusArgumentCaptor.capture());

    assertThat(workProgrammeActivityStatusArgumentCaptor.getValue())
        .extracting(
            WorkProgrammeActivityStatus::getStatus,
            WorkProgrammeActivityStatus::getScheduleEvent,
            WorkProgrammeActivityStatus::getAppliedDatetime
        )
        .containsExactly(
            form.getStatus(),
            activity,
            instant
        );
  }

  @Test
  void getLatestStatusFor() {
    var activity = new WorkProgrammeActivity();
    activity.setId(UUID.randomUUID());
    activity.setOriginalEventId(activity.getId());

    var activityStatus = new WorkProgrammeActivityStatus();
    activityStatus.setAppliedDatetime(instant);

    var activityStatus2 = new WorkProgrammeActivityStatus();
    activityStatus2.setAppliedDatetime(instant.plusSeconds(100L));

    when(workProgrammeActivityStatusRepository.findAllByScheduleEvent_OriginalEventId(activity.getOriginalEventId()))
        .thenReturn(List.of(activityStatus, activityStatus2));

    assertThat(workProgrammeActivityStatusService.getLatestStatusFor(activity)).isEqualTo(activityStatus2);
  }

  @Test
  void getLatestStatusesFor() {
    var activity = new WorkProgrammeActivity();
    activity.setId(UUID.randomUUID());
    activity.setOriginalEventId(activity.getId());

    var activityStatus = new WorkProgrammeActivityStatus();
    activityStatus.setScheduleEvent(activity);
    activityStatus.setAppliedDatetime(Instant.now());

    var activityStatus2 = new WorkProgrammeActivityStatus();
    activityStatus2.setScheduleEvent(activity);
    activityStatus2.setAppliedDatetime(Instant.now().plus(1, ChronoUnit.DAYS));

    var activity2 = new WorkProgrammeActivity();
    activity2.setId(UUID.randomUUID());
    activity2.setOriginalEventId(activity2.getId());

    var activityStatus3 = new WorkProgrammeActivityStatus();
    activityStatus3.setScheduleEvent(activity2);
    activityStatus3.setAppliedDatetime(Instant.now());

    when(workProgrammeActivityStatusRepository.findAllByScheduleEvent_OriginalEventIdIn(
        List.of(activity.getOriginalEventId(), activity2.getOriginalEventId())))
        .thenReturn(List.of(activityStatus, activityStatus2, activityStatus3));

    assertThat(workProgrammeActivityStatusService.getLatestStatusesFor(List.of(activity, activity2)))
        .containsExactly(
            entry(activity.getOriginalEventId(), activityStatus2),
            entry(activity2.getOriginalEventId(), activityStatus3)
        );
  }

  @Test
  void getStatusForm() {
    var activity = new WorkProgrammeActivity();
    activity.setId(UUID.randomUUID());
    activity.setOriginalEventId(activity.getId());

    var activityStatus = new WorkProgrammeActivityStatus();
    activityStatus.setStatus(WorkProgrammeStatus.IN_PROGRESS);
    activityStatus.setAppliedDatetime(instant);

    when(workProgrammeActivityStatusRepository.findAllByScheduleEvent_OriginalEventId(activity.getOriginalEventId()))
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
