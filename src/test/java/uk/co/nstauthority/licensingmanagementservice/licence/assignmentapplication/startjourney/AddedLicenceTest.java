package uk.co.nstauthority.licensingmanagementservice.licence.assignmentapplication.startjourney;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;

class AddedLicenceTest {

  @Test
  void from() {
    var licence = LicenceTestUtil.builder()
        .withId(123)
        .withLicenceReference("P123")
        .build();

    var result = AddedLicence.from(licence);

    assertThat(result).isEqualTo(new AddedLicence("123", "P123"));
    assertThat(result.isValid()).isTrue();
  }
}
