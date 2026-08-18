package uk.co.nstauthority.licensingmanagementservice.migration.pears;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import static uk.co.nstauthority.licensingmanagementservice.authentication.TestUserProvider.user;
import static uk.co.nstauthority.licensingmanagementservice.util.RedirectedToLoginUrlMatcher.redirectionToLoginUrl;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceScheduledJobService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@ActiveProfiles("migration")
@ContextConfiguration(classes = PearsRefreshController.class)
class PearsRefreshControllerTest extends AbstractControllerTest {

  private static final String REFRESH_ROUTE =
      ReverseRouter.route(on(PearsRefreshController.class).refreshPearsLicences());

  @MockitoBean
  private LicenceScheduledJobService licenceScheduledJobService;

  @Test
  void refreshPearsLicences_whenGet_thenRefreshesLicences() throws Exception {
    mockMvc.perform(get(REFRESH_ROUTE).with(user(regulatorUser)))
        .andExpect(status().isOk())
        .andExpect(content().string("PEARS licences and responsible organisations refreshed"));

    verify(licenceScheduledJobService).retrieveAndSavePearsLicences();
  }

  @Test
  void refreshPearsLicences_whenPost_thenRefreshesLicences() throws Exception {
    mockMvc.perform(post(REFRESH_ROUTE).with(user(regulatorUser)).with(csrf()))
        .andExpect(status().isOk())
        .andExpect(content().string("PEARS licences and responsible organisations refreshed"));

    verify(licenceScheduledJobService).retrieveAndSavePearsLicences();
  }

  @Test
  void refreshPearsLicences_whenNotAuthenticated_thenRedirectsToLogin() throws Exception {
    mockMvc.perform(get(REFRESH_ROUTE))
        .andExpect(redirectionToLoginUrl());

    verifyNoInteractions(licenceScheduledJobService);
  }
}
