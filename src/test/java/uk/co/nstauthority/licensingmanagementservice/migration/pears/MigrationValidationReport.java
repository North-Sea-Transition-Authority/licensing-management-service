package uk.co.nstauthority.licensingmanagementservice.migration.pears;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.operation.MigrationNotes;

/**
 * Accumulates what a sweep of every licence found, and renders it as the summary the CI step is read for. Each
 * {@link PearsComparisonSource} is reported separately, since a licence agreeing with the operations but not with
 * the data points means something quite different from the reverse.
 */
class MigrationValidationReport {

  private static final Path REPORT = Path.of("build", "reports", "pears-migration", "position-comparison.txt");

  private long licencesSwept;
  private final Map<PearsComparisonSource, SourceComparison> comparisons = new EnumMap<>(PearsComparisonSource.class);

  private final Map<String, MigratorNotes> migratorNotes = new LinkedHashMap<>();

  private final OperationCounts includedOperations = new OperationCounts(
      "Operations included, because a migration supports them", "included");
  private final OperationCounts ignoredOperations = new OperationCounts(
      "Operations ignored, because no migration supports them", "ignored");

  private final Tally notCompared = new Tally(
      System.lineSeparator() + "Licences not compared:");
  private final Tally failures = new Tally(
      System.lineSeparator() + "Licences that could not be compared at all:");

  MigrationValidationReport() {
    for (var comparisonSource : PearsComparisonSource.values()) {
      comparisons.put(comparisonSource, new SourceComparison(comparisonSource));
    }
  }

  /**
   * A licence reached the point of being compared, whatever the comparisons then found.
   */
  void recordLicenceSwept() {
    licencesSwept++;
  }

  /**
   * One licence's comparison against one reading of PEARS, whether or not anything differed.
   */
  void recordComparison(
      PearsComparisonSource comparisonSource,
      String licenceReference,
      PearsLicencePositions pearsPositions,
      int builtPositionCount,
      LivePositionComparison.UnmatchedPositions unmatched,
      List<PositionDifference> differences
  ) {
    comparisons.get(comparisonSource)
        .record(licenceReference, pearsPositions, builtPositionCount, unmatched, differences);
  }

  /**
   * What one migrator decided while carrying a licence across. Recorded per migrator rather than per comparison
   * source, because the question a reader has is what became of the administrator.
   */
  void recordMigratorNotes(String migratorName, String licenceReference, MigrationNotes notes) {
    migratorNotes
        .computeIfAbsent(migratorName, MigratorNotes::new)
        .record(licenceReference, notes);
  }

  /**
   * The operations of one licence that some migrator supports, by OPERATION_TYPE.
   */
  void recordIncludedOperations(String licenceReference, Map<String, Integer> includedOperationsByType) {
    includedOperations.record(licenceReference, includedOperationsByType);
  }

  /**
   * The operations of one licence that no migrator supports, by OPERATION_TYPE. Their positions
   * were still built; only what they did was left behind.
   */
  void recordIgnoredOperations(String licenceReference, Map<String, Integer> ignoredOperationsByType) {
    ignoredOperations.record(licenceReference, ignoredOperationsByType);
  }

  /**
   * A licence deliberately left out of the sweep, with the reason it was left out.
   */
  void recordNotCompared(String licenceReference, String reason) {
    notCompared.add(reason, licenceReference);
  }

  /**
   * A licence that threw before it could be compared.
   */
  void recordFailure(String licenceReference, Exception e) {
    failures.add("%s: %s".formatted(e.getClass().getSimpleName(), firstLine(e.getMessage())), licenceReference);
  }

  /**
   * The licences whose timeline did not match the given reading of PEARS.
   */
  List<String> differingLicenceReferences(PearsComparisonSource comparisonSource) {
    return comparisons.get(comparisonSource).differingLicenceReferences();
  }

