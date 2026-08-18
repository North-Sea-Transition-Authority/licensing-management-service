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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.energyportal.serviceproviders.epmq.ScopeType;
import uk.co.nstauthority.licensingmanagementservice.phasedrelease.FeatureFlagService;
import uk.co.nstauthority.licensingmanagementservice.phasedrelease.FeatureFlagServiceTestUtil;

@ExtendWith(MockitoExtension.class)
class TeamQueryServiceTest {

  @Mock
  private TeamRepository teamRepository;

  @Mock
  private TeamRoleRepository teamRoleRepository;

  @Spy
  private FeatureFlagService featureFlagService = FeatureFlagServiceTestUtil.allPhasesEnabled();

  @InjectMocks
  private TeamQueryService teamQueryService;

  @Test
  void userHasStaticRole_hasRole() {
    setupStaticTeamAndRoles(1L, TeamType.LICENCE_MANAGEMENT, List.of(
        Role.OFFLINE_LICENCE_ADMINISTRATOR,
        Role.MANAGE_TEAM
    ));

    assertThat(teamQueryService.userHasStaticRole(1L, TeamType.LICENCE_MANAGEMENT, Role.OFFLINE_LICENCE_ADMINISTRATOR))
        .isTrue();
  }

  @Test
  void userHasStaticRole_doesNotHaveRole() {
    setupStaticTeamAndRoles(1L, TeamType.LICENCE_MANAGEMENT, List.of(
        Role.MANAGE_TEAM
    ));

    assertThat(teamQueryService.userHasStaticRole(1L, TeamType.LICENCE_MANAGEMENT, Role.OFFLINE_LICENCE_ADMINISTRATOR))
        .isFalse();
  }

