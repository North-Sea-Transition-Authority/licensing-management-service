package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
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
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityCategory;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityCommitment;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.status.WorkProgrammeStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney.WorkProgrammeActivityView;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@ContextConfiguration(classes = SelectWorkProgrammeActivityController.class)
class SelectWorkProgrammeActivityControllerTest extends AbstractControllerTest {

  private static final String VIEW_NAME = "lms/licence/scheduleWorkProgrammeApplication/selectWorkProgrammeActivity";
  private static final Long REGULATOR_WUA_ID = 1L;
  private static final ServiceUserDetail USER = ServiceUserDetailTestUtil.newBuilder()
      .withWuaId(REGULATOR_WUA_ID)
      .build();

  @MockitoBean
  private RecordWorkProgrammeAmendmentDetailsService recordWorkProgrammeAmendmentDetailsService;

  @MockitoBean
  private SelectWorkProgrammeActivityFormValidator selectWorkProgrammeActivityFormValidator;

  @Test
  void renderForm_classAnnotations_presentAndCorrect() {
    assertThat(SelectWorkProgrammeActivityController.class)
        .hasAnnotation(ScheduleAmendmentApplicationHasStatus.class);
    assertThat(SelectWorkProgrammeActivityController.class
        .getAnnotation(ScheduleAmendmentApplicationHasStatus.class).value())
        .containsOnly(ApplicationStatus.ISSUE_DECISION);
    assertThat(SelectWorkProgrammeActivityController.class)
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
            get(ReverseRouter.route(on(SelectWorkProgrammeActivityController.class)
                .renderForm(applicationDetailId, null)))
                .with(user(USER)))
        .andExpect(status().isForbidden());
  }

  @Test
  void renderForm_withAccess_returnsOk() throws Exception {
    var applicationDetailId = UUID.randomUUID();
    var applicationDetail = buildApplicationDetail(applicationDetailId);

    setupPassingInterceptors(applicationDetail);
    when(recordWorkProgrammeAmendmentDetailsService.getSelectableActivityViews(applicationDetail))
        .thenReturn(List.of(new WorkProgrammeActivityView(
            UUID.randomUUID().toString(),
            "27 July 2026",
            WorkProgrammeActivityCategory.DRILL_WELL.getDisplayName(),
            "Drill well to 3,000m",
            "%s due by 27 July 2026".formatted(WorkProgrammeActivityCategory.DRILL_WELL.getDisplayName()),
            WorkProgrammeActivityCommitment.FIRM.getDisplayName(),
            WorkProgrammeStatus.OPEN)));

    mockMvc.perform(
            get(ReverseRouter.route(on(SelectWorkProgrammeActivityController.class)
                .renderForm(applicationDetailId, null)))
                .with(user(USER)))
        .andExpect(status().isOk())
        .andExpect(view().name(VIEW_NAME))
        .andExpect(model().attribute("pageTitle", SelectWorkProgrammeActivityController.PAGE_TITLE))
        .andExpect(model().attributeExists("form", "cancelUrl", "workProgrammeActivityViews"))
        .andExpect(content().string(containsString("Drill well to 3,000m")));
  }

  @Test
  void renderForm_whenEveryActivityAlreadyDecided_showsAllActionedMessageAndNoSubmitButton() throws Exception {
    var applicationDetailId = UUID.randomUUID();
    var applicationDetail = buildApplicationDetail(applicationDetailId);

    setupPassingInterceptors(applicationDetail);
    when(recordWorkProgrammeAmendmentDetailsService.getSelectableActivityViews(applicationDetail))
        .thenReturn(List.of());
    when(recordWorkProgrammeAmendmentDetailsService.hasAmendmentDetails(applicationDetail))
        .thenReturn(true);

    mockMvc.perform(
            get(ReverseRouter.route(on(SelectWorkProgrammeActivityController.class)
                .renderForm(applicationDetailId, null)))
                .with(user(USER)))
        .andExpect(status().isOk())
        .andExpect(view().name(VIEW_NAME))
        .andExpect(content().string(containsString(
            "A decision has been recorded against every work programme activity")))
        .andExpect(content().string(containsString("Return to task list")))
        .andExpect(content().string(not(containsString("Save and continue"))));
  }

  @Test
  void renderForm_whenScheduleHasNoActivities_showsNoActivitiesMessageAndNoSubmitButton() throws Exception {
    var applicationDetailId = UUID.randomUUID();
    var applicationDetail = buildApplicationDetail(applicationDetailId);

    setupPassingInterceptors(applicationDetail);
    when(recordWorkProgrammeAmendmentDetailsService.getSelectableActivityViews(applicationDetail))
        .thenReturn(List.of());
    when(recordWorkProgrammeAmendmentDetailsService.hasAmendmentDetails(applicationDetail))
        .thenReturn(false);

    mockMvc.perform(
            get(ReverseRouter.route(on(SelectWorkProgrammeActivityController.class)
                .renderForm(applicationDetailId, null)))
                .with(user(USER)))
        .andExpect(status().isOk())
        .andExpect(view().name(VIEW_NAME))
        .andExpect(content().string(containsString(
            "There are no work programme activities on this licence schedule")))
        .andExpect(content().string(containsString("Return to task list")))
        .andExpect(content().string(not(containsString("Save and continue"))));
  }

  @Test
  void submitForm_validForm_redirectsToTheAmendmentDetailsPage() throws Exception {
    var applicationDetailId = UUID.randomUUID();
    var applicationDetail = buildApplicationDetail(applicationDetailId);
    var activityId = UUID.randomUUID();

    setupPassingInterceptors(applicationDetail);
    doAnswer(invocation -> {
      SelectWorkProgrammeActivityForm form = invocation.getArgument(0);
      form.setWorkProgrammeActivityId(activityId.toString());
      return true;
    }).when(selectWorkProgrammeActivityFormValidator)
        .isValid(any(SelectWorkProgrammeActivityForm.class), any(BindingResult.class), eq(applicationDetail));

    mockMvc.perform(
            post(ReverseRouter.route(on(SelectWorkProgrammeActivityController.class)
                .submitForm(applicationDetailId, null, null, null)))
                .with(user(USER))
                .with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(RecordWorkProgrammeAmendmentDetailsController.class)
            .renderForm(applicationDetailId, activityId, null, null))));
  }

  @Test
  void submitForm_invalidForm_returnsForm() throws Exception {
    var applicationDetailId = UUID.randomUUID();
    var applicationDetail = buildApplicationDetail(applicationDetailId);

    setupPassingInterceptors(applicationDetail);
    when(recordWorkProgrammeAmendmentDetailsService.getSelectableActivityViews(applicationDetail))
        .thenReturn(List.of());
    doAnswer(invocation -> {
      Errors errors = invocation.getArgument(1);
      errors.rejectValue(
          "workProgrammeActivityId",
          "workProgrammeActivityId.required",
          SelectWorkProgrammeActivityFormValidator.REQUIRED_ERROR_MESSAGE);
      return false;
    }).when(selectWorkProgrammeActivityFormValidator)
        .isValid(any(SelectWorkProgrammeActivityForm.class), any(BindingResult.class), eq(applicationDetail));

    mockMvc.perform(
            post(ReverseRouter.route(on(SelectWorkProgrammeActivityController.class)
                .submitForm(applicationDetailId, null, null, null)))
                .with(user(USER))
                .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(view().name(VIEW_NAME))
        .andExpect(content().string(
            containsString(SelectWorkProgrammeActivityFormValidator.REQUIRED_ERROR_MESSAGE)));
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
