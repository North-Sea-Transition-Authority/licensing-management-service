package uk.co.nstauthority.licensingmanagementservice.licence.schedule.startjourney;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import static uk.co.nstauthority.licensingmanagementservice.authentication.TestUserProvider.user;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.fds.searchselector.SearchSelectorService;
import uk.co.nstauthority.licensingmanagementservice.licence.internalapi.LicenceInternalApiRestController;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.util.SecurityTest;

@ContextConfiguration(classes = LicenceScheduleSelectionController.class)
class LicenceScheduleSelectionControllerTest extends AbstractControllerTest {

  @MockitoBean
  private SelectLicenceFormValidator selectLicenceFormValidator;

  @MockitoBean
  private LicenceScheduleService licenceScheduleService;

  private ServiceUserDetail organisationUser;
  private static final Long ORGANISATION_USER_WUA_ID = 2L;

  @BeforeEach
  void setUp() {
    organisationUser = ServiceUserDetailTestUtil.newBuilder()
        .withWuaId(ORGANISATION_USER_WUA_ID)
        .build();
  }

  @SecurityTest
  void renderSelectLicenceForSchedule() throws Exception {
    mockMvc.perform(
            get(ReverseRouter.route(on(LicenceScheduleSelectionController.class).renderSelectLicenceForSchedule()))
                .with(user(organisationUser))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/schedule/selectLicence"))
        .andExpect(model().attribute("pageTitle", "What licence do you want to create a schedule for?"))
        .andExpect(model().attribute("searchUrl",
            SearchSelectorService.route(on(LicenceInternalApiRestController.class).searchLicencesByReference(null))))
        .andExpect(model().attribute("backUrl",
            ReverseRouter.route(on(StartLicenceScheduleJourneyController.class).renderStartLicenceScheduleJourney())));
  }

  @SecurityTest
  void submitSelectLicenceForSchedule() throws Exception {
    var form = new SelectLicenceForm();
    form.setLicenceId("1");
    when(selectLicenceFormValidator.isValid(eq(form), any())).thenReturn(true);

    mockMvc.perform(
            post(ReverseRouter.route(on(LicenceScheduleSelectionController.class).submitSelectLicenceForSchedule(form, null)))
                .with(user(organisationUser))
                .with(csrf())
                .flashAttr("form", form)
        )
        .andExpect(status().is3xxRedirection());
  }

  @SecurityTest
  void submitSelectLicenceForSchedule_invalidForm() throws Exception {
    when(selectLicenceFormValidator.isValid(any(), any())).thenReturn(false);

    mockMvc.perform(
            post(ReverseRouter.route(on(LicenceScheduleSelectionController.class).submitSelectLicenceForSchedule(null, null)))
                .with(user(organisationUser))
                .with(csrf())
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/schedule/selectLicence"))
        .andExpect(model().attribute("pageTitle", "What licence do you want to create a schedule for?"))
        .andExpect(model().attribute("searchUrl",
            SearchSelectorService.route(on(LicenceInternalApiRestController.class).searchLicencesByReference(null))))
        .andExpect(model().attribute("backUrl",
            ReverseRouter.route(on(StartLicenceScheduleJourneyController.class).renderStartLicenceScheduleJourney())));
  }
}