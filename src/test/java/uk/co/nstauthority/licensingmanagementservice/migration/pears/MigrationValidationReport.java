package uk.co.nstauthority.licensingmanagementservice.migration.pears;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Accumulates what a sweep of every licence found, and renders it as the summary the CI step is
 * read for.
 *
 * <p>The step's own failure is ignored, so this summary is the only durable account of why licences
 * did not match: it has to say how many licences were compared, how many agreed, and for the rest,
 * what kind of thing differed and on which licences. It reports each {@link PearsOracle}
 * separately, since a licence agreeing with the operations but not with the data points means
 * something quite different from the reverse.
 *
 * <p>The sweep this accumulates runs one licence at a time -- {@code pearsMigrationTest} takes a
 * single fork, and nothing turns JUnit's parallel execution on -- so nothing here is written for
 * more than one thread. Sweeping in parallel would need more than making these fields thread safe:
 * forks are separate JVMs, so each would render a report of the licences it happened to take.
 */
class MigrationValidationReport {

  private static final Path REPORT = Path.of("build", "reports", "pears-migration", "position-comparison.txt");

  private long licencesSwept;
  private final Map<PearsOracle, OracleComparison> comparisons = new EnumMap<>(PearsOracle.class);

  private final Tally notCompared = new Tally(
      System.lineSeparator() + "Licences not compared:");
  private final Tally failures = new Tally(
      System.lineSeparator() + "Licences that could not be compared at all:");

  MigrationValidationReport() {
    for (var oracle : PearsOracle.values()) {
      comparisons.put(oracle, new OracleComparison(oracle));
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
      PearsOracle oracle,
      String licenceReference,
      PearsLicencePositions pearsPositions,
      int builtPositionCount,
      List<PositionDifference> differences
  ) {
    comparisons.get(oracle).record(licenceReference, pearsPositions, builtPositionCount, differences);
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
  List<String> differingLicenceReferences(PearsOracle oracle) {
    return comparisons.get(oracle).differingLicenceReferences();
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
   * What the sweep found against one reading of PEARS.
   */
  private static final class OracleComparison {

    private final PearsOracle oracle;
    private long licencesCompared;
    private long pearsPositions;
    private long positionsBuilt;
    private final List<String> differingLicences = new ArrayList<>();

    private final Tally agreement;
    private final Tally differenceKinds;

    private OracleComparison(PearsOracle oracle) {
      this.oracle = oracle;
      this.agreement = new Tally("  Licences by how their timeline compares:");
      this.differenceKinds = new Tally("  Licences by what differs (a licence can differ in more than one way):");
    }

    private void record(
        String licenceReference,
        PearsLicencePositions positions,
        int builtPositionCount,
        List<PositionDifference> differences
    ) {
      licencesCompared++;
      pearsPositions += positions.positions().size();
      positionsBuilt += builtPositionCount;

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
          .append("--- Against %s ---%n".formatted(oracle.label()));
      out.append("  %,d licences compared%n".formatted(licencesCompared));
      out.append("  %,d positions held by PEARS%n".formatted(pearsPositions));
      out.append("  %,d positions built by this application%n".formatted(positionsBuilt));
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
}
