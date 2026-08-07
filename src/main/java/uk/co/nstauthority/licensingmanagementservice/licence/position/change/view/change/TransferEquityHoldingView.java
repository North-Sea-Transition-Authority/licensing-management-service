package uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change;

import java.math.BigDecimal;

public record TransferEquityHoldingView(
    String transferFromOrganisationName,
    String transferToOrganisationName,
    BigDecimal equity,
    Boolean retainBeneficialInterest
) {
}