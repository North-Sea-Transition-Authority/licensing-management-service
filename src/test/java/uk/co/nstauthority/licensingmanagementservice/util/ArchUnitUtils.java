package uk.co.nstauthority.licensingmanagementservice.util;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaAnnotation;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.core.domain.JavaMethod;
import java.util.Optional;
import java.util.stream.Stream;
import org.springframework.web.bind.annotation.RequestMapping;

public class ArchUnitUtils {

  private ArchUnitUtils() {
  }

  public static boolean isProductionClass(JavaClass javaClass) {
    var location = getLocation(javaClass);
    return !location.contains("/java/test/");
  }

  public static boolean isProductionCodeUnit(JavaCodeUnit codeUnit) {
    return isProductionClass(codeUnit.getOwner());
  }

  public static DescribedPredicate<JavaMethod> areProductionCode() {
    return new DescribedPredicate<>("are production code") {
      @Override
      public boolean test(JavaMethod javaMethod) {
        return ArchUnitUtils.isProductionClass(javaMethod.getOwner());
      }
    };
  }

  private static String getLocation(JavaClass javaClass) {
    return javaClass.reflect().getProtectionDomain().getCodeSource().getLocation().toString();
  }

  public static DescribedPredicate<JavaClass> isCorrectionController() {
    return DescribedPredicate.describe(
        "map to a /licence-corrections/ path",
        javaClass -> Stream.concat(
                javaClass.getAnnotations().stream(),
                javaClass.getMethods().stream().flatMap(method -> method.getAnnotations().stream()))
            .filter(ArchUnitUtils::isRequestMapping)
            .flatMap(ArchUnitUtils::pathsOf)
            .anyMatch(path -> path.contains("/licence-corrections/")));
  }

  private static boolean isRequestMapping(JavaAnnotation<?> annotation) {
    var type = annotation.getRawType();
    return type.isEquivalentTo(RequestMapping.class) || type.isMetaAnnotatedWith(RequestMapping.class);
  }

  private static Stream<String> pathsOf(JavaAnnotation<?> annotation) {
    return Stream.of("value", "path")
        .map(annotation::get)
        .flatMap(Optional::stream)
        .flatMap(value -> value instanceof Object[] array ? Stream.of(array) : Stream.of(value))
        .map(String::valueOf);
  }
}
