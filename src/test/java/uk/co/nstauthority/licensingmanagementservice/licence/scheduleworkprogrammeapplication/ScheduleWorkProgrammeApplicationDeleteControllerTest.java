package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication;

import static org.mockito.Mockito.any;
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
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.reviewandsubmit.LicenceScheduleSummarySectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.tasklist.ScheduleWorkProgrammeApplicationTaskListController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.workarea.WorkAreaController;

@ContextConfiguration(classes = ScheduleWorkProgrammeApplicationDeleteController.class)
class ScheduleWorkProgrammeApplicationDeleteControllerTest extends AbstractControllerTest {

  @MockitoBean
  private LicenceScheduleSummarySectionService licenceScheduleSummarySectionService;

  @MockitoBean
  private ScheduleWorkProgrammeApplicationTaskListController taskListController;

  @MockitoBean
  private WorkAreaController workAreaController;

  private ServiceUserDetail organisationUser;
  private static final Long ORGANISATION_USER_WUA_ID = 2L;
  private static final UUID SCHEDULE_APPLICATION_DETAIL_ID = UUID.randomUUID();

  private ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail;

  @BeforeEach
  void setUp() {
    organisationUser = ServiceUserDetailTestUtil.newBuilder()
                                                .withWuaId(ORGANISATION_USER_WUA_ID)
                                                .build();

    ScheduleWorkProgrammeApplication scheduleWorkProgrammeApplication = new ScheduleWorkProgrammeApplication();
    scheduleWorkProgrammeApplication.setId(UUID.randomUUID());

    scheduleWorkProgrammeApplicationDetail = ScheduleWorkProgrammeApplicationDetailTestUtil
        .builder()
        .withId(SCHEDULE_APPLICATION_DETAIL_ID)
        .withStatus(ScheduleWorkProgrammeApplicationStatus.DRAFT)
        .withScheduleWorkProgrammeApplication(scheduleWorkProgrammeApplication)
        .build();

    when(scheduleWorkProgrammeApplicationService.getDetailByIdOrThrow(SCHEDULE_APPLICATION_DETAIL_ID))
        .thenReturn(scheduleWorkProgrammeApplicationDetail);
  }

  @Test
  void renderForm() throws Exception {
    when(applicationAccessService.userHasAccessToApplication(any(), any(), any(), any()))
        .thenReturn(true);

    mockMvc.perform(
               get(ReverseRouter.route(on(ScheduleWorkProgrammeApplicationDeleteController.class).renderForm(SCHEDULE_APPLICATION_DETAIL_ID, null)))
                   .with(user(organisationUser))
           )
           .andExpect(status().isOk())
           .andExpect(view().name("lms/licence/scheduleWorkProgrammeApplication/scheduleWorkProgrammeApplicationDeleteConfirmation"))
           .andExpect(model().attribute("backToTaskListUrl", ReverseRouter.route(on(ScheduleWorkProgrammeApplicationTaskListController.class).getTaskList(SCHEDULE_APPLICATION_DETAIL_ID, null, null))))
           .andExpect(model().attribute("actionUrl", ReverseRouter.route(on(ScheduleWorkProgrammeApplicationDeleteController.class).deleteScheduleWorkProgrammeApplication(SCHEDULE_APPLICATION_DETAIL_ID, null, null))))
           .andExpect(model().attributeExists("summarySections"))
           .andExpect(model().attribute("accordionId", SCHEDULE_APPLICATION_DETAIL_ID));

    verify(licenceScheduleSummarySectionService).getSummarySections(scheduleWorkProgrammeApplicationDetail, null);
  }