  @Test
  void userHasStaticRole_invalidRole() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> teamQueryService.userHasStaticRole(1L, TeamType.LICENCE_MANAGEMENT, Role.APPLICATION_EDITOR));
  }

  @Test
  void userHasStaticRole_noTeamInstance() {
    when(teamRepository.findByTeamType(TeamType.LICENCE_MANAGEMENT)).thenReturn(List.of());
    assertThat(teamQueryService.userHasStaticRole(1L, TeamType.LICENCE_MANAGEMENT, Role.OFFLINE_LICENCE_ADMINISTRATOR))
        .isFalse();
  }

  @Test
  void userHasAtLeastOneStaticRole_hasRole() {
    setupStaticTeamAndRoles(1L, TeamType.LICENCE_MANAGEMENT, List.of(
        Role.OFFLINE_LICENCE_ADMINISTRATOR,
        Role.MANAGE_TEAM
    ));

    assertThat(teamQueryService.userHasAtLeastOneStaticRole(1L, TeamType.LICENCE_MANAGEMENT, Set.of(Role.OFFLINE_LICENCE_ADMINISTRATOR)))
        .isTrue();
  }

  @Test
  void userHasAtLeastOneStaticRole_doesNotHaveRole() {
    setupStaticTeamAndRoles(1L, TeamType.LICENCE_MANAGEMENT, List.of(
        Role.CREATE_MANAGE_ANY_ORGANISATION_TEAM,
        Role.MANAGE_TEAM
    ));

    assertThat(teamQueryService.userHasAtLeastOneStaticRole(1L, TeamType.LICENCE_MANAGEMENT, Set.of(Role.OFFLINE_LICENCE_ADMINISTRATOR)))
        .isFalse();
  }

  @Test
  void userHasAtLeastOneStaticRole_invalidRole() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> teamQueryService.userHasAtLeastOneStaticRole(1L, TeamType.LICENCE_MANAGEMENT, Set.of(Role.APPLICATION_EDITOR)));
  }

  @Test
  void userHasAtLeastOneStaticRole_noTeamInstance() {
    when(teamRepository.findByTeamType(TeamType.LICENCE_MANAGEMENT)).thenReturn(List.of());
    assertThat(teamQueryService.userHasAtLeastOneStaticRole(1L, TeamType.LICENCE_MANAGEMENT, Set.of(Role.LICENCE_SCHEDULE_WORK_PROGRAMME_VIEWER)))
        .isFalse();
  }

  @Test
  void userHasScopedRole_hasRole() {
    var scope = TeamScopeReference.from("123", ScopeType.ORGANISATION_GROUP.name());
    setupScopedTeamAndRoles(1L, TeamType.ORGANISATION, scope, List.of(
        Role.MANAGE_TEAM,
        Role.VIEW_ORGANISATION_LICENCES
    ));

    assertThat(teamQueryService.userHasScopedRole(1L, TeamType.ORGANISATION, scope, Role.VIEW_ORGANISATION_LICENCES))
        .isTrue();
  }

  @Test
  void userHasScopedRole_doesNotHaveRole() {
    var scope = TeamScopeReference.from("123", ScopeType.ORGANISATION_GROUP.name());
    setupScopedTeamAndRoles(1L, TeamType.ORGANISATION, scope, List.of(
        Role.MANAGE_TEAM,
        Role.VIEW_ORGANISATION_LICENCES
    ));

    assertThat(teamQueryService.userHasScopedRole(1L, TeamType.ORGANISATION, scope, Role.APPLICATION_EDITOR))
        .isFalse();
  }

  @Test
  void userHasScopedRole_invalidRole() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> teamQueryService.userHasScopedRole(1L, TeamType.ORGANISATION, TeamScopeReference.from("1", ScopeType.ORGANISATION_GROUP.name()), Role.CREATE_MANAGE_ANY_ORGANISATION_TEAM));
  }

  @Test
  void userHasScopedRole_noTeamInstance() {
    when(teamRepository.findByTeamTypeAndScopeTypeAndScopeId(TeamType.ORGANISATION, ScopeType.ORGANISATION_GROUP.name(), "1")).thenReturn(Optional.empty());
    assertThat(teamQueryService.userHasScopedRole(1L, TeamType.ORGANISATION, TeamScopeReference.from("1", ScopeType.ORGANISATION_GROUP.name()), Role.VIEW_ORGANISATION_LICENCES))
        .isFalse();
  }

  @Test
  void userHasAtLeastOneScopedRole_hasRole() {
    var scope = TeamScopeReference.from("123", ScopeType.ORGANISATION_GROUP.name());
    setupScopedTeamAndRoles(1L, TeamType.ORGANISATION, scope, List.of(
        Role.MANAGE_TEAM,
        Role.VIEW_ORGANISATION_LICENCES
    ));

    assertThat(teamQueryService.userHasAtLeastOneScopedRole(1L, TeamType.ORGANISATION, scope, Set.of(Role.APPLICATION_EDITOR, Role.VIEW_ORGANISATION_LICENCES)))
        .isTrue();
  }

  @Test
  void userHasAtLeastOneScopedRole_doesNotHaveRole() {
    var scope = TeamScopeReference.from("123", ScopeType.ORGANISATION_GROUP.name());
    setupScopedTeamAndRoles(1L, TeamType.ORGANISATION, scope, List.of(
        Role.MANAGE_TEAM,
        Role.VIEW_ORGANISATION_LICENCES
    ));

    assertThat(teamQueryService.userHasAtLeastOneScopedRole(1L, TeamType.ORGANISATION, scope, Set.of(Role.APPLICATION_EDITOR)))
        .isFalse();
  }

  @Test
  void userHasAtLeastOneScopedRole_invalidRole() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> teamQueryService.userHasScopedRole(1L, TeamType.ORGANISATION, TeamScopeReference.from("1", ScopeType.ORGANISATION_GROUP.name()), Role.VIEW_ANY_LICENCE));
  }

  @Test
  void userHasAtLeastOneScopedRole_noTeamInstance() {
    when(teamRepository.findByTeamTypeAndScopeTypeAndScopeId(TeamType.ORGANISATION, ScopeType.ORGANISATION_GROUP.name(), "1"))
        .thenReturn(Optional.empty());

    assertThat(teamQueryService.userHasAtLeastOneScopedRole(1L, TeamType.ORGANISATION, TeamScopeReference.from("1", ScopeType.ORGANISATION_GROUP.name()), Set.of(Role.VIEW_ORGANISATION_LICENCES)))
        .isFalse();
  }

  @Test
  void userIsInRegulatorTeam() {
    var team = new Team(UUID.randomUUID());
    team.setTeamType(TeamType.LICENCE_MANAGEMENT);

    var teamRole = new TeamRole();
    teamRole.setTeam(team);

    when(teamRoleRepository.findAllByWuaId(1L)).thenReturn(List.of(teamRole));

    assertThat(teamQueryService.userIsInRegulatorTeam(1L)).isTrue();
  }

  @Test
  void userIsInRegulatorTeam_notInRegulatorTeam() {
    var team = new Team(UUID.randomUUID());
    team.setTeamType(TeamType.ORGANISATION);

    var teamRole = new TeamRole();
    teamRole.setTeam(team);

    when(teamRoleRepository.findAllByWuaId(1L)).thenReturn(List.of(teamRole));

    assertThat(teamQueryService.userIsInRegulatorTeam(1L)).isFalse();
  }

  @ParameterizedTest
  @EnumSource(TeamType.class)
  void getAvailableRoles_whenAllPhasesEnabled_thenEveryAllowedRole(TeamType teamType) {
    assertThat(teamQueryService.getAvailableRoles(teamType))
        .isEqualTo(teamType.getAllowedRoles());
  }

  @Test
  void getAvailableRoles_whenInitialRelease_thenIndustryTeamOffersManageTeamAndLicenseeContactsOnly() {
    var initialRelease = new TeamQueryService(
        teamRepository, teamRoleRepository, FeatureFlagServiceTestUtil.initialReleaseOnly());

    assertThat(initialRelease.getAvailableRoles(TeamType.ORGANISATION))
        .containsExactly(Role.MANAGE_TEAM, Role.LICENSEE_CONTACTS_MANAGER);
  }

  @ParameterizedTest
  @EnumSource(value = TeamType.class, names = "ORGANISATION", mode = EnumSource.Mode.EXCLUDE)
  void getAvailableRoles_whenInitialRelease_thenRegulatorAndExternalContributorRolesAreUnaffected(TeamType teamType) {
    var initialRelease = new TeamQueryService(
        teamRepository, teamRoleRepository, FeatureFlagServiceTestUtil.initialReleaseOnly());

    assertThat(initialRelease.getAvailableRoles(teamType))
        .isEqualTo(teamType.getAllowedRoles());
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
