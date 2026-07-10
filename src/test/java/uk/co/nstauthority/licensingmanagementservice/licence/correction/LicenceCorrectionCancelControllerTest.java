package uk.co.nstauthority.licensingmanagementservice.licence.correction;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
import static uk.co.nstauthority.licensingmanagementservice.util.RedirectedToLoginUrlMatcher.redirectionToLoginUrl;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.licence.overview.LicenceOverviewController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@ContextConfiguration(classes = LicenceCorrectionCancelController.class)
@ActiveProfiles({"test", "enable-lms2"})
class LicenceCorrectionCancelControllerTest extends AbstractControllerTest {

  private final LicenceCorrection correction = LicenceCorrectionTestUtil.newBuilder().build();

  @Test
  void renderCancelCorrection_whenNotLoggedIn_redirectToLogin() throws Exception {
    mockMvc.perform(get(ReverseRouter.route(on(LicenceCorrectionCancelController.class)
            .renderCancelCorrection(UUID.randomUUID(), null))))
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void renderCancelCorrection_whenNotAllocatedToUser_forbidden() throws Exception {
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(correction.getId(), regulatorUser))
        .thenReturn(Optional.empty());

    mockMvc.perform(get(ReverseRouter.route(on(LicenceCorrectionCancelController.class)
            .renderCancelCorrection(correction.getId(), null)))
            .with(user(regulatorUser)))
        .andExpect(status().isForbidden());
  }

  @Test
  void renderCancelCorrection_whenAllocatedToUser_assertModelAndView() throws Exception {
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(correction.getId(), regulatorUser))
        .thenReturn(Optional.of(correction));

    mockMvc.perform(get(ReverseRouter.route(on(LicenceCorrectionCancelController.class)
            .renderCancelCorrection(correction.getId(), null)))
            .with(user(regulatorUser)))
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/correction/cancelCorrection"))
        .andExpect(model().attribute("correctionReference", correction.getCorrectionReference()))
        .andExpect(model().attribute("cancelUrl", ReverseRouter.route(on(LicenceCorrectionController.class)
            .renderCorrection(correction.getId(), null))));
  }

  @Test
  void processCancelCorrection_whenNotLoggedIn_redirectToLogin() throws Exception {
    mockMvc.perform(post(ReverseRouter.route(on(LicenceCorrectionCancelController.class)
            .processCancelCorrection(UUID.randomUUID(), null, null)))
        .with(csrf()))
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void processCancelCorrection_whenNotAllocatedToUser_forbidden() throws Exception {
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(correction.getId(), regulatorUser))
        .thenReturn(Optional.empty());

    mockMvc.perform(post(ReverseRouter.route(on(LicenceCorrectionCancelController.class)
            .processCancelCorrection(correction.getId(), null, null)))
            .with(user(regulatorUser))
            .with(csrf()))
        .andExpect(status().isForbidden());

    verify(licenceCorrectionService, never()).cancelCorrection(any());
  }

  @Test
  void processCancelCorrection_whenAllocatedToUser_assertRedirection() throws Exception {
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(correction.getId(), regulatorUser))
        .thenReturn(Optional.of(correction));

    mockMvc.perform(post(ReverseRouter.route(on(LicenceCorrectionCancelController.class)
            .processCancelCorrection(correction.getId(), null, null)))
            .with(user(regulatorUser))
            .with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(LicenceOverviewController.class)
            .renderLicenceOverview(correction.getLicence().getId(), null, null, null))));

    verify(licenceCorrectionService).cancelCorrection(correction);
  }
}