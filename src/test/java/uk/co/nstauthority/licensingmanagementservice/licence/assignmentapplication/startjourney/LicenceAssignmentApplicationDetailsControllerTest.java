package uk.co.nstauthority.licensingmanagementservice.licence.assignmentapplication.startjourney;

import static org.hamcrest.Matchers.instanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
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

import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTypeArgumentResolver;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationType;
import uk.co.nstauthority.licensingmanagementservice.licence.application.SelectApplicationTypeController;
import uk.co.nstauthority.licensingmanagementservice.licence.application.SelectApplicationTypeService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@ContextConfiguration(classes = LicenceAssignmentApplicationDetailsController.class)
class LicenceAssignmentApplicationDetailsControllerTest extends AbstractControllerTest {

  private static final LicenceType LICENCE_TYPE = LicenceType.CARBON_STORAGE;

  private final ServiceUserDetail organisationUser = ServiceUserDetailTestUtil.newBuilder().build();

  @MockitoBean
  private SelectApplicationTypeService selectApplicationTypeService;

  @MockitoBean
  private LicenceAssignmentApplicationStartJourneyService licenceAssignmentApplicationStartJourneyService;

  @MockitoBean
  private LicenceAssignmentApplicationDetailsFormValidator formValidator;

  @Test
  void render() throws Exception {
    when(industryRoleService.userHasEditorOrSubmitterRole(organisationUser.wuaId())).thenReturn(true);
    when(licenceAssignmentApplicationStartJourneyService.getOrganisationOptions(organisationUser))
        .thenReturn(Map.of("1", "Organisation"));

    mockMvc.perform(
            get(ReverseRouter.route(on(LicenceAssignmentApplicationDetailsController.class)
                .render(LICENCE_TYPE, null)))
                .with(user(organisationUser))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/assignmentapplication/applicationDetails"))
        .andExpect(model().attribute("pageCaption", "Carbon storage"))
        .andExpect(model().attribute("form", instanceOf(LicenceAssignmentApplicationDetailsForm.class)))
        .andExpect(model().attribute("organisationOptions", Map.of("1", "Organisation")))
        .andExpect(model().attribute(
            "backUrl",
            ReverseRouter.route(on(SelectApplicationTypeController.class).render(LICENCE_TYPE.getUrlSlug()))
        ));
  }

  @Test
  void render_whenLicenceAssignmentNotAvailableForLicenceType_thenNotFound() throws Exception {
    var licenceType = LicenceType.GAS_STORAGE;
    when(industryRoleService.userHasEditorOrSubmitterRole(organisationUser.wuaId())).thenReturn(true);
    doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND))
        .when(selectApplicationTypeService)
        .verifyApplicationTypeCanBeStarted(licenceType, ApplicationType.LICENCE_ASSIGNMENT_APPLICATION);

    mockMvc.perform(
            get(ReverseRouter.route(on(LicenceAssignmentApplicationDetailsController.class).render(licenceType, null)))
                .with(user(organisationUser))
        )
        .andExpect(status().isNotFound());
  }

  @Test
  void render_whenUnknownLicenceTypeSlug_thenNotFound() throws Exception {
    when(industryRoleService.userHasEditorOrSubmitterRole(organisationUser.wuaId())).thenReturn(true);

    mockMvc.perform(
            get(ReverseRouter.route(
                on(LicenceAssignmentApplicationDetailsController.class).render(null, null),
                Map.of(LicenceTypeArgumentResolver.LICENCE_TYPE_SLUG, "not-a-licence-type")
            ))
                .with(user(organisationUser))
        )
        .andExpect(status().isNotFound());

    verifyNoInteractions(selectApplicationTypeService);
  }

  @Test
  void render_whenUserCannotStartApplication_thenForbidden() throws Exception {
    when(industryRoleService.userHasEditorOrSubmitterRole(organisationUser.wuaId())).thenReturn(false);

    mockMvc.perform(
            get(ReverseRouter.route(on(LicenceAssignmentApplicationDetailsController.class)
                .render(LICENCE_TYPE, null)))
                .with(user(organisationUser))
        )
        .andExpect(status().isForbidden());
  }

  @Test
  void submit_whenFormValid_thenRedirectToLicences() throws Exception {
    when(industryRoleService.userHasEditorOrSubmitterRole(organisationUser.wuaId())).thenReturn(true);
    when(industryRoleService.getEditorOrSubmitterOrganisationUnitIds(organisationUser.wuaId())).thenReturn(Set.of(1));
    when(formValidator.isValid(any(), any(), eq(Set.of(1)))).thenReturn(true);

    mockMvc.perform(
            post(ReverseRouter.route(on(LicenceAssignmentApplicationDetailsController.class)
                .submit(LICENCE_TYPE, null, null, null)))
                .with(user(organisationUser))
                .with(csrf())
        )
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(LicenceAssignmentApplicationLicencesController.class)
            .render(LICENCE_TYPE))));
  }

  @Test
  void submit_whenFormInvalid_thenRenderDetails() throws Exception {
    when(industryRoleService.userHasEditorOrSubmitterRole(organisationUser.wuaId())).thenReturn(true);
    when(industryRoleService.getEditorOrSubmitterOrganisationUnitIds(organisationUser.wuaId())).thenReturn(Set.of(1));
    when(licenceAssignmentApplicationStartJourneyService.getOrganisationOptions(organisationUser))
        .thenReturn(Map.of("1", "Organisation"));
    when(formValidator.isValid(any(), any(), eq(Set.of(1)))).thenReturn(false);

    mockMvc.perform(
            post(ReverseRouter.route(on(LicenceAssignmentApplicationDetailsController.class)
                .submit(LICENCE_TYPE, null, null, null)))
                .with(user(organisationUser))
                .with(csrf())
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/assignmentapplication/applicationDetails"))
        .andExpect(model().attribute("organisationOptions", Map.of("1", "Organisation")));
  }
}
