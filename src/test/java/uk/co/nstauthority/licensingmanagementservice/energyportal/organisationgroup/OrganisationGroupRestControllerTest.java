package uk.co.nstauthority.licensingmanagementservice.energyportal.organisationgroup;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import static uk.co.nstauthority.licensingmanagementservice.authentication.TestUserProvider.user;
import static uk.co.nstauthority.licensingmanagementservice.util.RedirectedToLoginUrlMatcher.redirectionToLoginUrl;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.fds.searchselector.SearchSelectorService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@ContextConfiguration(classes = OrganisationGroupRestController.class)
class OrganisationGroupRestControllerTest extends AbstractControllerTest {

  @MockitoBean
  private SearchSelectorService searchSelectorService;

  @Test
  void getOrganisationGroupSearchResults_thenOk() throws Exception {
    var user = ServiceUserDetailTestUtil.newBuilder().build();
    var groupList = List.of(
        OrganisationGroupTestUtil.createOrganisationGroupDto(1, "Royal Dutch Shell"),
        OrganisationGroupTestUtil.createOrganisationGroupDto(2, "Shell")
    );

    when(organisationGroupQueryService.getOrganisationGroupsByName("shell"))
        .thenReturn(groupList);

    mockMvc.perform(
        get(
            ReverseRouter.route(on(OrganisationGroupRestController.class).getOrganisationGroupSearchResults(null)))
            .param("term", "shell")
            .with(user(user)))
        .andExpect(status().isOk());
  }

  @Test
  void getOrganisationGroupSearchResults_thenUnauthorised() throws Exception {
    mockMvc.perform(
        get(
            ReverseRouter.route(on(OrganisationGroupRestController.class).getOrganisationGroupSearchResults(null)))
            .param("term", "shell"))
        .andExpect(redirectionToLoginUrl());
  }
}