  @Test
  void deleteScheduleWorkProgrammeApplication() throws Exception {
    when(applicationAccessService.userHasAccessToApplication(any(), any(), any(), any()))
        .thenReturn(true);

    String expectedRedirectUrl = ReverseRouter.route(on(WorkAreaController.class).getWorkArea(null, null));

    mockMvc.perform(
        post(ReverseRouter.route(on(ScheduleWorkProgrammeApplicationDeleteController.class).deleteScheduleWorkProgrammeApplication(SCHEDULE_APPLICATION_DETAIL_ID, null, null)))
                        .with(user(organisationUser))
                        .with(csrf())
           )
           .andExpect(status().is3xxRedirection())
           .andExpect(view().name("redirect:" + expectedRedirectUrl));

    verify(scheduleWorkProgrammeApplicationService).deleteScheduleWorkProgrammeApplication(
        scheduleWorkProgrammeApplicationDetail);
  }

  @ParameterizedTest
  @EnumSource(value = ScheduleWorkProgrammeApplicationStatus.class, mode = EnumSource.Mode.EXCLUDE, names = "DRAFT")
  void renderForm_assertForbiddenOnNotDraft(ScheduleWorkProgrammeApplicationStatus status) throws Exception {
    var id = UUID.randomUUID();
    var submittedDetail = ScheduleWorkProgrammeApplicationDetailTestUtil
        .builder()
                                                                  .withId(id)
                                                                  .withStatus(status)
                                                                  .build();

    when(scheduleWorkProgrammeApplicationService.getDetailByIdOrThrow(id)).thenReturn(submittedDetail);

    mockMvc.perform(get(ReverseRouter.route(on(ScheduleWorkProgrammeApplicationDeleteController.class).renderForm(id, null)))
                        .with(user(organisationUser))
           )
           .andExpect(status().isForbidden());
  }

  @ParameterizedTest
  @EnumSource(value = ScheduleWorkProgrammeApplicationStatus.class, mode = EnumSource.Mode.EXCLUDE, names = "DRAFT")
  void deleteScheduleWorkProgrammeApplication_assertForbiddenOnNotDraft(ScheduleWorkProgrammeApplicationStatus status) throws Exception {
    var id = UUID.randomUUID();
    var submittedDetail = ScheduleWorkProgrammeApplicationDetailTestUtil
        .builder()
                                                                  .withId(id)
                                                                  .withStatus(status)
                                                                  .build();

    when(scheduleWorkProgrammeApplicationService.getDetailByIdOrThrow(id)).thenReturn(submittedDetail);

    mockMvc.perform(post(ReverseRouter.route(on(ScheduleWorkProgrammeApplicationDeleteController.class).deleteScheduleWorkProgrammeApplication(id, null, null)))
                        .with(user(organisationUser))
                        .with(csrf())
           )
           .andExpect(status().isForbidden());
  }

  @Test
  void renderPage_assertForbiddenUserNoAccess() throws Exception {
    var id = UUID.randomUUID();

    when(applicationAccessService.userHasAccessToApplication(any(), any(), any(), any())).thenReturn(false);
    when(scheduleWorkProgrammeApplicationService.getDetailByIdOrThrow(id)).thenReturn(scheduleWorkProgrammeApplicationDetail);

    mockMvc.perform(get(ReverseRouter.route(on(ScheduleWorkProgrammeApplicationDeleteController.class).renderForm(id, null)))
                     .with(user(organisationUser)))
        .andExpect(status().isForbidden());
  }

  @Test
  void deleteScheduleWorkProgrammeApplication_assertForbiddenUserNoAccess() throws Exception {
    var id = UUID.randomUUID();

    when(applicationAccessService.userHasAccessToApplication(any(), any(), any(), any())).thenReturn(false);
    when(scheduleWorkProgrammeApplicationService.getDetailByIdOrThrow(id)).thenReturn(scheduleWorkProgrammeApplicationDetail);

    mockMvc.perform(post(ReverseRouter.route(
            on(ScheduleWorkProgrammeApplicationDeleteController.class).deleteScheduleWorkProgrammeApplication(id, null, null)))
                     .with(user(organisationUser))
                     .with(csrf()))
        .andExpect(status().isForbidden());
  }
}