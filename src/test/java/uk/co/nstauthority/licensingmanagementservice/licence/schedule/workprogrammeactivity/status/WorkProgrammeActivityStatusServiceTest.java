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
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventreference.EventReference;
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
    var activityRef = new EventReference();
    activityRef.setId(UUID.randomUUID());
    activity.setEventReference(activityRef);

    when(workProgrammeActivityStatusRepository.findAllByEventReference(activity.getEventReference()))
        .thenReturn(List.of());

    when(clock.instant()).thenReturn(instant);

    workProgrammeActivityStatusService.createInitialStatusFor(activity);

    verify(workProgrammeActivityStatusRepository).save(workProgrammeActivityStatusArgumentCaptor.capture());

    assertThat(workProgrammeActivityStatusArgumentCaptor.getValue())
        .extracting(
            WorkProgrammeActivityStatus::getStatus,
            WorkProgrammeActivityStatus::getEventReference,
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
    var activityRef = new EventReference();
    activityRef.setId(UUID.randomUUID());
    activity.setEventReference(activityRef);

    var form = new WorkProgrammeActivityStatusForm();
    form.setStatus(WorkProgrammeStatus.IN_PROGRESS);

    when(clock.instant()).thenReturn(instant);

    workProgrammeActivityStatusService.saveStatusFromForm(form, activity);

    verify(workProgrammeActivityStatusRepository).save(workProgrammeActivityStatusArgumentCaptor.capture());

    assertThat(workProgrammeActivityStatusArgumentCaptor.getValue())
        .extracting(
            WorkProgrammeActivityStatus::getStatus,
            WorkProgrammeActivityStatus::getEventReference,
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
    var activityRef = new EventReference();
    activityRef.setId(UUID.randomUUID());
    activity.setEventReference(activityRef);

    var activityStatus = new WorkProgrammeActivityStatus();
    activityStatus.setAppliedDatetime(instant);

    var activityStatus2 = new WorkProgrammeActivityStatus();
    activityStatus2.setAppliedDatetime(instant.plusSeconds(100L));

    when(workProgrammeActivityStatusRepository.findAllByEventReference(activity.getEventReference()))
        .thenReturn(List.of(activityStatus, activityStatus2));

    assertThat(workProgrammeActivityStatusService.getLatestStatusFor(activity)).isEqualTo(activityStatus2);
  }

  @Test
  void getLatestStatusesFor() {
    var activity = new WorkProgrammeActivity();
    var activityRef = new EventReference();
    activityRef.setId(UUID.randomUUID());
    activity.setEventReference(activityRef);

    var activityStatus = new WorkProgrammeActivityStatus();
    activityStatus.setEventReference(activity.getEventReference());
    activityStatus.setAppliedDatetime(Instant.now());

    var activityStatus2 = new WorkProgrammeActivityStatus();
    activityStatus2.setEventReference(activity.getEventReference());
    activityStatus2.setAppliedDatetime(Instant.now().plus(1, ChronoUnit.DAYS));

    var activity2 = new WorkProgrammeActivity();
    var activity2Ref = new EventReference();
    activity2Ref.setId(UUID.randomUUID());
    activity2.setEventReference(activity2Ref);

    var activityStatus3 = new WorkProgrammeActivityStatus();
    activityStatus3.setEventReference(activity2.getEventReference());
    activityStatus3.setAppliedDatetime(Instant.now());

    var refList = List.of(activity.getEventReference(), activity2.getEventReference());

    when(workProgrammeActivityStatusRepository.findAllByEventReferenceIn(refList))
        .thenReturn(List.of(activityStatus, activityStatus2, activityStatus3));

    assertThat(workProgrammeActivityStatusService.getLatestStatusesFor(List.of(activity, activity2)))
        .containsExactly(
            entry(activity.getEventReference().getId(), activityStatus2),
            entry(activity2.getEventReference().getId(), activityStatus3)
        );
  }

  @Test
  void getStatusForm() {
    var activity = new WorkProgrammeActivity();
    var activityRef = new EventReference();
    activityRef.setId(UUID.randomUUID());
    activity.setEventReference(activityRef);

    var activityStatus = new WorkProgrammeActivityStatus();
    activityStatus.setStatus(WorkProgrammeStatus.IN_PROGRESS);
    activityStatus.setAppliedDatetime(instant);

    when(workProgrammeActivityStatusRepository.findAllByEventReference(activity.getEventReference()))
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