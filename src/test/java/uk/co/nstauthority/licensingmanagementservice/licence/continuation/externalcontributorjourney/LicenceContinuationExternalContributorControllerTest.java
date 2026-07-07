package uk.co.nstauthority.licensingmanagementservice.licence.continuation.externalcontributorjourney;

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
import static uk.co.nstauthority.licensingmanagementservice.licence.continuation.externalcontributorjourney.LicenceContinuationExternalContributorController.PAGE_TITLE;

import java.util.Map;
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
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.application.externalcontributors.ExternalContributorForm;
import uk.co.nstauthority.licensingmanagementservice.licence.application.externalcontributors.ExternalContributorFormValidator;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.tasklist.LicenceContinuationApplicationTaskListController;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.teams.Team;
import uk.co.nstauthority.licensingmanagementservice.teams.management.TeamManagementController;
import uk.co.nstauthority.licensingmanagementservice.workarea.WorkAreaController;

@ContextConfiguration(classes = LicenceContinuationExternalContributorController.class)
class LicenceContinuationExternalContributorControllerTest extends AbstractControllerTest {

  @MockitoBean
  private LicenceContinuationExternalContributorService licenceContinuationExternalContributorService;

  @MockitoBean
  private ExternalContributorFormValidator externalContributorFormValidator;

  private static final Licence LICENCE = LicenceTestUtil.builder().build();
  private static final LicenceScheduleDetail LICENCE_SCHEDULE_DETAIL
      = LicenceScheduleTestUtil.createLicenceScheduleDetail(LicenceScheduleTestUtil.createLicenceSchedule(LICENCE));
  private static final LicenceContinuationApplicationDetail LICENCE_CONTINUATION_APPLICATION_DETAIL
      = LicenceContinuationApplicationTestUtil.createLicenceContinuationApplicationDetail(LICENCE_SCHEDULE_DETAIL);

  private ServiceUserDetail organisationUser;
  private static final Long ORGANISATION_USER_WUA_ID = 2L;

  @BeforeEach
  void setUp() {
    organisationUser = ServiceUserDetailTestUtil.newBuilder()
        .withWuaId(ORGANISATION_USER_WUA_ID)
        .build();
  }

