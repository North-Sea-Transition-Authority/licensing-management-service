package uk.co.nstauthority.licensingmanagementservice.util;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.core.domain.JavaMethod;

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
}
