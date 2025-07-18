package uk.co.nstauthority.licensingmanagementservice.util;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.EnumValidationUtil;

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

  public static boolean filterIdList(List<Integer> idListFromDataItem, Integer idInputOnFilter) {
    if (idInputOnFilter ==  null) {
      return true;
    }

    if (idListFromDataItem == null || idListFromDataItem.isEmpty()) {
      return false;
    }

    return idListFromDataItem.contains(idInputOnFilter);
  }

  public static <T extends Enum<T>> boolean filterEnum(Class<T> enumClass,
                                                       T typeFromDataItem,
                                                       Collection<String> typesFromFilter) {
    if (EnumValidationUtil.containsInvalidEnumValue(enumClass, typesFromFilter)) {
      return true;
    }

    var typesSet = typesFromFilter.stream()
        .filter(Objects::nonNull)
        .filter(type -> EnumValidationUtil.isValidEnumValue(enumClass, type))
        .map(s -> Enum.valueOf(enumClass, s))
        .collect(Collectors.toSet());

    return typesSet.contains(typeFromDataItem);
  }
}