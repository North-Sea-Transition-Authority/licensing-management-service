package uk.co.nstauthority.licensingmanagementservice.actuator;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

class ActuatorAuthenticationFilter extends OncePerRequestFilter {

  private static final String ROLE_ACTUATOR = "ROLE_ACTUATOR";
  private static final String BEARER = "Bearer ";
  private final String apiKey;

  ActuatorAuthenticationFilter(ActuatorConfigurationProperties actuatorConfigurationProperties) {
    this.apiKey = actuatorConfigurationProperties.apiKey();
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    var requestUri = request.getRequestURI();
    var contextPath = request.getContextPath();
    return requestUri.equals(contextPath + "/actuator/health");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain
  ) throws ServletException, IOException {
    var authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER)) {
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      response.setHeader(HttpHeaders.WWW_AUTHENTICATE, BEARER.trim());
      return;
    }

    var bearerToken = authorizationHeader.substring(BEARER.length());
    if (!bearerToken.equals(apiKey)) {
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      return;
    }

    var authorities = AuthorityUtils.createAuthorityList(ROLE_ACTUATOR);
    var authentication = new PreAuthenticatedAuthenticationToken(bearerToken, null, authorities);

    var securityContext = SecurityContextHolder.createEmptyContext();
    securityContext.setAuthentication(authentication);
    SecurityContextHolder.setContext(securityContext);

    filterChain.doFilter(request, response);
  }
}
