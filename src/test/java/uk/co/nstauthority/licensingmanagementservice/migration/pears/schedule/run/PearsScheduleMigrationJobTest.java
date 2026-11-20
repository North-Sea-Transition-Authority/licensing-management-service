package uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.run;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.dao.QueryTimeoutException;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.PearsLicenceReference;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.operation.MigrationNotes;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.PearsScheduleCleardownOutcome;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.PearsScheduleCleardownService;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.PearsScheduleMigrationOutcome;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.PearsScheduleMigrationService;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.PearsScheduleService;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.PearsScheduleValidationService;

class PearsScheduleMigrationJobTest {

  private static final UUID RUN_ID = UUID.randomUUID();

  private final PearsScheduleMigrationRunService runService = mock(PearsScheduleMigrationRunService.class);
  private final PearsScheduleMigrationService migrationService = mock(PearsScheduleMigrationService.class);
  private final PearsScheduleService pearsScheduleService = mock(PearsScheduleService.class);

  @Test
  void workNextBatch_whenTheOutcomeCannotBeSaved_leavesTheLicencePendingRatherThanRecordingItAsFailed() {
    var licence = new PearsLicenceReference("P", 1);
    when(runService.nextBatch(10)).thenReturn(Optional.of(
        new PearsScheduleMigrationRunService.Batch(RUN_ID, PearsScheduleMigrationRunKind.MIGRATE, List.of("P1"))));
    when(runService.isActive(RUN_ID)).thenReturn(true);
    when(migrationService.migrateLicence(eq(licence), any(), eq(RUN_ID))).thenReturn(new PearsScheduleMigrationOutcome(
        licence, PearsScheduleMigrationOutcome.Result.MIGRATED, null, new MigrationNotes()));
    var databaseDown = new QueryTimeoutException("LMS database went away");
    when(runService.recordOutcome(any(), any(), any(), any(), anyMap(), any())).thenThrow(databaseDown);
    var job = new PearsScheduleMigrationJob(
        runService, migrationService, pearsScheduleService, mock(PearsScheduleCleardownService.class),
        mock(PearsScheduleValidationService.class), Clock.systemUTC(), 10);

    assertThatThrownBy(job::workNextBatch).isSameAs(databaseDown);

    verify(runService).recordOutcome(
        eq(RUN_ID), eq("P1"), eq(PearsScheduleMigrationQueueStatus.MIGRATED), any(), anyMap(), any());
    verify(runService, times(1)).recordOutcome(any(), any(), any(), any(), anyMap(), any());
  }

  @Test
  void workNextBatch_whenAClearIsPutOff_leavesItAndTheRestOfTheBatchPendingForTheNextPass() {
    when(runService.nextBatch(10)).thenReturn(Optional.of(
        new PearsScheduleMigrationRunService.Batch(RUN_ID, PearsScheduleMigrationRunKind.CLEARDOWN, List.of("P1", "P2"))));
    when(runService.isActive(RUN_ID)).thenReturn(true);
    var cleardownService = mock(PearsScheduleCleardownService.class);
    when(cleardownService.clear("P1")).thenReturn(new PearsScheduleCleardownOutcome(
        PearsScheduleCleardownOutcome.Result.POSTPONED, "The licence reminder job is running"));
    var job = new PearsScheduleMigrationJob(
        runService, migrationService, pearsScheduleService, cleardownService, mock(PearsScheduleValidationService.class),
        Clock.systemUTC(), 10);

    job.workNextBatch();

    verify(cleardownService, never()).clear("P2");
    verify(runService, never()).recordOutcome(any(), any(), any(), any(), anyMap(), any());
  }
}
