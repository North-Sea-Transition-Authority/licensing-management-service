package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.reviewandsubmit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import static uk.co.nstauthority.licensingmanagementservice.authentication.TestUserProvider.user;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.ResultActions;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.feedback.FeedbackController;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.tasklist.ScheduleWorkProgrammeApplicationTaskListController;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.tasklist.ScheduleWorkProgrammeApplicationTaskListService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.summary.SummarySection;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.util.SecurityTest;
import uk.co.nstauthority.licensingmanagementservice.workarea.WorkAreaController;

@ContextConfiguration(classes = ScheduleAmendmentApplicationReviewAndSubmitController.class)
class ScheduleAmendmentApplicationReviewAndSubmitControllerTest extends AbstractControllerTest {

  private static final String CAPTION = "Licence type - Licence ref";
  private static final ServiceUserDetail USER = ServiceUserDetailTestUtil.newBuilder().build();

  @MockitoBean
  LicenceScheduleSummarySectionService licenceScheduleSummarySectionService;

  @MockitoBean
  ScheduleWorkProgrammeApplicationTaskListService scheduleWorkProgrammeApplicationTaskListService;

  private ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail;
  private static final UUID SCHEDULE_APPLICATION_DETAIL_ID = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    Licence licence = LicenceTestUtil.builder().build();
    LicenceScheduleDetail licenceScheduleDetail = LicenceScheduleTestUtil.createLicenceScheduleDetail(LicenceScheduleTestUtil.createLicenceSchedule(licence));
    scheduleWorkProgrammeApplicationDetail = ScheduleWorkProgrammeApplicationDetailTestUtil
        .builder()
        .withId(SCHEDULE_APPLICATION_DETAIL_ID)
        .withStatus(ScheduleWorkProgrammeApplicationStatus.DRAFT)
        .withScheduleWorkProgrammeApplication(
            ScheduleWorkProgrammeApplicationDetailTestUtil.createScheduleWorkProgrammeApplication(licenceScheduleDetail))
        .build();

