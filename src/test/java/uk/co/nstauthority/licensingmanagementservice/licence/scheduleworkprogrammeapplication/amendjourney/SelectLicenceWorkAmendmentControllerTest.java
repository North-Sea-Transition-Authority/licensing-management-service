package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney;

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
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceSchedule;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityCommitment;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.status.WorkProgrammeStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplication;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.tasklist.ScheduleWorkProgrammeApplicationTaskListController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.workarea.WorkAreaController;

@ContextConfiguration(classes = SelectLicenceWorkAmendmentController.class)
class SelectLicenceWorkAmendmentControllerTest extends AbstractControllerTest {

  public static final String PAGE_TITLE = "What work programme activity are you requesting to amend?";

  @MockitoBean
  private SelectLicenceAmendmentFormValidator selectLicenceAmendmentFormValidator;

  @MockitoBean
  private LicenceWorkProgrammeAmendmentService licenceWorkProgrammeAmendmentService;

  private ServiceUserDetail organisationUser;
  private static final Long ORGANISATION_USER_WUA_ID = 2L;

  private static final UUID SCHEDULE_APPLICATION_DETAIL_ID = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    organisationUser = ServiceUserDetailTestUtil.newBuilder()
        .withWuaId(ORGANISATION_USER_WUA_ID)
        .build();

    var licenceSchedule = new LicenceSchedule();
    licenceSchedule.setLicence(new Licence());
    var licenceScheduleDetail = new LicenceScheduleDetail();
    licenceScheduleDetail.setLicenceSchedule(licenceSchedule);
    ScheduleWorkProgrammeApplication scheduleWorkProgrammeApplication =
        ScheduleWorkProgrammeApplicationDetailTestUtil.createScheduleWorkProgrammeApplication(licenceScheduleDetail);

    var scheduleWorkProgrammeApplicationDetail = new ScheduleWorkProgrammeApplicationDetail();
    scheduleWorkProgrammeApplicationDetail.setId(UUID.randomUUID());
    scheduleWorkProgrammeApplicationDetail.setVersionNumber(1);
    scheduleWorkProgrammeApplicationDetail.setId(SCHEDULE_APPLICATION_DETAIL_ID);
    scheduleWorkProgrammeApplicationDetail.setStatus(ScheduleWorkProgrammeApplicationStatus.DRAFT);
    scheduleWorkProgrammeApplicationDetail.setAllLicenseesPermissionConfirmed(true);
    scheduleWorkProgrammeApplicationDetail.setScheduleWorkProgrammeApplication(scheduleWorkProgrammeApplication);

