package uk.co.nstauthority.licensingmanagementservice.licence;

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
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitJson;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitRestController;
import uk.co.nstauthority.licensingmanagementservice.fds.searchselector.SearchSelectorService;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisationService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.util.SecurityTest;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.DisplayableEnumOptionUtil;

@ContextConfiguration(classes = LicenceController.class)
class LicenceControllerTest extends AbstractControllerTest {

  @MockitoBean
  private LicenceFormService licenceFormService;

  @MockitoBean
  private NewLicenceValidator newLicenceValidator;

  @MockitoBean
  private LicenceService licenceService;

  @MockitoBean
  private ManageLicenseesValidator manageLicenseesValidator;

  @MockitoBean
  private LicenceResponsibleOrganisationService licenceResponsibleOrganisationService;

  private ServiceUserDetail organisationUser;
  private static final Long ORGANISATION_USER_WUA_ID = 2L;

  @BeforeEach
  void setUp() {
    organisationUser = ServiceUserDetailTestUtil.newBuilder()
        .withWuaId(ORGANISATION_USER_WUA_ID)
        .build();
  }

  @SecurityTest
  void renderNewLicenceForm() throws Exception {
    when(licenceFormService.getPreselectedOrganisationUnits(List.of())).thenReturn(List.of());

    mockMvc.perform(
        get(ReverseRouter.route(on(LicenceController.class).renderNewLicenceForm()))
            .with(user(organisationUser))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/newLicence"))
        .andExpect(model().attribute("licenceTypeOptions",
            DisplayableEnumOptionUtil.getDisplayableOptions(LicenceType.getLicenceTypesManagedByLms())))
        .andExpect(model().attribute("preselectedOrgUnits", List.of()))
        .andExpect(model().attribute("organisationUnitSearchEndpoint",
            SearchSelectorService.route(on(OrganisationUnitRestController.class).searchOrganisationUnits(null))));
  }

  @SecurityTest
  void saveNewLicence_formIsValid() throws Exception {
    when(newLicenceValidator.isValid(any(), any())).thenReturn(true);

    mockMvc.perform(
        post(ReverseRouter.route(on(LicenceController.class).saveNewLicence(null, null)))
            .with(user(organisationUser))
            .with(csrf())
    )
    .andExpect(status().is3xxRedirection());

    verify(licenceFormService).saveNewLicenceFromForm(any());
  }

  @SecurityTest
  void saveNewLicence_formIsNotValid() throws Exception {
    when(newLicenceValidator.isValid(any(), any())).thenReturn(false);
    when(licenceFormService.getPreselectedOrganisationUnits(List.of())).thenReturn(List.of());

    mockMvc.perform(
        post(ReverseRouter.route(on(LicenceController.class).saveNewLicence(null, null)))
            .with(user(organisationUser))
            .with(csrf())
    )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/newLicence"))
        .andExpect(model().attribute("licenceTypeOptions",
            DisplayableEnumOptionUtil.getDisplayableOptions(LicenceType.getLicenceTypesManagedByLms())))
        .andExpect(model().attribute("preselectedOrgUnits", List.of()))
        .andExpect(model().attribute("organisationUnitSearchEndpoint",
            SearchSelectorService.route(on(OrganisationUnitRestController.class).searchOrganisationUnits(null))));
  }


  @SecurityTest
  void renderManageLicenseesPage() throws Exception {
    var licence = new Licence();
    licence.setType(LicenceType.CARBON_STORAGE);

    var selectedOrgUnits = List.of(new OrganisationUnitJson(1, "org name"));

    when(licenceService.findLicenceByIdOrThrow(1)).thenReturn(licence);
    when(licenceFormService.getSavedOrganisationUnits(licence)).thenReturn(selectedOrgUnits);

    mockMvc.perform(
            get(ReverseRouter.route(on(LicenceController.class).renderManageLicenseesPage(1)))
                .with(user(organisationUser))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/manageLicensees"))
        .andExpect(model().attribute("preselectedOrgUnits", selectedOrgUnits))
        .andExpect(model().attribute("organisationUnitSearchEndpoint",
            SearchSelectorService.route(on(OrganisationUnitRestController.class).searchOrganisationUnits(null))));
  }

  @SecurityTest
  void renderManageLicenseesPage_licenceNotManagedByLms() throws Exception {
    var licence = new Licence();
    licence.setType(LicenceType.SEAWARD_PRODUCTION);

    when(licenceService.findLicenceByIdOrThrow(1)).thenReturn(licence);

    mockMvc.perform(
            get(ReverseRouter.route(on(LicenceController.class).renderManageLicenseesPage(1)))
                .with(user(organisationUser))
        )
        .andExpect(status().isForbidden());
  }

  @SecurityTest
  void saveManageLicenseesPage_formIsValid() throws Exception {
    var licence = new Licence();
    licence.setType(LicenceType.CARBON_STORAGE);

    when(licenceService.findLicenceByIdOrThrow(1)).thenReturn(licence);
    when(manageLicenseesValidator.isValid(any(), any())).thenReturn(true);

    mockMvc.perform(
            post(ReverseRouter.route(on(LicenceController.class).saveManageLicenseesPage(1, null, null)))
                .with(user(organisationUser))
                .with(csrf())
        )
        .andExpect(status().is3xxRedirection());

    verify(licenceResponsibleOrganisationService).saveLicenseesFromForm(eq(licence), any());
  }

  @SecurityTest
  void saveManageLicenseesPage_formIsNotValid() throws Exception {
    var licence = new Licence();
    licence.setType(LicenceType.CARBON_STORAGE);

    when(licenceService.findLicenceByIdOrThrow(1)).thenReturn(licence);
    when(manageLicenseesValidator.isValid(any(), any())).thenReturn(false);
    when(licenceFormService.getPreselectedOrganisationUnits(List.of())).thenReturn(List.of());

    mockMvc.perform(
            post(ReverseRouter.route(on(LicenceController.class).saveManageLicenseesPage(1, null, null)))
                .with(user(organisationUser))
                .with(csrf())
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/manageLicensees"))
        .andExpect(model().attribute("preselectedOrgUnits", List.of()))
        .andExpect(model().attribute("organisationUnitSearchEndpoint",
            SearchSelectorService.route(on(OrganisationUnitRestController.class).searchOrganisationUnits(null))));

    verify(licenceFormService).getPreselectedOrganisationUnits(null);
  }

  @SecurityTest
  void saveManageLicenseesPage_licenceNotManagedByLms() throws Exception {
    var licence = new Licence();
    licence.setType(LicenceType.LANDWARD_PRODUCTION);

    when(licenceService.findLicenceByIdOrThrow(1)).thenReturn(licence);

    mockMvc.perform(
            post(ReverseRouter.route(on(LicenceController.class).saveManageLicenseesPage(1, null, null)))
                .with(user(organisationUser))
                .with(csrf())
        )
        .andExpect(status().isForbidden());
  }
}