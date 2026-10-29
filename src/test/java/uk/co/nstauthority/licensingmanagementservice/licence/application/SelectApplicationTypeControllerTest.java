package uk.co.nstauthority.licensingmanagementservice.licence.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import static uk.co.nstauthority.licensingmanagementservice.licence.application.SelectApplicationTypeController.PAGE_TITLE;

import java.util.List;
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
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.startjourney.StartContinuationApplicationController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@ContextConfiguration(classes = SelectApplicationTypeController.class)
class SelectApplicationTypeControllerTest extends AbstractControllerTest {

  private static final LicenceType LICENCE_TYPE = LicenceType.SEAWARD_PRODUCTION;
  private static final List<ApplicationType> AVAILABLE_APPLICATION_TYPES = List.of(
      ApplicationType.SCHEDULE_AMENDMENT_APPLICATION,
      ApplicationType.CONTINUATION_APPLICATION
  );

  @MockitoBean
  private SelectApplicationTypeFormValidator selectApplicationTypeFormValidator;

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
    when(selectApplicationTypeService.getLicenceTypeOrThrow(LICENCE_TYPE.getUrlSlug())).thenReturn(LICENCE_TYPE);
    when(selectApplicationTypeService.getAvailableApplicationTypes(LICENCE_TYPE)).thenReturn(AVAILABLE_APPLICATION_TYPES);

    mockMvc.perform(
            get(ReverseRouter.route(on(SelectApplicationTypeController.class).render(LICENCE_TYPE.getUrlSlug())))
                .with(user(organisationUser))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/application/selectApplicationType"))
        .andExpect(model().attribute("pageTitle", PAGE_TITLE))
        .andExpect(model().attribute("pageCaption", LICENCE_TYPE.getDisplayName()))
        .andExpect(model().attribute("applicationTypeOptions",
            ApplicationType.getSelectionDisplayOptions(AVAILABLE_APPLICATION_TYPES)))
        .andExpect(model().attribute("backUrl",
            ReverseRouter.route(on(SelectApplicationLicenceTypeController.class).renderSelectLicenceType())));
  }

  @Test
  void render_whenNoApplicationTypesForLicenceType_thenNotFound() throws Exception {
    var licenceTypeSlug = LicenceType.SEAWARD_EXPLORATION.getUrlSlug();
    when(industryRoleService.userHasEditorOrSubmitterRole(organisationUser.wuaId())).thenReturn(true);
    when(selectApplicationTypeService.getLicenceTypeOrThrow(licenceTypeSlug))
        .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));

    mockMvc.perform(
            get(ReverseRouter.route(on(SelectApplicationTypeController.class).render(licenceTypeSlug)))
                .with(user(organisationUser))
        )
        .andExpect(status().isNotFound());
  }

  @Test
  void submit() throws Exception {
    when(industryRoleService.userHasEditorOrSubmitterRole(organisationUser.wuaId())).thenReturn(true);
    when(selectApplicationTypeService.getLicenceTypeOrThrow(LICENCE_TYPE.getUrlSlug())).thenReturn(LICENCE_TYPE);
    when(selectApplicationTypeService.getAvailableApplicationTypes(LICENCE_TYPE)).thenReturn(AVAILABLE_APPLICATION_TYPES);
    when(selectApplicationTypeFormValidator.isValid(any(), any(), eq(AVAILABLE_APPLICATION_TYPES))).thenReturn(true);

    var applicationType = ApplicationType.CONTINUATION_APPLICATION;

    var form = new SelectApplicationTypeForm();
    form.setSelectedApplicationType(applicationType);

    when(selectApplicationTypeService.getJourneyStartRedirectRoute(applicationType, LICENCE_TYPE))
        .thenReturn(ReverseRouter.redirect(on(StartContinuationApplicationController.class).render(LICENCE_TYPE.getUrlSlug())));

    mockMvc.perform(
            post(ReverseRouter.route(on(SelectApplicationTypeController.class).submit(LICENCE_TYPE.getUrlSlug(), form, null)))
                .with(user(organisationUser))
                .with(csrf())
                .flashAttr("form", form)
        )
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(StartContinuationApplicationController.class)
            .render(LICENCE_TYPE.getUrlSlug()))));
  }

  @Test
  void submit_invalid() throws Exception {
    when(industryRoleService.userHasEditorOrSubmitterRole(organisationUser.wuaId())).thenReturn(true);
    when(selectApplicationTypeService.getLicenceTypeOrThrow(LICENCE_TYPE.getUrlSlug())).thenReturn(LICENCE_TYPE);
    when(selectApplicationTypeService.getAvailableApplicationTypes(LICENCE_TYPE)).thenReturn(AVAILABLE_APPLICATION_TYPES);
    when(selectApplicationTypeFormValidator.isValid(any(), any(), eq(AVAILABLE_APPLICATION_TYPES))).thenReturn(false);

    mockMvc.perform(
            post(ReverseRouter.route(on(SelectApplicationTypeController.class).submit(LICENCE_TYPE.getUrlSlug(), null, null)))
                .with(user(organisationUser))
                .with(csrf())
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/application/selectApplicationType"))
        .andExpect(model().attribute("pageTitle", PAGE_TITLE))
        .andExpect(model().attribute("pageCaption", LICENCE_TYPE.getDisplayName()))
        .andExpect(model().attribute("applicationTypeOptions",
            ApplicationType.getSelectionDisplayOptions(AVAILABLE_APPLICATION_TYPES)))
        .andExpect(model().attribute("backUrl",
            ReverseRouter.route(on(SelectApplicationLicenceTypeController.class).renderSelectLicenceType())));
  }

  @Test
  void render_ForbiddenUserNoAccess() throws Exception {
    when(industryRoleService.userHasEditorOrSubmitterRole(organisationUser.wuaId())).thenReturn(false);

    mockMvc.perform(
            get(ReverseRouter.route(on(SelectApplicationTypeController.class).render(LICENCE_TYPE.getUrlSlug())))
                .with(user(organisationUser))
        )
        .andExpect(status().isForbidden());

    verifyNoInteractions(selectApplicationTypeService);
  }

  @Test
  void submit_ForbiddenUserNoAccess() throws Exception {
    when(industryRoleService.userHasEditorOrSubmitterRole(organisationUser.wuaId())).thenReturn(false);

    mockMvc.perform(
            post(ReverseRouter.route(on(SelectApplicationTypeController.class).submit(LICENCE_TYPE.getUrlSlug(), null, null)))
                .with(user(organisationUser))
                .with(csrf())
        )
        .andExpect(status().isForbidden());

    verifyNoInteractions(selectApplicationTypeService);
  }
}
