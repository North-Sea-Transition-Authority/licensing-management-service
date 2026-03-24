package uk.co.nstauthority.licensingmanagementservice.energyportal.user;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.teams.Team;

@Service
@Profile("!use-service-access-request")
public class OracleAllowedDomainService implements AllowedDomainService {

  @Override
  public boolean isAllowedDomain(String email, Team team) {
    return true;
  }
}