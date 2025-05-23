package uk.co.nstauthority.licensingmanagementservice.summary;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import uk.co.fivium.formlibrary.validator.date.DateUtils;
import uk.co.nstauthority.licensingmanagementservice.formatting.DecimalFormatUtils;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.Displayable;

/**
 * Formats the data presented on the summary based on the data type.
 */
public class SummaryUtil {
  private SummaryUtil() {
    throw new IllegalStateException("Utility class");
  }

  private static String format(Object value) {
    return switch (value) {
      case String stringValue -> stringValue;
      case Integer integerValue -> String.valueOf(integerValue);
      case Boolean booleanValue -> StringUtils.capitalize(BooleanUtils.toStringYesNo(booleanValue));
      case LocalDate localDateValue -> DateUtils.format(localDateValue);
      case BigDecimal bigDecimalValue -> DecimalFormatUtils.bigDecimalToFormattedString(bigDecimalValue);
      case Displayable displayableValue -> displayableValue.getDisplayName();
      default -> throw new IllegalArgumentException("Unexpected value class type: " + value.getClass().getName());
    };
  }

  public static List<String> formatAsList(Object value) {
    if (value == null) {
      return Collections.emptyList();
    }
    if (value instanceof Collection) {
      return ((Collection<?>) value).stream().map(SummaryUtil::format).toList();
    }
    return List.of(format(value));
  }

  public static String formatAsString(Object value) {
    if (value == null) {
      return null;
    }
    return format(value);
  }
}
