package uk.co.nstauthority.licensingmanagementservice.teams;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.energyportal.serviceproviders.epmq.ScopeType;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisationgroup.OrganisationGroupQueryService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitJson;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;

@ExtendWith(MockitoExtension.class)
class IndustryRoleServiceTest {

  private static final Long USER_1_WUA_ID = 1L;
  private static final int ORG_UNIT_ID = 100;
  private static final int ORG_GROUP_ID = 999;

  @Mock
  private TeamQueryService teamQueryService;

  @Mock
  private OrganisationUnitQueryService organisationUnitQueryService;

  @Mock
  private OrganisationGroupQueryService organisationGroupQueryService;

  @InjectMocks
  private IndustryRoleService industryRoleService;

  private ServiceUserDetail organisationUser;

  @BeforeEach
  void setUp() {
    organisationUser = ServiceUserDetailTestUtil
        .newBuilder()
        .withWuaId(USER_1_WUA_ID)
        .build();
  }

  @Test
  void userHasEditorOrSubmitterRoleInOrganisationGroup_returnsTrue() {
    var team = TeamTestUtil
        .newBuilder()
        .withTeamType(TeamType.ORGANISATION)
        .withScopeType(ScopeType.ORGANISATION_GROUP.name())
        .build();

    var teamRole = TeamRoleTestUtil
        .newBuilder()
        .withRole(Role.APPLICATION_SUBMITTER)
        .withTeam(team)
        .build();

    when(teamQueryService.getTeamRolesForUser(organisationUser.wuaId())).thenReturn(Set.of(teamRole));

    assertThat(industryRoleService.userHasEditorOrSubmitterRoleInOrganisationGroup(organisationUser)).isTrue();
  }

  @Test
  void userHasEditorOrSubmitterRoleInOrganisationGroup_returnsFalse() {
    var team = TeamTestUtil
        .newBuilder()
        .withTeamType(TeamType.LICENCE_MANAGEMENT)
        .build();

    var teamRole = TeamRoleTestUtil
        .newBuilder()
        .withRole(Role.APPLICATION_SUBMITTER)
        .withTeam(team)
        .build();

    when(teamQueryService.getTeamRolesForUser(organisationUser.wuaId())).thenReturn(Set.of(teamRole));

    assertThat(industryRoleService.userHasEditorOrSubmitterRoleInOrganisationGroup(organisationUser)).isFalse();
  }

  @Test
  void getEditorOrSubmitterOrganisationUnitIds_returnsCorrectUnitIds() {
    int unitId1 = 5001;
    int unitId2 = 5002;
    var unitJson1 = new OrganisationUnitJson(unitId1, "Unit 1");
    var unitJson2 = new OrganisationUnitJson(unitId2, "Unit 2");

    when(organisationGroupQueryService.getOrganisationUnitsByOrganisationGroupIds(any())).thenReturn(List.of(unitJson1, unitJson2));

    var result = industryRoleService.getEditorOrSubmitterOrganisationUnitIds(organisationUser);

    assertThat(result).containsExactlyInAnyOrder(unitId1, unitId2);
  }

  @Test
  void getEditorOrSubmitterOrganisationUnitIds_whenNoGroups_returnsEmptySet() {
    when(organisationGroupQueryService.getOrganisationUnitsByOrganisationGroupIds(List.of())).thenReturn(List.of());

    var result = industryRoleService.getEditorOrSubmitterOrganisationUnitIds(organisationUser);

    assertThat(result).isEmpty();
  }

  @Test
  void userIsSubmitterForOrganisationUnit_whenUserIsSubmitterInOrgGroup_returnsTrue() {
    when(organisationUnitQueryService.findOrganisationGroupIdByUnitId(ORG_UNIT_ID))
        .thenReturn(Optional.of(ORG_GROUP_ID));

    when(teamQueryService.userHasScopedRole(
        eq(USER_1_WUA_ID),
        eq(TeamType.ORGANISATION),
        any(TeamScopeReference.class),
        eq(Role.APPLICATION_SUBMITTER)
    )).thenReturn(true);

    assertThat(industryRoleService.userIsSubmitterForOrganisationUnit(ORG_UNIT_ID, USER_1_WUA_ID)).isTrue();
  }

  @Test
  void userIsSubmitterForOrganisationUnit_whenUserIsNotSubmitter_returnsFalse() {
    when(organisationUnitQueryService.findOrganisationGroupIdByUnitId(ORG_UNIT_ID))
        .thenReturn(Optional.of(ORG_GROUP_ID));

    when(teamQueryService.userHasScopedRole(
        eq(USER_1_WUA_ID),
        eq(TeamType.ORGANISATION),
        any(TeamScopeReference.class),
        eq(Role.APPLICATION_SUBMITTER)
    )).thenReturn(false);

    assertThat(industryRoleService.userIsSubmitterForOrganisationUnit(ORG_UNIT_ID, USER_1_WUA_ID)).isFalse();
  }

  @Test
  void userHasEditorOrSubmitterRole_delegatesToTeamQueryService() {
    when(teamQueryService.userHasRoleInTeamType(
        USER_1_WUA_ID,
        TeamType.ORGANISATION,
        Set.of(Role.APPLICATION_EDITOR, Role.APPLICATION_SUBMITTER)
    )).thenReturn(true);

    assertThat(industryRoleService.userHasEditorOrSubmitterRole(USER_1_WUA_ID)).isTrue();
  }
}
