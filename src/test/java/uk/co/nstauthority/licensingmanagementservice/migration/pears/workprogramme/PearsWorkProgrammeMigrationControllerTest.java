package uk.co.nstauthority.licensingmanagementservice.migration.pears.workprogramme;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import static uk.co.nstauthority.licensingmanagementservice.authentication.TestUserProvider.user;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.migration.MigrationPreconditionException;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.PearsLicenceReference;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.run.PearsMigration;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.run.PearsScheduleMigrationQueueStatus;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.run.PearsScheduleMigrationRun;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.run.PearsScheduleMigrationRunKind;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.run.PearsScheduleMigrationRunReport;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.run.PearsScheduleMigrationRunService;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.run.PearsScheduleMigrationRunStatus;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@ActiveProfiles("migration")
@TestPropertySource(properties = {
    "pears.datasource.url=not-used-by-controller-tests",
    "pears.datasource.username=pears",
    "pears.datasource.password=pears",
    "pears.schedule-migration.cleardown-enabled=true"
})
@ContextConfiguration(classes = PearsWorkProgrammeMigrationController.class)
class PearsWorkProgrammeMigrationControllerTest extends AbstractControllerTest {

  private static final UUID RUN_ID = UUID.randomUUID();

  private static final String START_ROUTE =
      ReverseRouter.route(on(PearsWorkProgrammeMigrationController.class).startRun(null, null));
  private static final String CLEARDOWN_ROUTE =
      ReverseRouter.route(on(PearsWorkProgrammeMigrationController.class).startCleardown(null, null));
  private static final String REPORT_ROUTE =
      ReverseRouter.route(on(PearsWorkProgrammeMigrationController.class).getRunReport(RUN_ID));
  private static final String RETRY_ROUTE =
      ReverseRouter.route(on(PearsWorkProgrammeMigrationController.class).retryFailed(RUN_ID));
  private static final String CANCEL_ROUTE =
      ReverseRouter.route(on(PearsWorkProgrammeMigrationController.class).cancel(RUN_ID));

  @MockitoBean
  private PearsWorkProgrammeMigrationService migrationService;

  @MockitoBean
  private PearsScheduleMigrationRunService runService;

  @MockitoBean
  private PearsWorkProgrammeCleardownService cleardownService;

  @BeforeEach
  void givenTheRunIsAWorkProgrammeRun() {
    when(runService.report(RUN_ID)).thenReturn(Optional.of(report(PearsMigration.WORK_PROGRAMME)));
  }

  @Test
  void startValidation_whenGet_queuesADryRunOfEveryLicenceWithAPearsWorkProgramme() throws Exception {
    when(migrationService.licencesToMigrate())
        .thenReturn(List.of(new PearsLicenceReference("P", 2244), new PearsLicenceReference("P", 1161)));
    var run = mock(PearsScheduleMigrationRun.class);
    when(run.getId()).thenReturn(RUN_ID);
    when(runService.startWorkProgrammeValidation(any(), anyLong())).thenReturn(run);

    mockMvc.perform(get(ReverseRouter.route(on(PearsWorkProgrammeMigrationController.class)
            .startValidation(null, null))).with(user(regulatorUser)))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.licences").value(2));

    verify(runService).startWorkProgrammeValidation(List.of("P2244", "P1161"), regulatorUser.wuaId());
  }

  @Test
  void startRun_queuesEveryLicenceWithAPearsWorkProgramme() throws Exception {
    when(migrationService.licencesToMigrate())
        .thenReturn(List.of(new PearsLicenceReference("P", 2244), new PearsLicenceReference("P", 1161)));
    givenTheRunStarts();

    mockMvc.perform(post(START_ROUTE).with(user(regulatorUser)).with(csrf()))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.runId").value(RUN_ID.toString()))
        .andExpect(jsonPath("$.licences").value(2));

    verify(runService).startWorkProgrammeRun(List.of("P2244", "P1161"), regulatorUser.wuaId());
  }

  @Test
  void startRun_whenGet_withALicenceReference_queuesJustThatLicence() throws Exception {
    when(migrationService.licencesToMigrate())
        .thenReturn(List.of(new PearsLicenceReference("P", 2244), new PearsLicenceReference("P", 1161)));
    givenTheRunStarts();

    mockMvc.perform(get(START_ROUTE).param("licenceReference", "P1161").with(user(regulatorUser)))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.licences").value(1));

    verify(runService).startWorkProgrammeRun(List.of("P1161"), regulatorUser.wuaId());
  }

  @Test
  void startRun_whenNoScheduleHasBeenMigrated_isAConflict() throws Exception {
    when(migrationService.licencesToMigrate()).thenThrow(new MigrationPreconditionException(
        "No schedule has been migrated from PEARS. Migrate schedules before work programmes."));

    mockMvc.perform(post(START_ROUTE).with(user(regulatorUser)).with(csrf()))
        .andExpect(status().isConflict())
        .andExpect(content().string(
            "No schedule has been migrated from PEARS. Migrate schedules before work programmes."));
    verifyNoInteractions(runService);
  }

