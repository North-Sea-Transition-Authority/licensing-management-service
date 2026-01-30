package uk.co.nstauthority.licensingmanagementservice.licence.continuation.tasklist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplication;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationTestUtil;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListItem;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListLabel;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListSection;
import uk.co.nstauthority.licensingmanagementservice.teams.Team;
import uk.co.nstauthority.licensingmanagementservice.teams.management.TeamManagementController;
import uk.co.nstauthority.licensingmanagementservice.teams.management.TeamManagementService;

@ExtendWith(MockitoExtension.class)
class LicenceContinuationApplicationTaskListSectionServiceTest {

  @Mock
  private TeamManagementService teamManagementService;

  @InjectMocks
  private LicenceContinuationApplicationTaskListSectionService licenceContinuationApplicationTaskListSectionService;

  private LicenceContinuationApplicationDetail licenceContinuationApplicationDetail;
  private ServiceUserDetail user;
  private Team team;

  @BeforeEach
  void setUp() {
    user = ServiceUserDetailTestUtil
        .newBuilder().build();
    team = new Team(UUID.randomUUID());

    var licenceContinuationApplication = new LicenceContinuationApplication();
    licenceContinuationApplication.setId(UUID.randomUUID());

    this.licenceContinuationApplicationDetail = LicenceContinuationApplicationTestUtil
        .builder()
        .withId(UUID.randomUUID())
        .withLicenceContinuationApplication(licenceContinuationApplication)
        .build();

    when(teamManagementService.getScopedTeam(any(), any())).thenReturn(Optional.of(team));
  }

  @Test
  void getSection() {
    var sectionOptional = licenceContinuationApplicationTaskListSectionService.getSection(licenceContinuationApplicationDetail, user);
    assertThat(sectionOptional).isPresent();
    var section = sectionOptional.get();

    assertThat(section)
        .extracting(
            TaskListSection::items,
            TaskListSection::displayName,
            TaskListSection::displayOrder
        )
        .containsExactly(
            List.of(
                new TaskListItem(
                    LicenceContinuationApplicationTaskListSectionService.EXTERNAL_CONTRIBUTORS,
                    TaskListLabel.COMPLETE,
                    ReverseRouter.route(on(TeamManagementController.class).renderExternalContributorsTeamList(team.getId(), user))
                )
            ),
            LicenceContinuationApplicationTaskListSectionService.LICENCE_CONTINUATION_DETAILS_SECTION_NAME,
            LicenceContinuationApplicationTaskListSectionService.SECTION_ORDER
        );
  }
}