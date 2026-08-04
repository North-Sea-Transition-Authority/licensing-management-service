package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
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

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.validation.Errors;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.scheduleworkprogrammeapplication.InvokingUserCanAccessScheduleApplication;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.scheduleworkprogrammeapplication.ScheduleAmendmentApplicationHasStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@ContextConfiguration(classes = RecordDecisionController.class)
class RecordDecisionControllerTest extends AbstractControllerTest {

  private static final Long REGULATOR_WUA_ID = 1L;
  private static final ServiceUserDetail USER = ServiceUserDetailTestUtil.newBuilder()
      .withWuaId(REGULATOR_WUA_ID)
      .build();

  @MockitoBean
  private RecordDecisionFormValidator recordDecisionFormValidator;

  @MockitoBean
  private RecordOfDecisionService recordOfDecisionService;

  @Test
  void renderForm_classAnnotations_presentAndCorrect() {
    assertThat(RecordDecisionController.class)
        .hasAnnotation(ScheduleAmendmentApplicationHasStatus.class);
    assertThat(RecordDecisionController.class
        .getAnnotation(ScheduleAmendmentApplicationHasStatus.class).value())
        .containsOnly(ApplicationStatus.ISSUE_DECISION);
    assertThat(RecordDecisionController.class)
        .hasAnnotation(InvokingUserCanAccessScheduleApplication.class);
  }

  @Test
  void renderForm_noApplicationAccess_returnsForbidden() throws Exception {
    var applicationDetailId = UUID.randomUUID();
    var applicationDetail = buildApplicationDetail(applicationDetailId);

    when(scheduleWorkProgrammeApplicationService.getDetailByIdOrThrow(applicationDetailId))
        .thenReturn(applicationDetail);
    when(applicationAccessService.userHasAccessToApplication(eq(applicationDetail), anyMap(), eq(REGULATOR_WUA_ID)))
        .thenReturn(false);

    mockMvc.perform(
            get(ReverseRouter.route(on(RecordDecisionController.class)
                .renderForm(applicationDetailId, null)))
                .with(user(USER)))
        .andExpect(status().isForbidden());
  }

  @Test
  void renderForm_withAccess_returnsOk() throws Exception {
    var applicationDetailId = UUID.randomUUID();
    var applicationDetail = buildApplicationDetail(applicationDetailId);

    setupPassingInterceptors(applicationDetail);
    when(recordOfDecisionService.getFilledDecisionForm(applicationDetail))
        .thenReturn(new RecordDecisionForm());

    mockMvc.perform(
            get(ReverseRouter.route(on(RecordDecisionController.class)
                .renderForm(applicationDetailId, null)))
                .with(user(USER)))
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/scheduleWorkProgrammeApplication/whatIsTheDecision"))
        .andExpect(model().attribute("pageTitle", RecordDecisionController.PAGE_TITLE))
        .andExpect(model().attributeExists("form", "cancelUrl", "decisionOptions"));
  }

  @Test
  void submitForm_validForm_savesAndRedirectsToTaskList() throws Exception {
    var applicationDetailId = UUID.randomUUID();
    var applicationDetail = buildApplicationDetail(applicationDetailId);

    setupPassingInterceptors(applicationDetail);
    when(recordDecisionFormValidator.isValid(any(RecordDecisionForm.class), any(Errors.class)))
        .thenReturn(true);

    mockMvc.perform(
            post(ReverseRouter.route(on(RecordDecisionController.class)
                .submitForm(applicationDetailId, null, null, null)))
                .with(user(USER))
                .with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(RecordOfDecisionTaskListController.class)
            .getTaskList(applicationDetailId, null, null))));
  }

  @Test
  void submitForm_invalidForm_returnsForm() throws Exception {
    var applicationDetailId = UUID.randomUUID();
    var applicationDetail = buildApplicationDetail(applicationDetailId);

    setupPassingInterceptors(applicationDetail);
    when(recordDecisionFormValidator.isValid(any(RecordDecisionForm.class), any(Errors.class)))
        .thenReturn(false);

    mockMvc.perform(
            post(ReverseRouter.route(on(RecordDecisionController.class)
                .submitForm(applicationDetailId, null, null, null)))
                .with(user(USER))
                .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/scheduleWorkProgrammeApplication/whatIsTheDecision"));
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
