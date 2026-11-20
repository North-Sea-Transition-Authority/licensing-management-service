package uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.run;

import jakarta.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.migration.MigrationPreconditionException;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.ConditionalOnPearsDataSource;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.PearsLicenceReference;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.PearsScheduleCleardownService;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.PearsScheduleMigrationService;

/**
 * Starts and follows runs of the PEARS schedule migration, which {@link PearsScheduleMigrationJob} works through in
 * the background.
 *
 * <p>Starting a run accepts GET as well as POST, so it can be triggered by pasting the URL into a logged in browser,
 * which is the only practical way to reach it by hand given it sits behind SAML authentication. Retrying, cancelling
 * and clearing down are POST only.</p>
 */
@Profile("migration")
@ConditionalOnPearsDataSource
@RestController
@RequestMapping("/migration/pears-schedules")
public class PearsScheduleMigrationController {

  private final PearsScheduleMigrationService migrationService;
  private final PearsScheduleMigrationRunService runService;
  private final PearsScheduleCleardownService cleardownService;
  private final boolean cleardownEnabled;

  public PearsScheduleMigrationController(
      PearsScheduleMigrationService migrationService,
      PearsScheduleMigrationRunService runService,
      PearsScheduleCleardownService cleardownService,
      @Value("${pears.schedule-migration.cleardown-enabled}") boolean cleardownEnabled
  ) {
    this.migrationService = migrationService;
    this.runService = runService;
    this.cleardownService = cleardownService;
    this.cleardownEnabled = cleardownEnabled;
  }

  /**
   * Queues a run of every licence with a PEARS schedule, or of just the one given.
   */
  @RequestMapping(method = {RequestMethod.GET, RequestMethod.POST})
  public ResponseEntity<StartedRun> startRun(
      @RequestParam(required = false) String licenceReference,
      ServiceUserDetail user
  ) {
    var licenceReferences = licencesWithAPearsSchedule(licenceReference);
    var run = runService.startRun(licenceReferences, user.wuaId());
    return ResponseEntity.status(HttpStatus.ACCEPTED).body(new StartedRun(run.getId(), licenceReferences.size()));
  }

  /**
   * Queues a dry run of every licence with a PEARS schedule, or of just the one given: each is migrated and compared
   * with PEARS as a run would, then rolled back, so nothing in LMS changes. Follow it, and read the licences with issues,
   * at the usual status endpoint. GET as well as POST, as it changes nothing.
   */
  @RequestMapping(value = "/validate", method = {RequestMethod.GET, RequestMethod.POST})
  public ResponseEntity<StartedRun> startValidation(
      @RequestParam(required = false) String licenceReference,
      ServiceUserDetail user
  ) {
    var licenceReferences = licencesWithAPearsSchedule(licenceReference);
    var run = runService.startValidation(licenceReferences, user.wuaId());
    return ResponseEntity.status(HttpStatus.ACCEPTED).body(new StartedRun(run.getId(), licenceReferences.size()));
  }

  private List<String> licencesWithAPearsSchedule(@Nullable String licenceReference) {
    var licenceReferences = migrationService.licencesToMigrate().stream()
        .map(PearsLicenceReference::licenceReference)
        .filter(reference -> licenceReference == null || reference.equals(licenceReference))
        .toList();

    if (licenceReferences.isEmpty()) {
      throw new MigrationPreconditionException("PEARS holds no schedule for %s".formatted(licenceReference));
    }
    return licenceReferences;
  }

  /**
   * Queues a cleardown of every schedule the migration created, or of just the given licence's, so the migration can
   * be run again. Not found unless {@code pears.schedule-migration.cleardown-enabled} is set, which it never is in
   * production.
   */
  @PostMapping("/cleardown")
  public ResponseEntity<StartedRun> startCleardown(
      @RequestParam(required = false) String licenceReference,
      ServiceUserDetail user
  ) {
    if (!cleardownEnabled) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }

    var licenceReferences = cleardownService.licencesToClear().stream()
        .filter(reference -> licenceReference == null || reference.equals(licenceReference))
        .toList();

    if (licenceReferences.isEmpty()) {
      throw new MigrationPreconditionException(licenceReference == null
          ? "No schedules from the PEARS migration to clear"
          : "%s has no schedule from the PEARS migration".formatted(licenceReference));
    }

    var run = runService.startCleardown(licenceReferences, user.wuaId());
    return ResponseEntity.status(HttpStatus.ACCEPTED).body(new StartedRun(run.getId(), licenceReferences.size()));
  }

  @GetMapping("/{runId}")
  public ResponseEntity<PearsScheduleMigrationRunReport> getRun(@PathVariable UUID runId) {
    return ResponseEntity.of(runService.report(runId));
  }

  /**
   * Every licence of a run as a CSV download: what became of each, and for a dry run its issues and result counts.
   */
  @GetMapping("/{runId}/report")
  public ResponseEntity<String> getRunReport(@PathVariable UUID runId) {
    return runService.licences(runId)
        .map(licences -> ResponseEntity.ok()
            .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"pears-schedule-run-%s.csv\"".formatted(runId))
            .body(PearsScheduleMigrationRunCsv.csv(licences)))
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  @PostMapping("/{runId}/retry-failed")
  public ResponseEntity<PearsScheduleMigrationRunReport> retryFailed(@PathVariable UUID runId) {
    return ResponseEntity.of(runService.retryFailed(runId));
  }

  @PostMapping("/{runId}/cancel")
  public ResponseEntity<PearsScheduleMigrationRunReport> cancel(@PathVariable UUID runId) {
    return ResponseEntity.of(runService.cancel(runId));
  }

  /**
   * Returned when PEARS can't be read or has nothing to migrate, or a run can't be started, retried or cancelled in the
   * state it is in. Nothing has changed, so the request can be made again.
   */
  @ExceptionHandler(MigrationPreconditionException.class)
  public ResponseEntity<String> handleMigrationPreconditionException(MigrationPreconditionException exception) {
    return ResponseEntity.status(HttpStatus.CONFLICT).body(exception.getMessage());
  }

  public record StartedRun(UUID runId, int licences) {
  }
}
