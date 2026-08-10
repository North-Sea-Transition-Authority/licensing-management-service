package uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state;

import java.math.BigDecimal;

public record BeneficialInterestView(
    String organisationName,
    BigDecimal equity
) {
}