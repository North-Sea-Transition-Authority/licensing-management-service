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
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityDateOption;
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

  public Optional<LicenceWorkProgrammeAmendmentSummary> getLicenceWorkProgrammeAmendmentSummary(
      ScheduleWorkProgrammeApplicationDetail applicationDetail
  ) {
    return licenceWorkProgrammeAmendmentSummaryRepository
        .findLicenceWorkProgrammeAmendmentSummaryByScheduleWorkProgrammeApplicationDetails(applicationDetail);
  }

  public LicenceWorkProgrammeAmendmentSummaryView createSummaryViewFromWorkProgrammeAmendments(
      LicenceWorkProgrammeAmendmentRequest amendmentRequest,
      LicenceWorkProgrammeAmendmentSummaryMode summaryMode
  ) {
    var activityId = amendmentRequest.getWorkProgrammeActivity().getId();
    var workProgrammeActivity = workProgrammeActivityService.getWorkProgrammeActivityByIdOrThrow(activityId);
    return createSummaryViewFromWorkProgrammeAmendments(amendmentRequest, summaryMode, workProgrammeActivity);
  }

  private LicenceWorkProgrammeAmendmentSummaryView createSummaryViewFromWorkProgrammeAmendments(
      LicenceWorkProgrammeAmendmentRequest amendmentRequest,
      LicenceWorkProgrammeAmendmentSummaryMode summaryMode,
      WorkProgrammeActivity workProgrammeActivity
  ) {
    var activityId = amendmentRequest.getWorkProgrammeActivity().getId();
    var applicationDetailId = amendmentRequest.getScheduleWorkProgrammeApplicationDetails().getId();

    return new LicenceWorkProgrammeAmendmentSummaryView(
        resolveCategoryDisplay(workProgrammeActivity),
        BooleanUtil.yesNoFromBoolean(amendmentRequest.getWorkProgrammeChangeRequested(), ""),
        StringUtils.defaultIfBlank(amendmentRequest.getWorkProgrammeAmendmentInformation(), ""),
        BooleanUtil.yesNoFromBoolean(amendmentRequest.getWorkProgrammeCompletionDateChangeRequested(), ""),
        ThreeFieldDurationDisplayUtil.convertToDisplayText(amendmentRequest.getWorkProgrammeExtensionDuration()),
        summaryMode,
        buildChangeUrl(activityId, applicationDetailId),
        buildDeleteUrl(activityId, applicationDetailId),
        amendmentRequest.getWorkProgrammeCompletionDateChangeRequested(),
        amendmentRequest.getWorkProgrammeChangeRequested(),
        WorkProgrammeActivityDateOption.RELATIVE_DATE.equals(workProgrammeActivity.getDateOption())
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

  public List<LicenceWorkProgrammeAmendmentSummaryView> getWorkProgrammeAmendmentSummaryViews(
      ScheduleWorkProgrammeApplicationDetail applicationDetail
  ) {
    var workProgrammeAmendments = licenceWorkProgrammeAmendmentService
        .getAmendmentRequestsByScheduleWorkProgrammeApplicationDetail(applicationDetail);

    var activityIds = workProgrammeAmendments.stream()
        .map(a -> a.getWorkProgrammeActivity().getId())
        .toList();

    var activitiesById = workProgrammeActivityService.getWorkProgrammeActivitiesByIds(activityIds);

    return workProgrammeAmendments.stream()
        .map(wpa -> createSummaryViewFromWorkProgrammeAmendments(
            wpa,
            LicenceWorkProgrammeAmendmentSummaryMode.EDIT,
            activitiesById.get(wpa.getWorkProgrammeActivity().getId())))
        .toList();
  }

  private LicenceWorkProgrammeAmendmentSummaryForm licenceWorkProgramAmendmentSummaryToForm(
      LicenceWorkProgrammeAmendmentSummary licenceWorkProgrammeAmendmentSummary
  ) {
    var form = new LicenceWorkProgrammeAmendmentSummaryForm();
    form.setLicenceWorkProgrammeAmendmentSummaryOptions(
        licenceWorkProgrammeAmendmentSummary.getLicenceWorkProgrammeAmendmentSummaryOptions()
    );

    return form;
  }

  public LicenceWorkProgrammeAmendmentSummaryForm getLicenceWorkProgrammeAmendmentSummaryForm(
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail) {
    return getLicenceWorkProgrammeAmendmentSummary(
        scheduleWorkProgrammeApplicationDetail)
        .map(this::licenceWorkProgramAmendmentSummaryToForm)
        .orElse(new LicenceWorkProgrammeAmendmentSummaryForm());
  }

  @Transactional
  public void saveWorkProgrammeAmendmentSummaryForm(
      LicenceWorkProgrammeAmendmentSummaryForm form,
      ScheduleWorkProgrammeApplicationDetail applicationDetail
  ) {
    var licenceWorkProgrammeAmendmentSummaryOptions = getLicenceWorkProgrammeAmendmentSummary(applicationDetail)
        .orElse(new LicenceWorkProgrammeAmendmentSummary());

    licenceWorkProgrammeAmendmentSummaryOptions
        .setLicenceWorkProgrammeAmendmentSummaryOptions(form.getLicenceWorkProgrammeAmendmentSummaryOptions());
    licenceWorkProgrammeAmendmentSummaryOptions
        .setScheduleWorkProgrammeApplicationDetails(applicationDetail);

    licenceWorkProgrammeAmendmentSummaryRepository.save(licenceWorkProgrammeAmendmentSummaryOptions);
  }
}