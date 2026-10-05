package uk.co.nstauthority.licensingmanagementservice.licence.correction.search;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import static uk.co.nstauthority.licensingmanagementservice.authentication.TestUserProvider.user;
import static uk.co.nstauthority.licensingmanagementservice.util.RedirectedToLoginUrlMatcher.redirectionToLoginUrl;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@ContextConfiguration(classes = LicenceCorrectionSearchController.class)
class LicenceCorrectionSearchControllerTest extends AbstractControllerTest {

  private static final String RENDER_CORRECTION_SEARCH_ROUTE =
      ReverseRouter.route(on(LicenceCorrectionSearchController.class).renderCorrectionSearch());

  @Test
  void renderCorrectionSearch_whenNotLoggedIn_thenRedirectToLoginPage() throws Exception {
    mockMvc.perform(get(RENDER_CORRECTION_SEARCH_ROUTE))
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void renderCorrectionSearch_whenLoggedIn_thenRendersSearchPage() throws Exception {
    mockMvc.perform(
            get(RENDER_CORRECTION_SEARCH_ROUTE)
                .with(user(regulatorUser))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/correction/search/correctionSearch"))
        .andExpect(model().attribute("searchItems", List.of()));
  }
}