package uk.co.nstauthority.licensingmanagementservice.licence.schedule.startjourney;

import static org.mockito.ArgumentMatchers.any;
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
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.util.SecurityTest;

@ContextConfiguration(classes = StartLicenceScheduleJourneyController.class)
class StartLicenceScheduleJourneyControllerTest extends AbstractControllerTest {

  @MockitoBean
  private SelectCreateUpdateScheduleFormValidator selectCreateUpdateScheduleFormValidator;

  private ServiceUserDetail organisationUser;
  private static final Long ORGANISATION_USER_WUA_ID = 2L;

  @BeforeEach
  void setUp() {
    organisationUser = ServiceUserDetailTestUtil.newBuilder()
        .withWuaId(ORGANISATION_USER_WUA_ID)
        .build();
  }

  @SecurityTest
  void renderSelectCreateOrUpdateSchedule() throws Exception {
    mockMvc.perform(
            get(ReverseRouter.route(on(StartLicenceScheduleJourneyController.class).renderSelectCreateOrUpdateSchedule()))
                .with(user(organisationUser))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/schedule/selectCreateUpdateSchedule"))
        .andExpect(model().attribute("radioOptions", ScheduleJourneyOption.getScheduleJourneyRadioOptions()));
  }

  @SecurityTest
  void submitSelectCreateOrUpdateSchedule() throws Exception {
    when(selectCreateUpdateScheduleFormValidator.isValid(any(), any())).thenReturn(true);

    mockMvc.perform(
            post(ReverseRouter.route(on(StartLicenceScheduleJourneyController.class).submitSelectCreateOrUpdateSchedule(null, null)))
                .with(user(organisationUser))
                .with(csrf())
        )
        .andExpect(status().is3xxRedirection());
  }

  @SecurityTest
  void submitSelectCreateOrUpdateSchedule_invalid() throws Exception {
    when(selectCreateUpdateScheduleFormValidator.isValid(any(), any())).thenReturn(false);

    mockMvc.perform(
            post(ReverseRouter.route(on(StartLicenceScheduleJourneyController.class).submitSelectCreateOrUpdateSchedule(null, null)))
                .with(user(organisationUser))
                .with(csrf())
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/schedule/selectCreateUpdateSchedule"))
        .andExpect(model().attribute("radioOptions", ScheduleJourneyOption.getScheduleJourneyRadioOptions()));
  }

  @SecurityTest
  void renderStartLicenceScheduleJourney() throws Exception {
    mockMvc.perform(
            get(ReverseRouter.route(on(StartLicenceScheduleJourneyController.class).renderStartLicenceScheduleJourney()))
                .with(user(organisationUser))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/schedule/startScheduleJourney"))
        .andExpect(model().attribute("pageTitle", "Create a new licence schedule"))
        .andExpect(model().attribute("startUrl", ReverseRouter.route(on(LicenceScheduleSelectionController.class).renderSelectLicenceForSchedule())));
  }
}