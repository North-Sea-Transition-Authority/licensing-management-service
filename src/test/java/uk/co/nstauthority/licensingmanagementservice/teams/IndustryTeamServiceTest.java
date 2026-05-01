package uk.co.nstauthority.licensingmanagementservice.teams;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.teams.management.TeamManagementService;
import uk.co.nstauthority.licensingmanagementservice.teams.management.view.TeamMemberView;

@ExtendWith(MockitoExtension.class)
class IndustryTeamServiceTest {

  @Mock
  private TeamManagementService teamManagementService;

  @Mock
  private Team mockTeam;

  @Mock
  private TeamMemberView mockTeamMemberView;

  private IndustryTeamService industryTeamService;

  @BeforeEach
  void setUp() {
    industryTeamService = new IndustryTeamService(teamManagementService);
  }

  @Test
  void getSubmitterDetails_ReturnsEmptyList_WhenTeamIsNotFound() {
    Integer organisationGroupId = 123;

    when(teamManagementService.getScopedTeam(eq(TeamType.ORGANISATION), any(TeamScopeReference.class)))
        .thenReturn(Optional.empty());

    List<TeamMemberView> result = industryTeamService.getSubmitterDetails(organisationGroupId);

    assertTrue(result.isEmpty(), "Expected an empty list when the team is not found");
    verify(teamManagementService).getScopedTeam(eq(TeamType.ORGANISATION), any(TeamScopeReference.class));
  }

  @Test
  void getSubmitterDetails_ReturnsTeamMemberViews_WhenTeamIsFound() {
    Integer organisationGroupId = 123;
    var expectedList = List.of(mockTeamMemberView);

    when(teamManagementService.getScopedTeam(eq(TeamType.ORGANISATION), any(TeamScopeReference.class)))
        .thenReturn(Optional.of(mockTeam));

    when(teamManagementService.getActiveTeamMembersViewsForTeamAndRole(mockTeam, Role.APPLICATION_SUBMITTER))
        .thenReturn(expectedList);

    List<TeamMemberView> result = industryTeamService.getSubmitterDetails(organisationGroupId);

    assertEquals(expectedList, result, "Expected the submitter details list to match the mock response");

    verify(teamManagementService).getScopedTeam(eq(TeamType.ORGANISATION), any(TeamScopeReference.class));
    verify(teamManagementService).getActiveTeamMembersViewsForTeamAndRole(mockTeam, Role.APPLICATION_SUBMITTER);
  }
}