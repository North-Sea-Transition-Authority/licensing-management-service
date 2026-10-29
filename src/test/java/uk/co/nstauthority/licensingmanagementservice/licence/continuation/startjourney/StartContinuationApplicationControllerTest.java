package uk.co.nstauthority.licensingmanagementservice.licence.continuation.startjourney;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import static uk.co.nstauthority.licensingmanagementservice.authentication.TestUserProvider.user;
import static uk.co.nstauthority.licensingmanagementservice.licence.continuation.startjourney.StartContinuationApplicationController.PAGE_TITLE;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationType;
import uk.co.nstauthority.licensingmanagementservice.licence.application.SelectApplicationTypeController;
import uk.co.nstauthority.licensingmanagementservice.licence.application.SelectApplicationTypeService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@ContextConfiguration(classes = StartContinuationApplicationController.class)
class StartContinuationApplicationControllerTest extends AbstractControllerTest {

  private static final LicenceType LICENCE_TYPE = LicenceType.SEAWARD_PRODUCTION;

  @MockitoBean
  private SelectApplicationTypeService selectApplicationTypeService;

  private ServiceUserDetail organisationUser;
  private static final Long ORGANISATION_USER_WUA_ID = 2L;

  @BeforeEach
  void setUp() {
    organisationUser = ServiceUserDetailTestUtil.newBuilder()
        .withWuaId(ORGANISATION_USER_WUA_ID)
        .build();
  }

  @Test
  void render() throws Exception {
    when(industryRoleService.userHasEditorOrSubmitterRole(organisationUser.wuaId())).thenReturn(true);
    when(selectApplicationTypeService.getLicenceTypeOrThrow(LICENCE_TYPE.getUrlSlug(), ApplicationType.CONTINUATION_APPLICATION))
        .thenReturn(LICENCE_TYPE);

    mockMvc.perform(
            get(ReverseRouter.route(on(StartContinuationApplicationController.class).render(LICENCE_TYPE.getUrlSlug())))
                .with(user(organisationUser))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/continuation/startJourney"))
        .andExpect(model().attribute("pageTitle", PAGE_TITLE))
        .andExpect(model().attribute("pageCaption", LICENCE_TYPE.getDisplayName()))
        .andExpect(model().attribute("startUrl", ReverseRouter.route(on(SelectContinuationApplicationLicenceController.class)
            .render(LICENCE_TYPE.getUrlSlug()))))
        .andExpect(model().attribute("backUrl", ReverseRouter.route(on(SelectApplicationTypeController.class)
            .render(LICENCE_TYPE.getUrlSlug()))));
  }

  @Test
  void render_whenContinuationNotRelevantToLicenceType_thenNotFound() throws Exception {
    var licenceTypeSlug = LicenceType.CARBON_STORAGE.getUrlSlug();
    when(industryRoleService.userHasEditorOrSubmitterRole(organisationUser.wuaId())).thenReturn(true);
    when(selectApplicationTypeService.getLicenceTypeOrThrow(licenceTypeSlug, ApplicationType.CONTINUATION_APPLICATION))
        .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));

    mockMvc.perform(
            get(ReverseRouter.route(on(StartContinuationApplicationController.class).render(licenceTypeSlug)))
                .with(user(organisationUser))
        )
        .andExpect(status().isNotFound());
  }

  @Test
  void render_ForbiddenUserNoAccess() throws Exception {
    when(industryRoleService.userHasEditorOrSubmitterRole(organisationUser.wuaId())).thenReturn(false);

    mockMvc.perform(
            get(ReverseRouter.route(on(StartContinuationApplicationController.class).render(LICENCE_TYPE.getUrlSlug())))
                .with(user(organisationUser))
        )
        .andExpect(status().isForbidden());
  }
}
