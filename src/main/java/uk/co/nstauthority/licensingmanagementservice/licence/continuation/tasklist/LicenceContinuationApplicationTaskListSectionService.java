package uk.co.nstauthority.licensingmanagementservice.licence.continuation.tasklist;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.exception.LmsEntityNotFoundException;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationType;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListItem;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListLabel;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListSection;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListSectionService;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamScopeReference;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;
import uk.co.nstauthority.licensingmanagementservice.teams.management.TeamManagementController;
import uk.co.nstauthority.licensingmanagementservice.teams.management.TeamManagementService;

@Service
public class LicenceContinuationApplicationTaskListSectionService
    implements TaskListSectionService<LicenceContinuationApplicationDetail> {

  private final TeamManagementService teamManagementService;
  static final String LICENCE_CONTINUATION_DETAILS_SECTION_NAME = "Licence continuation application details";
  static final String EXTERNAL_CONTRIBUTORS = "External contributors";
  static final int SECTION_ORDER = 10;

  public LicenceContinuationApplicationTaskListSectionService(
      TeamManagementService teamManagementService
  ) {
    this.teamManagementService = teamManagementService;
  }

  @Override
  public Optional<TaskListSection> getSection(
      LicenceContinuationApplicationDetail licenceContinuationApplicationDetail,
      ServiceUserDetail user) {

    var scopeRef = TeamScopeReference.from(
        licenceContinuationApplicationDetail.getId().toString(),
        ApplicationType.CONTINUATION_APPLICATION.name()
    );

    var externalContributors = teamManagementService.getScopedTeam(
        TeamType.EXTERNAL_CONTRIBUTORS,
        scopeRef
    ).orElseThrow(() -> new LmsEntityNotFoundException(
        String.format("No external contacts team found for application with id : %s ",
        licenceContinuationApplicationDetail.getLicenceContinuationApplication().getId()
    )));

    var items = new ArrayList<>(List.of(
        new TaskListItem(
            EXTERNAL_CONTRIBUTORS,
            TaskListLabel.notStartedOrComplete(true),
            ReverseRouter.route(on(TeamManagementController.class)
                 .renderExternalContributorsTeamList(externalContributors.getId(), null))
        )
    ));

    return Optional.of(new TaskListSection(LICENCE_CONTINUATION_DETAILS_SECTION_NAME, SECTION_ORDER, items));
  }
}