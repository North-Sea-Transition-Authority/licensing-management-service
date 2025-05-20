package uk.co.nstauthority.template.xyzapplication.tasklist;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import static uk.co.nstauthority.template.authentication.TestUserProvider.user;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.co.nstauthority.template.AbstractControllerTest;
import uk.co.nstauthority.template.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.template.mvc.ReverseRouter;
import uk.co.nstauthority.template.tasklist.TaskListItem;
import uk.co.nstauthority.template.tasklist.TaskListLabel;
import uk.co.nstauthority.template.tasklist.TaskListSection;
import uk.co.nstauthority.template.util.SecurityTest;
import uk.co.nstauthority.template.workarea.WorkAreaController;
import uk.co.nstauthority.template.xyzapplication.XyzApplication;
import uk.co.nstauthority.template.xyzapplication.XyzApplicationStatus;

@ContextConfiguration(classes = XyzApplicationTaskListController.class)
class XyzApplicationTaskListControllerTest extends AbstractControllerTest {

  @MockitoBean
  private XyzApplicationTaskListService xyzApplicationTaskListService;

  @SecurityTest
  void getTaskList_assertOk() throws Exception {
    var items = List.of(new TaskListItem("display name", TaskListLabel.NOT_COMPLETE, "url"));
    var sections = List.of(new TaskListSection("Section 1", 10, items));

    var application = new XyzApplication(UUID.randomUUID(), "ref", "type", XyzApplicationStatus.DRAFT);
    when(xyzApplicationService.finalAllMockedApplications()).thenReturn(List.of(application));
    var user = ServiceUserDetailTestUtil.newBuilder().build();
    when(xyzApplicationTaskListService.getAllSections(application, user)).thenReturn(sections);
    when(xyzApplicationService.findXyzApplicationById(application.getId())).thenReturn(Optional.of(application));

    mockMvc.perform(
        get(ReverseRouter.route(on(XyzApplicationTaskListController.class).getTaskList(application.getId(), null, null)))
            .with(user(user))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("xyz/application/taskList"))
        .andExpect(model().attribute("taskListSections", sections))
        .andExpect(model().attribute("pageTitle", XyzApplicationTaskListController.PAGE_TITLE))
        .andExpect(model().attribute("breadcrumbs", Map.of(
            ReverseRouter.route(on(WorkAreaController.class).getWorkArea(null, null)),
            "Work area"
        )));
  }
}