  @Test
  void startRun_whenARunIsActive_isAConflict() throws Exception {
    when(migrationService.licencesToMigrate()).thenReturn(List.of(new PearsLicenceReference("P", 2244)));
    when(runService.startWorkProgrammeRun(any(), anyLong()))
        .thenThrow(new MigrationPreconditionException("Run is still RUNNING"));

    mockMvc.perform(post(START_ROUTE).with(user(regulatorUser)).with(csrf()))
        .andExpect(status().isConflict())
        .andExpect(content().string("Run is still RUNNING"));
  }

  @Test
  void startCleardown_queuesEveryLicenceWithAMigratedWorkProgramme() throws Exception {
    when(cleardownService.licencesToClear()).thenReturn(List.of("P1161", "P2244"));
    var run = mock(PearsScheduleMigrationRun.class);
    when(run.getId()).thenReturn(RUN_ID);
    when(runService.startWorkProgrammeCleardown(any(), anyLong())).thenReturn(run);

    mockMvc.perform(post(CLEARDOWN_ROUTE).with(user(regulatorUser)).with(csrf()))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.licences").value(2));

    verify(runService).startWorkProgrammeCleardown(List.of("P1161", "P2244"), regulatorUser.wuaId());
  }

  @Test
  void startCleardown_whenNothingWasMigrated_isAConflict() throws Exception {
    when(cleardownService.licencesToClear()).thenReturn(List.of());

    mockMvc.perform(post(CLEARDOWN_ROUTE).with(user(regulatorUser)).with(csrf()))
        .andExpect(status().isConflict())
        .andExpect(content().string("No work programmes from the PEARS migration to clear"));
  }

  @Test
  void getRunReport_isACsvOfTheRunsLicences() throws Exception {
    when(runService.licences(RUN_ID)).thenReturn(Optional.of(List.of(new PearsScheduleMigrationRunService.RunLicence(
        "P2244", PearsScheduleMigrationQueueStatus.MIGRATED, 1, null, Map.of()))));

    mockMvc.perform(get(REPORT_ROUTE).with(user(regulatorUser)))
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Disposition",
            "attachment; filename=\"pears-work-programme-run-%s.csv\"".formatted(RUN_ID)));
  }

  @Test
  void retryFailedAndCancel_whenTheRunIsUnknown_areNotFound() throws Exception {
    when(runService.retryFailed(RUN_ID)).thenReturn(Optional.empty());
    when(runService.cancel(RUN_ID)).thenReturn(Optional.empty());

    mockMvc.perform(post(RETRY_ROUTE).with(user(regulatorUser)).with(csrf())).andExpect(status().isNotFound());
    mockMvc.perform(post(CANCEL_ROUTE).with(user(regulatorUser)).with(csrf())).andExpect(status().isNotFound());
  }

  @Test
  void cancel_whenTheRunIsAScheduleRun_isNotFoundAndCancelsNothing() throws Exception {
    when(runService.report(RUN_ID)).thenReturn(Optional.of(report(PearsMigration.SCHEDULE)));

    mockMvc.perform(post(CANCEL_ROUTE).with(user(regulatorUser)).with(csrf())).andExpect(status().isNotFound());
    mockMvc.perform(post(RETRY_ROUTE).with(user(regulatorUser)).with(csrf())).andExpect(status().isNotFound());
    mockMvc.perform(get(REPORT_ROUTE).with(user(regulatorUser))).andExpect(status().isNotFound());

    verify(runService, never()).cancel(RUN_ID);
    verify(runService, never()).retryFailed(RUN_ID);
  }

  private static PearsScheduleMigrationRunReport report(PearsMigration migration) {
    return new PearsScheduleMigrationRunReport(RUN_ID, migration, PearsScheduleMigrationRunKind.MIGRATE,
        PearsScheduleMigrationRunStatus.RUNNING, 1L, Instant.parse("2026-11-25T09:00:00Z"), null, Map.of(), List.of(),
        List.of(), List.of(), Map.of());
  }

  private void givenTheRunStarts() {
    var run = mock(PearsScheduleMigrationRun.class);
    when(run.getId()).thenReturn(RUN_ID);
    when(runService.startWorkProgrammeRun(any(), anyLong())).thenReturn(run);
  }

  @Nested
  @TestPropertySource(properties = "pears.schedule-migration.cleardown-enabled=false")
  class WhenCleardownIsNotEnabled {

    @Test
    void startCleardown_isNotFound() throws Exception {
      mockMvc.perform(post(CLEARDOWN_ROUTE).with(user(regulatorUser)).with(csrf()))
          .andExpect(status().isNotFound());

      verifyNoInteractions(cleardownService, runService);
    }
  }
}
