package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import static uk.co.nstauthority.licensingmanagementservice.authentication.TestUserProvider.user;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceSchedule;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivity;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityCommitment;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityDateOption;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.status.WorkProgrammeStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplication;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.tasklist.ScheduleWorkProgrammeApplicationTaskListController;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.tasklist.ScheduleWorkProgrammeApplicationTaskListSectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.tasklist.ScheduleWorkProgrammeApplicationTaskListService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.workarea.WorkAreaController;

@ContextConfiguration(classes = LicenceWorkProgrammeAmendmentController.class)
class LicenceWorkProgrammeAmendmentControllerTest extends AbstractControllerTest {

  @MockitoBean
  private LicenceWorkProgrammeAmendmentService licenceWorkProgrammeAmendmentService;

  @MockitoBean
  private LicenceWorkProgrammeAmendmentFormValidator licenceWorkProgrammeAmendmentFormValidator;

  @MockitoBean
  private ScheduleWorkProgrammeApplicationTaskListService scheduleWorkProgrammeApplicationTaskListService;

  @MockitoBean
  private ScheduleWorkProgrammeApplicationTaskListSectionService scheduleWorkProgrammeApplicationTaskListSectionService;

  private ServiceUserDetail organisationUser;
  private static final Long ORGANISATION_USER_WUA_ID = 2L;

  private ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail;

  private static final UUID SCHEDULE_APPLICATION_DETAIL_ID = UUID.randomUUID();

  private WorkProgrammeActivity workProgrammeActivity;

  private static final UUID WORK_PROGRAMME_ACTIVITY_ID = UUID.randomUUID();
  @Autowired
  private WorkProgrammeActivityService workProgrammeActivityService;

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

    scheduleWorkProgrammeApplicationDetail = new ScheduleWorkProgrammeApplicationDetail();
    scheduleWorkProgrammeApplicationDetail.setVersionNumber(1);
    scheduleWorkProgrammeApplicationDetail.setStatus(ScheduleWorkProgrammeApplicationStatus.DRAFT);
    scheduleWorkProgrammeApplicationDetail.setId(SCHEDULE_APPLICATION_DETAIL_ID);
    scheduleWorkProgrammeApplicationDetail.setAllLicenseesPermissionConfirmed(true);
    scheduleWorkProgrammeApplicationDetail.setScheduleWorkProgrammeApplication(scheduleWorkProgrammeApplication);

    when(scheduleWorkProgrammeApplicationService.getDetailByIdOrThrow(SCHEDULE_APPLICATION_DETAIL_ID))
        .thenReturn(scheduleWorkProgrammeApplicationDetail);

    workProgrammeActivity = new WorkProgrammeActivity();
    workProgrammeActivity.setId(WORK_PROGRAMME_ACTIVITY_ID);
    workProgrammeActivity.setDateOption(WorkProgrammeActivityDateOption.RELATIVE_DATE);

