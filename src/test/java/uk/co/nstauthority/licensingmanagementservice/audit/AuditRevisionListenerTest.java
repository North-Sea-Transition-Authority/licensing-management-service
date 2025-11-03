package uk.co.nstauthority.licensingmanagementservice.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.authentication.saml.ServiceSaml2Authentication;

@ExtendWith(MockitoExtension.class)
class AuditRevisionListenerTest {

  @Mock
  private ServiceSaml2Authentication serviceSaml2Authentication;

  @Mock
  private SecurityContext securityContext;

  private AuditRevisionListener listener;

  @BeforeEach
  void setUp() {
    listener = new AuditRevisionListener();
    SecurityContextHolder.setContext(securityContext);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void newRevision_whenAuthenticatedUserPresent() {
    var serviceUserDetail = ServiceUserDetailTestUtil.newBuilder().build();
    var auditRevision = new AuditRevision();

    when(securityContext.getAuthentication()).thenReturn(serviceSaml2Authentication);
    when(serviceSaml2Authentication.getPrincipal()).thenReturn(serviceUserDetail);

    listener.newRevision(auditRevision);

    assertThat(auditRevision.getUserWuaId()).isEqualTo(serviceUserDetail.wuaId());
    assertThat(auditRevision.getProxyUserWuaId()).isEqualTo(serviceUserDetail.proxyWuaId());
  }

  @Test
  void newRevision_whenNoAuthenticatedUser_thenUseFallbackUser() {
    when(securityContext.getAuthentication()).thenReturn(null);

    var auditRevision = new AuditRevision();
    var serviceUserDetail = ServiceUserDetailTestUtil.newBuilder().build();

    try (MockedStatic<AuditRevisionUtil> mockedUtil = Mockito.mockStatic(AuditRevisionUtil.class)) {
      mockedUtil.when(AuditRevisionUtil::getFallbackAuditUser)
          .thenReturn(serviceUserDetail);

      listener.newRevision(auditRevision);
    }

    assertThat(auditRevision.getUserWuaId()).isEqualTo(serviceUserDetail.wuaId());
    assertThat(auditRevision.getProxyUserWuaId()).isEqualTo(serviceUserDetail.proxyWuaId());
  }
}