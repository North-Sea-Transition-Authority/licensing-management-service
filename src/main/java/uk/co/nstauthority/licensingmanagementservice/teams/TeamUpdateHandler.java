package uk.co.nstauthority.licensingmanagementservice.teams;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uk.co.fivium.energyportal.starter.organisationgroup.EnergyPortalOrganisationGroupConsumer;
import uk.co.fivium.energyportal.starter.organisationgroup.EnergyPortalOrganisationGroupEvent;
import uk.co.nstauthority.licensingmanagementservice.teams.management.TeamManagementService;

@Component
class TeamUpdateHandler implements EnergyPortalOrganisationGroupConsumer {

  private static final Logger LOGGER = LoggerFactory.getLogger(TeamUpdateHandler.class);

  private final TeamManagementService teamManagementService;

  TeamUpdateHandler(TeamManagementService teamManagementService) {
    this.teamManagementService = teamManagementService;
  }

  @Override
  public void onEnergyPortalOrganisationGroupEvent(EnergyPortalOrganisationGroupEvent energyPortalOrganisationGroupEvent) {
    if (energyPortalOrganisationGroupEvent.isCreated()) {
      LOGGER.info("Received organisation group created event for organisation {}", energyPortalOrganisationGroupEvent.groupId());
      return;
    }

    var teamOptional = teamManagementService.getScopedTeam(
        TeamType.ORGANISATION,
        TeamScopeReference.from(Long.toString(energyPortalOrganisationGroupEvent.groupId()), "ORGGRP")
    );

    if (teamOptional.isEmpty() || teamOptional.get().getName().equals(energyPortalOrganisationGroupEvent.name())) {
      return;
    }

    teamManagementService.updateTeamName(teamOptional.get(), energyPortalOrganisationGroupEvent.name());
    LOGGER.info("Updated team name for organisation {}", energyPortalOrganisationGroupEvent.groupId());
  }
}
