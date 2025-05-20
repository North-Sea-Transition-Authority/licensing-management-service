package uk.co.nstauthority.template.util;

import org.springframework.test.web.servlet.ResultMatcher;

import static org.assertj.core.api.Assertions.assertThat;

public class RedirectedToLoginUrlMatcher {

  private static final String SAML_LOGIN_REDIRECT_URL = "http://localhost/saml2/authenticate?registrationId=saml";

  private RedirectedToLoginUrlMatcher() {}

  public static ResultMatcher redirectionToLoginUrl() {
    return result -> assertThat(result.getResponse().getRedirectedUrl())
      .isEqualTo(SAML_LOGIN_REDIRECT_URL);
  }

}
