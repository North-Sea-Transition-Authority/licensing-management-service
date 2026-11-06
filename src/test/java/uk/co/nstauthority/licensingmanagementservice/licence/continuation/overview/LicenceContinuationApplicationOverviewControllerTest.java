package uk.co.nstauthority.licensingmanagementservice.licence.continuation.overview;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import static uk.co.nstauthority.licensingmanagementservice.authentication.TestUserProvider.user;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.caseevent.CaseEventService;
import uk.co.nstauthority.licensingmanagementservice.caseevent.CaseEventType;
import uk.co.nstauthority.licensingmanagementservice.caseevent.CaseEventView;
import uk.co.nstauthority.licensingmanagementservice.file.FileControllerHelperService;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.application.caseprocessing.OverviewTab;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.decision.ContinuationDecisionSummarySectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.decision.ContinuationLetterFileUsages;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.overview.action.LicenceContinuationActionService;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.reviewandsubmit.ContinuationSummarySectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.tasklist.LicenceContinuationApplicationTaskListService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryDataView;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryItem;
import uk.co.nstauthority.licensingmanagementservice.teams.RegulatorRoleService;

@ContextConfiguration(classes = LicenceContinuationApplicationOverviewController.class)
class LicenceContinuationApplicationOverviewControllerTest extends AbstractControllerTest {

  private static final Long ORGANISATION_USER_WUA_ID = 2L;

  private static final String DRAFT_BANNER_TEXT = "This is a draft application.";
  private static final String INCOMPLETE_SECTIONS_BANNER_TEXT = "some sections are incomplete";
  private static final String MISSING_INFORMATION_BANNER_TITLE = "Missing information";
  private static final String SUBMISSION_BLOCKED_TEXT = "You will not be allowed to submit";

  private static final ServiceUserDetail USER = ServiceUserDetailTestUtil.newBuilder()
      .withWuaId(ORGANISATION_USER_WUA_ID)
      .build();

  @MockitoBean
  private LicenceContinuationApplicationOverviewService overviewService;

  @MockitoBean
  private ContinuationSummarySectionService continuationSummarySectionService;

  @MockitoBean
  private LicenceContinuationActionService licenceContinuationActionService;

  @MockitoBean
  private FileControllerHelperService fileControllerHelperService;

  @MockitoBean
  private ContinuationDecisionSummarySectionService continuationDecisionSummarySectionService;

  @MockitoBean
  private RegulatorRoleService regulatorRoleService;

  @MockitoBean
  private CaseEventService caseEventService;

  @MockitoBean
  private LicenceContinuationApplicationTaskListService taskListService;

