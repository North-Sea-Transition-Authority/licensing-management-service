package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
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

@ContextConfiguration(classes = RecordWorkProgrammeAmendmentSummaryController.class)
class RecordWorkProgrammeAmendmentSummaryControllerTest extends AbstractControllerTest {

  private static final String VIEW_NAME =
      "lms/licence/scheduleWorkProgrammeApplication/recordWorkProgrammeAmendmentSummary";
  private static final Long REGULATOR_WUA_ID = 1L;
  private static final ServiceUserDetail USER = ServiceUserDetailTestUtil.newBuilder()
      .withWuaId(REGULATOR_WUA_ID)
      .build();

  @MockitoBean
  private RecordWorkProgrammeAmendmentDetailsService recordWorkProgrammeAmendmentDetailsService;

  @MockitoBean
  private RecordWorkProgrammeAmendmentSummaryFormValidator recordWorkProgrammeAmendmentSummaryFormValidator;

  @MockitoBean
  private RecordOfDecisionService recordOfDecisionService;

  @Test
  void renderForm_classAnnotations_presentAndCorrect() {
    assertThat(RecordWorkProgrammeAmendmentSummaryController.class)
        .hasAnnotation(ScheduleAmendmentApplicationHasStatus.class);
    assertThat(RecordWorkProgrammeAmendmentSummaryController.class
        .getAnnotation(ScheduleAmendmentApplicationHasStatus.class).value())
        .containsOnly(ApplicationStatus.ISSUE_DECISION);
    assertThat(RecordWorkProgrammeAmendmentSummaryController.class)
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
            get(ReverseRouter.route(on(RecordWorkProgrammeAmendmentSummaryController.class)
                .renderForm(applicationDetailId, null)))
                .with(user(USER)))
        .andExpect(status().isForbidden());
  }

  @Test
  void renderForm_whenDecisionsRecorded_showsCardsForEachDecision() throws Exception {
    var applicationDetailId = UUID.randomUUID();
    var applicationDetail = buildApplicationDetail(applicationDetailId);

    setupPassingInterceptors(applicationDetail);
    when(recordWorkProgrammeAmendmentDetailsService.hasAmendmentDetails(applicationDetail)).thenReturn(true);
    when(recordWorkProgrammeAmendmentDetailsService.getRecordedAmendmentViews(applicationDetail))
        .thenReturn(List.of(
            new RecordWorkProgrammeAmendmentSummaryView(
                "Drill well to 3,000m", "27 July 2026",
                WorkProgrammeAmendmentDecision.AMEND.getDisplayName(),
                "6 months", "Revised well commitment wording"),
            new RecordWorkProgrammeAmendmentSummaryView(
                "Acquire 3D seismic data", "31 March 2027",
                WorkProgrammeAmendmentDecision.WAIVE.getDisplayName(), null, null)));
    when(recordOfDecisionService.getFilledWorkProgrammeSummaryForm(applicationDetail))
        .thenReturn(new RecordWorkProgrammeAmendmentSummaryForm());

    mockMvc.perform(
            get(ReverseRouter.route(on(RecordWorkProgrammeAmendmentSummaryController.class)
                .renderForm(applicationDetailId, null)))
                .with(user(USER)))
        .andExpect(status().isOk())
        .andExpect(view().name(VIEW_NAME))
        .andExpect(model().attribute("pageTitle", RecordWorkProgrammeAmendmentSummaryController.PAGE_TITLE))
        .andExpect(model().attributeExists("form", "cancelUrl", "workProgrammeAmendments", "summaryOptions"))
        .andExpect(content().string(containsString("Drill well to 3,000m")))
        .andExpect(content().string(containsString(WorkProgrammeAmendmentDecision.AMEND.getDisplayName())))
        .andExpect(content().string(containsString("Acquire 3D seismic data")))
        .andExpect(content().string(containsString(WorkProgrammeAmendmentDecision.WAIVE.getDisplayName())))
        .andExpect(content().string(containsString("27 July 2026")))
        .andExpect(content().string(containsString("6 months")))
        .andExpect(content().string(containsString("Revised well commitment wording")))
        .andExpect(content().string(containsString(
            "Do you want to add another work programme activity to the scope of this decision?")));
  }

  @Test
  void renderForm_whenNoDecisionsRecorded_redirectsToSelectActivity() throws Exception {
    var applicationDetailId = UUID.randomUUID();
    var applicationDetail = buildApplicationDetail(applicationDetailId);

    setupPassingInterceptors(applicationDetail);
    when(recordWorkProgrammeAmendmentDetailsService.hasAmendmentDetails(applicationDetail)).thenReturn(false);

    mockMvc.perform(
            get(ReverseRouter.route(on(RecordWorkProgrammeAmendmentSummaryController.class)
                .renderForm(applicationDetailId, null)))
                .with(user(USER)))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(SelectWorkProgrammeActivityController.class)
            .renderForm(applicationDetailId, null))));
  }

  @Test
  void submitForm_whenAddingAnotherNow_redirectsToSelectActivity() throws Exception {
    var applicationDetailId = UUID.randomUUID();
    var applicationDetail = buildApplicationDetail(applicationDetailId);

    setupPassingInterceptors(applicationDetail);
    when(recordWorkProgrammeAmendmentDetailsService.hasAmendmentDetails(applicationDetail)).thenReturn(true);
    mockValidatorPassesWith(RecordWorkProgrammeAmendmentSummaryOptions.YES_NOW);

    mockMvc.perform(
            post(ReverseRouter.route(on(RecordWorkProgrammeAmendmentSummaryController.class)
                .submitForm(applicationDetailId, null, null, null)))
                .with(user(USER))
                .with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(SelectWorkProgrammeActivityController.class)
            .renderForm(applicationDetailId, null))));
  }

  @ParameterizedTest
  @EnumSource(value = RecordWorkProgrammeAmendmentSummaryOptions.class, names = {"NO_LATER", "NO_ALL_ADDED"})
  void submitForm_whenNotAddingAnotherNow_redirectsToTaskList(
      RecordWorkProgrammeAmendmentSummaryOptions option
  ) throws Exception {
    var applicationDetailId = UUID.randomUUID();
    var applicationDetail = buildApplicationDetail(applicationDetailId);

    setupPassingInterceptors(applicationDetail);
    when(recordWorkProgrammeAmendmentDetailsService.hasAmendmentDetails(applicationDetail)).thenReturn(true);
    mockValidatorPassesWith(option);

    mockMvc.perform(
            post(ReverseRouter.route(on(RecordWorkProgrammeAmendmentSummaryController.class)
                .submitForm(applicationDetailId, null, null, null)))
                .with(user(USER))
                .with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(RecordOfDecisionTaskListController.class)
            .getTaskList(applicationDetailId, null, null))));
  }

  @Test
  void submitForm_whenValid_savesTheSelectedOption() throws Exception {
    var applicationDetailId = UUID.randomUUID();
    var applicationDetail = buildApplicationDetail(applicationDetailId);

    setupPassingInterceptors(applicationDetail);
    when(recordWorkProgrammeAmendmentDetailsService.hasAmendmentDetails(applicationDetail)).thenReturn(true);
    mockValidatorPassesWith(RecordWorkProgrammeAmendmentSummaryOptions.NO_ALL_ADDED);

    mockMvc.perform(
            post(ReverseRouter.route(on(RecordWorkProgrammeAmendmentSummaryController.class)
                .submitForm(applicationDetailId, null, null, null)))
                .with(user(USER))
                .with(csrf()));

    verify(recordOfDecisionService).saveWorkProgrammeSummaryOption(
        eq(applicationDetail), any(RecordWorkProgrammeAmendmentSummaryForm.class));
  }

  @Test
  void submitForm_invalidForm_returnsFormAndSavesNothing() throws Exception {
    var applicationDetailId = UUID.randomUUID();
    var applicationDetail = buildApplicationDetail(applicationDetailId);

    setupPassingInterceptors(applicationDetail);
    when(recordWorkProgrammeAmendmentDetailsService.hasAmendmentDetails(applicationDetail)).thenReturn(true);
    when(recordWorkProgrammeAmendmentDetailsService.getRecordedAmendmentViews(applicationDetail))
        .thenReturn(List.of(new RecordWorkProgrammeAmendmentSummaryView(
            "Drill well to 3,000m", "27 July 2026",
            WorkProgrammeAmendmentDecision.AMEND.getDisplayName(), null, null)));
    doAnswer(invocation -> {
      Errors errors = invocation.getArgument(0);
      errors.rejectValue(
          "recordWorkProgrammeAmendmentSummaryOptions",
          "recordWorkProgrammeAmendmentSummaryOptions.required",
          RecordWorkProgrammeAmendmentSummaryFormValidator.REQUIRED_ERROR_MESSAGE);
      return false;
    }).when(recordWorkProgrammeAmendmentSummaryFormValidator).isValid(any(BindingResult.class));

    mockMvc.perform(
            post(ReverseRouter.route(on(RecordWorkProgrammeAmendmentSummaryController.class)
                .submitForm(applicationDetailId, null, null, null)))
                .with(user(USER))
                .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(view().name(VIEW_NAME))
        .andExpect(content().string(
            containsString(RecordWorkProgrammeAmendmentSummaryFormValidator.REQUIRED_ERROR_MESSAGE)));

    verify(recordOfDecisionService, never()).saveWorkProgrammeSummaryOption(
        eq(applicationDetail), any(RecordWorkProgrammeAmendmentSummaryForm.class));
  }

  @Test
  void submitForm_whenNoDecisionsRecorded_redirectsToSelectActivityAndSavesNothing() throws Exception {
    var applicationDetailId = UUID.randomUUID();
    var applicationDetail = buildApplicationDetail(applicationDetailId);

    setupPassingInterceptors(applicationDetail);
    when(recordWorkProgrammeAmendmentDetailsService.hasAmendmentDetails(applicationDetail)).thenReturn(false);

    mockMvc.perform(
            post(ReverseRouter.route(on(RecordWorkProgrammeAmendmentSummaryController.class)
                .submitForm(applicationDetailId, null, null, null)))
                .with(user(USER))
                .with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(SelectWorkProgrammeActivityController.class)
            .renderForm(applicationDetailId, null))));

    verify(recordOfDecisionService, never()).saveWorkProgrammeSummaryOption(
        eq(applicationDetail), any(RecordWorkProgrammeAmendmentSummaryForm.class));
  }

  private void mockValidatorPassesWith(RecordWorkProgrammeAmendmentSummaryOptions option) {
    doAnswer(invocation -> {
      BindingResult bindingResult = invocation.getArgument(0);
      var form = (RecordWorkProgrammeAmendmentSummaryForm) bindingResult.getTarget();
      form.setRecordWorkProgrammeAmendmentSummaryOptions(option);
      return true;
    }).when(recordWorkProgrammeAmendmentSummaryFormValidator).isValid(any(BindingResult.class));
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
