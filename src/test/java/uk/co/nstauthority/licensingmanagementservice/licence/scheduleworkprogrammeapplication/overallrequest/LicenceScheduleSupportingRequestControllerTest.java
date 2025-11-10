package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overallrequest;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
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
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney.LicenceWorkProgrammeAmendmentService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.extendjourney.LicenceScheduleExtensionService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.tasklist.ScheduleWorkProgrammeApplicationTaskListController;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.tasklist.requestpurpose.SwpApplicationRequestPurpose;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.util.SecurityTest;

@ContextConfiguration(classes = LicenceScheduleSupportingRequestController.class)
class LicenceScheduleSupportingRequestControllerTest extends AbstractControllerTest {

  @MockitoBean
  private LicenceScheduleSupportingRequestService licenceScheduleSupportingRequestService;

  @MockitoBean
  private LicenceScheduleSupportingRequestFormValidator licenceScheduleSupportingRequestFormValidator;

  @MockitoBean
  private LicenceScheduleExtensionService licenceScheduleExtensionService;

  @MockitoBean
  private LicenceWorkProgrammeAmendmentService licenceWorkProgrammeAmendmentService;

  private ServiceUserDetail organisationUser;
  private static final Long ORGANISATION_USER_WUA_ID = 2L;

  private ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail;

  private static final UUID SCHEDULE_APPLICATION_DETAIL_ID = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    organisationUser = ServiceUserDetailTestUtil.newBuilder()
        .withWuaId(ORGANISATION_USER_WUA_ID)
        .build();

    var scheduleWorkProgrammeApplication = ScheduleWorkProgrammeApplicationTestUtil.createScheduleWorkProgrammeApplication(new LicenceScheduleDetail());

    scheduleWorkProgrammeApplicationDetail = new ScheduleWorkProgrammeApplicationDetail();
    scheduleWorkProgrammeApplicationDetail.setScheduleWorkProgrammeApplication(scheduleWorkProgrammeApplication);
    scheduleWorkProgrammeApplicationDetail.setVersionNumber(1);
    scheduleWorkProgrammeApplicationDetail.setId(SCHEDULE_APPLICATION_DETAIL_ID);
    scheduleWorkProgrammeApplicationDetail.setAllLicenseesPermissionConfirmed(true);

    when(scheduleWorkProgrammeApplicationService.getDetailByIdOrThrow(SCHEDULE_APPLICATION_DETAIL_ID)).thenReturn(
        scheduleWorkProgrammeApplicationDetail);
  }

  @SecurityTest
  void renderOverallRequestForm() throws Exception {
    SwpApplicationRequestPurpose swpApplicationRequestPurpose = new SwpApplicationRequestPurpose();
    swpApplicationRequestPurpose.setExtendPhaseOrTerm(true);
    swpApplicationRequestPurpose.setExtendTerm(true);

    when(licenceScheduleExtensionService.isExtensionRequested(scheduleWorkProgrammeApplicationDetail)).thenReturn(true);
    when(licenceWorkProgrammeAmendmentService.isAmendmentRequested(scheduleWorkProgrammeApplicationDetail)).thenReturn(true);

    when(licenceScheduleSupportingRequestService.getLicenceScheduleRequestForm(any())).thenReturn(
        new LicenceScheduleSupportingRequestForm());

    mockMvc.perform(
            get(ReverseRouter.route(
                on(LicenceScheduleSupportingRequestController.class).renderForm(SCHEDULE_APPLICATION_DETAIL_ID,
                    scheduleWorkProgrammeApplicationDetail)))
                .with(user(organisationUser)
                ).with(csrf())
        )
        .andExpect(view().name("lms/licence/scheduleWorkProgrammeApplication/scheduleLicenceSupportingInformationRequest"))
        .andExpect(status().isOk())
        .andExpect(model().attribute("pageTitle", "Supporting information"))
        .andExpect(model().attribute("isExtension", false))
        .andExpect(model().attribute("cancelUrl", (ReverseRouter.route(on(
            ScheduleWorkProgrammeApplicationTaskListController.class)
            .getTaskList(SCHEDULE_APPLICATION_DETAIL_ID, null, null)))));

  }

  @Test
  void submitValidForm() throws Exception {
    when(licenceScheduleSupportingRequestFormValidator.isValid(any(), any())).thenReturn(true);

    mockMvc.perform(
            post(ReverseRouter.route(
                on(LicenceScheduleSupportingRequestController.class).submitForm(SCHEDULE_APPLICATION_DETAIL_ID, null, null, null)))
                .with(user(organisationUser))
                .with(csrf())
        )
        .andExpect(status().is3xxRedirection());

    verify(licenceScheduleSupportingRequestService).saveRequestForm(any(), eq(scheduleWorkProgrammeApplicationDetail));
  }

  @Test
  void submitInvalidForm() throws Exception {

    mockMvc.perform(
            post(ReverseRouter.route(
                on(LicenceScheduleSupportingRequestController.class).submitForm(SCHEDULE_APPLICATION_DETAIL_ID, null, null, null)))
                .with(user(organisationUser))
                .with(csrf())
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/scheduleWorkProgrammeApplication/scheduleLicenceSupportingInformationRequest"))
        .andExpect(model().attribute("pageTitle", "Supporting information"))
        .andExpect(model().attribute("isExtension", false))
        .andExpect(model().attribute("cancelUrl", (ReverseRouter.route(on(
            ScheduleWorkProgrammeApplicationTaskListController.class)
            .getTaskList(SCHEDULE_APPLICATION_DETAIL_ID, null, null)))));

    verify(licenceScheduleSupportingRequestService, never()).saveRequestForm(any(), any());

  }
}