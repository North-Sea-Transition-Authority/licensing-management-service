package uk.co.nstauthority.licensingmanagementservice.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.theClass;

import com.google.common.annotations.VisibleForTesting;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.properties.HasName;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.util.Set;
import org.springframework.stereotype.Controller;
import uk.co.fivium.digitalenummaterialisationlibrary.enummaterialisation.MaterialisableEnum;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.CorrectionHasStatus;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.InvokingUserCanViewCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.reviewandapply.CorrectionSummaryController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.reviewandapply.ReviewCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.search.LicenceCorrectionSearchController;
import uk.co.nstauthority.licensingmanagementservice.util.ArchUnitUtils;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.Displayable;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.IgnoreMaterialisationRule;

@AnalyzeClasses(packages = "uk.co.nstauthority.licensingmanagementservice")
class TestArchUnitRules {

  private static final Set<Class<?>> READ_ONLY_CORRECTION_CONTROLLERS = Set.of(
      LicenceCorrectionSearchController.class,
      CorrectionSummaryController.class,
      ReviewCorrectionController.class
  );

  private static final DescribedPredicate<JavaClass> isExemptReadOnlyController = DescribedPredicate.describe(
      "a read-only correction controller",
      javaClass -> READ_ONLY_CORRECTION_CONTROLLERS.stream().anyMatch(javaClass::isEquivalentTo)
  );

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
      .and().areNotAnnotatedWith(IgnoreMaterialisationRule.class)
      .and().containAnyMethodsThat(
          HasName.Predicates.name("getDisplayName").or(HasName.Predicates.name("getDisplayOrder"))
      )
      .should().implement(MaterialisableEnum.class);

  @ArchTest
  public static final ArchRule displayableArchRule = theClass(Displayable.class)
      .should().beAssignableTo(MaterialisableEnum.class);

  @ArchTest
  final ArchTests duplicationEntityRules = ArchTests.in(DuplicationArchRule.class);

  @ArchTest
  public static final ArchRule correctionControllerHasSecurityRules = classes()
      .that().areAnnotatedWith(Controller.class)
      .and(ArchUnitUtils.isCorrectionController())
      .and(DescribedPredicate.not(isExemptReadOnlyController))
      .should().beAnnotatedWith(InvokingUserCanViewCorrection.class)
      .andShould(new ArchCondition<>("be annotated with @CorrectionHasStatus(IN_PROGRESS)") {
        @Override
        public void check(JavaClass javaClass, ConditionEvents events) {
          var hasInProgressStatus = javaClass.tryGetAnnotationOfType(CorrectionHasStatus.class)
              .map(correctionHasStatus ->
                  Set.of(correctionHasStatus.value()).equals(Set.of(LicenceCorrectionStatus.IN_PROGRESS)))
              .orElse(false);
          if (!hasInProgressStatus) {
            events.add(SimpleConditionEvent.violated(javaClass,
                "%s must be annotated with @CorrectionHasStatus(IN_PROGRESS) or is an exempt controller (%s)"
                    .formatted(javaClass.getName(), javaClass.getSourceCodeLocation())));
          }
        }
      });

  @ArchTest
  public static final ArchRule correctionHandlersDoNotOverrideStatusCheck = methods()
      .that().areDeclaredInClassesThat(ArchUnitUtils.isCorrectionController())
      .and().areDeclaredInClassesThat(DescribedPredicate.not(isExemptReadOnlyController))
      .should().notBeAnnotatedWith(CorrectionHasStatus.class);
}
