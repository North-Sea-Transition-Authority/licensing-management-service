package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.startjourney;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import static uk.co.nstauthority.licensingmanagementservice.authentication.TestUserProvider.user;
import static uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.startjourney.LicenseeInformationController.PAGE_TITLE;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplication;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.requestpurpose.SwpApplicationRequestPurpose;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.requestpurpose.SwpApplicationRequestPurposeService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.teams.Team;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;
import uk.co.nstauthority.licensingmanagementservice.util.SecurityTest;

@ContextConfiguration(classes = LicenseeInformationController.class)
class LicenseeInformationControllerTest extends AbstractControllerTest {

  @MockitoBean
  private LicenseeInformationFormValidator licenseeInformationFormValidator;

  @MockitoBean
  private LicenseeInformationService licenseeInformationService;

  private ServiceUserDetail organisationUser;
  private static final Long ORGANISATION_USER_WUA_ID = 2L;
  private static final String CAPTION = "Licence type - Licence ref";
  private static final Integer LICENCE_ID = 1;
  private static final Licence LICENCE = LicenceTestUtil.builder().withId(LICENCE_ID).build();

  @MockitoBean
  private SwpApplicationRequestPurposeService swpApplicationRequestPurposeService;

  @BeforeEach
  void setUp() {
    organisationUser = ServiceUserDetailTestUtil
        .newBuilder()
        .withWuaId(ORGANISATION_USER_WUA_ID)
        .build();

    var swpApplicationRequestPurpose = new SwpApplicationRequestPurpose();
    when(swpApplicationRequestPurposeService.saveOrUpdateRequestPurpose(any(),any())).thenReturn(swpApplicationRequestPurpose);
    when(licenceService.findLicenceByIdOrThrow(LICENCE_ID)).thenReturn(LICENCE);
    when(licenceService.getLicencePageCaption(LICENCE)).thenReturn(CAPTION);
  }

  @SecurityTest
  void render() throws Exception {
    var licenceType = LicenceType.SEAWARD_EXPLORATION;

    when(applicationAccessService.userHasAccessToStartApplication(organisationUser.wuaId())).thenReturn(true);

    mockMvc.perform(
            get(ReverseRouter.route(on(LicenseeInformationController.class).renderConfirmLicenseePermission(licenceType.getUrlSlug(), LICENCE_ID, null, organisationUser)))
                .with(user(organisationUser))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/scheduleWorkProgrammeApplication/licenseeInformation"))
        .andExpect(model().attribute("pageTitle", PAGE_TITLE))
        .andExpect(model().attribute("pageCaption", CAPTION))
        .andExpect(model().attribute("backUrl",  ReverseRouter.route(on(SelectScheduleWorkProgrammeApplicationLicenceController.class)
            .renderSelectLicenceForScheduleWorkProgrammeApplication(licenceType.getUrlSlug()))));
  }

  @SecurityTest
  void submit() throws Exception {
    when(licenseeInformationFormValidator.isValid(any(), any())).thenReturn(true);
    when(applicationAccessService.userHasAccessToStartApplication(organisationUser.wuaId())).thenReturn(true);

    var licenceType = LicenceType.SEAWARD_EXPLORATION;

    var licence = mock(Licence.class);
    when(licenceService.findLicenceByIdOrThrow(1)).thenReturn(licence);

    var form = new LicenseeInformationForm();
    form.setAllLicenseesPermissionConfirmed(true);

    var team = new Team(UUID.randomUUID());
    when(teamManagementService.createScopedTeam(any(), any(), any())).thenReturn(team);

    ScheduleWorkProgrammeApplication scheduleWorkProgrammeApplication = new ScheduleWorkProgrammeApplication();
    scheduleWorkProgrammeApplication.setId(UUID.randomUUID());

    var scheduleWorkProgrammeApplicationDetail = ScheduleWorkProgrammeApplicationDetailTestUtil
        .builder()
        .withId(UUID.randomUUID())
        .withScheduleWorkProgrammeApplication(scheduleWorkProgrammeApplication)
        .build();

    when(scheduleWorkProgrammeApplicationService.createNewScheduleWorkProgrammeApplicationForLicence(licence, form))
        .thenReturn(scheduleWorkProgrammeApplicationDetail);

    mockMvc.perform(
            post(ReverseRouter.route(on(LicenseeInformationController.class).submitLicenseePermissionConfirmation(licenceType.getUrlSlug(), 1, null, form, null, organisationUser)))
                .with(user(organisationUser))
                .with(csrf())
                .flashAttr("form", form)
        )
        .andExpect(status().is3xxRedirection());

    verify(teamManagementService).createScopedTeam(eq(TeamType.EXTERNAL_CONTRIBUTORS.getDisplayName()), eq(TeamType.EXTERNAL_CONTRIBUTORS), any());
    verify(scheduleWorkProgrammeApplicationService).createNewScheduleWorkProgrammeApplicationForLicence(licence, form);
  }

  @SecurityTest
  void submitLicenseePermissionConfirmation_invalid() throws Exception {
    var licenceType = LicenceType.SEAWARD_EXPLORATION;

    when(licenseeInformationFormValidator.isValid(any(), any())).thenReturn(false);
    when(applicationAccessService.userHasAccessToStartApplication(organisationUser.wuaId())).thenReturn(true);

    mockMvc.perform(
            post(ReverseRouter.route(on(LicenseeInformationController.class).submitLicenseePermissionConfirmation(licenceType.getUrlSlug(), LICENCE_ID, null,null, null, organisationUser)))
                .with(user(organisationUser))
                .with(csrf())
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/scheduleWorkProgrammeApplication/licenseeInformation"))
        .andExpect(model().attribute("pageTitle", PAGE_TITLE))
        .andExpect(model().attribute("pageCaption", CAPTION))
        .andExpect(model().attribute("backUrl",  ReverseRouter.route(on(SelectScheduleWorkProgrammeApplicationLicenceController.class)
            .renderSelectLicenceForScheduleWorkProgrammeApplication(licenceType.getUrlSlug()))));

    verifyNoInteractions(scheduleWorkProgrammeApplicationService);
  }

  @SecurityTest
  void render_ForbiddenUserNoAccess() throws Exception {
    var licenceType = LicenceType.SEAWARD_EXPLORATION;

    when(applicationAccessService.userHasAccessToStartApplication(organisationUser.wuaId())).thenReturn(false);

    mockMvc.perform(
            get(ReverseRouter.route(on(LicenseeInformationController.class).renderConfirmLicenseePermission(licenceType.getUrlSlug(), LICENCE_ID, null, organisationUser)))
                .with(user(organisationUser))
        )
        .andExpect(status().isForbidden());
  }

  @SecurityTest
  void submit_ForbiddenUserNoAccess() throws Exception {
    when(applicationAccessService.userHasAccessToStartApplication(organisationUser.wuaId())).thenReturn(false);

    var licenceType = LicenceType.SEAWARD_EXPLORATION;

    mockMvc.perform(
            post(ReverseRouter.route(on(LicenseeInformationController.class).submitLicenseePermissionConfirmation(licenceType.getUrlSlug(), 1, null, null, null, organisationUser)))
                .with(user(organisationUser))
                .with(csrf()))
        .andExpect(status().isForbidden());
  }
}