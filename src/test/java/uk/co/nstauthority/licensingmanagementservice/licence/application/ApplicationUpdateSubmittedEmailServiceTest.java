package uk.co.nstauthority.licensingmanagementservice.licence.application;

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
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.overview.LicenceContinuationApplicationOverviewController;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overview.ScheduleWorkProgrammeApplicationOverviewController;
import uk.co.nstauthority.licensingmanagementservice.mvc.LmsAbsoluteUrlUtil;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamQueryService;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamRoleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.workarea.workareaitemview.WorkAreaDataItemType;

@ExtendWith(MockitoExtension.class)
class ApplicationUpdateSubmittedEmailServiceTest {

  private static final String LOOKUP_PURPOSE = "Notify case managers of a submitted application update";
  private static final String NOTIFY_TEMPLATE_ID = "079d1ea5-2c3e-4cc7-86b3-42c8843bb1d4";
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
  private ApplicationUpdateSubmittedEmailService service;

  @Captor
  private ArgumentCaptor<MergedTemplate> mergedTemplateCaptor;

  @Captor
  private ArgumentCaptor<DomainReference> domainReferenceCaptor;

  @BeforeEach
  void setUp() {
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
  }

  @AfterEach
  void tearDown() {
    RequestContextHolder.resetRequestAttributes();
  }

  @Test
  void sendUpdateSubmittedEmails_forScheduleApplication_sendsEmailToEachCaseManager() {
    var detail = scheduleDetail(LicenceType.SEAWARD_PRODUCTION);
    givenCaseManagerExists();

    service.sendUpdateSubmittedEmails(detail);

    verify(emailService).sendEmail(
        mergedTemplateCaptor.capture(),
        argThat(recipient -> "jane.doe@test.com".equals(recipient.getEmailAddress())),
        domainReferenceCaptor.capture()
    );

    var expectedUrl = ReverseRouter.route(
        on(ScheduleWorkProgrammeApplicationOverviewController.class).renderOverview(detail.getId(), null, null, null));
    var email = mergedTemplateCaptor.getValue();
    assertThat(email.getTemplate().notifyTemplateId()).isEqualTo(NOTIFY_TEMPLATE_ID);
    assertThat(email.getMailMergeFields())
        .extracting(MailMergeField::name, MailMergeField::value)
        .containsExactlyInAnyOrder(
            tuple("APPLICATION_TYPE", ApplicationType.SCHEDULE_AMENDMENT_APPLICATION.getDisplayName().toLowerCase()),
            tuple("APPLICATION_REFERENCE", "LMS/EAA/2024/5/2"),
            tuple("LICENCE_REFERENCE", detail.getLicence().getLicenceReference()),
            tuple("APPLICATION_LINK", LmsAbsoluteUrlUtil.getAbsoluteUrl(expectedUrl)),
            tuple("USER_NAME", "Jane Doe")
        );
    assertThat(domainReferenceCaptor.getValue())
        .extracting(DomainReference::getDomainId, DomainReference::getDomainType)
        .containsExactly(
            detail.getScheduleWorkProgrammeApplication().getId().toString(),
            WorkAreaDataItemType.SCHEDULE_WORK_PROGRAMME_APPLICATION.name()
        );
  }

  @Test
  void sendUpdateSubmittedEmails_forContinuationApplication_linksToContinuationOverview() {
    var detail = continuationDetail(LicenceType.SEAWARD_PRODUCTION);
    givenCaseManagerExists();

    service.sendUpdateSubmittedEmails(detail);

    verify(emailService).sendEmail(mergedTemplateCaptor.capture(), any(), domainReferenceCaptor.capture());

    var expectedUrl = ReverseRouter.route(
        on(LicenceContinuationApplicationOverviewController.class).renderOverview(detail.getId(), null, null, null));
    assertThat(mergedTemplateCaptor.getValue().getMailMergeFields())
        .extracting(MailMergeField::name, MailMergeField::value)
        .contains(
            tuple("APPLICATION_TYPE", ApplicationType.CONTINUATION_APPLICATION.getDisplayName().toLowerCase()),
            tuple("APPLICATION_LINK", LmsAbsoluteUrlUtil.getAbsoluteUrl(expectedUrl))
        );
    assertThat(domainReferenceCaptor.getValue().getDomainType())
        .isEqualTo(WorkAreaDataItemType.LICENCE_CONTINUATION_APPLICATION.name());
  }

