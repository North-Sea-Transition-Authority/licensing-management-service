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

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivity;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplication;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.tasklist.ScheduleWorkProgrammeApplicationTaskListSectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.tasklist.ScheduleWorkProgrammeApplicationTaskListService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.util.SecurityTest;

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

  private static final UUID WORK_PROGRAMME_ACTIVITY_ID = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    organisationUser = ServiceUserDetailTestUtil.newBuilder()
        .withWuaId(ORGANISATION_USER_WUA_ID)
        .build();

    ScheduleWorkProgrammeApplication scheduleWorkProgrammeApplication =
        ScheduleWorkProgrammeApplicationTestUtil.createScheduleWorkProgrammeApplication(new LicenceScheduleDetail());

    scheduleWorkProgrammeApplicationDetail = new ScheduleWorkProgrammeApplicationDetail();
    scheduleWorkProgrammeApplicationDetail.setVersionNumber(1);
    scheduleWorkProgrammeApplicationDetail.setId(SCHEDULE_APPLICATION_DETAIL_ID);
    scheduleWorkProgrammeApplicationDetail.setAllLicenseesPermissionConfirmed(true);
    scheduleWorkProgrammeApplicationDetail.setScheduleWorkProgrammeApplication(scheduleWorkProgrammeApplication);

    when(scheduleWorkProgrammeApplicationService.getDetailByIdOrThrow(SCHEDULE_APPLICATION_DETAIL_ID)).thenReturn(
        scheduleWorkProgrammeApplicationDetail);
  }

  @SecurityTest
  void renderForm() throws Exception {
    when(licenceWorkProgrammeAmendmentService.getLicenceWorkProgrammeActivityAmendmentForm(any(), any())).thenReturn(
        new LicenceWorkProgrammeAmendmentForm());

    var mockWorkProgrammeActivityAmendmentView = getMockWorkProgrammeActivityAmendmentView();

    when(licenceWorkProgrammeAmendmentService.getLicenceWorkProgramAmendmentView(any(), any())).thenReturn(
        mockWorkProgrammeActivityAmendmentView);

    mockMvc.perform(
            get(ReverseRouter.route(
                on(LicenceWorkProgrammeAmendmentController.class).renderForm(
                    UUID.randomUUID(), new WorkProgrammeActivity(), SCHEDULE_APPLICATION_DETAIL_ID,
                    scheduleWorkProgrammeApplicationDetail
                )))
                .with(user(organisationUser)
                ).with(csrf())
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/scheduleWorkProgrammeApplication/scheduleWorkProgrammeAmendment"))
        .andExpect(model().attribute("pageTitle", "Work programme amendments"))
        .andExpect(model().attribute("workProgrammeActivityDetails", mockWorkProgrammeActivityAmendmentView))
           .andExpect(model().attribute("isLinkedFixedDate", false))
           .andExpect(model().attribute("cancelUrl", (ReverseRouter.route(on(LicenceWorkProgrammeAmendmentSummaryController.class)
            .renderForm(scheduleWorkProgrammeApplicationDetail.getId(),null)))));
  }

  @Test
  void submitValidForm() throws Exception {
    when(licenceWorkProgrammeAmendmentFormValidator.isValid(any(), any()))
        .thenReturn(true);
    mockMvc.perform(
               post(ReverseRouter.route(
                   on(LicenceWorkProgrammeAmendmentController.class).submitForm(
                       WORK_PROGRAMME_ACTIVITY_ID, new WorkProgrammeActivity(), SCHEDULE_APPLICATION_DETAIL_ID, null, null,
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

    when(licenceWorkProgrammeAmendmentService.getLicenceWorkProgramAmendmentView(any(), any())).thenReturn(
        mockWorkProgrammeActivityAmendmentView);

    mockMvc.perform(
               post(ReverseRouter.route(
                   on(LicenceWorkProgrammeAmendmentController.class).submitForm(
                       WORK_PROGRAMME_ACTIVITY_ID, new WorkProgrammeActivity(), SCHEDULE_APPLICATION_DETAIL_ID, null,
                       null, null
                   )))
                  .with(user(organisationUser))
                  .with(csrf())
          )
          .andExpect(status().isOk())
          .andExpect(view().name("lms/licence/scheduleWorkProgrammeApplication/scheduleWorkProgrammeAmendment"))
          .andExpect(model().attribute("pageTitle", "Work programme amendments"))
          .andExpect(model().attribute("workProgrammeActivityDetails", mockWorkProgrammeActivityAmendmentView))
             .andExpect(model().attribute("isLinkedFixedDate", false))
             .andExpect(model().attribute("cancelUrl", (ReverseRouter.route(on(LicenceWorkProgrammeAmendmentSummaryController.class)
              .renderForm(scheduleWorkProgrammeApplicationDetail.getId(),null)))));

      verify(licenceWorkProgrammeAmendmentService, never()).saveAmendmentForm(any(), any(), any());

    }

  private WorkProgrammeActivityAmendmentView getMockWorkProgrammeActivityAmendmentView() {
    return new WorkProgrammeActivityAmendmentView(
        UUID.randomUUID().toString(),
        "12/12/2025",
        "Category A",
        "Description 1",
        "Category A Due Date"
    );
  }
  }