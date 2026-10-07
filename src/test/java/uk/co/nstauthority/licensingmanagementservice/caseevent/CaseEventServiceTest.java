package uk.co.nstauthority.licensingmanagementservice.caseevent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.audit.AuditRevisionUtil;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.authentication.UserDetailService;
import uk.co.nstauthority.licensingmanagementservice.caseevent.payload.CaseNotePayload;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.EnergyPortalUserService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.WebUserAccountId;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationType;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplication;
import uk.co.nstauthority.licensingmanagementservice.util.EnergyPortalUserTestUtil;

@ExtendWith(MockitoExtension.class)
class CaseEventServiceTest {

  private static final Instant NOW = Instant.parse("2026-09-24T10:15:30Z");
  private static final UUID APPLICATION_ID = UUID.randomUUID();
  private static final CaseNotePayload PAYLOAD = new CaseNotePayload(UUID.randomUUID());

  @Mock
  private CaseEventRepository caseEventRepository;

  @Mock
  private UserDetailService userDetailService;

  @Mock
  private EnergyPortalUserService energyPortalUserService;

  @Captor
  private ArgumentCaptor<CaseEvent> caseEventCaptor;

  private CaseEventService caseEventService;

  private ScheduleWorkProgrammeApplication application;

  @BeforeEach
  void setUp() {
    caseEventService = new CaseEventService(
        caseEventRepository,
        userDetailService,
        energyPortalUserService,
        Clock.fixed(NOW, ZoneId.of("UTC"))
    );

    application = new ScheduleWorkProgrammeApplication();
    application.setId(APPLICATION_ID);
  }

  @Test
  void recordCaseEvent_whenUserLoggedIn_thenRecordsUserAndProxyUser() {
    var user = ServiceUserDetailTestUtil.newBuilder()
        .withWuaId(10L)
        .withProxyWuaId(20L)
        .build();
    when(userDetailService.findUserDetail()).thenReturn(Optional.of(user));

    caseEventService.recordCaseEvent(CaseEventType.CASE_NOTE_ADDED, application, PAYLOAD);

    verify(caseEventRepository).save(caseEventCaptor.capture());
    assertThat(caseEventCaptor.getValue())
        .usingRecursiveComparison()
        .isEqualTo(expectedCaseEvent(new CaseEventUser(10L, 20L, false)));
  }

  @Test
  void recordCaseEvent_whenNoUserLoggedInButFallbackAuditUserSet_thenRecordsFallbackUser() {
    var fallbackUser = ServiceUserDetailTestUtil.newBuilder()
        .withWuaId(30L)
        .buildWithoutProxy();
    when(userDetailService.findUserDetail()).thenReturn(Optional.empty());

    AuditRevisionUtil.withFallbackAuditUser(
        fallbackUser,
        () -> caseEventService.recordCaseEvent(CaseEventType.CASE_NOTE_ADDED, application, PAYLOAD)
    );

    verify(caseEventRepository).save(caseEventCaptor.capture());
    assertThat(caseEventCaptor.getValue())
        .usingRecursiveComparison()
        .isEqualTo(expectedCaseEvent(new CaseEventUser(30L, null, false)));
  }

  @Test
  void recordCaseEvent_whenNoUserAtAll_thenRecordsSystemEvent() {
    when(userDetailService.findUserDetail()).thenReturn(Optional.empty());

    caseEventService.recordCaseEvent(CaseEventType.CASE_NOTE_ADDED, application, PAYLOAD);

    verify(caseEventRepository).save(caseEventCaptor.capture());
    assertThat(caseEventCaptor.getValue())
        .usingRecursiveComparison()
        .isEqualTo(expectedCaseEvent(new CaseEventUser(null, null, true)));
  }

  @Test
  void getCaseEventViews_whenNoEvents_thenReturnsNoViews() {
    when(caseEventRepository.findByApplicationTypeAndApplicationIdOrderByEventInstantDesc(
        ApplicationType.SCHEDULE_AMENDMENT_APPLICATION, APPLICATION_ID))
        .thenReturn(List.of());

    var result = caseEventService.getCaseEventViews(application);

    assertThat(result).isEmpty();
    verifyNoInteractions(energyPortalUserService);
  }

  @Test
  void getCaseEventViews_whenEvents_thenOneViewPerEventShowingProxyOrUserOrSystem() {
    var proxyEventInstant = Instant.parse("2026-09-24T12:00:00Z");
    var userEventInstant = Instant.parse("2026-09-23T12:00:00Z");
    var systemEventInstant = Instant.parse("2026-09-22T12:00:00Z");

    var proxyEvent = new CaseEvent(ApplicationType.SCHEDULE_AMENDMENT_APPLICATION, APPLICATION_ID,
        CaseEventType.APPLICATION_SUBMITTED, proxyEventInstant, new CaseEventUser(10L, 20L, false), null);
    var userEvent = new CaseEvent(ApplicationType.SCHEDULE_AMENDMENT_APPLICATION, APPLICATION_ID,
        CaseEventType.APPLICATION_SUBMITTED, userEventInstant, new CaseEventUser(10L, null, false), null);
    var systemEvent = new CaseEvent(ApplicationType.SCHEDULE_AMENDMENT_APPLICATION, APPLICATION_ID,
        CaseEventType.APPLICATION_SUBMITTED, systemEventInstant, CaseEventUser.system(), null);

    when(caseEventRepository.findByApplicationTypeAndApplicationIdOrderByEventInstantDesc(
        ApplicationType.SCHEDULE_AMENDMENT_APPLICATION, APPLICATION_ID))
        .thenReturn(List.of(proxyEvent, userEvent, systemEvent));

    var proxyUser = EnergyPortalUserTestUtil.newBuilder()
        .withWebUserAccountId(20L)
        .withForename("Proxy")
        .withSurname("User")
        .withEmailAddress("proxy.user@example.com")
        .buildJson();
    var realUser = EnergyPortalUserTestUtil.newBuilder()
        .withWebUserAccountId(10L)
        .withForename("Real")
        .withSurname("User")
        .withEmailAddress("real.user@example.com")
        .buildJson();
    when(energyPortalUserService.getEnergyPortalUserMap(
        List.of(WebUserAccountId.from(20L), WebUserAccountId.from(10L)),
        CaseEventService.CASE_EVENT_USER_PURPOSE))
        .thenReturn(Map.of(
            WebUserAccountId.from(20L), proxyUser,
            WebUserAccountId.from(10L), realUser
        ));

    var result = caseEventService.getCaseEventViews(application);

    assertThat(result).containsExactly(
        new CaseEventView(
            CaseEventType.APPLICATION_SUBMITTED, "Proxy User (proxy.user@example.com)", proxyEventInstant),
        new CaseEventView(
            CaseEventType.APPLICATION_SUBMITTED, "Real User (real.user@example.com)", userEventInstant),
        new CaseEventView(
            CaseEventType.APPLICATION_SUBMITTED, CaseEventService.SYSTEM_USER_DISPLAY_NAME, systemEventInstant)
    );
  }

  private CaseEvent expectedCaseEvent(CaseEventUser user) {
    return new CaseEvent(
        ApplicationType.SCHEDULE_AMENDMENT_APPLICATION,
        APPLICATION_ID,
        CaseEventType.CASE_NOTE_ADDED,
        NOW,
        user,
        PAYLOAD
    );
  }
}
