package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.startjourney;

import static java.lang.Integer.parseInt;
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
import static uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.startjourney.SelectScheduleWorkProgrammeApplicationLicenceController.PAGE_TITLE;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.fds.searchselector.SearchSelectorService;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.internalapi.LicenceInternalApiRestController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.util.SecurityTest;

@ContextConfiguration(classes = SelectScheduleWorkProgrammeApplicationLicenceController.class)
class SelectScheduleWorkProgrammeApplicationLicenceControllerTest extends AbstractControllerTest {

  @MockitoBean
  private SelectScheduleWorkProgrammeApplicationLicenceFormValidator selectScheduleWorkProgrammeApplicationLicenceFormValidator;


  private ServiceUserDetail organisationUser;
  private static final Long ORGANISATION_USER_WUA_ID = 2L;

  @BeforeEach
  void setUp() {
    organisationUser = ServiceUserDetailTestUtil.newBuilder()
        .withWuaId(ORGANISATION_USER_WUA_ID)
        .build();
  }

  @SecurityTest
  void renderSelectLicenceForScheduleWorkProgrammeApplication() throws Exception {
    var licenceType = LicenceType.SEAWARD_EXPLORATION;
    when(applicationAccessService.userHasAccessToStartApplication(organisationUser.wuaId())).thenReturn(true);

    mockMvc.perform(
            get(ReverseRouter.route(on(SelectScheduleWorkProgrammeApplicationLicenceController.class).renderSelectLicenceForScheduleWorkProgrammeApplication(licenceType.getUrlSlug())))
                .with(user(organisationUser))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/scheduleWorkProgrammeApplication/selectLicence"))
        .andExpect(model().attribute("pageTitle", PAGE_TITLE))
        .andExpect(model().attribute("pageCaption", licenceType.getDisplayName()))
        .andExpect(model().attribute("searchUrl",
            SearchSelectorService.route(on(LicenceInternalApiRestController.class).searchActiveLicenceSchedulesByReferenceAndTypeForEaaApplication(licenceType.getUrlSlug(), null, null))))
        .andExpect(model().attribute("backUrl",
            ReverseRouter.route(on(StartScheduleWorkProgrammeApplicationJourneyController.class).renderStartScheduleWorkProgrammeApplicationJourney(licenceType.getUrlSlug()))));
  }

  @SecurityTest
  void submitSelectLicenceForScheduleWorkProgrammeApplication() throws Exception {
    var licenceType = LicenceType.SEAWARD_EXPLORATION;
    var licenceId = 1;

    var form = new SelectScheduleWorkProgrammeApplicationLicenceForm();
    form.setLicenceId(String.valueOf(licenceId));
    when(selectScheduleWorkProgrammeApplicationLicenceFormValidator.isValid(any())).thenReturn(true);
    when(applicationAccessService.userHasAccessToStartApplication(organisationUser.wuaId())).thenReturn(true);

    Licence licence = new Licence();
    licence.setId(licenceId);
    when(licenceService.findLicenceByIdOrThrow(parseInt(form.getLicenceId()))).thenReturn(licence);

    mockMvc.perform(
            post(ReverseRouter.route(on(SelectScheduleWorkProgrammeApplicationLicenceController.class).submitSelectLicenceForScheduleWorkProgrammeApplication(licenceType.getUrlSlug(), form, null)))
                .with(user(organisationUser))
                .with(csrf())
                .flashAttr("form", form)
        )
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(LicenseeInformationController.class)
            .renderConfirmLicenseePermission(licenceType.getUrlSlug(), licenceId, null, null))));
  }

  @SecurityTest
  void submit_SelectLicenceForScheduleWorkProgrammeApplication_invalidForm() throws Exception {
    var licenceType = LicenceType.SEAWARD_EXPLORATION;
    when(applicationAccessService.userHasAccessToStartApplication(organisationUser.wuaId())).thenReturn(true);
    when(selectScheduleWorkProgrammeApplicationLicenceFormValidator.isValid(any())).thenReturn(false);

    mockMvc.perform(
            post(ReverseRouter.route(on(SelectScheduleWorkProgrammeApplicationLicenceController.class).submitSelectLicenceForScheduleWorkProgrammeApplication(licenceType.getUrlSlug(), null, null)))
                .with(user(organisationUser))
                .with(csrf())
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/scheduleWorkProgrammeApplication/selectLicence"))
        .andExpect(model().attribute("pageTitle", PAGE_TITLE))
        .andExpect(model().attribute("pageCaption", licenceType.getDisplayName()))
        .andExpect(model().attribute("searchUrl",
            SearchSelectorService.route(on(LicenceInternalApiRestController.class).searchActiveLicenceSchedulesByReferenceAndTypeForEaaApplication(licenceType.getUrlSlug(), null, null))))
        .andExpect(model().attribute("backUrl",
            ReverseRouter.route(on(StartScheduleWorkProgrammeApplicationJourneyController.class).renderStartScheduleWorkProgrammeApplicationJourney(licenceType.getUrlSlug()))));
  }


  @SecurityTest
  void render_SelectLicenceForScheduleWorkProgrammeApplicationForbiddenUserNoAccess() throws Exception {
    when(applicationAccessService.userHasAccessToStartApplication(organisationUser.wuaId())).thenReturn(false);
    mockMvc.perform(
        get(ReverseRouter.route(on(SelectScheduleWorkProgrammeApplicationLicenceController.class).renderSelectLicenceForScheduleWorkProgrammeApplication(null)))
            .with(user(organisationUser))
            .with(csrf()));
  }

  @SecurityTest
  void submitSelectLicenceForScheduleWorkProgrammeApplicationForbiddenUserNoAccess() throws Exception {
    when(applicationAccessService.userHasAccessToStartApplication(organisationUser.wuaId())).thenReturn(false);
    mockMvc.perform(
            post(ReverseRouter.route(on(SelectScheduleWorkProgrammeApplicationLicenceController.class).submitSelectLicenceForScheduleWorkProgrammeApplication(null, null, null)))
                .with(user(organisationUser))
                .with(csrf()));
  }
}