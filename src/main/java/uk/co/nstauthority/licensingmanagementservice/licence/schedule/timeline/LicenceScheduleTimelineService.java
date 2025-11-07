package uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDurationDisplayUtil;
import uk.co.nstauthority.licensingmanagementservice.formatting.DateFormatUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.rules.LicenceTypeFeatureService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTermController;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTermDeletionController;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTermService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencestartdate.LicenceStartDateService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Service
public class LicenceScheduleTimelineService {

  private final LicenceStartDateService licenceStartDateService;
  private final LicenceTypeFeatureService licenceTypeFeatureService;
  private final LicenceScheduleTermService licenceScheduleTermService;

  public LicenceScheduleTimelineService(
      LicenceStartDateService licenceStartDateService,
      LicenceTypeFeatureService licenceTypeFeatureService,
      LicenceScheduleTermService licenceScheduleTermService
  ) {
    this.licenceStartDateService = licenceStartDateService;
    this.licenceTypeFeatureService = licenceTypeFeatureService;
    this.licenceScheduleTermService = licenceScheduleTermService;
  }

  TimelineSummaryCardView getTimelineSummaryCardView(LicenceScheduleDetail licenceScheduleDetail) {
    var licence = licenceScheduleDetail.getLicenceSchedule().getLicence();
    var licenceStartDate = licenceStartDateService.getByLicenceScheduleDetailOrThrow(licenceScheduleDetail);

    return new TimelineSummaryCardView(
        DateFormatUtil.convertToDisplayText(licenceStartDate.getStartDate()),
        licence.getRoundIssuedOn(),
        licence.getStatus().getDisplayText()
    );
  }

  List<TimelineActionView> getLicenceScheduleTimelineActions(LicenceScheduleDetail licenceScheduleDetail) {
    var licenceType = licenceScheduleDetail.getLicenceSchedule().getLicence().getType();

    var actions = new ArrayList<LicenceScheduleTimelineAction>();

    actions.add(LicenceScheduleTimelineAction.ADD_A_TERM);

    if (licenceTypeFeatureService.arePhasesCaptured(licenceType)) {
      actions.add(LicenceScheduleTimelineAction.ADD_A_PHASE);
    }

    if (licenceTypeFeatureService.hasWorkProgramme(licenceType)) {
      actions.add(LicenceScheduleTimelineAction.ADD_A_WORK_PROGRAMME_ACTIVITY);
    }

    return actions.stream()
        .sorted(Comparator.comparing(LicenceScheduleTimelineAction::getDisplayOrder))
        .map(action -> toTimelineActionView(action, licenceScheduleDetail))
        .toList();
  }

  private TimelineActionView toTimelineActionView(
      LicenceScheduleTimelineAction licenceScheduleTimelineAction,
      LicenceScheduleDetail licenceScheduleDetail
  ) {
    return new TimelineActionView(
        licenceScheduleTimelineAction,
        licenceScheduleTimelineAction.getActionRedirectUrl(licenceScheduleDetail)
    );
  }

  List<TimelineTermView> getLicenceScheduleEventViews(LicenceScheduleDetail licenceScheduleDetail) {
    return licenceScheduleTermService.getActiveTermsByLicenceScheduleDetail(licenceScheduleDetail).stream()
        .sorted(Comparator.comparing(term -> term.getTermType().getDisplayOrder()))
        .map(this::convertToTimelineTermView)
        .toList();
  }

  private TimelineTermView convertToTimelineTermView(LicenceScheduleTerm licenceScheduleTerm) {
    var dateDurationString = "%s to %s (%s)".formatted(
        DateFormatUtil.convertToDisplayText(licenceScheduleTerm.getStartDate()),
        DateFormatUtil.convertToDisplayText(licenceScheduleTerm.getEndDate()),
        ThreeFieldDurationDisplayUtil.convertToDisplayText(licenceScheduleTerm.getTermDuration())
    );

    return new TimelineTermView(
        List.of(),
        licenceScheduleTerm.getTermType(),
        dateDurationString,
        DateFormatUtil.convertToDisplayText(licenceScheduleTerm.getEndDate()),
        ReverseRouter.route(on(LicenceScheduleTermController.class).renderUpdateTermForm(licenceScheduleTerm.getId())),
        ReverseRouter.route(on(LicenceScheduleTermDeletionController.class).renderDeleteTermPage(licenceScheduleTerm.getId()))
    );
  }
}
