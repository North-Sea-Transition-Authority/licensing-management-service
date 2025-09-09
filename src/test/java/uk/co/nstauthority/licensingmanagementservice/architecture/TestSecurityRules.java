package uk.co.nstauthority.licensingmanagementservice.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import uk.co.nstauthority.licensingmanagementservice.util.AnnotationSecurityTest;
import uk.co.nstauthority.licensingmanagementservice.util.AuthorisationSecurityTest;
import uk.co.nstauthority.licensingmanagementservice.util.ParameterizedAuthorisationSecurityTest;
import uk.co.nstauthority.licensingmanagementservice.util.SecurityTest;

public class TestSecurityRules {

  ArchCondition<JavaClass> containAtLeastOneSecurityTest =
      new ArchCondition<>("contain at least one @SecurityTest") {
        @Override
        public void check(JavaClass javaClass, ConditionEvents conditionEvents) {

          var securityTest = javaClass.getAllMethods()
              .stream()
              .filter(javaMethodCall -> javaMethodCall.isAnnotatedWith(SecurityTest.class)
                  || javaMethodCall.isAnnotatedWith(ParameterizedAuthorisationSecurityTest.class)
                  || javaMethodCall.isAnnotatedWith(AuthorisationSecurityTest.class)
                  || javaMethodCall.isAnnotatedWith(AnnotationSecurityTest.class))
              .findAny();

          if (securityTest.isEmpty()) {
            conditionEvents.add(
                SimpleConditionEvent.violated(javaClass,
                    String.format("%s doesn't contain a @SecurityTest or @ParameterizedSecurityTest or @AnnotationSecurityTest or @AuthorisationSecurityTest",
                        javaClass.getSimpleName())));
          }
        }
      };

  @ArchTest
  final ArchRule securityTestAnnotationRule = classes()
      .that().haveSimpleNameEndingWith("ControllerTest")
      .and().doNotHaveSimpleName("AbstractControllerTest")
      .should(containAtLeastOneSecurityTest);
}
