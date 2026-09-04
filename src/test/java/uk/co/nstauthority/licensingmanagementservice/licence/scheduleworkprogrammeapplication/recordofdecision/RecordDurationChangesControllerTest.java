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
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.scheduleworkprogrammeapplication.InvokingUserCanAccessScheduleApplication;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.scheduleworkprogrammeapplication.ScheduleAmendmentApplicationHasStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.TermType;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@ContextConfiguration(classes = RecordDurationChangesController.class)
class RecordDurationChangesControllerTest extends AbstractControllerTest {

  private static final String VIEW_NAME = "lms/licence/scheduleWorkProgrammeApplication/recordDurationChanges";
  private static final Long REGULATOR_WUA_ID = 1L;
  private static final ServiceUserDetail USER = ServiceUserDetailTestUtil.newBuilder()
      .withWuaId(REGULATOR_WUA_ID)
      .build();

  @MockitoBean
  private RecordDurationChangesService recordDurationChangesService;

  @MockitoBean
  private RecordDurationChangesFormValidator recordDurationChangesFormValidator;

  @Test
  void renderForm_classAnnotations_presentAndCorrect() {
    assertThat(RecordDurationChangesController.class)
        .hasAnnotation(ScheduleAmendmentApplicationHasStatus.class);
    assertThat(RecordDurationChangesController.class
        .getAnnotation(ScheduleAmendmentApplicationHasStatus.class).value())
        .containsOnly(ApplicationStatus.ISSUE_DECISION);
    assertThat(RecordDurationChangesController.class)
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
            get(ReverseRouter.route(on(RecordDurationChangesController.class)
                .renderForm(applicationDetailId, null)))
                .with(user(USER)))
        .andExpect(status().isForbidden());
  }

  @Test
  void renderForm_showsEveryTermWithItsAvailableOptionsAndDates() throws Exception {
    var applicationDetailId = UUID.randomUUID();
    var applicationDetail = buildApplicationDetail(applicationDetailId);

    setupPassingInterceptors(applicationDetail);
    when(recordDurationChangesService.getFilledForm(applicationDetail))
        .thenReturn(new RecordDurationChangesForm());
    when(recordDurationChangesService.getDurationChangeViews(applicationDetail))
        .thenReturn(List.of(
            termView(TermType.INITIAL, "31 December 2027", "4 years", false, true),
            termView(TermType.THIRD, "31 December 2049", "18 years", true, false)));

    mockMvc.perform(
            get(ReverseRouter.route(on(RecordDurationChangesController.class)
                .renderForm(applicationDetailId, null)))
                .with(user(USER)))
        .andExpect(status().isOk())
        .andExpect(view().name(VIEW_NAME))
        .andExpect(model().attribute("pageTitle", RecordDurationChangesController.PAGE_TITLE))
        .andExpect(model().attributeExists("form", "cancelUrl", "durationChangeViews"))
        .andExpect(content().string(containsString(TermType.INITIAL.getDisplayName())))
        .andExpect(content().string(containsString(TermType.THIRD.getDisplayName())))
        .andExpect(content().string(containsString("31 December 2027")))
        .andExpect(content().string(containsString("18 years")))
        .andExpect(content().string(containsString(DurationChangeType.MAINTAIN.getDisplayName())));
  }

  @Test
  void renderForm_theReduceAndExtendOptionsRevealADurationInput() throws Exception {
    var applicationDetailId = UUID.randomUUID();
    var applicationDetail = buildApplicationDetail(applicationDetailId);
    var view = termView(TermType.SECOND, "31 December 2031", "4 years", true, true);

    setupPassingInterceptors(applicationDetail);
    when(recordDurationChangesService.getFilledForm(applicationDetail))
        .thenReturn(new RecordDurationChangesForm());
    when(recordDurationChangesService.getDurationChangeViews(applicationDetail))
        .thenReturn(List.of(view));

    var body = mockMvc.perform(
            get(ReverseRouter.route(on(RecordDurationChangesController.class)
                .renderForm(applicationDetailId, null)))
                .with(user(USER)))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();

    assertThat(body).contains("data-module=\"govuk-radios\"");
    assertThat(body).contains("govuk-radios__conditional");
    assertThat(body).contains("How long is this to be reduced by?");
    assertThat(body).contains("How long is this to be extended by?");
    assertThat(body).contains("reduceDuration[" + view.id() + "].years");
    assertThat(body).contains("extendDuration[" + view.id() + "].years");
  }

  @Test
  void renderForm_offersAllThreeOptionsEvenWhenOneIsNotSuitable() throws Exception {
    var applicationDetailId = UUID.randomUUID();
    var applicationDetail = buildApplicationDetail(applicationDetailId);

    setupPassingInterceptors(applicationDetail);
    when(recordDurationChangesService.getFilledForm(applicationDetail))
        .thenReturn(new RecordDurationChangesForm());
    when(recordDurationChangesService.getDurationChangeViews(applicationDetail))
        .thenReturn(List.of(termView(TermType.INITIAL, "31 December 2027", "4 years", false, true)));

    mockMvc.perform(
            get(ReverseRouter.route(on(RecordDurationChangesController.class)
                .renderForm(applicationDetailId, null)))
                .with(user(USER)))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString(DurationChangeType.MAINTAIN.getDisplayName())))
        .andExpect(content().string(containsString(DurationChangeType.REDUCE.getDisplayName())))
        .andExpect(content().string(containsString(DurationChangeType.EXTEND.getDisplayName())));
  }

  @Test
  void submitForm_validForm_savesAndRedirectsToTaskList() throws Exception {
    var applicationDetailId = UUID.randomUUID();
    var applicationDetail = buildApplicationDetail(applicationDetailId);

    setupPassingInterceptors(applicationDetail);
    when(recordDurationChangesFormValidator.isValid(
        any(RecordDurationChangesForm.class), any(BindingResult.class), eq(applicationDetail)))
        .thenReturn(true);

    mockMvc.perform(
            post(ReverseRouter.route(on(RecordDurationChangesController.class)
                .submitForm(applicationDetailId, null, null, null)))
                .with(user(USER))
                .with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(RecordOfDecisionTaskListController.class)
            .getTaskList(applicationDetailId, null, null))));

    verify(recordDurationChangesService).saveDurationChanges(
        any(RecordDurationChangesForm.class), eq(applicationDetail));
  }

  @Test
  void submitForm_invalidForm_returnsFormAndSavesNothing() throws Exception {
    var applicationDetailId = UUID.randomUUID();
    var applicationDetail = buildApplicationDetail(applicationDetailId);
    var view = termView(TermType.INITIAL, "31 December 2027", "4 years", false, true);

    setupPassingInterceptors(applicationDetail);
    when(recordDurationChangesService.getDurationChangeViews(applicationDetail))
        .thenReturn(List.of(view));
    doAnswer(invocation -> {
      RecordDurationChangesForm form = invocation.getArgument(0);
      Errors errors = invocation.getArgument(1);
      form.getReduceDuration().put(view.id(), RecordDurationChangesForm.newReduceDurationInput(view.id()));
      form.getExtendDuration().put(view.id(), RecordDurationChangesForm.newExtendDurationInput(view.id()));
      errors.rejectValue(
          "extendDuration[%s].years".formatted(view.id()),
          "duration.total.mismatch",
          "The totals do not balance");
      return false;
    }).when(recordDurationChangesFormValidator).isValid(
        any(RecordDurationChangesForm.class), any(BindingResult.class), eq(applicationDetail));

    mockMvc.perform(
            post(ReverseRouter.route(on(RecordDurationChangesController.class)
                .submitForm(applicationDetailId, null, null, null)))
                .with(user(USER))
                .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(view().name(VIEW_NAME))
        .andExpect(content().string(containsString("The totals do not balance")));

    verify(recordDurationChangesService, never()).saveDurationChanges(
        any(RecordDurationChangesForm.class), eq(applicationDetail));
  }

  private RecordDurationChangeView termView(
      TermType termType, String endDate, String duration, boolean canReduce, boolean canExtend
  ) {
    return new RecordDurationChangeView(
        UUID.randomUUID().toString(),
        termType.getDisplayName(),
        false,
        endDate,
        duration,
        canReduce,
        canExtend);
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
