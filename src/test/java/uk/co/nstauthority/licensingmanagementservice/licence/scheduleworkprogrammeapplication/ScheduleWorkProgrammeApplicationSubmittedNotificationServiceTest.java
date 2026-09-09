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
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamQueryService;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamRoleTestUtil;

@ExtendWith(MockitoExtension.class)
class ScheduleWorkProgrammeApplicationSubmittedNotificationServiceTest {

  private static final String LOOKUP_PURPOSE = "Notify case managers of new schedule amendment application";
  private static final String NOTIFY_TEMPLATE_ID = "0d07824b-5b09-4ab9-9999-bbdee063d46a";

  private static final Template TEMPLATE = new Template(
      NOTIFY_TEMPLATE_ID,
      TemplateType.EMAIL,
      Set.of(),
      Template.VerificationStatus.CONFIRMED_NOTIFY_TEMPLATE
  );

  @Mock
  private EmailService emailService;

  @Mock
  private TeamQueryService teamQueryService;

  @Mock
  private EnergyPortalUserService energyPortalUserService;

  @InjectMocks
  private ScheduleWorkProgrammeApplicationSubmittedNotificationService scheduleWorkProgrammeApplicationSubmittedNotificationService;

  @Captor
  private ArgumentCaptor<MergedTemplate> mergedTemplateCaptor;

  private ScheduleWorkProgrammeApplicationDetail applicationDetail;

  @BeforeEach
  void setUp() {
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));

    applicationDetail = buildApplicationDetail(LicenceType.SEAWARD_PRODUCTION);
  }

  @AfterEach
  void tearDown() {
    RequestContextHolder.resetRequestAttributes();
  }

  @Test
  void sendNewApplicationSubmittedEmails_whenCaseManagersExist_sendsEmailToEachCaseManager() {
    setupEmailTemplateMock();

    var teamRole = TeamRoleTestUtil.newBuilder().withWuaId(100L).withRole(Role.CASE_MANAGER_OFFSHORE).build();

    var caseManager = new EnergyPortalUserJson(100L, null, "Jane", "Doe", "jane.doe@test.com", null, true, null, false);

    when(teamQueryService.getAllTeamRolesWithRoles(Set.of(Role.CASE_MANAGER_OFFSHORE)))
        .thenReturn(List.of(teamRole));
    when(energyPortalUserService.findByWuaIds(List.of(WebUserAccountId.from(100L)), LOOKUP_PURPOSE))
        .thenReturn(List.of(caseManager));

    scheduleWorkProgrammeApplicationSubmittedNotificationService.sendNewApplicationSubmittedEmails(applicationDetail);

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
  void sendNewApplicationSubmittedEmails_whenLicenceTypeHasNoCaseManagerRole_returnsEarly() {
    var unmappedApplicationDetail = buildApplicationDetail(LicenceType.GAS_STORAGE);

    scheduleWorkProgrammeApplicationSubmittedNotificationService.sendNewApplicationSubmittedEmails(unmappedApplicationDetail);

    verifyNoInteractions(emailService, teamQueryService, energyPortalUserService);
  }

  @Test
  void sendNewApplicationSubmittedEmails_whenNoCaseManagersFound_returnsEarly() {
    when(teamQueryService.getAllTeamRolesWithRoles(Set.of(Role.CASE_MANAGER_OFFSHORE)))
        .thenReturn(List.of());
    when(energyPortalUserService.findByWuaIds(List.of(), LOOKUP_PURPOSE))
        .thenReturn(List.of());

    scheduleWorkProgrammeApplicationSubmittedNotificationService.sendNewApplicationSubmittedEmails(applicationDetail);

    verifyNoInteractions(emailService);
  }

  @Test
  void sendNewApplicationSubmittedEmails_whenSendFailsForOneRecipient_continuesToNextRecipient() {
    setupEmailTemplateMock();

    var teamRole1 = TeamRoleTestUtil.newBuilder().withWuaId(100L).withRole(Role.CASE_MANAGER_OFFSHORE).build();
    var teamRole2 = TeamRoleTestUtil.newBuilder().withWuaId(200L).withRole(Role.CASE_MANAGER_OFFSHORE).build();

    var caseManager1 = new EnergyPortalUserJson(100L, null, "Jane", "Doe", "jane.doe@test.com", null, true, null, false);
    var caseManager2 = new EnergyPortalUserJson(200L, null, "John", "Smith", "john.smith@test.com", null, true, null, false);

    when(teamQueryService.getAllTeamRolesWithRoles(Set.of(Role.CASE_MANAGER_OFFSHORE)))
        .thenReturn(List.of(teamRole1, teamRole2));
    when(energyPortalUserService.findByWuaIds(
        List.of(WebUserAccountId.from(100L), WebUserAccountId.from(200L)),
        LOOKUP_PURPOSE
    )).thenReturn(List.of(caseManager1, caseManager2));

    when(emailService.sendEmail(
        any(MergedTemplate.class),
        argThat(recipient -> "jane.doe@test.com".equals(recipient.getEmailAddress())),
        any(DomainReference.class)
    )).thenThrow(new RuntimeException("Notify unavailable"));

    scheduleWorkProgrammeApplicationSubmittedNotificationService.sendNewApplicationSubmittedEmails(applicationDetail);

    verify(emailService).sendEmail(
        any(MergedTemplate.class),
        argThat(recipient -> "john.smith@test.com".equals(recipient.getEmailAddress())),
        any(DomainReference.class)
    );
  }

  private void setupEmailTemplateMock() {
    when(emailService.getTemplate(GovukNotifyTemplate.NEW_SCHEDULE_AMENDMENT_APPLICATION_SUBMITTED_V1))
        .thenReturn(MergedTemplate.builder(TEMPLATE));
  }

  private ScheduleWorkProgrammeApplicationDetail buildApplicationDetail(LicenceType licenceType) {
    var licence = LicenceTestUtil.builder()
        .withLicenceType(licenceType)
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
