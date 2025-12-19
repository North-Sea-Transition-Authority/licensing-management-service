package uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDuration;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDurationDisplayUtil;
import uk.co.nstauthority.licensingmanagementservice.formatting.DateFormatUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.rules.LicenceTypeRulesResolver;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhase;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhaseController;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhaseDeletionController;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhaseService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTermController;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTermDeletionController;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTermService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencestartdate.LicenceStartDateService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivity;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityDateOption;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Service
public class LicenceScheduleTimelineService {

  private final LicenceStartDateService licenceStartDateService;
  private final LicenceTypeRulesResolver licenceTypeRulesResolver;
  private final LicenceScheduleTermService licenceScheduleTermService;
  private final LicenceSchedulePhaseService licenceSchedulePhaseService;
  private final WorkProgrammeActivityService workProgrammeActivityService;

  public LicenceScheduleTimelineService(
      LicenceStartDateService licenceStartDateService,
      LicenceTypeRulesResolver licenceTypeRulesResolver,
      LicenceScheduleTermService licenceScheduleTermService,
      LicenceSchedulePhaseService licenceSchedulePhaseService,
      WorkProgrammeActivityService workProgrammeActivityService
  ) {
    this.licenceStartDateService = licenceStartDateService;
    this.licenceTypeRulesResolver = licenceTypeRulesResolver;
    this.licenceScheduleTermService = licenceScheduleTermService;
    this.licenceSchedulePhaseService = licenceSchedulePhaseService;
    this.workProgrammeActivityService = workProgrammeActivityService;
  }

  public TimelineSummaryCardView getTimelineSummaryCardView(LicenceScheduleDetail licenceScheduleDetail) {
    var licence = licenceScheduleDetail.getLicenceSchedule().getLicence();
    var licenceStartDate = licenceStartDateService.getByLicenceScheduleDetailOrThrow(licenceScheduleDetail);

    return new TimelineSummaryCardView(
        DateFormatUtil.convertToDisplayText(licenceStartDate.getStartDate()),
        licenceTypeRulesResolver.canShowLicenceRoundIssuedOn(licence.getType()),
        licence.getRoundIssuedOn(),
        licence.getStatus().getDisplayText()
    );
  }

