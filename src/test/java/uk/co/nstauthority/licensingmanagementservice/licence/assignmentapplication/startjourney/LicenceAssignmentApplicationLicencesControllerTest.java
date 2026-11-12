package uk.co.nstauthority.licensingmanagementservice.licence.assignmentapplication.startjourney;

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

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.fds.searchselector.SearchSelectorService;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.application.SelectApplicationTypeService;
import uk.co.nstauthority.licensingmanagementservice.licence.internalapi.LicenceInternalApiRestController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@ContextConfiguration(classes = {
    LicenceAssignmentApplicationLicencesController.class,
    LicenceAssignmentApplicationLicencesFormValidator.class
})
class LicenceAssignmentApplicationLicencesControllerTest extends AbstractControllerTest {

  private static final LicenceType LICENCE_TYPE = LicenceType.SEAWARD_PRODUCTION;

  private final ServiceUserDetail organisationUser = ServiceUserDetailTestUtil.newBuilder().build();

  @MockitoBean
  private SelectApplicationTypeService selectApplicationTypeService;

  @MockitoBean
  private LicenceAssignmentApplicationStartJourneyService licenceAssignmentApplicationStartJourneyService;

  @BeforeEach
  void setUp() {
    when(industryRoleService.userHasEditorOrSubmitterRole(organisationUser.wuaId())).thenReturn(true);
  }

  @Test
  void render() throws Exception {
    mockMvc.perform(
            get(ReverseRouter.route(on(LicenceAssignmentApplicationLicencesController.class)
                .render(LICENCE_TYPE)))
                .with(user(organisationUser))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/assignmentapplication/selectLicences"))
        .andExpect(model().attribute("pageCaption", "Seaward production"))
        .andExpect(model().attribute("addedLicences", List.of()))
        .andExpect(model().attribute(
            "licenceSearchUrl",
            SearchSelectorService.route(on(LicenceInternalApiRestController.class)
                .searchLicencesByReferenceAndType(LICENCE_TYPE.getUrlSlug(), null))
        ))
        .andExpect(model().attribute(
            "backUrl",
            ReverseRouter.route(on(LicenceAssignmentApplicationDetailsController.class)
                .render(LICENCE_TYPE, null))
        ));
  }

  @Test
  void submit_whenLicenceOfTypeAdded_thenRedirectToLicences() throws Exception {
    var licence = LicenceTestUtil.builder()
        .withId(1)
        .withLicenceType(LICENCE_TYPE)
        .build();
    when(licenceAssignmentApplicationStartJourneyService.getLicencesOfType(List.of(1), LICENCE_TYPE))
        .thenReturn(List.of(licence));

    mockMvc.perform(
            post(ReverseRouter.route(on(LicenceAssignmentApplicationLicencesController.class)
                .submit(LICENCE_TYPE, null, null)))
                .param("licenceIds", "1")
                .with(user(organisationUser))
                .with(csrf())
        )
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(LicenceAssignmentApplicationLicencesController.class)
            .render(LICENCE_TYPE))));
  }

  @Test
  void submit_whenNoLicenceOfTypeAdded_thenRenderWithError() throws Exception {
    when(licenceAssignmentApplicationStartJourneyService.getLicencesOfType(List.of(1), LICENCE_TYPE))
        .thenReturn(List.of());

    mockMvc.perform(
            post(ReverseRouter.route(on(LicenceAssignmentApplicationLicencesController.class)
                .submit(LICENCE_TYPE, null, null)))
                .param("licenceIds", "1")
                .with(user(organisationUser))
                .with(csrf())
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/assignmentapplication/selectLicences"))
        .andExpect(model().attributeHasFieldErrorCode("form", "licenceSelector", "licenceSelector.required"))
        .andExpect(model().attribute("addedLicences", List.of()));
  }

  @Test
  void submit_whenLicenceIdOutOfIntegerRange_thenRenderWithError() throws Exception {
    mockMvc.perform(
            post(ReverseRouter.route(on(LicenceAssignmentApplicationLicencesController.class)
                .submit(LICENCE_TYPE, null, null)))
                .param("licenceIds", "2147483648")
                .with(user(organisationUser))
                .with(csrf())
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/assignmentapplication/selectLicences"))
        .andExpect(model().attributeHasFieldErrorCode("form", "licenceIds", "typeMismatch"));
  }
}
