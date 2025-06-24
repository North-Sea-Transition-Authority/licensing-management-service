package uk.co.nstauthority.licensingmanagementservice.licence;

import static org.mockito.ArgumentMatchers.any;
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
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitRestController;
import uk.co.nstauthority.licensingmanagementservice.fds.searchselector.SearchSelectorService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.util.SecurityTest;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.DisplayableEnumOptionUtil;

@ContextConfiguration(classes = NewLicenceController.class)
class NewLicenceControllerTest extends AbstractControllerTest {

  @MockitoBean
  private NewLicenceFormService newLicenceFormService;

  @MockitoBean
  private NewLicenceValidator newLicenceValidator;

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
    when(newLicenceFormService.getPreselectedOrganisationUnits(List.of())).thenReturn(List.of());

    mockMvc.perform(
        get(ReverseRouter.route(on(NewLicenceController.class).renderNewLicenceForm()))
            .with(user(organisationUser))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/newLicence"))
        .andExpect(model().attribute("licenceTypeOptions",
            DisplayableEnumOptionUtil.getDisplayableOptions(LicenceType.getLicenceTypesManagedByLms())))
        .andExpect(model().attribute("preselectedItems", List.of()))
        .andExpect(model().attribute("organisationUnitSearchEndpoint",
            SearchSelectorService.route(on(OrganisationUnitRestController.class).searchOrganisationUnits(null))));
  }

  @SecurityTest
  void saveNewLicence_formIsValid() throws Exception {
    when(newLicenceValidator.isValid(any(), any())).thenReturn(true);

    mockMvc.perform(
        post(ReverseRouter.route(on(NewLicenceController.class).saveNewLicence(null, null)))
            .with(user(organisationUser))
            .with(csrf())
    )
    .andExpect(status().is3xxRedirection());
  }

  @SecurityTest
  void saveNewLicence_formIsNotValid() throws Exception {
    when(newLicenceValidator.isValid(any(), any())).thenReturn(false);
    when(newLicenceFormService.getPreselectedOrganisationUnits(List.of())).thenReturn(List.of());

    mockMvc.perform(
        post(ReverseRouter.route(on(NewLicenceController.class).saveNewLicence(null, null)))
            .with(user(organisationUser))
            .with(csrf())
    )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/newLicence"))
        .andExpect(model().attribute("licenceTypeOptions",
            DisplayableEnumOptionUtil.getDisplayableOptions(LicenceType.getLicenceTypesManagedByLms())))
        .andExpect(model().attribute("preselectedItems", List.of()))
        .andExpect(model().attribute("organisationUnitSearchEndpoint",
            SearchSelectorService.route(on(OrganisationUnitRestController.class).searchOrganisationUnits(null))));
  }

}