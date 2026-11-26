package uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.run;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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
import static uk.co.nstauthority.licensingmanagementservice.util.RedirectedToLoginUrlMatcher.redirectionToLoginUrl;

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
import uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.PearsScheduleCleardownService;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.PearsScheduleMigrationService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@ActiveProfiles("migration")
@TestPropertySource(properties = {
    "pears.datasource.url=not-used-by-controller-tests",
    "pears.datasource.username=pears",
    "pears.datasource.password=pears",
    "pears.schedule-migration.cleardown-enabled=true"
})
@ContextConfiguration(classes = PearsScheduleMigrationController.class)
class PearsScheduleMigrationControllerTest extends AbstractControllerTest {

  private static final UUID RUN_ID = UUID.randomUUID();

  private static final String VALIDATE_ROUTE =
      ReverseRouter.route(on(PearsScheduleMigrationController.class).startValidation(null, null));

  private static final String CLEARDOWN_ROUTE =
      ReverseRouter.route(on(PearsScheduleMigrationController.class).startCleardown(null, null));

  private static final String START_ROUTE =
      ReverseRouter.route(on(PearsScheduleMigrationController.class).startRun(null, null));
  private static final String REPORT_ROUTE =
      ReverseRouter.route(on(PearsScheduleMigrationController.class).getRunReport(RUN_ID));

  private static final String RUN_ROUTE =
      ReverseRouter.route(on(PearsScheduleMigrationController.class).getRun(RUN_ID));
  private static final String RETRY_ROUTE =
      ReverseRouter.route(on(PearsScheduleMigrationController.class).retryFailed(RUN_ID));
  private static final String RESUME_ROUTE =
      ReverseRouter.route(on(PearsScheduleMigrationController.class).resume(RUN_ID));
  private static final String CANCEL_ROUTE =
      ReverseRouter.route(on(PearsScheduleMigrationController.class).cancel(RUN_ID));

  @MockitoBean
  private PearsScheduleMigrationService migrationService;

  @MockitoBean
  private PearsScheduleMigrationRunService runService;

  @MockitoBean
  private PearsScheduleCleardownService cleardownService;

  @BeforeEach
  void givenTheRunIsAScheduleRun() {
    when(runService.report(RUN_ID)).thenReturn(Optional.of(report(PearsScheduleMigrationRunStatus.RUNNING)));
  }

  @Test
  void startRun_whenPost_queuesEveryLicenceWithAPearsSchedule() throws Exception {
    givenPearsHasSchedulesFor(new PearsLicenceReference("P", 2244), new PearsLicenceReference("P", 1161));
    givenTheRunStarts();

    mockMvc.perform(post(START_ROUTE).with(user(regulatorUser)).with(csrf()))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.runId").value(RUN_ID.toString()))
        .andExpect(jsonPath("$.licences").value(2));

    verify(runService).startRun(List.of("P2244", "P1161"), regulatorUser.wuaId());
  }

  @Test
  void startRun_whenGet_queuesEveryLicenceWithAPearsSchedule() throws Exception {
    givenPearsHasSchedulesFor(new PearsLicenceReference("P", 2244));
    givenTheRunStarts();

    mockMvc.perform(get(START_ROUTE).with(user(regulatorUser)))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.licences").value(1));
  }

  @Test
  void startRun_withALicenceReference_queuesJustThatLicence() throws Exception {
    givenPearsHasSchedulesFor(new PearsLicenceReference("P", 2244), new PearsLicenceReference("P", 1161));
    givenTheRunStarts();

    mockMvc.perform(post(START_ROUTE).param("licenceReference", "P1161").with(user(regulatorUser)).with(csrf()))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.licences").value(1));

    verify(runService).startRun(List.of("P1161"), regulatorUser.wuaId());
  }

  @Test
  void startValidation_whenGet_queuesADryRunOfEveryLicenceWithAPearsSchedule() throws Exception {
    givenPearsHasSchedulesFor(new PearsLicenceReference("P", 2244), new PearsLicenceReference("P", 1161));
    givenTheDryRunStarts();

    mockMvc.perform(get(VALIDATE_ROUTE).with(user(regulatorUser)))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.runId").value(RUN_ID.toString()))
        .andExpect(jsonPath("$.licences").value(2));

    verify(runService).startValidation(List.of("P2244", "P1161"), regulatorUser.wuaId());
  }

