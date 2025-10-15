package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney;

import static org.mockito.ArgumentMatchers.any;
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

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.tasklist.ScheduleWorkProgrammeApplicationTaskListController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.util.SecurityTest;

@ContextConfiguration(classes = SelectLicenceWorkAmendmentController.class)
class SelectLicenceWorkAmendmentControllerTest extends AbstractControllerTest {


  public static final String PAGE_TITLE = "What work programme activity are you requesting to amend?";

  @MockitoBean
  private SelectLicenceAmendmentFormValidator selectLicenceAmendmentFormValidator;

  @MockitoBean
  private SelectLicenceAmendmentService selectLicenceAmendmentService;


  private ServiceUserDetail organisationUser;
  private static final Long ORGANISATION_USER_WUA_ID = 2L;

  private ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail;

  private static final UUID SCHEDULE_APPLICATION_DETAIL_ID = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    organisationUser = ServiceUserDetailTestUtil.newBuilder()
        .withWuaId(ORGANISATION_USER_WUA_ID)
        .build();

    scheduleWorkProgrammeApplicationDetail = new ScheduleWorkProgrammeApplicationDetail();
    scheduleWorkProgrammeApplicationDetail.setVersionNumber(1);
    scheduleWorkProgrammeApplicationDetail.setId(SCHEDULE_APPLICATION_DETAIL_ID);
    scheduleWorkProgrammeApplicationDetail.setAllLicenseesPermissionConfirmed(true);

    when(scheduleWorkProgrammeApplicationService.getDetailByIdOrThrow(SCHEDULE_APPLICATION_DETAIL_ID)).thenReturn(
        scheduleWorkProgrammeApplicationDetail);
  }


  @SecurityTest
  void renderSelectAmendmentForm() throws Exception {
    when(selectLicenceAmendmentFormValidator.isValid(any(),any())).thenReturn(true);
    when(selectLicenceAmendmentService.getLicenceSelectWorkProgramAmendmentForm(scheduleWorkProgrammeApplicationDetail)).thenReturn((new SelectLicenceAmendmentForm()));

    mockMvc.perform(
            get(ReverseRouter.route(
                on(SelectLicenceWorkAmendmentController.class).renderForm(SCHEDULE_APPLICATION_DETAIL_ID,
                    scheduleWorkProgrammeApplicationDetail)))
                .with(user(organisationUser)
                ).with(csrf())

        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/scheduleWorkProgrammeApplication/selectScheduleWorkProgrammeToAmend"))
        .andExpect(model().attribute("pageTitle", PAGE_TITLE))
        .andExpect(model().attribute("cancelUrl", (ReverseRouter.route(on(
            ScheduleWorkProgrammeApplicationTaskListController.class)
            .getTaskList(SCHEDULE_APPLICATION_DETAIL_ID, null, null)))));
  }

  @Test
  void submitValidForm() throws Exception {

    when(selectLicenceAmendmentFormValidator.isValid(any(),any())).thenReturn(true);
    mockMvc.perform(
            post(ReverseRouter.route(
                on(SelectLicenceWorkAmendmentController.class).submitForm(SCHEDULE_APPLICATION_DETAIL_ID,
                    scheduleWorkProgrammeApplicationDetail, null, null)))
                .with(user(organisationUser)
                ).with(csrf())

        )
        .andExpect(status().is3xxRedirection());
    verify(selectLicenceAmendmentService).saveAmendmentForm(any(), any(),any());
  }


  @Test
  void submitInvalidForm() throws Exception {

    when(selectLicenceAmendmentFormValidator.isValid(any(),any())).thenReturn(false);
    mockMvc.perform(
            post(ReverseRouter.route(
                on(SelectLicenceWorkAmendmentController.class).submitForm(SCHEDULE_APPLICATION_DETAIL_ID,
                    scheduleWorkProgrammeApplicationDetail, null, null)))
                .with(user(organisationUser)
                ).with(csrf())

        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/scheduleWorkProgrammeApplication/selectScheduleWorkProgrammeToAmend"))
        .andExpect(model().attribute("pageTitle", PAGE_TITLE))
        .andExpect(model().attribute("cancelUrl", (ReverseRouter.route(on(
            ScheduleWorkProgrammeApplicationTaskListController.class)
            .getTaskList(SCHEDULE_APPLICATION_DETAIL_ID, null, null)))));

    verify(selectLicenceAmendmentService, never()).saveAmendmentForm(any(), any(),any());

  }
}