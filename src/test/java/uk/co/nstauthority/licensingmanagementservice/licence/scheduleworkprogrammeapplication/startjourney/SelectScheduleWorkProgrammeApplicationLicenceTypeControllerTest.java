package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.startjourney;

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
import static uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.startjourney.SelectScheduleWorkProgrammeApplicationLicenceTypeController.PAGE_TITLE;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.application.SelectApplicationTypeController;
import uk.co.nstauthority.licensingmanagementservice.licence.rules.LicenceTypeRulesResolver;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.DisplayableEnumOptionUtil;

@ContextConfiguration(classes = SelectScheduleWorkProgrammeApplicationLicenceTypeController.class)
class SelectScheduleWorkProgrammeApplicationLicenceTypeControllerTest extends AbstractControllerTest {

  @MockitoBean
  private SelectScheduleWorkProgrammeApplicationLicenceTypeFormValidator selectScheduleWorkProgrammeApplicationLicenceTypeFormValidator;

  @MockitoBean
  private LicenceTypeRulesResolver licenceTypeRulesResolver;

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
    var licenceTypes = List.of(LicenceType.LANDWARD_PRODUCTION, LicenceType.CARBON_STORAGE);
    when(licenceTypeRulesResolver.getLicenceTypesThatCanCreateScheduleWorkProgrammeApplications()).thenReturn(licenceTypes);
    when(applicationAccessService.userHasAccessToStartApplication(organisationUser.wuaId())).thenReturn(true);

    mockMvc.perform(
            get(ReverseRouter.route(on(SelectScheduleWorkProgrammeApplicationLicenceTypeController.class).renderSelectLicenceType()))
                .with(user(organisationUser))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/scheduleWorkProgrammeApplication/selectLicenceType"))
        .andExpect(model().attribute("pageTitle", PAGE_TITLE))
        .andExpect(model().attribute("licenceTypeOptions", DisplayableEnumOptionUtil.getDisplayableOptions(licenceTypes)))
        .andExpect(model().attribute("backUrl", ReverseRouter.route(on(SelectApplicationTypeController.class).render())));
  }

  @Test
  void submitSelectedLicenceType() throws Exception {
    when(selectScheduleWorkProgrammeApplicationLicenceTypeFormValidator.isValid(any())).thenReturn(true);
    when(applicationAccessService.userHasAccessToStartApplication(organisationUser.wuaId())).thenReturn(true);

    var licenceType = LicenceType.SEAWARD_EXPLORATION;

    var form = new SelectScheduleWorkProgrammeApplicationLicenceTypeForm();
    form.setSelectedLicenceType(licenceType);

    mockMvc.perform(
            post(ReverseRouter.route(on(SelectScheduleWorkProgrammeApplicationLicenceTypeController.class).submitSelectedLicenceType(form, null)))
                .with(user(organisationUser))
                .with(csrf())
                .flashAttr("form", form)
        )
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(StartScheduleWorkProgrammeApplicationJourneyController.class)
                .renderStartScheduleWorkProgrammeApplicationJourney(licenceType.getUrlSlug()))));
  }

  @Test
  void submitSelectedLicenceType_invalid() throws Exception {
    var licenceTypes = List.of(LicenceType.LANDWARD_PRODUCTION, LicenceType.CARBON_STORAGE);
    when(licenceTypeRulesResolver.getLicenceTypesThatCanCreateScheduleWorkProgrammeApplications()).thenReturn(licenceTypes);
    when(applicationAccessService.userHasAccessToStartApplication(organisationUser.wuaId())).thenReturn(true);

    when(selectScheduleWorkProgrammeApplicationLicenceTypeFormValidator.isValid(any())).thenReturn(false);

    mockMvc.perform(
            post(ReverseRouter.route(on(SelectScheduleWorkProgrammeApplicationLicenceTypeController.class).submitSelectedLicenceType(null, null)))
                .with(user(organisationUser))
                .with(csrf())
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/scheduleWorkProgrammeApplication/selectLicenceType"))
        .andExpect(model().attribute("pageTitle", PAGE_TITLE))
        .andExpect(model().attribute("licenceTypeOptions", DisplayableEnumOptionUtil.getDisplayableOptions(licenceTypes)))
        .andExpect(model().attribute("backUrl", ReverseRouter.route(on(SelectApplicationTypeController.class).render())));
  }

  @Test
  void render_SelectLicenceTypeForbiddenUserNoAccess() throws Exception {
    when(applicationAccessService.userHasAccessToStartApplication(organisationUser.wuaId())).thenReturn(false);
    mockMvc.perform(
            get(ReverseRouter.route(on(SelectScheduleWorkProgrammeApplicationLicenceTypeController.class).renderSelectLicenceType()))
                .with(user(organisationUser))
        )
        .andExpect(status().isForbidden());
  }

  @Test
  void submit_SelectedLicenceTypeForbiddenUserNoAccess() throws Exception {
    when(applicationAccessService.userHasAccessToStartApplication(organisationUser.wuaId())).thenReturn(false);
    mockMvc.perform(
            post(ReverseRouter.route(on(SelectScheduleWorkProgrammeApplicationLicenceTypeController.class).submitSelectedLicenceType(null, null)))
                .with(user(organisationUser))
                .with(csrf())
        )
        .andExpect(status().isForbidden());
  }

}