  /**
   * Prints the summary to the step log and writes it alongside the other build reports, where the
   * existing {@code sync-reports} CI step publishes it.
   */
  void publish() {
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

  String render() {
    var out = new StringBuilder();
    out.append(System.lineSeparator())
        .append("=== Licence timeline compared with PEARS ===")
        .append(System.lineSeparator());
    out.append("  %,d licences swept%n".formatted(licencesSwept));

    comparisons.values().forEach(comparison -> comparison.appendTo(out));
    migratorNotes.values().forEach(notes -> notes.appendTo(out));

    includedOperations.appendTo(out);
    ignoredOperations.appendTo(out);
    notCompared.appendTo(out);
    failures.appendTo(out);

    return out.toString();
  }

  private static String firstLine(String message) {
    if (message == null) {
      return "(no message)";
    }
    var newline = message.indexOf('\n');
    return newline < 0 ? message : message.substring(0, newline);
  }

  /**
   * PEARS operations across the sweep, by OPERATION_TYPE, with how many licences held each type.
   */
  private static final class OperationCounts {

    private final String heading;
    private final String verb;
    private long operations;
    private final Set<String> licences = new HashSet<>();
    private final Map<String, Long> operationsByType = new TreeMap<>();
    private final Map<String, Long> licencesByOperationType = new TreeMap<>();

    private OperationCounts(String heading, String verb) {
      this.heading = heading;
      this.verb = verb;
    }

    private void record(String licenceReference, Map<String, Integer> licenceOperationsByType) {
      if (!licenceOperationsByType.isEmpty()) {
        licences.add(licenceReference);
      }
      licenceOperationsByType.forEach((operationType, count) -> {
        operations += count;
        operationsByType.merge(operationType, (long) count, Long::sum);
        licencesByOperationType.merge(operationType, 1L, Long::sum);
      });
    }

    private void appendTo(StringBuilder out) {
      out.append(System.lineSeparator())
          .append("--- %s ---%n".formatted(heading));
      out.append("  %,d operations %s on %,d licences%n".formatted(operations, verb, licences.size()));

      if (operationsByType.isEmpty()) {
        return;
      }

      out.append("  Operations %s by type:%n".formatted(verb));
      operationsByType.entrySet().stream()
          .sorted(Map.Entry.<String, Long>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
          .forEach(entry -> out.append("  %-48s %,9d operations on %,d licences%n".formatted(
              entry.getKey(), entry.getValue(), licencesByOperationType.get(entry.getKey()))));
    }
  }

  /**
   * What the sweep found against one reading of PEARS.
   */
  private static final class SourceComparison {

    private final PearsComparisonSource comparisonSource;
    private long licencesCompared;
    private long pearsPositions;
    private long positionsBuilt;
    private long positionsMissing;
    private long positionsAdditional;
    private final List<String> differingLicences = new ArrayList<>();

    private final Tally agreement;
    private final Tally differenceKinds;

    private SourceComparison(PearsComparisonSource comparisonSource) {
      this.comparisonSource = comparisonSource;
      this.agreement = new Tally("  Licences by how their timeline compares:");
      this.differenceKinds = new Tally("  Licences by what differs (a licence can differ in more than one way):");
    }

    private void record(
        String licenceReference,
        PearsLicencePositions positions,
        int builtPositionCount,
        LivePositionComparison.UnmatchedPositions unmatched,
        List<PositionDifference> differences
    ) {
      licencesCompared++;
      pearsPositions += positions.positions().size();
      positionsBuilt += builtPositionCount;
      positionsMissing += unmatched.missing();
      positionsAdditional += unmatched.additional();

      if (differences.isEmpty()) {
        agreement.add("timeline matches PEARS", licenceReference);
        return;
      }

      differingLicences.add(licenceReference);
      agreement.add("differs: " + coarsestDifference(differences), licenceReference);

      differences.stream()
          .map(PositionDifference::kind)
          .distinct()
          .forEach(kind -> {
            differenceKinds.add(kind.label(), licenceReference);
            differences.stream()
                .filter(difference -> difference.kind() == kind)
                .findFirst()
                .ifPresent(difference -> differenceKinds.example(
                    kind.label(), "%s -- %s".formatted(licenceReference, difference.detail())));
          });
    }

    private List<String> differingLicenceReferences() {
      return differingLicences.stream().sorted().toList();
    }

    private void appendTo(StringBuilder out) {
      out.append(System.lineSeparator())
          .append("--- Against %s ---%n".formatted(comparisonSource.label()));
      out.append("  %,d licences compared%n".formatted(licencesCompared));
      out.append("  %,d positions held by PEARS%n".formatted(pearsPositions));
      out.append("  %,d positions built by this application%n".formatted(positionsBuilt));
      out.append("  %,d positions PEARS holds that are missing from this application%n".formatted(positionsMissing));
      out.append("  %,d positions this application holds that PEARS does not%n".formatted(positionsAdditional));
      out.append("  %,d licences differ%n".formatted(differingLicences.size()));

      agreement.appendTo(out);
      differenceKinds.appendTo(out);

      if (licencesCompared > 0 && differingLicences.isEmpty()) {
        out.append("  Every licence compared holds the positions PEARS holds, "
            + "on the same dates, in the same order.%n".formatted());
      }
    }

    /**
     * The coarsest difference a licence has, so each licence lands in exactly one agreement bucket.
     * A licence holding the wrong positions makes any statement about their dates or order beside
     * the point.
     */
    private static String coarsestDifference(List<PositionDifference> differences) {
      return differences.stream()
          .map(PositionDifference::kind)
          .min(Comparator.comparingInt(Enum::ordinal))
          .map(PositionDifference.Kind::label)
          .orElseThrow();
    }
  }

  /**
   * What the migration decided while carrying one kind of operation across. A note is the migration deciding not to
   * carry something across, which is the migration working as intended; an unmapped reason is something PEARS holds
   * that it could not carry at all.
   */
  private static final class MigratorNotes {

    private final String migratorName;
    private long licencesMigrated;

    private final Tally decisions;
    private final Tally unmapped;

    private MigratorNotes(String migratorName) {
      this.migratorName = migratorName;
      this.decisions = new Tally("  Licences by what the migration decided on them:");
      this.unmapped = new Tally("  Licences by what PEARS holds that could not be carried across:");
    }

    private void record(String licenceReference, MigrationNotes notes) {
      licencesMigrated++;

      // Counted once per licence however often a reason came up on it, because a tally names the
      // licences behind a number and a licence named twice would say nothing.
      notes.noteCountsByReason().keySet().forEach(reason -> decisions.add(reason, licenceReference));
      notes.unmappedCountsByReason().keySet().forEach(reason -> {
        unmapped.add(reason, licenceReference);
        var example = notes.unmappedExamplesByReason().get(reason);
        if (example != null) {
          unmapped.example(reason, "%s -- %s".formatted(licenceReference, example));
        }
      });
    }

    private void appendTo(StringBuilder out) {
      out.append(System.lineSeparator())
          .append("--- The %s, carried across ---%n".formatted(migratorName));
      out.append("  %,d licences migrated%n".formatted(licencesMigrated));

      decisions.appendTo(out);
      unmapped.appendTo(out);
    }
  }
}
