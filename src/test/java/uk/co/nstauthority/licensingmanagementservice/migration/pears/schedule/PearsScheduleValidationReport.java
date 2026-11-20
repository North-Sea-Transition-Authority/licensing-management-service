package uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.Tally;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.PearsScheduleComparison.Result;

/**
 * What the schedule validation sweep found, gathered across licences swept in parallel and written out once at the end,
 * where the {@code sync-reports} CI step publishes it alongside the position comparison.
 */
class PearsScheduleValidationReport {

  private static final Path REPORT = Path.of("build", "reports", "pears-migration", "schedule-comparison.txt");

  private static final String NEW_LINE = System.lineSeparator();

  private int licencesSwept;
  private final Map<Result, Long> rowsByResult = new EnumMap<>(Result.class);
  private final Tally outcomes = new Tally(NEW_LINE + "Migration outcomes:");
  private final Tally notes = new Tally(NEW_LINE + "Migration notes (licences noting each):");
  private final Tally licencesByResult = new Tally(NEW_LINE + "Comparison results (licences with each):");
  private final Tally issues = new Tally(NEW_LINE + "Issues to look into (licences with each):");
  private final Tally notMigrated = new Tally(NEW_LINE + "Events not migrated, by reason (licences with each):");
  private final Tally notCompared = new Tally(NEW_LINE + "Licences not compared:");
  private final Tally failures = new Tally(NEW_LINE + "Licences that could not be compared at all:");

  synchronized void recordLicenceSwept() {
    licencesSwept++;
  }

  synchronized void recordOutcome(String licenceReference, PearsScheduleMigrationOutcome outcome) {
    var key = outcome.reason() == null
        ? outcome.result().name()
        : "%s: %s".formatted(outcome.result().name(), outcome.reason());
    outcomes.add(key, licenceReference);
    outcome.notes().noteCountsByReason().keySet().forEach(reason -> notes.add(reason, licenceReference));
  }

  synchronized void recordComparison(PearsScheduleComparison comparison) {
    var licenceReference = comparison.licenceReference();
    if (comparison.problem() != null) {
      notCompared.add(comparison.problem(), licenceReference);
      return;
    }

    var results = new HashSet<String>();
    var issueResults = new HashSet<String>();
    var notMigratedReasons = new HashSet<String>();
    for (var version : comparison.versions()) {
      for (var row : version.rows()) {
        rowsByResult.merge(row.result(), 1L, Long::sum);
        addOnce(licencesByResult, results, row.result().getText(), licenceReference);

        if (row.result().isIssue()) {
          addOnce(issues, issueResults, row.result().getText(), licenceReference);
          issues.example(row.result().getText(), licenceReference, PearsScheduleComparison.describe(version, row));
        }
        if (row.result() == Result.NOT_MIGRATED) {
          var reason = Objects.requireNonNullElse(row.detail(), "Version or licence left out of the migration");
          addOnce(notMigrated, notMigratedReasons, reason, licenceReference);
          notMigrated.example(reason, licenceReference, PearsScheduleComparison.describe(version, row));
        }
      }
    }
  }

  synchronized void recordNotCompared(String licenceReference, String reason) {
    notCompared.add(reason, licenceReference);
  }

  synchronized void recordFailure(String licenceReference, Exception e) {
    var message = Objects.toString(e.getMessage(), "").lines().findFirst().orElse("");
    failures.add("%s: %s".formatted(e.getClass().getSimpleName(), message), licenceReference);
  }

  /**
   * Prints the report and writes it under {@code build/reports}.
   */
  synchronized void publish() {
    var report = render();
    System.out.println(report);
    try {
      Files.createDirectories(REPORT.getParent());
      Files.writeString(REPORT, report, StandardCharsets.UTF_8);
      System.out.println("Report written to " + REPORT.toAbsolutePath());
    } catch (IOException e) {
      throw new UncheckedIOException("Could not write " + REPORT.toAbsolutePath(), e);
    }
  }

  synchronized String render() {
    var out = new StringBuilder()
        .append(NEW_LINE)
        .append("=== Licence schedules migrated and compared with PEARS ===")
        .append(NEW_LINE)
        .append("  %,d licences swept%n".formatted(licencesSwept));

    if (!rowsByResult.isEmpty()) {
      out.append(NEW_LINE).append("Comparison rows, every version:").append(NEW_LINE);
      rowsByResult.forEach((result, count) ->
          out.append("  %-64s %,9d%s%n".formatted(result.getText(), count, result.isIssue() ? "  (issue)" : "")));
    }

    for (var tally : List.of(outcomes, notes, licencesByResult, issues, notMigrated, notCompared, failures)) {
      tally.appendTo(out);
    }
    return out.toString();
  }

  private static void addOnce(Tally tally, Set<String> added, String key, String licenceReference) {
    if (added.add(key)) {
      tally.add(key, licenceReference);
    }
  }

}
