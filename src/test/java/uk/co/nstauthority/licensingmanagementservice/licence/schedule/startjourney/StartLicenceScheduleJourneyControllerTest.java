package uk.co.nstauthority.licensingmanagementservice.licence.schedule.startjourney;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import static uk.co.nstauthority.licensingmanagementservice.authentication.TestUserProvider.user;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.context.ContextConfiguration;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.components.actions.ActionItemView;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencestartdate.LicenceStartDateController;
import uk.co.nstauthority.licensingmanagementservice.licence.search.LicenceSearchController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.util.SecurityTest;

@ContextConfiguration(classes = StartLicenceScheduleJourneyController.class)
class StartLicenceScheduleJourneyControllerTest extends AbstractControllerTest {

  private ServiceUserDetail organisationUser;
  private static final Long ORGANISATION_USER_WUA_ID = 2L;

  @BeforeEach
  void setUp() {
    organisationUser = ServiceUserDetailTestUtil.newBuilder()
        .withWuaId(ORGANISATION_USER_WUA_ID)
        .build();
  }

  @SecurityTest
  void renderStartLicenceScheduleJourney() throws Exception {
    var licenceId = 1;
    var licence = LicenceTestUtil.builder().build();
    var pageCaption = "pageCaption";

    when(licenceActionService.getAvailableUserActionItems(licence,organisationUser))
        .thenReturn(List.of(new ActionItemView(
            "Create licence schedule",
            1,
            false,
            ReverseRouter.route(on(StartLicenceScheduleJourneyController.class).renderStartLicenceScheduleJourney(licence.getId(), licence)), null)));

    when(licenceService.findLicenceByIdOrThrow(licenceId)).thenReturn(licence);
    when(licenceService.getLicencePageCaption(licence)).thenReturn(pageCaption);

    mockMvc.perform(
            get(ReverseRouter.route(on(StartLicenceScheduleJourneyController.class).renderStartLicenceScheduleJourney(licenceId, null)))
                .with(user(organisationUser))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/schedule/startScheduleJourney"))
        .andExpect(model().attribute("pageTitle", "Create a new licence schedule"))
        .andExpect(model().attribute("pageCaption", pageCaption))
        .andExpect(model().attribute("startUrl",
            ReverseRouter.route(on(LicenceStartDateController.class).renderLicenceStartDateForm(licenceId, null))))
        .andExpect(model().attribute("backUrl",
            ReverseRouter.route(on(LicenceSearchController.class).renderLicenceOverview(licenceId, null, null))));
  }

  @SecurityTest
  void renderStartLicenceScheduleJourney_noAuth() throws Exception {
    var licenceId = 1;
    var licence = LicenceTestUtil.builder().build();

    when(licenceService.findLicenceByIdOrThrow(licenceId)).thenReturn(licence);
    when(licenceActionService.getAvailableUserActionItems(licence,organisationUser))
        .thenReturn(List.of(new ActionItemView("test", 1, false, "test", null)));

    mockMvc.perform(
            get(ReverseRouter.route(on(StartLicenceScheduleJourneyController.class).renderStartLicenceScheduleJourney(licenceId, null)))
                .with(user(organisationUser))
        )
        .andExpect(status().isForbidden());
  }
}