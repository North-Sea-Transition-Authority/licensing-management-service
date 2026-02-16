package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.startjourney;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import static uk.co.nstauthority.licensingmanagementservice.authentication.TestUserProvider.user;
import static uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.startjourney.StartScheduleWorkProgrammeApplicationJourneyController.PAGE_TITLE;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.context.ContextConfiguration;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.util.SecurityTest;

@ContextConfiguration(classes = StartScheduleWorkProgrammeApplicationJourneyController.class)
class StartScheduleWorkProgrammeApplicationJourneyControllerTest extends AbstractControllerTest {

  private ServiceUserDetail organisationUser;
  private static final Long ORGANISATION_USER_WUA_ID = 2L;

  @BeforeEach
  void setUp() {
    organisationUser = ServiceUserDetailTestUtil.newBuilder()
        .withWuaId(ORGANISATION_USER_WUA_ID)
        .build();
  }

  @SecurityTest
  void renderStartScheduleWorkProgrammeApplicationJourney() throws Exception {
    var licenceType = LicenceType.SEAWARD_EXPLORATION;
    when(applicationAccessService.userHasAccessToStartApplication(organisationUser.wuaId())).thenReturn(true);
    mockMvc.perform(
            get(ReverseRouter.route(on(StartScheduleWorkProgrammeApplicationJourneyController.class).renderStartScheduleWorkProgrammeApplicationJourney(licenceType.getUrlSlug())))
                .with(user(organisationUser))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/scheduleWorkProgrammeApplication/startJourney"))
        .andExpect(model().attribute("pageTitle", PAGE_TITLE))
        .andExpect(model().attribute("pageCaption", licenceType.getDisplayName()))
        .andExpect(model().attribute("startUrl", ReverseRouter.route(on(SelectScheduleWorkProgrammeApplicationLicenceController.class).renderSelectLicenceForScheduleWorkProgrammeApplication(licenceType.getUrlSlug()))))
        .andExpect(model().attribute("backUrl", ReverseRouter.route(on(SelectScheduleWorkProgrammeApplicationLicenceTypeController.class).renderSelectLicenceType())));
  }

  @SecurityTest
  void render_StartScheduleWorkProgrammeApplicationJourneyForbiddenUserNoAccess() throws Exception {
    var licenceType = LicenceType.SEAWARD_EXPLORATION;
    when(applicationAccessService.userHasAccessToStartApplication(organisationUser.wuaId())).thenReturn(false);
    mockMvc.perform(
            get(ReverseRouter.route(on(StartScheduleWorkProgrammeApplicationJourneyController.class).renderStartScheduleWorkProgrammeApplicationJourney(licenceType.getUrlSlug())))
                .with(user(organisationUser))
        )
        .andExpect(status().isForbidden());
  }
}