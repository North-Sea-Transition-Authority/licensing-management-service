package uk.co.nstauthority.licensingmanagementservice.licence.application;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplication;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetailRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceSchedule;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplication;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetailRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationRepository;
import uk.co.nstauthority.licensingmanagementservice.util.IntegrationTest;

@Transactional
@IntegrationTest
class ApplicationPersistenceIntegrationTest {

  private static final Instant SUBMITTED_IN_2091 = Instant.parse("2091-06-01T10:00:00Z");

  @Autowired
  private EntityManager entityManager;

  @Autowired
  private EntityManagerFactory entityManagerFactory;

  @Autowired
  private ScheduleWorkProgrammeApplicationRepository scheduleWorkProgrammeApplicationRepository;

  @Autowired
  private ScheduleWorkProgrammeApplicationDetailRepository scheduleWorkProgrammeApplicationDetailRepository;

  @Autowired
  private LicenceContinuationApplicationRepository licenceContinuationApplicationRepository;

  @Autowired
  private LicenceContinuationApplicationDetailRepository licenceContinuationApplicationDetailRepository;

  @Test
  void countByVersionNumberAndSubmittedDatetimeBetween_whenBothTypesSubmittedInYear_thenEachCountsOnlyItsOwnType() {
    var licenceSchedule = persistLicenceSchedule(9701);
    persistExtensionDetail(persistExtensionApplication(licenceSchedule), SUBMITTED_IN_2091);
    persistContinuationDetail(persistContinuationApplication(licenceSchedule), SUBMITTED_IN_2091);
    var startOfYear = Instant.parse("2091-01-01T00:00:00Z");
    var endOfYear = Instant.parse("2091-12-31T23:59:59Z");

    var extensionCount = scheduleWorkProgrammeApplicationDetailRepository
        .countByVersionNumberAndSubmittedDatetimeBetween(1, startOfYear, endOfYear);
    var continuationCount = licenceContinuationApplicationDetailRepository
        .countByVersionNumberAndSubmittedDatetimeBetween(1, startOfYear, endOfYear);

    assertThat(List.of(extensionCount, continuationCount)).containsExactly(1, 1);
  }

  @Test
  void findById_whenIdBelongsToTheOtherApplicationType_thenNothingIsFound() {
    var licenceSchedule = persistLicenceSchedule(9702);
    var extensionApplication = persistExtensionApplication(licenceSchedule);
    var continuationApplication = persistContinuationApplication(licenceSchedule);
    var extensionDetail = persistExtensionDetail(extensionApplication, null);
    var continuationDetail = persistContinuationDetail(continuationApplication, null);
    entityManager.clear();

    var results = List.of(
        scheduleWorkProgrammeApplicationRepository.findById(continuationApplication.getId()),
        licenceContinuationApplicationRepository.findById(extensionApplication.getId()),
        scheduleWorkProgrammeApplicationDetailRepository.findById(continuationDetail.getId()),
        licenceContinuationApplicationDetailRepository.findById(extensionDetail.getId())
    );

    assertThat(results).containsOnly(Optional.empty());
  }

  @Test
  void setStewardWuaId_whenStewardIsReallocated_thenOneRowHoldsTheLatestSteward() {
    var application = persistExtensionApplication(persistLicenceSchedule(9703));
    application.setStewardWuaId(111L);
    entityManager.flush();
    application.setStewardWuaId(222L);
    entityManager.flush();
    entityManager.clear();

    var reloaded = scheduleWorkProgrammeApplicationRepository.findById(application.getId()).orElseThrow();

    assertThat(List.of(reloaded.getStewardWuaId(), countStewardRows(application.getId()))).containsExactly(222L, 1L);
  }

  @Test
  void getStewardWuaId_whenNoStewardAssigned_thenNullAndNoRowIsWritten() {
    var application = persistExtensionApplication(persistLicenceSchedule(9704));
    entityManager.clear();

    var reloaded = scheduleWorkProgrammeApplicationRepository.findById(application.getId()).orElseThrow();

    assertThat(Arrays.asList(reloaded.getStewardWuaId(), countStewardRows(application.getId())))
        .containsExactly(null, 0L);
  }

  @Test
  void setLicenceSchedule_whenApplicationIsSaved_thenLicenceAndScheduleArePersisted() {
    var licenceSchedule = persistLicenceSchedule(9705);
    var application = persistContinuationApplication(licenceSchedule);
    entityManager.clear();

    var reloaded = licenceContinuationApplicationRepository.findById(application.getId()).orElseThrow();

    assertThat(List.of(reloaded.getLicence().getId(), reloaded.getLicenceSchedule().getId()))
        .containsExactly(licenceSchedule.getLicence().getId(), licenceSchedule.getId());
  }

  @Test
  void getTypedApplication_whenDetailIsLoadedFromTheDatabase_thenReturnsItsApplication() {
    var licenceSchedule = persistLicenceSchedule(9706);
    var extensionApplication = persistExtensionApplication(licenceSchedule);
    var continuationApplication = persistContinuationApplication(licenceSchedule);
    var extensionDetail = persistExtensionDetail(extensionApplication, null);
    var continuationDetail = persistContinuationDetail(continuationApplication, null);
    entityManager.clear();

    var reloadedExtension = scheduleWorkProgrammeApplicationDetailRepository.findById(extensionDetail.getId())
        .orElseThrow()
        .getScheduleWorkProgrammeApplication();
    var reloadedContinuation = licenceContinuationApplicationDetailRepository.findById(continuationDetail.getId())
        .orElseThrow()
        .getLicenceContinuationApplication();

    assertThat(List.of(reloadedExtension.getId(), reloadedContinuation.getId()))
        .containsExactly(extensionApplication.getId(), continuationApplication.getId());
  }

