package uk.co.nstauthority.licensingmanagementservice.authentication;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import uk.co.nstauthority.licensingmanagementservice.authentication.saml.SamlAuthenticationUtil;
import uk.co.nstauthority.licensingmanagementservice.authentication.saml.ServiceSaml2Authentication;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserDetailServiceTest {

  private static UserDetailService userDetailService;

  @BeforeAll
  static void setUp() {
    userDetailService = new UserDetailService();
  }

  @AfterAll
  static void tearDown() {
    SecurityContextHolder.setContext(new SecurityContextImpl(null));
  }

  @Test
  void getUserDetail_whenUserInContext_thenExpectedUserReturned() {
    var user = ServiceUserDetailTestUtil.newBuilder()
        .withWuaId(100L)
        .build();

    SamlAuthenticationUtil.newBuilder()
        .withUser(user)
        .setSecurityContext();

    assertThat(userDetailService.getUserDetail())
        .extracting(ServiceUserDetail::wuaId)
        .isEqualTo(user.wuaId());
  }

  @Test
  void getUserDetail_whenNoPrincipal_thenException() {
    SecurityContextHolder.setContext(new SecurityContextImpl(new ServiceSaml2Authentication(null, Set.of())));

    assertThatThrownBy(() -> userDetailService.getUserDetail())
        .isInstanceOf(InvalidAuthenticationException.class)
        .message()
        .isEqualTo("ServiceUserDetail not found in ServiceSaml2Authentication principal");
  }

  @Test
  void getUserDetail_whenNoAuthenticationInContext_thenException() {
    SecurityContextHolder.setContext(new SecurityContextImpl(null));

    assertThatThrownBy(() -> userDetailService.getUserDetail())
        .isInstanceOf(InvalidAuthenticationException.class)
        .message()
        .isEqualTo("ServiceSaml2Authentication not found in authentication context");
  }
}
