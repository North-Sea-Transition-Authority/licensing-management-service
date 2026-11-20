package uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.PearsEventFixture.term;

import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.transaction.support.TransactionOperations;
import uk.co.fivium.energyportalapi.client.organisation.OrganisationApi;
import uk.co.fivium.energyportalapi.client.user.UserApi;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleService;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.PearsLicenceReference;
import uk.co.nstauthority.licensingmanagementservice.util.IntegrationTest;

/**
 * Not transactional itself, so the writer's own transaction commits or rolls back as it would in a run.
 */
@Sql(
    scripts = "classpath:migration/create-pears-schedule-migration-tables.sql",
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS,
    config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED)
)
@IntegrationTest
class PearsScheduleWriterRollbackIntegrationTest {

  private static final List<PearsScheduleEventType> EVENT_TYPES = List.of(new PearsScheduleEventType("T", "Term", -3));

  @Autowired
  private EntityManager entityManager;

  @Autowired
  private TransactionOperations transactionOperations;

  @Autowired
  private PearsScheduleWriter pearsScheduleWriter;

  @Autowired
  private LicenceScheduleService licenceScheduleService;

  @MockitoBean
  private OrganisationApi organisationApi;

  @MockitoBean
  private UserApi userApi;

  private Licence licence;

  @BeforeEach
  void setUp() {
    licence = LicenceTestUtil.builder()
        .withId(9245)
        .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
        .withLicenceNumber("9245")
        .withLicenceReference("P9245")
        .build();
    transactionOperations.executeWithoutResult(status -> entityManager.persist(licence));
  }

  @AfterEach
  void tearDown() {
    transactionOperations.executeWithoutResult(
        status -> entityManager.remove(entityManager.find(Licence.class, licence.getId())));
  }

  @Test
  void write_whenItFailsPartWay_rollsBackTheWholeLicence() {
    var version = new PearsScheduleVersion(1, LocalDateTime.of(2019, Month.JANUARY, 1, 9, 0), null,
        LocalDate.of(2014, Month.DECEMBER, 1),
        List.of(
            term("1", LocalDate.of(2018, Month.DECEMBER, 1)).build(),
            term("2", LocalDate.of(2040, Month.DECEMBER, 1)).build()
        ));
    var pearsSchedule = new PearsSchedule(new PearsLicenceReference("P", 9245), List.of(version));
    // The mapper never gives a term 5, so it fails once the schedule, its detail, start date and first term are saved,
    // leaving the rollback work to undo
    var mappedSchedule = withSecondTermAsTermFive(new PearsScheduleMapper().map(pearsSchedule, EVENT_TYPES));

    assertThatThrownBy(() -> transactionOperations.executeWithoutResult(
        status -> pearsScheduleWriter.write(licence, mappedSchedule, pearsSchedule, UUID.randomUUID())))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("No LMS term type for term 5");

    assertThat(licenceScheduleService.getIdsOfLicencesWithASchedule(List.of(licence))).isEmpty();
  }

  private static MappedSchedule withSecondTermAsTermFive(MappedSchedule mappedSchedule) {
    var version = mappedSchedule.versions().getFirst();
    var terms = version.terms().stream()
        .map(term -> term.termNo() == 2
            ? new MappedTerm(term.eventKey(), 5, term.startDate(), term.endDate(), term.duration(), term.comment(),
                term.source())
            : term)
        .toList();
    var brokenVersion = new MappedScheduleVersion(version.plsdId(), version.status(), version.createdInstant(),
        version.startDate(), version.startDateBorrowed(), version.datesFromOffsets(), terms, version.phases(),
        version.expiry(), version.rates(), version.otherEvents());
    return new MappedSchedule(mappedSchedule.licence(), List.of(brokenVersion), mappedSchedule.leftOutVersions(),
        mappedSchedule.notes());
  }
}
