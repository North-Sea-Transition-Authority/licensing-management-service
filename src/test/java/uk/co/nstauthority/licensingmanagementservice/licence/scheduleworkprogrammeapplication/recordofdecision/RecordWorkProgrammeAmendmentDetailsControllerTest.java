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

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
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
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceSchedule;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivity;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityCategory;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityCommitment;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.status.WorkProgrammeStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney.WorkProgrammeActivityView;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@ContextConfiguration(classes = RecordWorkProgrammeAmendmentDetailsController.class)
class RecordWorkProgrammeAmendmentDetailsControllerTest extends AbstractControllerTest {

  private static final String VIEW_NAME =
      "lms/licence/scheduleWorkProgrammeApplication/recordWorkProgrammeAmendmentDetails";
  private static final Long REGULATOR_WUA_ID = 1L;
  private static final ServiceUserDetail USER = ServiceUserDetailTestUtil.newBuilder()
      .withWuaId(REGULATOR_WUA_ID)
      .build();

  @MockitoBean
  private RecordWorkProgrammeAmendmentDetailsService recordWorkProgrammeAmendmentDetailsService;

  @MockitoBean
  private RecordWorkProgrammeAmendmentDetailsFormValidator recordWorkProgrammeAmendmentDetailsFormValidator;

  private UUID applicationDetailId;
  private UUID activityId;
  private ScheduleWorkProgrammeApplicationDetail applicationDetail;
  private WorkProgrammeActivity workProgrammeActivity;

  @BeforeEach
  void setUp() {
    applicationDetailId = UUID.randomUUID();
    activityId = UUID.randomUUID();
    applicationDetail = ScheduleWorkProgrammeApplicationDetailTestUtil.builder()
        .withId(applicationDetailId)
        .withStatus(ApplicationStatus.ISSUE_DECISION)
        .build();
    workProgrammeActivity = buildActivity();
  }

  @Test
  void renderForm_classAnnotations_presentAndCorrect() {
    assertThat(RecordWorkProgrammeAmendmentDetailsController.class)
        .hasAnnotation(ScheduleAmendmentApplicationHasStatus.class);
    assertThat(RecordWorkProgrammeAmendmentDetailsController.class
        .getAnnotation(ScheduleAmendmentApplicationHasStatus.class).value())
        .containsOnly(ApplicationStatus.ISSUE_DECISION);
    assertThat(RecordWorkProgrammeAmendmentDetailsController.class)
        .hasAnnotation(InvokingUserCanAccessScheduleApplication.class);
  }

  @Test
  void renderForm_noApplicationAccess_returnsForbidden() throws Exception {
    when(scheduleWorkProgrammeApplicationService.getDetailByIdOrThrow(applicationDetailId))
        .thenReturn(applicationDetail);
    when(applicationAccessService.userHasAccessToApplication(eq(applicationDetail), anyMap(), eq(REGULATOR_WUA_ID)))
        .thenReturn(false);

    mockMvc.perform(
            get(ReverseRouter.route(on(RecordWorkProgrammeAmendmentDetailsController.class)
                .renderForm(applicationDetailId, activityId, null, null)))
                .with(user(USER)))
        .andExpect(status().isForbidden());
  }

  @Test
  void renderForm_withAccess_returnsOk() throws Exception {
    setupPassingInterceptors();
    when(recordWorkProgrammeAmendmentDetailsService.getFilledForm(applicationDetail, workProgrammeActivity))
        .thenReturn(new RecordWorkProgrammeAmendmentDetailsForm());
    mockPageObjects();

    mockMvc.perform(
            get(ReverseRouter.route(on(RecordWorkProgrammeAmendmentDetailsController.class)
                .renderForm(applicationDetailId, activityId, null, null)))
                .with(user(USER)))
        .andExpect(status().isOk())
        .andExpect(view().name(VIEW_NAME))
        .andExpect(model().attribute("pageTitle", RecordWorkProgrammeAmendmentDetailsController.PAGE_TITLE))
        .andExpect(model().attributeExists(
            "form", "cancelUrl", "workProgrammeActivityDetails", "decisionOptions", "targetLicences", "searchUrl"))
        .andExpect(content().string(containsString("Drill well to 3,000m")))
        .andExpect(content().string(containsString("27 July 2026")));
  }