  List<TimelineActionView> getLicenceScheduleTimelineActions(LicenceScheduleDetail licenceScheduleDetail) {
    var licenceType = licenceScheduleDetail.getLicenceSchedule().getLicence().getType();

    var actions = new ArrayList<LicenceScheduleTimelineAction>();

    actions.add(LicenceScheduleTimelineAction.ADD_A_TERM);

    if (licenceTypeRulesResolver.arePhasesCaptured(licenceType)) {
      actions.add(LicenceScheduleTimelineAction.ADD_A_PHASE);
    }

    if (licenceTypeRulesResolver.hasWorkProgramme(licenceType)) {
      actions.add(LicenceScheduleTimelineAction.ADD_A_WORK_PROGRAMME_ACTIVITY);
    }

    if (licenceTypeRulesResolver.hasRentalRate(licenceType)) {
      actions.add(LicenceScheduleTimelineAction.ADD_A_RATE);
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
    var dateDurationString = getDateDurationString(
        licenceScheduleTerm.getStartDate(),
        licenceScheduleTerm.getEndDate(),
        licenceScheduleTerm.getTermDuration()
    );

    var scheduleEvents = getScheduleEventsForTerm(licenceScheduleTerm);

    var hasPhases = scheduleEvents.stream()
        .map(ScheduleEvent::getEventType)
        .anyMatch(eventType -> eventType.equals(ScheduleEventType.PHASE));

    var endOfTermEvents = getEndOfTermRequirementEvents(licenceScheduleTerm);

    return new TimelineTermView(
        scheduleEvents,
        endOfTermEvents,
        licenceScheduleTerm.getTermType(),
        dateDurationString,
        DateFormatUtil.convertToDisplayText(licenceScheduleTerm.getEndDate()),
        ReverseRouter.route(on(LicenceScheduleTermController.class).renderUpdateTermForm(licenceScheduleTerm.getId())),
        ReverseRouter.route(on(LicenceScheduleTermDeletionController.class).renderDeleteTermPage(licenceScheduleTerm.getId())),
        hasPhases
    );
  }

  private List<ScheduleEvent> getScheduleEventsForTerm(LicenceScheduleTerm licenceScheduleTerm) {
    var phaseViews = licenceSchedulePhaseService.getActivePhasesByTerm(licenceScheduleTerm).stream()
        .sorted(Comparator.comparing(phase -> phase.getPhaseType().getDisplayOrder()))
        .map(this::convertToTimelinePhaseView)
        .toList();

    if (!phaseViews.isEmpty()) {
      return phaseViews;
    }

    return workProgrammeActivityService.getWorkProgrammeActivitiesByDateRange(
        licenceScheduleTerm.getLicenceScheduleDetail(),
        licenceScheduleTerm.getStartDate(),
        licenceScheduleTerm.getEndDate()
    ).stream()
        .sorted(
            Comparator.comparing(WorkProgrammeActivity::getDueDate)
            .thenComparing(WorkProgrammeActivity::getCategoryString)
        )
        .map(this::convertToWorkProgrammeActivityView)
        .toList();
  }

  private List<ScheduleEvent> getEndOfTermRequirementEvents(LicenceScheduleTerm licenceScheduleTerm) {
    return workProgrammeActivityService.getWorkProgrammeActivitiesByTermAndDateOption(
        licenceScheduleTerm,
        WorkProgrammeActivityDateOption.WITHIN_A_TERM
    ).stream()
        .sorted(Comparator.comparing(WorkProgrammeActivity::getCategoryString))
        .map(this::convertToWorkProgrammeActivityView)
        .toList();
  }

  private ScheduleEvent convertToTimelinePhaseView(LicenceSchedulePhase licenceSchedulePhase) {
    var dateDurationString = getDateDurationString(
        licenceSchedulePhase.getStartDate(),
        licenceSchedulePhase.getEndDate(),
        licenceSchedulePhase.getPhaseDuration()
    );

    return new TimelinePhaseView(
        getScheduleEventsForPhase(licenceSchedulePhase),
        getEndOfPhaseRequirementEvents(licenceSchedulePhase),
        licenceSchedulePhase.getPhaseType(),
        dateDurationString,
        DateFormatUtil.convertToDisplayText(licenceSchedulePhase.getEndDate()),
        ReverseRouter.route(on(LicenceSchedulePhaseController.class).renderUpdatePhaseForm(licenceSchedulePhase.getId())),
        ReverseRouter.route(on(LicenceSchedulePhaseDeletionController.class).renderDeletePhasePage(licenceSchedulePhase.getId()))
    );
  }

  private List<ScheduleEvent> getScheduleEventsForPhase(LicenceSchedulePhase licenceSchedulePhase) {
    return workProgrammeActivityService.getWorkProgrammeActivitiesByDateRange(
            licenceSchedulePhase.getLicenceScheduleDetail(),
            licenceSchedulePhase.getStartDate(),
            licenceSchedulePhase.getEndDate()
        ).stream()
        .sorted(
            Comparator.comparing(WorkProgrammeActivity::getDueDate)
                .thenComparing(WorkProgrammeActivity::getCategoryString)
        )
        .map(this::convertToWorkProgrammeActivityView)
        .toList();
  }

  private List<ScheduleEvent> getEndOfPhaseRequirementEvents(LicenceSchedulePhase licenceSchedulePhase) {
    return workProgrammeActivityService.getWorkProgrammeActivitiesByPhaseAndDateOption(
            licenceSchedulePhase,
            WorkProgrammeActivityDateOption.WITHIN_A_PHASE
        ).stream()
        .sorted(Comparator.comparing(WorkProgrammeActivity::getCategoryString))
        .map(this::convertToWorkProgrammeActivityView)
        .toList();
  }

  private ScheduleEvent convertToWorkProgrammeActivityView(WorkProgrammeActivity workProgrammeActivity) {
    var dueDateString = workProgrammeActivity.getDueDate() != null
        ? DateFormatUtil.convertToDisplayText(workProgrammeActivity.getDueDate())
        : "";

    return new TimelineWorkProgrammeActivityView(
        workProgrammeActivity.getCategoryString(),
        workProgrammeActivity.getDescription(),
        dueDateString,
        "",
        ""
    );
  }

  private String getDateDurationString(
      LocalDate startDate,
      LocalDate endDate,
      ThreeFieldDuration duration
  ) {
    return "%s to %s (%s)".formatted(
        DateFormatUtil.convertToDisplayText(startDate),
        DateFormatUtil.convertToDisplayText(endDate),
        ThreeFieldDurationDisplayUtil.convertToDisplayText(duration)
    );
  }
}
