package uk.co.nstauthority.licensingmanagementservice.licence.internalapi;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import static uk.co.nstauthority.licensingmanagementservice.authentication.TestUserProvider.user;

import java.util.List;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.fds.searchselector.SearchSelectorService;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailStatus;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.util.SecurityTest;

@ContextConfiguration(classes = LicenceInternalApiRestController.class)
class LicenceInternalApiRestControllerTest extends AbstractControllerTest {

  @MockitoBean
  private LicenceInternalApiService licenceInternalApiService;

  @MockitoBean
  private SearchSelectorService searchSelectorService;

  @SecurityTest
  void searchActiveLicenceSchedulesByReferenceAndType() throws Exception {
    var user = ServiceUserDetailTestUtil.newBuilder().build();

    var licenceType = LicenceType.CARBON_STORAGE;
    var searchTerm = "searchTerm";

    var response = List.of(new LicenceJson(1, "CS001"));

    when(licenceInternalApiService.searchLicencesWithSchedulesByReferenceTypeAndStatus(searchTerm, List.of(licenceType), LicenceScheduleDetailStatus.ACTIVE))
        .thenReturn(response);

    mockMvc.perform(
            get(ReverseRouter.route(on(LicenceInternalApiRestController.class).searchActiveLicenceSchedulesByReferenceAndType(licenceType.getUrlSlug(), null)))
                .with(user(user))
                .param("term", searchTerm)
        )
        .andExpect(status().isOk());
  }
}