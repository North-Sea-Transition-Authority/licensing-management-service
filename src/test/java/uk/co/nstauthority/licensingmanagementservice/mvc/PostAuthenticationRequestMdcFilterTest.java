package uk.co.nstauthority.licensingmanagementservice.mvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.authentication.UserDetailService;

@ExtendWith(MockitoExtension.class)
class PostAuthenticationRequestMdcFilterTest {

  @Mock
  private UserDetailService userDetailService;

  @Mock
  private MockFilterChain filterChain;

  @InjectMocks
  private PostAuthenticationRequestMdcFilter postAuthenticationRequestMdcFilter;

  private MockHttpServletRequest request;
  private MockHttpServletResponse response;

  @BeforeEach
  void setUp() {
    request = new MockHttpServletRequest();
    response = new MockHttpServletResponse();
    MDC.remove(RequestLogFilter.MDC_WUA_ID);
    MDC.remove(RequestLogFilter.MDC_PROXY_WUA_ID);
  }

  @Test
  void doFilterInternal_noUser() throws ServletException, IOException {
    when(userDetailService.findUserDetail())
        .thenReturn(Optional.empty());

    postAuthenticationRequestMdcFilter.doFilterInternal(request, response, filterChain);

    assertThat(MDC.get(RequestLogFilter.MDC_WUA_ID)).isNull();
    assertThat(MDC.get(RequestLogFilter.MDC_PROXY_WUA_ID)).isNull();

    verify(filterChain).doFilter(request, response);
  }

  @Test
  void doFilterInternal_userExistsNoProxy() throws ServletException, IOException {
    var user = ServiceUserDetailTestUtil.newBuilder().buildWithoutProxy();
    when(userDetailService.findUserDetail())
        .thenReturn(Optional.of(user));

    postAuthenticationRequestMdcFilter.doFilterInternal(request, response, filterChain);

    assertThat(MDC.get(RequestLogFilter.MDC_WUA_ID)).isEqualTo(user.wuaId().toString());
    assertThat(MDC.get(RequestLogFilter.MDC_PROXY_WUA_ID)).isNull();

    verify(filterChain).doFilter(request, response);
  }

  @Test
  void doFilterInternal_userExistsWithProxy() throws ServletException, IOException {
    var user = ServiceUserDetailTestUtil.newBuilder().build();
    when(userDetailService.findUserDetail())
        .thenReturn(Optional.of(user));

    postAuthenticationRequestMdcFilter.doFilterInternal(request, response, filterChain);

    assertThat(MDC.get(RequestLogFilter.MDC_WUA_ID)).isEqualTo(user.wuaId().toString());
    assertThat(MDC.get(RequestLogFilter.MDC_PROXY_WUA_ID)).isEqualTo(user.proxyWuaId().toString());

    verify(filterChain).doFilter(request, response);
  }
}
