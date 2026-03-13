package uk.co.nstauthority.licensingmanagementservice.licence.continuation.overview.action;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationTestUtil;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.Team;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamQueryService;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamRole;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamRoleTestUtil;

@ExtendWith(MockitoExtension.class)
class LicenceContinuationActionServiceTest {

  private static final Long USER_WUA_ID = 2L;

  @Mock
  private TeamQueryService teamQueryService;

  @InjectMocks
  private LicenceContinuationActionService licenceContinuationActionService;

  private ServiceUserDetail serviceUserDetail;

  @BeforeEach
  void setUp() {
    serviceUserDetail = ServiceUserDetailTestUtil.newBuilder()
        .withWuaId(USER_WUA_ID)
        .build();
  }

  @Test
  void getAvailableUserActionItems_confirmContinuation_availableWhenSubmittedAndRoleAllowed() {
    var applicationDetail = LicenceContinuationApplicationTestUtil.builder()
        .withId(UUID.randomUUID())
        .withStatus(LicenceContinuationApplicationStatus.SUBMITTED)
        .build();

    TeamRole teamRole = TeamRoleTestUtil.newBuilder()
        .withRole(Role.CONTINUATION_REVIEWER_OPERATIONS)
        .withTeam(new Team())
        .withWuaId(USER_WUA_ID)
        .build();

    when(teamQueryService.getTeamRolesForUser(USER_WUA_ID)).thenReturn(Set.of(teamRole));

    assertThat(licenceContinuationActionService.getAvailableUserActionItems(applicationDetail, serviceUserDetail))
        .contains(LicenceContinuationActionItem.CONFIRM_CONTINUATION.toActionItemView(applicationDetail));
  }

  @Test
  void getAvailableUserActionItems_confirmContinuation_notAvailableWhenWrongStatus() {
    var applicationDetail = LicenceContinuationApplicationTestUtil.builder()
        .withId(UUID.randomUUID())
        .withStatus(LicenceContinuationApplicationStatus.DRAFT)
        .build();

    TeamRole teamRole = TeamRoleTestUtil.newBuilder()
        .withRole(Role.CONTINUATION_REVIEWER_OPERATIONS)
        .withTeam(new Team())
        .withWuaId(USER_WUA_ID)
        .build();

    when(teamQueryService.getTeamRolesForUser(USER_WUA_ID)).thenReturn(Set.of(teamRole));

    assertThat(licenceContinuationActionService.getAvailableUserActionItems(applicationDetail, serviceUserDetail))
        .doesNotContain(LicenceContinuationActionItem.CONFIRM_CONTINUATION.toActionItemView(applicationDetail));
  }

  @Test
  void getAvailableUserActionItems_confirmContinuation_notAvailableWhenRoleNotAllowed() {
    var applicationDetail = LicenceContinuationApplicationTestUtil.builder()
        .withId(UUID.randomUUID())
        .withStatus(LicenceContinuationApplicationStatus.SUBMITTED)
        .build();

    TeamRole teamRole = TeamRoleTestUtil.newBuilder()
        .withRole(Role.SCHEDULE_ADMINISTRATOR)
        .withTeam(new Team())
        .withWuaId(USER_WUA_ID)
        .build();

    when(teamQueryService.getTeamRolesForUser(USER_WUA_ID)).thenReturn(Set.of(teamRole));

    assertThat(licenceContinuationActionService.getAvailableUserActionItems(applicationDetail, serviceUserDetail))
        .doesNotContain(LicenceContinuationActionItem.CONFIRM_CONTINUATION.toActionItemView(applicationDetail));
  }

}