package uk.co.nstauthority.licensingmanagementservice.migration.pears;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceService;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionService;
import uk.co.nstauthority.licensingmanagementservice.util.IntegrationTest;

/**
 * Sweeps every licence PEARS holds, rebuilds it in this application out of the PEARS operation history, and checks
 * the positions it arrives at are the positions PEARS holds, against both readings of PEARS -- see
 * {@link PearsComparisonSource}.
 *
 * <p>Excluded from the {@code test} task by its tag and run by {@code pearsMigrationTest} instead, since a difference
 * here is a discrepancy to investigate rather than a broken build. Reads PEARS over a live Oracle connection, so it
 * disables itself without credentials; set {@code PEARS_MIGRATION_LICENCES} to narrow the sweep.
 */
@Tag("pears-migration")
@IntegrationTest
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledIfEnvironmentVariable(
    named = "PEARS_DATASOURCE_URL",
    matches = ".+",
    disabledReason = "No PEARS datasource: set PEARS_DATASOURCE_URL, PEARS_DATASOURCE_USERNAME and PEARS_DATASOURCE_PASSWORD"
)
class PearsPositionMigrationValidationTest {

  private static final MigrationValidationReport REPORT = new MigrationValidationReport();

  @Autowired
  private PearsLicenceService pearsLicenceService;

  @Autowired
  private LicenceWritebackService licenceWritebackService;

  @Autowired
  private LicencePositionService licencePositionService;

  @Autowired
  private LicenceRepository licenceRepository;

  @Autowired
  private LicenceService licenceService;

  @ParameterizedTest(name = "{0}")
  @MethodSource("pearsLicences")
  void licenceTimeline_whenBuiltFromPears_thenMatchesTheTimelinePearsHolds(PearsLicenceReference pearsLicence) {
    var licenceType = supportedLicenceType(pearsLicence);
    var licence = licenceRepository.save(LicenceTestUtil.builder()
        .withId(licenceService.getNextLicenceId())
        .withLicenceType(licenceType)
        .withLicencePrefix(pearsLicence.licenceType())
        .withLicenceNumber(String.valueOf(pearsLicence.licenceNo()))
        .withLicenceReference(pearsLicence.licenceReference())
        .build());

    var differences = new ArrayList<String>();
    try {
      REPORT.recordLicenceSwept();

      var dataPointPositions = pearsLicenceService.dataPointPositions(pearsLicence.licenceType(), pearsLicence.licenceNo());
      var writeback = licenceWritebackService.overwriteLicencePositionsFromPears(licence);
      var builtPositions = licencePositionService.getExecutedChronologicalLicencePositions(licence);

      REPORT.recordIncludedOperations(pearsLicence.licenceReference(), writeback.includedOperationsByType());
      REPORT.recordIgnoredOperations(pearsLicence.licenceReference(), writeback.ignoredOperationsByType());

      differences.addAll(recordPositions(
          PearsComparisonSource.OPERATIONS, pearsLicence, writeback.history().toLicencePositions(), builtPositions)
      );
      differences.addAll(recordPositions(
          PearsComparisonSource.DATA_POINTS, pearsLicence, dataPointPositions, builtPositions
      ));

      writeback.notesByMigrator().forEach((migratorName, notes) ->
          REPORT.recordMigratorNotes(migratorName, pearsLicence.licenceReference(), notes));
    } catch (RuntimeException e) {
      // Recorded before rethrowing so the summary can group the distinct defects while this
      // licence still fails on its own.
      REPORT.recordFailure(pearsLicence.licenceReference(), e);
      throw e;
    }

    assertThat(ReportText.capped(differences))
        .as("%s should hold the positions PEARS holds, on the same dates, in the same order (%,d difference(s))",
            pearsLicence.licenceReference(), differences.size())
        .isEmpty();
  }

  /**
   * Records one position comparison and returns its differences, labelled with the reading of
   * PEARS they are against so a failure says which of the two disagreed.
   */
  private static List<String> recordPositions(
      PearsComparisonSource comparisonSource,
      PearsLicenceReference pearsLicence,
      PearsLicencePositions pearsPositions,
      List<LicencePosition> builtPositions
  ) {
    var differences = LivePositionComparison.compare(pearsPositions, builtPositions);
    REPORT.recordComparison(
        comparisonSource,
        pearsLicence.licenceReference(),
        pearsPositions,
        builtPositions.size(),
        LivePositionComparison.unmatched(pearsPositions, builtPositions),
        differences
    );

    return differences.stream()
        .map(difference -> "[%s] %s".formatted(comparisonSource.name(), difference))
        .toList();
  }

  @AfterAll
  void publishReport() {
    REPORT.publish();
  }

  private Stream<PearsLicenceReference> pearsLicences() {
    var licences = narrowed(pearsLicenceService.licenceReferences());

    Assumptions.assumeFalse(licences.isEmpty(), "PEARS holds no licences to compare");

    return licences.stream();
  }

  private static List<PearsLicenceReference> narrowed(List<PearsLicenceReference> licences) {
    var only = System.getenv("PEARS_MIGRATION_LICENCES");
    if (StringUtils.isBlank(only)) {
      return licences;
    }

    var wanted = Stream.of(only.split(","))
        .map(reference -> reference.strip().toUpperCase())
        .filter(StringUtils::isNotBlank)
        .toList();

    return licences.stream()
        .filter(licence -> wanted.contains(licence.licenceReference().toUpperCase()))
        .toList();
  }

  /**
   * The {@link LicenceType} the PEARS licence type maps to, aborting the licence where there is no mapping. The mapping
   * plays no other part in the comparison: the writeback reads PEARS back by {@code licence.getPrefix()}, which the
   * sweep sets to the PEARS licence type itself.
   */
  private static LicenceType supportedLicenceType(PearsLicenceReference pearsLicence) {
    try {
      return LicenceType.getFromPrefix(pearsLicence.licenceType());
    } catch (RuntimeException e) {
      var reason = "PEARS licence type %s has no LicenceType".formatted(pearsLicence.licenceType());
      REPORT.recordNotCompared(pearsLicence.licenceReference(), reason);
      return Assumptions.abort(reason);
    }
  }
}
