package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationActionEndPointInterceptorRule;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overview.ScheduleWorkProgrammeApplicationOverviewController;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overview.action.ScheduleWorkProgrammeApplicationActionItem;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@ContextConfiguration(classes = StartRecordOfDecisionController.class)
class StartRecordOfDecisionControllerTest extends AbstractControllerTest {

  private static final Long REGULATOR_WUA_ID = 1L;
  private static final ServiceUserDetail USER = ServiceUserDetailTestUtil.newBuilder()
      .withWuaId(REGULATOR_WUA_ID)
      .build();
  private static final String APPLICATION_REFERENCE = "LMS/EAA/2026/1";

  @MockitoBean
  private RecordOfDecisionTaskListService recordOfDecisionTaskListService;

  @Test
  void classAnnotations_presentAndCorrect() {
    assertThat(StartRecordOfDecisionController.class)
        .hasAnnotation(ScheduleAmendmentApplicationHasStatus.class)
        .hasAnnotation(InvokingUserCanAccessScheduleApplication.class)
        .hasAnnotation(ScheduleWorkProgrammeApplicationActionEndPointInterceptorRule.ActionEndPoint.class);
    assertThat(StartRecordOfDecisionController.class
        .getAnnotation(ScheduleAmendmentApplicationHasStatus.class).value())
        .containsOnly(ApplicationStatus.DSP_UPLOADED);
    assertThat(StartRecordOfDecisionController.class
        .getAnnotation(ScheduleWorkProgrammeApplicationActionEndPointInterceptorRule.ActionEndPoint.class).value())
        .containsOnly(ScheduleWorkProgrammeApplicationActionItem.RECORD_DECISION);
  }

  @Test
  void render_whenNoActionAccess_thenForbidden() throws Exception {
    var applicationDetail = buildApplicationDetail();
    when(scheduleWorkProgrammeApplicationService.getDetailByIdOrThrow(applicationDetail.getId()))
        .thenReturn(applicationDetail);
    when(applicationAccessService.userHasAccessToApplication(eq(applicationDetail), anyMap(), eq(REGULATOR_WUA_ID)))
        .thenReturn(true);
    when(scheduleWorkProgrammeApplicationActionService.getAvailableUserActionItems(applicationDetail, USER))
        .thenReturn(List.of());

    mockMvc.perform(
            get(ReverseRouter.route(on(StartRecordOfDecisionController.class)
                .render(applicationDetail.getId(), null)))
                .with(user(USER)))
        .andExpect(status().isForbidden());
  }

  @Test
  void render_whenAccessGranted_thenRendersStartPage() throws Exception {
    var applicationDetail = buildApplicationDetail();
    setupPassingInterceptors(applicationDetail);

    mockMvc.perform(
            get(ReverseRouter.route(on(StartRecordOfDecisionController.class)
                .render(applicationDetail.getId(), null)))
                .with(user(USER)))
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/scheduleWorkProgrammeApplication/startRecordOfDecision"))
        .andExpect(model().attribute("pageTitle", StartRecordOfDecisionController.PAGE_TITLE))
        .andExpect(model().attribute("pageCaption", APPLICATION_REFERENCE))
        .andExpect(model().attribute("applicationTypeDisplayName",
            "licence and work programme extension and amendment application"))
        .andExpect(model().attribute("startUrl", ReverseRouter.route(on(StartRecordOfDecisionController.class)
            .startDecision(applicationDetail.getId(), null, null))))
        .andExpect(model().attribute("backUrl", ReverseRouter.route(on(ScheduleWorkProgrammeApplicationOverviewController.class)
            .renderOverview(applicationDetail.getId(), null, null, null))));
  }

  @Test
  void startDecision_whenNoActionAccess_thenForbidden() throws Exception {
    var applicationDetail = buildApplicationDetail();
    when(scheduleWorkProgrammeApplicationService.getDetailByIdOrThrow(applicationDetail.getId()))
        .thenReturn(applicationDetail);
    when(applicationAccessService.userHasAccessToApplication(eq(applicationDetail), anyMap(), eq(REGULATOR_WUA_ID)))
        .thenReturn(true);
    when(scheduleWorkProgrammeApplicationActionService.getAvailableUserActionItems(applicationDetail, USER))
        .thenReturn(List.of());

    mockMvc.perform(
            post(ReverseRouter.route(on(StartRecordOfDecisionController.class)
                .startDecision(applicationDetail.getId(), null, null)))
                .with(user(USER))
                .with(csrf()))
        .andExpect(status().isForbidden());

    verifyNoInteractions(recordOfDecisionTaskListService);
  }

  @Test
  void startDecision_whenAccessGranted_thenCreatesDraftAndRedirectsToTaskList() throws Exception {
    var applicationDetail = buildApplicationDetail();
    setupPassingInterceptors(applicationDetail);

    mockMvc.perform(
            post(ReverseRouter.route(on(StartRecordOfDecisionController.class)
                .startDecision(applicationDetail.getId(), null, null)))
                .with(user(USER))
                .with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(RecordOfDecisionTaskListController.class)
            .getTaskList(applicationDetail.getId(), null, null))));

    verify(recordOfDecisionTaskListService).startDecision(applicationDetail, USER);
  }

  private ScheduleWorkProgrammeApplicationDetail buildApplicationDetail() {
    return ScheduleWorkProgrammeApplicationDetailTestUtil.builder()
        .withId(UUID.randomUUID())
        .withStatus(ApplicationStatus.DSP_UPLOADED)
        .withApplicationReference(APPLICATION_REFERENCE)
        .build();
  }

  private void setupPassingInterceptors(ScheduleWorkProgrammeApplicationDetail applicationDetail) {
    when(scheduleWorkProgrammeApplicationService.getDetailByIdOrThrow(applicationDetail.getId()))
        .thenReturn(applicationDetail);
    when(applicationAccessService.userHasAccessToApplication(eq(applicationDetail), anyMap(), eq(REGULATOR_WUA_ID)))
        .thenReturn(true);
    when(scheduleWorkProgrammeApplicationActionService.getAvailableUserActionItems(applicationDetail, USER))
        .thenReturn(List.of(
            ScheduleWorkProgrammeApplicationActionItem.RECORD_DECISION.toActionItemView(applicationDetail)));
  }
}