  @Test
  void renderForm_showsEveryDecisionOption() throws Exception {
    setupPassingInterceptors();
    when(recordWorkProgrammeAmendmentDetailsService.getFilledForm(applicationDetail, workProgrammeActivity))
        .thenReturn(new RecordWorkProgrammeAmendmentDetailsForm());
    mockPageObjects();

    var result = mockMvc.perform(
            get(ReverseRouter.route(on(RecordWorkProgrammeAmendmentDetailsController.class)
                .renderForm(applicationDetailId, activityId, null, null)))
                .with(user(USER)))
        .andExpect(status().isOk())
        .andReturn();

    var body = result.getResponse().getContentAsString();
    for (var decision : WorkProgrammeAmendmentDecision.values()) {
      assertThat(body).contains(decision.getDisplayName());
    }
  }

  @Test
  void submitForm_validForm_savesAndRedirectsToTaskList() throws Exception {
    setupPassingInterceptors();
    when(recordWorkProgrammeAmendmentDetailsFormValidator.isValid(
        any(RecordWorkProgrammeAmendmentDetailsForm.class), any(BindingResult.class)))
        .thenReturn(true);

    mockMvc.perform(
            post(ReverseRouter.route(on(RecordWorkProgrammeAmendmentDetailsController.class)
                .submitForm(applicationDetailId, activityId, null, null, null, null)))
                .with(user(USER))
                .with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(RecordOfDecisionTaskListController.class)
            .getTaskList(applicationDetailId, null, null))));

    verify(recordWorkProgrammeAmendmentDetailsService).saveAmendmentDetails(
        any(RecordWorkProgrammeAmendmentDetailsForm.class), eq(applicationDetail), eq(workProgrammeActivity));
  }

  @Test
  void submitForm_invalidForm_returnsFormWithError() throws Exception {
    setupPassingInterceptors();
    mockPageObjects();
    doAnswer(invocation -> {
      Errors errors = invocation.getArgument(1);
      errors.rejectValue(
          "decision",
          "decision.required",
          RecordWorkProgrammeAmendmentDetailsFormValidator.DECISION_REQUIRED_ERROR_MESSAGE);
      return false;
    }).when(recordWorkProgrammeAmendmentDetailsFormValidator)
        .isValid(any(RecordWorkProgrammeAmendmentDetailsForm.class), any(BindingResult.class));

    mockMvc.perform(
            post(ReverseRouter.route(on(RecordWorkProgrammeAmendmentDetailsController.class)
                .submitForm(applicationDetailId, activityId, null, null, null, null)))
                .with(user(USER))
                .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(view().name(VIEW_NAME))
        .andExpect(content().string(containsString(
            RecordWorkProgrammeAmendmentDetailsFormValidator.DECISION_REQUIRED_ERROR_MESSAGE)))
        .andExpect(content().string(containsString("govuk-error-message")));
  }

  private void mockPageObjects() {
    when(workProgrammeActivityService.createWorkProgrammeActivityView(workProgrammeActivity))
        .thenReturn(new WorkProgrammeActivityView(
            activityId.toString(),
            "27 July 2026",
            WorkProgrammeActivityCategory.DRILL_WELL.getDisplayName(),
            "Drill well to 3,000m",
            "%s due by 27 July 2026".formatted(WorkProgrammeActivityCategory.DRILL_WELL.getDisplayName()),
            WorkProgrammeActivityCommitment.FIRM.getDisplayName(),
            WorkProgrammeStatus.OPEN));
    when(recordWorkProgrammeAmendmentDetailsService.getTargetLicenceSelections(List.of()))
        .thenReturn(List.of());
  }

  private void setupPassingInterceptors() {
    when(scheduleWorkProgrammeApplicationService.getDetailByIdOrThrow(applicationDetailId))
        .thenReturn(applicationDetail);
    when(applicationAccessService.userHasAccessToApplication(
        eq(applicationDetail), anyMap(), eq(REGULATOR_WUA_ID)))
        .thenReturn(true);
    when(workProgrammeActivityService.getWorkProgrammeActivityByIdOrThrow(activityId))
        .thenReturn(workProgrammeActivity);
  }

  private WorkProgrammeActivity buildActivity() {
    var licence = LicenceTestUtil.builder()
        .withId(21)
        .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
        .withLicenceReference("P123")
        .build();

    var licenceSchedule = new LicenceSchedule();
    licenceSchedule.setLicence(licence);

    var licenceScheduleDetail = new LicenceScheduleDetail();
    licenceScheduleDetail.setLicenceSchedule(licenceSchedule);

    var activity = new WorkProgrammeActivity();
    activity.setId(activityId);
    activity.setLicenceScheduleDetail(licenceScheduleDetail);
    activity.setDescription("Drill well to 3,000m");
    activity.setCategory(WorkProgrammeActivityCategory.DRILL_WELL);
    activity.setCommitment(WorkProgrammeActivityCommitment.FIRM);
    activity.setDueDate(LocalDate.of(2026, 7, 27));
    return activity;
  }
}
