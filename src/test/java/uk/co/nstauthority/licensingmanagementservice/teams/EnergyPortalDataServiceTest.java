package uk.co.nstauthority.licensingmanagementservice.teams;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.energyportal.serviceproviders.epmq.ScopeType;
import uk.co.fivium.energyportal.serviceproviders.epmq.messages.ServiceProviderTeamDto;
import uk.co.fivium.energyportal.serviceproviders.epmq.messages.ServiceProviderTeamTypeRoleDto;
import uk.co.fivium.energyportal.serviceproviders.epmq.messages.ServiceProviderUserTeamRolesDto;

@ExtendWith(MockitoExtension.class)
class EnergyPortalDataServiceTest {

  @Mock
  private TeamRepository teamRepository;

  @Mock
  private TeamRoleRepository teamRoleRepository;

  @InjectMocks
  private EnergyPortalDataService energyPortalDataService;

  @Test
  void getServiceProviderTeamDtos() {
    var team1 = TeamTestUtil.newBuilder().build();
    var team2 = TeamTestUtil.newBuilder().build();

    var expectedDto1 = new ServiceProviderTeamDto(
        team1.getId().toString(),
        team1.getScopeId(),
        ScopeType.ORGANISATION_GROUP,
        team1.getTeamType().name()
    );
    var expectedDto2 = new ServiceProviderTeamDto(
        team2.getId().toString(),
        team2.getScopeId(),
        ScopeType.ORGANISATION_GROUP,
        team2.getTeamType().name()
    );

    when(teamRepository.findAll()).thenReturn(List.of(team1, team2));

    assertThat(energyPortalDataService.getServiceProviderTeamDtos())
        .containsExactlyInAnyOrder(expectedDto1, expectedDto2);
  }

  @Test
  void getTeamTypeToServiceProviderTeamTypeRoleDtos() {
    var regulatorServiceRoleDtos = Set.of(
        createServiceRoleDto(Role.MANAGE_TEAM, true),
        createServiceRoleDto(Role.CREATE_MANAGE_ANY_ORGANISATION_TEAM, false),
        createServiceRoleDto(Role.VIEW_ANY_APPLICATION, false)
    );

    var consulteeServiceRoleDtos = Set.of(
        createServiceRoleDto(Role.MANAGE_TEAM, true),
        createServiceRoleDto(Role.VIEW_ANY_APPLICATION, false)
    );

    var organisationServiceRoleDtos = Set.of(
        createServiceRoleDto(Role.MANAGE_TEAM, true),
        createServiceRoleDto(Role.EDIT_APPLICATION, false),
        createServiceRoleDto(Role.VIEW_APPLICATION, false)
    );

    assertThat(energyPortalDataService.getTeamTypeToServiceProviderTeamTypeRoleDtos())
        .isEqualTo(
            Map.of(
                TeamType.REGULATOR.name(), regulatorServiceRoleDtos,
                TeamType.CONSULTEE.name(), consulteeServiceRoleDtos,
                TeamType.ORGANISATION.name(), organisationServiceRoleDtos
            )
        );
  }

  @Test
  void getTeamTypes() {
    assertThat(energyPortalDataService.getTeamTypes()).isEqualTo(Set.of(
        TeamType.REGULATOR.name(),
        TeamType.CONSULTEE.name(),
        TeamType.ORGANISATION.name()
    ));
  }

  @Test
  void getServiceProviderUserTeamRolesDtos() {
    var team1 = TeamTestUtil.newBuilder().build();
    var team2 = TeamTestUtil.newBuilder().build();

    var wuaId1Team1TeamRole = TeamRoleTestUtil.newBuilder()
        .withWuaId(1L)
        .withTeam(team1)
        .withRole(Role.MANAGE_TEAM)
        .build();

    var wuaId1Team2TeamRole = TeamRoleTestUtil.newBuilder()
        .withWuaId(1L)
        .withTeam(team2)
        .withRole(Role.VIEW_ANY_APPLICATION)
        .build();

    var wuaId2Team2TeamRole1 = TeamRoleTestUtil.newBuilder()
        .withWuaId(2L)
        .withTeam(team2)
        .withRole(Role.MANAGE_TEAM)
        .build();

    var wuaId2Team2TeamRole2 = TeamRoleTestUtil.newBuilder()
        .withWuaId(2L)
        .withTeam(team2)
        .withRole(Role.VIEW_ANY_APPLICATION)
        .build();

    when(teamRoleRepository.findAll()).thenReturn(List.of(
        wuaId1Team1TeamRole,
        wuaId1Team2TeamRole,
        wuaId2Team2TeamRole1,
        wuaId2Team2TeamRole2
    ));

    assertThat(energyPortalDataService.getServiceProviderUserTeamRolesDtos())
        .containsExactlyInAnyOrder(
            new ServiceProviderUserTeamRolesDto(
                1L,
                team1.getId().toString(),
                team1.getTeamType().name(),
                Set.of(Role.MANAGE_TEAM.name())
            ),
            new ServiceProviderUserTeamRolesDto(
                1L,
                team2.getId().toString(),
                team2.getTeamType().name(),
                Set.of(Role.VIEW_ANY_APPLICATION.name())
            ),
            new ServiceProviderUserTeamRolesDto(
                2L,
                team2.getId().toString(),
                team2.getTeamType().name(),
                Set.of(Role.MANAGE_TEAM.name(), Role.VIEW_ANY_APPLICATION.name())
            )
        );
  }

  private ServiceProviderTeamTypeRoleDto createServiceRoleDto(Role role, boolean isAssessManager) {
    return new ServiceProviderTeamTypeRoleDto(
        role.name(),
        role.getName(),
        role.getDescription(),
        isAssessManager,
        role.ordinal()
    );
  }
}