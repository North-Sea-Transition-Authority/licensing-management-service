package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
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
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@ContextConfiguration(classes = RecordDurationChangesSummaryController.class)
class RecordDurationChangesSummaryControllerTest extends AbstractControllerTest {

  private static final String VIEW_NAME =
      "lms/licence/scheduleWorkProgrammeApplication/recordDurationChangesSummary";
  private static final Long REGULATOR_WUA_ID = 1L;
  private static final ServiceUserDetail USER = ServiceUserDetailTestUtil.newBuilder()
      .withWuaId(REGULATOR_WUA_ID)
      .build();

  @MockitoBean
  private RecordDurationChangesService recordDurationChangesService;

  @Test
  void renderSummary_classAnnotations_presentAndCorrect() {
    assertThat(RecordDurationChangesSummaryController.class)
        .hasAnnotation(ScheduleAmendmentApplicationHasStatus.class);
    assertThat(RecordDurationChangesSummaryController.class
        .getAnnotation(ScheduleAmendmentApplicationHasStatus.class).value())
        .containsOnly(ApplicationStatus.ISSUE_DECISION);
    assertThat(RecordDurationChangesSummaryController.class)
        .hasAnnotation(InvokingUserCanAccessScheduleApplication.class);
  }

  @Test
  void renderSummary_noApplicationAccess_returnsForbidden() throws Exception {
    var applicationDetailId = UUID.randomUUID();
    var applicationDetail = buildApplicationDetail(applicationDetailId);

    when(scheduleWorkProgrammeApplicationService.getDetailByIdOrThrow(applicationDetailId))
        .thenReturn(applicationDetail);
    when(applicationAccessService.userHasAccessToApplication(eq(applicationDetail), anyMap(), eq(REGULATOR_WUA_ID)))
        .thenReturn(false);

    mockMvc.perform(
            get(ReverseRouter.route(on(RecordDurationChangesSummaryController.class)
                .renderSummary(applicationDetailId, null)))
                .with(user(USER)))
        .andExpect(status().isForbidden());
  }

  @Test
  void renderSummary_whenDurationChangesRecorded_showsACardForEachPeriod() throws Exception {
    var applicationDetailId = UUID.randomUUID();
    var applicationDetail = buildApplicationDetail(applicationDetailId);

    setupPassingInterceptors(applicationDetail);
    when(recordDurationChangesService.isComplete(applicationDetail)).thenReturn(true);
    when(recordDurationChangesService.getSummaryViews(applicationDetail))
        .thenReturn(List.of(
            new RecordDurationChangeSummaryView(
                "Initial term", false, "4 years", "31 December 2027",
                "Maintained", "4 years", "31 December 2027"),
            new RecordDurationChangeSummaryView(
                "Second term", false, "4 years", "31 December 2031",
                "Reduced by 1 year", "3 years", "31 December 2030")));

    mockMvc.perform(
            get(ReverseRouter.route(on(RecordDurationChangesSummaryController.class)
                .renderSummary(applicationDetailId, null)))
                .with(user(USER)))
        .andExpect(status().isOk())
        .andExpect(view().name(VIEW_NAME))
        .andExpect(model().attribute("pageTitle", RecordDurationChangesSummaryController.PAGE_TITLE))
        .andExpect(model().attributeExists("durationChangeSummaryViews", "backUrl"))
        .andExpect(content().string(containsString("Initial term")))
        .andExpect(content().string(containsString("Second term")))
        .andExpect(content().string(containsString("Reduced by 1 year")))
        .andExpect(content().string(containsString("31 December 2030")))
        .andExpect(content().string(containsString("Current duration")))
        .andExpect(content().string(containsString("Current end date")))
        .andExpect(content().string(containsString("Change")))
        .andExpect(content().string(containsString("New duration")))
        .andExpect(content().string(containsString("New end date")))
        .andExpect(content().string(containsString("Continue")));
  }

  @Test
  void renderSummary_whenDurationChangesAreIncomplete_redirectsToDurationChanges() throws Exception {
    var applicationDetailId = UUID.randomUUID();
    var applicationDetail = buildApplicationDetail(applicationDetailId);

    setupPassingInterceptors(applicationDetail);
    when(recordDurationChangesService.isComplete(applicationDetail)).thenReturn(false);

    mockMvc.perform(
            get(ReverseRouter.route(on(RecordDurationChangesSummaryController.class)
                .renderSummary(applicationDetailId, null)))
                .with(user(USER)))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(RecordDurationChangesController.class)
            .renderForm(applicationDetailId, null))));
  }

  @Test
  void confirmSummary_whenDurationChangesRecorded_redirectsToTaskList() throws Exception {
    var applicationDetailId = UUID.randomUUID();
    var applicationDetail = buildApplicationDetail(applicationDetailId);

    setupPassingInterceptors(applicationDetail);
    when(recordDurationChangesService.isComplete(applicationDetail)).thenReturn(true);

    mockMvc.perform(
            post(ReverseRouter.route(on(RecordDurationChangesSummaryController.class)
                .confirmSummary(applicationDetailId, null)))
                .with(user(USER))
                .with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(RecordOfDecisionTaskListController.class)
            .getTaskList(applicationDetailId, null, null))));
  }

  @Test
  void confirmSummary_whenDurationChangesAreIncomplete_redirectsToDurationChanges() throws Exception {
    var applicationDetailId = UUID.randomUUID();
    var applicationDetail = buildApplicationDetail(applicationDetailId);

    setupPassingInterceptors(applicationDetail);
    when(recordDurationChangesService.isComplete(applicationDetail)).thenReturn(false);

    mockMvc.perform(
            post(ReverseRouter.route(on(RecordDurationChangesSummaryController.class)
                .confirmSummary(applicationDetailId, null)))
                .with(user(USER))
                .with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(RecordDurationChangesController.class)
            .renderForm(applicationDetailId, null))));
  }

  private ScheduleWorkProgrammeApplicationDetail buildApplicationDetail(UUID applicationDetailId) {
    return ScheduleWorkProgrammeApplicationDetailTestUtil.builder()
        .withId(applicationDetailId)
        .withStatus(ApplicationStatus.ISSUE_DECISION)
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
