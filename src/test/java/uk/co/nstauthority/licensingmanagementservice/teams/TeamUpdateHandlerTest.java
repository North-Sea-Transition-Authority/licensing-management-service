package uk.co.nstauthority.licensingmanagementservice.teams;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.energyportal.starter.organisationgroup.EnergyPortalOrganisationGroupEvent;
import uk.co.nstauthority.licensingmanagementservice.teams.management.TeamManagementService;

@ExtendWith(MockitoExtension.class)
class TeamUpdateHandlerTest {

  private static final Team TEAM = TeamTestUtil
      .newBuilder()
      .withTeamType(TeamType.ORGANISATION)
      .withName("Test Organisation")
      .withScopeType("ORGGRP")
      .withScopeId("1")
      .build();

  @Mock
  private TeamManagementService teamManagementService;

  @InjectMocks
  private TeamUpdateHandler teamUpdateHandler;

  private EnergyPortalOrganisationGroupEvent event;

  @Test
  void accept_whenGroupCreated_thenDoNothing(){
    createEvent("Test Organisation", true);

    teamUpdateHandler.accept(event);

    verifyNoInteractions(teamManagementService);
  }

  @Test
  void accept_whenGroupUpdated_andNoTeam_thenDoNothing(){
    createEvent("Test Organisation", false);

    when(teamManagementService.getScopedTeam(
        eq(TeamType.ORGANISATION),
        refEq(TeamScopeReference.from(Long.toString(event.groupId()), "ORGGRP"))
    )).thenReturn(Optional.empty());

    teamUpdateHandler.accept(event);

    verify(teamManagementService, never()).updateTeamName(any(), any());
  }

  @Test
  void accept_whenGroupUpdated_andNameNotChanged_thenDoNothing(){
    createEvent("Test Organisation", false);

    when(teamManagementService.getScopedTeam(
        eq(TeamType.ORGANISATION),
        refEq(TeamScopeReference.from(Long.toString(event.groupId()), "ORGGRP"))
    )).thenReturn(Optional.of(TEAM));

    teamUpdateHandler.accept(event);

    verify(teamManagementService, never()).updateTeamName(any(), any());
  }

  @Test
  void accept_whenGroupUpdated_andNameChanged_thenUpdateTeamName(){
    createEvent("Updated Test Organisation", false);

    when(teamManagementService.getScopedTeam(
        eq(TeamType.ORGANISATION),
        refEq(TeamScopeReference.from(Long.toString(event.groupId()), "ORGGRP"))
    )).thenReturn(Optional.of(TEAM));

    teamUpdateHandler.accept(event);

    verify(teamManagementService).updateTeamName(TEAM, event.name());
  }

  private void createEvent(String name, boolean isCreated){
    event = new EnergyPortalOrganisationGroupEvent(
        1L,
        name,
        "TEST",
        isCreated
    );
  }

}