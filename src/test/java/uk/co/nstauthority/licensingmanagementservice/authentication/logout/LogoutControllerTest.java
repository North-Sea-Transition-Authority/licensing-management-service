package uk.co.nstauthority.licensingmanagementservice.authentication.logout;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.context.WebApplicationContext;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.configuration.EnergyPortalConfiguration;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@ContextConfiguration(classes = LogoutController.class)
class LogoutControllerTest extends AbstractControllerTest {

  private static final Long WUA_ID = 1L;

  @Autowired
  private EnergyPortalConfiguration energyPortalConfiguration;

  @Autowired
  protected WebApplicationContext context;

  @MockitoBean
  private LogoutService logoutService;

  @Test
  void logoutService() throws Exception {
    mockMvc
        .perform(post(ReverseRouter.route(on(LogoutController.class).logoutOfService(null, WUA_ID)))
            .header("Authorization", "Bearer " + energyPortalConfiguration.logoutPreSharedKey()))
        .andExpect(status().isOk());
    verify(logoutService).logoutUser(WUA_ID);
  }

  @Test
  void logoutService_unauthorized() throws Exception {
    mockMvc
        .perform(post(ReverseRouter.route(on(LogoutController.class).logoutOfService(null, WUA_ID)))
            .header("Authorization", "Bearer INVALID_KEY"))
        .andExpect(status().isUnauthorized());
    verify(logoutService, never()).logoutUser(any());
  }

  @Test
  void logoutService_invalidKey() throws Exception {
    mockMvc
        .perform(post(ReverseRouter.route(on(LogoutController.class).logoutOfService(null, WUA_ID)))
            .header("Authorization", "foo"))
        .andExpect(status().isUnauthorized());
    verify(logoutService, never()).logoutUser(any());
  }
}
