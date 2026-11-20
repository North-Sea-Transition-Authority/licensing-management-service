package uk.co.nstauthority.licensingmanagementservice.migration.pears;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PearsLicenceReferenceTest {

  @Test
  void parse_splitsTheLicenceTypeFromTheNumber() {
    assertThat(PearsLicenceReference.parse("PEDL236")).isEqualTo(new PearsLicenceReference("PEDL", 236));
    assertThat(PearsLicenceReference.parse("P2244").licenceReference()).isEqualTo("P2244");
  }

  @ParameterizedTest
  @ValueSource(strings = {"2244", "P", "P 2244", "p2244", "P2244A"})
  void parse_whenNotALicenceReference_throws(String licenceReference) {
    assertThatThrownBy(() -> PearsLicenceReference.parse(licenceReference))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("%s is not a PEARS licence reference".formatted(licenceReference));
  }
}
