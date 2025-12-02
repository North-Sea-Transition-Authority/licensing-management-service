package uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisationgroup.OrganisationGroupQueryService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitJson;
import uk.co.nstauthority.licensingmanagementservice.teams.Team;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamQueryService;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamRole;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;

@ExtendWith(MockitoExtension.class)
class LicenceOrganisationServiceTest {

  @Mock
  private TeamQueryService teamQueryService;

  @Mock
  private OrganisationGroupQueryService organisationGroupQueryService;

  @InjectMocks
  private LicenceOrganisationService licenceOrganisationService;

  @Mock
  private ServiceUserDetail serviceUserDetail;

  @Test
  void getUsersOrgUnits_WhenNoOrganisationGroupIds_returnsEmptyList() {
    when(serviceUserDetail.wuaId()).thenReturn(1L);
    when(teamQueryService.getTeamRolesForUser(1L)).thenReturn(Set.of());

    var result = licenceOrganisationService.getUsersOrgUnits(serviceUserDetail);

    assertThat(result).isEmpty();
  }

  @Test
  void getUsersOrgUnits_returnsOrganisationUnitsForOrganisationTeams() {
    when(serviceUserDetail.wuaId()).thenReturn(1L);

    Team team = mock(Team.class);
    TeamRole teamRole = mock(TeamRole.class);
    when(teamRole.getTeam()).thenReturn(team);
    when(team.getTeamType()).thenReturn(TeamType.ORGANISATION);
    when(team.getScopeId()).thenReturn("2");

    when(teamQueryService.getTeamRolesForUser(1L)).thenReturn(Set.of(teamRole));

    OrganisationUnitJson ou = new OrganisationUnitJson(2, "Org Two");
    when(organisationGroupQueryService.getOrganisationUnitsByOrganisationGroupIds(List.of(2))).thenReturn(List.of(ou));

    var result = licenceOrganisationService.getUsersOrgUnits(serviceUserDetail);

    assertThat(result).hasSize(1).contains(ou);
  }


}