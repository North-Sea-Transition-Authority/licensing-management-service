package uk.co.nstauthority.licensingmanagementservice.duplication;

import java.lang.reflect.Field;
import java.util.Arrays;
import org.springframework.beans.BeanUtils;
import uk.co.nstauthority.licensingmanagementservice.util.ReflectionUtil;

public class DuplicationUtil {

  private DuplicationUtil() {
    throw new IllegalStateException("Utility class should not be instantiated");
  }

  public static <T> T instantiateBlankInstance(Class<T> instanciationClass) {
    return BeanUtils.instantiateClass(instanciationClass);
  }

  public static <T> void copyProperties(T source, T target, String... fieldsToIgnore) {
    ReflectionUtil.getAllFields(source.getClass())
        .stream()
        .filter(field ->
            Arrays.stream(fieldsToIgnore).noneMatch(fieldToIgnore -> fieldToIgnore.equals(field.getName())))
        .forEach(field -> {
          try {
            copyField(field, source, target);
          } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
          }
        });
  }

  private static <T> void copyField(Field field, T source, T target) throws IllegalAccessException {
    var sourcePreviouslyAccessible = field.canAccess(source);
    field.setAccessible(true);
    var originalValue = field.get(source);
    field.set(target, originalValue);
    field.setAccessible(sourcePreviouslyAccessible);
  }
}
