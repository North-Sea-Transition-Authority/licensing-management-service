package uk.co.nstauthority.licensingmanagementservice.teams;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TeamQueryServiceTest {

  @Mock
  private TeamRepository teamRepository;

  @Mock
  private TeamRoleRepository teamRoleRepository;

  @InjectMocks
  private TeamQueryService teamQueryService;

  @Test
  void userHasStaticRole_hasRole() {
    setupStaticTeamAndRoles(1L, TeamType.REGULATOR, List.of(
        Role.CREATE_MANAGE_ANY_ORGANISATION_TEAM,
        Role.MANAGE_TEAM
    ));

    assertThat(teamQueryService.userHasStaticRole(1L, TeamType.REGULATOR, Role.CREATE_MANAGE_ANY_ORGANISATION_TEAM))
        .isTrue();
  }

  @Test
  void userHasStaticRole_doesNotHaveRole() {
    setupStaticTeamAndRoles(1L, TeamType.REGULATOR, List.of(
        Role.CREATE_MANAGE_ANY_ORGANISATION_TEAM,
        Role.MANAGE_TEAM
    ));

    assertThat(teamQueryService.userHasStaticRole(1L, TeamType.REGULATOR, Role.VIEW_ANY_APPLICATION))
        .isFalse();
  }

  @Test
  void userHasStaticRole_invalidRole() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> teamQueryService.userHasStaticRole(1L, TeamType.REGULATOR, Role.EDIT_APPLICATION));
  }

  @Test
  void userHasStaticRole_noTeamInstance() {
    when(teamRepository.findByTeamType(TeamType.REGULATOR)).thenReturn(List.of());
    assertThat(teamQueryService.userHasStaticRole(1L, TeamType.REGULATOR, Role.VIEW_ANY_APPLICATION))
        .isFalse();
  }

  @Test
  void userHasAtLeastOneStaticRole_hasRole() {
    setupStaticTeamAndRoles(1L, TeamType.REGULATOR, List.of(
        Role.CREATE_MANAGE_ANY_ORGANISATION_TEAM,
        Role.MANAGE_TEAM
    ));

    assertThat(teamQueryService.userHasAtLeastOneStaticRole(1L, TeamType.REGULATOR, Set.of(Role.CREATE_MANAGE_ANY_ORGANISATION_TEAM, Role.VIEW_ANY_APPLICATION)))
        .isTrue();
  }

  @Test
  void userHasAtLeastOneStaticRole_doesNotHaveRole() {
    setupStaticTeamAndRoles(1L, TeamType.REGULATOR, List.of(
        Role.CREATE_MANAGE_ANY_ORGANISATION_TEAM,
        Role.MANAGE_TEAM
    ));

    assertThat(teamQueryService.userHasAtLeastOneStaticRole(1L, TeamType.REGULATOR, Set.of(Role.VIEW_ANY_APPLICATION)))
        .isFalse();
  }

  @Test
  void userHasAtLeastOneStaticRole_invalidRole() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> teamQueryService.userHasAtLeastOneStaticRole(1L, TeamType.REGULATOR, Set.of(Role.EDIT_APPLICATION)));
  }

  @Test
  void userHasAtLeastOneStaticRole_noTeamInstance() {
    when(teamRepository.findByTeamType(TeamType.REGULATOR)).thenReturn(List.of());
    assertThat(teamQueryService.userHasAtLeastOneStaticRole(1L, TeamType.REGULATOR, Set.of(Role.CREATE_MANAGE_ANY_ORGANISATION_TEAM)))
        .isFalse();
  }

  @Test
  void userHasScopedRole_hasRole() {
    var scope = TeamScopeReference.from("123", "ORGGRP");
    setupScopedTeamAndRoles(1L, TeamType.ORGANISATION, scope, List.of(
        Role.MANAGE_TEAM,
        Role.VIEW_APPLICATION
    ));

    assertThat(teamQueryService.userHasScopedRole(1L, TeamType.ORGANISATION, scope, Role.VIEW_APPLICATION))
        .isTrue();
  }

  @Test
  void userHasScopedRole_doesNotHaveRole() {
    var scope = TeamScopeReference.from("123", "ORGGRP");
    setupScopedTeamAndRoles(1L, TeamType.ORGANISATION, scope, List.of(
        Role.MANAGE_TEAM,
        Role.VIEW_APPLICATION
    ));

    assertThat(teamQueryService.userHasScopedRole(1L, TeamType.ORGANISATION, scope, Role.EDIT_APPLICATION))
        .isFalse();
  }

  @Test
  void userHasScopedRole_invalidRole() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> teamQueryService.userHasScopedRole(1L, TeamType.ORGANISATION, TeamScopeReference.from("1", "ORGGRP"), Role.CREATE_MANAGE_ANY_ORGANISATION_TEAM));
  }

  @Test
  void userHasScopedRole_noTeamInstance() {
    when(teamRepository.findByTeamTypeAndScopeTypeAndScopeId(TeamType.ORGANISATION, "ORGGRP", "1")).thenReturn(Optional.empty());
    assertThat(teamQueryService.userHasScopedRole(1L, TeamType.ORGANISATION, TeamScopeReference.from("1", "ORGGRP"), Role.VIEW_APPLICATION))
        .isFalse();
  }

  @Test
  void userHasAtLeastOneScopedRole_hasRole() {
    var scope = TeamScopeReference.from("123", "ORGGRP");
    setupScopedTeamAndRoles(1L, TeamType.ORGANISATION, scope, List.of(
        Role.MANAGE_TEAM,
        Role.VIEW_APPLICATION
    ));

    assertThat(teamQueryService.userHasAtLeastOneScopedRole(1L, TeamType.ORGANISATION, scope, Set.of(Role.EDIT_APPLICATION, Role.VIEW_APPLICATION)))
        .isTrue();
  }

  @Test
  void userHasAtLeastOneScopedRole_doesNotHaveRole() {
    var scope = TeamScopeReference.from("123", "ORGGRP");
    setupScopedTeamAndRoles(1L, TeamType.ORGANISATION, scope, List.of(
        Role.MANAGE_TEAM,
        Role.VIEW_APPLICATION
    ));

    assertThat(teamQueryService.userHasAtLeastOneScopedRole(1L, TeamType.ORGANISATION, scope, Set.of(Role.EDIT_APPLICATION)))
        .isFalse();
  }

  @Test
  void userHasAtLeastOneScopedRole_invalidRole() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> teamQueryService.userHasScopedRole(1L, TeamType.ORGANISATION, TeamScopeReference.from("1", "ORGGRP"), Role.VIEW_ANY_APPLICATION));
  }

  @Test
  void userHasAtLeastOneScopedRole_noTeamInstance() {
    when(teamRepository.findByTeamTypeAndScopeTypeAndScopeId(TeamType.ORGANISATION, "ORGGRP", "1"))
        .thenReturn(Optional.empty());

    assertThat(teamQueryService.userHasAtLeastOneScopedRole(1L, TeamType.ORGANISATION, TeamScopeReference.from("1", "ORGGRP"), Set.of(Role.VIEW_APPLICATION)))
        .isFalse();
  }

  private void setupStaticTeamAndRoles(Long wuaId, TeamType teamType, List<Role> roles) {
    var team = new Team(UUID.randomUUID());
    team.setTeamType(teamType);
    var teamRoles = roles.stream()
        .map(role -> createTeamRole(wuaId, team, role))
        .toList();

    when(teamRepository.findByTeamType(teamType))
        .thenReturn(List.of(team));
    when(teamRoleRepository.findByWuaIdAndTeam(wuaId, team))
        .thenReturn(teamRoles);
  }

  private void setupScopedTeamAndRoles(Long wuaId, TeamType teamType, TeamScopeReference scopeRef, List<Role> roles) {
    var team = new Team(UUID.randomUUID());
    team.setScopeType(scopeRef.getType());
    team.setScopeId(scopeRef.getId());
    team.setTeamType(teamType);
    var teamRoles = roles.stream()
        .map(role -> createTeamRole(wuaId, team, role))
        .toList();

    when(teamRepository.findByTeamTypeAndScopeTypeAndScopeId(teamType, scopeRef.getType(), scopeRef.getId()))
        .thenReturn(Optional.of(team));
    when(teamRoleRepository.findByWuaIdAndTeam(wuaId, team))
        .thenReturn(teamRoles);
  }


  private TeamRole createTeamRole(Long wuaId, Team team, Role role) {
    var teamRole = new TeamRole(UUID.randomUUID());
    teamRole.setWuaId(wuaId);
    teamRole.setTeam(team);
    teamRole.setRole(role);
    return teamRole;
  }

}
