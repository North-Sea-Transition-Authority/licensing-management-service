package uk.co.nstauthority.licensingmanagementservice.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.theClass;

import com.google.common.annotations.VisibleForTesting;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.properties.HasName;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import uk.co.fivium.digitalenummaterialisationlibrary.enummaterialisation.MaterialisableEnum;
import uk.co.nstauthority.licensingmanagementservice.util.ArchUnitUtils;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.Displayable;

@AnalyzeClasses(packages = "uk.co.nstauthority.licensingmanagementservice")
class TestArchUnitRules {

  @ArchTest
  public static final ArchRule visibleForTestingMethodsNotCalledFromProductionCode = methods().that()
      .areAnnotatedWith(VisibleForTesting.class)
      .should(new ArchCondition<>("not be called from any production class which does not own the method") {
        @Override
        public void check(JavaMethod method, ConditionEvents events) {
          method.getCallsOfSelf().stream()
              .filter(methodCall -> ArchUnitUtils.isProductionClass(methodCall.getOriginOwner()))
              .filter(methodCall -> !methodCall.getOriginOwner().equals(method.getOwner()))
              .forEach(methodCall -> events.add(new SimpleConditionEvent(methodCall, false,
                  "%s called from production code %s".formatted(method, methodCall.getSourceCodeLocation()))));
        }
      })
      .allowEmptyShould(true);

  @ArchTest
  public static final ArchRule enumMaterialisationArchRule = classes()
      .that().areEnums()
      .and().containAnyMethodsThat(
          HasName.Predicates.name("getDisplayName").or(HasName.Predicates.name("getDisplayOrder"))
      )
      .should().implement(MaterialisableEnum.class);

  @ArchTest
  public static final ArchRule displayableArchRule = theClass(Displayable.class)
      .should().beAssignableTo(MaterialisableEnum.class);
}
