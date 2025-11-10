package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.tasklist;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney.LicenceWorkProgrammeAmendmentSubmissionService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney.LicenceWorkProgrammeAmendmentSummaryController;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney.SelectLicenceWorkAmendmentController;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.extendjourney.LicenceScheduleExtensionController;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.extendjourney.LicenceScheduleExtensionSubmissionService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overallrequest.LicenceScheduleSupportingInformationController;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overallrequest.LicenceScheduleSupportingInformationSubmissionService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.tasklist.requestpurpose.SwpApplicationRequestPurposeController;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.tasklist.requestpurpose.SwpApplicationRequestPurposeRepository;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListItem;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListLabel;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListSection;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListSectionService;

@Service
public class ScheduleWorkProgrammeApplicationTaskListSectionService
    implements TaskListSectionService<ScheduleWorkProgrammeApplicationDetail> {

  private final LicenceScheduleExtensionSubmissionService licenceScheduleExtensionSubmissionService;
  private final LicenceWorkProgrammeAmendmentSubmissionService licenceWorkProgrammeAmendmentSubmissionService;
  private final SwpApplicationRequestPurposeRepository swpApplicationRequestPurposeRepository;
  private final LicenceScheduleSupportingInformationSubmissionService licenceScheduleSupportingInformationSubmissionService;

  private boolean extensionSelection;
  private boolean amendmentSelection;

  public ScheduleWorkProgrammeApplicationTaskListSectionService(
      SwpApplicationRequestPurposeRepository swpApplicationRequestPurposeRepository,
      LicenceScheduleExtensionSubmissionService licenceScheduleExtensionSubmissionService,
      LicenceWorkProgrammeAmendmentSubmissionService licenceWorkProgrammeAmendmentSubmissionService,
      LicenceScheduleSupportingInformationSubmissionService licenceScheduleSupportingInformationSubmissionService
  ) {
    this.licenceScheduleExtensionSubmissionService = licenceScheduleExtensionSubmissionService;
    this.swpApplicationRequestPurposeRepository = swpApplicationRequestPurposeRepository;
    this.licenceWorkProgrammeAmendmentSubmissionService = licenceWorkProgrammeAmendmentSubmissionService;
    this.licenceScheduleSupportingInformationSubmissionService = licenceScheduleSupportingInformationSubmissionService;
  }

  static final String APPLICATION_DETAILS_SECTION_NAME = "Schedule and work programme application details";
  static final String WHAT_ARE_YOU_REQUESTING_TO_DO = "What are you requesting to do?";
  static final String EXTENSION_DETAILS = "Extension Details";
  static final String SUPPORTING_INFORMATION = "Supporting information";
  static final String AMENDMENT_DETAILS = "Work programme amendment details";
  static final int SECTION_ORDER = 10;


  @Override
  public Optional<TaskListSection> getSection(
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail,
      ServiceUserDetail user) {

    swpApplicationRequestPurposeRepository
        .getByScheduleWorkProgrammeApplicationDetail(scheduleWorkProgrammeApplicationDetail)
        .ifPresent(purpose -> {
          extensionSelection = purpose.getExtendTerm() || purpose.getExtendPhaseOrTerm();
          amendmentSelection = purpose.getAmendWorkProgramme();
        });

    var items = new ArrayList<>(List.of(
        new TaskListItem(
            WHAT_ARE_YOU_REQUESTING_TO_DO,
            TaskListLabel.notStartedOrComplete(extensionSelection || amendmentSelection),
            ReverseRouter.route(on(SwpApplicationRequestPurposeController.class)
                .renderForm(scheduleWorkProgrammeApplicationDetail.getId(), null))
        )));

    if (extensionSelection) {
      items.add(new TaskListItem(
          EXTENSION_DETAILS,
          TaskListLabel.notStartedOrComplete(
              licenceScheduleExtensionSubmissionService.isSectionSubmittable(
                  scheduleWorkProgrammeApplicationDetail)),
          ReverseRouter.route(on(LicenceScheduleExtensionController.class)
              .renderForm(scheduleWorkProgrammeApplicationDetail.getId(), null))
      ));
    }
    if (amendmentSelection) {
      var isSubmittable = licenceWorkProgrammeAmendmentSubmissionService.isAmendmentSectionSubmittable(
          scheduleWorkProgrammeApplicationDetail);
      var isComplete = licenceWorkProgrammeAmendmentSubmissionService.isAmendmentSectionComplete(
          scheduleWorkProgrammeApplicationDetail);

      items.add(new TaskListItem(
          AMENDMENT_DETAILS,
          TaskListLabel.notStartedOrComplete(isComplete),
          isSubmittable
          ? ReverseRouter.route(on(LicenceWorkProgrammeAmendmentSummaryController.class).renderForm(
          scheduleWorkProgrammeApplicationDetail.getId(), scheduleWorkProgrammeApplicationDetail))
          : ReverseRouter.route(on(SelectLicenceWorkAmendmentController.class).renderForm(
          scheduleWorkProgrammeApplicationDetail.getId(), null))));
    }

    if (extensionSelection || amendmentSelection) {
      items.add(new TaskListItem(
          SUPPORTING_INFORMATION,
          TaskListLabel.notStartedOrComplete(
              licenceScheduleSupportingInformationSubmissionService.isSectionSubmittable(
                  scheduleWorkProgrammeApplicationDetail)),
          ReverseRouter.route(on(LicenceScheduleSupportingInformationController.class)
              .renderForm(scheduleWorkProgrammeApplicationDetail.getId(), null))
      ));
    }

    return Optional.of(new TaskListSection(APPLICATION_DETAILS_SECTION_NAME, SECTION_ORDER, items));
  }
}