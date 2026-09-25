package uk.co.nstauthority.licensingmanagementservice.authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.util.UriComponentsBuilder;
import uk.co.nstauthority.licensingmanagementservice.configuration.EnergyPortalConfiguration;

@ExtendWith(MockitoExtension.class)
class ServiceLogoutSuccessHandlerTest {

  private static final String LOGOUT_URL = "https://itportal.dev/accounts/service-provider-sign-out";

  @Mock
  private Environment environment;

  private ServiceLogoutSuccessHandler serviceLogoutSuccessHandler;

  private EnergyPortalConfiguration energyPortalConfiguration;

  private MockHttpServletRequest request;

  private MockHttpServletResponse response;

  @Test
  void onLogoutSuccess_whenDevelopmentProfileActive_thenRedirectsWithReturnUrl() throws Exception {
    givenEnergyPortalConfiguration();
    given(environment.acceptsProfiles(Profiles.of("development"))).willReturn(true);
    serviceLogoutSuccessHandler = new ServiceLogoutSuccessHandler(energyPortalConfiguration, environment);

    request = new MockHttpServletRequest("GET", "/lms/logout");
    request.setScheme("http");
    request.setServerName("localhost");
    request.setServerPort(8081);
    request.setContextPath("/lms");
    response = new MockHttpServletResponse();

    serviceLogoutSuccessHandler.onLogoutSuccess(request, response, null);

    var expectedRedirectUrl = UriComponentsBuilder.fromUriString(LOGOUT_URL)
        .queryParam("return_url", "http://localhost:8081/lms/work-area")
        .encode()
        .toUriString();

    assertThat(response.getRedirectedUrl()).isEqualTo(expectedRedirectUrl);
  }

  @Test
  void onLogoutSuccess_whenDevelopmentProfileNotActive_thenRedirectsWithoutReturnUrl() throws Exception {
    givenEnergyPortalConfiguration();
    given(environment.acceptsProfiles(Profiles.of("development"))).willReturn(false);
    serviceLogoutSuccessHandler = new ServiceLogoutSuccessHandler(energyPortalConfiguration, environment);

    request = new MockHttpServletRequest("GET", "/logout");
    response = new MockHttpServletResponse();

    serviceLogoutSuccessHandler.onLogoutSuccess(request, response, null);

    assertThat(response.getRedirectedUrl()).isEqualTo(LOGOUT_URL);

    then(environment).should().acceptsProfiles(Profiles.of("development"));
  }

  private void givenEnergyPortalConfiguration() {
    energyPortalConfiguration = new EnergyPortalConfiguration(LOGOUT_URL, "preshared-key", "https://itportal.dev/accounts/register");
  }
}
