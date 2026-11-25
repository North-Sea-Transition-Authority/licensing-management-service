package uk.co.nstauthority.licensingmanagementservice.caseevent;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.caseevent.casenote.CaseNoteRepository;
import uk.co.nstauthority.licensingmanagementservice.caseevent.casenote.CaseNoteService;
import uk.co.nstauthority.licensingmanagementservice.caseevent.payload.CaseAllocationPayload;
import uk.co.nstauthority.licensingmanagementservice.caseevent.payload.CaseEventPayload;
import uk.co.nstauthority.licensingmanagementservice.caseevent.payload.CaseNotePayload;
import uk.co.nstauthority.licensingmanagementservice.caseevent.payload.CaseUpdateRequestPayload;
import uk.co.nstauthority.licensingmanagementservice.caseevent.payload.CaseUpdateSubmittedPayload;
import uk.co.nstauthority.licensingmanagementservice.caseevent.payload.DspAddendumUploadedPayload;
import uk.co.nstauthority.licensingmanagementservice.caseevent.payload.DspCorrectedPayload;
import uk.co.nstauthority.licensingmanagementservice.caseevent.payload.DspUploadedPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceSchedule;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplication;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationRepository;
import uk.co.nstauthority.licensingmanagementservice.util.IntegrationTest;

@IntegrationTest
class CaseEventIntegrationTest {

  private static final int LICENCE_ID = 9801;

  @Autowired
  private CaseEventRepository caseEventRepository;

  @Autowired
  private CaseNoteRepository caseNoteRepository;

  @Autowired
  private CaseNoteService caseNoteService;

  @Autowired
  private ScheduleWorkProgrammeApplicationRepository scheduleWorkProgrammeApplicationRepository;

  @Autowired
  private TransactionTemplate transactionTemplate;

  @Autowired
  private EntityManager em;

  @Test
  void addCaseNote_whenTransactionRollsBack_thenNeitherNoteNorEventIsSaved() {
    var author = ServiceUserDetailTestUtil.newBuilder().build();

    var saved = transactionTemplate.execute(status -> {
      var application = persistApplication(persistLicenceSchedule());
      var caseNote = caseNoteService.addCaseNote(application, "Chased the operator for the missing map", author);
      status.setRollbackOnly();
      return new SavedCaseNote(application.getId(), caseNote.getId());
    });

    assertThat(caseNoteRepository.findById(saved.caseNoteId())).isEmpty();
    assertThat(caseEventRepository.findByApplicationIdOrderByEventInstantDesc(saved.applicationId())).isEmpty();
  }

  private record SavedCaseNote(UUID applicationId, UUID caseNoteId) {
  }

  @Test
  void findByApplicationIdOrderByEventInstantDesc_returnsThisApplicationsEventsNewestFirst() {
    transactionTemplate.executeWithoutResult(status -> {
      var licenceSchedule = persistLicenceSchedule();
      var application = persistApplication(licenceSchedule);
      var otherApplication = persistApplication(licenceSchedule);
      var older = saveEvent(application.getId(), Instant.parse("2026-09-01T09:00:00Z"));
      var newer = saveEvent(application.getId(), Instant.parse("2026-09-02T09:00:00Z"));
      saveEvent(otherApplication.getId(), Instant.parse("2026-09-03T09:00:00Z"));
      em.flush();
      em.clear();

      assertThat(caseEventRepository.findByApplicationIdOrderByEventInstantDesc(application.getId()))
          .extracting(CaseEvent::getId)
          .containsExactly(newer.getId(), older.getId());

      status.setRollbackOnly();
    });
  }

  @ParameterizedTest
  @MethodSource("payloads")
  void payload_isSavedAsJsonAndReadBackAsTheSameType(CaseEventType eventType, CaseEventPayload payload) {
    transactionTemplate.executeWithoutResult(status -> {
      var application = persistApplication(persistLicenceSchedule());
      caseEventRepository.save(new CaseEvent(
          application.getId(),
          eventType,
          Instant.parse("2026-09-01T09:00:00Z"),
          CaseEventUser.system(),
          payload
      ));
      em.flush();
      em.clear();

      assertThat(caseEventRepository.findByApplicationIdOrderByEventInstantDesc(application.getId()))
          .extracting(CaseEvent::getPayload)
          .containsExactly(payload);

      status.setRollbackOnly();
    });
  }

