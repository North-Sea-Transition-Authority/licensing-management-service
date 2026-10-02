package uk.co.nstauthority.licensingmanagementservice.energyportal.organisations;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import static uk.co.nstauthority.licensingmanagementservice.authentication.TestUserProvider.user;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.fds.searchselector.RestSearchItem;
import uk.co.nstauthority.licensingmanagementservice.fds.searchselector.RestSearchResult;
import uk.co.nstauthority.licensingmanagementservice.fds.searchselector.SearchSelectorService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@ContextConfiguration(classes = OrganisationUnitRestController.class)
class OrganisationUnitRestControllerTest extends AbstractControllerTest {

  @MockitoBean
  private OrganisationUnitQueryService organisationUnitQueryService;

  @MockitoBean
  private SearchSelectorService searchSelectorService;

  @Test
  void searchOrganisationUnits() throws Exception {
    var user = ServiceUserDetailTestUtil.newBuilder().build();

    var searchTerm = "searchTerm";

    var response = List.of(new OrganisationUnitJson(1, ""));

    when(organisationUnitQueryService.searchOrganisationUnitsWithName(searchTerm))
        .thenReturn(response);

    mockMvc.perform(
            get(ReverseRouter.route(on(OrganisationUnitRestController.class).searchOrganisationUnits(null)))
                .with(user(user))
                .param("term", searchTerm)
        )
        .andExpect(status().isOk());
  }

  @Test
  void searchOrganisationUnitsOnlyIncludingPreviousLicensees() throws Exception {
    var user = ServiceUserDetailTestUtil.newBuilder().build();
    var searchTerm = "searchTerm";
    var previousOrganisationNames = List.of("org1", "org2");
    var previousNamesSet = Set.of("org1", "org2");

    var response = List.of(new OrganisationUnitJson(1, "name"));

    var restSearchResult = new RestSearchResult(List.of(new RestSearchItem("1", "name")));

    when(organisationUnitQueryService.searchOrganisationUnitsWithNameCompareToList(searchTerm, previousNamesSet,
        Set.of()))
        .thenReturn(response);
    when(searchSelectorService.search(searchTerm, response)).thenReturn(restSearchResult);

    mockMvc.perform(
            get(SearchSelectorService.routeWithConstraints(on(OrganisationUnitRestController.class)
                .searchOrganisationUnitsWithConstraints(null, previousOrganisationNames, null)))
                .with(user(user))
                .param("term", searchTerm)
        )
        .andExpectAll(
            status().isOk(),
            jsonPath("$.results[0].id").value("1"),
            jsonPath("$.results[0].text").value("name")
        );
  }
}