package uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.run;

import jakarta.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;
import uk.co.nstauthority.licensingmanagementservice.migration.MigrationPreconditionException;

/**
 * The endpoints every PEARS migration has for following its runs, which share one queue: a run's status, its licences
 * as a CSV, retrying its failed licences, resuming it after a restart and cancelling it. Each controller only acts
 * on its own migration's runs: the other migration's are not found.
 *
 * <p>Messages never repeat the licence reference a request gave, so nothing from a request is written back.</p>
 */
public abstract class PearsMigrationRunController {

  private final PearsScheduleMigrationRunService runService;
  private final PearsMigration migration;
  private final String reportFilePrefix;
  private final boolean cleardownEnabled;

  protected PearsMigrationRunController(
      PearsScheduleMigrationRunService runService,
      PearsMigration migration,
      String reportFilePrefix,
      boolean cleardownEnabled
  ) {
    this.runService = runService;
    this.migration = migration;
    this.reportFilePrefix = reportFilePrefix;
    this.cleardownEnabled = cleardownEnabled;
  }

  @GetMapping("/{runId}")
  public ResponseEntity<PearsScheduleMigrationRunReport> getRun(@PathVariable UUID runId) {
    return ResponseEntity.of(runService.report(runId).filter(report -> report.migration() == migration));
  }

  /**
   * Every licence of a run as a CSV download: what became of each, and for a dry run its issues and result counts.
   */
  @GetMapping("/{runId}/report")
  public ResponseEntity<String> getRunReport(@PathVariable UUID runId) {
    if (!isOwnRun(runId)) {
      return ResponseEntity.notFound().build();
    }
    return runService.licences(runId)
        .map(licences -> ResponseEntity.ok()
            .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"%s-%s.csv\"".formatted(reportFilePrefix, runId))
            .body(PearsScheduleMigrationRunCsv.csv(licences)))
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  @PostMapping("/{runId}/retry-failed")
  public ResponseEntity<PearsScheduleMigrationRunReport> retryFailed(@PathVariable UUID runId) {
    return isOwnRun(runId) ? ResponseEntity.of(runService.retryFailed(runId)) : ResponseEntity.notFound().build();
  }

  @PostMapping("/{runId}/resume")
  public ResponseEntity<PearsScheduleMigrationRunReport> resume(@PathVariable UUID runId) {
    return isOwnRun(runId) ? ResponseEntity.of(runService.resume(runId)) : ResponseEntity.notFound().build();
  }

  @PostMapping("/{runId}/cancel")
  public ResponseEntity<PearsScheduleMigrationRunReport> cancel(@PathVariable UUID runId) {
    return isOwnRun(runId) ? ResponseEntity.of(runService.cancel(runId)) : ResponseEntity.notFound().build();
  }

  private boolean isOwnRun(UUID runId) {
    return runService.report(runId).map(report -> report.migration() == migration).orElse(false);
  }

  /**
   * Returned when PEARS can't be read or has nothing to migrate, or a run can't be started, retried, resumed or
   * cancelled in the state it is in. Nothing has changed, so the request can be made again.
   */
  @ExceptionHandler(MigrationPreconditionException.class)
  public ResponseEntity<String> handleMigrationPreconditionException(MigrationPreconditionException exception) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .contentType(MediaType.TEXT_PLAIN)
        .body(exception.getMessage());
  }

  /**
   * The licences to queue: all of them, or just the one asked for.
   *
   * @param noneToQueue why there are none, which doesn't repeat the licence asked for
   * @throws MigrationPreconditionException if there are none
   */
  protected static List<String> licencesToQueue(
      List<String> licenceReferences,
      @Nullable String licenceReference,
      String noneToQueue
  ) {
    var toQueue = licenceReferences.stream()
        .filter(reference -> licenceReference == null || reference.equals(licenceReference))
        .toList();
    if (toQueue.isEmpty()) {
      throw new MigrationPreconditionException(noneToQueue);
    }
    return toQueue;
  }

  /**
   * Not found unless {@code pears.schedule-migration.cleardown-enabled} is set, which it never is in production.
   */
  protected void failUnlessCleardownEnabled() {
    if (!cleardownEnabled) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
  }

  protected static ResponseEntity<StartedRun> started(PearsScheduleMigrationRun run, int licences) {
    return ResponseEntity.status(HttpStatus.ACCEPTED).body(new StartedRun(run.getId(), licences));
  }

  public record StartedRun(UUID runId, int licences) {
  }
}
