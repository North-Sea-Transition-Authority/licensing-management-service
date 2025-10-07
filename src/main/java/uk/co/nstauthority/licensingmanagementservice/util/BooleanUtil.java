package uk.co.nstauthority.licensingmanagementservice.util;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

public class BooleanUtil {

  private BooleanUtil() {
  }

  public static String yesNoFromBoolean(Boolean b) {
    return StringUtils.capitalize(BooleanUtils.toStringYesNo(b));
  }

  public static String yesNoFromBoolean(Boolean b, String defaultValue) {
    if (b == null) {
      return defaultValue;
    }
    return yesNoFromBoolean(b);
  }
}