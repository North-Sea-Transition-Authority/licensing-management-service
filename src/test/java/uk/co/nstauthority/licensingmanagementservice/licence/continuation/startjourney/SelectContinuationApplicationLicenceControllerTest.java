package uk.co.nstauthority.licensingmanagementservice.licence.continuation.startjourney;

import static java.lang.Integer.parseInt;
import static org.mockito.ArgumentMatchers.any;
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
import static uk.co.nstauthority.licensingmanagementservice.licence.continuation.startjourney.SelectContinuationApplicationLicenceController.PAGE_TITLE;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.fds.searchselector.SearchSelectorService;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationType;
import uk.co.nstauthority.licensingmanagementservice.licence.application.SelectApplicationTypeService;
import uk.co.nstauthority.licensingmanagementservice.licence.internalapi.LicenceInternalApiRestController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@ContextConfiguration(classes = SelectContinuationApplicationLicenceController.class)
class SelectContinuationApplicationLicenceControllerTest extends AbstractControllerTest {

  private static final LicenceType LICENCE_TYPE = LicenceType.LANDWARD_PRODUCTION;

  @MockitoBean
  private SelectContinuationApplicationLicenceFormValidator selectContinuationApplicationLicenceFormValidator;

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
    givenContinuationRelevantToLicenceType();

    mockMvc.perform(
            get(ReverseRouter.route(on(SelectContinuationApplicationLicenceController.class).render(LICENCE_TYPE.getUrlSlug())))
                .with(user(organisationUser))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/continuation/selectLicence"))
        .andExpect(model().attribute("pageTitle", PAGE_TITLE))
        .andExpect(model().attribute("pageCaption", LICENCE_TYPE.getDisplayName()))
        .andExpect(model().attribute("searchUrl",
            SearchSelectorService.route(on(LicenceInternalApiRestController.class)
                .searchActiveLicenceSchedulesByReferenceAndType(LICENCE_TYPE.getUrlSlug(), null, null))))
        .andExpect(model().attribute("backUrl",
            ReverseRouter.route(on(StartContinuationApplicationController.class).render(LICENCE_TYPE.getUrlSlug()))));
  }

  @Test
  void render_whenContinuationNotRelevantToLicenceType_thenNotFound() throws Exception {
    var licenceTypeSlug = LicenceType.CARBON_STORAGE.getUrlSlug();
    when(industryRoleService.userHasEditorOrSubmitterRole(organisationUser.wuaId())).thenReturn(true);
    when(selectApplicationTypeService.getLicenceTypeOrThrow(licenceTypeSlug, ApplicationType.CONTINUATION_APPLICATION))
        .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));

    mockMvc.perform(
            get(ReverseRouter.route(on(SelectContinuationApplicationLicenceController.class).render(licenceTypeSlug)))
                .with(user(organisationUser))
        )
        .andExpect(status().isNotFound());
  }

  @Test
  void submit() throws Exception {
    var licenceId = 1;

    var form = new SelectContinuationApplicationLicenceForm();
    form.setLicenceId(String.valueOf(licenceId));
    when(selectContinuationApplicationLicenceFormValidator.isValid(any())).thenReturn(true);
    when(industryRoleService.userHasEditorOrSubmitterRole(organisationUser.wuaId())).thenReturn(true);
    givenContinuationRelevantToLicenceType();

    var licence = LicenceTestUtil.builder()
        .withId(licenceId)
        .withLicenceType(LICENCE_TYPE)
        .build();
    when(licenceService.findLicenceByIdOrThrow(parseInt(form.getLicenceId()))).thenReturn(licence);

    mockMvc.perform(
            post(ReverseRouter.route(on(SelectContinuationApplicationLicenceController.class)
                .submit(LICENCE_TYPE.getUrlSlug(), form, null)))
                .with(user(organisationUser))
                .with(csrf())
                .flashAttr("form", form)
        )
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(LicenceContinuationLicenseeInformationController.class)
            .renderConfirmLicenseePermission(licenceId, null , null))));
  }

  @Test
  void submit_whenLicenceNotOfLicenceType_thenBadRequest() throws Exception {
    var licenceId = 1;

    var form = new SelectContinuationApplicationLicenceForm();
    form.setLicenceId(String.valueOf(licenceId));
    when(selectContinuationApplicationLicenceFormValidator.isValid(any())).thenReturn(true);
    when(industryRoleService.userHasEditorOrSubmitterRole(organisationUser.wuaId())).thenReturn(true);
    givenContinuationRelevantToLicenceType();

    var licence = LicenceTestUtil.builder()
        .withId(licenceId)
        .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
        .build();
    when(licenceService.findLicenceByIdOrThrow(licenceId)).thenReturn(licence);

    mockMvc.perform(
            post(ReverseRouter.route(on(SelectContinuationApplicationLicenceController.class)
                .submit(LICENCE_TYPE.getUrlSlug(), form, null)))
                .with(user(organisationUser))
                .with(csrf())
                .flashAttr("form", form)
        )
        .andExpect(status().isBadRequest());
  }

  @Test
  void submit_invalidForm() throws Exception {
    when(industryRoleService.userHasEditorOrSubmitterRole(organisationUser.wuaId())).thenReturn(true);
    givenContinuationRelevantToLicenceType();
    when(selectContinuationApplicationLicenceFormValidator.isValid(any())).thenReturn(false);

    mockMvc.perform(
            post(ReverseRouter.route(on(SelectContinuationApplicationLicenceController.class)
                .submit(LICENCE_TYPE.getUrlSlug(), null, null)))
                .with(user(organisationUser))
                .with(csrf())
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/continuation/selectLicence"))
        .andExpect(model().attribute("pageTitle", PAGE_TITLE))
        .andExpect(model().attribute("pageCaption", LICENCE_TYPE.getDisplayName()))
        .andExpect(model().attribute("searchUrl",
            SearchSelectorService.route(on(LicenceInternalApiRestController.class)
                .searchActiveLicenceSchedulesByReferenceAndType(LICENCE_TYPE.getUrlSlug(), null, null))))
        .andExpect(model().attribute("backUrl",
            ReverseRouter.route(on(StartContinuationApplicationController.class).render(LICENCE_TYPE.getUrlSlug()))));
  }

  @Test
  void render_ForbiddenUserNoAccess() throws Exception {
    when(industryRoleService.userHasEditorOrSubmitterRole(organisationUser.wuaId())).thenReturn(false);

    mockMvc.perform(
            get(ReverseRouter.route(on(SelectContinuationApplicationLicenceController.class).render(LICENCE_TYPE.getUrlSlug())))
                .with(user(organisationUser))
        )
        .andExpect(status().isForbidden());
  }

  @Test
  void submit_ForbiddenUserNoAccess() throws Exception {
    when(industryRoleService.userHasEditorOrSubmitterRole(organisationUser.wuaId())).thenReturn(false);

    mockMvc.perform(
            post(ReverseRouter.route(on(SelectContinuationApplicationLicenceController.class)
                .submit(LICENCE_TYPE.getUrlSlug(), null, null)))
                .with(user(organisationUser))
                .with(csrf())
                .flashAttr("form", new SelectContinuationApplicationLicenceForm())
        )
        .andExpect(status().isForbidden());

    verifyNoInteractions(licenceService);
  }

  private void givenContinuationRelevantToLicenceType() {
    when(selectApplicationTypeService.getLicenceTypeOrThrow(LICENCE_TYPE.getUrlSlug(), ApplicationType.CONTINUATION_APPLICATION))
        .thenReturn(LICENCE_TYPE);
  }
}
