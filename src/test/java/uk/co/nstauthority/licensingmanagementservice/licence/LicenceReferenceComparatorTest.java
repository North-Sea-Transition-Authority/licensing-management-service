package uk.co.nstauthority.licensingmanagementservice.licence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class LicenceReferenceComparatorTest {

  private final LicenceReferenceComparator comparator = new LicenceReferenceComparator();

  @Test
  void sortsLicenceReferencesInNaturalOrder() {
    var input = List.of("C10", "C2", "C1", "P10", "P5", "P1");

    var sorted = input.stream().sorted(comparator).toList();

    assertThat(sorted).containsExactly("C1", "C2", "C10", "P1", "P5", "P10");
  }

  @Test
  void sortsIdenticalReferencesAsEqual() {
    assertThat(comparator.compare("C1", "C1")).isZero();
  }

  @Test
  void sortsAlphabeticPrefixFirst() {
    assertThat(comparator.compare("A1", "B1")).isNegative();
  }

  @Test
  void sortsNumericPartNumerically() {
    assertThat(comparator.compare("C2", "C10")).isNegative();
  }

  @Test
  void handlesCaseInsensitivePrefix() {
    assertThat(comparator.compare("cs1", "CS1")).isZero();
  }

  @Test
  void fallsBackToStringComparisonWhenNotMatchingPattern() {
    assertThat(comparator.compare("unknown", "zzz")).isNegative();
  }
}
