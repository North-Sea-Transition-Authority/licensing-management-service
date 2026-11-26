package uk.co.nstauthority.licensingmanagementservice.migration.pears.workprogramme;

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
import uk.co.nstauthority.licensingmanagementservice.migration.pears.Tally;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.workprogramme.PearsWorkProgrammeComparison.Result;

/**
 * What the work programme validation sweep found, gathered across licences swept in parallel and written out once at
 * the end, next to the schedule sweep's report.
 */
class PearsWorkProgrammeValidationReport {

  private static final Path REPORT = Path.of("build", "reports", "pears-migration", "work-programme-comparison.txt");

  private static final String NEW_LINE = System.lineSeparator();

  private int licencesSwept;
  private final Map<Result, Long> rowsByResult = new EnumMap<>(Result.class);
  private final Tally scheduleOutcomes = new Tally(NEW_LINE + "Schedules not migrated, so not swept further:");
  private final Tally outcomes = new Tally(NEW_LINE + "Work programme migration outcomes:");
  private final Tally notes = new Tally(NEW_LINE + "Migration notes (licences noting each):");
  private final Tally issues = new Tally(NEW_LINE + "Issues to look into (licences with each):");
  private final Tally notMigrated = new Tally(NEW_LINE + "Activities not migrated, by reason (licences with each):");
  private final Tally notCompared = new Tally(NEW_LINE + "Licences not compared:");
  private final Tally failures = new Tally(NEW_LINE + "Licences that could not be compared at all:");

  synchronized void recordLicenceSwept() {
    licencesSwept++;
  }

  synchronized void recordScheduleNotMigrated(String licenceReference, String reason) {
    scheduleOutcomes.add(reason, licenceReference);
  }

  synchronized void recordOutcome(String licenceReference, PearsWorkProgrammeMigrationOutcome outcome) {
    var key = outcome.reason() == null
        ? outcome.result().name()
        : "%s: %s".formatted(outcome.result().name(), outcome.reason());
    outcomes.add(key, licenceReference);
    outcome.notes().noteCountsByReason().keySet().forEach(reason -> notes.add(reason, licenceReference));
  }

  synchronized void recordComparison(PearsWorkProgrammeComparison comparison) {
    var licenceReference = comparison.licenceReference();
    if (comparison.problem() != null) {
      notCompared.add(comparison.problem(), licenceReference);
      return;
    }

    var issueKinds = new HashSet<String>();
    var notMigratedReasons = new HashSet<String>();
    for (var row : comparison.rows()) {
      rowsByResult.merge(row.result(), 1L, Long::sum);
      if (row.result().isIssue() && issueKinds.add(row.result().getText())) {
        issues.add(row.result().getText(), licenceReference);
        issues.example(row.result().getText(), licenceReference,
            "activity %s: %s".formatted(row.pearsActivityId(), Objects.toString(row.detail(), "")));
      }
      if (row.result() == Result.NOT_MIGRATED) {
        var reason = Objects.requireNonNullElse(row.detail(), "Left out of the migration");
        if (notMigratedReasons.add(reason)) {
          notMigrated.add(reason, licenceReference);
        }
      }
    }
    comparison.scheduleComments().forEach(row -> {
      rowsByResult.merge(row.result(), 1L, Long::sum);
      if (row.result().isIssue() && issueKinds.add("Schedule comment " + row.result().getText())) {
        issues.add("Schedule comment " + row.result().getText(), licenceReference);
      }
    });
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
        .append("=== Licence work programmes migrated and compared with PEARS ===")
        .append(NEW_LINE)
        .append("  %,d licences swept%n".formatted(licencesSwept));

    if (!rowsByResult.isEmpty()) {
      out.append(NEW_LINE).append("Comparison rows, activities and schedule comments:").append(NEW_LINE);
      rowsByResult.forEach((result, count) ->
          out.append("  %-64s %,9d%s%n".formatted(result.getText(), count, result.isIssue() ? "  (issue)" : "")));
    }

    for (var tally : List.of(scheduleOutcomes, outcomes, notes, issues, notMigrated, notCompared, failures)) {
      tally.appendTo(out);
    }
    return out.toString();
  }
}
