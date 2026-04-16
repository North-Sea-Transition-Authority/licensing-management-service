package uk.co.nstauthority.licensingmanagementservice.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.util.Set;
import org.springframework.stereotype.Repository;
import uk.co.nstauthority.licensingmanagementservice.duplication.DuplicationSource;
import uk.co.nstauthority.licensingmanagementservice.duplication.NotDuplicationSource;

public class DuplicationArchRule {

  ArchCondition<JavaClass> implementDuplicationSource =
      new ArchCondition<>("extend %s".formatted(DuplicationSource.class.getSimpleName())) {
        @Override
        public void check(JavaClass javaClass, ConditionEvents conditionEvents) {
          boolean doesNotImplementDuplicationSource = javaClass.getInterfaces().stream()
              .noneMatch(javaType -> javaType.toErasure().isAssignableFrom(DuplicationSource.class.getName()));

          boolean isPublic = javaClass.getModifiers().equals(Set.of(JavaModifier.PUBLIC));

            if (doesNotImplementDuplicationSource && !isPublic) {
              conditionEvents.add(
                  SimpleConditionEvent.violated(javaClass,
                      "%s should extend %s and should have access modifier %s"
                          .formatted(javaClass.getSimpleName(), DuplicationSource.class.getSimpleName(), JavaModifier.PUBLIC.name())));
            }
        }
      };

  ArchCondition<JavaClass> implementNotDuplicationSource =
      new ArchCondition<>("extend %s".formatted(NotDuplicationSource.class.getSimpleName())) {
        @Override
        public void check(JavaClass javaClass, ConditionEvents conditionEvents) {
          boolean doesNotImplementNotDuplicationSource = javaClass.getInterfaces().stream()
              .noneMatch(javaType -> javaType.getName().equals(NotDuplicationSource.class.getName()));

          if (doesNotImplementNotDuplicationSource) {
            conditionEvents.add(
                SimpleConditionEvent.violated(javaClass,
                    "%s should extend %s".formatted(javaClass.getSimpleName(), NotDuplicationSource.class.getSimpleName())));
          }
        }
      };

  @ArchTest
  public final ArchRule repositoriesImplementDuplicationInterfacesRule = classes()
      .that().areAnnotatedWith(Repository.class)
      .should(implementDuplicationSource)
      .orShould(implementNotDuplicationSource);
}
