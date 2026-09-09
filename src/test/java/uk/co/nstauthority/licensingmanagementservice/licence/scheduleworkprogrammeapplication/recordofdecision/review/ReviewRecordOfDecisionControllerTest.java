package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import static uk.co.nstauthority.licensingmanagementservice.authentication.TestUserProvider.user;
import static uk.co.nstauthority.licensingmanagementservice.util.NotificationBannerTestUtil.notificationBanner;
import static uk.co.nstauthority.licensingmanagementservice.util.NotificationBannerTestUtil.notificationBannerDoesNotExist;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.scheduleworkprogrammeapplication.InvokingUserCanAccessScheduleApplication;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.scheduleworkprogrammeapplication.ScheduleAmendmentApplicationHasStatus;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision.RecordOfDecisionTaskListContext;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision.RecordOfDecisionTaskListService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryCard;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryDataView;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryItem;
import uk.co.nstauthority.licensingmanagementservice.summary.SummarySection;
import uk.co.nstauthority.licensingmanagementservice.workarea.WorkAreaController;

@ContextConfiguration(classes = ReviewRecordOfDecisionController.class)
class ReviewRecordOfDecisionControllerTest extends AbstractControllerTest {

  private static final String VIEW_NAME = "lms/licence/scheduleWorkProgrammeApplication/reviewRecordOfDecision";
  private static final String MISSING_INFORMATION = "Missing information";
  private static final String SUBMIT_BUTTON = "Submit";
  private static final String APPLICATION_REFERENCE = "SWP/1";
  private static final Long REGULATOR_WUA_ID = 1L;
  private static final ServiceUserDetail USER = ServiceUserDetailTestUtil.newBuilder()
      .withWuaId(REGULATOR_WUA_ID)
      .build();

  @MockitoBean
  private RecordOfDecisionSummarySectionService recordOfDecisionSummarySectionService;

  @MockitoBean
  private RecordOfDecisionTaskListService recordOfDecisionTaskListService;

  @Test
  void renderReview_classAnnotations_presentAndCorrect() {
    assertThat(ReviewRecordOfDecisionController.class)
        .hasAnnotation(ScheduleAmendmentApplicationHasStatus.class);
    assertThat(ReviewRecordOfDecisionController.class
        .getAnnotation(ScheduleAmendmentApplicationHasStatus.class).value())
        .containsOnly(ApplicationStatus.ISSUE_DECISION);
    assertThat(ReviewRecordOfDecisionController.class)
        .hasAnnotation(InvokingUserCanAccessScheduleApplication.class);
  }

  @Test
  void renderReview_noApplicationAccess_returnsForbidden() throws Exception {
    var applicationDetailId = UUID.randomUUID();
    var applicationDetail = buildApplicationDetail(applicationDetailId);

    when(scheduleWorkProgrammeApplicationService.getDetailByIdOrThrow(applicationDetailId))
        .thenReturn(applicationDetail);
    when(applicationAccessService.userHasAccessToApplication(eq(applicationDetail), anyMap(), eq(REGULATOR_WUA_ID)))
        .thenReturn(false);

    mockMvc.perform(
            get(ReverseRouter.route(on(ReviewRecordOfDecisionController.class)
                .renderReview(applicationDetailId, null, null)))
                .with(user(USER)))
        .andExpect(status().isForbidden());
  }

  @Test
  void renderReview_showsTheRecordedSummary() throws Exception {
    var applicationDetailId = UUID.randomUUID();
    var applicationDetail = buildApplicationDetail(applicationDetailId);

    setupPassingInterceptors(applicationDetail);
    mockSubmittable(applicationDetail, true);
    when(recordOfDecisionSummarySectionService.getSummarySections(
        any(RecordOfDecisionSummaryContext.class), eq(USER)))
        .thenReturn(List.of(summarySection(
            DurationChangeDecisionSummarySectionService.SECTION_NAME, "Initial Term extension duration")));

    mockMvc.perform(
            get(ReverseRouter.route(on(ReviewRecordOfDecisionController.class)
                .renderReview(applicationDetailId, null, null)))
                .with(user(USER)))
        .andExpect(status().isOk())
        .andExpect(view().name(VIEW_NAME))
        .andExpect(model().attribute("pageTitle", ReviewRecordOfDecisionController.PAGE_TITLE))
        .andExpect(model().attributeExists("summarySections", "accordionId", "cancelUrl", "isSubmittable"))
        .andExpect(content().string(containsString(DurationChangeDecisionSummarySectionService.SECTION_NAME)))
        .andExpect(content().string(containsString("Initial Term extension duration")));
  }

