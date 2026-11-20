package uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.run;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Where a run has got to, with the licences that were skipped or failed and why.
 *
 * @param withIssues        for a dry run, the licences whose comparison found issues, with the first of them
 * @param comparisonResults for a dry run, how many comparison rows had each result across the run; otherwise empty
 */
public record PearsScheduleMigrationRunReport(
    UUID runId,
    PearsScheduleMigrationRunKind kind,
    PearsScheduleMigrationRunStatus status,
    long requestedBy,
    Instant requestedAt,
    Instant completedAt,
    Map<PearsScheduleMigrationQueueStatus, Long> licencesByStatus,
    List<LicenceOutcome> failed,
    List<LicenceOutcome> skipped,
    List<LicenceOutcome> withIssues,
    Map<String, Long> comparisonResults
) {

  public record LicenceOutcome(String licenceReference, String reason, int attempts) {
  }
}
