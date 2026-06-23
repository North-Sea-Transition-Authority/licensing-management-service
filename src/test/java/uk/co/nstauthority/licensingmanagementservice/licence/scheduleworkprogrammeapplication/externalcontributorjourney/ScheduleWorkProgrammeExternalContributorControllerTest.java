package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.externalcontributorjourney;

import static org.mockito.ArgumentMatchers.any;
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
import static uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.externalcontributorjourney.ScheduleWorkProgrammeExternalContributorController.PAGE_TITLE;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.validation.Errors;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.application.externalcontributors.ExternalContributorForm;
import uk.co.nstauthority.licensingmanagementservice.licence.application.externalcontributors.ExternalContributorFormValidator;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceSchedule;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplication;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.tasklist.ScheduleWorkProgrammeApplicationTaskListController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.teams.Team;
import uk.co.nstauthority.licensingmanagementservice.teams.management.TeamManagementController;

@ContextConfiguration(classes = ScheduleWorkProgrammeExternalContributorController.class)
class ScheduleWorkProgrammeExternalContributorControllerTest extends AbstractControllerTest {

  @MockitoBean
  private ScheduleWorkProgrammeExternalContributorService scheduleWorkProgrammeExternalContributorService;

  @MockitoBean
  private ExternalContributorFormValidator externalContributorFormValidator;

  private ServiceUserDetail organisationUser;
  private static final Long ORGANISATION_USER_WUA_ID = 2L;

  private static final UUID DETAIL_ID = UUID.randomUUID();
  private ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail;

  @BeforeEach
  void setUp() {
    organisationUser = ServiceUserDetailTestUtil.newBuilder()
        .withWuaId(ORGANISATION_USER_WUA_ID)
        .build();

    var application = new ScheduleWorkProgrammeApplication();
    application.setId(UUID.randomUUID());
    var licenceSchedule = new LicenceSchedule();
    licenceSchedule.setLicence(new Licence());
    application.setLicenceSchedule(licenceSchedule);

    scheduleWorkProgrammeApplicationDetail = ScheduleWorkProgrammeApplicationDetailTestUtil.builder()
        .withId(DETAIL_ID)
        .withStatus(ScheduleWorkProgrammeApplicationStatus.DRAFT)
        .withScheduleWorkProgrammeApplication(application)
        .build();

    when(scheduleWorkProgrammeApplicationService.getDetailByIdOrThrow(DETAIL_ID))
        .thenReturn(scheduleWorkProgrammeApplicationDetail);
  }

  @Test
  void renderForm() throws Exception {
    var form = new ExternalContributorForm();

    when(scheduleWorkProgrammeExternalContributorService.getExternalContributorForm(scheduleWorkProgrammeApplicationDetail))
        .thenReturn(form);
    when(applicationAccessService.userHasAccessToApplication(any(), any(), any())).thenReturn(true);

    mockMvc.perform(
            get(ReverseRouter.route(on(ScheduleWorkProgrammeExternalContributorController.class)
                .renderForm(DETAIL_ID, null)))
                .with(user(organisationUser))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/application/externalContributor"))
        .andExpect(model().attribute("pageTitle", PAGE_TITLE))
        .andExpect(model().attribute("form", form))
        .andExpect(model().attribute("cancelUrl", ReverseRouter.route(
            on(ScheduleWorkProgrammeApplicationTaskListController.class).getTaskList(DETAIL_ID, null, null))));
  }

  @Test
  void submitForm_whenYes_redirectsToExternalContributorsTeamList() throws Exception {
    var form = new ExternalContributorForm();
    form.setAddExternalContributors(true);
    var team = new Team(UUID.randomUUID());

    when(externalContributorFormValidator.isValid(any(Errors.class))).thenReturn(true);
    when(applicationAccessService.userHasAccessToApplication(any(), any(), any())).thenReturn(true);
    when(scheduleWorkProgrammeExternalContributorService
        .getExternalContributorsTeam(scheduleWorkProgrammeApplicationDetail)).thenReturn(team);

    mockMvc.perform(post(ReverseRouter.route(on(ScheduleWorkProgrammeExternalContributorController.class).submitForm(
                DETAIL_ID, null, form, null)))
                .with(user(organisationUser))
                .with(csrf())
                .flashAttr("form", form))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(TeamManagementController.class)
            .renderExternalContributorsTeamList(team.getId(), null))));

    verify(scheduleWorkProgrammeExternalContributorService)
        .saveExternalContributorForm(form, scheduleWorkProgrammeApplicationDetail);
  }

  @Test
  void submitForm_whenNo_redirectsToTaskList() throws Exception {
    var form = new ExternalContributorForm();
    form.setAddExternalContributors(false);

    when(externalContributorFormValidator.isValid(any(Errors.class))).thenReturn(true);
    when(applicationAccessService.userHasAccessToApplication(any(), any(), any())).thenReturn(true);

    mockMvc.perform(post(ReverseRouter.route(on(ScheduleWorkProgrammeExternalContributorController.class).submitForm(
                DETAIL_ID, null, form, null)))
                .with(user(organisationUser))
                .with(csrf())
                .flashAttr("form", form))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(ScheduleWorkProgrammeApplicationTaskListController.class)
            .getTaskList(DETAIL_ID, null, null))));

    verify(scheduleWorkProgrammeExternalContributorService)
        .saveExternalContributorForm(form, scheduleWorkProgrammeApplicationDetail);
  }

  @Test
  void renderForm_whenNoAccess_isForbidden() throws Exception {
    when(applicationAccessService.userHasAccessToApplication(any(), any(), any())).thenReturn(false);

    mockMvc.perform(
            get(ReverseRouter.route(on(ScheduleWorkProgrammeExternalContributorController.class)
                .renderForm(DETAIL_ID, null)))
                .with(user(organisationUser)))
        .andExpect(status().isForbidden());

    verifyNoInteractions(scheduleWorkProgrammeExternalContributorService);
  }

  @Test
  void submitForm_whenNoAccess_isForbidden() throws Exception {
    when(applicationAccessService.userHasAccessToApplication(any(), any(), any())).thenReturn(false);

    mockMvc.perform(post(ReverseRouter.route(
            on(ScheduleWorkProgrammeExternalContributorController.class).submitForm(DETAIL_ID, null, null, null)))
                .with(user(organisationUser))
                .with(csrf()))
        .andExpect(status().isForbidden());

    verifyNoInteractions(scheduleWorkProgrammeExternalContributorService);
  }
}
