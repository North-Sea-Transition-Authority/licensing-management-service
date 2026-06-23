package uk.co.nstauthority.licensingmanagementservice.licence.application.externalcontributors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.exception.LmsEntityNotFoundException;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationType;
import uk.co.nstauthority.licensingmanagementservice.teams.Team;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamScopeReference;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;
import uk.co.nstauthority.licensingmanagementservice.teams.management.TeamManagementService;

@ExtendWith(MockitoExtension.class)
class ExternalContributorServiceTest {

  @Mock
  private TeamManagementService teamManagementService;

  @InjectMocks
  private ExternalContributorService externalContributorService;

  private static final TeamScopeReference SCOPE_REF =
      TeamScopeReference.from(UUID.randomUUID().toString(), ApplicationType.CONTINUATION_APPLICATION.name());

  @Test
  void getExternalContributorsTeam_whenFound_returnsTeam() {
    var team = new Team(UUID.randomUUID());
    when(teamManagementService.getScopedTeam(eq(TeamType.EXTERNAL_CONTRIBUTORS), any(TeamScopeReference.class)))
        .thenReturn(Optional.of(team));

    assertThat(externalContributorService.getExternalContributorsTeam(SCOPE_REF)).isEqualTo(team);
  }

  @Test
  void getExternalContributorsTeam_whenNotFound_throws() {
    when(teamManagementService.getScopedTeam(eq(TeamType.EXTERNAL_CONTRIBUTORS), any(TeamScopeReference.class)))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> externalContributorService.getExternalContributorsTeam(SCOPE_REF))
        .isInstanceOf(LmsEntityNotFoundException.class);
  }

  @Test
  void isSectionComplete_whenAnswerNull_returnsFalse() {
    assertThat(externalContributorService.isSectionComplete(null, SCOPE_REF)).isFalse();
  }

  @Test
  void isSectionComplete_whenAnswerNo_returnsTrue() {
    assertThat(externalContributorService.isSectionComplete(false, SCOPE_REF)).isTrue();
  }

  @Test
  void isSectionComplete_whenAnswerYesAndTeamHasMembers_returnsTrue() {
    var team = new Team(UUID.randomUUID());
    when(teamManagementService.getScopedTeam(eq(TeamType.EXTERNAL_CONTRIBUTORS), any(TeamScopeReference.class)))
        .thenReturn(Optional.of(team));
    when(teamManagementService.teamHasMembers(team)).thenReturn(true);

    assertThat(externalContributorService.isSectionComplete(true, SCOPE_REF)).isTrue();
  }

  @Test
  void isSectionComplete_whenAnswerYesAndTeamHasNoMembers_returnsFalse() {
    var team = new Team(UUID.randomUUID());
    when(teamManagementService.getScopedTeam(eq(TeamType.EXTERNAL_CONTRIBUTORS), any(TeamScopeReference.class)))
        .thenReturn(Optional.of(team));
    when(teamManagementService.teamHasMembers(team)).thenReturn(false);

    assertThat(externalContributorService.isSectionComplete(true, SCOPE_REF)).isFalse();
  }
}
