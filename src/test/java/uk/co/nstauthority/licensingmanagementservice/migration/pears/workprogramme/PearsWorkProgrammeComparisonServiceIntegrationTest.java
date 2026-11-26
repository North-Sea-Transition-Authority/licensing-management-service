package uk.co.nstauthority.licensingmanagementservice.migration.pears.workprogramme;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.co.nstauthority.licensingmanagementservice.migration.pears.workprogramme.PearsWorkProgrammeWriterIntegrationTest.P9244;
import static uk.co.nstauthority.licensingmanagementservice.migration.pears.workprogramme.PearsWorkProgrammeWriterIntegrationTest.activity;
import static uk.co.nstauthority.licensingmanagementservice.migration.pears.workprogramme.PearsWorkProgrammeWriterIntegrationTest.note;
import static uk.co.nstauthority.licensingmanagementservice.migration.pears.workprogramme.PearsWorkProgrammeWriterIntegrationTest.programme;

import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.transaction.annotation.Transactional;
import uk.co.fivium.energyportalapi.client.organisation.OrganisationApi;
import uk.co.fivium.energyportalapi.client.user.UserApi;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.authentication.UserDetailService;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceService;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceSchedule;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.NewScheduleVersionFixture;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.schedulecomments.ScheduleComment;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivity;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.PearsLicenceComparisons;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.PearsLicenceReference;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.MigratedScheduleFixture;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.workprogramme.PearsWorkProgrammeComparison.CommentRow;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.workprogramme.PearsWorkProgrammeComparison.Result;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.workprogramme.PearsWorkProgrammeComparison.Row;
import uk.co.nstauthority.licensingmanagementservice.util.IntegrationTest;

/**
 * Compares a work programme migrated onto a schedule written by the schedule migration's own writer, with PEARS read
 * through a mock returning the programme migrated.
 */
@Sql(
    scripts = {
        "classpath:migration/create-pears-schedule-migration-tables.sql",
        "classpath:migration/create-pears-work-programme-migration-tables.sql"
    },
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS,
    config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED)
)
@Import({MigratedScheduleFixture.class, NewScheduleVersionFixture.class})
@Transactional
@IntegrationTest
class PearsWorkProgrammeComparisonServiceIntegrationTest {

  private static final PearsWorkProgramme PROGRAMME = programme(
      List.of(
          activity("1", "DRILL_OR_DROP_WELL", "Drill or drop well to 3,000m", "DROPPED"),
          activity("2", "OTHER", "Geological studies", "NOT_STARTED"),
          activity("3", "OTHER", "None", "NOT_STARTED")
      ),
      List.of(note(null, null, LocalDateTime.of(2017, Month.JANUARY, 5, 14, 0), "John Smith", "General note"))
  );

  @Autowired
  private PearsLicenceComparisons licenceComparisons;

  @Autowired
  private EntityManager entityManager;

  @Autowired
  private MigratedScheduleFixture migratedScheduleFixture;

  @Autowired
  private MigratedScheduleReader migratedScheduleReader;

  @Autowired
  private LmsWorkProgrammeReader lmsWorkProgrammeReader;

  @Autowired
  private PearsWorkProgrammeWriter pearsWorkProgrammeWriter;

  @Autowired
  private PearsWorkProgrammeMigratedLicenceRepository migratedLicenceRepository;

  @Autowired
  private LicenceService licenceService;

  @MockitoBean
  private OrganisationApi organisationApi;

  @MockitoBean
  private UserApi userApi;

  @MockitoBean
  private UserDetailService userDetailService;

  @Autowired
  private NewScheduleVersionFixture newScheduleVersionFixture;

  private PearsWorkProgrammeService pearsWorkProgrammeService;

  private PearsWorkProgrammeComparisonService comparisonService;

  private Licence licence;

  private LicenceSchedule licenceSchedule;

  @BeforeEach
  void setUp() {
    pearsWorkProgrammeService = mock(PearsWorkProgrammeService.class);
    when(pearsWorkProgrammeService.licenceWorkProgrammes(P9244)).thenReturn(List.of(PROGRAMME));
    comparisonService = new PearsWorkProgrammeComparisonService(pearsWorkProgrammeService, licenceService,
        migratedScheduleReader, lmsWorkProgrammeReader, migratedLicenceRepository, entityManager, licenceComparisons);

    licence = LicenceTestUtil.builder()
        .withId(9244)
        .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
        .withLicenceNumber("9244")
        .withLicenceReference("P9244")
        .build();
    entityManager.persist(licence);
    licenceSchedule = migratedScheduleFixture.migrate(licence);
    entityManager.flush();
  }

  @Test
  void compare_whenMigrated_matchesEveryActivityAndScheduleComment() {
    migrate();

    var comparison = comparisonService.compare(List.of(P9244)).getFirst();

    assertThat(comparison.issueDescriptions()).isEmpty();
    assertThat(comparison.rows())
        .extracting(Row::pearsActivityId, Row::result)
        .containsExactly(
            tuple("1", Result.MATCH),
            tuple("2", Result.MAPPED),
            tuple("3", Result.NOT_MIGRATED)
        );
    assertThat(comparison.rows().getFirst().lmsDate()).isEqualTo("Second Term + 1y 0m 0d, due 2019-12-01");
    assertThat(comparison.rows().get(2).detail()).isNotBlank();
    assertThat(comparison.scheduleComments())
        .extracting(CommentRow::result)
        .containsExactly(Result.MATCH);
    assertThat(comparisonService.migratedLicences()).contains(P9244);
  }

