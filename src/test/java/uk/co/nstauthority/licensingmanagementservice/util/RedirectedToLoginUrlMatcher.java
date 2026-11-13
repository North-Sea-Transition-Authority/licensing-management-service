package uk.co.nstauthority.licensingmanagementservice.util;

import org.springframework.test.web.servlet.ResultMatcher;

import static org.assertj.core.api.Assertions.assertThat;

public class RedirectedToLoginUrlMatcher {

  // Spring Security 7 redirects relatively rather than absolutising to http://localhost/...
  private static final String SAML_LOGIN_REDIRECT_URL = "/saml2/authenticate?registrationId=saml";

  private RedirectedToLoginUrlMatcher() {}

  public static ResultMatcher redirectionToLoginUrl() {
    return result -> assertThat(result.getResponse().getRedirectedUrl())
      .isEqualTo(SAML_LOGIN_REDIRECT_URL);
  }

}
