package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.tasklist;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.ArrayList;
import java.util.Optional;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney.LicenceWorkProgrammeAmendmentSubmissionService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney.LicenceWorkProgrammeAmendmentSummaryController;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney.SelectLicenceWorkAmendmentController;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.extendjourney.LicenceScheduleExtensionController;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.extendjourney.LicenceScheduleExtensionSubmissionService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.externalcontributorjourney.ScheduleWorkProgrammeExternalContributorController;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.externalcontributorjourney.ScheduleWorkProgrammeExternalContributorService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overallrequest.LicenceScheduleSupportingInformationController;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overallrequest.LicenceScheduleSupportingInformationSubmissionService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.requestpurpose.SwpApplicationRequestPurpose;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.requestpurpose.SwpApplicationRequestPurposeController;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.requestpurpose.SwpApplicationRequestPurposeRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.requestpurpose.SwpApplicationRequestPurposeService;
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
  private final SwpApplicationRequestPurposeService swpApplicationRequestPurposeService;
  private final LicenceScheduleSupportingInformationSubmissionService licenceScheduleSupportingInformationSubmissionService;
  private final ScheduleWorkProgrammeExternalContributorService scheduleWorkProgrammeExternalContributorService;

  public ScheduleWorkProgrammeApplicationTaskListSectionService(
      SwpApplicationRequestPurposeRepository swpApplicationRequestPurposeRepository,
      SwpApplicationRequestPurposeService swpApplicationRequestPurposeService,
      LicenceScheduleExtensionSubmissionService licenceScheduleExtensionSubmissionService,
      LicenceWorkProgrammeAmendmentSubmissionService licenceWorkProgrammeAmendmentSubmissionService,
      LicenceScheduleSupportingInformationSubmissionService licenceScheduleSupportingInformationSubmissionService,
      ScheduleWorkProgrammeExternalContributorService scheduleWorkProgrammeExternalContributorService
  ) {
    this.licenceScheduleExtensionSubmissionService = licenceScheduleExtensionSubmissionService;
    this.swpApplicationRequestPurposeRepository = swpApplicationRequestPurposeRepository;
    this.swpApplicationRequestPurposeService = swpApplicationRequestPurposeService;
    this.licenceWorkProgrammeAmendmentSubmissionService = licenceWorkProgrammeAmendmentSubmissionService;
    this.licenceScheduleSupportingInformationSubmissionService = licenceScheduleSupportingInformationSubmissionService;
    this.scheduleWorkProgrammeExternalContributorService = scheduleWorkProgrammeExternalContributorService;
  }

  static final String APPLICATION_DETAILS_SECTION_NAME = "Schedule and work programme application details";
  static final String WHAT_ARE_YOU_REQUESTING_TO_DO = "What are you requesting to do?";
  static final String EXTERNAL_CONTRIBUTORS = "External contributors";
  static final String EXTENSION_DETAILS = "Extension Details";
  static final String SUPPORTING_INFORMATION = "Supporting information";
  static final String AMENDMENT_DETAILS = "Work programme amendment details";
  static final int SECTION_ORDER = 10;


  @Override
  public Optional<TaskListSection> getSection(
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail,
      ServiceUserDetail user) {

    var existingPurpose = swpApplicationRequestPurposeRepository
        .getByScheduleWorkProgrammeApplicationDetail(scheduleWorkProgrammeApplicationDetail);

    boolean extensionSelection = existingPurpose
        .map(requestPurpose -> requestPurpose.getExtendTerm() || requestPurpose.getExtendPhaseOrTerm())
        .orElse(false);

    var hasAmendableActivities = swpApplicationRequestPurposeService
        .hasAmendableWorkProgrammeActivities(scheduleWorkProgrammeApplicationDetail);

    boolean amendmentSelection = existingPurpose
        .map(SwpApplicationRequestPurpose::getAmendWorkProgramme)
        .orElse(false)
        && hasAmendableActivities;

    var items = new ArrayList<TaskListItem>();
    items.add(new TaskListItem(
        EXTERNAL_CONTRIBUTORS,
        TaskListLabel.notStartedOrComplete(
            scheduleWorkProgrammeExternalContributorService.isExternalContributorSectionComplete(
                scheduleWorkProgrammeApplicationDetail)),
        ReverseRouter.route(on(ScheduleWorkProgrammeExternalContributorController.class)
            .renderForm(scheduleWorkProgrammeApplicationDetail.getId(), null))
    ));

    if (hasAmendableActivities) {
      items.add(new TaskListItem(
          WHAT_ARE_YOU_REQUESTING_TO_DO,
          TaskListLabel.notStartedOrComplete(extensionSelection || amendmentSelection),
          ReverseRouter.route(on(SwpApplicationRequestPurposeController.class)
              .renderForm(scheduleWorkProgrammeApplicationDetail.getId(), null))
      ));
    }

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
      var amendmentStatus = licenceWorkProgrammeAmendmentSubmissionService.getAmendmentSectionStatus(
          scheduleWorkProgrammeApplicationDetail);

      items.add(new TaskListItem(
          AMENDMENT_DETAILS,
          TaskListLabel.notStartedOrComplete(amendmentStatus.complete()),
          amendmentStatus.submittable()
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