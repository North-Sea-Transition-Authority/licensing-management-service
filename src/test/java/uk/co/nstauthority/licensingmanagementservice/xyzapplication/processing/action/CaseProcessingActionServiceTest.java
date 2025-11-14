package uk.co.nstauthority.licensingmanagementservice.xyzapplication.processing.action;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamQueryService;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamRole;
import uk.co.nstauthority.licensingmanagementservice.xyzapplication.XyzApplication;
import uk.co.nstauthority.licensingmanagementservice.xyzapplication.XyzApplicationStatus;

@ExtendWith(MockitoExtension.class)
class CaseProcessingActionServiceTest {

  @Mock
  private TeamQueryService teamQueryService;

  @InjectMocks
  private CaseProcessingActionService caseProcessingActionService;

  @Test
  void getAvailableUserActionItems_inFirstStatusAndUserHasEditApplication_returnProgressApplication() {
    var application = new XyzApplication();
    application.setStatus(XyzApplicationStatus.DRAFT);

    var user = ServiceUserDetailTestUtil.newBuilder().build();
    var teamRole = new TeamRole();
    teamRole.setRole(Role.APPLICATION_EDITOR);

    when(teamQueryService.getTeamRolesForUser(user.wuaId()))
        .thenReturn(Set.of(teamRole));

    assertThat(caseProcessingActionService.getAvailableUserActionItems(application, user))
        .containsExactly(CaseProcessingActionItem.PROGRESS_APPLICATION);
  }

  @Test
  void getAvailableUserActionItems_inSecondStatusAndUserHasEditApplication_returnVerifyApplication() {
    var application = new XyzApplication();
    application.setStatus(XyzApplicationStatus.SUBMITTED);

    var user = ServiceUserDetailTestUtil.newBuilder().build();
    var teamRole = new TeamRole();
    teamRole.setRole(Role.VIEW_ORGANISATION_LICENCES);

    when(teamQueryService.getTeamRolesForUser(user.wuaId()))
        .thenReturn(Set.of(teamRole));

    assertThat(caseProcessingActionService.getAvailableUserActionItems(application, user))
        .containsExactly(CaseProcessingActionItem.VERIFY_APPLICATION);
  }

  @Test
  void getAvailableUserActionItems_inFirstStatusAndUserHasViewApplication_returnEmptySet() {
    var application = new XyzApplication();
    application.setStatus(XyzApplicationStatus.DRAFT);

    var user = ServiceUserDetailTestUtil.newBuilder().build();
    var teamRole = new TeamRole();
    teamRole.setRole(Role.VIEW_ORGANISATION_LICENCES);

    when(teamQueryService.getTeamRolesForUser(user.wuaId()))
        .thenReturn(Set.of(teamRole));

    assertThat(caseProcessingActionService.getAvailableUserActionItems(application, user))
        .isEmpty();
  }

}
