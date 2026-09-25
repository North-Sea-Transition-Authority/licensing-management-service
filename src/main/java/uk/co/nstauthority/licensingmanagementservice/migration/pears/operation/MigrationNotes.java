package uk.co.nstauthority.licensingmanagementservice.migration.pears.operation;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * What a migrator decided, and what it could not carry, while working a licence over. Write only,
 * and counted by reason rather than listed, with one example kept per reason so a count can be
 * looked up in the feed.
 */
public class MigrationNotes {

  private final Map<String, Integer> noteCounts = new LinkedHashMap<>();
  private final Map<String, Integer> unmappedCounts = new LinkedHashMap<>();
  private final Map<String, String> unmappedExamples = new LinkedHashMap<>();

  /**
   * A decision the migrator took, which is the migration working as intended.
   */
  public void note(String reason) {
    noteCounts.merge(reason, 1, Integer::sum);
  }

  /**
   * Something PEARS holds that the migrator could not carry across, with the operation it was on
   * so it can be looked up.
   */
  public void unmapped(String reason, long pearsOperationId) {
    unmappedCounts.merge(reason, 1, Integer::sum);
    unmappedExamples.putIfAbsent(reason, "PEARS operation %d".formatted(pearsOperationId));
  }

  public Map<String, Integer> noteCountsByReason() {
    return noteCounts;
  }

  public Map<String, Integer> unmappedCountsByReason() {
    return unmappedCounts;
  }

  public Map<String, String> unmappedExamplesByReason() {
    return unmappedExamples;
  }

  public int unmappedCount() {
    return unmappedCounts.values().stream().mapToInt(Integer::intValue).sum();
  }
}
