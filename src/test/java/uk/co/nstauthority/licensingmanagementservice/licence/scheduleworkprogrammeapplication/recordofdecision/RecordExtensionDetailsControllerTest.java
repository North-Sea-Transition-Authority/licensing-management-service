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

@ContextConfiguration(classes = RecordExtensionDetailsController.class)
class RecordExtensionDetailsControllerTest extends AbstractControllerTest {

  private static final Long REGULATOR_WUA_ID = 1L;
  private static final ServiceUserDetail USER = ServiceUserDetailTestUtil.newBuilder()
      .withWuaId(REGULATOR_WUA_ID)
      .build();

  @MockitoBean
  private RecordExtensionDetailsService recordExtensionDetailsService;

  @MockitoBean
  private RecordExtensionDetailsFormValidator recordExtensionDetailsFormValidator;

  @Test
  void renderForm_classAnnotations_presentAndCorrect() {
    assertThat(RecordExtensionDetailsController.class)
        .hasAnnotation(ScheduleAmendmentApplicationHasStatus.class);
    assertThat(RecordExtensionDetailsController.class
        .getAnnotation(ScheduleAmendmentApplicationHasStatus.class).value())
        .containsOnly(ApplicationStatus.ISSUE_DECISION);
    assertThat(RecordExtensionDetailsController.class)
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
            get(ReverseRouter.route(on(RecordExtensionDetailsController.class)
                .renderForm(applicationDetailId, null)))
                .with(user(USER)))
        .andExpect(status().isForbidden());
  }

  @Test
  void renderForm_withAccess_returnsOk() throws Exception {
    var applicationDetailId = UUID.randomUUID();
    var applicationDetail = buildApplicationDetail(applicationDetailId);

    setupPassingInterceptors(applicationDetail);
    when(recordExtensionDetailsService.getFilledForm(applicationDetail))
        .thenReturn(new RecordExtensionDetailsForm());
    when(recordExtensionDetailsService.getExtensionDetailsViews(applicationDetail))
        .thenReturn(List.of());

    mockMvc.perform(
            get(ReverseRouter.route(on(RecordExtensionDetailsController.class)
                .renderForm(applicationDetailId, null)))
                .with(user(USER)))
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/scheduleWorkProgrammeApplication/recordExtensionDetails"))
        .andExpect(model().attribute("pageTitle", RecordExtensionDetailsController.PAGE_TITLE))
        .andExpect(model().attributeExists("form", "cancelUrl", "extensionDetailsViews", "canExtendMoreThanOneOption"));
  }

  @Test
  void submitForm_validForm_savesAndRedirectsToTaskList() throws Exception {
    var applicationDetailId = UUID.randomUUID();
    var applicationDetail = buildApplicationDetail(applicationDetailId);

    setupPassingInterceptors(applicationDetail);
    when(recordExtensionDetailsFormValidator.isValid(
        any(RecordExtensionDetailsForm.class), any(BindingResult.class), eq(applicationDetail)))
        .thenReturn(true);

    mockMvc.perform(
            post(ReverseRouter.route(on(RecordExtensionDetailsController.class)
                .submitForm(applicationDetailId, null, null, null)))
                .with(user(USER))
                .with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(RecordOfDecisionTaskListController.class)
            .getTaskList(applicationDetailId, null, null))));

    verify(recordExtensionDetailsService).saveExtensionDetails(any(RecordExtensionDetailsForm.class), eq(applicationDetail));
  }

  @Test
  void submitForm_whenOnlyPhaseOptionsAndPhaseRejected_showsErrorAgainstTheCheckboxGroup() throws Exception {
    var applicationDetailId = UUID.randomUUID();
    var applicationDetail = buildApplicationDetail(applicationDetailId);

    setupPassingInterceptors(applicationDetail);
    when(recordExtensionDetailsService.getExtensionDetailsViews(applicationDetail)).thenReturn(List.of(
        new RecordExtensionDetailsView(UUID.randomUUID().toString(), "Phase A", "1 January 2030", true, false, null),
        new RecordExtensionDetailsView(UUID.randomUUID().toString(), "Phase B", "1 January 2031", true, false, null)
    ));
    doAnswer(invocation -> {
      Errors errors = invocation.getArgument(1);
      errors.rejectValue("selectedPhase", "selectedPhase.required", "Select at least one phase being extended");
      return false;
    }).when(recordExtensionDetailsFormValidator)
        .isValid(any(RecordExtensionDetailsForm.class), any(BindingResult.class), eq(applicationDetail));

    mockMvc.perform(
            post(ReverseRouter.route(on(RecordExtensionDetailsController.class)
                .submitForm(applicationDetailId, null, null, null)))
                .with(user(USER))
                .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Select at least one phase being extended")))
        .andExpect(content().string(containsString("govuk-form-group--error")))
        .andExpect(content().string(containsString("govuk-error-message")));
  }

  @Test
  void submitForm_invalidForm_returnsForm() throws Exception {
    var applicationDetailId = UUID.randomUUID();
    var applicationDetail = buildApplicationDetail(applicationDetailId);

    setupPassingInterceptors(applicationDetail);
    when(recordExtensionDetailsFormValidator.isValid(
        any(RecordExtensionDetailsForm.class), any(BindingResult.class), eq(applicationDetail)))
        .thenReturn(false);
    when(recordExtensionDetailsService.getExtensionDetailsViews(applicationDetail))
        .thenReturn(List.of());

    mockMvc.perform(
            post(ReverseRouter.route(on(RecordExtensionDetailsController.class)
                .submitForm(applicationDetailId, null, null, null)))
                .with(user(USER))
                .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/scheduleWorkProgrammeApplication/recordExtensionDetails"));
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
