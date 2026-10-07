package uk.co.nstauthority.licensingmanagementservice.caseevent;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
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
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationType;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplication;
import uk.co.nstauthority.licensingmanagementservice.util.IntegrationTest;

@IntegrationTest
class CaseEventIntegrationTest {

  @Autowired
  private CaseEventRepository caseEventRepository;

  @Autowired
  private CaseNoteRepository caseNoteRepository;

  @Autowired
  private CaseNoteService caseNoteService;

  @Autowired
  private TransactionTemplate transactionTemplate;

  @Autowired
  private EntityManager em;

  private ScheduleWorkProgrammeApplication application;

  @BeforeEach
  void setUp() {
    application = new ScheduleWorkProgrammeApplication();
    application.setId(UUID.randomUUID());
  }

  @Test
  void addCaseNote_whenTransactionRollsBack_thenNeitherNoteNorEventIsSaved() {
    var author = ServiceUserDetailTestUtil.newBuilder().build();

    var caseNoteId = transactionTemplate.execute(status -> {
      var caseNote = caseNoteService.addCaseNote(application, "Chased the operator for the missing map", author);
      status.setRollbackOnly();
      return caseNote.getId();
    });

    assertThat(caseNoteRepository.findById(caseNoteId)).isEmpty();
    assertThat(eventsForApplication()).isEmpty();
  }

  @Test
  void findByApplicationTypeAndApplicationIdOrderByEventInstantDesc_returnsThisApplicationsEventsNewestFirst() {
    transactionTemplate.executeWithoutResult(status -> {
      var older = saveEvent(application.getId(), Instant.parse("2026-09-01T09:00:00Z"));
      var newer = saveEvent(application.getId(), Instant.parse("2026-09-02T09:00:00Z"));
      saveEvent(UUID.randomUUID(), Instant.parse("2026-09-03T09:00:00Z"));
      em.flush();
      em.clear();

      assertThat(eventsForApplication())
          .extracting(CaseEvent::getId)
          .containsExactly(newer.getId(), older.getId());

      status.setRollbackOnly();
    });
  }

  @ParameterizedTest
  @MethodSource("payloads")
  void payload_isSavedAsJsonAndReadBackAsTheSameType(CaseEventType eventType, CaseEventPayload payload) {
    transactionTemplate.executeWithoutResult(status -> {
      caseEventRepository.save(new CaseEvent(
          application.getApplicationType(),
          application.getId(),
          eventType,
          Instant.parse("2026-09-01T09:00:00Z"),
          CaseEventUser.system(),
          payload
      ));
      em.flush();
      em.clear();

      assertThat(eventsForApplication())
          .extracting(CaseEvent::getPayload)
          .containsExactly(payload);

      status.setRollbackOnly();
    });
  }

  @Test
  void payload_whenStoredRowIsMissingAField_thenStillLoads() {
    transactionTemplate.executeWithoutResult(status -> {
      caseEventRepository.save(new CaseEvent(
          application.getApplicationType(),
          application.getId(),
          CaseEventType.CASE_NOTE_ADDED,
          Instant.parse("2026-09-01T09:00:00Z"),
          CaseEventUser.system(),
          new CaseNotePayload(null)
      ));
      em.flush();
      em.clear();

      assertThat(eventsForApplication())
          .extracting(CaseEvent::getPayload)
          .containsExactly(new CaseNotePayload(null));

      status.setRollbackOnly();
    });
  }

  @Test
  void payload_whenStoredRowHasAnUnknownField_thenStillLoads() {
    var caseNoteId = UUID.randomUUID();

    transactionTemplate.executeWithoutResult(status -> {
      insertRawPayload("{\"type\": \"case-note\", \"caseNoteId\": \"%s\", \"removedField\": \"x\"}".formatted(caseNoteId));

      assertThat(eventsForApplication())
          .extracting(CaseEvent::getPayload)
          .containsExactly(new CaseNotePayload(caseNoteId));

      status.setRollbackOnly();
    });
  }

  private CaseEvent saveEvent(UUID applicationId, Instant eventInstant) {
    return caseEventRepository.save(new CaseEvent(
        ApplicationType.SCHEDULE_AMENDMENT_APPLICATION,
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
        Arguments.of(CaseEventType.STEWARD_ALLOCATED, new CaseAllocationPayload(123L))
    );
  }

  private void insertRawPayload(String json) {
    em.createNativeQuery(
            "INSERT INTO lms.case_events (id, application_type, application_id, event_type, event_instant, is_system, payload) "
                + "VALUES (:id, :applicationType, :applicationId, 'CASE_NOTE_ADDED', now(), TRUE, CAST(:payload AS jsonb))")
        .setParameter("id", UUID.randomUUID())
        .setParameter("applicationType", application.getApplicationType().name())
        .setParameter("applicationId", application.getId())
        .setParameter("payload", json)
        .executeUpdate();
    em.clear();
  }

  private List<CaseEvent> eventsForApplication() {
    return caseEventRepository.findByApplicationTypeAndApplicationIdOrderByEventInstantDesc(
        application.getApplicationType(),
        application.getId()
    );
  }
}
