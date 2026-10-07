package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overview;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import static uk.co.nstauthority.licensingmanagementservice.authentication.TestUserProvider.user;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.scheduleworkprogrammeapplication.InvokingUserCanAccessScheduleApplication;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.scheduleworkprogrammeapplication.ScheduleAmendmentApplicationHasStatus;
import uk.co.nstauthority.licensingmanagementservice.caseevent.CaseEventService;
import uk.co.nstauthority.licensingmanagementservice.caseevent.CaseEventType;
import uk.co.nstauthority.licensingmanagementservice.caseevent.CaseEventView;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.application.caseprocessing.DecisionStringToTabConverter;
import uk.co.nstauthority.licensingmanagementservice.licence.application.caseprocessing.OverviewTab;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.reviewandsubmit.LicenceScheduleSummarySectionService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryDataView;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryItem;
import uk.co.nstauthority.licensingmanagementservice.teams.RegulatorRoleService;

@ContextConfiguration(classes = {
    ScheduleWorkProgrammeApplicationOverviewController.class,
    DecisionStringToTabConverter.class
})
class ScheduleWorkProgrammeApplicationOverviewControllerTest extends AbstractControllerTest {

  private static final Long ORGANISATION_USER_WUA_ID = 2L;

  private static final ServiceUserDetail USER = ServiceUserDetailTestUtil.newBuilder()
      .withWuaId(ORGANISATION_USER_WUA_ID)
      .build();

  @MockitoBean
  private ScheduleWorkProgrammeApplicationOverviewService overviewService;

  @MockitoBean
  private LicenceScheduleSummarySectionService licenceScheduleSummarySectionService;

  @MockitoBean
  private RegulatorRoleService regulatorRoleService;

  @MockitoBean
  private CaseEventService caseEventService;

  @Test
  void renderOverview_classAnnotations_presentAndCorrect() {
    assertThat(ScheduleWorkProgrammeApplicationOverviewController.class)
        .hasAnnotation(ScheduleAmendmentApplicationHasStatus.class);
    assertThat(ScheduleWorkProgrammeApplicationOverviewController.class
        .getAnnotation(ScheduleAmendmentApplicationHasStatus.class).value())
        .containsExactlyInAnyOrder(
            ApplicationStatus.SUBMITTED,
            ApplicationStatus.DSP_UPLOADED,
            ApplicationStatus.ISSUE_DECISION);
    assertThat(ScheduleWorkProgrammeApplicationOverviewController.class)
        .hasAnnotation(InvokingUserCanAccessScheduleApplication.class);
  }

