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
    var licenceManagementRoleDtos = Set.of(
        createServiceRoleDto(Role.MANAGE_TEAM, true),
        createServiceRoleDto(Role.CREATE_MANAGE_ANY_ORGANISATION_TEAM, false),
        createServiceRoleDto(Role.OFFLINE_LICENCE_ADMINISTRATOR, false),
        createServiceRoleDto(Role.SCHEDULE_ADMINISTRATOR, false),
        createServiceRoleDto(Role.WORK_PROGRAMME_ADMINISTRATOR, false),
        createServiceRoleDto(Role.WORK_PROGRAMME_STATUS_ADMINISTRATOR, false),
        createServiceRoleDto(Role.LICENCE_SCHEDULE_WORK_PROGRAMME_VIEWER, false),
        createServiceRoleDto(Role.DOCUMENT_TEMPLATE_MANAGER, false),
        createServiceRoleDto(Role.LICENCE_CONTACTS_MANAGER, false)
    );

    var productionRoleDtos = Set.of(
        createServiceRoleDto(Role.MANAGE_TEAM, true),
        createServiceRoleDto(Role.VIEW_ANY_LICENCE, false)
    );

    var carbonStorageRoleDtos = Set.of(
        createServiceRoleDto(Role.MANAGE_TEAM, true),
        createServiceRoleDto(Role.VIEW_ANY_LICENCE, false)
    );

    var organisationServiceRoleDtos = Set.of(
        createServiceRoleDto(Role.MANAGE_TEAM, true),
        createServiceRoleDto(Role.APPLICATION_EDITOR, false),
        createServiceRoleDto(Role.APPLICATION_SUBMITTER, false),
        createServiceRoleDto(Role.VIEW_ORGANISATION_LICENCES, false),
        createServiceRoleDto(Role.LICENSEE_CONTACTS_MANAGER, false)
    );

    var externalContributorsRoleDtos = Set.of(
        createServiceRoleDto(Role.EXTERNAL_APPLICATION_EDITOR, false),
        createServiceRoleDto(Role.EXTERNAL_APPLICATION_VIEWER, false)
    );

    var offshoreProductionLicensingRoleDtos = Set.of(
        createServiceRoleDto(Role.MANAGE_TEAM, true),
        createServiceRoleDto(Role.CASE_MANAGER_NEW_VENTURES, false),
        createServiceRoleDto(Role.CASE_MANAGER_OPERATIONS, false),
        createServiceRoleDto(Role.STEWARD_NEW_VENTURES, false),
        createServiceRoleDto(Role.STEWARD_OPERATIONS, false),
        createServiceRoleDto(Role.DECISION_ISSUER_NEW_VENTURES, false),
        createServiceRoleDto(Role.DECISION_ISSUER_OPERATIONS, false),
        createServiceRoleDto(Role.CONTINUATION_REVIEWER_NEW_VENTURES, false),
        createServiceRoleDto(Role.CONTINUATION_REVIEWER_OPERATIONS, false)
    );

    var carbonStorageLicensingRoleDtos = Set.of(
        createServiceRoleDto(Role.MANAGE_TEAM, true),
        createServiceRoleDto(Role.CASE_MANAGER_CS_NEW_VENTURES, false),
        createServiceRoleDto(Role.CASE_MANAGER_CS_CTS, false),
        createServiceRoleDto(Role.STEWARD_CS_NEW_VENTURES, false),
        createServiceRoleDto(Role.STEWARD_CS_CTS, false),
        createServiceRoleDto(Role.DECISION_ISSUER_CS_NEW_VENTURES, false),
        createServiceRoleDto(Role.DECISION_ISSUER_CS_CTS, false)
    );

    var onshoreProductionLicensingRoleDtos = Set.of(
        createServiceRoleDto(Role.MANAGE_TEAM, true),
        createServiceRoleDto(Role.CASE_MANAGER_ONSHORE, false),
        createServiceRoleDto(Role.STEWARD_ONSHORE, false),
        createServiceRoleDto(Role.DECISION_ISSUER_ONSHORE, false)
    );

    var regulationsLicensingRoleDtos = Set.of(
        createServiceRoleDto(Role.MANAGE_TEAM, true),
        createServiceRoleDto(Role.CONTINUATION_ISSUER, false),
        createServiceRoleDto(Role.DECISION_EXECUTOR, false)
    );

    assertThat(energyPortalDataService.getTeamTypeToServiceProviderTeamTypeRoleDtos())
        .isEqualTo(
            Map.of(
                TeamType.LICENCE_MANAGEMENT.name(), licenceManagementRoleDtos,
                TeamType.ORGANISATION.name(), organisationServiceRoleDtos,
                TeamType.EXTERNAL_CONTRIBUTORS.name(), externalContributorsRoleDtos,
                TeamType.OFFSHORE_PRODUCTION_LICENSING.name(), offshoreProductionLicensingRoleDtos,
                TeamType.CARBON_STORAGE_LICENSING.name(), carbonStorageLicensingRoleDtos,
                TeamType.ONSHORE_PRODUCTION_LICENSING.name(), onshoreProductionLicensingRoleDtos,
                TeamType.REGULATIONS_LICENSING.name(), regulationsLicensingRoleDtos
            )
        );
  }

  @Test
  void getTeamTypes() {
    assertThat(energyPortalDataService.getTeamTypes()).isEqualTo(Set.of(
        TeamType.LICENCE_MANAGEMENT.name(),
        TeamType.ORGANISATION.name(),
        TeamType.EXTERNAL_CONTRIBUTORS.name(),
        TeamType.OFFSHORE_PRODUCTION_LICENSING.name(),
        TeamType.CARBON_STORAGE_LICENSING.name(),
        TeamType.ONSHORE_PRODUCTION_LICENSING.name(),
        TeamType.REGULATIONS_LICENSING.name()
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
        .withRole(Role.VIEW_ANY_LICENCE)
        .build();

    var wuaId2Team2TeamRole1 = TeamRoleTestUtil.newBuilder()
        .withWuaId(2L)
        .withTeam(team2)
        .withRole(Role.MANAGE_TEAM)
        .build();

    var wuaId2Team2TeamRole2 = TeamRoleTestUtil.newBuilder()
        .withWuaId(2L)
        .withTeam(team2)
        .withRole(Role.VIEW_ANY_LICENCE)
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
                Set.of(Role.VIEW_ANY_LICENCE.name())
            ),
            new ServiceProviderUserTeamRolesDto(
                2L,
                team2.getId().toString(),
                team2.getTeamType().name(),
                Set.of(Role.MANAGE_TEAM.name(), Role.VIEW_ANY_LICENCE.name())
            )
        );
  }

  @Test
  void belongsToAnyTeam_returnsTrue_whenUserHasTeamRoles() {
    when(teamRoleRepository.existsByWuaId(300165L)).thenReturn(true);

    assertThat(energyPortalDataService.belongsToAnyTeam(300165L)).isTrue();
  }

  @Test
  void belongsToAnyTeam_returnsFalse_whenUserHasNoTeamRoles() {
    when(teamRoleRepository.existsByWuaId(300165L)).thenReturn(false);

    assertThat(energyPortalDataService.belongsToAnyTeam(300165L)).isFalse();
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