package uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity;

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

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.common.LicenceScheduleRelativeOptionsService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.DisplayableEnumOptionUtil;

@ContextConfiguration(classes = WorkProgrammeActivityController.class)
class WorkProgrammeActivityControllerTest extends AbstractControllerTest {

  @MockitoBean
  private WorkProgrammeActivityFormService workProgrammeActivityFormService;

  @MockitoBean
  private WorkProgrammeActivityFormValidator workProgrammeActivityFormValidator;

  @MockitoBean
  private LicenceScheduleRelativeOptionsService licenceScheduleRelativeOptionsService;

  private Licence licence;
  private static final String PAGE_CAPTION = "page caption";

  private LicenceScheduleDetail licenceScheduleDetail;
  private WorkProgrammeActivity workProgrammeActivity;

  @BeforeEach
  void setUp() {
    licence = LicenceTestUtil.builder()
        .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
        .build();

    var licenceSchedule = LicenceScheduleTestUtil.createLicenceSchedule(licence);

    licenceScheduleDetail = LicenceScheduleTestUtil.createLicenceScheduleDetail(licenceSchedule);

    workProgrammeActivity = new WorkProgrammeActivity();
    workProgrammeActivity.setId(UUID.randomUUID());
    workProgrammeActivity.setLicenceScheduleDetail(licenceScheduleDetail);
  }

