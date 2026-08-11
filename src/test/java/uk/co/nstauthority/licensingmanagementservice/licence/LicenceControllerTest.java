package uk.co.nstauthority.licensingmanagementservice.licence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitJson;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitRestController;
import uk.co.nstauthority.licensingmanagementservice.fds.searchselector.SearchSelectorService;
import uk.co.nstauthority.licensingmanagementservice.licence.search.LicenceSearchController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.DisplayableEnumOptionUtil;

@ContextConfiguration(classes = LicenceController.class)
class LicenceControllerTest extends AbstractControllerTest {

  @MockitoBean
  private LicenceFormService licenceFormService;

  @MockitoBean
  private NewLicenceValidator newLicenceValidator;

  @MockitoBean
  private EditLicenceDetailsValidator editLicenceDetailsValidator;

  private ServiceUserDetail organisationUser;
  private static final Long ORGANISATION_USER_WUA_ID = 2L;

  @BeforeEach
  void setUp() {
    organisationUser = ServiceUserDetailTestUtil.newBuilder()
        .withWuaId(ORGANISATION_USER_WUA_ID)
        .build();
  }

  @Test
  void renderNewLicenceForm() throws Exception {
    when(licenceFormService.getPreselectedOrganisationUnits(List.of())).thenReturn(List.of());
    when(teamQueryService.userHasRoleInTeamType(
        organisationUser.wuaId(),
        TeamType.LICENCE_MANAGEMENT,
        Set.of(Role.OFFLINE_LICENCE_ADMINISTRATOR))
    ).thenReturn(true);


    mockMvc.perform(
        get(ReverseRouter.route(on(LicenceController.class).renderNewLicenceForm()))
            .with(user(organisationUser))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/newLicence"))
        .andExpect(model().attribute("licenceTypeOptions", DisplayableEnumOptionUtil.getDisplayableOptions(LicenceType.getLicenceTypesManagedByLms())))
        .andExpect(model().attribute("licenceStatusOptions", DisplayableEnumOptionUtil.getDisplayableOptions(LicenceStatusType.class)))
        .andExpect(model().attribute("preselectedOrgUnits", List.of()))
        .andExpect(model().attribute("organisationUnitSearchEndpoint",
            SearchSelectorService.route(on(OrganisationUnitRestController.class).searchOrganisationUnits(null))))
        .andExpect(model().attribute("backUrl", ReverseRouter.route(on(LicenceSearchController.class).renderSearchPage(null, null))));
  }

  @Test
  void saveNewLicence_formIsValid() throws Exception {
    var licence = new Licence();
    licence.setId(1);

    when(newLicenceValidator.isValid(any(), any())).thenReturn(true);
    when(licenceFormService.saveNewLicenceFromForm(any())).thenReturn(licence);
    when(teamQueryService.userHasRoleInTeamType(
        organisationUser.wuaId(),
        TeamType.LICENCE_MANAGEMENT,
        Set.of(Role.OFFLINE_LICENCE_ADMINISTRATOR))
    ).thenReturn(true);

    mockMvc.perform(
        post(ReverseRouter.route(on(LicenceController.class).saveNewLicence(null, null)))
            .with(user(organisationUser))
            .with(csrf())
    )
    .andExpect(status().is3xxRedirection());

    verify(licenceFormService).saveNewLicenceFromForm(any());
  }

  @Test
  void saveNewLicence_formIsNotValid() throws Exception {
    when(newLicenceValidator.isValid(any(), any())).thenReturn(false);
    when(licenceFormService.getPreselectedOrganisationUnits(List.of())).thenReturn(List.of());
    when(teamQueryService.userHasRoleInTeamType(
        organisationUser.wuaId(),
        TeamType.LICENCE_MANAGEMENT,
        Set.of(Role.OFFLINE_LICENCE_ADMINISTRATOR))
    ).thenReturn(true);

    mockMvc.perform(
        post(ReverseRouter.route(on(LicenceController.class).saveNewLicence(null, null)))
            .with(user(organisationUser))
            .with(csrf())
    )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/newLicence"))
        .andExpect(model().attribute("licenceTypeOptions", DisplayableEnumOptionUtil.getDisplayableOptions(LicenceType.getLicenceTypesManagedByLms())))
        .andExpect(model().attribute("licenceStatusOptions", DisplayableEnumOptionUtil.getDisplayableOptions(LicenceStatusType.class)))
        .andExpect(model().attribute("preselectedOrgUnits", List.of()))
        .andExpect(model().attribute("organisationUnitSearchEndpoint",
            SearchSelectorService.route(on(OrganisationUnitRestController.class).searchOrganisationUnits(null))));
  }


  @Test
  void renderEditLicenceDetailsPage() throws Exception {
    var licence = new Licence();
    licence.setType(LicenceType.CARBON_STORAGE);
    licence.setLicenceReference("CS1");

    var selectedOrgUnits = List.of(new OrganisationUnitJson(1, "org name"));

    var editLicenceDetailsForm = new EditLicenceDetailsForm();
    editLicenceDetailsForm.setLicenceStatus(LicenceStatusType.EXTANT);

    when(licenceService.findLicenceByIdOrThrow(1)).thenReturn(licence);
    when(licenceFormService.getEditLicenceDetailsForm(licence)).thenReturn(editLicenceDetailsForm);
    when(licenceFormService.getSavedOrganisationUnits(licence)).thenReturn(selectedOrgUnits);
    when(teamQueryService.userHasRoleInTeamType(
        organisationUser.wuaId(),
        TeamType.LICENCE_MANAGEMENT,
        Set.of(Role.OFFLINE_LICENCE_ADMINISTRATOR))
    ).thenReturn(true);

    var result = mockMvc.perform(
            get(ReverseRouter.route(on(LicenceController.class).renderEditLicenceDetailsPage(1, null)))
                .with(user(organisationUser))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/editLicenceDetails"))
        .andExpect(model().attribute("licenceStatusOptions",
            DisplayableEnumOptionUtil.getDisplayableOptions(
                LicenceStatusType.getApplicableStatusesForLicenceType(LicenceType.CARBON_STORAGE))))
        .andExpect(model().attribute("preselectedOrgUnits", selectedOrgUnits))
        .andExpect(model().attribute("organisationUnitSearchEndpoint",
            SearchSelectorService.route(on(OrganisationUnitRestController.class).searchOrganisationUnits(null))))
        .andReturn();

    var form = (EditLicenceDetailsForm) result.getModelAndView().getModel().get("form");
    assertThat(form.getLicenceStatus()).isEqualTo(LicenceStatusType.EXTANT);
  }

  @Test
  void renderEditLicenceDetailsPage_licenceNotManagedByLms() throws Exception {
    var licence = new Licence();
    licence.setType(LicenceType.SEAWARD_PRODUCTION);

    when(licenceService.findLicenceByIdOrThrow(1)).thenReturn(licence);
    when(teamQueryService.userHasRoleInTeamType(
        organisationUser.wuaId(),
        TeamType.LICENCE_MANAGEMENT,
        Set.of(Role.OFFLINE_LICENCE_ADMINISTRATOR))
    ).thenReturn(true);

    mockMvc.perform(
            get(ReverseRouter.route(on(LicenceController.class).renderEditLicenceDetailsPage(1, null)))
                .with(user(organisationUser))
        )
        .andExpect(status().isForbidden());
  }

  @Test
  void saveEditLicenceDetailsPage_formIsValid() throws Exception {
    var licence = new Licence();
    licence.setType(LicenceType.CARBON_STORAGE);

    when(licenceService.findLicenceByIdOrThrow(1)).thenReturn(licence);
    when(editLicenceDetailsValidator.isValid(any(), any(), any())).thenReturn(true);
    when(teamQueryService.userHasRoleInTeamType(
        organisationUser.wuaId(),
        TeamType.LICENCE_MANAGEMENT,
        Set.of(Role.OFFLINE_LICENCE_ADMINISTRATOR))
    ).thenReturn(true);

    mockMvc.perform(
            post(ReverseRouter.route(on(LicenceController.class).saveEditLicenceDetailsPage(1, null, null, null)))
                .with(user(organisationUser))
                .with(csrf())
        )
        .andExpect(status().is3xxRedirection());

    verify(licenceFormService).saveEditLicenceDetailsFromForm(eq(licence), any());
  }

  @Test
  void saveEditLicenceDetailsPage_formIsNotValid() throws Exception {
    var licence = new Licence();
    licence.setType(LicenceType.CARBON_STORAGE);
    licence.setLicenceReference("CS1");

    when(licenceService.findLicenceByIdOrThrow(1)).thenReturn(licence);
    when(editLicenceDetailsValidator.isValid(any(), any(), any())).thenReturn(false);
    when(licenceFormService.getPreselectedOrganisationUnits(List.of())).thenReturn(List.of());
    when(teamQueryService.userHasRoleInTeamType(
        organisationUser.wuaId(),
        TeamType.LICENCE_MANAGEMENT,
        Set.of(Role.OFFLINE_LICENCE_ADMINISTRATOR))
    ).thenReturn(true);

    mockMvc.perform(
            post(ReverseRouter.route(on(LicenceController.class).saveEditLicenceDetailsPage(1, null, null, null)))
                .with(user(organisationUser))
                .with(csrf())
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/editLicenceDetails"))
        .andExpect(model().attribute("licenceStatusOptions",
            DisplayableEnumOptionUtil.getDisplayableOptions(
                LicenceStatusType.getApplicableStatusesForLicenceType(LicenceType.CARBON_STORAGE))))
        .andExpect(model().attribute("preselectedOrgUnits", List.of()))
        .andExpect(model().attribute("organisationUnitSearchEndpoint",
            SearchSelectorService.route(on(OrganisationUnitRestController.class).searchOrganisationUnits(null))));

    verify(licenceFormService).getPreselectedOrganisationUnits(null);
  }

  @Test
  void saveEditLicenceDetailsPage_licenceNotManagedByLms() throws Exception {
    var licence = new Licence();
    licence.setType(LicenceType.LANDWARD_PRODUCTION);

    when(licenceService.findLicenceByIdOrThrow(1)).thenReturn(licence);
    when(teamQueryService.userHasRoleInTeamType(
        organisationUser.wuaId(),
        TeamType.LICENCE_MANAGEMENT,
        Set.of(Role.OFFLINE_LICENCE_ADMINISTRATOR))
    ).thenReturn(true);

    mockMvc.perform(
            post(ReverseRouter.route(on(LicenceController.class).saveEditLicenceDetailsPage(1, null, null, null)))
                .with(user(organisationUser))
                .with(csrf())
        )
        .andExpect(status().isForbidden());
  }
}