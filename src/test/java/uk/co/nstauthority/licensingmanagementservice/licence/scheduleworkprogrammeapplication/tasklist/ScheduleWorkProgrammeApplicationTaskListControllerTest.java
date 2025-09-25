package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.tasklist;

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
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationTestUtil;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListItem;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListLabel;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListSection;
import uk.co.nstauthority.licensingmanagementservice.util.SecurityTest;
import uk.co.nstauthority.licensingmanagementservice.workarea.WorkAreaController;

@ContextConfiguration(classes = ScheduleWorkProgrammeApplicationTaskListController.class)
class ScheduleWorkProgrammeApplicationTaskListControllerTest extends AbstractControllerTest {

  private static final String CAPTION = "Licence type - Licence ref";
  private static final Licence LICENCE = LicenceTestUtil.builder().build();
  private static final LicenceScheduleDetail LICENCE_SCHEDULE_DETAIL =
      LicenceScheduleTestUtil.createLicenceScheduleDetail(LicenceScheduleTestUtil.createLicenceSchedule(LICENCE));
  private static final ScheduleWorkProgrammeApplicationDetail SCHEDULE_WORK_PROGRAMME_APPLICATION_DETAIL =
      ScheduleWorkProgrammeApplicationTestUtil.builder()
          .withId(UUID.randomUUID())
          .withScheduleWorkProgrammeApplication(
              ScheduleWorkProgrammeApplicationTestUtil.createScheduleWorkProgrammeApplication(LICENCE_SCHEDULE_DETAIL))
          .build();

  @MockitoBean
  private ScheduleWorkProgrammeApplicationTaskListService scheduleWorkProgrammeApplicationTaskListService;

  @SecurityTest
  void getTaskList_assertOk() throws Exception {
    var items = List.of(new TaskListItem("display name", TaskListLabel.NOT_COMPLETE, "url"));
    var sections = List.of(new TaskListSection("Section 1", 10, items));
    var id = SCHEDULE_WORK_PROGRAMME_APPLICATION_DETAIL.getId();
    when(scheduleWorkProgrammeApplicationService.getDetailByIdOrThrow(id)).thenReturn(SCHEDULE_WORK_PROGRAMME_APPLICATION_DETAIL);

    var user = ServiceUserDetailTestUtil.newBuilder().build();
    when(scheduleWorkProgrammeApplicationTaskListService.getAllSections(SCHEDULE_WORK_PROGRAMME_APPLICATION_DETAIL, user
    )).thenReturn(sections);

    when(licenceService.getLicencePageCaption(LICENCE)).thenReturn(CAPTION);

    mockMvc.perform(
        get(ReverseRouter.route(on(ScheduleWorkProgrammeApplicationTaskListController.class).getTaskList(
            id,
            SCHEDULE_WORK_PROGRAMME_APPLICATION_DETAIL,
            user
        ))
        ).with(user(user)))
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/scheduleWorkProgrammeApplication/taskList"))
        .andExpect(model().attribute("taskListSections", sections))
        .andExpect(model().attribute("pageTitle", ScheduleWorkProgrammeApplicationTaskListController.PAGE_TITLE))
        .andExpect(model().attribute("pageCaption", CAPTION))
        .andExpect(model().attribute("breadcrumbs", Map.of(
            ReverseRouter.route(on(WorkAreaController.class).getWorkArea(null, null)),
            "Work area"
        )));
  }
}