  @Test
  void findAllByStatusIn_whenMoreExtensionApplicationsExist_thenQueryCountStaysConstant() {
    var statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
    statistics.setStatisticsEnabled(true);
    persistSubmittedExtensionApplications(9750, 5);
    var queryCountForFiveApplications = countQueries(statistics,
        () -> scheduleWorkProgrammeApplicationDetailRepository.findAllByStatusIn(Set.of(ApplicationStatus.SUBMITTED)));
    persistSubmittedExtensionApplications(9755, 15);

    var queryCountForTwentyApplications = countQueries(statistics,
        () -> scheduleWorkProgrammeApplicationDetailRepository.findAllByStatusIn(Set.of(ApplicationStatus.SUBMITTED)));

    assertThat(queryCountForTwentyApplications).isEqualTo(queryCountForFiveApplications);
  }

  @Test
  void findAllByStatusIn_whenMoreContinuationApplicationsExist_thenQueryCountStaysConstant() {
    var statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
    statistics.setStatisticsEnabled(true);
    persistSubmittedContinuationApplications(9770, 5);
    var queryCountForFiveApplications = countQueries(statistics,
        () -> licenceContinuationApplicationDetailRepository.findAllByStatusIn(Set.of(ApplicationStatus.SUBMITTED)));
    persistSubmittedContinuationApplications(9775, 15);

    var queryCountForTwentyApplications = countQueries(statistics,
        () -> licenceContinuationApplicationDetailRepository.findAllByStatusIn(Set.of(ApplicationStatus.SUBMITTED)));

    assertThat(queryCountForTwentyApplications).isEqualTo(queryCountForFiveApplications);
  }

  private void persistSubmittedExtensionApplications(int firstLicenceId, int count) {
    for (int licenceId = firstLicenceId; licenceId < firstLicenceId + count; licenceId++) {
      var application = persistExtensionApplication(persistLicenceSchedule(licenceId));
      application.setStewardWuaId(111L);
      persistExtensionDetail(application, SUBMITTED_IN_2091);
    }
  }

  private void persistSubmittedContinuationApplications(int firstLicenceId, int count) {
    for (int licenceId = firstLicenceId; licenceId < firstLicenceId + count; licenceId++) {
      persistContinuationDetail(persistContinuationApplication(persistLicenceSchedule(licenceId)), SUBMITTED_IN_2091);
    }
  }

  // Clears the first-level cache so the count reflects a fresh request, rather than resolving associations
  // against entities this test has just persisted.
  private long countQueries(Statistics statistics, Runnable query) {
    entityManager.flush();
    entityManager.clear();
    statistics.clear();
    query.run();
    return statistics.getPrepareStatementCount();
  }

  private LicenceSchedule persistLicenceSchedule(int licenceId) {
    Licence licence = LicenceTestUtil.builder()
        .withId(licenceId)
        .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
        .withLicenceReference("P " + licenceId)
        .build();
    entityManager.persist(licence);

    var licenceSchedule = LicenceScheduleTestUtil.createLicenceSchedule(null, licence);
    entityManager.persist(licenceSchedule);
    return licenceSchedule;
  }

  private ScheduleWorkProgrammeApplication persistExtensionApplication(LicenceSchedule licenceSchedule) {
    var application = new ScheduleWorkProgrammeApplication();
    application.setLicenceSchedule(licenceSchedule);
    return scheduleWorkProgrammeApplicationRepository.saveAndFlush(application);
  }

  private LicenceContinuationApplication persistContinuationApplication(LicenceSchedule licenceSchedule) {
    var application = new LicenceContinuationApplication();
    application.setLicenceSchedule(licenceSchedule);
    return licenceContinuationApplicationRepository.saveAndFlush(application);
  }

  private ScheduleWorkProgrammeApplicationDetail persistExtensionDetail(
      ScheduleWorkProgrammeApplication application,
      Instant submittedDatetime
  ) {
    var detail = new ScheduleWorkProgrammeApplicationDetail();
    detail.setScheduleWorkProgrammeApplication(application);
    detail.setVersionNumber(1);
    detail.setStatus(submittedDatetime == null ? ApplicationStatus.DRAFT : ApplicationStatus.SUBMITTED);
    detail.setCreatedDatetime(Instant.now());
    detail.setSubmittedDatetime(submittedDatetime);
    return scheduleWorkProgrammeApplicationDetailRepository.saveAndFlush(detail);
  }

  private LicenceContinuationApplicationDetail persistContinuationDetail(
      LicenceContinuationApplication application,
      Instant submittedDatetime
  ) {
    var detail = new LicenceContinuationApplicationDetail();
    detail.setLicenceContinuationApplication(application);
    detail.setVersionNumber(1);
    detail.setStatus(submittedDatetime == null ? ApplicationStatus.DRAFT : ApplicationStatus.SUBMITTED);
    detail.setCreatedDatetime(Instant.now());
    detail.setSubmittedDatetime(submittedDatetime);
    return licenceContinuationApplicationDetailRepository.saveAndFlush(detail);
  }

  private long countStewardRows(UUID applicationId) {
    return (Long) entityManager.createNativeQuery(
            "SELECT count(*) FROM lms.schedule_work_programme_applications WHERE id = :id", Long.class)
        .setParameter("id", applicationId)
        .getSingleResult();
  }
}
