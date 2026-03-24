package uk.co.nstauthority.licensingmanagementservice.energyportal.user;


import uk.co.nstauthority.licensingmanagementservice.teams.Team;

public interface AllowedDomainService {

  boolean isAllowedDomain(String domain, Team team);
}