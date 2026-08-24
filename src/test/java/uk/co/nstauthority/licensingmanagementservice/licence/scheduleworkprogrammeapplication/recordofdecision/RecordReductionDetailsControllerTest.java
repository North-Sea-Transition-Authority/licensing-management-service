package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
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
import org.springframework.validation.BindingResult;
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

@ContextConfiguration(classes = RecordReductionDetailsController.class)
class RecordReductionDetailsControllerTest extends AbstractControllerTest {

  private static final Long REGULATOR_WUA_ID = 1L;
  private static final ServiceUserDetail USER = ServiceUserDetailTestUtil.newBuilder()
      .withWuaId(REGULATOR_WUA_ID)
      .build();

  @MockitoBean
  private RecordReductionDetailsService recordReductionDetailsService;

  @MockitoBean
  private RecordReductionDetailsFormValidator recordReductionDetailsFormValidator;

  @Test
  void renderForm_classAnnotations_presentAndCorrect() {
    assertThat(RecordReductionDetailsController.class)
        .hasAnnotation(ScheduleAmendmentApplicationHasStatus.class);
    assertThat(RecordReductionDetailsController.class
        .getAnnotation(ScheduleAmendmentApplicationHasStatus.class).value())
        .containsOnly(ApplicationStatus.ISSUE_DECISION);
    assertThat(RecordReductionDetailsController.class)
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
            get(ReverseRouter.route(on(RecordReductionDetailsController.class)
                .renderForm(applicationDetailId, null)))
                .with(user(USER)))
        .andExpect(status().isForbidden());
  }

  @Test
  void renderForm_withAccess_returnsOk() throws Exception {
    var applicationDetailId = UUID.randomUUID();
    var applicationDetail = buildApplicationDetail(applicationDetailId);

    setupPassingInterceptors(applicationDetail);
    when(recordReductionDetailsService.getFilledForm(applicationDetail))
        .thenReturn(new RecordReductionDetailsForm());
    when(recordReductionDetailsService.getReductionDetailsViews(applicationDetail))
        .thenReturn(List.of());

    mockMvc.perform(
            get(ReverseRouter.route(on(RecordReductionDetailsController.class)
                .renderForm(applicationDetailId, null)))
                .with(user(USER)))
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/scheduleWorkProgrammeApplication/recordReductionDetails"))
        .andExpect(model().attribute("pageTitle", RecordReductionDetailsController.PAGE_TITLE))
        .andExpect(model().attributeExists("form", "cancelUrl", "reductionDetailsViews", "canReduceMoreThanOneOption"));
  }

  @Test
  void submitForm_validForm_savesAndRedirectsToTaskList() throws Exception {
    var applicationDetailId = UUID.randomUUID();
    var applicationDetail = buildApplicationDetail(applicationDetailId);

    setupPassingInterceptors(applicationDetail);
    when(recordReductionDetailsFormValidator.isValid(
        any(RecordReductionDetailsForm.class), any(BindingResult.class), eq(applicationDetail)))
        .thenReturn(true);

    mockMvc.perform(
            post(ReverseRouter.route(on(RecordReductionDetailsController.class)
                .submitForm(applicationDetailId, null, null, null)))
                .with(user(USER))
                .with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(RecordOfDecisionTaskListController.class)
            .getTaskList(applicationDetailId, null, null))));

    verify(recordReductionDetailsService).saveReductionDetails(any(RecordReductionDetailsForm.class), eq(applicationDetail));
  }

  @Test
  void submitForm_whenTotalDoesNotMatchTheExtension_showsErrorAgainstTheDuration() throws Exception {
    var applicationDetailId = UUID.randomUUID();
    var applicationDetail = buildApplicationDetail(applicationDetailId);
    var termId = UUID.randomUUID().toString();

    setupPassingInterceptors(applicationDetail);
    when(recordReductionDetailsService.getReductionDetailsViews(applicationDetail)).thenReturn(List.of(
        new RecordReductionDetailsView(termId, "Second Term", "17 July 2026", false, false, null),
        new RecordReductionDetailsView(UUID.randomUUID().toString(), "Third Term", "17 July 2030", false, false, null)
    ));
    doAnswer(invocation -> {
      RecordReductionDetailsForm form = invocation.getArgument(0);
      form.getReductionDuration().put(termId, RecordReductionDetailsForm.newDurationInput(termId));
      Errors errors = invocation.getArgument(1);
      errors.rejectValue(
          "reductionDuration[%s].years".formatted(termId),
          "reductionDuration.total.mismatch",
          "The total reduction must equal the total extension of 1 year");
      return false;
    }).when(recordReductionDetailsFormValidator)
        .isValid(any(RecordReductionDetailsForm.class), any(BindingResult.class), eq(applicationDetail));

    mockMvc.perform(
            post(ReverseRouter.route(on(RecordReductionDetailsController.class)
                .submitForm(applicationDetailId, null, null, null)))
                .with(user(USER))
                .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("The total reduction must equal the total extension of 1 year")))
        .andExpect(content().string(containsString("govuk-form-group--error")))
        .andExpect(content().string(containsString("govuk-error-message")));
  }

  @Test
  void submitForm_invalidForm_returnsForm() throws Exception {
    var applicationDetailId = UUID.randomUUID();
    var applicationDetail = buildApplicationDetail(applicationDetailId);

    setupPassingInterceptors(applicationDetail);
    when(recordReductionDetailsFormValidator.isValid(
        any(RecordReductionDetailsForm.class), any(BindingResult.class), eq(applicationDetail)))
        .thenReturn(false);
    when(recordReductionDetailsService.getReductionDetailsViews(applicationDetail))
        .thenReturn(List.of());

    mockMvc.perform(
            post(ReverseRouter.route(on(RecordReductionDetailsController.class)
                .submitForm(applicationDetailId, null, null, null)))
                .with(user(USER))
                .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/scheduleWorkProgrammeApplication/recordReductionDetails"));
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
