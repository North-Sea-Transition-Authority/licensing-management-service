package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney;

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

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.tasklist.ScheduleWorkProgrammeApplicationTaskListSectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.tasklist.ScheduleWorkProgrammeApplicationTaskListService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.util.SecurityTest;

@ContextConfiguration(classes = LicenceWorkProgrammeAmendmentDeleteController.class)
class LicenceWorkProgrammeAmendmentDeleteControllerTest extends AbstractControllerTest {

  @MockitoBean
  private LicenceWorkProgrammeAmendmentService licenceWorkProgrammeAmendmentService;

  @MockitoBean
  private LicenceWorkProgrammeAmendmentSummaryService licenceWorkProgrammeAmendmentSummaryService;

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

  LicenceWorkProgrammeAmendmentRequest amendmentRequest;

  @BeforeEach
  void setUp() {

    amendmentRequest = new LicenceWorkProgrammeAmendmentRequest();
    amendmentRequest.setWorkProgrammeActivityId(WORK_PROGRAMME_ACTIVITY_ID);

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
  void renderConfirmationForm() throws Exception {
    when(licenceWorkProgrammeAmendmentService.getAmendmentRequestByScheduleWorkProgrammeApplicationDetail(any(),any())).thenReturn(
        Optional.of(amendmentRequest));
    when(licenceWorkProgrammeAmendmentSummaryService.createSummaryViewFromWorkProgrammeAmendments(any(), any())).thenReturn(
        new LicenceWorkProgrammeAmendmentSummaryView("duration", "additionalInfo", "label", "extensionRequired",
            "information", LicenceWorkProgrammeAmendmentSummaryMode.VIEW, "changeUrl", "deleteUrl",false,false));

    mockMvc.perform(
            get(ReverseRouter.route(
                on(LicenceWorkProgrammeAmendmentDeleteController.class).renderForm(WORK_PROGRAMME_ACTIVITY_ID,SCHEDULE_APPLICATION_DETAIL_ID,
                    scheduleWorkProgrammeApplicationDetail)))
                .with(user(organisationUser)
                ).with(csrf())
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/scheduleWorkProgrammeApplication/scheduleWorkProgrammeAmendmentDeleteConfirmation"))
        .andExpect(model().attributeExists("backToSummaryUrl"))
        .andExpect(model().attributeExists("actionUrl"))
        .andExpect(model().attributeExists("LicenceWorkProgrammeAmendmentSummaryView"));

    verify(licenceWorkProgrammeAmendmentService).getAmendmentRequestByScheduleWorkProgrammeApplicationDetail(
        scheduleWorkProgrammeApplicationDetail, WORK_PROGRAMME_ACTIVITY_ID);
  }

  @SecurityTest
  void renderConfirmationForm_whenAmendmentNotFound() throws Exception {
    when(licenceWorkProgrammeAmendmentService.getAmendmentRequestByScheduleWorkProgrammeApplicationDetail(any(),any())).thenReturn(
        Optional.empty());
    when(licenceWorkProgrammeAmendmentSummaryService.createSummaryViewFromWorkProgrammeAmendments(any(), any())).thenReturn(
        new LicenceWorkProgrammeAmendmentSummaryView("duration", "additionalInfo", "label", "extensionRequired",
            "information", LicenceWorkProgrammeAmendmentSummaryMode.VIEW, "changeUrl", "deleteUrl",false,false));

    mockMvc.perform(
            get(ReverseRouter.route(
                on(LicenceWorkProgrammeAmendmentDeleteController.class).renderForm(WORK_PROGRAMME_ACTIVITY_ID,SCHEDULE_APPLICATION_DETAIL_ID,
                    scheduleWorkProgrammeApplicationDetail)))
                .with(user(organisationUser)
                ).with(csrf())
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/scheduleWorkProgrammeApplication/scheduleWorkProgrammeAmendmentDeleteConfirmation"));
  }

  @Test
  void deleteWorkProgrammeAmendment_withRemainingAmendments() throws Exception {
    LicenceWorkProgrammeAmendmentRequest remainingAmendment = new LicenceWorkProgrammeAmendmentRequest();

    when(licenceWorkProgrammeAmendmentService.getAmendmentRequestByScheduleWorkProgrammeApplicationDetailElseThrow(
        scheduleWorkProgrammeApplicationDetail, WORK_PROGRAMME_ACTIVITY_ID))
        .thenReturn(amendmentRequest);
    when(licenceWorkProgrammeAmendmentService.getAmendmentRequestsByScheduleWorkProgrammeApplicationDetail(
        scheduleWorkProgrammeApplicationDetail))
        .thenReturn(List.of(remainingAmendment));

    mockMvc.perform(
            post(ReverseRouter.route(
                on(LicenceWorkProgrammeAmendmentDeleteController.class).deleteLicenceWorkProgrammeAmendment(
                    WORK_PROGRAMME_ACTIVITY_ID, SCHEDULE_APPLICATION_DETAIL_ID,
                    scheduleWorkProgrammeApplicationDetail, null)))
                .with(user(organisationUser))
                .with(csrf())
        )
        .andExpect(status().is3xxRedirection());

    verify(licenceWorkProgrammeAmendmentService).deleteWorkProgrammeAmendment(amendmentRequest,
        scheduleWorkProgrammeApplicationDetail);
    verify(licenceWorkProgrammeAmendmentService).hasAmendmentRequestsByScheduleWorkProgrammeApplicationDetail(scheduleWorkProgrammeApplicationDetail);
  }

  @Test
  void deleteWorkProgrammeAmendment_withNoRemainingAmendments() throws Exception {
    when(licenceWorkProgrammeAmendmentService.getAmendmentRequestByScheduleWorkProgrammeApplicationDetailElseThrow(
        scheduleWorkProgrammeApplicationDetail, WORK_PROGRAMME_ACTIVITY_ID))
        .thenReturn(amendmentRequest);
    when(licenceWorkProgrammeAmendmentService.getAmendmentRequestsByScheduleWorkProgrammeApplicationDetail(
        scheduleWorkProgrammeApplicationDetail))
        .thenReturn(List.of());

    mockMvc.perform(
            post(ReverseRouter.route(
                on(LicenceWorkProgrammeAmendmentDeleteController.class).deleteLicenceWorkProgrammeAmendment(
                    WORK_PROGRAMME_ACTIVITY_ID, SCHEDULE_APPLICATION_DETAIL_ID,
                    scheduleWorkProgrammeApplicationDetail, null)))
                .with(user(organisationUser))
                .with(csrf())
        )
        .andExpect(status().is3xxRedirection());

    verify(licenceWorkProgrammeAmendmentService).deleteWorkProgrammeAmendment(amendmentRequest,
        scheduleWorkProgrammeApplicationDetail);
  }

  @Test
  void deleteWorkProgrammeAmendment_withNullRemainingAmendments() throws Exception {


    when(licenceWorkProgrammeAmendmentService.getAmendmentRequestByScheduleWorkProgrammeApplicationDetailElseThrow(
        scheduleWorkProgrammeApplicationDetail, WORK_PROGRAMME_ACTIVITY_ID))
        .thenReturn(amendmentRequest);
    when(licenceWorkProgrammeAmendmentService.getAmendmentRequestsByScheduleWorkProgrammeApplicationDetail(
        scheduleWorkProgrammeApplicationDetail))
        .thenReturn(null);

    mockMvc.perform(
            post(ReverseRouter.route(
                on(LicenceWorkProgrammeAmendmentDeleteController.class).deleteLicenceWorkProgrammeAmendment(
                    WORK_PROGRAMME_ACTIVITY_ID, SCHEDULE_APPLICATION_DETAIL_ID,
                    scheduleWorkProgrammeApplicationDetail, null)))
                .with(user(organisationUser))
                .with(csrf())
        )
        .andExpect(status().is3xxRedirection());

    verify(licenceWorkProgrammeAmendmentService).deleteWorkProgrammeAmendment(amendmentRequest,
        scheduleWorkProgrammeApplicationDetail);
  }

}