  @Test
  void startValidation_withALicenceReference_queuesADryRunOfJustThatLicence() throws Exception {
    givenPearsHasSchedulesFor(new PearsLicenceReference("P", 2244), new PearsLicenceReference("P", 1161));
    givenTheDryRunStarts();

    mockMvc.perform(post(VALIDATE_ROUTE).param("licenceReference", "P1161").with(user(regulatorUser)).with(csrf()))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.licences").value(1));

    verify(runService).startValidation(List.of("P1161"), regulatorUser.wuaId());
  }

  @Test
  void startValidation_withALicencePearsHoldsNoScheduleFor_isAConflict() throws Exception {
    givenPearsHasSchedulesFor(new PearsLicenceReference("P", 2244));

    mockMvc.perform(get(VALIDATE_ROUTE).param("licenceReference", "P9999").with(user(regulatorUser)))
        .andExpect(status().isConflict())
        .andExpect(content().string("PEARS holds no schedule for the licence"));
  }

  @Test
  void startRun_withALicencePearsHoldsNoScheduleFor_isAConflict() throws Exception {
    givenPearsHasSchedulesFor(new PearsLicenceReference("P", 2244));

    mockMvc.perform(post(START_ROUTE).param("licenceReference", "P9999").with(user(regulatorUser)).with(csrf()))
        .andExpect(status().isConflict())
        .andExpect(content().string("PEARS holds no schedule for the licence"));

    verifyNoInteractions(runService);
  }

  @Test
  void startRun_whenPearsHasNothingToMigrate_isAConflict() throws Exception {
    when(migrationService.licencesToMigrate())
        .thenThrow(new MigrationPreconditionException("PEARS returned no licence schedules"));

    mockMvc.perform(post(START_ROUTE).with(user(regulatorUser)).with(csrf()))
        .andExpect(status().isConflict())
        .andExpect(content().string("PEARS returned no licence schedules"));
  }

  @Test
  void startRun_whenARunIsActive_isAConflict() throws Exception {
    givenPearsHasSchedulesFor(new PearsLicenceReference("P", 2244));
    when(runService.startRun(any(), anyLong())).thenThrow(new MigrationPreconditionException("Run is still RUNNING"));

    mockMvc.perform(post(START_ROUTE).with(user(regulatorUser)).with(csrf()))
        .andExpect(status().isConflict())
        .andExpect(content().string("Run is still RUNNING"));
  }

  @Test
  void startRun_whenNotAuthenticated_redirectsToLogin() throws Exception {
    mockMvc.perform(get(START_ROUTE))
        .andExpect(redirectionToLoginUrl());

    verifyNoInteractions(migrationService, runService);
  }

