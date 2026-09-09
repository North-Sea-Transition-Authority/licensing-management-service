package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import uk.co.fivium.digitalnotificationlibrary.core.notification.DomainReference;
import uk.co.fivium.digitalnotificationlibrary.core.notification.MailMergeField;
import uk.co.fivium.digitalnotificationlibrary.core.notification.MergedTemplate;
import uk.co.fivium.digitalnotificationlibrary.core.notification.Template;
import uk.co.fivium.digitalnotificationlibrary.core.notification.TemplateType;
import uk.co.nstauthority.licensingmanagementservice.email.EmailService;
import uk.co.nstauthority.licensingmanagementservice.email.GovukNotifyTemplate;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.EnergyPortalUserJson;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.EnergyPortalUserService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.WebUserAccountId;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overview.ScheduleWorkProgrammeApplicationOverviewController;
import uk.co.nstauthority.licensingmanagementservice.mvc.LmsAbsoluteUrlUtil;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@ExtendWith(MockitoExtension.class)
class StewardAssignedNotificationServiceTest {

  private static final String LOOKUP_PURPOSE = "Notify steward of case assignment";
  private static final long STEWARD_WUA_ID = 100L;
  private static final String NOTIFY_TEMPLATE_ID = "00000000-0000-0000-0000-000000000000";

  private static final Template TEMPLATE = new Template(
      NOTIFY_TEMPLATE_ID,
      TemplateType.EMAIL,
      Set.of(),
      Template.VerificationStatus.CONFIRMED_NOTIFY_TEMPLATE
  );

  @Mock
  private EmailService emailService;

  @Mock
  private EnergyPortalUserService energyPortalUserService;

  @InjectMocks
  private StewardAssignedNotificationService stewardAssignedNotificationService;

  @Captor
  private ArgumentCaptor<MergedTemplate> mergedTemplateCaptor;

  private ScheduleWorkProgrammeApplicationDetail applicationDetail;

  @BeforeEach
  void setUp() {
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));

    applicationDetail = buildApplicationDetail();
  }

  @AfterEach
  void tearDown() {
    RequestContextHolder.resetRequestAttributes();
  }

  @Test
  void sendCaseAssignedEmail_whenStewardExists_sendsEmail() {
    setupEmailTemplateMock();

    var steward = new EnergyPortalUserJson(STEWARD_WUA_ID, null, "Jane", "Doe", "jane.doe@test.com", null, true, null, false);

    when(energyPortalUserService.findByWuaIds(List.of(WebUserAccountId.from(STEWARD_WUA_ID)), LOOKUP_PURPOSE))
        .thenReturn(List.of(steward));

    stewardAssignedNotificationService.sendCaseAssignedEmail(applicationDetail, STEWARD_WUA_ID);

    verify(emailService).sendEmail(
        mergedTemplateCaptor.capture(),
        argThat(recipient -> "jane.doe@test.com".equals(recipient.getEmailAddress())),
        any(DomainReference.class)
    );

    var expectedOverviewUrl = ReverseRouter.route(
        on(ScheduleWorkProgrammeApplicationOverviewController.class).renderOverview(applicationDetail.getId(), null, null)
    );

    var email = mergedTemplateCaptor.getValue();
    assertThat(email.getTemplate().notifyTemplateId()).isEqualTo(NOTIFY_TEMPLATE_ID);
    assertThat(email.getMailMergeFields())
        .extracting(MailMergeField::name, MailMergeField::value)
        .containsExactlyInAnyOrder(
            tuple("APPLICATION_TYPE", ApplicationType.SCHEDULE_AMENDMENT_APPLICATION.getDisplayName()),
            tuple("APPLICATION_REFERENCE", applicationDetail.getScheduleWorkProgrammeApplication().getApplicationReference()),
            tuple("LICENCE_REFERENCE", applicationDetail.getLicence().getLicenceReference()),
            tuple("APPLICATION_LINK", LmsAbsoluteUrlUtil.getAbsoluteUrl(expectedOverviewUrl)),
            tuple("USER_NAME", "Jane Doe")
        );
  }

  @Test
  void sendCaseAssignedEmail_whenNoUserFound_returnsEarly() {
    when(energyPortalUserService.findByWuaIds(List.of(WebUserAccountId.from(STEWARD_WUA_ID)), LOOKUP_PURPOSE))
        .thenReturn(List.of());

    stewardAssignedNotificationService.sendCaseAssignedEmail(applicationDetail, STEWARD_WUA_ID);

    verifyNoInteractions(emailService);
  }

  @Test
  void sendCaseAssignedEmail_whenSendFails_isCaughtAndLogged() {
    setupEmailTemplateMock();

    var steward = new EnergyPortalUserJson(STEWARD_WUA_ID, null, "Jane", "Doe", "jane.doe@test.com", null, true, null, false);

    when(energyPortalUserService.findByWuaIds(List.of(WebUserAccountId.from(STEWARD_WUA_ID)), LOOKUP_PURPOSE))
        .thenReturn(List.of(steward));
    when(emailService.sendEmail(any(MergedTemplate.class), any(), any(DomainReference.class)))
        .thenThrow(new RuntimeException("Notify unavailable"));

    stewardAssignedNotificationService.sendCaseAssignedEmail(applicationDetail, STEWARD_WUA_ID);

    verify(emailService).sendEmail(any(MergedTemplate.class), any(), any(DomainReference.class));
  }

  private void setupEmailTemplateMock() {
    when(emailService.getTemplate(GovukNotifyTemplate.STEWARD_ASSIGNED_TO_APPLICATION_V1))
        .thenReturn(MergedTemplate.builder(TEMPLATE));
  }

  private ScheduleWorkProgrammeApplicationDetail buildApplicationDetail() {
    var licence = LicenceTestUtil.builder()
        .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
        .withLicenceReference("P1234")
        .build();
    var licenceSchedule = LicenceScheduleTestUtil.createLicenceSchedule(licence);
    var licenceScheduleDetail = LicenceScheduleTestUtil.createLicenceScheduleDetail(licenceSchedule);
    var application = ScheduleWorkProgrammeApplicationDetailTestUtil.createScheduleWorkProgrammeApplication(licenceScheduleDetail);

    return ScheduleWorkProgrammeApplicationDetailTestUtil.builder()
        .withId(UUID.randomUUID())
        .withScheduleWorkProgrammeApplication(application)
        .withApplicationReference("LMS/EAA/2026/1")
        .build();
  }
}
