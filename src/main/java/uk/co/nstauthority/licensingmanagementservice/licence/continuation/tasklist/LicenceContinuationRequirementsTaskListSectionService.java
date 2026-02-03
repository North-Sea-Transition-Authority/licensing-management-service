package uk.co.nstauthority.licensingmanagementservice.licence.continuation.tasklist;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.requirementjourney.LicenceContinuationWpaRequirementController;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.requirementjourney.LicenceContinuationWpaSubmissionService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListItem;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListLabel;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListSection;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListSectionService;

@Service
public class LicenceContinuationRequirementsTaskListSectionService
    implements TaskListSectionService<LicenceContinuationApplicationDetail> {

  private final LicenceContinuationWpaSubmissionService licenceContinuationWpaSubmissionService;
  static final String LICENCE_CONTINUATION_REQUIREMENTS_SECTION_NAME = "Continuation requirements";

  public LicenceContinuationRequirementsTaskListSectionService(
      LicenceContinuationWpaSubmissionService licenceContinuationWpaSubmissionService
  ) {
    this.licenceContinuationWpaSubmissionService = licenceContinuationWpaSubmissionService;
  }

  static final String WORK_PROGRAMMES = "Work programme activities";

  static final int SECTION_ORDER = 20;


  @Override
  public Optional<TaskListSection> getSection(
      LicenceContinuationApplicationDetail licenceContinuationApplicationDetail,
      ServiceUserDetail user) {

    var items = new ArrayList<>(List.of(
        new TaskListItem(
            WORK_PROGRAMMES,
            TaskListLabel.notStartedOrComplete(licenceContinuationWpaSubmissionService.isSectionSubmittable(
                licenceContinuationApplicationDetail
            )),
            ReverseRouter.route(on(LicenceContinuationWpaRequirementController.class)
                                    .renderForm(licenceContinuationApplicationDetail.getId(), null))
        )
    ));

    return Optional.of(new TaskListSection(LICENCE_CONTINUATION_REQUIREMENTS_SECTION_NAME, SECTION_ORDER, items));
  }
}