  @Test
  void renderOverview_whenSubmitted_displaysApplicationContext() throws Exception {
    var licence = createLicence();
    var applicationDetailId = UUID.randomUUID();
    var submittedDatetime = Instant.parse("2024-03-15T10:30:00Z");
    var licenceSchedule = LicenceScheduleTestUtil.createLicenceSchedule(licence);
    var licenceScheduleDetail = LicenceScheduleTestUtil.createLicenceScheduleDetail(licenceSchedule);
    var swpApp = ScheduleWorkProgrammeApplicationDetailTestUtil.createScheduleWorkProgrammeApplication(licenceScheduleDetail);
    var applicationDetail = ScheduleWorkProgrammeApplicationDetailTestUtil.builder()
        .withId(applicationDetailId)
        .withStatus(ApplicationStatus.SUBMITTED)
        .withSubmittedDatetime(submittedDatetime)
        .withApplicationReference("LMS/EAA/2024/1")
        .withScheduleWorkProgrammeApplication(swpApp)
        .build();

    var applicationContext = new ScheduleWorkProgrammeApplicationContext(
        "LMS/EAA/2024/1",
        "Carbon storage licence - CS1",
        List.of(SummaryDataView.newBuilder()
            .addStringValue("Status", "Submitted")
            .addStringValue("Licence reference", "CS1")
            .addStringValue("Submitted by", "John Smith")
            .addStringValue("Submission date", "15 March 2024")
            .build())
    );

    when(scheduleWorkProgrammeApplicationService.getDetailByIdOrThrow(applicationDetailId))
        .thenReturn(applicationDetail);
    when(applicationAccessService.userHasAccessToApplication(
        eq(applicationDetail),
        anyMap(),
        eq(ORGANISATION_USER_WUA_ID)))
        .thenReturn(true);
    when(overviewService.getApplicationContext(applicationDetail, licence))
        .thenReturn(applicationContext);
    when(licenceScheduleSummarySectionService.getSummarySections(applicationDetail, USER))
        .thenReturn(List.of());
    when(scheduleWorkProgrammeApplicationActionService.getAvailableUserActionItems(applicationDetail, USER))
        .thenReturn(List.of());

    mockMvc.perform(
            get(ReverseRouter.route(on(ScheduleWorkProgrammeApplicationOverviewController.class)
                .renderOverview(applicationDetailId, null, null, null)))
                .with(user(USER))
        )
        .andExpect(status().isOk())
        .andExpect(view().name(
            "lms/licence/scheduleWorkProgrammeApplication/scheduleWorkProgrammeApplicationOverview"))
        .andExpect(model().attribute("applicationContext", applicationContext))
        .andExpect(model().attribute("summarySections", List.of()))
        .andExpect(model().attribute("accordionId", applicationDetailId))
        .andExpect(model().attribute("applicationActions", List.of()))
        .andExpect(model().attribute("availableTabs", List.of(OverviewTab.OVERVIEW)))
        .andExpect(model().attribute("selectedTab", OverviewTab.OVERVIEW));
  }

  @Test
  void renderOverview_whenRegulator_thenCaseEventsTabIsAvailable() throws Exception {
    var applicationDetail = createSubmittedApplicationDetail();
    stubOverviewFor(applicationDetail, regulatorUser);
    when(regulatorRoleService.isRegulator(regulatorUser)).thenReturn(true);

    mockMvc.perform(
            get(ReverseRouter.route(on(ScheduleWorkProgrammeApplicationOverviewController.class)
                .renderOverview(applicationDetail.getId(), null, null, null)))
                .with(user(regulatorUser))
        )
        .andExpect(status().isOk())
        .andExpect(model().attribute("availableTabs", List.of(OverviewTab.OVERVIEW, OverviewTab.CASE_EVENTS)))
        .andExpect(model().attribute("selectedTab", OverviewTab.OVERVIEW))
        .andExpect(model().attributeDoesNotExist("caseEventsSummaryItem"));

    verifyNoInteractions(caseEventService);
  }

  @Test
  void renderOverview_whenRegulatorSelectsCaseEventsTab_thenCaseEventCardsAreShown() throws Exception {
    var applicationDetail = createSubmittedApplicationDetail();
    stubOverviewFor(applicationDetail, regulatorUser);
    when(regulatorRoleService.isRegulator(regulatorUser)).thenReturn(true);

    var caseEventView = new CaseEventView(
        CaseEventType.APPLICATION_SUBMITTED,
        Map.of(),
        "John Smith",
        Instant.parse("2026-09-24T10:15:30Z")
    );
    when(caseEventService.getCaseEventViews(applicationDetail.getScheduleWorkProgrammeApplication()))
        .thenReturn(List.of(caseEventView));

    mockMvc.perform(
            get(ReverseRouter.route(on(ScheduleWorkProgrammeApplicationOverviewController.class)
                .renderOverview(applicationDetail.getId(), null, null, OverviewTab.CASE_EVENTS)))
                .with(user(regulatorUser))
        )
        .andExpect(status().isOk())
        .andExpect(model().attribute("selectedTab", OverviewTab.CASE_EVENTS))
        .andExpect(model().attribute(
            "caseEventsSummaryItem",
            new SummaryItem("Case events", List.of(caseEventView.toSummaryCard()))
        ));
  }

