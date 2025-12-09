package uk.co.nstauthority.licensingmanagementservice.licence.continuation.startjourney;

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
import static uk.co.nstauthority.licensingmanagementservice.licence.continuation.startjourney.SelectContinuationApplicationLicenceController.PAGE_TITLE;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.fds.searchselector.SearchSelectorService;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTypeGroup;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.tasklist.LicenceContinuationApplicationTaskListController;
import uk.co.nstauthority.licensingmanagementservice.licence.internalapi.LicenceInternalApiRestController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.util.SecurityTest;

@ContextConfiguration(classes = SelectContinuationApplicationLicenceController.class)
class SelectContinuationApplicationLicenceControllerTest extends AbstractControllerTest {

  @MockitoBean
  private SelectContinuationApplicationLicenceFormValidator selectContinuationApplicationLicenceFormValidator;

  private ServiceUserDetail organisationUser;
  private static final Long ORGANISATION_USER_WUA_ID = 2L;

  @BeforeEach
  void setUp() {
    organisationUser = ServiceUserDetailTestUtil.newBuilder()
        .withWuaId(ORGANISATION_USER_WUA_ID)
        .build();
  }

  @SecurityTest
  void render() throws Exception {
    var licenceTypeGroup = LicenceTypeGroup.PRODUCTION;

    mockMvc.perform(
            get(ReverseRouter.route(on(SelectContinuationApplicationLicenceController.class).render()))
                .with(user(organisationUser))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/continuation/selectLicence"))
        .andExpect(model().attribute("pageTitle", PAGE_TITLE))
        .andExpect(model().attribute("searchUrl",
            SearchSelectorService.route(on(LicenceInternalApiRestController.class).searchActiveLicenceSchedulesByReferenceAndType(licenceTypeGroup.getUrlSlugList(), null))))
        .andExpect(model().attribute("backUrl",
            ReverseRouter.route(on(StartContinuationApplicationController.class).render())));
  }

  @SecurityTest
  void submit() throws Exception {
    var licenceId = 1;

    var form = new SelectContinuationApplicationLicenceForm();
    form.setLicenceId(String.valueOf(licenceId));
    when(selectContinuationApplicationLicenceFormValidator.isValid(any())).thenReturn(true);

    var licence = new Licence();
    when(licenceService.findLicenceByIdOrThrow(parseInt(form.getLicenceId()))).thenReturn(licence);

    var licenceContinuationApplicationDetail = new LicenceContinuationApplicationDetail(UUID.randomUUID());
    when(licenceContinuationService.createNewLicenceContinuationApplication(licence)).thenReturn(licenceContinuationApplicationDetail);

    mockMvc.perform(
            post(ReverseRouter.route(on(SelectContinuationApplicationLicenceController.class).submit(form, null)))
                .with(user(organisationUser))
                .with(csrf())
                .flashAttr("form", form)
        )
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(LicenceContinuationApplicationTaskListController.class)
            .getTaskList(licenceContinuationApplicationDetail.getId(), null))));
  }

  @SecurityTest
  void submit_invalidForm() throws Exception {
    var licenceTypeGroup = LicenceTypeGroup.PRODUCTION;

    when(selectContinuationApplicationLicenceFormValidator.isValid(any())).thenReturn(false);

    mockMvc.perform(
            post(ReverseRouter.route(on(SelectContinuationApplicationLicenceController.class).submit(null, null)))
                .with(user(organisationUser))
                .with(csrf())
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/continuation/selectLicence"))
        .andExpect(model().attribute("pageTitle", PAGE_TITLE))
        .andExpect(model().attribute("searchUrl",
            SearchSelectorService.route(on(LicenceInternalApiRestController.class).searchActiveLicenceSchedulesByReferenceAndType(licenceTypeGroup.getUrlSlugList(), null))))
        .andExpect(model().attribute("backUrl",
            ReverseRouter.route(on(StartContinuationApplicationController.class).render())));
  }
}