  @Test
  void payload_whenStoredRowIsMissingAField_thenStillLoads() {
    transactionTemplate.executeWithoutResult(status -> {
      var application = persistApplication(persistLicenceSchedule());
      caseEventRepository.save(new CaseEvent(
          application.getId(),
          CaseEventType.CASE_NOTE_ADDED,
          Instant.parse("2026-09-01T09:00:00Z"),
          CaseEventUser.system(),
          new CaseNotePayload(null)
      ));
      em.flush();
      em.clear();

      assertThat(caseEventRepository.findByApplicationIdOrderByEventInstantDesc(application.getId()))
          .extracting(CaseEvent::getPayload)
          .containsExactly(new CaseNotePayload(null));

      status.setRollbackOnly();
    });
  }

  @Test
  void payload_whenStoredRowHasAnUnknownField_thenStillLoads() {
    var caseNoteId = UUID.randomUUID();

    transactionTemplate.executeWithoutResult(status -> {
      var application = persistApplication(persistLicenceSchedule());
      insertRawPayload(
          application.getId(),
          "{\"type\": \"case-note\", \"caseNoteId\": \"%s\", \"removedField\": \"x\"}".formatted(caseNoteId)
      );

      assertThat(caseEventRepository.findByApplicationIdOrderByEventInstantDesc(application.getId()))
          .extracting(CaseEvent::getPayload)
          .containsExactly(new CaseNotePayload(caseNoteId));

      status.setRollbackOnly();
    });
  }

  private LicenceSchedule persistLicenceSchedule() {
    var licence = LicenceTestUtil.builder()
        .withId(LICENCE_ID)
        .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
        .withLicenceReference("P " + LICENCE_ID)
        .build();
    em.persist(licence);

    var licenceSchedule = LicenceScheduleTestUtil.createLicenceSchedule(null, licence);
    em.persist(licenceSchedule);
    return licenceSchedule;
  }

  private ScheduleWorkProgrammeApplication persistApplication(LicenceSchedule licenceSchedule) {
    var application = new ScheduleWorkProgrammeApplication();
    application.setLicenceSchedule(licenceSchedule);
    return scheduleWorkProgrammeApplicationRepository.saveAndFlush(application);
  }

  private CaseEvent saveEvent(UUID applicationId, Instant eventInstant) {
    return caseEventRepository.save(new CaseEvent(
        applicationId,
        CaseEventType.CASE_NOTE_ADDED,
        eventInstant,
        CaseEventUser.system(),
        null
    ));
  }

  private static Stream<Arguments> payloads() {
    return Stream.of(
        Arguments.of(CaseEventType.CASE_NOTE_ADDED, new CaseNotePayload(UUID.randomUUID())),
        Arguments.of(CaseEventType.STEWARD_ALLOCATED, new CaseAllocationPayload(123L)),
        Arguments.of(CaseEventType.APPLICATION_UPDATE_REQUESTED, new CaseUpdateRequestPayload(LocalDate.of(2026, Month.DECEMBER, 1), "Further information is needed")),
        Arguments.of(CaseEventType.APPLICATION_UPDATE_SUBMITTED, new CaseUpdateSubmittedPayload(2)),
        Arguments.of(CaseEventType.DSP_UPLOADED, new DspUploadedPayload(List.of(UUID.randomUUID(), UUID.randomUUID()))),
        Arguments.of(CaseEventType.DSP_CORRECTED, new DspCorrectedPayload(List.of(UUID.randomUUID()), List.of(UUID.randomUUID()))),
        Arguments.of(CaseEventType.DSP_ADDENDUM_UPLOADED, new DspAddendumUploadedPayload(List.of(UUID.randomUUID())))
    );
  }

  private void insertRawPayload(UUID applicationId, String json) {
    em.createNativeQuery(
            "INSERT INTO lms.case_events (id, application_id, event_type, event_instant, is_system, payload) "
                + "VALUES (:id, :applicationId, 'CASE_NOTE_ADDED', now(), TRUE, CAST(:payload AS jsonb))")
        .setParameter("id", UUID.randomUUID())
        .setParameter("applicationId", applicationId)
        .setParameter("payload", json)
        .executeUpdate();
    em.clear();
  }
}
