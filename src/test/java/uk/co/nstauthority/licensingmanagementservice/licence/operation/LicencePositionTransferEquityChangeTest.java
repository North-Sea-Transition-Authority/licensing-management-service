package uk.co.nstauthority.licensingmanagementservice.licence.operation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class LicencePositionTransferEquityChangeTest {

  @Test
  void type_isTransferEquity() {
    var operation = new TransferEquityOperation(1, 2, BigDecimal.TEN, null);
    assertThat(operation.type()).isEqualTo(LicenceOperation.TRANSFER_EQUITY);
  }

  @Test
  void constructor_whenTransferFromNull_throws() {
    assertThatThrownBy(() -> new TransferEquityOperation(null, 2, BigDecimal.TEN, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("transferFrom");
  }

  @Test
  void constructor_whenTransferToNull_throws() {
    assertThatThrownBy(() -> new TransferEquityOperation(1, null, BigDecimal.TEN, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("transferTo");
  }

  @Test
  void constructor_whenEquityNull_throws() {
    assertThatThrownBy(() -> new TransferEquityOperation(1, 2, null, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("equity");
  }

  @Test
  void constructor_allowsNullRetainBeneficialInterest() {
    var operation = new TransferEquityOperation(1, 2, BigDecimal.TEN, null);
    assertThat(operation.retainBeneficialInterest()).isNull();
  }

  @Test
  void builder_setsAllFields() {
    var operation = LicenceOperation.newTransferEquityOperation()
        .withTransferFrom(1)
        .withTransferTo(2)
        .withEquity(BigDecimal.valueOf(40))
        .withRetainBeneficialInterest(true)
        .build();

    assertThat(operation.transferFrom()).isEqualTo(1);
    assertThat(operation.transferTo()).isEqualTo(2);
    assertThat(operation.equity()).isEqualByComparingTo(BigDecimal.valueOf(40));
    assertThat(operation.retainBeneficialInterest()).isTrue();
  }
}