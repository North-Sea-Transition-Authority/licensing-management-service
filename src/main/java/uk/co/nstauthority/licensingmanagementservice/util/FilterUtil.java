package uk.co.nstauthority.licensingmanagementservice.util;

import org.apache.commons.lang3.StringUtils;

public class FilterUtil {
  private FilterUtil() {
    throw new IllegalUtilClassInstantiationException(this.getClass());
  }

  public static boolean filterTextInput(String textFromDataItem, String textInputOnFilter) {
    if (StringUtils.isBlank(textInputOnFilter)) {
      return true;
    }

    return textFromDataItem.toLowerCase().contains(textInputOnFilter.toLowerCase());
  }
}