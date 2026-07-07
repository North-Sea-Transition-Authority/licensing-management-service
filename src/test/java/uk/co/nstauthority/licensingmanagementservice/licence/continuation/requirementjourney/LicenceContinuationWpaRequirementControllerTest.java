package uk.co.nstauthority.licensingmanagementservice.licence.continuation.requirementjourney;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import static uk.co.nstauthority.licensingmanagementservice.authentication.TestUserProvider.user;
import static uk.co.nstauthority.licensingmanagementservice.licence.continuation.requirementjourney.LicenceContinuationWpaRequirementController.PAGE_TITLE;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.tasklist.LicenceContinuationApplicationTaskListController;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityCategory;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityCommitment;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.status.WorkProgrammeStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney.WorkProgrammeActivityView;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.workarea.WorkAreaController;

@ContextConfiguration(classes = LicenceContinuationWpaRequirementController.class)
class LicenceContinuationWpaRequirementControllerTest extends AbstractControllerTest {

  @MockitoBean
  private LicenceContinuationWpaRequirementService licenceContinuationWpaRequirementService;

  @MockitoBean
  private LicenceContinuationWpaRequirementValidator licenceContinuationWpaRequirementValidator;

  private static final Licence LICENCE = LicenceTestUtil
      .builder().build();
  private static final LicenceScheduleDetail LICENCE_SCHEDULE_DETAIL
      = LicenceScheduleTestUtil.createLicenceScheduleDetail(LicenceScheduleTestUtil.createLicenceSchedule(LICENCE));
  private static final LicenceContinuationApplicationDetail LICENCE_CONTINUATION_APPLICATION_DETAIL
      = LicenceContinuationApplicationTestUtil.createLicenceContinuationApplicationDetail(LICENCE_SCHEDULE_DETAIL);

  private ServiceUserDetail organisationUser;
  private static final Long ORGANISATION_USER_WUA_ID = 2L;
  private WorkProgrammeActivityView workProgrammeActivityView;

  @BeforeEach
  void setUp() {
    organisationUser = ServiceUserDetailTestUtil.newBuilder()
        .withWuaId(ORGANISATION_USER_WUA_ID)
        .build();

    workProgrammeActivityView = new WorkProgrammeActivityView(
        "activity-id",
        LocalDate.of(2026, 5, 10).toString(),
        WorkProgrammeActivityCategory.WELL_TEST.getDisplayName(),
        "Test Description",
        WorkProgrammeActivityCategory.WELL_TEST.getDisplayName(),
        WorkProgrammeActivityCommitment.FIRM.getDisplayName(),
        WorkProgrammeStatus.OPEN
    );
  }

  @Test
  void renderForm() throws Exception {
    var form = new LicenceContinuationWpaRequirementForm();

    List<WorkProgrammeActivityView> activities = List.of(workProgrammeActivityView);

    when(licenceContinuationWpaRequirementService.getLicenceContinuationWorkProgrammeActivitiesRequirementForm(any()))
        .thenReturn(form);
    when(licenceScheduleService.getCurrentWorkProgrammeActivitiesViews(any()))
        .thenReturn(activities);
    when(applicationAccessService.userHasAccessToApplication(any(), any(), any())).thenReturn(true);
    when(licenceContinuationService.getDetailByIdOrThrow(LICENCE_CONTINUATION_APPLICATION_DETAIL.getId()))
        .thenReturn(LICENCE_CONTINUATION_APPLICATION_DETAIL);

    mockMvc.perform(
            get(ReverseRouter.route(on(LicenceContinuationWpaRequirementController.class).renderForm(LICENCE_CONTINUATION_APPLICATION_DETAIL.getId(), null)))
                .with(user(organisationUser))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/continuation/licenceContinuationWpaRequirement"))
        .andExpect(model().attribute("pageTitle", PAGE_TITLE))
        .andExpect(model().attribute("form", form))
        .andExpect(model().attribute("workProgrammeActivities", activities))
        .andExpect(model().attribute("cancelUrl", ReverseRouter.route(on(LicenceContinuationApplicationTaskListController.class).getTaskList(LICENCE_CONTINUATION_APPLICATION_DETAIL.getId(), null, null))))
        .andExpect(model().attribute("breadcrumbs", Map.of(
            ReverseRouter.route(on(WorkAreaController.class).getWorkArea(null, null)), "Work area",
            ReverseRouter.route(on(LicenceContinuationApplicationTaskListController.class).getTaskList(LICENCE_CONTINUATION_APPLICATION_DETAIL.getId(), null, null)), "Task list"
        )))
        .andExpect(model().attribute("currentPage", PAGE_TITLE));
  }

  @Test
  void submitForm_Valid() throws Exception {
    var form = new LicenceContinuationWpaRequirementForm();

    when(licenceContinuationWpaRequirementValidator.isValid(any(), any()))
        .thenReturn(true);
    when(licenceContinuationService.getDetailByIdOrThrow(any()))
        .thenReturn(LICENCE_CONTINUATION_APPLICATION_DETAIL);
    when(applicationAccessService.userHasAccessToApplication(any(), any(), any())).thenReturn(true);
    when(licenceContinuationService.getDetailByIdOrThrow(LICENCE_CONTINUATION_APPLICATION_DETAIL.getId()))
        .thenReturn(LICENCE_CONTINUATION_APPLICATION_DETAIL);

    mockMvc.perform(post(ReverseRouter.route(on(LicenceContinuationWpaRequirementController.class).submitForm(
                LICENCE_CONTINUATION_APPLICATION_DETAIL.getId(),
                null,
                form,
                null
            )))
                .with(user(organisationUser))
                .with(csrf())
                .flashAttr("form", form))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(LicenceContinuationApplicationTaskListController.class).getTaskList(LICENCE_CONTINUATION_APPLICATION_DETAIL.getId(), null, null))));

    verify(licenceContinuationWpaRequirementService).saveLicenceContinuationWorkProgrammeActivitiesRequirementForm(eq(form), any());
  }

  @Test
  void renderForm_Invalid() throws Exception {
    when(licenceContinuationService.getDetailByIdOrThrow(any()))
        .thenReturn(LICENCE_CONTINUATION_APPLICATION_DETAIL);
    when(applicationAccessService.userHasAccessToApplication(any(), any(), any()))
        .thenReturn(false);

    mockMvc.perform(
            get(ReverseRouter.route(on(LicenceContinuationWpaRequirementController.class).renderForm(LICENCE_CONTINUATION_APPLICATION_DETAIL.getId(), null)))
                .with(user(organisationUser)))
        .andExpect(status().isForbidden());

    verifyNoInteractions(licenceContinuationWpaRequirementService);
  }

  @Test
  void submitForm_Invalid() throws Exception {
    when(licenceContinuationService.getDetailByIdOrThrow(any()))
        .thenReturn(LICENCE_CONTINUATION_APPLICATION_DETAIL);
    when(applicationAccessService.userHasAccessToApplication(any(), any(), any()))
        .thenReturn(false);

    mockMvc.perform(post(ReverseRouter.route(
            on(LicenceContinuationWpaRequirementController.class).submitForm(LICENCE_CONTINUATION_APPLICATION_DETAIL.getId(), null, null, null)))
                     .with(user(organisationUser))
                     .with(csrf()))
        .andExpect(status().isForbidden());
  }
}