package uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change;

import java.math.BigDecimal;

public record SetEquityRow(
    String organisationName,
    BigDecimal equity
) {
}