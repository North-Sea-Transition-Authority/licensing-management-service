package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDurationDisplayUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.util.BooleanUtil;

@Service
public class LicenceWorkProgrammeAmendmentSummaryService {

  private final LicenceWorkProgrammeAmendmentSummaryRepository licenceWorkProgrammeAmendmentSummaryRepository;
  private final LicenceWorkProgrammeAmendmentService licenceWorkProgrammeAmendmentService;

  public LicenceWorkProgrammeAmendmentSummaryService(
      LicenceWorkProgrammeAmendmentSummaryRepository licenceWorkProgrammeAmendmentSummaryRepository,
      LicenceWorkProgrammeAmendmentService licenceWorkProgrammeAmendmentService) {
    this.licenceWorkProgrammeAmendmentSummaryRepository = licenceWorkProgrammeAmendmentSummaryRepository;
    this.licenceWorkProgrammeAmendmentService = licenceWorkProgrammeAmendmentService;
  }

  public Optional<LicenceWorkProgrammeAmendmentSummary>
       getLicenceWorkProgrammeAmendmentSummaryByScheduleWorkProgrammeApplicationDetail(
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail) {
    return licenceWorkProgrammeAmendmentSummaryRepository
        .findLicenceWorkProgrammeAmendmentSummaryByScheduleWorkProgrammeApplicationDetails(
        scheduleWorkProgrammeApplicationDetail);
  }

  public LicenceWorkProgrammeAmendmentSummaryView createSummaryViewFromWorkProgrammeAmendments(
      LicenceWorkProgrammeAmendmentRequest licenceWorkProgrammeAmendmentRequest,
      LicenceWorkProgrammeAmendmentSummaryMode summaryMode) {

    return new LicenceWorkProgrammeAmendmentSummaryView(
        StringUtils.defaultIfBlank(licenceWorkProgrammeAmendmentRequest.getWorkProgrammeActivityId().toString(), ""),
        BooleanUtil.yesNoFromBoolean(licenceWorkProgrammeAmendmentRequest.getWorkProgrammeChangeRequested(), ""),
        StringUtils.defaultIfBlank(licenceWorkProgrammeAmendmentRequest.getWorkProgrammeAmendmentInformation(), ""),
        BooleanUtil.yesNoFromBoolean(
            licenceWorkProgrammeAmendmentRequest.getWorkProgrammeCompletionDateChangeRequested(), ""),
        ThreeFieldDurationDisplayUtil
            .convertToDisplayText(licenceWorkProgrammeAmendmentRequest.getWorkProgrammeExtensionDuration()),
        summaryMode,
        ReverseRouter.route(on(LicenceWorkProgrammeAmendmentController.class).renderForm(
        licenceWorkProgrammeAmendmentRequest.getWorkProgrammeActivityId(),
        licenceWorkProgrammeAmendmentRequest.getScheduleWorkProgrammeApplicationDetails().getId(),
       null)),
        ReverseRouter.route(on(LicenceWorkProgrammeAmendmentDeleteController.class).renderForm(
            licenceWorkProgrammeAmendmentRequest.getWorkProgrammeActivityId(),
            licenceWorkProgrammeAmendmentRequest.getScheduleWorkProgrammeApplicationDetails().getId(),
            null)),
        licenceWorkProgrammeAmendmentRequest.getWorkProgrammeCompletionDateChangeRequested(),
        licenceWorkProgrammeAmendmentRequest.getWorkProgrammeChangeRequested());
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
}