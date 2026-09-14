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
 * Sweeps every licence PEARS holds, rebuilds its timeline in this application out of the PEARS live
 * positions, and checks the result is the timeline PEARS holds.
 *
 * <p>Excluded from the {@code test} task by its tag and run by the {@code pearsMigrationTest} task
 * instead, because a difference here is a discrepancy to investigate -- a bug in this service, a
 * bug in PEARS, or a legitimate edge case for stakeholders -- rather than a broken build. Reads
 * PEARS over a live Oracle connection, so it disables itself where there are no credentials.
 *
 * <p>Each licence is compared against both readings of PEARS -- see {@link PearsOracle}. The
 * migration builds a licence from the operations, so comparing the result against them again
 * checks the replay rather than the reading; the data points are what PEARS holds in its own
 * right, and are the only one of the two that can catch the reading itself being wrong.
 *
 * <p>Set {@code PEARS_MIGRATION_LICENCES} to a comma separated list of licence references to narrow
 * the sweep, which is worth doing when working on the comparison itself.
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

      var operationPositions = pearsLicenceService.getLicencePositions(
          pearsLicence.licenceType(), pearsLicence.licenceNo());
      var dataPointPositions = pearsLicenceService.dataPointPositions(
          pearsLicence.licenceType(), pearsLicence.licenceNo());

      licenceWritebackService.overwriteLicencePositionsFromPears(licence);
      var builtPositions = licencePositionService.getExecutedChronologicalLicencePositions(licence);

      differences.addAll(record(PearsOracle.OPERATIONS, pearsLicence, operationPositions, builtPositions));
      differences.addAll(record(PearsOracle.DATA_POINTS, pearsLicence, dataPointPositions, builtPositions));
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
   * Records one comparison and returns its differences, labelled with the reading of PEARS they are
   * against so a failure says which of the two disagreed.
   */
  private static List<String> record(
      PearsOracle oracle,
      PearsLicenceReference pearsLicence,
      PearsLicencePositions pearsPositions,
      List<LicencePosition> builtPositions
  ) {
    var differences = LivePositionComparison.compare(pearsPositions, builtPositions);
    REPORT.recordComparison(
        oracle, pearsLicence.licenceReference(), pearsPositions, builtPositions.size(), differences);

    return differences.stream()
        .map(difference -> "[%s] %s".formatted(oracle.name(), difference))
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
   * The {@link LicenceType} the PEARS licence type maps to, aborting the licence where there is no
   * mapping.
   *
   * <p>A licence cannot be saved without a type, so a PEARS licence type this application does not
   * know is reported as not compared rather than swept. The mapping plays no other part in the
   * comparison: the writeback reads PEARS back by {@code licence.getPrefix()}, which the sweep sets
   * to the PEARS licence type itself, so the several types sharing one {@code LicenceType} --
   * {@code EXL}, {@code ML}, {@code PEDL} and {@code PL} are all {@code LANDWARD_PRODUCTION} -- are
   * each still read back as themselves.
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
