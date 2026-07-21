package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.tasklist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import static uk.co.nstauthority.licensingmanagementservice.authentication.TestUserProvider.user;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.LogWorkAreaItemView;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDeleteController;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListItem;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListLabel;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListSection;
import uk.co.nstauthority.licensingmanagementservice.workarea.WorkAreaController;
import uk.co.nstauthority.licensingmanagementservice.workarea.workareaitemview.WorkAreaDataItemType;

@ContextConfiguration(classes = ScheduleWorkProgrammeApplicationTaskListController.class)
class ScheduleWorkProgrammeApplicationTaskListControllerTest extends AbstractControllerTest {

  private static final String CAPTION = "Licence type - Licence ref";
  private static final Licence LICENCE = LicenceTestUtil.builder().build();
  private static final LicenceScheduleDetail LICENCE_SCHEDULE_DETAIL =
      LicenceScheduleTestUtil.createLicenceScheduleDetail(LicenceScheduleTestUtil.createLicenceSchedule(LICENCE));
  private static final ScheduleWorkProgrammeApplicationDetail SCHEDULE_WORK_PROGRAMME_APPLICATION_DETAIL =
      ScheduleWorkProgrammeApplicationDetailTestUtil
          .builder()
          .withId(UUID.randomUUID())
          .withStatus(ApplicationStatus.DRAFT)
          .withScheduleWorkProgrammeApplication(
              ScheduleWorkProgrammeApplicationDetailTestUtil.createScheduleWorkProgrammeApplication(LICENCE_SCHEDULE_DETAIL))
          .build();
  private static final ServiceUserDetail USER = ServiceUserDetailTestUtil.newBuilder().build();

  @MockitoBean
  private ScheduleWorkProgrammeApplicationTaskListService scheduleWorkProgrammeApplicationTaskListService;

  @Test
  void getTaskList_assertOk() throws Exception {
    var items = List.of(new TaskListItem("display name", TaskListLabel.NOT_COMPLETE, "url"));
    var sections = List.of(new TaskListSection("Section 1", 10, items));
    var id = SCHEDULE_WORK_PROGRAMME_APPLICATION_DETAIL.getId();
    when(scheduleWorkProgrammeApplicationService.getDetailByIdOrThrow(id)).thenReturn(SCHEDULE_WORK_PROGRAMME_APPLICATION_DETAIL);


    when(scheduleWorkProgrammeApplicationTaskListService.getAllSections(SCHEDULE_WORK_PROGRAMME_APPLICATION_DETAIL, USER
    )).thenReturn(sections);

    when(licenceService.getLicencePageCaption(LICENCE)).thenReturn(CAPTION);
    when(applicationAccessService.userHasAccessToApplication(any(), any(), any())).thenReturn(true);

    mockMvc.perform(get(ReverseRouter.route(on(ScheduleWorkProgrammeApplicationTaskListController.class).getTaskList(
        id, null, null))
        ).with(user(USER)))
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/scheduleWorkProgrammeApplication/taskList"))
        .andExpect(model().attribute("taskListSections", sections))
        .andExpect(model().attribute("pageTitle", ScheduleWorkProgrammeApplicationTaskListController.PAGE_TITLE))
        .andExpect(model().attribute("pageCaption", CAPTION))
        .andExpect(model().attribute("deleteScheduleWorkProgrammeApplicationUrl", ReverseRouter.route(on(
               ScheduleWorkProgrammeApplicationDeleteController.class).renderForm(id, null))))
        .andExpect(model().attribute("breadcrumbs", Map.of(
            ReverseRouter.route(on(WorkAreaController.class).getWorkArea(null, null)),
            "Work area"
        )));
  }

  @ParameterizedTest
  @EnumSource(value = ApplicationStatus.class, mode = EnumSource.Mode.EXCLUDE, names = "DRAFT")
  void getTaskList_assertForbiddenOnNotDraft(ApplicationStatus status) throws Exception {
    var id = UUID.randomUUID();
    var submittedDetail = ScheduleWorkProgrammeApplicationDetailTestUtil
        .builder()
        .withId(id)
        .withStatus(status)
        .withScheduleWorkProgrammeApplication(
            ScheduleWorkProgrammeApplicationDetailTestUtil.createScheduleWorkProgrammeApplication(LICENCE_SCHEDULE_DETAIL))
        .build();

    when(scheduleWorkProgrammeApplicationService.getDetailByIdOrThrow(id)).thenReturn(submittedDetail);

    mockMvc.perform(get(ReverseRouter.route(on(ScheduleWorkProgrammeApplicationTaskListController.class).getTaskList(
        id, null, null))).with(user(USER))).andExpect(status().isForbidden());
  }

  @Test
  void submitPage_assertForbiddenUserNoAccess() throws Exception {
    var id = UUID.randomUUID();

    var submittedDetail = ScheduleWorkProgrammeApplicationDetailTestUtil
        .builder()
        .withStatus(ApplicationStatus.DRAFT)
        .withId(id)
        .withScheduleWorkProgrammeApplication(
            ScheduleWorkProgrammeApplicationDetailTestUtil.createScheduleWorkProgrammeApplication(LICENCE_SCHEDULE_DETAIL))
        .build();

    when(scheduleWorkProgrammeApplicationService.getDetailByIdOrThrow(id)).thenReturn(submittedDetail);
    when(applicationAccessService.userHasAccessToApplication(any(), any(), any())).thenReturn(false);

    mockMvc.perform(get(ReverseRouter.route(on(ScheduleWorkProgrammeApplicationTaskListController.class).getTaskList(
        id, null, null))).with(user(USER))).andExpect(status().isForbidden());
  }

  @Test
  void logWorkAreaItemViewAnnotation_isPresentForScheduleWorkProgrammeApplication() {
    var annotation = ScheduleWorkProgrammeApplicationTaskListController.class.getAnnotation(LogWorkAreaItemView.class);

    assertThat(annotation).isNotNull();
    assertThat(annotation.itemType()).isEqualTo(WorkAreaDataItemType.SCHEDULE_WORK_PROGRAMME_APPLICATION);
    assertThat(annotation.pathVariable()).isEqualTo("scheduleWorkProgrammeApplicationDetailId");
  }
}