    when(workProgrammeActivityService.getWorkProgrammeActivityByIdOrThrow(WORK_PROGRAMME_ACTIVITY_ID))
        .thenReturn(workProgrammeActivity);
  }

  @Test
  void renderForm() throws Exception {
    when(licenceWorkProgrammeAmendmentService.getLicenceWorkProgrammeActivityAmendmentForm(any(), any())).thenReturn(
        new LicenceWorkProgrammeAmendmentForm());

    var mockWorkProgrammeActivityAmendmentView = getMockWorkProgrammeActivityAmendmentView();

    when(workProgrammeActivityService.createWorkProgrammeActivityView(any())).thenReturn(
        mockWorkProgrammeActivityAmendmentView);

    when(applicationAccessService.userHasAccessToApplication(any(), any(), any())).thenReturn(true);

    mockMvc.perform(
            get(ReverseRouter.route(on(LicenceWorkProgrammeAmendmentController.class).renderForm(
                    WORK_PROGRAMME_ACTIVITY_ID,
                    null,
                    SCHEDULE_APPLICATION_DETAIL_ID,
                    null
                )))
                .with(user(organisationUser))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/scheduleWorkProgrammeApplication/scheduleWorkProgrammeAmendment"))
        .andExpect(model().attribute("pageTitle", "Work programme amendments"))
        .andExpect(model().attribute("workProgrammeActivityDetails", mockWorkProgrammeActivityAmendmentView))
        .andExpect(model().attribute("isLinkedRelativeDate", true))
        .andExpect(model().attribute("cancelUrl", (ReverseRouter.route(on(LicenceWorkProgrammeAmendmentSummaryController.class)
            .renderForm(scheduleWorkProgrammeApplicationDetail.getId(),null)))))
        .andExpect(model().attribute("breadcrumbs", Map.of(
            ReverseRouter.route(on(WorkAreaController.class).getWorkArea(null, null)), "Work area",
            ReverseRouter.route(on(ScheduleWorkProgrammeApplicationTaskListController.class).getTaskList(SCHEDULE_APPLICATION_DETAIL_ID, null, null)), "Task list"
        )))
        .andExpect(model().attribute("currentPage", "Work programme amendments"));
  }

  @Test
  void submitValidForm() throws Exception {
    when(licenceWorkProgrammeAmendmentFormValidator.isValid(any(), any()))
        .thenReturn(true);
    when(applicationAccessService.userHasAccessToApplication(any(), any(), any())).thenReturn(true);

    mockMvc.perform(
            post(ReverseRouter.route(on(LicenceWorkProgrammeAmendmentController.class).submitForm(
                     WORK_PROGRAMME_ACTIVITY_ID,
                     null,
                     SCHEDULE_APPLICATION_DETAIL_ID,
                null,
                null,
                    null
                )))
             .with(user(organisationUser))
             .with(csrf())
        )
        .andExpect(status().is3xxRedirection());

    verify(licenceWorkProgrammeAmendmentService).saveAmendmentForm(any(),
        eq(scheduleWorkProgrammeApplicationDetail), any());
  }

  @Test
  void submitInvalidForm() throws Exception {
    when(licenceWorkProgrammeAmendmentService.getLicenceWorkProgrammeActivityAmendmentForm(any(), any())).thenReturn(
        new LicenceWorkProgrammeAmendmentForm());

    var mockWorkProgrammeActivityAmendmentView = getMockWorkProgrammeActivityAmendmentView();

    when(workProgrammeActivityService.createWorkProgrammeActivityView(any())).thenReturn(
        mockWorkProgrammeActivityAmendmentView);

    when(applicationAccessService.userHasAccessToApplication(any(), any(), any())).thenReturn(true);

    mockMvc.perform(
            post(ReverseRouter.route(on(LicenceWorkProgrammeAmendmentController.class).submitForm(
                    WORK_PROGRAMME_ACTIVITY_ID,
                    null,
                    SCHEDULE_APPLICATION_DETAIL_ID,
                    null,
                    null,
                    null
                )))
                .with(user(organisationUser))
                .with(csrf())
          )
          .andExpect(status().isOk())
          .andExpect(view().name("lms/licence/scheduleWorkProgrammeApplication/scheduleWorkProgrammeAmendment"))
          .andExpect(model().attribute("pageTitle", "Work programme amendments"))
          .andExpect(model().attribute("workProgrammeActivityDetails", mockWorkProgrammeActivityAmendmentView))
          .andExpect(model().attribute("isLinkedRelativeDate", true))
          .andExpect(model().attribute("cancelUrl", (ReverseRouter.route(on(LicenceWorkProgrammeAmendmentSummaryController.class)
              .renderForm(scheduleWorkProgrammeApplicationDetail.getId(),null)))))
          .andExpect(model().attribute("breadcrumbs", Map.of(
              ReverseRouter.route(on(WorkAreaController.class).getWorkArea(null, null)), "Work area",
              ReverseRouter.route(on(ScheduleWorkProgrammeApplicationTaskListController.class).getTaskList(SCHEDULE_APPLICATION_DETAIL_ID, null, null)), "Task list"
          )))
          .andExpect(model().attribute("currentPage", "Work programme amendments"));

      verify(licenceWorkProgrammeAmendmentService, never()).saveAmendmentForm(any(), any(), any());
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

    mockMvc.perform(get(ReverseRouter.route(on(LicenceWorkProgrammeAmendmentController.class).renderForm(
        UUID.randomUUID(), null, id, null))).with(user(organisationUser))).andExpect(status().isForbidden());
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

    mockMvc.perform(post(ReverseRouter.route(on(LicenceWorkProgrammeAmendmentController.class).submitForm(
            UUID.randomUUID(), null, id, null, null, null)))
            .with(user(organisationUser))
            .with(csrf()))
        .andExpect(status().isForbidden());
  }

  @Test
  void renderPage_assertForbiddenUserNoAccess() throws Exception {
    var activityId = UUID.randomUUID();

    when(applicationAccessService.userHasAccessToApplication(any(), any(), any())).thenReturn(false);

    mockMvc.perform(get(ReverseRouter.route(on(LicenceWorkProgrammeAmendmentController.class).renderForm(
        activityId, null, SCHEDULE_APPLICATION_DETAIL_ID, null)))
               .with(user(organisationUser)))
           .andExpect(status().isForbidden());
  }

  @Test
  void submitPage_assertForbiddenUserNoAccess() throws Exception {
    var activityId = UUID.randomUUID();

    when(applicationAccessService.userHasAccessToApplication(any(), any(), any())).thenReturn(false);

    mockMvc.perform(post(ReverseRouter.route(on(LicenceWorkProgrammeAmendmentController.class).submitForm(
        activityId, null, SCHEDULE_APPLICATION_DETAIL_ID, null, null, null)))
               .with(user(organisationUser))
               .with(csrf()))
           .andExpect(status().isForbidden());
  }

  private WorkProgrammeActivityView getMockWorkProgrammeActivityAmendmentView() {
    return new WorkProgrammeActivityView(
        UUID.randomUUID().toString(),
        "12/12/2025",
        "Category A",
        "Description 1",
        "Category A Due Date",
        WorkProgrammeActivityCommitment.FIRM.getDisplayName(),
        WorkProgrammeStatus.OPEN
    );
  }
  }