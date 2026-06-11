package uk.co.nstauthority.licensingmanagementservice.actuator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class ActuatorAuthenticationFilterTest {

  private static final String API_KEY = "valid-api-key";

  @Mock
  private MockFilterChain filterChain;

  private ActuatorAuthenticationFilter actuatorAuthenticationFilter;

  private MockHttpServletRequest request;
  private MockHttpServletResponse response;

  @BeforeEach
  void setUp() {
    actuatorAuthenticationFilter = new ActuatorAuthenticationFilter(new ActuatorConfigurationProperties(API_KEY));
    request = new MockHttpServletRequest();
    response = new MockHttpServletResponse();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void shouldNotFilter_whenHealthEndpoint_assertTrue() {
    request.setRequestURI("/actuator/health");

    assertThat(actuatorAuthenticationFilter.shouldNotFilter(request)).isTrue();
  }

  @Test
  void shouldNotFilter_whenOtherActuatorEndpoint_assertFalse() {
    request.setRequestURI("/actuator/gismigration");

    assertThat(actuatorAuthenticationFilter.shouldNotFilter(request)).isFalse();
  }

  @Test
  void doFilterInternal_whenNoAuthorizationHeader_assertUnauthorised() throws ServletException, IOException {
    actuatorAuthenticationFilter.doFilterInternal(request, response, filterChain);

    assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
    assertThat(response.getHeader(HttpHeaders.WWW_AUTHENTICATE)).isEqualTo("Bearer");
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    verifyNoInteractions(filterChain);
  }

  @Test
  void doFilterInternal_whenAuthorizationHeaderNotBearer_assertUnauthorised() throws ServletException, IOException {
    request.addHeader(HttpHeaders.AUTHORIZATION, "Basic %s".formatted(API_KEY));

    actuatorAuthenticationFilter.doFilterInternal(request, response, filterChain);

    assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
    assertThat(response.getHeader(HttpHeaders.WWW_AUTHENTICATE)).isEqualTo("Bearer");
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    verifyNoInteractions(filterChain);
  }

  @Test
  void doFilterInternal_whenIncorrectApiKey_assertUnauthorised() throws ServletException, IOException {
    request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer invalid-api-key");

    actuatorAuthenticationFilter.doFilterInternal(request, response, filterChain);

    assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    verifyNoInteractions(filterChain);
  }

  @Test
  void doFilterInternal_whenCorrectApiKey_assertAuthenticatedWithActuatorRole() throws ServletException, IOException {
    request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer %s".formatted(API_KEY));

    actuatorAuthenticationFilter.doFilterInternal(request, response, filterChain);

    assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);

    var authentication = SecurityContextHolder.getContext().getAuthentication();
    assertThat(authentication).isNotNull();
    assertThat(authentication.getAuthorities())
        .extracting(GrantedAuthority::getAuthority)
        .containsExactly("ROLE_ACTUATOR");

    verify(filterChain).doFilter(request, response);
  }
}