  @Test
  void getRun_reportsTheRun() throws Exception {
    when(runService.report(RUN_ID)).thenReturn(Optional.of(report(PearsScheduleMigrationRunStatus.RUNNING)));

    mockMvc.perform(get(RUN_ROUTE).with(user(regulatorUser)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.runId").value(RUN_ID.toString()))
        .andExpect(jsonPath("$.status").value("RUNNING"))
        .andExpect(jsonPath("$.licencesByStatus.MIGRATED").value(3))
        .andExpect(jsonPath("$.failed[0].licenceReference").value("P1161"))
        .andExpect(jsonPath("$.failed[0].reason").value("Could not be written"));
  }

  @Test
  void getRunReport_downloadsEveryLicenceOfTheRunAsCsv() throws Exception {
    when(runService.licences(RUN_ID)).thenReturn(Optional.of(List.of(new PearsScheduleMigrationRunService.RunLicence(
        "P1728", PearsScheduleMigrationQueueStatus.MATCHED, 1, null, Map.of("Result: Match", 19)))));

    mockMvc.perform(get(REPORT_ROUTE).with(user(regulatorUser)))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith("text/csv"))
        .andExpect(header().string("Content-Disposition",
            "attachment; filename=\"pears-schedule-run-%s.csv\"".formatted(RUN_ID)))
        .andExpect(content().string("Licence,Status,Attempts,Outcome,Match,Notes\r\nP1728,MATCHED,1,,19,\r\n"));
  }

  @Test
  void getRunReport_whenTheRunIsUnknown_isNotFound() throws Exception {
    when(runService.licences(RUN_ID)).thenReturn(Optional.empty());

    mockMvc.perform(get(REPORT_ROUTE).with(user(regulatorUser)))
        .andExpect(status().isNotFound());
  }

  @Test
  void getRun_whenTheRunIsUnknown_isNotFound() throws Exception {
    when(runService.report(RUN_ID)).thenReturn(Optional.empty());

    mockMvc.perform(get(RUN_ROUTE).with(user(regulatorUser)))
        .andExpect(status().isNotFound());
  }

  @Test
  void retryFailed_whenPost_queuesTheFailedLicencesAgain() throws Exception {
    when(runService.retryFailed(RUN_ID)).thenReturn(Optional.of(report(PearsScheduleMigrationRunStatus.RUNNING)));

    mockMvc.perform(post(RETRY_ROUTE).with(user(regulatorUser)).with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("RUNNING"));

    verify(runService).retryFailed(RUN_ID);
  }

  @Test
  void retryFailed_whenTheRunWasCancelled_isAConflict() throws Exception {
    when(runService.retryFailed(RUN_ID)).thenThrow(new MigrationPreconditionException("Run was cancelled"));

    mockMvc.perform(post(RETRY_ROUTE).with(user(regulatorUser)).with(csrf()))
        .andExpect(status().isConflict())
        .andExpect(content().string("Run was cancelled"));
  }

  @Test
  void retryFailed_whenTheRunIsUnknown_isNotFound() throws Exception {
    when(runService.retryFailed(RUN_ID)).thenReturn(Optional.empty());

    mockMvc.perform(post(RETRY_ROUTE).with(user(regulatorUser)).with(csrf()))
        .andExpect(status().isNotFound());
  }

  @Test
  void resume_whenPost_resumesTheRun() throws Exception {
    when(runService.resume(RUN_ID)).thenReturn(Optional.of(report(PearsScheduleMigrationRunStatus.RUNNING)));

    mockMvc.perform(post(RESUME_ROUTE).with(user(regulatorUser)).with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("RUNNING"));
  }

  @Test
  void resume_whenTheRunHasFinished_isAConflict() throws Exception {
    when(runService.resume(RUN_ID)).thenThrow(new MigrationPreconditionException("Run is already COMPLETE"));

    mockMvc.perform(post(RESUME_ROUTE).with(user(regulatorUser)).with(csrf()))
        .andExpect(status().isConflict())
        .andExpect(content().string("Run is already COMPLETE"));
  }

  @Test
  void resume_whenTheRunIsUnknown_isNotFound() throws Exception {
    when(runService.resume(RUN_ID)).thenReturn(Optional.empty());

    mockMvc.perform(post(RESUME_ROUTE).with(user(regulatorUser)).with(csrf()))
        .andExpect(status().isNotFound());
  }

  @Test
  void cancel_whenPost_cancelsTheRun() throws Exception {
    when(runService.cancel(RUN_ID)).thenReturn(Optional.of(report(PearsScheduleMigrationRunStatus.CANCELLED)));

    mockMvc.perform(post(CANCEL_ROUTE).with(user(regulatorUser)).with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CANCELLED"));
  }

  @Test
  void cancel_whenTheRunHasFinished_isAConflict() throws Exception {
    when(runService.cancel(RUN_ID)).thenThrow(new MigrationPreconditionException("Run is already COMPLETE"));

    mockMvc.perform(post(CANCEL_ROUTE).with(user(regulatorUser)).with(csrf()))
        .andExpect(status().isConflict())
        .andExpect(content().string("Run is already COMPLETE"));
  }

  @Test
  void cancel_whenTheRunIsUnknown_isNotFound() throws Exception {
    when(runService.cancel(RUN_ID)).thenReturn(Optional.empty());

    mockMvc.perform(post(CANCEL_ROUTE).with(user(regulatorUser)).with(csrf()))
        .andExpect(status().isNotFound());
  }

  @Test
  void retryFailed_whenGet_isNotAllowed() throws Exception {
    mockMvc.perform(get(RETRY_ROUTE).with(user(regulatorUser)))
        .andExpect(status().isMethodNotAllowed());

    verifyNoInteractions(runService);
  }

  @Test
  void cancel_whenGet_isNotAllowed() throws Exception {
    mockMvc.perform(get(CANCEL_ROUTE).with(user(regulatorUser)))
        .andExpect(status().isMethodNotAllowed());

    verifyNoInteractions(runService);
  }

  @Test
  void startCleardown_queuesEveryLicenceWithAMigratedSchedule() throws Exception {
    when(cleardownService.licencesToClear()).thenReturn(List.of("P1161", "P2244"));
    givenTheCleardownStarts();

    mockMvc.perform(post(CLEARDOWN_ROUTE).with(user(regulatorUser)).with(csrf()))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.runId").value(RUN_ID.toString()))
        .andExpect(jsonPath("$.licences").value(2));

    verify(runService).startCleardown(List.of("P1161", "P2244"), regulatorUser.wuaId());
  }

  @Test
  void startCleardown_withALicenceReference_queuesJustThatLicence() throws Exception {
    when(cleardownService.licencesToClear()).thenReturn(List.of("P1161", "P2244"));
    givenTheCleardownStarts();

    mockMvc.perform(post(CLEARDOWN_ROUTE).param("licenceReference", "P2244").with(user(regulatorUser)).with(csrf()))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.licences").value(1));

    verify(runService).startCleardown(List.of("P2244"), regulatorUser.wuaId());
  }

  @Test
  void startCleardown_withALicenceThatWasNotMigrated_isAConflict() throws Exception {
    when(cleardownService.licencesToClear()).thenReturn(List.of("P1161"));

    mockMvc.perform(post(CLEARDOWN_ROUTE).param("licenceReference", "P9999").with(user(regulatorUser)).with(csrf()))
        .andExpect(status().isConflict())
        .andExpect(content().string("The licence has no schedule from the PEARS migration"));

    verifyNoInteractions(runService);
  }

  @Test
  void startCleardown_whenNothingWasMigrated_isAConflict() throws Exception {
    when(cleardownService.licencesToClear()).thenReturn(List.of());

    mockMvc.perform(post(CLEARDOWN_ROUTE).with(user(regulatorUser)).with(csrf()))
        .andExpect(status().isConflict())
        .andExpect(content().string("No schedules from the PEARS migration to clear"));

    verifyNoInteractions(runService);
  }

  @Test
  void startCleardown_whenARunIsActive_isAConflict() throws Exception {
    when(cleardownService.licencesToClear()).thenReturn(List.of("P1161"));
    when(runService.startCleardown(any(), anyLong()))
        .thenThrow(new MigrationPreconditionException("Run is still RUNNING"));

    mockMvc.perform(post(CLEARDOWN_ROUTE).with(user(regulatorUser)).with(csrf()))
        .andExpect(status().isConflict())
        .andExpect(content().string("Run is still RUNNING"));
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

  private void givenPearsHasSchedulesFor(PearsLicenceReference... licences) {
    when(migrationService.licencesToMigrate()).thenReturn(List.of(licences));
  }

  private void givenTheDryRunStarts() {
    when(runService.startValidation(any(), anyLong())).thenAnswer(invocation -> new PearsScheduleMigrationRun(
        RUN_ID, PearsMigration.SCHEDULE, PearsScheduleMigrationRunKind.VALIDATE, invocation.getArgument(1), Instant.now()));
  }

  private void givenTheRunStarts() {
    when(runService.startRun(any(), anyLong())).thenAnswer(invocation -> new PearsScheduleMigrationRun(
          RUN_ID, PearsMigration.SCHEDULE, PearsScheduleMigrationRunKind.MIGRATE, invocation.getArgument(1), Instant.now()));
  }

  private void givenTheCleardownStarts() {
    when(runService.startCleardown(any(), anyLong())).thenAnswer(invocation -> new PearsScheduleMigrationRun(
        RUN_ID, PearsMigration.SCHEDULE, PearsScheduleMigrationRunKind.CLEARDOWN, invocation.getArgument(1), Instant.now()));
  }

  private static PearsScheduleMigrationRunReport report(PearsScheduleMigrationRunStatus status) {
    return new PearsScheduleMigrationRunReport(
        RUN_ID,
        PearsMigration.SCHEDULE,
        PearsScheduleMigrationRunKind.MIGRATE,
        status,
        1L,
        Instant.parse("2026-11-18T09:00:00Z"),
        null,
        Map.of(PearsScheduleMigrationQueueStatus.MIGRATED, 3L),
        List.of(new PearsScheduleMigrationRunReport.LicenceOutcome("P1161", "Could not be written", 1)),
        List.of(),
        List.of(),
        Map.of()
    );
  }
}
