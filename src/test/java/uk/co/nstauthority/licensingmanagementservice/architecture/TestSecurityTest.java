package uk.co.nstauthority.licensingmanagementservice.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;

@AnalyzeClasses(
    packages = "uk.co.nstauthority.licensingmanagementservice",
    importOptions = ImportOption.OnlyIncludeTests.class
)
class TestSecurityTest {

  @ArchTest
  final ArchTests securityRules = ArchTests.in(TestSecurityRules.class);
}
