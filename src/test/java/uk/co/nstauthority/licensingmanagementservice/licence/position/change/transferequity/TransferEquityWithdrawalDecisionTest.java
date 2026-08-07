package uk.co.nstauthority.licensingmanagementservice.licence.position.change.transferequity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import org.junit.jupiter.api.Test;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.transferequity.TransferEquityWithdrawalDecision;

class TransferEquityWithdrawalDecisionTest {

  @Test
  void retainsBeneficialInterest_isTrueOnlyForRetain() {
    assertThat(TransferEquityWithdrawalDecision.RETAIN.retainsBeneficialInterest()).isTrue();
    assertThat(TransferEquityWithdrawalDecision.WITHDRAW.retainsBeneficialInterest()).isFalse();
  }

  @Test
  void getOptions_returnsBothOptionsInDisplayOrderKeyedByName() {
    assertThat(TransferEquityWithdrawalDecision.getOptions())
        .containsExactly(
            entry("WITHDRAW", "Yes, remove them from the position"),
            entry("RETAIN",
                "No, this organisation will retain a beneficial interest in the licence position despite holding no equity"));
  }
}