package uk.co.nstauthority.licensingmanagementservice.licence;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import static uk.co.nstauthority.licensingmanagementservice.authentication.TestUserProvider.user;
import static uk.co.nstauthority.licensingmanagementservice.util.RedirectedToLoginUrlMatcher.redirectionToLoginUrl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.licence.overview.LicenceScheduleTabController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@ContextConfiguration(classes = LicenceRedirectorController.class)
class LicenceRedirectorControllerTest extends AbstractControllerTest {

  private static final String LICENCE_REFERENCE = "P001";

  private uk.co.nstauthority.licensingmanagementservice.licence.Licence licence;

  @BeforeEach
  void setUp() {
    licence = LicenceTestUtil.builder()
        .withId(1)
        .withLicenceReference(LICENCE_REFERENCE)
        .build();
  }

  @Test
  void redirectToScheduleTimeline_whenNotLoggedIn_thenRedirectToLoginPage() throws Exception {
    mockMvc.perform(
            get(ReverseRouter.route(on(LicenceRedirectorController.class)
                .redirectToScheduleTimeline(LICENCE_REFERENCE)))
        )
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void redirectToScheduleTimeline_whenLicenceNotFound_thenNotFound() throws Exception {
    when(licenceService.findByLicenceReferenceOrThrow(LICENCE_REFERENCE)).thenCallRealMethod();
    mockMvc.perform(
            get(ReverseRouter.route(on(LicenceRedirectorController.class)
                .redirectToScheduleTimeline(LICENCE_REFERENCE)))
                .with(user(regulatorUser))
        )
        .andExpect(status().isNotFound());
  }

  @Test
  void redirectToScheduleTimeline_thenRedirectsToTimeline() throws Exception {
    when(licenceService.findByLicenceReferenceOrThrow(LICENCE_REFERENCE)).thenReturn(licence);

    mockMvc.perform(
            get(ReverseRouter.route(on(LicenceRedirectorController.class)
                .redirectToScheduleTimeline(LICENCE_REFERENCE)))
                .with(user(regulatorUser))
        )
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(LicenceScheduleTabController.class)
            .renderLicenceOverview(licence.getId(), null, null, null))));
  }

  @Test
  void redirectToWorkProgrammesTimeline_whenNotLoggedIn_thenRedirectToLoginPage() throws Exception {
    mockMvc.perform(
            get(ReverseRouter.route(on(LicenceRedirectorController.class)
                .redirectToWorkProgrammesTimeline(LICENCE_REFERENCE)))
        )
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void redirectToWorkProgrammesTimeline_whenLicenceNotFound_thenNotFound() throws Exception {
    when(licenceService.findByLicenceReferenceOrThrow(LICENCE_REFERENCE)).thenCallRealMethod();
    mockMvc.perform(
            get(ReverseRouter.route(on(LicenceRedirectorController.class)
                .redirectToWorkProgrammesTimeline(LICENCE_REFERENCE)))
                .with(user(regulatorUser))
        )
        .andExpect(status().isNotFound());
  }

  @Test
  void redirectToWorkProgrammesTimeline_thenRedirectsToWorkProgrammesTimeline() throws Exception {
    when(licenceService.findByLicenceReferenceOrThrow(LICENCE_REFERENCE)).thenReturn(licence);

    mockMvc.perform(
            get(ReverseRouter.route(on(LicenceRedirectorController.class)
                .redirectToWorkProgrammesTimeline(LICENCE_REFERENCE)))
                .with(user(regulatorUser))
        )
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(LicenceScheduleTabController.class)
            .renderWorkProgrammesOnlyTimeline(licence.getId(), null))));
  }
}
