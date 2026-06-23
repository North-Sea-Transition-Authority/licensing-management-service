package uk.co.nstauthority.licensingmanagementservice.licence.position.change.operations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class LicencePositionAdministratorChangeTest {

  @Test
  void builder() {
    var change = LicencePositionChangeOperation.newAdministratorChange()
        .withOperator(116)
        .build();

    assertThat(change.operatorId()).isEqualTo(116);
    assertThat(change.type()).isEqualTo(LicencePositionChangeOperation.LICENCE_ADMINISTRATOR);
  }

  @Test
  void nullOperatorId_assertThrows() {
    assertThatThrownBy(() -> new LicencePositionAdministratorChange(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("operatorId must not be null");
  }
}