  @Test
  void sendUpdateSubmittedEmails_whenLicenceTypeHasNoCaseManagerRole_sendsNothing() {
    service.sendUpdateSubmittedEmails(scheduleDetail(LicenceType.GAS_STORAGE));

    verifyNoInteractions(emailService, teamQueryService, energyPortalUserService);
  }

  @Test
  void sendUpdateSubmittedEmails_whenNoCaseManagersFound_sendsNothing() {
    when(teamQueryService.getAllTeamRolesWithRoles(Set.of(Role.CASE_MANAGER_OFFSHORE))).thenReturn(List.of());
    when(energyPortalUserService.findByWuaIds(List.of(), LOOKUP_PURPOSE)).thenReturn(List.of());

    service.sendUpdateSubmittedEmails(scheduleDetail(LicenceType.SEAWARD_PRODUCTION));

    verifyNoInteractions(emailService);
  }

  @Test
  void sendUpdateSubmittedEmails_whenStewardAssigned_alsoSendsEmailToSteward() {
    var detail = scheduleDetail(LicenceType.SEAWARD_PRODUCTION);
    detail.getScheduleWorkProgrammeApplication().setStewardWuaId(300L);
    when(emailService.getTemplate(GovukNotifyTemplate.APPLICATION_UPDATE_SUBMITTED_V1))
        .thenReturn(MergedTemplate.builder(TEMPLATE));
    var teamRole = TeamRoleTestUtil.newBuilder().withWuaId(100L).withRole(Role.CASE_MANAGER_OFFSHORE).build();
    var caseManager = new EnergyPortalUserJson(100L, null, "Jane", "Doe", "jane.doe@test.com", null, true, null, false);
    var steward = new EnergyPortalUserJson(300L, null, "Sam", "Steward", "sam.steward@test.com", null, true, null, false);
    when(teamQueryService.getAllTeamRolesWithRoles(Set.of(Role.CASE_MANAGER_OFFSHORE))).thenReturn(List.of(teamRole));
    when(energyPortalUserService.findByWuaIds(
        List.of(WebUserAccountId.from(100L), WebUserAccountId.from(300L)), LOOKUP_PURPOSE))
        .thenReturn(List.of(caseManager, steward));

    service.sendUpdateSubmittedEmails(detail);

    verify(emailService).sendEmail(
        any(MergedTemplate.class),
        argThat(recipient -> "jane.doe@test.com".equals(recipient.getEmailAddress())),
        any(DomainReference.class)
    );
    verify(emailService).sendEmail(
        mergedTemplateCaptor.capture(),
        argThat(recipient -> "sam.steward@test.com".equals(recipient.getEmailAddress())),
        any(DomainReference.class)
    );
    assertThat(mergedTemplateCaptor.getValue().getMailMergeFields())
        .extracting(MailMergeField::name, MailMergeField::value)
        .contains(tuple("USER_NAME", "Sam Steward"));
  }

