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
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.feedback.FeedbackController;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.tasklist.ScheduleWorkProgrammeApplicationTaskListController;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.tasklist.ScheduleWorkProgrammeApplicationTaskListService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.summary.SummarySection;
import uk.co.nstauthority.licensingmanagementservice.util.SecurityTest;
import uk.co.nstauthority.licensingmanagementservice.workarea.WorkAreaController;

@ContextConfiguration(classes = LicenceScheduleReviewAndSubmitController.class)
class LicenceScheduleReviewAndSubmitControllerTest extends AbstractControllerTest {

  private static final String CAPTION = "Licence type - Licence ref";

  @MockitoBean
  LicenceScheduleSummarySectionService licenceScheduleSummarySectionService;

  @MockitoBean
  ScheduleWorkProgrammeApplicationTaskListService scheduleWorkProgrammeApplicationTaskListService;

  private ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail;

  @BeforeEach
  void setUp() {
    Licence licence = LicenceTestUtil.builder().build();
    LicenceScheduleDetail licenceScheduleDetail = LicenceScheduleTestUtil.createLicenceScheduleDetail(LicenceScheduleTestUtil.createLicenceSchedule(licence));
    scheduleWorkProgrammeApplicationDetail = ScheduleWorkProgrammeApplicationTestUtil
        .builder()
        .withId(UUID.randomUUID())
        .withScheduleWorkProgrammeApplication(
            ScheduleWorkProgrammeApplicationTestUtil.createScheduleWorkProgrammeApplication(licenceScheduleDetail))
        .build();
  }

  @SecurityTest
  void getReviewAndSubmit() throws Exception {

    var id = scheduleWorkProgrammeApplicationDetail.getId();

    when(scheduleWorkProgrammeApplicationService.getDetailByIdOrThrow(id)).thenReturn(scheduleWorkProgrammeApplicationDetail);
    when(licenceService.getLicencePageCaption(any())).thenReturn(CAPTION);
    when(licenceScheduleSummarySectionService.getSummarySections(any(), any())).thenReturn(List.of(new SummarySection(1, List.of())));
    mockMvc
        .perform(get(ReverseRouter.route(on(LicenceScheduleReviewAndSubmitController.class).getReviewAndSubmit(
            id,
            null,
            null
        )))
            .with(user(ServiceUserDetailTestUtil.newBuilder().build()))
            .with(csrf()))
        .andExpect(status().isOk())
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
        .andExpect(model().attribute("accordionId", scheduleWorkProgrammeApplicationDetail.getId()));
  }

  @SecurityTest
  void submitApplication_notSubmittable() throws Exception {
    var id = scheduleWorkProgrammeApplicationDetail.getId();

    when(scheduleWorkProgrammeApplicationService.getDetailByIdOrThrow(id)).thenReturn(scheduleWorkProgrammeApplicationDetail);
    when(licenceService.getLicencePageCaption(any())).thenReturn(CAPTION);
    when(licenceScheduleSummarySectionService.getSummarySections(any(), any())).thenReturn(List.of(new SummarySection(1, List.of())));
    when(scheduleWorkProgrammeApplicationTaskListService.isSubmittable(any(), any())).thenReturn(false);

    mockMvc
        .perform(post(ReverseRouter.route(on(LicenceScheduleReviewAndSubmitController.class)
            .submitApplication(id, null, null, null)))
            .with(user(ServiceUserDetailTestUtil.newBuilder().build()))
            .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/scheduleWorkProgrammeApplication/reviewAndSubmit"))
        .andExpect(model().attribute("cancelUrl", ReverseRouter.route(on(ScheduleWorkProgrammeApplicationTaskListController.class)
            .getTaskList(id, null, null))))
        .andExpect(model().attribute("pageCaption", CAPTION))
        .andExpect(model().attribute("summarySections", licenceScheduleSummarySectionService.getSummarySections(
            scheduleWorkProgrammeApplicationDetail,
            null
        )))
        .andExpect(model().attribute("accordionId", scheduleWorkProgrammeApplicationDetail.getId()))
        .andExpect(model().attribute("isSubmittable", false));
  }

  @SecurityTest
  void submitApplication_submittable() throws Exception {
    var id = scheduleWorkProgrammeApplicationDetail.getId();
    var application = scheduleWorkProgrammeApplicationDetail.getScheduleWorkProgrammeApplication();
    application.setApplicationReference("APP-REF-123");

    when(scheduleWorkProgrammeApplicationService.getDetailByIdOrThrow(id)).thenReturn(scheduleWorkProgrammeApplicationDetail);
    when(scheduleWorkProgrammeApplicationTaskListService.isSubmittable(any(), any())).thenReturn(true);
    when(scheduleWorkProgrammeApplicationService.submitApplication(any(), any())).thenReturn(application);

    mockMvc.perform(post(ReverseRouter.route(on(LicenceScheduleReviewAndSubmitController.class)
                .submitApplication(id, null, null, null)))
            .with(user(ServiceUserDetailTestUtil.newBuilder().build()))
            .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/scheduleWorkProgrammeApplication/submissionConfirmation"))
        .andExpect(model().attribute("feedbackUrl", ReverseRouter.route(on(FeedbackController.class).getFeedback(null))))
        .andExpect(model().attribute("workAreaUrl", ReverseRouter.route(on(WorkAreaController.class).getWorkArea(null, null))))
        .andExpect(model().attribute("applicationReference", application.getApplicationReference()));
  }
}