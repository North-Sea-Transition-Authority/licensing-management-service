package uk.co.nstauthority.template.authentication.logout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.co.nstauthority.template.configuration.EnergyPortalConfiguration;

@RestController
@RequestMapping("api/v1/logout")
public class LogoutController {

  private static final Logger LOGGER = LoggerFactory.getLogger(LogoutController.class);
  private static final String AUTH_HEADER_BEARER_PREFIX = "Bearer ";
  private final EnergyPortalConfiguration energyPortalConfiguration;
  private final LogoutService logoutService;

  @Autowired
  LogoutController(EnergyPortalConfiguration energyPortalConfiguration,
                   LogoutService logoutService) {
    this.energyPortalConfiguration = energyPortalConfiguration;
    this.logoutService = logoutService;
  }

  /** Logs the specified user out from the service.
   * @param apiKey the pre-shared key used to authorize the request
   * @param wuaId the web user account id of the user to be logged out the service
   * @return a http response with the code 200 if successful
   */
  @PostMapping("{wuaId}")
  ResponseEntity<?> logoutOfService(@RequestHeader("Authorization") String apiKey,
                                    @PathVariable("wuaId") Long wuaId) {
    var expectedKey = energyPortalConfiguration.logoutPreSharedKey();
    if (apiKey.startsWith(AUTH_HEADER_BEARER_PREFIX) && expectedKey.equals(apiKey.replace(AUTH_HEADER_BEARER_PREFIX, ""))) {
      logoutService.logoutUser(wuaId);
      LOGGER.info("Logout request received from energy portal for wuaId: {}. User logged out.", wuaId);
      return ResponseEntity.ok().build();
    } else {
      var errorMessage = "Attempted to logout of service, invalid key used";
      LOGGER.warn(errorMessage);
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
  }
}
