package uk.co.nstauthority.template.authentication.logout;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.MapSession;
import org.springframework.session.Session;

@ExtendWith(MockitoExtension.class)
class LogoutServiceTest {

  private static final Long WUA_ID = 1L;

  @Mock
  private FindByIndexNameSessionRepository<Session> sessionRepository;

  @InjectMocks
  private LogoutService logoutService;

  @Test
  void logoutUser() {
    var sessionId = UUID.randomUUID().toString();

    Map<String, Session> sessions = new HashMap<>();
    sessions.put(sessionId, new MapSession());
    when(sessionRepository.findByPrincipalName(WUA_ID.toString())).thenReturn(sessions);

    logoutService.logoutUser(WUA_ID);
    verify(sessionRepository).findByPrincipalName(WUA_ID.toString());
    verify(sessionRepository).deleteById(sessionId);
  }

  @Test
  void logoutUser_noSessions() {
    when(sessionRepository.findByPrincipalName(WUA_ID.toString())).thenReturn(Collections.emptyMap());
    logoutService.logoutUser(WUA_ID);
    verify(sessionRepository).findByPrincipalName(WUA_ID.toString());
    verify(sessionRepository, never()).deleteById(any());
  }
}