  @Test
  void compare_afterANewScheduleVersionIsApplied_stillPairsTheCopiedActivitiesWithPears() {
    migrate();
    when(userDetailService.getUserDetail()).thenReturn(ServiceUserDetailTestUtil.newBuilder().build());
    var newDetail = newScheduleVersionFixture.applyNewVersion(migratedScheduleReader.read(licence).orElseThrow().detail());
    entityManager.flush();
    entityManager.clear();

    var comparison = comparisonService.compare(List.of(P9244)).getFirst();

    assertThat(migratedScheduleReader.read(licence).orElseThrow().detail().getId()).isEqualTo(newDetail.getId());
    assertThat(comparison.issueDescriptions()).isEmpty();
    assertThat(comparison.rows())
        .extracting(Row::pearsActivityId, Row::result)
        .containsExactly(
            tuple("1", Result.MATCH),
            tuple("2", Result.MAPPED),
            tuple("3", Result.NOT_MIGRATED)
        );
  }

  @Test
  void compare_whenLmsWasChangedSinceTheMigration_showsWhatDiffers() {
    migrate();
    var studies = entityManager.createQuery(
            "FROM WorkProgrammeActivity WHERE licenceSchedule.id = :id AND description = 'Geological studies'",
            WorkProgrammeActivity.class)
        .setParameter("id", licenceSchedule.getId())
        .getSingleResult();
    studies.setDescription("Geological and geophysical studies");
    var addedInLms = new ScheduleComment();
    addedInLms.setLicenceSchedule(licenceSchedule);
    addedInLms.setComment("Added in LMS");
    addedInLms.setTimestamp(Instant.now());
    entityManager.persist(addedInLms);
    entityManager.flush();

    var comparison = comparisonService.compare(List.of(P9244)).getFirst();

    assertThat(comparison.hasIssues()).isTrue();
    assertThat(comparison.rows())
        .filteredOn(row -> "2".equals(row.pearsActivityId()))
        .extracting(Row::result, Row::detail)
        .containsExactly(tuple(Result.DIFFERS, "Description differs"));
    assertThat(comparison.scheduleComments())
        .extracting(CommentRow::lms, CommentRow::result)
        .containsExactly(
            tuple("General note (PEARS note by John Smith)", Result.MATCH),
            tuple("Added in LMS", Result.ADDED_IN_LMS)
        );
  }

  @Test
  void compare_beforeTheWorkProgrammeIsMigrated_showsItMissingInLms() {
    var comparison = comparisonService.compare(List.of(P9244)).getFirst();

    assertThat(comparison.rows())
        .extracting(Row::pearsActivityId, Row::result)
        .containsExactly(
            tuple("1", Result.MISSING_IN_LMS),
            tuple("2", Result.MISSING_IN_LMS),
            tuple("3", Result.NOT_MIGRATED)
        );
    assertThat(comparison.scheduleComments())
        .extracting(CommentRow::result)
        .containsExactly(Result.MISSING_IN_LMS);
  }

  @Test
  void compare_whenTheLicenceHasNoMigratedSchedule_showsEveryActivityNotMigrated() {
    var other = new PearsLicenceReference("P", 9246);
    entityManager.persist(LicenceTestUtil.builder()
        .withId(9246)
        .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
        .withLicenceNumber("9246")
        .withLicenceReference("P9246")
        .build());
    when(pearsWorkProgrammeService.licenceWorkProgrammes(other)).thenReturn(List.of(PROGRAMME));

    var comparison = comparisonService.compare(List.of(other)).getFirst();

    assertThat(comparison.migrationFailure()).isEqualTo(PearsWorkProgrammeMigrationService.NO_MIGRATED_SCHEDULE);
    assertThat(comparison.rows()).extracting(Row::result).containsOnly(Result.NOT_MIGRATED);
    assertThat(comparison.hasIssues()).isTrue();
  }

  @Test
  void compare_whenTheLicenceIsNotInLmsOrPearsHasNoWorkProgramme_saysSo() {
    var notInLms = new PearsLicenceReference("P", 1);
    when(pearsWorkProgrammeService.licenceWorkProgrammes(P9244)).thenReturn(List.of());

    var comparisons = comparisonService.compare(List.of(notInLms, P9244));

    assertThat(comparisons).extracting(PearsWorkProgrammeComparison::problem).containsExactly(
        PearsWorkProgrammeMigrationService.NOT_IN_LMS,
        PearsWorkProgrammeComparisonService.NO_PEARS_WORK_PROGRAMME
    );
  }

  @Test
  void compareInParallel_comparesEachLicenceAndKeepsTheirOrder() {
    // Each licence is compared in a transaction of its own, which can't see this test's, so none is in LMS there
    var licences = List.of(new PearsLicenceReference("P", 99993), new PearsLicenceReference("P", 99991),
        new PearsLicenceReference("P", 99992));

    var comparisons = comparisonService.compareInParallel(licences);

    assertThat(comparisons).extracting(PearsWorkProgrammeComparison::licenceReference)
        .containsExactly("P99993", "P99991", "P99992");
    assertThat(comparisons).extracting(PearsWorkProgrammeComparison::problem)
        .containsOnly(PearsWorkProgrammeMigrationService.NOT_IN_LMS);
  }

  private void migrate() {
    var schedule = migratedScheduleReader.read(licence).orElseThrow().schedule();
    var mapped = PearsWorkProgrammeMapper.map(P9244, List.of(PROGRAMME), schedule);
    assertThat(pearsWorkProgrammeWriter.write(licence, mapped, UUID.randomUUID())).isTrue();
    entityManager.flush();
    entityManager.clear();
  }
}
