package uk.co.nstauthority.licensingmanagementservice.teams;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toSet;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import org.springframework.stereotype.Service;
import uk.co.fivium.energyportal.serviceproviders.epmq.ScopeType;
import uk.co.fivium.energyportal.serviceproviders.epmq.messages.ServiceProviderTeamDto;
import uk.co.fivium.energyportal.serviceproviders.epmq.messages.ServiceProviderTeamTypeRoleDto;
import uk.co.fivium.energyportal.serviceproviders.epmq.messages.ServiceProviderUserTeamRolesDto;
import uk.co.fivium.energyportal.starter.serviceproviders.EnergyPortalServiceProviderDataService;

@Service
class EnergyPortalDataService implements EnergyPortalServiceProviderDataService {

  private final TeamRepository teamRepository;
  private final TeamRoleRepository teamRoleRepository;

  EnergyPortalDataService(
      TeamRepository teamRepository,
      TeamRoleRepository teamRoleRepository
  ) {
    this.teamRepository = teamRepository;
    this.teamRoleRepository = teamRoleRepository;
  }

  @Override
  public Collection<ServiceProviderTeamDto> getServiceProviderTeamDtos() {
    return teamRepository.findAll()
        .stream()
        .map(team -> new ServiceProviderTeamDto(
            team.getId().toString(),
            team.getScopeId(),
            ScopeType.ORGANISATION_GROUP,
            team.getTeamType().name()
        ))
        .collect(toSet());
  }

  @Override
  public Map<String, Collection<ServiceProviderTeamTypeRoleDto>> getTeamTypeToServiceProviderTeamTypeRoleDtos() {
    Map<String, Collection<ServiceProviderTeamTypeRoleDto>> teamTypeToRoles = new HashMap<>();

    for (var teamType : TeamType.values()) {
      var serviceRoleDtos = teamType.getAllowedRoles()
          .stream()
          .map(role -> new ServiceProviderTeamTypeRoleDto(
                  role.name(),
                  role.getName(),
                  role.getDescription(),
                  role == Role.MANAGE_TEAM,
                  role.ordinal()
              )
          ).collect(toSet());

      teamTypeToRoles.put(teamType.name(), serviceRoleDtos);
    }

    return teamTypeToRoles;
  }

  @Override
  public Collection<String> getTeamTypes() {
    return Arrays.stream(TeamType.values())
        .map(TeamType::name)
        .collect(toSet());
  }

  @Override
  public Collection<ServiceProviderUserTeamRolesDto> getServiceProviderUserTeamRolesDtos() {
    var serviceProviderUserTeamRolesDtos = new HashSet<ServiceProviderUserTeamRolesDto>();

    var wuaIdToTeamRoles = teamRoleRepository.findAll()
        .stream()
        .collect(groupingBy(TeamRole::getWuaId, toSet()));

    for (var wuaIdToTeamRoleEntry : wuaIdToTeamRoles.entrySet()) {
      var teamToTeamRoles = wuaIdToTeamRoleEntry.getValue()
          .stream()
          .collect(groupingBy(TeamRole::getTeam, toSet()));

      for (var teamToTeamRoleEntry : teamToTeamRoles.entrySet()) {
        var wuaId = wuaIdToTeamRoleEntry.getKey();
        var team = teamToTeamRoleEntry.getKey();
        var roles = teamToTeamRoleEntry.getValue()
            .stream()
            .map(teamRole -> teamRole.getRole().name())
            .collect(toSet());

        serviceProviderUserTeamRolesDtos.add(new ServiceProviderUserTeamRolesDto(
            wuaId,
            team.getId().toString(),
            team.getTeamType().name(),
            roles
        ));
      }
    }
    return serviceProviderUserTeamRolesDtos;
  }
}