  @Test
  void renderReview_whenTasksAreIncomplete_showsTheMissingInformationBanner() throws Exception {
    var applicationDetailId = UUID.randomUUID();
    var applicationDetail = buildApplicationDetail(applicationDetailId);

    setupPassingInterceptors(applicationDetail);
    mockSubmittable(applicationDetail, false);
    when(recordOfDecisionSummarySectionService.getSummarySections(
        any(RecordOfDecisionSummaryContext.class), eq(USER)))
        .thenReturn(List.of());

    mockMvc.perform(
            get(ReverseRouter.route(on(ReviewRecordOfDecisionController.class)
                .renderReview(applicationDetailId, null, null)))
                .with(user(USER)))
        .andExpect(status().isOk())
        .andExpect(model().attribute("isSubmittable", false))
        .andExpect(content().string(containsString(MISSING_INFORMATION)))
        .andExpect(content().string(not(containsString(SUBMIT_BUTTON))));
  }

  @Test
  void renderReview_whenAllTasksAreComplete_doesNotShowTheMissingInformationBanner() throws Exception {
    var applicationDetailId = UUID.randomUUID();
    var applicationDetail = buildApplicationDetail(applicationDetailId);

    setupPassingInterceptors(applicationDetail);
    mockSubmittable(applicationDetail, true);
    when(recordOfDecisionSummarySectionService.getSummarySections(
        any(RecordOfDecisionSummaryContext.class), eq(USER)))
        .thenReturn(List.of());

    mockMvc.perform(
            get(ReverseRouter.route(on(ReviewRecordOfDecisionController.class)
                .renderReview(applicationDetailId, null, null)))
                .with(user(USER)))
        .andExpect(status().isOk())
        .andExpect(model().attribute("isSubmittable", true))
        .andExpect(content().string(not(containsString(MISSING_INFORMATION))))
        .andExpect(content().string(containsString(SUBMIT_BUTTON)));
  }

  @Test
  void submitReview_whenAllTasksAreComplete_redirectsToWorkAreaLeavingTheStatusUnchanged() throws Exception {
    var applicationDetailId = UUID.randomUUID();
    var applicationDetail = buildApplicationDetail(applicationDetailId);

    setupPassingInterceptors(applicationDetail);
    mockSubmittable(applicationDetail, true);

    mockMvc.perform(
            post(ReverseRouter.route(on(ReviewRecordOfDecisionController.class)
                .submitReview(applicationDetailId, null, null, null)))
                .with(user(USER))
                .with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(WorkAreaController.class).getWorkArea(null, null))))
        .andExpect(notificationBanner(NotificationBanner.newSuccessBanner()
            .withHeadingContent(ReviewRecordOfDecisionController.SUBMITTED_BANNER.formatted(APPLICATION_REFERENCE))
            .build()));

    assertThat(applicationDetail.getStatus()).isEqualTo(ApplicationStatus.ISSUE_DECISION);
  }

  @Test
  void submitReview_whenTasksAreIncomplete_redirectsBackAndCompletesNothing() throws Exception {
    var applicationDetailId = UUID.randomUUID();
    var applicationDetail = buildApplicationDetail(applicationDetailId);

    setupPassingInterceptors(applicationDetail);
    mockSubmittable(applicationDetail, false);

    mockMvc.perform(
            post(ReverseRouter.route(on(ReviewRecordOfDecisionController.class)
                .submitReview(applicationDetailId, null, null, null)))
                .with(user(USER))
                .with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(ReviewRecordOfDecisionController.class)
            .renderReview(applicationDetailId, null, null))))
        .andExpect(notificationBannerDoesNotExist());

    assertThat(applicationDetail.getStatus()).isEqualTo(ApplicationStatus.ISSUE_DECISION);
  }

  private void mockSubmittable(ScheduleWorkProgrammeApplicationDetail applicationDetail, boolean submittable) {
    when(recordOfDecisionTaskListService.isSubmittable(
        any(RecordOfDecisionTaskListContext.class), eq(USER)))
        .thenReturn(submittable);
  }

  private SummarySection summarySection(String sectionName, String key) {
    var summaryData = SummaryDataView.newBuilder().addStringValue(key, "1 year").build();
    return new SummarySection(
        10,
        List.of(SummaryItem.withCard(sectionName, SummaryCard.simpleSummaryCard(summaryData))));
  }

  private ScheduleWorkProgrammeApplicationDetail buildApplicationDetail(UUID applicationDetailId) {
    return ScheduleWorkProgrammeApplicationDetailTestUtil.builder()
        .withId(applicationDetailId)
        .withStatus(ApplicationStatus.ISSUE_DECISION)
        .withApplicationReference(APPLICATION_REFERENCE)
        .build();
  }

  private void setupPassingInterceptors(ScheduleWorkProgrammeApplicationDetail applicationDetail) {
    when(scheduleWorkProgrammeApplicationService.getDetailByIdOrThrow(applicationDetail.getId()))
        .thenReturn(applicationDetail);
    when(applicationAccessService.userHasAccessToApplication(
        eq(applicationDetail), anyMap(), eq(REGULATOR_WUA_ID)))
        .thenReturn(true);
  }
}
