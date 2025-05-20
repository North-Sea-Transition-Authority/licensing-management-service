package uk.co.nstauthority.template.authentication;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import uk.co.nstauthority.template.authentication.saml.SamlAuthenticationUtil;
import uk.co.nstauthority.template.configuration.WebSecurityConfiguration;

import java.util.Collection;
import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

public class TestUserProvider {

  private TestUserProvider() {}

  public static RequestPostProcessor user(ServiceUserDetail serviceUserDetail) {
    return user(serviceUserDetail, List.of(new SimpleGrantedAuthority(WebSecurityConfiguration.IDP_ACCESS_GRANTED_AUTHORITY_NAME)));
  }

  public static RequestPostProcessor user(ServiceUserDetail serviceUserDetail,
                                          Collection<GrantedAuthority> authorities) {

    var authentication = SamlAuthenticationUtil.newBuilder()
        .withUser(serviceUserDetail)
        .withGrantedAuthorities(authorities)
        .build();

    return authentication(authentication);
  }
}
