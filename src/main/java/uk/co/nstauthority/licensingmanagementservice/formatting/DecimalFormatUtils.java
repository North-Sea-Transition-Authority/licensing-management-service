package uk.co.nstauthority.licensingmanagementservice.formatting;

import java.math.BigDecimal;
import java.text.DecimalFormat;

public class DecimalFormatUtils {

  public static final DecimalFormat DEFAULT_DECIMAL_FORMAT = new DecimalFormat("#.##########");

  private DecimalFormatUtils() {
    throw new IllegalStateException("Utility class");
  }

  public static String doubleToFormattedString(Double value) {
    return value != null ? DEFAULT_DECIMAL_FORMAT.format(value) : "";
  }

  public static String bigDecimalToFormattedString(BigDecimal value) {
    return value != null ? DEFAULT_DECIMAL_FORMAT.format(value) : "";
  }
}