  @Test
  void sendUpdateSubmittedEmails_whenSendFailsForOneRecipient_continuesToNextRecipient() {
    var detail = scheduleDetail(LicenceType.SEAWARD_PRODUCTION);
    when(emailService.getTemplate(GovukNotifyTemplate.APPLICATION_UPDATE_SUBMITTED_V1))
        .thenReturn(MergedTemplate.builder(TEMPLATE));
    var teamRole1 = TeamRoleTestUtil.newBuilder().withWuaId(100L).withRole(Role.CASE_MANAGER_OFFSHORE).build();
    var teamRole2 = TeamRoleTestUtil.newBuilder().withWuaId(200L).withRole(Role.CASE_MANAGER_OFFSHORE).build();
    var caseManager1 = new EnergyPortalUserJson(100L, null, "Jane", "Doe", "jane.doe@test.com", null, true, null, false);
    var caseManager2 = new EnergyPortalUserJson(200L, null, "John", "Smith", "john.smith@test.com", null, true, null, false);
    when(teamQueryService.getAllTeamRolesWithRoles(Set.of(Role.CASE_MANAGER_OFFSHORE)))
        .thenReturn(List.of(teamRole1, teamRole2));
    when(energyPortalUserService.findByWuaIds(
        List.of(WebUserAccountId.from(100L), WebUserAccountId.from(200L)), LOOKUP_PURPOSE))
        .thenReturn(List.of(caseManager1, caseManager2));
    when(emailService.sendEmail(
        any(MergedTemplate.class),
        argThat(recipient -> "jane.doe@test.com".equals(recipient.getEmailAddress())),
        any(DomainReference.class)
    )).thenThrow(new RuntimeException("Notify unavailable"));

    service.sendUpdateSubmittedEmails(detail);

    verify(emailService).sendEmail(
        mergedTemplateCaptor.capture(),
        argThat(recipient -> "john.smith@test.com".equals(recipient.getEmailAddress())),
        any(DomainReference.class)
    );
    assertThat(mergedTemplateCaptor.getValue().getMailMergeFields())
        .extracting(MailMergeField::name, MailMergeField::value)
        .contains(tuple("USER_NAME", "John Smith"));
  }

  private void givenCaseManagerExists() {
    when(emailService.getTemplate(GovukNotifyTemplate.APPLICATION_UPDATE_SUBMITTED_V1))
        .thenReturn(MergedTemplate.builder(TEMPLATE));
    var teamRole = TeamRoleTestUtil.newBuilder().withWuaId(100L).withRole(Role.CASE_MANAGER_OFFSHORE).build();
    var caseManager = new EnergyPortalUserJson(100L, null, "Jane", "Doe", "jane.doe@test.com", null, true, null, false);
    when(teamQueryService.getAllTeamRolesWithRoles(Set.of(Role.CASE_MANAGER_OFFSHORE))).thenReturn(List.of(teamRole));
    when(energyPortalUserService.findByWuaIds(List.of(WebUserAccountId.from(100L)), LOOKUP_PURPOSE))
        .thenReturn(List.of(caseManager));
  }

  private ScheduleWorkProgrammeApplicationDetail scheduleDetail(LicenceType licenceType) {
    var licenceScheduleDetail = LicenceScheduleTestUtil.createLicenceScheduleDetail(
        LicenceScheduleTestUtil.createLicenceSchedule(licenceTestObject(licenceType)));
    var application = ScheduleWorkProgrammeApplicationDetailTestUtil
        .createScheduleWorkProgrammeApplication(licenceScheduleDetail);
    return ScheduleWorkProgrammeApplicationDetailTestUtil.builder()
        .withId(UUID.randomUUID())
        .withScheduleWorkProgrammeApplication(application)
        .withApplicationReference("LMS/EAA/2024/5/2")
        .build();
  }

  private LicenceContinuationApplicationDetail continuationDetail(LicenceType licenceType) {
    var licenceScheduleDetail = LicenceScheduleTestUtil.createLicenceScheduleDetail(
        LicenceScheduleTestUtil.createLicenceSchedule(licenceTestObject(licenceType)));
    return LicenceContinuationApplicationTestUtil.builder()
        .withId(UUID.randomUUID())
        .withLicenceContinuationApplication(
            LicenceContinuationApplicationTestUtil.createLicenceContinuationApplication(licenceScheduleDetail))
        .withApplicationReference("LMS/CA/2024/5/2")
        .build();
  }

  private Licence licenceTestObject(LicenceType licenceType) {
    return LicenceTestUtil.builder()
        .withLicenceType(licenceType)
        .withLicenceReference("P1234")
        .build();
  }
}
