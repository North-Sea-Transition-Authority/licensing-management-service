package uk.co.nstauthority.licensingmanagementservice.caseevent.casenote;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.caseevent.CaseEventService;
import uk.co.nstauthority.licensingmanagementservice.caseevent.CaseEventType;
import uk.co.nstauthority.licensingmanagementservice.caseevent.payload.CaseNotePayload;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationType;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplication;

@ExtendWith(MockitoExtension.class)
class CaseNoteServiceTest {

  private static final Instant NOW = Instant.parse("2026-09-24T10:15:30Z");
  private static final UUID CASE_NOTE_ID = UUID.randomUUID();

  @Mock
  private CaseNoteRepository caseNoteRepository;

  @Mock
  private CaseEventService caseEventService;

  @Captor
  private ArgumentCaptor<CaseNote> caseNoteCaptor;

  private CaseNoteService caseNoteService;

  @BeforeEach
  void setUp() {
    caseNoteService = new CaseNoteService(caseNoteRepository, caseEventService, Clock.fixed(NOW, ZoneId.of("UTC")));
  }

  @Test
  void addCaseNote() {
    var application = new ScheduleWorkProgrammeApplication();
    application.setId(UUID.randomUUID());
    var author = ServiceUserDetailTestUtil.newBuilder()
        .withWuaId(10L)
        .withProxyWuaId(20L)
        .build();

    var savedNote = new CaseNote(CASE_NOTE_ID);
    when(caseNoteRepository.save(caseNoteCaptor.capture())).thenReturn(savedNote);

    var result = caseNoteService.addCaseNote(application, "Chased the operator for the missing map", author);

    var expectedNote = new CaseNote(
        ApplicationType.SCHEDULE_AMENDMENT_APPLICATION,
        application.getId(),
        "Chased the operator for the missing map",
        NOW,
        10L,
        20L
    );

    assertThat(caseNoteCaptor.getValue()).usingRecursiveComparison().isEqualTo(expectedNote);
    assertThat(result).isSameAs(savedNote);
    verify(caseEventService).recordCaseEvent(
        CaseEventType.CASE_NOTE_ADDED,
        application,
        new CaseNotePayload(CASE_NOTE_ID)
    );
  }
}
