package uk.co.nstauthority.licensingmanagementservice.energyportal.user;


import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisationgroup.OrganisationGroupDto;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisationgroup.OrganisationGroupQueryService;
import uk.co.nstauthority.licensingmanagementservice.teams.Team;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;

@Service
public class AllowedDomainService {

  private final OrganisationGroupQueryService organisationGroupQueryService;

  AllowedDomainService(OrganisationGroupQueryService organisationGroupQueryService) {
    this.organisationGroupQueryService = organisationGroupQueryService;
  }

  public boolean isAllowedDomain(String userEmail, Team team) {
    if (team.getTeamType() == TeamType.EXTERNAL_CONTRIBUTORS) {
      return true;
    }

    var group =  switch (team.getTeamType()) {
      case TeamType.ORGANISATION -> organisationGroupQueryService
          .getOrganisationGroupById(Integer.parseInt(team.getScopeId()));
      case TeamType.CARBON_STORAGE_LICENSING,
           TeamType.LICENCE_MANAGEMENT,
           TeamType.OFFSHORE_PRODUCTION_LICENSING,
           TeamType.ONSHORE_PRODUCTION_LICENSING,
           TeamType.REGULATIONS_LICENSING -> organisationGroupQueryService.getRegulatorOrganisationGroup();
      default -> throw new IllegalStateException("Unexpected value: " + team.getTeamType());
    };

    List<String> emailDomains = group.map(OrganisationGroupDto::getEmailDomains).orElseGet(ArrayList::new);

    return emailDomains.stream()
        .map(String::toLowerCase)
        .anyMatch(domain -> userEmail.toLowerCase().endsWith('@' + domain));
  }
}