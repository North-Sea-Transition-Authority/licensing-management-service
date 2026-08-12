package uk.co.nstauthority.licensingmanagementservice.licence.application;

import static org.mockito.ArgumentMatchers.any;
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.startjourney.StartContinuationApplicationController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.workarea.WorkAreaController;

@ContextConfiguration(classes = SelectApplicationTypeController.class)
class SelectApplicationTypeControllerTest extends AbstractControllerTest {

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
    when(applicationAccessService.userHasAccessToStartApplication(organisationUser.wuaId())).thenReturn(true);
    mockMvc.perform(
            get(ReverseRouter.route(on(SelectApplicationTypeController.class).render()))
                .with(user(organisationUser))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/application/selectApplicationType"))
        .andExpect(model().attribute("pageTitle", PAGE_TITLE))
        .andExpect(model().attribute("applicationTypeOptions", ApplicationType.getSelectionDisplayOptions()))
        .andExpect(model().attribute("cancelUrl", ReverseRouter.route(on(WorkAreaController.class).getWorkArea(null, null))));
  }

  @Test
  void submit() throws Exception {
    when(selectApplicationTypeFormValidator.isValid(any(), any())).thenReturn(true);
    when(applicationAccessService.userHasAccessToStartApplication(organisationUser.wuaId())).thenReturn(true);

    var applicationType = ApplicationType.CONTINUATION_APPLICATION;

    var form = new SelectApplicationTypeForm();
    form.setSelectedApplicationType(applicationType);

    when(selectApplicationTypeService.getJourneyStartRedirectRoute(form.getSelectedApplicationType()))
        .thenReturn(ReverseRouter.redirect(on(StartContinuationApplicationController.class).render()));

    mockMvc.perform(
            post(ReverseRouter.route(on(SelectApplicationTypeController.class).submit(form, null)))
                .with(user(organisationUser))
                .with(csrf())
                .flashAttr("form", form)
        )
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(StartContinuationApplicationController.class).render())));
  }

  @Test
  void submit_invalid() throws Exception {
    when(applicationAccessService.userHasAccessToStartApplication(organisationUser.wuaId())).thenReturn(true);
    when(selectApplicationTypeFormValidator.isValid(any(), any())).thenReturn(false);

    mockMvc.perform(
            post(ReverseRouter.route(on(SelectApplicationTypeController.class).submit(null, null)))
                .with(user(organisationUser))
                .with(csrf())
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/application/selectApplicationType"))
        .andExpect(model().attribute("pageTitle", PAGE_TITLE))
        .andExpect(model().attribute("applicationTypeOptions", ApplicationType.getSelectionDisplayOptions()))
        .andExpect(model().attribute("cancelUrl", ReverseRouter.route(on(WorkAreaController.class).getWorkArea(null, null))));
  }

  @Test
  void render_ForbiddenUserNoAccess() throws Exception {
    when(applicationAccessService.userHasAccessToStartApplication(organisationUser.wuaId())).thenReturn(false);
    mockMvc.perform(
            get(ReverseRouter.route(on(SelectApplicationTypeController.class).render()))
                .with(user(organisationUser))
        )
        .andExpect(status().isForbidden());

  }

  @Test
  void submit_ForbiddenUserNoAccess() throws Exception {
    when(applicationAccessService.userHasAccessToStartApplication(organisationUser.wuaId())).thenReturn(false);
    mockMvc.perform(
            post(ReverseRouter.route(on(SelectApplicationTypeController.class).submit(null, null)))
                .with(user(organisationUser))
                .with(csrf())
        )
        .andExpect(status().isForbidden());
  }
}