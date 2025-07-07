package uk.co.nstauthority.licensingmanagementservice.licence.search;

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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.util.SecurityTest;

@ContextConfiguration(classes = LicenceSearchController.class)
class LicenceSearchControllerTest extends AbstractControllerTest {

  @MockitoBean
  private LicenceSearchService licenceSearchService;

  private ServiceUserDetail organisationUser;
  private static final Long ORGANISATION_USER_WUA_ID = 2L;

  @BeforeEach
  void setUp() {
    organisationUser = ServiceUserDetailTestUtil.newBuilder()
        .withWuaId(ORGANISATION_USER_WUA_ID)
        .build();
  }

  @SecurityTest
  void renderLicenceSearchPage() throws Exception {
    when(licenceSearchService.getSearchResultItems()).thenReturn(List.of());

    mockMvc.perform(
            get(ReverseRouter.route(on(LicenceSearchController.class).renderLicenceSearchPage()))
                .with(user(organisationUser))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/search/licenceSearch"))
        .andExpect(model().attribute("searchItems", List.of()));
  }

  @SecurityTest
  void renderLicenceOverview() throws Exception {
    var licence = new Licence();
    licence.setType(LicenceType.CARBON_STORAGE);
    licence.setLicenceNumber("1");
    licence.setPrefix("CS");

    when(licenceService.findLicenceByIdOrThrow(1)).thenReturn(licence);

    mockMvc.perform(
            get(ReverseRouter.route(on(LicenceSearchController.class).renderLicenceOverview(1, null)))
                .with(user(organisationUser))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/search/licenceOverview"))
        .andExpect(model().attribute("pageTitle", licence.getLicenceReference()))
        .andExpect(model().attribute("caption", licence.getType().getDisplayName()));
  }
}