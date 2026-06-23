package uk.co.nstauthority.licensingmanagementservice.licence.continuation.tasklist;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.externalcontributorjourney.LicenceContinuationExternalContributorController;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.externalcontributorjourney.LicenceContinuationExternalContributorService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListItem;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListLabel;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListSection;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListSectionService;

@Service
public class LicenceContinuationApplicationTaskListSectionService
    implements TaskListSectionService<LicenceContinuationApplicationDetail> {

  private final LicenceContinuationExternalContributorService licenceContinuationExternalContributorService;
  static final String LICENCE_CONTINUATION_DETAILS_SECTION_NAME = "Licence continuation application details";
  static final String EXTERNAL_CONTRIBUTORS = "External contributors";
  static final int SECTION_ORDER = 10;

  public LicenceContinuationApplicationTaskListSectionService(
      LicenceContinuationExternalContributorService licenceContinuationExternalContributorService
  ) {
    this.licenceContinuationExternalContributorService = licenceContinuationExternalContributorService;
  }

  @Override
  public Optional<TaskListSection> getSection(
      LicenceContinuationApplicationDetail licenceContinuationApplicationDetail,
      ServiceUserDetail user) {

    var externalContributorsComplete = licenceContinuationExternalContributorService
        .isExternalContributorSectionComplete(licenceContinuationApplicationDetail);

    var items = new ArrayList<>(List.of(
        new TaskListItem(
            EXTERNAL_CONTRIBUTORS,
            TaskListLabel.notStartedOrComplete(externalContributorsComplete),
            ReverseRouter.route(on(LicenceContinuationExternalContributorController.class)
                 .renderForm(licenceContinuationApplicationDetail.getId(), null))
        )
    ));

    return Optional.of(new TaskListSection(LICENCE_CONTINUATION_DETAILS_SECTION_NAME, SECTION_ORDER, items));
  }
}