  @Test
  void renderForm() throws Exception {
    var form = new ExternalContributorForm();

    when(licenceContinuationExternalContributorService.getExternalContributorForm(LICENCE_CONTINUATION_APPLICATION_DETAIL))
        .thenReturn(form);
    when(applicationAccessService.userHasAccessToApplication(
        LICENCE_CONTINUATION_APPLICATION_DETAIL, Map.of(), organisationUser.wuaId()))
        .thenReturn(true);
    when(licenceContinuationService.getDetailByIdOrThrow(LICENCE_CONTINUATION_APPLICATION_DETAIL.getId()))
        .thenReturn(LICENCE_CONTINUATION_APPLICATION_DETAIL);

    mockMvc.perform(
            get(ReverseRouter.route(on(LicenceContinuationExternalContributorController.class)
                .renderForm(LICENCE_CONTINUATION_APPLICATION_DETAIL.getId(), null)))
                .with(user(organisationUser))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/application/externalContributor"))
        .andExpect(model().attribute("pageTitle", PAGE_TITLE))
        .andExpect(model().attribute("form", form))
        .andExpect(model().attribute("cancelUrl", ReverseRouter.route(
            on(LicenceContinuationApplicationTaskListController.class)
                .getTaskList(LICENCE_CONTINUATION_APPLICATION_DETAIL.getId(), null, null))))
        .andExpect(model().attribute("breadcrumbs", Map.of(
            ReverseRouter.route(on(WorkAreaController.class).getWorkArea(null, null)), "Work area",
            ReverseRouter.route(on(LicenceContinuationApplicationTaskListController.class).getTaskList(LICENCE_CONTINUATION_APPLICATION_DETAIL.getId(), null, null)), "Task list"
        )))
        .andExpect(model().attribute("currentPage", PAGE_TITLE));
  }

  @Test
  void submitForm_whenYes_redirectsToExternalContributorsTeamList() throws Exception {
    var form = new ExternalContributorForm();
    form.setAddExternalContributors(true);
    var team = new Team(UUID.randomUUID());

    when(externalContributorFormValidator.isValid(any(Errors.class)))
        .thenReturn(true);
    when(applicationAccessService.userHasAccessToApplication(
        LICENCE_CONTINUATION_APPLICATION_DETAIL, Map.of(), organisationUser.wuaId()))
        .thenReturn(true);
    when(licenceContinuationService.getDetailByIdOrThrow(LICENCE_CONTINUATION_APPLICATION_DETAIL.getId()))
        .thenReturn(LICENCE_CONTINUATION_APPLICATION_DETAIL);
    when(licenceContinuationExternalContributorService.getExternalContributorsTeam(LICENCE_CONTINUATION_APPLICATION_DETAIL))
        .thenReturn(team);

    mockMvc.perform(post(ReverseRouter.route(on(LicenceContinuationExternalContributorController.class).submitForm(
                LICENCE_CONTINUATION_APPLICATION_DETAIL.getId(),
                null,
                form,
                null
            )))
                .with(user(organisationUser))
                .with(csrf())
                .flashAttr("form", form))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(TeamManagementController.class)
            .renderExternalContributorsTeamList(team.getId(), null))));

    verify(licenceContinuationExternalContributorService)
        .saveExternalContributorForm(form, LICENCE_CONTINUATION_APPLICATION_DETAIL);
  }

  @Test
  void submitForm_whenNo_redirectsToTaskList() throws Exception {
    var form = new ExternalContributorForm();
    form.setAddExternalContributors(false);

    when(externalContributorFormValidator.isValid(any(Errors.class)))
        .thenReturn(true);
    when(applicationAccessService.userHasAccessToApplication(
        LICENCE_CONTINUATION_APPLICATION_DETAIL, Map.of(), organisationUser.wuaId()))
        .thenReturn(true);
    when(licenceContinuationService.getDetailByIdOrThrow(LICENCE_CONTINUATION_APPLICATION_DETAIL.getId()))
        .thenReturn(LICENCE_CONTINUATION_APPLICATION_DETAIL);

    mockMvc.perform(post(ReverseRouter.route(on(LicenceContinuationExternalContributorController.class).submitForm(
                LICENCE_CONTINUATION_APPLICATION_DETAIL.getId(),
                null,
                form,
                null
            )))
                .with(user(organisationUser))
                .with(csrf())
                .flashAttr("form", form))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(LicenceContinuationApplicationTaskListController.class)
            .getTaskList(LICENCE_CONTINUATION_APPLICATION_DETAIL.getId(), null, null))));

    verify(licenceContinuationExternalContributorService)
        .saveExternalContributorForm(form, LICENCE_CONTINUATION_APPLICATION_DETAIL);
  }

  @Test
  void renderForm_whenNoAccess_isForbidden() throws Exception {
    when(licenceContinuationService.getDetailByIdOrThrow(LICENCE_CONTINUATION_APPLICATION_DETAIL.getId()))
        .thenReturn(LICENCE_CONTINUATION_APPLICATION_DETAIL);
    when(applicationAccessService.userHasAccessToApplication(
        LICENCE_CONTINUATION_APPLICATION_DETAIL, Map.of(), organisationUser.wuaId()))
        .thenReturn(false);

    mockMvc.perform(
            get(ReverseRouter.route(on(LicenceContinuationExternalContributorController.class)
                .renderForm(LICENCE_CONTINUATION_APPLICATION_DETAIL.getId(), null)))
                .with(user(organisationUser)))
        .andExpect(status().isForbidden());

    verifyNoInteractions(licenceContinuationExternalContributorService);
  }

  @Test
  void submitForm_whenNoAccess_isForbidden() throws Exception {
    when(licenceContinuationService.getDetailByIdOrThrow(LICENCE_CONTINUATION_APPLICATION_DETAIL.getId()))
        .thenReturn(LICENCE_CONTINUATION_APPLICATION_DETAIL);
    when(applicationAccessService.userHasAccessToApplication(
        LICENCE_CONTINUATION_APPLICATION_DETAIL, Map.of(), organisationUser.wuaId()))
        .thenReturn(false);

    mockMvc.perform(post(ReverseRouter.route(
            on(LicenceContinuationExternalContributorController.class)
                .submitForm(LICENCE_CONTINUATION_APPLICATION_DETAIL.getId(), null, null, null)))
                .with(user(organisationUser))
                .with(csrf()))
        .andExpect(status().isForbidden());

    verifyNoInteractions(licenceContinuationExternalContributorService);
  }
}
