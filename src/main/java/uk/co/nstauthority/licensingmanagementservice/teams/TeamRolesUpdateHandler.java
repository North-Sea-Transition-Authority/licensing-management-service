package uk.co.nstauthority.licensingmanagementservice.teams;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uk.co.fivium.energyportal.serviceproviders.epmq.messages.ServiceProviderTeamRolesEpasMessage;
import uk.co.fivium.energyportal.starter.configuration.EnergyPortalAccountsConfigurationProperties;
import uk.co.fivium.energyportal.starter.serviceproviders.EnergyPortalServiceProviderTeamRolesUpdateHandler;
import uk.co.nstauthority.licensingmanagementservice.audit.AuditRevisionUtil;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.EnergyPortalUserService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.WebUserAccountId;
import uk.co.nstauthority.licensingmanagementservice.teams.management.TeamManagementService;

@Component
public class TeamRolesUpdateHandler implements EnergyPortalServiceProviderTeamRolesUpdateHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(TeamRolesUpdateHandler.class);

  private final EnergyPortalUserService energyPortalUserService;
  private final TeamManagementService teamManagementService;
  private final String serviceName;

  TeamRolesUpdateHandler(EnergyPortalUserService energyPortalUserService,
                         TeamManagementService teamManagementService,
                         EnergyPortalAccountsConfigurationProperties energyPortalAccountsConfigurationProperties
  ) {
    this.energyPortalUserService = energyPortalUserService;
    this.teamManagementService = teamManagementService;
    this.serviceName = energyPortalAccountsConfigurationProperties.serviceName();
  }

  @Override
  public void accept(ServiceProviderTeamRolesEpasMessage serviceProviderTeamRolesEpasMessage) {
    if (!serviceName.equals(serviceProviderTeamRolesEpasMessage.getService())) {
      return;
    }

    var serviceProviderUserTeamRolesDto = serviceProviderTeamRolesEpasMessage.getServiceProviderUserTeamRolesDto();
    try {
      var team = teamManagementService.getTeam(UUID.fromString(serviceProviderUserTeamRolesDto.teamId()));

      var invokingUser = energyPortalUserService
          .getByWuaId(
              WebUserAccountId.from(serviceProviderTeamRolesEpasMessage.getDeciderWuaId()),
              "Audit who updated user roles"
          );

      AuditRevisionUtil.withFallbackAuditUser(
          ServiceUserDetail.from(invokingUser),
          () -> teamManagementService.setUserTeamRoles(
              serviceProviderUserTeamRolesDto.wuaId(),
              team,
              serviceProviderUserTeamRolesDto.roles().stream().map(Role::valueOf).toList(),
              ServiceUserDetail.from(invokingUser)
          )
      );
    } catch (Exception e) {
      LOGGER.error("Unable to update roles for user {} on team {}, correlationId: {} error: {}",
          serviceProviderUserTeamRolesDto.wuaId(),
          serviceProviderUserTeamRolesDto.teamId(),
          serviceProviderTeamRolesEpasMessage.getCorrelationId(),
          e.getMessage()
      );
    }
  }
}