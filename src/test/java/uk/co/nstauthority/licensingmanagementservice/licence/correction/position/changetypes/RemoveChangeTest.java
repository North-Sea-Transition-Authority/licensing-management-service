package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RemoveChangeTest {

  @Test
  void builder() {
    var removeChange = LicencePositionChangeType.removeChange()
        .withChangeId("123")
        .build();

    assertThat(removeChange.changeId()).isEqualTo("123");
    assertThat(removeChange.type()).isEqualTo(LicencePositionChangeType.REMOVE_CHANGE);
  }
}
