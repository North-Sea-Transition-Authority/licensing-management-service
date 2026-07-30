package uk.co.nstauthority.licensingmanagementservice.licence.operation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class SetEquityOperationTest {

  @Test
  void type_isSetEquity() {
    var operation = new SetEquityOperation(123, BigDecimal.TEN);
    assertThat(operation.type()).isEqualTo(LicenceOperation.SET_EQUITY);
  }

  @Test
  void constructor_whenTransferToNull_throws() {
    assertThatThrownBy(() -> new SetEquityOperation(null, BigDecimal.TEN))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("transferTo");
  }

  @Test
  void constructor_whenEquityNull_throws() {
    assertThatThrownBy(() -> new SetEquityOperation(123, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("equity");
  }
}