  @Test
  void renderAddNewActivityForm() throws Exception {
    when(teamQueryService.userHasRoleInTeamType(regulatorUser.wuaId(), TeamType.LICENCE_MANAGEMENT, Set.of(Role.WORK_PROGRAMME_ADMINISTRATOR)))
        .thenReturn(true);
    when(licenceScheduleDetailService.getByIdOrThrow(licenceScheduleDetail.getId())).thenReturn(licenceScheduleDetail);
    when(licenceService.getLicencePageCaption(licence)).thenReturn(PAGE_CAPTION);
    when(workProgrammeActivityFormService.getDateOptions(licenceScheduleDetail)).thenReturn(Map.of());
    when(licenceScheduleRelativeOptionsService.getScheduleTermOptions(licenceScheduleDetail)).thenReturn(Map.of());
    when(licenceScheduleRelativeOptionsService.getSchedulePhaseOptions(licenceScheduleDetail)).thenReturn(Map.of());
    when(licenceScheduleRelativeOptionsService.getRelativeEventOptions(licenceScheduleDetail)).thenReturn(Map.of());

    mockMvc.perform(
            get(ReverseRouter.route(on(WorkProgrammeActivityController.class)
                .renderAddNewActivityForm(licenceScheduleDetail.getId(), null)))
                .with(user(regulatorUser))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/schedule/createWorkProgrammeActivity"))
        .andExpect(model().attribute("categoryRadioOptions", WorkProgrammeActivityCategory.getCategoriesForLicenceType(licence.getType())))
        .andExpect(model().attribute("commitmentRadioOptions", DisplayableEnumOptionUtil.getDisplayableOptions(WorkProgrammeActivityCommitment.class)))
        .andExpect(model().attribute("activityDateRadioOptions", Map.of()))
        .andExpect(model().attribute("termOptions", Map.of()))
        .andExpect(model().attribute("phaseOptions", Map.of()))
        .andExpect(model().attribute("relativeOptions", Map.of()))
        .andExpect(model().attribute("cancelUrl", licenceScheduleDetail.getScheduleTimelineRouteUrl()))
        .andExpect(model().attribute("pageCaption", PAGE_CAPTION));
  }

  @Test
  void renderAddNewActivityForm_noRoles() throws Exception {
    when(teamQueryService.userHasRoleInTeamType(regulatorUser.wuaId(), TeamType.LICENCE_MANAGEMENT, Set.of(Role.WORK_PROGRAMME_ADMINISTRATOR)))
        .thenReturn(false);

    mockMvc.perform(
            get(ReverseRouter.route(on(WorkProgrammeActivityController.class)
                .renderAddNewActivityForm(licenceScheduleDetail.getId(), null)))
                .with(user(regulatorUser))
        )
        .andExpect(status().isForbidden());
  }

  @Test
  void submitAddNewActivityForm() throws Exception {
    when(teamQueryService.userHasRoleInTeamType(regulatorUser.wuaId(), TeamType.LICENCE_MANAGEMENT, Set.of(Role.WORK_PROGRAMME_ADMINISTRATOR)))
        .thenReturn(true);
    when(licenceScheduleDetailService.getByIdOrThrow(licenceScheduleDetail.getId())).thenReturn(licenceScheduleDetail);
    when(workProgrammeActivityFormValidator.isValid(any(), any())).thenReturn(true);

    mockMvc.perform(
            post(ReverseRouter.route(on(WorkProgrammeActivityController.class)
                .submitAddNewActivityForm(licenceScheduleDetail.getId(), null, null, null, null)))
                .with(user(regulatorUser))
                .with(csrf())
        )
        .andExpect(status().is3xxRedirection());

    verify(workProgrammeActivityFormService).saveActivityFromForm(any(), eq(licenceScheduleDetail), any(), any());
  }

  @Test
  void submitAddNewActivityForm_invalid() throws Exception {
    when(teamQueryService.userHasRoleInTeamType(regulatorUser.wuaId(), TeamType.LICENCE_MANAGEMENT, Set.of(Role.WORK_PROGRAMME_ADMINISTRATOR)))
        .thenReturn(true);
    when(licenceScheduleDetailService.getByIdOrThrow(licenceScheduleDetail.getId())).thenReturn(licenceScheduleDetail);
    when(licenceService.getLicencePageCaption(licence)).thenReturn(PAGE_CAPTION);
    when(workProgrammeActivityFormValidator.isValid(any(), any())).thenReturn(false);
    when(workProgrammeActivityFormService.getDateOptions(licenceScheduleDetail)).thenReturn(Map.of());
    when(licenceScheduleRelativeOptionsService.getScheduleTermOptions(licenceScheduleDetail)).thenReturn(Map.of());
    when(licenceScheduleRelativeOptionsService.getSchedulePhaseOptions(licenceScheduleDetail)).thenReturn(Map.of());
    when(licenceScheduleRelativeOptionsService.getRelativeEventOptions(licenceScheduleDetail)).thenReturn(Map.of());

    mockMvc.perform(
            post(ReverseRouter.route(on(WorkProgrammeActivityController.class)
                .submitAddNewActivityForm(licenceScheduleDetail.getId(), null, null, null, null)))
                .with(user(regulatorUser))
                .with(csrf())
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/schedule/createWorkProgrammeActivity"))
        .andExpect(model().attribute("categoryRadioOptions", WorkProgrammeActivityCategory.getCategoriesForLicenceType(licence.getType())))
        .andExpect(model().attribute("commitmentRadioOptions", DisplayableEnumOptionUtil.getDisplayableOptions(WorkProgrammeActivityCommitment.class)))
        .andExpect(model().attribute("activityDateRadioOptions", Map.of()))
        .andExpect(model().attribute("termOptions", Map.of()))
        .andExpect(model().attribute("phaseOptions", Map.of()))
        .andExpect(model().attribute("relativeOptions", Map.of()))
        .andExpect(model().attribute("cancelUrl", licenceScheduleDetail.getScheduleTimelineRouteUrl()))
        .andExpect(model().attribute("pageCaption", PAGE_CAPTION));

    verify(workProgrammeActivityFormService, never()).saveActivityFromForm(any(), any(), any(), any());
  }

  @Test
  void submitAddNewActivityForm_noRoles() throws Exception {
    when(teamQueryService.userHasRoleInTeamType(regulatorUser.wuaId(), TeamType.LICENCE_MANAGEMENT, Set.of(Role.WORK_PROGRAMME_ADMINISTRATOR)))
        .thenReturn(false);

    mockMvc.perform(
            post(ReverseRouter.route(on(WorkProgrammeActivityController.class)
                .submitAddNewActivityForm(licenceScheduleDetail.getId(), null, null, null, null)))
                .with(user(regulatorUser))
                .with(csrf())
        )
        .andExpect(status().isForbidden());

    verify(workProgrammeActivityFormService, never()).saveActivityFromForm(any(), any(), any(), any());
  }

  @Test
  void renderUpdateActivityForm() throws Exception {
    when(teamQueryService.userHasRoleInTeamType(regulatorUser.wuaId(), TeamType.LICENCE_MANAGEMENT, Set.of(Role.WORK_PROGRAMME_ADMINISTRATOR)))
        .thenReturn(true);
    when(workProgrammeActivityService.getWorkProgrammeActivityByIdOrThrow(workProgrammeActivity.getId())).thenReturn(workProgrammeActivity);
    when(licenceService.getLicencePageCaption(licence)).thenReturn(PAGE_CAPTION);
    when(workProgrammeActivityFormService.getDateOptions(licenceScheduleDetail)).thenReturn(Map.of());
    when(licenceScheduleRelativeOptionsService.getScheduleTermOptions(licenceScheduleDetail)).thenReturn(Map.of());
    when(licenceScheduleRelativeOptionsService.getSchedulePhaseOptions(licenceScheduleDetail)).thenReturn(Map.of());
    when(licenceScheduleRelativeOptionsService.getRelativeEventOptions(licenceScheduleDetail)).thenReturn(Map.of());
    when(workProgrammeActivityFormService.getActivityForm(workProgrammeActivity)).thenReturn(new WorkProgrammeActivityForm());

    mockMvc.perform(
            get(ReverseRouter.route(on(WorkProgrammeActivityController.class)
                .renderUpdateActivityForm(workProgrammeActivity.getId(), null)))
                .with(user(regulatorUser))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/schedule/createWorkProgrammeActivity"))
        .andExpect(model().attribute("categoryRadioOptions", WorkProgrammeActivityCategory.getCategoriesForLicenceType(licence.getType())))
        .andExpect(model().attribute("commitmentRadioOptions", DisplayableEnumOptionUtil.getDisplayableOptions(WorkProgrammeActivityCommitment.class)))
        .andExpect(model().attribute("activityDateRadioOptions", Map.of()))
        .andExpect(model().attribute("termOptions", Map.of()))
        .andExpect(model().attribute("phaseOptions", Map.of()))
        .andExpect(model().attribute("relativeOptions", Map.of()))
        .andExpect(model().attribute("cancelUrl", licenceScheduleDetail.getScheduleTimelineRouteUrl()))
        .andExpect(model().attribute("pageCaption", PAGE_CAPTION));
  }

  @Test
  void renderUpdateActivityForm_noRoles() throws Exception {
    when(teamQueryService.userHasRoleInTeamType(regulatorUser.wuaId(), TeamType.LICENCE_MANAGEMENT, Set.of(Role.WORK_PROGRAMME_ADMINISTRATOR)))
        .thenReturn(false);

    mockMvc.perform(
            get(ReverseRouter.route(on(WorkProgrammeActivityController.class)
                .renderUpdateActivityForm(workProgrammeActivity.getId(), null)))
                .with(user(regulatorUser))
        )
        .andExpect(status().isForbidden());
  }

  @Test
  void submitUpdateActivityForm() throws Exception {
    when(teamQueryService.userHasRoleInTeamType(regulatorUser.wuaId(), TeamType.LICENCE_MANAGEMENT, Set.of(Role.WORK_PROGRAMME_ADMINISTRATOR)))
        .thenReturn(true);
    when(workProgrammeActivityService.getWorkProgrammeActivityByIdOrThrow(workProgrammeActivity.getId())).thenReturn(workProgrammeActivity);
    when(workProgrammeActivityFormValidator.isValid(any(), any())).thenReturn(true);

    mockMvc.perform(
            post(ReverseRouter.route(on(WorkProgrammeActivityController.class)
                .submitUpdateActivityForm(workProgrammeActivity.getId(), null, null, null, null)))
                .with(user(regulatorUser))
                .with(csrf())
        )
        .andExpect(status().is3xxRedirection());

    verify(workProgrammeActivityFormService).saveActivityFromForm(any(), eq(licenceScheduleDetail), eq(workProgrammeActivity), any());
  }

  @Test
  void submitUpdateActivityForm_invalid() throws Exception {
    when(teamQueryService.userHasRoleInTeamType(regulatorUser.wuaId(), TeamType.LICENCE_MANAGEMENT, Set.of(Role.WORK_PROGRAMME_ADMINISTRATOR)))
        .thenReturn(true);
    when(workProgrammeActivityService.getWorkProgrammeActivityByIdOrThrow(workProgrammeActivity.getId())).thenReturn(workProgrammeActivity);
    when(licenceService.getLicencePageCaption(licence)).thenReturn(PAGE_CAPTION);
    when(workProgrammeActivityFormValidator.isValid(any(), any())).thenReturn(false);
    when(workProgrammeActivityFormService.getDateOptions(licenceScheduleDetail)).thenReturn(Map.of());
    when(licenceScheduleRelativeOptionsService.getScheduleTermOptions(licenceScheduleDetail)).thenReturn(Map.of());
    when(licenceScheduleRelativeOptionsService.getSchedulePhaseOptions(licenceScheduleDetail)).thenReturn(Map.of());
    when(licenceScheduleRelativeOptionsService.getRelativeEventOptions(licenceScheduleDetail)).thenReturn(Map.of());
    when(workProgrammeActivityFormService.getActivityForm(workProgrammeActivity)).thenReturn(new WorkProgrammeActivityForm());

    mockMvc.perform(
            post(ReverseRouter.route(on(WorkProgrammeActivityController.class)
                .submitUpdateActivityForm(workProgrammeActivity.getId(), null, null, null, null)))
                .with(user(regulatorUser))
                .with(csrf())
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/schedule/createWorkProgrammeActivity"))
        .andExpect(model().attribute("categoryRadioOptions", WorkProgrammeActivityCategory.getCategoriesForLicenceType(licence.getType())))
        .andExpect(model().attribute("commitmentRadioOptions", DisplayableEnumOptionUtil.getDisplayableOptions(WorkProgrammeActivityCommitment.class)))
        .andExpect(model().attribute("activityDateRadioOptions", Map.of()))
        .andExpect(model().attribute("termOptions", Map.of()))
        .andExpect(model().attribute("phaseOptions", Map.of()))
        .andExpect(model().attribute("relativeOptions", Map.of()))
        .andExpect(model().attribute("cancelUrl", licenceScheduleDetail.getScheduleTimelineRouteUrl()))
        .andExpect(model().attribute("pageCaption", PAGE_CAPTION));

    verify(workProgrammeActivityFormService, never()).saveActivityFromForm(any(), any(), any(), any());
  }

  @Test
  void submitUpdateActivityForm_noRoles() throws Exception {
    when(teamQueryService.userHasRoleInTeamType(regulatorUser.wuaId(), TeamType.LICENCE_MANAGEMENT, Set.of(Role.WORK_PROGRAMME_ADMINISTRATOR)))
        .thenReturn(false);

    mockMvc.perform(
            post(ReverseRouter.route(on(WorkProgrammeActivityController.class)
                .submitUpdateActivityForm(workProgrammeActivity.getId(), null, null, null, null)))
                .with(user(regulatorUser))
                .with(csrf())
        )
        .andExpect(status().isForbidden());

    verify(workProgrammeActivityFormService, never()).saveActivityFromForm(any(), any(), any(), any());
  }
}