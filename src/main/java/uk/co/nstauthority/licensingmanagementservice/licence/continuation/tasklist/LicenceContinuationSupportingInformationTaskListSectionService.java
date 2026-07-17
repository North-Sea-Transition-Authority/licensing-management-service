package uk.co.nstauthority.licensingmanagementservice.licence.continuation.tasklist;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.supportinginformation.LicenceContinuationSupportingInformationController;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.supportinginformation.LicenceContinuationSupportingInformationSubmissionService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListItem;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListLabel;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListSection;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListSectionService;

@Service
public class LicenceContinuationSupportingInformationTaskListSectionService
    implements TaskListSectionService<LicenceContinuationApplicationDetail> {

  static final String ADDITIONAL_SUPPORTING_INFORMATION_SECTION_NAME = "Additional supporting information";
  static final int SECTION_ORDER = 30;

  private final LicenceContinuationSupportingInformationSubmissionService
      licenceContinuationSupportingInformationSubmissionService;

  public LicenceContinuationSupportingInformationTaskListSectionService(
      LicenceContinuationSupportingInformationSubmissionService licenceContinuationSupportingInformationSubmissionService
  ) {
    this.licenceContinuationSupportingInformationSubmissionService =
        licenceContinuationSupportingInformationSubmissionService;
  }

  @Override
  public Optional<TaskListSection> getSection(
      LicenceContinuationApplicationDetail licenceContinuationApplicationDetail,
      ServiceUserDetail user
  ) {
    var items = List.of(
        new TaskListItem(
            ADDITIONAL_SUPPORTING_INFORMATION_SECTION_NAME,
            TaskListLabel.notStartedOrComplete(licenceContinuationSupportingInformationSubmissionService
                .isSectionSubmittable(licenceContinuationApplicationDetail)),
            ReverseRouter.route(on(LicenceContinuationSupportingInformationController.class)
                .renderForm(licenceContinuationApplicationDetail.getId(), null))
        )
    );

    return Optional.of(new TaskListSection(ADDITIONAL_SUPPORTING_INFORMATION_SECTION_NAME, SECTION_ORDER, items));
  }
}
