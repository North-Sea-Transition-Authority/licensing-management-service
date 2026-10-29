package uk.co.nstauthority.licensingmanagementservice.licence.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import static uk.co.nstauthority.licensingmanagementservice.licence.application.SelectApplicationLicenceTypeController.PAGE_TITLE;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.DisplayableEnumOptionUtil;
import uk.co.nstauthority.licensingmanagementservice.workarea.WorkAreaController;

@ContextConfiguration(classes = SelectApplicationLicenceTypeController.class)
class SelectApplicationLicenceTypeControllerTest extends AbstractControllerTest {

  private static final List<LicenceType> AVAILABLE_LICENCE_TYPES =
      List.of(LicenceType.LANDWARD_PRODUCTION, LicenceType.CARBON_STORAGE);

  @MockitoBean
  private SelectApplicationLicenceTypeFormValidator selectApplicationLicenceTypeFormValidator;

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
  void renderSelectLicenceType() throws Exception {
    when(selectApplicationTypeService.getAvailableLicenceTypes()).thenReturn(AVAILABLE_LICENCE_TYPES);
    when(industryRoleService.userHasEditorOrSubmitterRole(organisationUser.wuaId())).thenReturn(true);

    mockMvc.perform(
            get(ReverseRouter.route(on(SelectApplicationLicenceTypeController.class).renderSelectLicenceType()))
                .with(user(organisationUser))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/application/selectLicenceType"))
        .andExpect(model().attribute("pageTitle", PAGE_TITLE))
        .andExpect(model().attribute("licenceTypeOptions", DisplayableEnumOptionUtil.getDisplayableOptions(AVAILABLE_LICENCE_TYPES)))
        .andExpect(model().attribute("cancelUrl", ReverseRouter.route(on(WorkAreaController.class).getWorkArea(null, null))));
  }

  @Test
  void submitSelectedLicenceType() throws Exception {
    when(selectApplicationTypeService.getAvailableLicenceTypes()).thenReturn(AVAILABLE_LICENCE_TYPES);
    when(selectApplicationLicenceTypeFormValidator.isValid(any(), any(), eq(AVAILABLE_LICENCE_TYPES))).thenReturn(true);
    when(industryRoleService.userHasEditorOrSubmitterRole(organisationUser.wuaId())).thenReturn(true);

    var licenceType = LicenceType.CARBON_STORAGE;

    var form = new SelectApplicationLicenceTypeForm();
    form.setSelectedLicenceType(licenceType);

    mockMvc.perform(
            post(ReverseRouter.route(on(SelectApplicationLicenceTypeController.class).submitSelectedLicenceType(form, null)))
                .with(user(organisationUser))
                .with(csrf())
                .flashAttr("form", form)
        )
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(SelectApplicationTypeController.class)
            .render(licenceType.getUrlSlug()))));
  }

  @Test
  void submitSelectedLicenceType_invalid() throws Exception {
    when(selectApplicationTypeService.getAvailableLicenceTypes()).thenReturn(AVAILABLE_LICENCE_TYPES);
    when(industryRoleService.userHasEditorOrSubmitterRole(organisationUser.wuaId())).thenReturn(true);
    when(selectApplicationLicenceTypeFormValidator.isValid(any(), any(), eq(AVAILABLE_LICENCE_TYPES))).thenReturn(false);

    mockMvc.perform(
            post(ReverseRouter.route(on(SelectApplicationLicenceTypeController.class).submitSelectedLicenceType(null, null)))
                .with(user(organisationUser))
                .with(csrf())
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/application/selectLicenceType"))
        .andExpect(model().attribute("pageTitle", PAGE_TITLE))
        .andExpect(model().attribute("licenceTypeOptions", DisplayableEnumOptionUtil.getDisplayableOptions(AVAILABLE_LICENCE_TYPES)))
        .andExpect(model().attribute("cancelUrl", ReverseRouter.route(on(WorkAreaController.class).getWorkArea(null, null))));
  }

  @Test
  void render_SelectLicenceTypeForbiddenUserNoAccess() throws Exception {
    when(industryRoleService.userHasEditorOrSubmitterRole(organisationUser.wuaId())).thenReturn(false);

    mockMvc.perform(
            get(ReverseRouter.route(on(SelectApplicationLicenceTypeController.class).renderSelectLicenceType()))
                .with(user(organisationUser))
        )
        .andExpect(status().isForbidden());
  }

  @Test
  void submit_SelectedLicenceTypeForbiddenUserNoAccess() throws Exception {
    when(industryRoleService.userHasEditorOrSubmitterRole(organisationUser.wuaId())).thenReturn(false);

    mockMvc.perform(
            post(ReverseRouter.route(on(SelectApplicationLicenceTypeController.class).submitSelectedLicenceType(null, null)))
                .with(user(organisationUser))
                .with(csrf())
        )
        .andExpect(status().isForbidden());
  }

}
