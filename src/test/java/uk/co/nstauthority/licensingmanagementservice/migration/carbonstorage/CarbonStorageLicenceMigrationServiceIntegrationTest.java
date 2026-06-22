package uk.co.nstauthority.licensingmanagementservice.migration.carbonstorage;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDuration;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.TermType;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisation;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisationRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTermRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencestartdate.LicenceStartDateRepository;
import uk.co.nstauthority.licensingmanagementservice.util.IntegrationTest;

@Sql(
    scripts = "classpath:migration/create-cs-migration-tables.sql",
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS,
    config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED)
)
@Transactional
@IntegrationTest
class CarbonStorageLicenceMigrationServiceIntegrationTest {

  @Autowired
  private CarbonStorageLicenceMigrationService carbonStorageLicenceMigrationService;

  @Autowired
  private EntityManager em;

  @Autowired
  private LicenceRepository licenceRepository;

  @Autowired
  private LicenceResponsibleOrganisationRepository licenceResponsibleOrganisationRepository;

  @Autowired
  private LicenceScheduleDetailRepository licenceScheduleDetailRepository;

  @Autowired
  private LicenceStartDateRepository licenceStartDateRepository;

  @Autowired
  private LicenceScheduleTermRepository licenceScheduleTermRepository;

  @Test
  void migrateLicences_persistsLicencesAndResponsibleOrganisations() {
    var extract = new CarbonStorageLicenceMigrationExtract();
    extract.setLicenceRef("CS001/2020");
    extract.setLicenceNumber("1");
    extract.setResponsibleOrgs("Org A, Org B");
    extract.setStatus("Extant");
    em.persist(extract);

    var orgMappingA = new CarbonStorageLicenceOrgMapping();
    orgMappingA.setCsExtractResponsibleOrganisation("Org A");
    orgMappingA.setOrganisationUnitId(100);
    em.persist(orgMappingA);

    var orgMappingB = new CarbonStorageLicenceOrgMapping();
    orgMappingB.setCsExtractResponsibleOrganisation("Org B");
    orgMappingB.setOrganisationUnitId(200);
    em.persist(orgMappingB);

    em.flush();

    carbonStorageLicenceMigrationService.migrateLicences();

    var licence = licenceRepository.findByLicenceReference("CS001/2020").orElseThrow();

    assertThat(licence.getType()).isEqualTo(LicenceType.CARBON_STORAGE);
    assertThat(licence.getLicenceNumber()).isEqualTo("1");
    assertThat(licence.getPrefix()).isEqualTo(LicenceType.CARBON_STORAGE.getPrefix());
    assertThat(licence.getStatus()).isEqualTo(LicenceStatus.EXTANT);

    var responsibleOrgs = licenceResponsibleOrganisationRepository.findAllByLicence(licence);
    assertThat(responsibleOrgs).hasSize(2);
    assertThat(responsibleOrgs)
        .extracting(LicenceResponsibleOrganisation::getResponsibleOrganisationId)
        .containsExactlyInAnyOrder(100, 200);
    assertThat(responsibleOrgs)
        .extracting(LicenceResponsibleOrganisation::getManagedByLms)
        .containsOnly(true);
  }

  @Test
  void migrateSchedules_persistsScheduleStartDateAndTermForEachCsLicence() {
    var licence = LicenceTestUtil.builder()
        .withId(10001)
        .withLicenceReference("CS002/2021")
        .withLicenceType(LicenceType.CARBON_STORAGE)
        .build();
    em.persist(licence);

    var startDateExtract = new CarbonStorageStartDateMigrationExtract();
    startDateExtract.setLicenceRef("CS002/2021");
    startDateExtract.setStartDate("01/01/2020");
    em.persist(startDateExtract);

    var termExtract = new CarbonStorageTermMigrationExtract();
    termExtract.setId(1);
    termExtract.setLicenceRef("CS002/2021");
    termExtract.setTerm("Initial");
    termExtract.setYears(5);
    termExtract.setMonths(0);
    termExtract.setDays(0);
    em.persist(termExtract);

    em.flush();

    carbonStorageLicenceMigrationService.migrateSchedules();

    em.flush();

    var scheduleDetail = licenceScheduleDetailRepository
        .findByLicenceSchedule_LicenceAndStatus(licence, LicenceScheduleDetailStatus.ACTIVE)
        .orElseThrow();

    assertThat(scheduleDetail.getStatus()).isEqualTo(LicenceScheduleDetailStatus.ACTIVE);
    assertThat(scheduleDetail.getCreatedInstant()).isNotNull();

    var startDate = licenceStartDateRepository.findByLicenceScheduleDetail(scheduleDetail).orElseThrow();
    assertThat(startDate.getStartDate()).isEqualTo(LocalDate.of(2020, 1, 1));

    var terms = licenceScheduleTermRepository.findAllByLicenceScheduleDetail(scheduleDetail);
    assertThat(terms).hasSize(1);

    var term = terms.get(0);
    assertThat(term.getTermType()).isEqualTo(TermType.INITIAL_CS);
    assertThat(term.getTermDuration()).isEqualTo(new ThreeFieldDuration(5, 0, 0));
    assertThat(term.getStartDate()).isEqualTo(LocalDate.of(2020, 1, 1));
    assertThat(term.getEndDate()).isEqualTo(LocalDate.of(2024, 12, 31));
  }
}
