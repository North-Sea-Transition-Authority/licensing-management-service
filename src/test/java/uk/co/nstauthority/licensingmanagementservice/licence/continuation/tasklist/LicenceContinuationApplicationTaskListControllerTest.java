package uk.co.nstauthority.licensingmanagementservice.licence.continuation.tasklist;

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
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationType;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListItem;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListLabel;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListSection;
import uk.co.nstauthority.licensingmanagementservice.workarea.WorkAreaController;

@ContextConfiguration(classes = LicenceContinuationApplicationTaskListController.class)
class LicenceContinuationApplicationTaskListControllerTest extends AbstractControllerTest {

  private static final String CAPTION = "Licence type - Licence ref";
  private static final Licence LICENCE = LicenceTestUtil.builder().build();
  private static final LicenceScheduleDetail LICENCE_SCHEDULE_DETAIL
      = LicenceScheduleTestUtil.createLicenceScheduleDetail(LicenceScheduleTestUtil.createLicenceSchedule(LICENCE));
  private static final LicenceContinuationApplicationDetail LICENCE_CONTINUATION_APPLICATION_DETAIL
      = LicenceContinuationApplicationTestUtil.createLicenceContinuationApplicationDetail(LICENCE_SCHEDULE_DETAIL);
  private static final ServiceUserDetail USER = ServiceUserDetailTestUtil.newBuilder().build();

  @MockitoBean
  private LicenceContinuationApplicationTaskListService licenceContinuationApplicationTaskListService;


  @Test
  void getTaskList_assertOk() throws Exception {
    var items = List.of(new TaskListItem("display name", TaskListLabel.NOT_COMPLETE, "url"));
    var sections = List.of(new TaskListSection("Section 1", 10, items));

    when(licenceContinuationApplicationTaskListService.getAllSections(LICENCE_CONTINUATION_APPLICATION_DETAIL, USER)).thenReturn(sections);
    when(licenceContinuationService.getLicenceFromContinuationApplicationDetail(LICENCE_CONTINUATION_APPLICATION_DETAIL)).thenReturn(LICENCE);
    when(licenceService.getLicencePageCaption(LICENCE)).thenReturn(CAPTION);
    when(licenceContinuationService.getDetailByIdOrThrow(LICENCE_CONTINUATION_APPLICATION_DETAIL.getId())).thenReturn(LICENCE_CONTINUATION_APPLICATION_DETAIL);
    when(applicationAccessService.userHasAccessToApplication(
        String.valueOf(LICENCE_CONTINUATION_APPLICATION_DETAIL.getId()),
        ApplicationType.CONTINUATION_APPLICATION,
        null,
        USER.wuaId()
    )).thenReturn(true);

    mockMvc.perform(get(ReverseRouter.route(on(LicenceContinuationApplicationTaskListController.class)
            .getTaskList(LICENCE_CONTINUATION_APPLICATION_DETAIL.getId(), null, null))
        ).with(user(USER)))
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/continuation/taskList"))
        .andExpect(model().attribute("taskListSections", sections))
        .andExpect(model().attribute("pageTitle", LicenceContinuationApplicationTaskListController.PAGE_TITLE))
        .andExpect(model().attribute("pageCaption", CAPTION))
        .andExpect(model().attribute("breadcrumbs", Map.of(
            ReverseRouter.route(on(WorkAreaController.class).getWorkArea(null, null)),
            "Work area"
        )));
  }

  @ParameterizedTest
  @EnumSource(value = LicenceContinuationApplicationStatus.class, mode = EnumSource.Mode.EXCLUDE, names = "DRAFT")
  void getTaskList_assertForbiddenOnNotDraft(LicenceContinuationApplicationStatus status) throws Exception {
    var id = UUID.randomUUID();

    LICENCE_CONTINUATION_APPLICATION_DETAIL.setId(id);
    LICENCE_CONTINUATION_APPLICATION_DETAIL.setStatus(status);

    when(licenceContinuationService.getDetailByIdOrThrow(id)).thenReturn(LICENCE_CONTINUATION_APPLICATION_DETAIL);

    mockMvc.perform(get(ReverseRouter.route(on(LicenceContinuationApplicationTaskListController.class).getTaskList(
        id, null, null))).with(user(USER))).andExpect(status().isForbidden());
  }

  @Test
  void submitPage_assertForbiddenUserNoAccess() throws Exception {
    var id = UUID.randomUUID();

    LICENCE_CONTINUATION_APPLICATION_DETAIL.setId(id);

    when(licenceContinuationService.getDetailByIdOrThrow(id)).thenReturn(LICENCE_CONTINUATION_APPLICATION_DETAIL);
    when(applicationAccessService.userHasAccessToApplication(any(), any(), any(), any())).thenReturn(false);

    mockMvc.perform(get(ReverseRouter.route(on(LicenceContinuationApplicationTaskListController.class).getTaskList(
        id, null, null))).with(user(USER))).andExpect(status().isForbidden());
  }
}