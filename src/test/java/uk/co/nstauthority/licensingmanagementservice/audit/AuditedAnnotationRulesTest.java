package uk.co.nstauthority.licensingmanagementservice.audit;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.equivalentTo;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.are;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import jakarta.persistence.Entity;
import org.hibernate.envers.Audited;

@AnalyzeClasses(
    packages = "uk.co.nstauthority.licensingmanagementservice",
    importOptions = ImportOption.DoNotIncludeTests.class
)
class AuditedAnnotationRulesTest {

  @ArchTest
  static final ArchRule allEntitiesShouldBeAudited = classes()
      .that(are(not(equivalentTo(AuditRevision.class))))
      .and().areAnnotatedWith(Entity.class)
      .should().beAnnotatedWith(Audited.class)
      .because("Entities should all be audited") ;
}
