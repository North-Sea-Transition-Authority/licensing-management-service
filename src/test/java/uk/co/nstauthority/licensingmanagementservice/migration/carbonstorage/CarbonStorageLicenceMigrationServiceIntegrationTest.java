package uk.co.nstauthority.licensingmanagementservice.migration.carbonstorage;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDuration;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceStatusType;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.TermType;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisation;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisationRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventcomments.EventCommentRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventcomments.EventCommentStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTermRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencestartdate.LicenceStartDateRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivity;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityCategory;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityCommitment;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityDateOption;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.status.WorkProgrammeActivityStatusRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.status.WorkProgrammeStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.status.LicenceStatusRepository;
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

  @Autowired
  private WorkProgrammeActivityRepository workProgrammeActivityRepository;

  @Autowired
  private WorkProgrammeActivityStatusRepository workProgrammeActivityStatusRepository;

  @Autowired
  private EventCommentRepository eventCommentRepository;

  @Autowired
  private LicenceStatusRepository licenceStatusRepository;

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

    var licenceStatuses = licenceStatusRepository.findAllByLicence(licence);
    assertThat(licenceStatuses).hasSize(1);
    assertThat(licenceStatuses.get(0).getStatus()).isEqualTo(LicenceStatusType.EXTANT);
    assertThat(licenceStatuses.get(0).getStatusDate()).isNotNull();

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
    termExtract.setCaseDate("01/06/2021");
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
    assertThat(scheduleDetail.getCreatedInstant())
        .isEqualTo(LocalDate.of(2021, Month.JUNE, 1).atStartOfDay(ZoneOffset.UTC).toInstant());

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

  @Test
  void migrateSchedules_whenMultipleCaseDates_createsDetailsForEachCaseWithCorrectStatuses() {
    var licence = LicenceTestUtil.builder()
        .withId(10002)
        .withLicenceReference("CS003/2021")
        .withLicenceType(LicenceType.CARBON_STORAGE)
        .build();
    em.persist(licence);

    var termExtract1 = new CarbonStorageTermMigrationExtract();
    termExtract1.setId(2);
    termExtract1.setLicenceRef("CS003/2021");
    termExtract1.setCaseDate("01/01/2021");
    termExtract1.setTerm("Initial");
    termExtract1.setYears(5);
    termExtract1.setMonths(0);
    termExtract1.setDays(0);
    em.persist(termExtract1);

    var termExtract2 = new CarbonStorageTermMigrationExtract();
    termExtract2.setId(3);
    termExtract2.setLicenceRef("CS003/2021");
    termExtract2.setCaseDate("01/06/2021");
    termExtract2.setTerm("Initial");
    termExtract2.setYears(6);
    termExtract2.setMonths(0);
    termExtract2.setDays(0);
    em.persist(termExtract2);

    em.flush();

    carbonStorageLicenceMigrationService.migrateSchedules();

    em.flush();

    var allDetails = licenceScheduleDetailRepository.findAllByLicenceSchedule_Licence(licence);
    assertThat(allDetails).hasSize(2);

    var replacedDetail = allDetails.stream()
        .filter(d -> d.getStatus() == LicenceScheduleDetailStatus.REPLACED)
        .findFirst().orElseThrow();
    var activeDetail = allDetails.stream()
        .filter(d -> d.getStatus() == LicenceScheduleDetailStatus.ACTIVE)
        .findFirst().orElseThrow();

    assertThat(replacedDetail.getCreatedInstant())
        .isEqualTo(LocalDate.of(2021, Month.JANUARY, 1).atStartOfDay(ZoneOffset.UTC).toInstant());
    assertThat(activeDetail.getCreatedInstant())
        .isEqualTo(LocalDate.of(2021, Month.JUNE, 1).atStartOfDay(ZoneOffset.UTC).toInstant());

    var replacedTerms = licenceScheduleTermRepository.findAllByLicenceScheduleDetail(replacedDetail);
    assertThat(replacedTerms).hasSize(1);
    assertThat(replacedTerms.get(0).getTermDuration()).isEqualTo(new ThreeFieldDuration(5, 0, 0));

    var activeTerms = licenceScheduleTermRepository.findAllByLicenceScheduleDetail(activeDetail);
    assertThat(activeTerms).hasSize(1);
    assertThat(activeTerms.get(0).getTermDuration()).isEqualTo(new ThreeFieldDuration(6, 0, 0));
  }

  @Test
  void migrateSchedules_whenCaseHasNoTermsInTermTable_inheritsTermsFromPreviousCase() {
    var licence = LicenceTestUtil.builder()
        .withId(10003)
        .withLicenceReference("CS004/2021")
        .withLicenceType(LicenceType.CARBON_STORAGE)
        .build();
    em.persist(licence);

    var termExtract = new CarbonStorageTermMigrationExtract();
    termExtract.setId(4);
    termExtract.setLicenceRef("CS004/2021");
    termExtract.setCaseDate("01/01/2021");
    termExtract.setTerm("Initial");
    termExtract.setYears(5);
    termExtract.setMonths(0);
    termExtract.setDays(0);
    em.persist(termExtract);

    // second case appears only in the work programme table — no entry in the term table
    var wpExtract = new CarbonStorageWorkProgrammeMigrationExtract();
    wpExtract.setId(1);
    wpExtract.setLicenceRef("CS004/2021");
    wpExtract.setCaseDate("01/06/2021");
    em.persist(wpExtract);

    em.flush();

    carbonStorageLicenceMigrationService.migrateSchedules();

    em.flush();

    var allDetails = licenceScheduleDetailRepository.findAllByLicenceSchedule_Licence(licence);
    assertThat(allDetails).hasSize(2);

    var replacedDetail = allDetails.stream()
        .filter(d -> d.getStatus() == LicenceScheduleDetailStatus.REPLACED)
        .findFirst().orElseThrow();
    var activeDetail = allDetails.stream()
        .filter(d -> d.getStatus() == LicenceScheduleDetailStatus.ACTIVE)
        .findFirst().orElseThrow();

    var replacedTerms = licenceScheduleTermRepository.findAllByLicenceScheduleDetail(replacedDetail);
    assertThat(replacedTerms).hasSize(1);
    assertThat(replacedTerms.get(0).getTermDuration()).isEqualTo(new ThreeFieldDuration(5, 0, 0));

    // active case has no own terms — should inherit the 5-year term from the replaced case
    var activeTerms = licenceScheduleTermRepository.findAllByLicenceScheduleDetail(activeDetail);
    assertThat(activeTerms).hasSize(1);
    assertThat(activeTerms.get(0).getTermDuration()).isEqualTo(new ThreeFieldDuration(5, 0, 0));
  }

  @Test
  void migrateSchedules_whenLicenceHasNoCases_createsActiveDetailAsFallback() {
    var licence = LicenceTestUtil.builder()
        .withId(10004)
        .withLicenceReference("CS005/2021")
        .withLicenceType(LicenceType.CARBON_STORAGE)
        .build();
    em.persist(licence);

    em.flush();

    carbonStorageLicenceMigrationService.migrateSchedules();

    em.flush();

    var allDetails = licenceScheduleDetailRepository.findAllByLicenceSchedule_Licence(licence);
    assertThat(allDetails).hasSize(1);
    assertThat(allDetails.get(0).getStatus()).isEqualTo(LicenceScheduleDetailStatus.ACTIVE);
  }

  @Test
  void migrateSchedules_whenSameCaseDateInBothExtractTables_createsSingleDetail() {
    var licence = LicenceTestUtil.builder()
        .withId(10005)
        .withLicenceReference("CS006/2021")
        .withLicenceType(LicenceType.CARBON_STORAGE)
        .build();
    em.persist(licence);

    var termExtract = new CarbonStorageTermMigrationExtract();
    termExtract.setId(5);
    termExtract.setLicenceRef("CS006/2021");
    termExtract.setCaseDate("01/01/2021");
    termExtract.setTerm("Initial");
    termExtract.setYears(5);
    termExtract.setMonths(0);
    termExtract.setDays(0);
    em.persist(termExtract);

    var wpExtract = new CarbonStorageWorkProgrammeMigrationExtract();
    wpExtract.setId(2);
    wpExtract.setLicenceRef("CS006/2021");
    wpExtract.setCaseDate("01/01/2021");
    em.persist(wpExtract);

    em.flush();

    carbonStorageLicenceMigrationService.migrateSchedules();

    em.flush();

    var allDetails = licenceScheduleDetailRepository.findAllByLicenceSchedule_Licence(licence);
    assertThat(allDetails).hasSize(1);
    assertThat(allDetails.get(0).getStatus()).isEqualTo(LicenceScheduleDetailStatus.ACTIVE);
  }

  @Test
  void migrateSchedules_savesWorkProgrammeActivitiesLinkedToMatchingDetail() {
    var licence = LicenceTestUtil.builder()
        .withId(10006)
        .withLicenceReference("CS007/2021")
        .withLicenceType(LicenceType.CARBON_STORAGE)
        .build();
    em.persist(licence);

    var termExtract = new CarbonStorageTermMigrationExtract();
    termExtract.setId(6);
    termExtract.setLicenceRef("CS007/2021");
    termExtract.setCaseDate("01/01/2021");
    termExtract.setTerm("Initial");
    termExtract.setYears(5);
    termExtract.setMonths(0);
    termExtract.setDays(0);
    em.persist(termExtract);

    var eventId1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
    var eventId2 = UUID.fromString("00000000-0000-0000-0000-000000000002");

    var wpExtract1 = new CarbonStorageWorkProgrammeMigrationExtract();
    wpExtract1.setId(3);
    wpExtract1.setLicenceRef("CS007/2021");
    wpExtract1.setCaseDate("01/01/2021");
    wpExtract1.setUniqueEventId(eventId1);
    em.persist(wpExtract1);

    var wpExtract2 = new CarbonStorageWorkProgrammeMigrationExtract();
    wpExtract2.setId(4);
    wpExtract2.setLicenceRef("CS007/2021");
    wpExtract2.setCaseDate("01/01/2021");
    wpExtract2.setUniqueEventId(eventId2);
    em.persist(wpExtract2);

    em.flush();

    carbonStorageLicenceMigrationService.migrateSchedules();

    em.flush();

    var activeDetail = licenceScheduleDetailRepository
        .findByLicenceSchedule_LicenceAndStatus(licence, LicenceScheduleDetailStatus.ACTIVE)
        .orElseThrow();

    var activities = workProgrammeActivityRepository.findAllByLicenceScheduleDetail(activeDetail);
    assertThat(activities).hasSize(2);
    assertThat(activities)
        .extracting(WorkProgrammeActivity::getOriginalEventId)
        .containsExactlyInAnyOrder(eventId1, eventId2);
  }

  @Test
  void migrateSchedules_whenWorkProgrammeActivityHasStatus_savesActivityStatus() {
    var licence = LicenceTestUtil.builder()
        .withId(10007)
        .withLicenceReference("CS008/2021")
        .withLicenceType(LicenceType.CARBON_STORAGE)
        .build();
    em.persist(licence);

    var termExtract = new CarbonStorageTermMigrationExtract();
    termExtract.setId(7);
    termExtract.setLicenceRef("CS008/2021");
    termExtract.setCaseDate("01/01/2021");
    termExtract.setTerm("Initial");
    termExtract.setYears(5);
    termExtract.setMonths(0);
    termExtract.setDays(0);
    em.persist(termExtract);

    var wpExtract = new CarbonStorageWorkProgrammeMigrationExtract();
    wpExtract.setId(10);
    wpExtract.setLicenceRef("CS008/2021");
    wpExtract.setCaseDate("01/01/2021");
    wpExtract.setUniqueEventId(UUID.fromString("00000000-0000-0000-0000-000000000010"));
    wpExtract.setStatus("In progress");
    em.persist(wpExtract);

    em.flush();

    carbonStorageLicenceMigrationService.migrateSchedules();

    em.flush();

    var activeDetail = licenceScheduleDetailRepository
        .findByLicenceSchedule_LicenceAndStatus(licence, LicenceScheduleDetailStatus.ACTIVE)
        .orElseThrow();
    var activity = workProgrammeActivityRepository.findAllByLicenceScheduleDetail(activeDetail).get(0);
    var statuses = workProgrammeActivityStatusRepository.findAllByScheduleEvent_OriginalEventId(activity.getOriginalEventId());

    assertThat(statuses).hasSize(1);
    assertThat(statuses.get(0).getStatus()).isEqualTo(WorkProgrammeStatus.IN_PROGRESS);
    assertThat(statuses.get(0).getAppliedDatetime())
        .isEqualTo(LocalDate.of(2021, Month.JANUARY, 1).atStartOfDay(ZoneOffset.UTC).toInstant());
  }

  @Test
  void migrateSchedules_whenWorkProgrammeActivityHasCategory_setsCategory() {
    var licence = LicenceTestUtil.builder()
        .withId(10008)
        .withLicenceReference("CS009/2021")
        .withLicenceType(LicenceType.CARBON_STORAGE)
        .build();
    em.persist(licence);

    var termExtract = new CarbonStorageTermMigrationExtract();
    termExtract.setId(8);
    termExtract.setLicenceRef("CS009/2021");
    termExtract.setCaseDate("01/01/2021");
    termExtract.setTerm("Initial");
    termExtract.setYears(5);
    termExtract.setMonths(0);
    termExtract.setDays(0);
    em.persist(termExtract);

    var wpExtract = new CarbonStorageWorkProgrammeMigrationExtract();
    wpExtract.setId(11);
    wpExtract.setLicenceRef("CS009/2021");
    wpExtract.setCaseDate("01/01/2021");
    wpExtract.setUniqueEventId(UUID.fromString("00000000-0000-0000-0000-000000000011"));
    wpExtract.setCategory("Drilling Well");
    em.persist(wpExtract);

    em.flush();

    carbonStorageLicenceMigrationService.migrateSchedules();

    em.flush();

    var activeDetail = licenceScheduleDetailRepository
        .findByLicenceSchedule_LicenceAndStatus(licence, LicenceScheduleDetailStatus.ACTIVE)
        .orElseThrow();
    var activity = workProgrammeActivityRepository.findAllByLicenceScheduleDetail(activeDetail).get(0);

    assertThat(activity.getCategory()).isEqualTo(WorkProgrammeActivityCategory.DRILLING_WELL);
  }

  @Test
  void migrateSchedules_whenWorkProgrammeActivityHasOtherCategory_setsOtherCategoryName() {
    var licence = LicenceTestUtil.builder()
        .withId(10009)
        .withLicenceReference("CS010/2021")
        .withLicenceType(LicenceType.CARBON_STORAGE)
        .build();
    em.persist(licence);

    var termExtract = new CarbonStorageTermMigrationExtract();
    termExtract.setId(9);
    termExtract.setLicenceRef("CS010/2021");
    termExtract.setCaseDate("01/01/2021");
    termExtract.setTerm("Initial");
    termExtract.setYears(5);
    termExtract.setMonths(0);
    termExtract.setDays(0);
    em.persist(termExtract);

    var wpExtract = new CarbonStorageWorkProgrammeMigrationExtract();
    wpExtract.setId(12);
    wpExtract.setLicenceRef("CS010/2021");
    wpExtract.setCaseDate("01/01/2021");
    wpExtract.setUniqueEventId(UUID.fromString("00000000-0000-0000-0000-000000000012"));
    wpExtract.setCategory("Other activity");
    wpExtract.setOtherCategory("Custom legacy activity");
    em.persist(wpExtract);

    em.flush();

    carbonStorageLicenceMigrationService.migrateSchedules();

    em.flush();

    var activeDetail = licenceScheduleDetailRepository
        .findByLicenceSchedule_LicenceAndStatus(licence, LicenceScheduleDetailStatus.ACTIVE)
        .orElseThrow();
    var activity = workProgrammeActivityRepository.findAllByLicenceScheduleDetail(activeDetail).get(0);

    assertThat(activity.getCategory()).isEqualTo(WorkProgrammeActivityCategory.OTHER_ACTIVITY);
    assertThat(activity.getOtherCategoryName()).isEqualTo("Custom legacy activity");
  }

  @Test
  void migrateSchedules_whenWorkProgrammeActivityHasCommitment_setsCommitment() {
    var licence = LicenceTestUtil.builder()
        .withId(10010)
        .withLicenceReference("CS011/2021")
        .withLicenceType(LicenceType.CARBON_STORAGE)
        .build();
    em.persist(licence);

    var termExtract = new CarbonStorageTermMigrationExtract();
    termExtract.setId(10);
    termExtract.setLicenceRef("CS011/2021");
    termExtract.setCaseDate("01/01/2021");
    termExtract.setTerm("Initial");
    termExtract.setYears(5);
    termExtract.setMonths(0);
    termExtract.setDays(0);
    em.persist(termExtract);

    var wpExtract = new CarbonStorageWorkProgrammeMigrationExtract();
    wpExtract.setId(13);
    wpExtract.setLicenceRef("CS011/2021");
    wpExtract.setCaseDate("01/01/2021");
    wpExtract.setUniqueEventId(UUID.fromString("00000000-0000-0000-0000-000000000013"));
    wpExtract.setCommitment("Contingent");
    em.persist(wpExtract);

    em.flush();

    carbonStorageLicenceMigrationService.migrateSchedules();

    em.flush();

    var activeDetail = licenceScheduleDetailRepository
        .findByLicenceSchedule_LicenceAndStatus(licence, LicenceScheduleDetailStatus.ACTIVE)
        .orElseThrow();
    var activity = workProgrammeActivityRepository.findAllByLicenceScheduleDetail(activeDetail).get(0);

    assertThat(activity.getCommitment()).isEqualTo(WorkProgrammeActivityCommitment.CONTINGENT);
  }

  @Test
  void migrateSchedules_whenWorkProgrammeActivityHasTerm_linksToScheduleTerm() {
    var licence = LicenceTestUtil.builder()
        .withId(10011)
        .withLicenceReference("CS012/2021")
        .withLicenceType(LicenceType.CARBON_STORAGE)
        .build();
    em.persist(licence);

    var termExtract = new CarbonStorageTermMigrationExtract();
    termExtract.setId(11);
    termExtract.setLicenceRef("CS012/2021");
    termExtract.setCaseDate("01/01/2021");
    termExtract.setTerm("APPRAISAL");
    termExtract.setYears(3);
    termExtract.setMonths(0);
    termExtract.setDays(0);
    em.persist(termExtract);

    var wpExtract = new CarbonStorageWorkProgrammeMigrationExtract();
    wpExtract.setId(14);
    wpExtract.setLicenceRef("CS012/2021");
    wpExtract.setCaseDate("01/01/2021");
    wpExtract.setUniqueEventId(UUID.fromString("00000000-0000-0000-0000-000000000014"));
    wpExtract.setTerm("Appraisal Term");
    em.persist(wpExtract);

    em.flush();

    carbonStorageLicenceMigrationService.migrateSchedules();

    em.flush();

    var activeDetail = licenceScheduleDetailRepository
        .findByLicenceSchedule_LicenceAndStatus(licence, LicenceScheduleDetailStatus.ACTIVE)
        .orElseThrow();
    var activity = workProgrammeActivityRepository.findAllByLicenceScheduleDetail(activeDetail).get(0);

    assertThat(activity.getLicenceScheduleTerm()).isNotNull();
    assertThat(activity.getLicenceScheduleTerm().getTermType()).isEqualTo(TermType.APPRAISAL);
  }

  @Test
  void migrateSchedules_whenWorkProgrammeActivityHasRelativeDateOption_setsRelativeDuration() {
    var licence = LicenceTestUtil.builder()
        .withId(10012)
        .withLicenceReference("CS013/2021")
        .withLicenceType(LicenceType.CARBON_STORAGE)
        .build();
    em.persist(licence);

    var termExtract = new CarbonStorageTermMigrationExtract();
    termExtract.setId(12);
    termExtract.setLicenceRef("CS013/2021");
    termExtract.setCaseDate("01/01/2021");
    termExtract.setTerm("Initial");
    termExtract.setYears(5);
    termExtract.setMonths(0);
    termExtract.setDays(0);
    em.persist(termExtract);

    var wpExtract = new CarbonStorageWorkProgrammeMigrationExtract();
    wpExtract.setId(15);
    wpExtract.setLicenceRef("CS013/2021");
    wpExtract.setCaseDate("01/01/2021");
    wpExtract.setUniqueEventId(UUID.fromString("00000000-0000-0000-0000-000000000015"));
    wpExtract.setDateOption(WorkProgrammeActivityDateOption.RELATIVE_DATE);
    wpExtract.setRelativeYears(1);
    wpExtract.setRelativeMonths(6);
    wpExtract.setRelativeDays(0);
    em.persist(wpExtract);

    em.flush();

    carbonStorageLicenceMigrationService.migrateSchedules();

    em.flush();

    var activeDetail = licenceScheduleDetailRepository
        .findByLicenceSchedule_LicenceAndStatus(licence, LicenceScheduleDetailStatus.ACTIVE)
        .orElseThrow();
    var activity = workProgrammeActivityRepository.findAllByLicenceScheduleDetail(activeDetail).get(0);

    assertThat(activity.getDateOption()).isEqualTo(WorkProgrammeActivityDateOption.RELATIVE_DATE);
    assertThat(activity.getRelativeDuration()).isEqualTo(new ThreeFieldDuration(1, 6, 0));
  }

  @Test
  void migrateSchedules_whenWorkProgrammeActivityHasComment_savesEventComment() {
    var licence = LicenceTestUtil.builder()
        .withId(10013)
        .withLicenceReference("CS014/2021")
        .withLicenceType(LicenceType.CARBON_STORAGE)
        .build();
    em.persist(licence);

    var termExtract = new CarbonStorageTermMigrationExtract();
    termExtract.setId(13);
    termExtract.setLicenceRef("CS014/2021");
    termExtract.setCaseDate("15/03/2021");
    termExtract.setTerm("Initial");
    termExtract.setYears(5);
    termExtract.setMonths(0);
    termExtract.setDays(0);
    em.persist(termExtract);

    var wpExtract = new CarbonStorageWorkProgrammeMigrationExtract();
    wpExtract.setId(16);
    wpExtract.setLicenceRef("CS014/2021");
    wpExtract.setCaseDate("15/03/2021");
    wpExtract.setUniqueEventId(UUID.fromString("00000000-0000-0000-0000-000000000016"));
    wpExtract.setComments("Legacy note from migration");
    em.persist(wpExtract);

    em.flush();

    carbonStorageLicenceMigrationService.migrateSchedules();

    em.flush();

    var activeDetail = licenceScheduleDetailRepository
        .findByLicenceSchedule_LicenceAndStatus(licence, LicenceScheduleDetailStatus.ACTIVE)
        .orElseThrow();
    var activity = workProgrammeActivityRepository.findAllByLicenceScheduleDetail(activeDetail).get(0);
    var comments = eventCommentRepository.getAllByScheduleEvent_LicenceScheduleAndStatus(
        activeDetail.getLicenceSchedule(), EventCommentStatus.PUBLISHED);

    assertThat(comments).hasSize(1);
    assertThat(comments.get(0).getScheduleEvent().getId()).isEqualTo(activity.getId());
    assertThat(comments.get(0).getComment()).isEqualTo("Legacy note from migration");
    assertThat(comments.get(0).getStatus()).isEqualTo(EventCommentStatus.PUBLISHED);
    assertThat(comments.get(0).getTimestamp())
        .isEqualTo(LocalDate.of(2021, Month.MARCH, 15).atStartOfDay(ZoneOffset.UTC).toInstant());
  }

  @Test
  void migrateSchedules_whenSameUniqueEventIdAcrossTwoCases_createsActivityForEachDetail() {
    var licence = LicenceTestUtil.builder()
        .withId(10014)
        .withLicenceReference("CS015/2021")
        .withLicenceType(LicenceType.CARBON_STORAGE)
        .build();
    em.persist(licence);

    var termExtract1 = new CarbonStorageTermMigrationExtract();
    termExtract1.setId(14);
    termExtract1.setLicenceRef("CS015/2021");
    termExtract1.setCaseDate("01/01/2021");
    termExtract1.setTerm("Initial");
    termExtract1.setYears(5);
    termExtract1.setMonths(0);
    termExtract1.setDays(0);
    em.persist(termExtract1);

    var termExtract2 = new CarbonStorageTermMigrationExtract();
    termExtract2.setId(15);
    termExtract2.setLicenceRef("CS015/2021");
    termExtract2.setCaseDate("01/06/2022");
    termExtract2.setTerm("Initial");
    termExtract2.setYears(6);
    termExtract2.setMonths(0);
    termExtract2.setDays(0);
    em.persist(termExtract2);

    var sharedEventId = UUID.fromString("00000000-0000-0000-0000-000000000017");

    var wpExtract1 = new CarbonStorageWorkProgrammeMigrationExtract();
    wpExtract1.setId(17);
    wpExtract1.setLicenceRef("CS015/2021");
    wpExtract1.setCaseDate("01/01/2021");
    wpExtract1.setUniqueEventId(sharedEventId);
    wpExtract1.setStatus("Open");
    wpExtract1.setComments("Initial case comment");
    em.persist(wpExtract1);

    var wpExtract2 = new CarbonStorageWorkProgrammeMigrationExtract();
    wpExtract2.setId(18);
    wpExtract2.setLicenceRef("CS015/2021");
    wpExtract2.setCaseDate("01/06/2022");
    wpExtract2.setUniqueEventId(sharedEventId);
    wpExtract2.setStatus("Complete");
    wpExtract2.setComments("Updated case comment");
    em.persist(wpExtract2);

    em.flush();

    carbonStorageLicenceMigrationService.migrateSchedules();

    em.flush();

    var allDetails = licenceScheduleDetailRepository.findAllByLicenceSchedule_Licence(licence);
    assertThat(allDetails).hasSize(2);

    var replacedDetail = allDetails.stream()
        .filter(d -> d.getStatus() == LicenceScheduleDetailStatus.REPLACED)
        .findFirst().orElseThrow();
    var activeDetail = allDetails.stream()
        .filter(d -> d.getStatus() == LicenceScheduleDetailStatus.ACTIVE)
        .findFirst().orElseThrow();

    var replacedActivities = workProgrammeActivityRepository.findAllByLicenceScheduleDetail(replacedDetail);
    var activeActivities = workProgrammeActivityRepository.findAllByLicenceScheduleDetail(activeDetail);

    assertThat(replacedActivities).hasSize(1);
    assertThat(activeActivities).hasSize(1);
    assertThat(replacedActivities.get(0).getOriginalEventId()).isEqualTo(sharedEventId);
    assertThat(activeActivities.get(0).getOriginalEventId()).isEqualTo(sharedEventId);

    var replacedStatus = workProgrammeActivityStatusRepository
        .findAllByScheduleEvent_OriginalEventId(replacedActivities.get(0).getOriginalEventId());
    var activeStatus = workProgrammeActivityStatusRepository
        .findAllByScheduleEvent_OriginalEventId(activeActivities.get(0).getOriginalEventId());

    assertThat(replacedStatus).hasSize(2);
    assertThat(activeStatus).hasSize(2);
    assertThat(replacedStatus)
        .extracting(s -> s.getStatus())
        .containsExactlyInAnyOrder(WorkProgrammeStatus.OPEN, WorkProgrammeStatus.COMPLETE);

    var schedule = activeDetail.getLicenceSchedule();
    var comments = eventCommentRepository.getAllByScheduleEvent_LicenceScheduleAndStatus(
        schedule, EventCommentStatus.PUBLISHED);

    assertThat(comments).hasSize(2);
    assertThat(comments)
        .extracting(c -> c.getComment())
        .containsExactlyInAnyOrder("Initial case comment", "Updated case comment");
    assertThat(comments)
        .extracting(c -> c.getScheduleEvent().getId())
        .containsExactlyInAnyOrder(replacedActivities.get(0).getId(), activeActivities.get(0).getId());
  }
}
