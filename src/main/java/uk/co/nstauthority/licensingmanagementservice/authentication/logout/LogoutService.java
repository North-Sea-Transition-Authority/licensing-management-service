package uk.co.nstauthority.licensingmanagementservice.authentication.logout;

import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Service;
import uk.co.fivium.energyportal.starter.accounts.EnergyPortalLogoutService;

@Service
public class LogoutService implements EnergyPortalLogoutService {

  private final FindByIndexNameSessionRepository<? extends Session> sessionRepository;

  /**
   * Constructor for the LogoutService.
   * Note that IntelliJ does not realise that the bean for FindByIndexNameSessionRepository is provided by
   * the dependency "org.springframework.session:spring-session-jdbc". At runtime however JdbcIndexedSessionRepository
   * does exist.
   * @param sessionRepository the repository of the spring sessions
   */
  LogoutService(FindByIndexNameSessionRepository<? extends Session> sessionRepository) {
    this.sessionRepository = sessionRepository;
  }

  /**
   * Deletes the spring sessions for all the principals that have a name that matches the provided wuaId.
   * @param wuaId the web user account id of the user to be logged out of the application
   */
  @Override
  public void logoutUser(Long wuaId) {
    var sessions = sessionRepository.findByPrincipalName(wuaId.toString());
    sessions.keySet().forEach(sessionRepository::deleteById);
  }
}