package uk.co.nstauthority.licensingmanagementservice.util.enumutil;

import java.util.Collection;
import java.util.stream.Stream;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

public class EnumValidationUtil {

  private EnumValidationUtil() {
    throw new IllegalStateException("This is a util class and should not be instantiated");
  }

  public static boolean isValidEnumValue(Class<? extends Enum<?>> enumClass, String enumOption) {
    return StringUtils.isNotBlank(enumOption) && Stream.of(enumClass.getEnumConstants())
        .map(Enum::name)
        .anyMatch(enumOption::equals);
  }

  public static boolean isNotValidEnumValue(Class<? extends Enum<?>> enumClass, String enumOption) {
    return !isValidEnumValue(enumClass, enumOption);
  }

  public static boolean containsInvalidEnumValue(Class<? extends Enum<?>> enumClass, Collection<String> enumOptions) {
    return CollectionUtils.isEmpty(enumOptions)
        || enumOptions.stream().anyMatch(enumOption -> isNotValidEnumValue(enumClass, enumOption));
  }
}
