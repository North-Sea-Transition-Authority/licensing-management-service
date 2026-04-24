package uk.co.nstauthority.licensingmanagementservice.licence.continuation.startjourney;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import static uk.co.nstauthority.licensingmanagementservice.authentication.TestUserProvider.user;
import static uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.startjourney.LicenseeInformationController.PAGE_TITLE;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplication;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisationService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.teams.Team;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;

@ContextConfiguration(classes = {LicenceContinuationLicenseeInformationController.class})
class LicenceContinuationLicenseeInformationControllerTest extends AbstractControllerTest {

  @MockitoBean
  private LicenceContinuationLicenseeInformationFormValidator licenceContinuationLicenseeInformationFormValidator;

  @MockitoBean
  private LicenceResponsibleOrganisationService licenceResponsibleOrganisationService;

  private ServiceUserDetail organisationUser;
  private static final Long ORGANISATION_USER_WUA_ID = 2L;
  private static final String CAPTION = "Licence type - Licence ref";
  private static final Integer LICENCE_ID = 1;
  private static final Licence LICENCE = LicenceTestUtil
      .builder().withId(LICENCE_ID).build();

  @BeforeEach
  void setUp() {
    organisationUser = ServiceUserDetailTestUtil.newBuilder()
        .withWuaId(ORGANISATION_USER_WUA_ID)
        .build();

    when(licenceService.findLicenceByIdOrThrow(LICENCE_ID)).thenReturn(LICENCE);
    when(licenceService.getLicencePageCaption(LICENCE)).thenReturn(CAPTION);
  }

  @Test
  void render() throws Exception {
    when(applicationAccessService.userHasAccessToStartApplication(organisationUser.wuaId())).thenReturn(true);

    mockMvc.perform(
            get(ReverseRouter.route(on(LicenceContinuationLicenseeInformationController.class).renderConfirmLicenseePermission(LICENCE_ID, null, organisationUser)))
                .with(user(organisationUser))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/continuation/licenseeInformationContinuation"))
        .andExpect(model().attribute("pageTitle", PAGE_TITLE))
        .andExpect(model().attribute("pageCaption", CAPTION))
        .andExpect(model().attribute("backUrl",  ReverseRouter.route(on(SelectContinuationApplicationLicenceController.class).render())));
  }

  @Test
  void submit() throws Exception {
    when(applicationAccessService.userHasAccessToStartApplication(organisationUser.wuaId())).thenReturn(true);
    when(licenceContinuationLicenseeInformationFormValidator.isValid(any())).thenReturn(true);

    var licence = mock(Licence.class);
    when(licenceService.findLicenceByIdOrThrow(1)).thenReturn(licence);


    var form = new LicenceContinuationLicenseeInformationForm();
    form.setResponsibleOrganisationUnitId(1);

    var team = new Team(UUID.randomUUID());
    when(teamManagementService.createScopedTeam(any(), any(), any())).thenReturn(team);

    LicenceContinuationApplication licenceContinuationApplication = new LicenceContinuationApplication();
    licenceContinuationApplication.setId(UUID.randomUUID());

    var licenceContinuationApplicationDetail = LicenceContinuationApplicationTestUtil
        .builder()
        .withId(UUID.randomUUID())
        .withLicenceContinuationApplication(licenceContinuationApplication)
        .build();

    when(licenceContinuationService.createNewLicenceContinuationApplication(licence, 1))
        .thenReturn(licenceContinuationApplicationDetail);

    mockMvc.perform(
            post(ReverseRouter.route(on(LicenceContinuationLicenseeInformationController.class).submitLicenseePermissionConfirmation(1, null, form, null, organisationUser)))
                .with(user(organisationUser))
                .with(csrf())
                .flashAttr("form", form)
        )
        .andExpect(status().is3xxRedirection());

    verify(teamManagementService).createScopedTeam(eq(TeamType.EXTERNAL_CONTRIBUTORS.getDisplayName()), eq(TeamType.EXTERNAL_CONTRIBUTORS), any());
    verify(licenceContinuationService).createNewLicenceContinuationApplication(licence, 1);
  }

  @Test
  void submitLicenseePermissionConfirmation_invalid() throws Exception {
    when(licenceContinuationLicenseeInformationFormValidator.isValid(any())).thenReturn(false);
    when(applicationAccessService.userHasAccessToStartApplication(organisationUser.wuaId())).thenReturn(true);

    mockMvc.perform(
            post(ReverseRouter.route(on(LicenceContinuationLicenseeInformationController.class).submitLicenseePermissionConfirmation(LICENCE_ID, null,null, null, organisationUser)))
                .with(user(organisationUser))
                .with(csrf())
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/continuation/licenseeInformationContinuation"))
        .andExpect(model().attribute("pageTitle", PAGE_TITLE))
        .andExpect(model().attribute("pageCaption", CAPTION))
        .andExpect(model().attribute("backUrl",  ReverseRouter.route(on(SelectContinuationApplicationLicenceController.class).render())));

    verifyNoInteractions(scheduleWorkProgrammeApplicationService);
  }

  @Test
  void render_ForbiddenUserNoAccess() throws Exception {
    when(applicationAccessService.userHasAccessToStartApplication(organisationUser.wuaId())).thenReturn(false);

    mockMvc.perform(
            get(ReverseRouter.route(on(LicenceContinuationLicenseeInformationController.class).renderConfirmLicenseePermission(LICENCE_ID, null, organisationUser)))
                .with(user(organisationUser))
        )
        .andExpect(status().isForbidden());
  }

  @Test
  void submit_ForbiddenUserNoAccess() throws Exception {
    when(applicationAccessService.userHasAccessToStartApplication(organisationUser.wuaId())).thenReturn(false);

    mockMvc.perform(
            post(ReverseRouter.route(on(LicenceContinuationLicenseeInformationController.class).submitLicenseePermissionConfirmation(1, null, null, null, organisationUser)))
                .with(user(organisationUser))
                .with(csrf()))
        .andExpect(status().isForbidden());
  }

}