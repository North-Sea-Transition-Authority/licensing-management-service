package uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent;

import java.math.BigDecimal;

public final class RelinquishmentPercentageFormat {

  private RelinquishmentPercentageFormat() {
  }

  public static String format(BigDecimal relinquishmentPercentage) {
    return relinquishmentPercentage.stripTrailingZeros().toPlainString();
  }
}
