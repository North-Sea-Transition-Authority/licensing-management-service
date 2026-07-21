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
import java.util.Map;
import java.util.Optional;
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
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceSchedule;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivity;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityCategory;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplication;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.tasklist.ScheduleWorkProgrammeApplicationTaskListController;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.tasklist.ScheduleWorkProgrammeApplicationTaskListSectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.tasklist.ScheduleWorkProgrammeApplicationTaskListService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.workarea.WorkAreaController;

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
  private  WorkProgrammeActivity workProgrammeActivity;


  private static final UUID SCHEDULE_APPLICATION_DETAIL_ID = UUID.randomUUID();
  private static final UUID WORK_PROGRAMME_ACTIVITY_ID = UUID.randomUUID();

  LicenceWorkProgrammeAmendmentRequest amendmentRequest;

  @BeforeEach
  void setUp() {
    workProgrammeActivity = new WorkProgrammeActivity();
    workProgrammeActivity.setId(WORK_PROGRAMME_ACTIVITY_ID);
    amendmentRequest = new LicenceWorkProgrammeAmendmentRequest();
    amendmentRequest.setWorkProgrammeActivity(workProgrammeActivity);

    organisationUser = ServiceUserDetailTestUtil.newBuilder()
        .withWuaId(ORGANISATION_USER_WUA_ID)
        .build();

    var scheduleWorkProgrammeApplication = new ScheduleWorkProgrammeApplication();
    scheduleWorkProgrammeApplication.setId(UUID.randomUUID());
    var licenceSchedule = new LicenceSchedule();
    licenceSchedule.setLicence(new Licence());
    scheduleWorkProgrammeApplication.setLicenceSchedule(licenceSchedule);
    scheduleWorkProgrammeApplicationDetail = new ScheduleWorkProgrammeApplicationDetail();
    scheduleWorkProgrammeApplicationDetail.setVersionNumber(1);
    scheduleWorkProgrammeApplicationDetail.setId(SCHEDULE_APPLICATION_DETAIL_ID);
    scheduleWorkProgrammeApplicationDetail.setStatus(ApplicationStatus.DRAFT);
    scheduleWorkProgrammeApplicationDetail.setAllLicenseesPermissionConfirmed(true);
    scheduleWorkProgrammeApplicationDetail.setScheduleWorkProgrammeApplication(scheduleWorkProgrammeApplication);

    workProgrammeActivity.setCategory(WorkProgrammeActivityCategory.WELL_TEST);
    when(workProgrammeActivityService.getWorkProgrammeActivityByIdOrThrow(any())).thenReturn(workProgrammeActivity);

    when(scheduleWorkProgrammeApplicationService.getDetailByIdOrThrow(SCHEDULE_APPLICATION_DETAIL_ID)).thenReturn(
        scheduleWorkProgrammeApplicationDetail);
  }

  @Test
  void renderConfirmationForm() throws Exception {
    when(licenceWorkProgrammeAmendmentService.getAmendmentRequestByScheduleWorkProgrammeApplicationDetail(any(),any())).thenReturn(
        Optional.of(amendmentRequest));
    when(licenceWorkProgrammeAmendmentSummaryService.createSummaryViewFromWorkProgrammeAmendments(any(), any())).thenReturn(
        new LicenceWorkProgrammeAmendmentSummaryView("duration", "additionalInfo", "label", "extensionRequired",
            "information", LicenceWorkProgrammeAmendmentSummaryMode.VIEW, "changeUrl", "deleteUrl",false,false, false));
    when(applicationAccessService.userHasAccessToApplication(any(), any(), any())).thenReturn(true);

    mockMvc.perform(get(ReverseRouter.route(on(LicenceWorkProgrammeAmendmentDeleteController.class).renderForm(
        WORK_PROGRAMME_ACTIVITY_ID, null, SCHEDULE_APPLICATION_DETAIL_ID, null)))
                .with(user(organisationUser))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/scheduleWorkProgrammeApplication/scheduleWorkProgrammeAmendmentDeleteConfirmation"))
        .andExpect(model().attributeExists("backToSummaryUrl"))
        .andExpect(model().attributeExists("actionUrl"))
        .andExpect(model().attributeExists("LicenceWorkProgrammeAmendmentSummaryView"))
        .andExpect(model().attribute("breadcrumbs", Map.of(
            ReverseRouter.route(on(WorkAreaController.class).getWorkArea(null, null)), "Work area",
            ReverseRouter.route(on(ScheduleWorkProgrammeApplicationTaskListController.class).getTaskList(SCHEDULE_APPLICATION_DETAIL_ID, null, null)), "Task list"
        )))
        .andExpect(model().attribute("currentPage", "Are you sure you want to delete this work programme amendment?"));

    verify(licenceWorkProgrammeAmendmentService).getAmendmentRequestByScheduleWorkProgrammeApplicationDetail(
        scheduleWorkProgrammeApplicationDetail, workProgrammeActivity);
  }

  @Test
  void renderConfirmationForm_whenAmendmentNotFound() throws Exception {
    when(licenceWorkProgrammeAmendmentService.getAmendmentRequestByScheduleWorkProgrammeApplicationDetail(any(),any())).thenReturn(
        Optional.empty());
    when(licenceWorkProgrammeAmendmentSummaryService.createSummaryViewFromWorkProgrammeAmendments(any(), any())).thenReturn(
        new LicenceWorkProgrammeAmendmentSummaryView("duration", "additionalInfo", "label", "extensionRequired",
            "information", LicenceWorkProgrammeAmendmentSummaryMode.VIEW, "changeUrl", "deleteUrl",false,false, false));
    when(applicationAccessService.userHasAccessToApplication(any(), any(), any())).thenReturn(true);

    mockMvc.perform(get(ReverseRouter.route(on(LicenceWorkProgrammeAmendmentDeleteController.class).renderForm(
        WORK_PROGRAMME_ACTIVITY_ID, null, SCHEDULE_APPLICATION_DETAIL_ID, null)))
            .with(user(organisationUser)))
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/scheduleWorkProgrammeApplication/scheduleWorkProgrammeAmendmentDeleteConfirmation"))
        .andExpect(model().attribute("breadcrumbs", Map.of(
            ReverseRouter.route(on(WorkAreaController.class).getWorkArea(null, null)), "Work area",
            ReverseRouter.route(on(ScheduleWorkProgrammeApplicationTaskListController.class).getTaskList(SCHEDULE_APPLICATION_DETAIL_ID, null, null)), "Task list"
        )))
        .andExpect(model().attribute("currentPage", "Are you sure you want to delete this work programme amendment?"));
  }

  @Test
  void deleteWorkProgrammeAmendment_withRemainingAmendments() throws Exception {
    LicenceWorkProgrammeAmendmentRequest remainingAmendment = new LicenceWorkProgrammeAmendmentRequest();

    when(licenceWorkProgrammeAmendmentService.getAmendmentRequestByScheduleWorkProgrammeApplicationDetailElseThrow(
        scheduleWorkProgrammeApplicationDetail, workProgrammeActivity))
        .thenReturn(amendmentRequest);
    when(licenceWorkProgrammeAmendmentService.getAmendmentRequestsByScheduleWorkProgrammeApplicationDetail(
        scheduleWorkProgrammeApplicationDetail))
        .thenReturn(List.of(remainingAmendment));
    when(applicationAccessService.userHasAccessToApplication(any(), any(), any())).thenReturn(true);

    mockMvc.perform(post(ReverseRouter.route(on(LicenceWorkProgrammeAmendmentDeleteController.class)
                .deleteLicenceWorkProgrammeAmendment(
                    WORK_PROGRAMME_ACTIVITY_ID,
                    null,
                    SCHEDULE_APPLICATION_DETAIL_ID,
                    null,
                    null
                )
            ))
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
        scheduleWorkProgrammeApplicationDetail, workProgrammeActivity))
        .thenReturn(amendmentRequest);
    when(licenceWorkProgrammeAmendmentService.getAmendmentRequestsByScheduleWorkProgrammeApplicationDetail(
        scheduleWorkProgrammeApplicationDetail))
        .thenReturn(List.of());
    when(applicationAccessService.userHasAccessToApplication(any(), any(), any())).thenReturn(true);

    mockMvc.perform(post(ReverseRouter.route(on(LicenceWorkProgrammeAmendmentDeleteController.class)
            .deleteLicenceWorkProgrammeAmendment(
                WORK_PROGRAMME_ACTIVITY_ID,
                null,
                SCHEDULE_APPLICATION_DETAIL_ID,
                null,
                null
            )))
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
        scheduleWorkProgrammeApplicationDetail, workProgrammeActivity))
        .thenReturn(amendmentRequest);
    when(licenceWorkProgrammeAmendmentService.getAmendmentRequestsByScheduleWorkProgrammeApplicationDetail(
        scheduleWorkProgrammeApplicationDetail))
        .thenReturn(null);
    when(applicationAccessService.userHasAccessToApplication(any(), any(), any())).thenReturn(true);

    mockMvc.perform(post(ReverseRouter.route(on(LicenceWorkProgrammeAmendmentDeleteController.class)
            .deleteLicenceWorkProgrammeAmendment(
                WORK_PROGRAMME_ACTIVITY_ID,
                null,
                SCHEDULE_APPLICATION_DETAIL_ID,
                null,
                null
            )))
            .with(user(organisationUser))
            .with(csrf())
        )
        .andExpect(status().is3xxRedirection());

    verify(licenceWorkProgrammeAmendmentService).deleteWorkProgrammeAmendment(amendmentRequest,
        scheduleWorkProgrammeApplicationDetail);
  }
  @ParameterizedTest
  @EnumSource(value = ApplicationStatus.class, mode = EnumSource.Mode.EXCLUDE, names = "DRAFT")
  void renderPage_assertForbiddenOnNotDraft(ApplicationStatus status) throws Exception {
    var id = UUID.randomUUID();
    var submittedDetail = ScheduleWorkProgrammeApplicationDetailTestUtil
        .builder()
        .withId(id)
        .withStatus(status)
        .build();

    when(scheduleWorkProgrammeApplicationService.getDetailByIdOrThrow(id)).thenReturn(submittedDetail);

    mockMvc.perform(get(ReverseRouter.route(on(LicenceWorkProgrammeAmendmentDeleteController.class).renderForm(
        UUID.randomUUID(), null, id, null)))
        .with(user(organisationUser))).andExpect(status().isForbidden());
  }

  @ParameterizedTest
  @EnumSource(value = ApplicationStatus.class, mode = EnumSource.Mode.EXCLUDE, names = "DRAFT")
  void submitPage_assertForbiddenOnNotDraft(ApplicationStatus status) throws Exception {
    var id = UUID.randomUUID();
    var submittedDetail = ScheduleWorkProgrammeApplicationDetailTestUtil
        .builder()
        .withId(id)
        .withStatus(status)
        .build();

    when(scheduleWorkProgrammeApplicationService.getDetailByIdOrThrow(id)).thenReturn(submittedDetail);

    mockMvc.perform(post(ReverseRouter.route(on(LicenceWorkProgrammeAmendmentDeleteController.class).deleteLicenceWorkProgrammeAmendment(
            UUID.randomUUID(), null, id, null, null)))
            .with(user(organisationUser))
            .with(csrf()))
        .andExpect(status().isForbidden());
  }

  @Test
  void renderPage_assertForbiddenUserNoAccess() throws Exception {
    when(applicationAccessService.userHasAccessToApplication(any(), any(), any())).thenReturn(false);

    mockMvc.perform(get(ReverseRouter.route(on(LicenceWorkProgrammeAmendmentDeleteController.class).renderForm(
        UUID.randomUUID(), null,SCHEDULE_APPLICATION_DETAIL_ID , null)))
        .with(user(organisationUser))).andExpect(status().isForbidden());
  }

  @Test
  void submitPage_assertForbiddenUserNoAccess() throws Exception {
    when(applicationAccessService.userHasAccessToApplication(any(), any(), any())).thenReturn(false);

    mockMvc.perform(post(ReverseRouter.route(on(LicenceWorkProgrammeAmendmentDeleteController.class).deleteLicenceWorkProgrammeAmendment(
               UUID.randomUUID(), null, SCHEDULE_APPLICATION_DETAIL_ID, null, null)))
               .with(user(organisationUser))
               .with(csrf()))
           .andExpect(status().isForbidden());
  }
}