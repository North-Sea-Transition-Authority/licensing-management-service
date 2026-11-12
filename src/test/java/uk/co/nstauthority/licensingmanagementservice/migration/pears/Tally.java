package uk.co.nstauthority.licensingmanagementservice.migration.pears;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.collections4.ListValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;

/**
 * One section of the migration validation summary: a count per key, the licences behind each key, and one worked
 * example. Licences are named only where there are few enough of them to be worth naming. Licences are listed, and
 * the example chosen, by {@link ReportText#SWEEP_ORDER} rather than the order they were added in, which a parallel
 * sweep does not fix.
 */
class Tally {

  /**
   * How many licences a key can hold before naming them stops being useful.
   */
  private static final int WORTH_LISTING = 25;

  private final String heading;
  private final ListValuedMap<String, String> licences = new ArrayListValuedHashMap<>();
  private final Map<String, Example> examples = new HashMap<>();

  Tally(String heading) {
    this.heading = heading;
  }

  void add(String key, String licenceReference) {
    licences.put(key, licenceReference);
  }

  /**
   * Offers an example for the key, kept if no licence earlier in the sweep has already given one. A licence offering
   * several keeps its first.
   */
  void example(String key, String licenceReference, String detail) {
    examples.merge(key, new Example(licenceReference, detail), (kept, offered) ->
        ReportText.SWEEP_ORDER.compare(offered.licenceReference(), kept.licenceReference()) < 0 ? offered : kept
    );
  }

  boolean isEmpty() {
    return licences.isEmpty();
  }

  /**
   * The section, biggest key first, or nothing at all when the tally is empty.
   */
  void appendTo(StringBuilder out) {
    if (isEmpty()) {
      return;
    }

    out.append(heading).append(System.lineSeparator());
    licences.keySet().stream()
        .sorted(Comparator
            .comparingInt((String key) -> licences.get(key).size()).reversed()
            .thenComparing(Comparator.naturalOrder()))
        .forEach(key -> {
          var affected = licences.get(key).stream().sorted(ReportText.SWEEP_ORDER).toList();
          out.append("  %-64s %,9d%n".formatted(key, affected.size()));

          if (affected.size() <= WORTH_LISTING) {
            out.append("      %s%n".formatted(ReportText.sample(affected)));
          }

          var example = examples.get(key);
          if (example != null) {
            out.append("      e.g. %s -- %s%n".formatted(example.licenceReference(), example.detail()));
          }
        });
  }

  private record Example(String licenceReference, String detail) {
  }
}