    when(scheduleWorkProgrammeApplicationService.getDetailByIdOrThrow(SCHEDULE_APPLICATION_DETAIL_ID)).thenReturn(scheduleWorkProgrammeApplicationDetail);
  }

  @Test
  void getReviewAndSubmit() throws Exception {
    var applicationDetailId = scheduleWorkProgrammeApplicationDetail.getId();
    mockScheduleWorkProgrammeApplicationDetailScenario(applicationDetailId);
    when(licenceService.getLicencePageCaption(any())).thenReturn(CAPTION);

    var resultActions = mockMvc
        .perform(get(ReverseRouter.route(on(ScheduleAmendmentApplicationReviewAndSubmitController.class).getReviewAndSubmit(
            applicationDetailId,
            null,
            null
        )))
            .with(user(USER)))
        .andExpect(status().isOk());

    assertRenderPageModelsAttributesArePresent(resultActions, applicationDetailId);
  }

  @Test
  void getReviewAndSubmit_UserCantSubmit() throws Exception {
    var applicationDetailId = scheduleWorkProgrammeApplicationDetail.getId();
    mockScheduleWorkProgrammeApplicationDetailScenario(applicationDetailId);
    when(licenceService.getLicencePageCaption(any())).thenReturn(CAPTION);
    when(scheduleWorkProgrammeApplicationService.userCanSubmitApplication(scheduleWorkProgrammeApplicationDetail, USER)).thenReturn(false);

    var resultActions = mockMvc
        .perform(get(ReverseRouter.route(on(ScheduleAmendmentApplicationReviewAndSubmitController.class).getReviewAndSubmit(
            applicationDetailId,
            null,
            null
        )))
            .with(user(USER)))
        .andExpect(status().isOk());

    assertRenderPageModelsAttributesArePresent(resultActions, applicationDetailId);
    resultActions
        .andExpect(model().attribute("userCanSubmit", false));
  }

  @Test
  void submitApplication_notSubmittable() throws Exception {
    var applicationDetailId = scheduleWorkProgrammeApplicationDetail.getId();
    mockScheduleWorkProgrammeApplicationDetailScenario(applicationDetailId);
    when(licenceService.getLicencePageCaption(any())).thenReturn(CAPTION);

    when(scheduleWorkProgrammeApplicationTaskListService.isSubmittable(any(), any())).thenReturn(false);
    var resultActions = mockMvc
        .perform(post(ReverseRouter.route(on(ScheduleAmendmentApplicationReviewAndSubmitController.class)
            .submitApplication(applicationDetailId, null, null, null)))
            .with(user(USER))
            .with(csrf()))
        .andExpect(status().isOk());

    assertRenderPageModelsAttributesArePresent(resultActions, applicationDetailId);
    resultActions
        .andExpect(model().attribute("isSubmittable", false));
  }

  private void assertRenderPageModelsAttributesArePresent(ResultActions resultActions, UUID id) throws Exception {
    resultActions
        .andExpect(view().name("lms/licence/scheduleWorkProgrammeApplication/reviewAndSubmit"))
        .andExpect(model().attribute("cancelUrl", ReverseRouter.route(on(ScheduleWorkProgrammeApplicationTaskListController.class).getTaskList(
            id,
            null,
            null
        ))))
        .andExpect(model().attribute("pageCaption", CAPTION))
        .andExpect(model().attribute("summarySections", licenceScheduleSummarySectionService.getSummarySections(
            scheduleWorkProgrammeApplicationDetail,
            null
        )))
        .andExpect(model().attribute("accordionId", scheduleWorkProgrammeApplicationDetail.getId()))
        .andExpect(model().attributeExists("isSubmittable"))
        .andExpect(model().attributeExists("userCanSubmit"))
        .andExpect(model().attribute("submitterRoleName", Role.APPLICATION_SUBMITTER.getName()));
  }

  private void mockScheduleWorkProgrammeApplicationDetailScenario(UUID applicationDetailId) {
    when(scheduleWorkProgrammeApplicationService.getDetailByIdOrThrow(applicationDetailId)).thenReturn(scheduleWorkProgrammeApplicationDetail);
    when(licenceScheduleSummarySectionService.getSummarySections(any(), any())).thenReturn(List.of(new SummarySection(1, List.of())));
    when(applicationAccessService.userHasAccessToApplication(any(), any(), any(), any())).thenReturn(true);
  }

  @Test
  void submitApplication_submittable() throws Exception {
    var applicationDetailId = scheduleWorkProgrammeApplicationDetail.getId();
    var application = scheduleWorkProgrammeApplicationDetail.getScheduleWorkProgrammeApplication();
    application.setApplicationReference("APP-REF-123");
    mockScheduleWorkProgrammeApplicationDetailScenario(applicationDetailId);
    when(scheduleWorkProgrammeApplicationService.submitApplication(any(), any())).thenReturn(application);

    when(scheduleWorkProgrammeApplicationTaskListService.isSubmittable(any(), any())).thenReturn(true);

    mockMvc.perform(post(ReverseRouter.route(on(ScheduleAmendmentApplicationReviewAndSubmitController.class)
                .submitApplication(applicationDetailId, null, null, null)))
            .with(user(USER))
            .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/application/submissionConfirmation"))
        .andExpect(model().attribute("feedbackUrl", ReverseRouter.route(on(FeedbackController.class).getFeedback(null))))
        .andExpect(model().attribute("workAreaUrl", ReverseRouter.route(on(WorkAreaController.class).getWorkArea(null, null))))
        .andExpect(model().attribute("applicationReference", application.getApplicationReference()));
  }

  @ParameterizedTest
  @EnumSource(value = ScheduleWorkProgrammeApplicationStatus.class, mode = EnumSource.Mode.EXCLUDE, names = "DRAFT")
  void renderPage_assertForbiddenOnNotDraft(ScheduleWorkProgrammeApplicationStatus status) throws Exception {
    var id = UUID.randomUUID();
    var submittedDetail = ScheduleWorkProgrammeApplicationDetailTestUtil
        .builder()
        .withId(id)
        .withStatus(status)
        .build();

    when(scheduleWorkProgrammeApplicationService.getDetailByIdOrThrow(id)).thenReturn(submittedDetail);
    when(applicationAccessService.userHasAccessToApplication(any(), any(), any(), any())).thenReturn(true);

    mockMvc.perform(get(ReverseRouter.route(on(ScheduleAmendmentApplicationReviewAndSubmitController.class).getReviewAndSubmit(
        id, null, null))).with(user(USER))).andExpect(status().isForbidden());
  }

  @ParameterizedTest
  @EnumSource(value = ScheduleWorkProgrammeApplicationStatus.class, mode = EnumSource.Mode.EXCLUDE, names = "DRAFT")
  void submitPage_assertForbiddenOnNotDraft(ScheduleWorkProgrammeApplicationStatus status) throws Exception {
    var id = UUID.randomUUID();
    var submittedDetail = ScheduleWorkProgrammeApplicationDetailTestUtil
        .builder()
        .withId(id)
        .withStatus(status)
        .build();

    when(scheduleWorkProgrammeApplicationService.getDetailByIdOrThrow(id)).thenReturn(submittedDetail);

    mockMvc.perform(post(ReverseRouter.route(on(ScheduleAmendmentApplicationReviewAndSubmitController.class).submitApplication(
        id, null, null, null)))
            .with(user(USER))
            .with(csrf()))
        .andExpect(status().isForbidden());
  }

  @SecurityTest
  void renderPage_assertForbiddenUserNoAccess() throws Exception {
    when(applicationAccessService.userHasAccessToApplication(any(), any(), any(), any())).thenReturn(false);
    mockMvc.perform(get(ReverseRouter.route(on(ScheduleAmendmentApplicationReviewAndSubmitController.class).getReviewAndSubmit(
            SCHEDULE_APPLICATION_DETAIL_ID, null, null)))
               .with(user(USER)))
           .andExpect(status().isForbidden());
  }

  @SecurityTest
  void submitPage_assertForbiddenUserNoAccess() throws Exception {
    when(applicationAccessService.userHasAccessToApplication(any(), any(), any(), any())).thenReturn(false);
    mockMvc.perform(post(ReverseRouter.route(on(ScheduleAmendmentApplicationReviewAndSubmitController.class).submitApplication(
            SCHEDULE_APPLICATION_DETAIL_ID, null, null, null)))
               .with(user(USER))
               .with(csrf()))
           .andExpect(status().isForbidden());
  }
}