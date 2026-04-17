package uk.co.fivium.gisframework.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;


@AnalyzeClasses(packages = "uk.co.fivium.gisframework")
class TestArchUnitRules {

  private static final String GIS_FRAMEWORK_PREFIX = "gis_framework_";

  @ArchTest
  static final ArchRule entitiesMustHaveTableAnnotation =
      classes()
          .that().areAnnotatedWith(Entity.class)
          .should().beAnnotatedWith(Table.class);

  @ArchTest
  static final ArchRule entityTableNamesMustStartWithGisFrameworkPrefix =
      classes()
          .that().areAnnotatedWith(Entity.class)
          .should(haveTableNameStartingWith(GIS_FRAMEWORK_PREFIX));

  private static ArchCondition<JavaClass> haveTableNameStartingWith(String prefix) {
    return new ArchCondition<>("have @Table(name) starting with '%s'".formatted(prefix)) {
      @Override
      public void check(JavaClass javaClass, ConditionEvents events) {
        String tableName = javaClass.getAnnotationOfType(Table.class).name();
        if (tableName == null || !tableName.startsWith(prefix)) {
          events.add(SimpleConditionEvent.violated(
              javaClass, "%s has @Table(name = \"%s\") which must start with \"%s\""
                  .formatted(javaClass.getName(), tableName, prefix)));
        }
      }
    };
  }
}
