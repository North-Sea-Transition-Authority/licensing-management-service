package uk.co.nstauthority.licensingmanagementservice.licence.continuation.tasklist;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.ArrayList;
import java.util.Optional;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.requirementjourney.LicenceContinuationLicenceOperatorsController;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.requirementjourney.LicenceContinuationLicenceOperatorsSubmissionService;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.requirementjourney.LicenceContinuationOtherRequirementController;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.requirementjourney.LicenceContinuationOtherRequirementSubmissionService;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.requirementjourney.LicenceContinuationWpaRequirementController;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.requirementjourney.LicenceContinuationWpaSubmissionService;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.requirementjourney.OtherRequirementsVisibilityResolverService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListItem;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListLabel;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListSection;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListSectionService;

@Service
public class LicenceContinuationRequirementsTaskListSectionService
    implements TaskListSectionService<LicenceContinuationApplicationDetail> {

  private final LicenceContinuationWpaSubmissionService licenceContinuationWpaSubmissionService;
  private final LicenceContinuationOtherRequirementSubmissionService licenceContinuationOtherRequirementSubmissionService;
  private final LicenceContinuationLicenceOperatorsSubmissionService licenceContinuationLicenceOperatorsSubmissionService;
  private final OtherRequirementsVisibilityResolverService otherRequirementsVisibilityResolverService;

  static final String LICENCE_CONTINUATION_REQUIREMENTS_SECTION_NAME = "Continuation requirements";

  public LicenceContinuationRequirementsTaskListSectionService(
      LicenceContinuationWpaSubmissionService licenceContinuationWpaSubmissionService,
      LicenceContinuationOtherRequirementSubmissionService licenceContinuationOtherRequirementSubmissionService,
      LicenceContinuationLicenceOperatorsSubmissionService licenceContinuationLicenceOperatorsSubmissionService,
      OtherRequirementsVisibilityResolverService otherRequirementsVisibilityResolverService
  ) {
    this.licenceContinuationWpaSubmissionService = licenceContinuationWpaSubmissionService;
    this.licenceContinuationOtherRequirementSubmissionService = licenceContinuationOtherRequirementSubmissionService;
    this.licenceContinuationLicenceOperatorsSubmissionService = licenceContinuationLicenceOperatorsSubmissionService;
    this.otherRequirementsVisibilityResolverService = otherRequirementsVisibilityResolverService;
  }

  static final String WORK_PROGRAMMES = "Work programme activities";
  static final String OTHER_REQUIREMENTS = "Other requirements";
  static final String LICENCE_OPERATORS = "Licence operators";

  static final int SECTION_ORDER = 20;


  @Override
  public Optional<TaskListSection> getSection(
      LicenceContinuationApplicationDetail licenceContinuationApplicationDetail,
      ServiceUserDetail user
  ) {

    var items = new ArrayList<TaskListItem>();
    var otherRequirementsVisibility = otherRequirementsVisibilityResolverService.resolveVisibility(
        licenceContinuationApplicationDetail
    );

    items.add(new TaskListItem(
        WORK_PROGRAMMES,
        TaskListLabel.notStartedOrComplete(licenceContinuationWpaSubmissionService.isSectionSubmittable(
            licenceContinuationApplicationDetail
        )),
        ReverseRouter.route(on(LicenceContinuationWpaRequirementController.class)
                                .renderForm(licenceContinuationApplicationDetail.getId(), null))
    ));

    if (otherRequirementsVisibility.hasAnyRequirements()) {
      items.add(new TaskListItem(
          OTHER_REQUIREMENTS,
          TaskListLabel.notStartedOrComplete(licenceContinuationOtherRequirementSubmissionService.isSectionSubmittable(
              licenceContinuationApplicationDetail
          )),
          ReverseRouter.route(on(LicenceContinuationOtherRequirementController.class)
                                  .renderForm(licenceContinuationApplicationDetail.getId(), null))
      ));
    }

    items.add(new TaskListItem(
        LICENCE_OPERATORS,
        TaskListLabel.notStartedOrComplete(licenceContinuationLicenceOperatorsSubmissionService.isSectionSubmittable(
            licenceContinuationApplicationDetail
        )),
        ReverseRouter.route(on(LicenceContinuationLicenceOperatorsController.class)
                                .renderForm(licenceContinuationApplicationDetail.getId(), null))
    ));

    return Optional.of(new TaskListSection(LICENCE_CONTINUATION_REQUIREMENTS_SECTION_NAME, SECTION_ORDER, items));
  }
}