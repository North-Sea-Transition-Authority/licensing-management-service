package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDurationDisplayUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivity;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.util.BooleanUtil;

@Service
public class LicenceWorkProgrammeAmendmentSummaryService {

  private final LicenceWorkProgrammeAmendmentSummaryRepository licenceWorkProgrammeAmendmentSummaryRepository;
  private final LicenceWorkProgrammeAmendmentService licenceWorkProgrammeAmendmentService;
  private final WorkProgrammeActivityService  workProgrammeActivityService;

  public LicenceWorkProgrammeAmendmentSummaryService(
      LicenceWorkProgrammeAmendmentSummaryRepository licenceWorkProgrammeAmendmentSummaryRepository,
      LicenceWorkProgrammeAmendmentService licenceWorkProgrammeAmendmentService,
      WorkProgrammeActivityService workProgrammeActivityService
  ) {
    this.licenceWorkProgrammeAmendmentSummaryRepository = licenceWorkProgrammeAmendmentSummaryRepository;
    this.licenceWorkProgrammeAmendmentService = licenceWorkProgrammeAmendmentService;
    this.workProgrammeActivityService = workProgrammeActivityService;
  }

  public Optional<LicenceWorkProgrammeAmendmentSummary>
      getLicenceWorkProgrammeAmendmentSummaryByScheduleWorkProgrammeApplicationDetail(
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail
  ) {
    return licenceWorkProgrammeAmendmentSummaryRepository
        .findLicenceWorkProgrammeAmendmentSummaryByScheduleWorkProgrammeApplicationDetails(
        scheduleWorkProgrammeApplicationDetail);
  }

  public LicenceWorkProgrammeAmendmentSummaryView createSummaryViewFromWorkProgrammeAmendments(
      LicenceWorkProgrammeAmendmentRequest amendmentRequest,
      LicenceWorkProgrammeAmendmentSummaryMode summaryMode) {

    WorkProgrammeActivity workProgrammeActivity = workProgrammeActivityService.getWorkProgrammeActivityByIdOrThrow(
        amendmentRequest.getWorkProgrammeActivity().getId());

    String summaryCardTitle = resolveCategoryDisplay(workProgrammeActivity);

    UUID activityId = amendmentRequest.getWorkProgrammeActivity().getId();
    UUID applicationDetailId = amendmentRequest.getScheduleWorkProgrammeApplicationDetails().getId();

    String changeUrl = buildChangeUrl(activityId, applicationDetailId);
    String deleteUrl = buildDeleteUrl(activityId, applicationDetailId);

    return new LicenceWorkProgrammeAmendmentSummaryView(
        summaryCardTitle,
        BooleanUtil.yesNoFromBoolean(amendmentRequest.getWorkProgrammeChangeRequested(), ""),
        StringUtils.defaultIfBlank(amendmentRequest.getWorkProgrammeAmendmentInformation(), ""),
        BooleanUtil.yesNoFromBoolean(amendmentRequest.getWorkProgrammeCompletionDateChangeRequested(), ""),
        ThreeFieldDurationDisplayUtil.convertToDisplayText(amendmentRequest.getWorkProgrammeExtensionDuration()),
        summaryMode,
        changeUrl,
        deleteUrl,
        amendmentRequest.getWorkProgrammeCompletionDateChangeRequested(),
        amendmentRequest.getWorkProgrammeChangeRequested()
    );
  }

  private String resolveCategoryDisplay(WorkProgrammeActivity workProgrammeActivity) {
    return StringUtils.defaultIfBlank(
        workProgrammeActivity.getOtherCategoryName(),
        StringUtils.defaultIfBlank(workProgrammeActivity.getCategory().getDisplayName(), "")
    );
  }

  private String buildChangeUrl(UUID workProgrammeActivityId, UUID scheduleWorkProgrammeApplicationDetailId) {
    return ReverseRouter.route(on(LicenceWorkProgrammeAmendmentController.class).renderForm(
        workProgrammeActivityId,
        null,
        scheduleWorkProgrammeApplicationDetailId,
        null)
    );
  }

  private String buildDeleteUrl(UUID workProgrammeActivityId, UUID scheduleWorkProgrammeApplicationDetailId) {
    return ReverseRouter.route(on(LicenceWorkProgrammeAmendmentDeleteController.class).renderForm(
        workProgrammeActivityId,
        null,
        scheduleWorkProgrammeApplicationDetailId,
        null)
    );
  }

  public List<LicenceWorkProgrammeAmendmentSummaryView>
      getWorkProgrammeAmendmentSummaryViewsFromScheduleWorkProgrammeApplicationDetail(
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail) {

    var workProgrammeAmendments = licenceWorkProgrammeAmendmentService
        .getAmendmentRequestsByScheduleWorkProgrammeApplicationDetail(
            scheduleWorkProgrammeApplicationDetail);

    return workProgrammeAmendments.stream()
        .map(wpa ->
            createSummaryViewFromWorkProgrammeAmendments(wpa, LicenceWorkProgrammeAmendmentSummaryMode.EDIT))
        .toList();
  }

  private LicenceWorkProgrammeAmendmentSummaryForm licenceWorkProgramAmendmentSummaryToForm(
      LicenceWorkProgrammeAmendmentSummary licenceWorkProgrammeAmendmentSummary) {

    var form = new LicenceWorkProgrammeAmendmentSummaryForm();

    form.setLicenceWorkProgrammeAmendmentSummaryOptions(
        licenceWorkProgrammeAmendmentSummary.getLicenceWorkProgrammeAmendmentSummaryOptions());
    return form;
  }

  public LicenceWorkProgrammeAmendmentSummaryForm getWorkProgrammeAmendmentByScheduleWorkProgrammeApplicationDetail(
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail) {
    return getLicenceWorkProgrammeAmendmentSummaryByScheduleWorkProgrammeApplicationDetail(
        scheduleWorkProgrammeApplicationDetail)
        .map(this::licenceWorkProgramAmendmentSummaryToForm)
        .orElse(new LicenceWorkProgrammeAmendmentSummaryForm());
  }

  @Transactional
  public void saveWorkProgrammeAmendmentSummaryForm(
      LicenceWorkProgrammeAmendmentSummaryForm form,
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail
  ) {

    var licenceWorkProgrammeAmendmentSummaryOptions =
        getLicenceWorkProgrammeAmendmentSummaryByScheduleWorkProgrammeApplicationDetail(
        scheduleWorkProgrammeApplicationDetail
    ).orElse(new LicenceWorkProgrammeAmendmentSummary());

    licenceWorkProgrammeAmendmentSummaryOptions.setLicenceWorkProgrammeAmendmentSummaryOptions(
        form.getLicenceWorkProgrammeAmendmentSummaryOptions());
    licenceWorkProgrammeAmendmentSummaryOptions.setScheduleWorkProgrammeApplicationDetails(
        scheduleWorkProgrammeApplicationDetail);

    licenceWorkProgrammeAmendmentSummaryRepository.save(licenceWorkProgrammeAmendmentSummaryOptions);
  }
}