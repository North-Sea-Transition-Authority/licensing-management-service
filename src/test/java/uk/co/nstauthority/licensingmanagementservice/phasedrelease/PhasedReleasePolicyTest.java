package uk.co.nstauthority.licensingmanagementservice.phasedrelease;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Controller;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.search.LicenceSearchController;
import uk.co.nstauthority.licensingmanagementservice.workarea.WorkAreaController;

class PhasedReleasePolicyTest {

  // Main-source classes only (excludes test-fixture controllers); static analysis so @Profile controllers are included.
  private static final JavaClasses PRODUCTION_CLASSES = new ClassFileImporter()
      .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
      .importPackages("uk.co.nstauthority.licensingmanagementservice");

  private static List<Class<?>> productionControllers() {
    return PRODUCTION_CLASSES.stream()
        .filter(javaClass -> javaClass.isAnnotatedWith(Controller.class)
            || javaClass.isMetaAnnotatedWith(Controller.class))
        .<Class<?>>map(JavaClass::reflect)
        .toList();
  }

  @Test
  void phaseFor_classifiesRepresentativeControllers() {
    assertThat(PhasedReleasePolicy.phaseFor(WorkAreaController.class)).contains(ReleasePhase.NOT_FLAGGED);
    assertThat(PhasedReleasePolicy.phaseFor(LicenceSearchController.class)).contains(ReleasePhase.LMS1);
    assertThat(PhasedReleasePolicy.phaseFor(LicenceCorrectionController.class)).contains(ReleasePhase.LMS2);
  }

  @Test
  void phaseFor_unclassifiedPackage_isEmpty() {
    assertThat(PhasedReleasePolicy.phaseFor(FeatureFlagService.class)).isEmpty();
  }

  @Test
  void everyControllerIsClassified() {
    var unclassified = productionControllers().stream()
        .filter(controllerClass -> PhasedReleasePolicy.phaseFor(controllerClass).isEmpty())
        .map(Class::getName)
        .toList();

    assertThat(unclassified)
        .withFailMessage("Controllers not classified by PhasedReleasePolicy (would 404 under the phased-release "
            + "interceptor): %s", unclassified)
        .isEmpty();
  }

  @Test
  void allReleasePhasesAreReachableFromTheAllowList() {
    // Guards against a phase that no controller maps to (a mapping typo would otherwise pass silently)
    var mappedPhases = productionControllers().stream()
        .flatMap(controllerClass -> PhasedReleasePolicy.phaseFor(controllerClass).stream())
        .distinct()
        .toList();

    assertThat(mappedPhases).containsAll(List.of(ReleasePhase.NOT_FLAGGED, ReleasePhase.LMS1, ReleasePhase.LMS2));
  }
}