    when(scheduleWorkProgrammeApplicationService.getDetailByIdOrThrow(SCHEDULE_APPLICATION_DETAIL_ID)).thenReturn(
        scheduleWorkProgrammeApplicationDetail);
  }


  @Test
  void renderSelectAmendmentForm() throws Exception {
    when(selectLicenceAmendmentFormValidator.isValid(any(), any(), any())).thenReturn(true);

    var mockWorkProgrammeActivityAmendmentViews = getMockWorkProgrammeActivityAmendmentViews();

    when(licenceScheduleService.getCurrentWorkProgrammeActivitiesViews(any())).thenReturn(mockWorkProgrammeActivityAmendmentViews);
    when(applicationAccessService.userHasAccessToApplication(any(), any(), any())).thenReturn(true);

    mockMvc.perform(
            get(ReverseRouter.route(
                on(SelectLicenceWorkAmendmentController.class).renderForm(SCHEDULE_APPLICATION_DETAIL_ID, null)))
                .with(user(organisationUser)
                )

        )
           .andExpect(status().isOk())
           .andExpect(view().name("lms/licence/scheduleWorkProgrammeApplication/selectScheduleWorkProgrammeToAmend"))
           .andExpect(model().attribute("pageTitle", PAGE_TITLE))
           .andExpect(model().attribute("workProgrammeAmendmentViews", mockWorkProgrammeActivityAmendmentViews))
           .andExpect(model().attribute("cancelUrl", (ReverseRouter.route(on(
            ScheduleWorkProgrammeApplicationTaskListController.class)
            .getTaskList(SCHEDULE_APPLICATION_DETAIL_ID, null, null)))))
           .andExpect(model().attribute("breadcrumbs", Map.of(
               ReverseRouter.route(on(WorkAreaController.class).getWorkArea(null, null)), "Work area",
               ReverseRouter.route(on(ScheduleWorkProgrammeApplicationTaskListController.class).getTaskList(SCHEDULE_APPLICATION_DETAIL_ID, null, null)), "Task list"
           )))
           .andExpect(model().attribute("currentPage", PAGE_TITLE));
  }

  @Test
  void submitValidForm() throws Exception {

    when(selectLicenceAmendmentFormValidator.isValid(any(), any(), any())).thenReturn(true);
    when(applicationAccessService.userHasAccessToApplication(any(), any(), any())).thenReturn(true);

    mockMvc.perform(
            post(ReverseRouter.route(
                on(SelectLicenceWorkAmendmentController.class).submitForm(SCHEDULE_APPLICATION_DETAIL_ID,
                    null, null, null)))
                .with(user(organisationUser)
                ).with(csrf())

        )
        .andExpect(status().is3xxRedirection());
  }


  @Test
  void submitInvalidForm() throws Exception {

    when(selectLicenceAmendmentFormValidator.isValid(any(),any(),any())).thenReturn(false);

    var mockWorkProgrammeActivityAmendmentViews = getMockWorkProgrammeActivityAmendmentViews();

    when(licenceScheduleService.getCurrentWorkProgrammeActivitiesViews(any())).thenReturn(mockWorkProgrammeActivityAmendmentViews);
    when(applicationAccessService.userHasAccessToApplication(any(), any(), any())).thenReturn(true);

    mockMvc.perform(
            post(ReverseRouter.route(
                on(SelectLicenceWorkAmendmentController.class).submitForm(SCHEDULE_APPLICATION_DETAIL_ID, null, null, null)))
                .with(user(organisationUser)
                ).with(csrf())

        )
           .andExpect(status().isOk())
           .andExpect(view().name("lms/licence/scheduleWorkProgrammeApplication/selectScheduleWorkProgrammeToAmend"))
           .andExpect(model().attribute("pageTitle", PAGE_TITLE))
           .andExpect(model().attribute("workProgrammeAmendmentViews", mockWorkProgrammeActivityAmendmentViews))
           .andExpect(model().attribute("cancelUrl", (ReverseRouter.route(on(
            ScheduleWorkProgrammeApplicationTaskListController.class)
            .getTaskList(SCHEDULE_APPLICATION_DETAIL_ID, null, null)))))
           .andExpect(model().attribute("breadcrumbs", Map.of(
               ReverseRouter.route(on(WorkAreaController.class).getWorkArea(null, null)), "Work area",
               ReverseRouter.route(on(ScheduleWorkProgrammeApplicationTaskListController.class).getTaskList(SCHEDULE_APPLICATION_DETAIL_ID, null, null)), "Task list"
           )))
           .andExpect(model().attribute("currentPage", PAGE_TITLE));

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

    mockMvc.perform(get(ReverseRouter.route(on(SelectLicenceWorkAmendmentController.class).renderForm(
        id, null))).with(user(organisationUser))).andExpect(status().isForbidden());
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

    mockMvc.perform(post(ReverseRouter.route(on(SelectLicenceWorkAmendmentController.class).submitForm(
            id, null, null, null)))
            .with(user(organisationUser))
            .with(csrf()))
        .andExpect(status().isForbidden());
  }

  @Test
  void renderPage_assertForbiddenUserNoAccess() throws Exception {
    when(applicationAccessService.userHasAccessToApplication(any(), any(), any())).thenReturn(false);

    mockMvc.perform(get(ReverseRouter.route(on(SelectLicenceWorkAmendmentController.class).renderForm(
               SCHEDULE_APPLICATION_DETAIL_ID, null)))
               .with(user(organisationUser)))
           .andExpect(status().isForbidden());
  }

  @Test
  void submitPage_assertForbiddenUserNoAccess() throws Exception {
    when(applicationAccessService.userHasAccessToApplication(any(), any(), any())).thenReturn(false);

    mockMvc.perform(post(ReverseRouter.route(on(SelectLicenceWorkAmendmentController.class).submitForm(
               SCHEDULE_APPLICATION_DETAIL_ID, null, null, null)))
               .with(user(organisationUser))
               .with(csrf()))
           .andExpect(status().isForbidden());
  }

  private List<WorkProgrammeActivityView> getMockWorkProgrammeActivityAmendmentViews() {
    WorkProgrammeActivityView mockView = new WorkProgrammeActivityView(
        UUID.randomUUID().toString(),
        "12/12/2025",
        "Category A",
        "Description 1",
        "Category A Due Date",
        WorkProgrammeActivityCommitment.FIRM.getDisplayName(),
        WorkProgrammeStatus.OPEN
    );

    return List.of(mockView);
  }
}