  @Test
  void renderOverview_whenNonRegulatorSelectsCaseEventsTab_thenForbidden() throws Exception {
    var applicationDetail = createSubmittedApplicationDetail();
    when(scheduleWorkProgrammeApplicationService.getDetailByIdOrThrow(applicationDetail.getId()))
        .thenReturn(applicationDetail);
    when(applicationAccessService.userHasAccessToApplication(
        eq(applicationDetail),
        anyMap(),
        eq(ORGANISATION_USER_WUA_ID)))
        .thenReturn(true);
    when(regulatorRoleService.isRegulator(USER)).thenReturn(false);

    mockMvc.perform(
            get(ReverseRouter.route(on(ScheduleWorkProgrammeApplicationOverviewController.class)
                .renderOverview(applicationDetail.getId(), null, null, OverviewTab.CASE_EVENTS)))
                .with(user(USER))
        )
        .andExpect(status().isForbidden());

    verifyNoInteractions(caseEventService);
  }

  @Test
  void renderOverview_whenRegulatorSelectsLetterTab_thenForbidden() throws Exception {
    var applicationDetail = createSubmittedApplicationDetail();
    when(scheduleWorkProgrammeApplicationService.getDetailByIdOrThrow(applicationDetail.getId()))
        .thenReturn(applicationDetail);
    when(applicationAccessService.userHasAccessToApplication(
        eq(applicationDetail),
        anyMap(),
        eq(regulatorUser.wuaId())))
        .thenReturn(true);
    when(regulatorRoleService.isRegulator(regulatorUser)).thenReturn(true);

    mockMvc.perform(
            get(ReverseRouter.route(on(ScheduleWorkProgrammeApplicationOverviewController.class)
                .renderOverview(applicationDetail.getId(), null, null, OverviewTab.LETTER)))
                .with(user(regulatorUser))
        )
        .andExpect(status().isForbidden());

    verifyNoInteractions(caseEventService);
  }

  private ScheduleWorkProgrammeApplicationDetail createSubmittedApplicationDetail() {
    var licenceSchedule = LicenceScheduleTestUtil.createLicenceSchedule(createLicence());
    var licenceScheduleDetail = LicenceScheduleTestUtil.createLicenceScheduleDetail(licenceSchedule);
    var swpApp = ScheduleWorkProgrammeApplicationDetailTestUtil.createScheduleWorkProgrammeApplication(licenceScheduleDetail);
    return ScheduleWorkProgrammeApplicationDetailTestUtil.builder()
        .withId(UUID.randomUUID())
        .withStatus(ApplicationStatus.SUBMITTED)
        .withScheduleWorkProgrammeApplication(swpApp)
        .build();
  }

  private void stubOverviewFor(ScheduleWorkProgrammeApplicationDetail applicationDetail, ServiceUserDetail user) {
    when(scheduleWorkProgrammeApplicationService.getDetailByIdOrThrow(applicationDetail.getId()))
        .thenReturn(applicationDetail);
    when(applicationAccessService.userHasAccessToApplication(
        eq(applicationDetail),
        anyMap(),
        eq(user.wuaId())))
        .thenReturn(true);
    when(overviewService.getApplicationContext(applicationDetail, applicationDetail.getLicence()))
        .thenReturn(new ScheduleWorkProgrammeApplicationContext("LMS/EAA/2024/1", "Carbon storage licence - CS1", List.of()));
    when(licenceScheduleSummarySectionService.getSummarySections(applicationDetail, user))
        .thenReturn(List.of());
    when(scheduleWorkProgrammeApplicationActionService.getAvailableUserActionItems(applicationDetail, user))
        .thenReturn(List.of());
  }

  private Licence createLicence() {
    var licence = new Licence();
    licence.setId(1);
    licence.setType(LicenceType.CARBON_STORAGE);
    licence.setLicenceReference("CS1");
    return licence;
  }
}
