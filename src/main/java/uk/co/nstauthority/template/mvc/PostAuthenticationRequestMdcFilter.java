package uk.co.nstauthority.template.mvc;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import uk.co.nstauthority.template.authentication.UserDetailService;

@Component
public class PostAuthenticationRequestMdcFilter extends OncePerRequestFilter {

  private final UserDetailService userDetailService;

  PostAuthenticationRequestMdcFilter(UserDetailService userDetailService) {
    this.userDetailService = userDetailService;
  }

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain
  ) throws ServletException, IOException {
    var userOptional = userDetailService.findUserDetail();
    if (userOptional.isPresent()) {
      var user = userOptional.get();
      MDC.put(RequestLogFilter.MDC_WUA_ID, user.wuaId().toString());
      if (user.proxyWuaId() != null) {
        MDC.put(RequestLogFilter.MDC_PROXY_WUA_ID, user.proxyWuaId().toString());
      }
    }

    filterChain.doFilter(request, response);
  }

}
