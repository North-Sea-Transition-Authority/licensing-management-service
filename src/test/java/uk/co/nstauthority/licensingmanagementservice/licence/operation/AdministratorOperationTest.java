package uk.co.nstauthority.licensingmanagementservice.licence.operation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class AdministratorOperationTest {

  @Test
  void builder() {
    var change = LicenceOperation.newAdministratorChange()
        .withOperator(116)
        .build();

    assertThat(change.id()).isEqualTo(1);
    assertThat(change.operatorId()).isEqualTo(116);
    assertThat(change.type()).isEqualTo(LicenceOperation.LICENCE_ADMINISTRATOR);
  }

  @Test
  void nullOperatorId_assertThrows() {
    assertThatThrownBy(() -> new AdministratorOperation(1, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("operatorId must not be null");
  }
}
