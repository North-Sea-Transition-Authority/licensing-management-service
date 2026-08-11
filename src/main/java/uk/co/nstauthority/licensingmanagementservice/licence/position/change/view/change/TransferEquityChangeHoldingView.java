package uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change;

import jakarta.annotation.Nullable;
import java.math.BigDecimal;

public record TransferEquityChangeHoldingView(
    String transferFromOrganisationName,
    BigDecimal transferFromStartingEquity,
    BigDecimal transferFromResultingEquity,
    String transferToOrganisationName,
    BigDecimal transferToStartingEquity,
    BigDecimal transferToResultingEquity,
    BigDecimal equity,
    @Nullable Boolean retainBeneficialInterest
) {
}