package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.EnergyPortalUserJson;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.EnergyPortalUserService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.WebUserAccountId;
import uk.co.nstauthority.licensingmanagementservice.file.ApplicationFileService;
import uk.co.nstauthority.licensingmanagementservice.formatting.DateFormatUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplication;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overview.ScheduleWorkProgrammeApplicationContext;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overview.finaldecision.RecordFinalDecisionFileUsage;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryCard;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryDataView;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryFileView;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryItem;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListSection;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListSectionService;

@Service
public class RecordOfDecisionTaskListService {

  static final String SIGNED_DSP_HEADING = "Signed DSP";
  static final String SUBMITTED_BY_USER_PURPOSE = "Fetch submitted by user for record of decision task list";
  static final String STEWARD_USER_PURPOSE = "Fetch steward user for record of decision task list";

  private final List<TaskListSectionService<RecordOfDecisionTaskListContext>> taskListSectionServices;
  private final ApplicationFileService applicationFileService;
  private final LicenceService licenceService;
  private final EnergyPortalUserService energyPortalUserService;

  @Autowired
  public RecordOfDecisionTaskListService(
      List<TaskListSectionService<RecordOfDecisionTaskListContext>> taskListSectionServices,
      ApplicationFileService applicationFileService,
      LicenceService licenceService,
      EnergyPortalUserService energyPortalUserService
  ) {
    this.taskListSectionServices = taskListSectionServices;
    this.applicationFileService = applicationFileService;
    this.licenceService = licenceService;
    this.energyPortalUserService = energyPortalUserService;
  }

  public List<TaskListSection> getTaskListSections(RecordOfDecisionTaskListContext context, ServiceUserDetail user) {
    return taskListSectionServices.stream()
        .map(taskListSectionService -> taskListSectionService.getSection(context, user))
        .flatMap(Optional::stream)
        .sorted(Comparator.comparing(TaskListSection::displayOrder))
        .toList();
  }

  // TODO: placeholder for the signed DSP
  public Optional<SummaryItem> getSignedDspSummaryItem(ScheduleWorkProgrammeApplicationDetail applicationDetail) {
    var uploadedFiles = applicationFileService.getUploadedFiles(
        RecordFinalDecisionFileUsage.fromApplication(applicationDetail));

    if (uploadedFiles.isEmpty()) {
      return Optional.empty();
    }

    var fileViews = uploadedFiles.stream()
        .map(file -> SummaryFileView.newFromUploadedFile(
            file.getKey(),
            file,
            ReverseRouter.route(on(RecordOfDecisionTaskListController.class)
                .downloadSignedDsp(file.getId(), applicationDetail.getId(), null, null))))
        .toList();

    return Optional.of(SummaryItem.withCards(
        null,
        List.of(SummaryCard.filesSummaryCardWithHeading(SIGNED_DSP_HEADING, fileViews))));
  }

  public ScheduleWorkProgrammeApplicationContext getApplicationContext(
      ScheduleWorkProgrammeApplicationDetail applicationDetail) {
    var licence = applicationDetail.getLicence();

    var submittedByUser = energyPortalUserService.getByWuaId(
        WebUserAccountId.from(applicationDetail.getSubmittedByWuaId()),
        SUBMITTED_BY_USER_PURPOSE);

    var summaryDataView = SummaryDataView.newBuilder()
        .addStringValue("Submitted by", submittedByUser.displayName())
        .addStringValue("Submission date",
            DateFormatUtil.convertToDisplayTextWithTime(applicationDetail.getSubmittedDatetime()))
        .addStringValue("Steward", getStewardName(applicationDetail.getScheduleWorkProgrammeApplication()))
        .build();

    return new ScheduleWorkProgrammeApplicationContext(
        applicationDetail.getScheduleWorkProgrammeApplication().getApplicationReference(),
        licenceService.getLicencePageCaption(licence),
        List.of(summaryDataView));
  }

  private String getStewardName(ScheduleWorkProgrammeApplication application) {
    return Optional.ofNullable(application.getStewardWuaId())
        .map(wuaId -> WebUserAccountId.from(application.getStewardWuaId()))
        .flatMap(webUserAccountId -> energyPortalUserService.findByWuaId(webUserAccountId, STEWARD_USER_PURPOSE))
        .map(EnergyPortalUserJson::displayName)
        .orElse("Not allocated");
  }
}
