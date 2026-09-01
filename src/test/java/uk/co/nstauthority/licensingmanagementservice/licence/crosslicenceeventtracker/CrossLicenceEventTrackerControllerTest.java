package uk.co.nstauthority.licensingmanagementservice.licence.crosslicenceeventtracker;

import static org.hamcrest.Matchers.instanceOf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import static uk.co.nstauthority.licensingmanagementservice.authentication.TestUserProvider.user;

import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitRestController;
import uk.co.nstauthority.licensingmanagementservice.fds.searchselector.SearchSelectorService;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.DisplayableEnumOptionUtil;

@ContextConfiguration(classes = CrossLicenceEventTrackerController.class)
class CrossLicenceEventTrackerControllerTest extends AbstractControllerTest {

  private static final String RENDER_EVENT_TRACKER_ROUTE =
      ReverseRouter.route(on(CrossLicenceEventTrackerController.class).renderEventTracker());

  @Test
  void renderEventTracker_rendersExpectedViewAndModel() throws Exception {
    mockMvc.perform(
            get(RENDER_EVENT_TRACKER_ROUTE)
                .with(user(regulatorUser))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/crosslicenceeventtracker/eventTracker"))
        .andExpect(model().attributeExists("form"))
        .andExpect(model().attribute("form", instanceOf(EventTrackerForm.class)))
        .andExpect(model().attribute("licenceTypes",
            DisplayableEnumOptionUtil.getDisplayableOptions(LicenceType.getDisplayableTypes())))
        .andExpect(model().attribute("licenseeOrgUnitUrl",
            SearchSelectorService.route(on(OrganisationUnitRestController.class).searchOrganisationUnits(null))))
        .andExpect(model().attribute("preSelectedLicenseeOrgUnit", Collections.emptyMap()))
        .andExpect(model().attribute("requestTypes",
            DisplayableEnumOptionUtil.getDisplayableOptions(EventTrackerRequestType.class)))
        .andExpect(model().attribute("eventStatuses",
            DisplayableEnumOptionUtil.getDisplayableOptions(EventTrackerApplicationStatus.class)));
  }
}