  @Test
  void renderOverview_displaysApplicationContextAndSummaries() throws Exception {
    var licence = LicenceTestUtil .builder()
        .withId(1)
        .withLicenceType(LicenceType.CARBON_STORAGE)
        .withLicenceReference("CS1")
        .build();
    var applicationDetailId = UUID.randomUUID();
    var submittedDatetime = Instant.parse("2024-03-15T10:30:00Z");

    var licenceContinuationApp = LicenceContinuationApplicationTestUtil.createLicenceContinuationApplication(
        LicenceScheduleTestUtil.createLicenceScheduleDetail(LicenceScheduleTestUtil.createLicenceSchedule(licence)));
    var applicationDetail = LicenceContinuationApplicationTestUtil.builder()
        .withId(applicationDetailId)
        .withStatus(ApplicationStatus.SUBMITTED)
        .withSubmittedDatetime(submittedDatetime)
        .withApplicationReference("LMS/CA/2024/1")
        .withLicenceContinuationApplication(licenceContinuationApp)
        .build();

    var applicationContext = new LicenceContinuationApplicationContext(
        "LMS/CA/2024/1",
        "Carbon storage licence - CS1",
        List.of(SummaryDataView.newBuilder()
                    .addStringValue("Submitted by", "John Smith")
                    .addStringValue("Submission date", "15 March 2024")
                    .build())
    );

    when(licenceContinuationService.getDetailByIdOrThrow(applicationDetailId))
        .thenReturn(applicationDetail);
    when(applicationAccessService.userHasAccessToApplication(any(), any(), any()))
        .thenReturn(true);
    when(overviewService.getApplicationContext(applicationDetail, licence))
        .thenReturn(applicationContext);
    when(continuationSummarySectionService.getSummarySections(applicationDetail, USER))
        .thenReturn(List.of());
    when(licenceScheduleStateService.getCurrentWorkProgrammeActivitiesViews(any()))
        .thenReturn(List.of());
    when(licenceContinuationActionService.getAvailableUserActionItems(applicationDetail, USER))
        .thenReturn(List.of());
    when(continuationDecisionSummarySectionService.getSummarySection(applicationDetail))
        .thenReturn(Optional.empty());

    mockMvc.perform(
            get(ReverseRouter.route(on(LicenceContinuationApplicationOverviewController.class).renderOverview(applicationDetailId, applicationDetail, null, OverviewTab.OVERVIEW)))
                .with(user(USER))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/continuation/licenceContinuationApplicationOverview"))
        .andExpect(model().attribute("applicationContext", applicationContext))
        .andExpect(model().attribute("summarySections", List.of()))
        .andExpect(model().attribute("workProgrammeActivities", List.of()))
        .andExpect(model().attribute("accordionId", applicationDetailId))
        .andExpect(model().attribute("applicationActions", List.of()))
        .andExpect(model().attribute("letterIssueSummarySection", (Object) null))
        .andExpect(model().attribute("availableTabs", List.of(OverviewTab.OVERVIEW, OverviewTab.LETTER)))
        .andExpect(model().attribute("selectedTab", OverviewTab.OVERVIEW))
        .andExpect(model().attributeExists("controllerUrl"));
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void renderOverview_whenDraft_thenIncompleteSectionsFlagReflectsSubmittability(boolean isSubmittable) throws Exception {
    var applicationDetail = createApplicationDetail(ApplicationStatus.DRAFT);
    var applicationContext = new LicenceContinuationApplicationContext("LMS/CA/2024/1", "Carbon storage licence - CS1", List.of());

    when(licenceContinuationService.getDetailByIdOrThrow(applicationDetail.getId()))
        .thenReturn(applicationDetail);
    when(applicationAccessService.userHasAccessToApplication(
        eq(applicationDetail),
        anyMap(),
        eq(ORGANISATION_USER_WUA_ID)))
        .thenReturn(true);
    when(overviewService.getApplicationContext(applicationDetail, applicationDetail.getLicence()))
        .thenReturn(applicationContext);
    when(continuationSummarySectionService.getSummarySections(applicationDetail, USER))
        .thenReturn(List.of());
    when(licenceScheduleStateService.getCurrentWorkProgrammeActivitiesViews(any()))
        .thenReturn(List.of());
    when(licenceContinuationActionService.getAvailableUserActionItems(applicationDetail, USER))
        .thenReturn(List.of());
    when(continuationDecisionSummarySectionService.getSummarySection(applicationDetail))
        .thenReturn(Optional.empty());
    when(taskListService.isSubmittable(applicationDetail, USER)).thenReturn(isSubmittable);

    mockMvc.perform(
            get(ReverseRouter.route(on(LicenceContinuationApplicationOverviewController.class)
                .renderOverview(applicationDetail.getId(), null, null, OverviewTab.OVERVIEW)))
                .with(user(USER))
        )
        .andExpect(status().isOk())
        .andExpect(model().attribute("isDraft", true))
        .andExpect(model().attribute("hasIncompleteSections", !isSubmittable))
        .andExpect(content().string(containsString(DRAFT_BANNER_TEXT)))
        .andExpect(content().string(isSubmittable
            ? not(containsString(INCOMPLETE_SECTIONS_BANNER_TEXT))
            : containsString(INCOMPLETE_SECTIONS_BANNER_TEXT)))
        .andExpect(content().string(not(containsString(MISSING_INFORMATION_BANNER_TITLE))))
        .andExpect(content().string(not(containsString(SUBMISSION_BLOCKED_TEXT))));
  }

  @Test
  void renderOverview_whenRegulator_thenCaseEventsTabIsAvailable() throws Exception {
    var applicationDetail = createSubmittedApplicationDetail();
    stubOverviewFor(applicationDetail, regulatorUser);
    when(regulatorRoleService.isRegulator(regulatorUser)).thenReturn(true);

    mockMvc.perform(
            get(ReverseRouter.route(on(LicenceContinuationApplicationOverviewController.class)
                .renderOverview(applicationDetail.getId(), null, null, OverviewTab.OVERVIEW)))
                .with(user(regulatorUser))
        )
        .andExpect(status().isOk())
        .andExpect(model().attribute(
            "availableTabs",
            List.of(OverviewTab.OVERVIEW, OverviewTab.LETTER, OverviewTab.CASE_EVENTS)
        ))
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
    when(caseEventService.getCaseEventViews(applicationDetail.getLicenceContinuationApplication()))
        .thenReturn(List.of(caseEventView));

    mockMvc.perform(
            get(ReverseRouter.route(on(LicenceContinuationApplicationOverviewController.class)
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
    when(licenceContinuationService.getDetailByIdOrThrow(applicationDetail.getId()))
        .thenReturn(applicationDetail);
    when(applicationAccessService.userHasAccessToApplication(
        eq(applicationDetail),
        anyMap(),
        eq(ORGANISATION_USER_WUA_ID)))
        .thenReturn(true);
    when(regulatorRoleService.isRegulator(USER)).thenReturn(false);

    mockMvc.perform(
            get(ReverseRouter.route(on(LicenceContinuationApplicationOverviewController.class)
                .renderOverview(applicationDetail.getId(), null, null, OverviewTab.CASE_EVENTS)))
                .with(user(USER))
        )
        .andExpect(status().isForbidden());

    verifyNoInteractions(caseEventService);
  }

  @Test
  void downloadLetter_success() throws Exception {
    var applicationDetailId = UUID.randomUUID();
    var fileId = UUID.randomUUID();
    var applicationDetail = LicenceContinuationApplicationTestUtil.builder()
        .withId(applicationDetailId)
        .withStatus(ApplicationStatus.SUBMITTED)
        .build();

    when(licenceContinuationService.getDetailByIdOrThrow(applicationDetailId))
        .thenReturn(applicationDetail);
    when(applicationAccessService.userHasAccessToApplication(any(), any(), any()))
        .thenReturn(true);

    when(fileControllerHelperService.download(any(UUID.class), any(Supplier.class), any(ServiceUserDetail.class)))
        .thenReturn(ResponseEntity.ok().build());

    mockMvc.perform(
            get(ReverseRouter.route(on(LicenceContinuationApplicationOverviewController.class)
                                        .downloadLetter(applicationDetailId, applicationDetail, fileId, null)))
                .with(user(USER)).with(csrf())
        )
        .andExpect(status().isOk());

    ArgumentCaptor<Supplier> fileUsageSupplierCaptor = ArgumentCaptor.forClass(Supplier.class);

    verify(fileControllerHelperService).download(any(), fileUsageSupplierCaptor.capture(), any());

    assertThat(fileUsageSupplierCaptor.getValue().get())
        .isEqualTo(ContinuationLetterFileUsages.fromApplication(applicationDetail));
  }

  private LicenceContinuationApplicationDetail createSubmittedApplicationDetail() {
    return createApplicationDetail(ApplicationStatus.SUBMITTED);
  }

  private LicenceContinuationApplicationDetail createApplicationDetail(ApplicationStatus status) {
    var licence = LicenceTestUtil.builder()
        .withId(1)
        .withLicenceType(LicenceType.CARBON_STORAGE)
        .withLicenceReference("CS1")
        .build();
    var licenceContinuationApp = LicenceContinuationApplicationTestUtil.createLicenceContinuationApplication(
        LicenceScheduleTestUtil.createLicenceScheduleDetail(LicenceScheduleTestUtil.createLicenceSchedule(licence)));
    return LicenceContinuationApplicationTestUtil.builder()
        .withId(UUID.randomUUID())
        .withStatus(status)
        .withLicenceContinuationApplication(licenceContinuationApp)
        .build();
  }

  private void stubOverviewFor(LicenceContinuationApplicationDetail applicationDetail, ServiceUserDetail user) {
    when(licenceContinuationService.getDetailByIdOrThrow(applicationDetail.getId()))
        .thenReturn(applicationDetail);
    when(applicationAccessService.userHasAccessToApplication(
        eq(applicationDetail),
        anyMap(),
        eq(user.wuaId())))
        .thenReturn(true);
    when(overviewService.getApplicationContext(applicationDetail, applicationDetail.getLicence()))
        .thenReturn(new LicenceContinuationApplicationContext("LMS/CA/2024/1", "Carbon storage licence - CS1", List.of()));
    when(continuationSummarySectionService.getSummarySections(applicationDetail, user))
        .thenReturn(List.of());
    when(licenceContinuationActionService.getAvailableUserActionItems(applicationDetail, user))
        .thenReturn(List.of());
    when(continuationDecisionSummarySectionService.getSummarySection(applicationDetail))
        .thenReturn(Optional.empty());
  }
}