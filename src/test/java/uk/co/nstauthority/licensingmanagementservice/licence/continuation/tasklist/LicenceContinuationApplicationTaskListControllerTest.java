package uk.co.nstauthority.licensingmanagementservice.licence.continuation.tasklist;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import static uk.co.nstauthority.licensingmanagementservice.authentication.TestUserProvider.user;

import java.util.List;
import java.util.Map;
import org.springframework.test.context.ContextConfiguration;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.util.SecurityTest;
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

  @SecurityTest
  void getTaskList_assertOk() throws Exception {

    when(licenceContinuationService
        .getDetailByIdOrThrow(LICENCE_CONTINUATION_APPLICATION_DETAIL.getId()))
        .thenReturn(LICENCE_CONTINUATION_APPLICATION_DETAIL);

    when(licenceService.getLicencePageCaption(LICENCE)).thenReturn(CAPTION);

    mockMvc.perform(get(ReverseRouter.route(on(LicenceContinuationApplicationTaskListController.class)
            .getTaskList(LICENCE_CONTINUATION_APPLICATION_DETAIL.getId(), null))
        ).with(user(USER)))
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/continuation/taskList"))
        .andExpect(model().attribute("taskListSections", List.of()))
        .andExpect(model().attribute("pageTitle", LicenceContinuationApplicationTaskListController.PAGE_TITLE))
        .andExpect(model().attribute("pageCaption", CAPTION))
        .andExpect(model().attribute("breadcrumbs", Map.of(
            ReverseRouter.route(on(WorkAreaController.class).getWorkArea(null, null)),
            "Work area"
        )));
  }
}