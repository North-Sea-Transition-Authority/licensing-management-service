package uk.co.nstauthority.licensingmanagementservice.authentication;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import uk.co.nstauthority.licensingmanagementservice.configuration.EnergyPortalConfiguration;
import uk.co.nstauthority.licensingmanagementservice.mvc.LmsAbsoluteUrlUtil;

@Component
public class ServiceLogoutSuccessHandler implements LogoutSuccessHandler {

  private static final String DEVELOPMENT_PROFILE = "development";

  private final EnergyPortalConfiguration energyPortalConfiguration;
  private final Environment environment;

  @Autowired
  public ServiceLogoutSuccessHandler(EnergyPortalConfiguration energyPortalConfiguration, Environment environment) {
    this.energyPortalConfiguration = energyPortalConfiguration;
    this.environment = environment;
  }

  @Override
  public void onLogoutSuccess(HttpServletRequest request,
                              HttpServletResponse response,
                              Authentication authentication) throws IOException {
    var logoutUrlBuilder = UriComponentsBuilder.fromUriString(energyPortalConfiguration.logoutUrl());

    // Only for local-dev, where EPAS is on shared-dev.
    if (environment.acceptsProfiles(Profiles.of(DEVELOPMENT_PROFILE))) {
      logoutUrlBuilder.queryParam("return_url", LmsAbsoluteUrlUtil.getWorkAreaUrl(request));
    }

    response.sendRedirect(logoutUrlBuilder.encode().toUriString());
  }
}
