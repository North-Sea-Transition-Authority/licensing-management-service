package uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDuration;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDurationDisplayUtil;
import uk.co.nstauthority.licensingmanagementservice.formatting.DateFormatUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.PhaseType;
import uk.co.nstauthority.licensingmanagementservice.licence.rules.LicenceTypeRulesResolver;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleexpiry.LicenceScheduleExpiry;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleexpiry.LicenceScheduleExpiryService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhase;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhaseController;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhaseDeletionController;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhaseService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate.LicenceScheduleRateService;
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
  private final LicenceScheduleRateService licenceScheduleRateService;
  private final LicenceScheduleExpiryService licenceScheduleExpiryService;

  public LicenceScheduleTimelineService(
      LicenceStartDateService licenceStartDateService,
      LicenceTypeRulesResolver licenceTypeRulesResolver,
      LicenceScheduleTermService licenceScheduleTermService,
      LicenceSchedulePhaseService licenceSchedulePhaseService,
      WorkProgrammeActivityService workProgrammeActivityService,
      LicenceScheduleRateService licenceScheduleRateService,
      LicenceScheduleExpiryService licenceScheduleExpiryService
  ) {
    this.licenceStartDateService = licenceStartDateService;
    this.licenceTypeRulesResolver = licenceTypeRulesResolver;
    this.licenceScheduleTermService = licenceScheduleTermService;
    this.licenceSchedulePhaseService = licenceSchedulePhaseService;
    this.workProgrammeActivityService = workProgrammeActivityService;
    this.licenceScheduleRateService = licenceScheduleRateService;
    this.licenceScheduleExpiryService = licenceScheduleExpiryService;
  }

  public TimelineSummaryCardView getTimelineSummaryCardView(LicenceScheduleDetail licenceScheduleDetail) {
    var licence = licenceScheduleDetail.getLicenceSchedule().getLicence();
    var licenceStartDate = licenceStartDateService.getByLicenceScheduleDetailOrThrow(licenceScheduleDetail);
    var licenceExpiryDateString = licenceScheduleExpiryService.getExpiryForLicenceScheduleDetail(licenceScheduleDetail)
        .map(this::getExpiryDateString)
        .orElse("");

    return new TimelineSummaryCardView(
        DateFormatUtil.convertToDisplayText(licenceStartDate.getStartDate()),
        licenceExpiryDateString,
        licenceTypeRulesResolver.canShowLicenceRoundIssuedOn(licence.getType()),
        licence.getRoundIssuedOn(),
        licence.getStatus().getDisplayName()
    );
  }

  private String getExpiryDateString(LicenceScheduleExpiry licenceScheduleExpiry) {
    if (licenceScheduleExpiry.getExpiryDate() == null) {
      return "";
    }

    return DateFormatUtil.convertToDisplayText(licenceScheduleExpiry.getExpiryDate());
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
    var phases =  licenceSchedulePhaseService.getActivePhasesByTerm(licenceScheduleTerm);

    var firstPhaseType = phases.stream()
        .min(Comparator.comparing(phase -> phase.getPhaseType().getDisplayOrder()))
        .map(LicenceSchedulePhase::getPhaseType)
        .orElse(null);

    var phaseViews = phases.stream()
        .sorted(Comparator.comparing(phase -> phase.getPhaseType().getDisplayOrder()))
        .map(phase -> convertToTimelinePhaseView(phase, firstPhaseType))
        .toList();

    if (!phaseViews.isEmpty()) {
      var termRateViews = licenceScheduleRateService.getActiveLicenceScheduleRatesAttachedToTerm(licenceScheduleTerm).stream()
          .map(TimelineRateView::getScheduleEventFrom)
          .toList();

      return Stream.concat(phaseViews.stream(), termRateViews.stream())
          .sorted(Comparator.comparing(ScheduleEvent::getSortingDate)
              .thenComparing(event -> event.getEventType().getEventTypeOrder()))
          .toList();
    }

    var workProgrammeActivities = workProgrammeActivityService
        .getActiveWorkProgrammeActivitiesByDateRangeFor(licenceScheduleTerm).stream()
        .sorted(
            Comparator.comparing(WorkProgrammeActivity::getDueDate)
            .thenComparing(WorkProgrammeActivity::getCategoryString))
        .map(TimelineWorkProgrammeActivityView::getScheduleEventFrom);

    var rates = licenceScheduleRateService.getActiveLicenceScheduleRatesByTerm(licenceScheduleTerm)
        .stream()
        .map(TimelineRateView::getScheduleEventFrom);

    return Stream.of(workProgrammeActivities, rates)
        .flatMap(Function.identity())
        .sorted(Comparator.comparing(ScheduleEvent::getSortingDate)
            .thenComparing(event -> event.getEventType().getEventTypeOrder()))
        .toList();
  }

  private List<ScheduleEvent> getEndOfTermRequirementEvents(LicenceScheduleTerm licenceScheduleTerm) {
    return workProgrammeActivityService.getActiveWorkProgrammeActivitiesByTermAndDateOption(
        licenceScheduleTerm,
        WorkProgrammeActivityDateOption.WITHIN_A_TERM
    ).stream()
        .sorted(Comparator.comparing(WorkProgrammeActivity::getCategoryString))
        .map(TimelineWorkProgrammeActivityView::getScheduleEventFrom)
        .toList();
  }

  private ScheduleEvent convertToTimelinePhaseView(LicenceSchedulePhase licenceSchedulePhase, PhaseType firstPhaseType) {
    var dateDurationString = getDateDurationString(
        licenceSchedulePhase.getStartDate(),
        licenceSchedulePhase.getEndDate(),
        licenceSchedulePhase.getPhaseDuration()
    );

    return new TimelinePhaseView(
        getScheduleEventsForPhase(licenceSchedulePhase, firstPhaseType),
        getEndOfPhaseRequirementEvents(licenceSchedulePhase),
        licenceSchedulePhase.getPhaseType(),
        licenceSchedulePhase.getStartDate(),
        dateDurationString,
        DateFormatUtil.convertToDisplayText(licenceSchedulePhase.getEndDate()),
        ReverseRouter.route(on(LicenceSchedulePhaseController.class).renderUpdatePhaseForm(licenceSchedulePhase.getId())),
        ReverseRouter.route(on(LicenceSchedulePhaseDeletionController.class).renderDeletePhasePage(licenceSchedulePhase.getId()))
    );
  }

  private List<ScheduleEvent> getScheduleEventsForPhase(
      LicenceSchedulePhase licenceSchedulePhase,
      PhaseType firstPhaseType
  ) {
    var workProgrammeActivities = workProgrammeActivityService
        .getActiveWorkProgrammeActivitiesByDateRangeFor(licenceSchedulePhase).stream()
        .sorted(
            Comparator.comparing(WorkProgrammeActivity::getDueDate)
                .thenComparing(WorkProgrammeActivity::getCategoryString)
        )
        .map(TimelineWorkProgrammeActivityView::getScheduleEventFrom);

    var rates = licenceScheduleRateService.getActiveLicenceScheduleRatesByPhase(licenceSchedulePhase, firstPhaseType).stream()
        .map(TimelineRateView::getScheduleEventFrom);

    return Stream.of(workProgrammeActivities, rates)
        .flatMap(Function.identity())
        .sorted(Comparator.comparing(ScheduleEvent::getSortingDate)
            .thenComparing(event -> event.getEventType().getEventTypeOrder()))
        .toList();
  }

  private List<ScheduleEvent> getEndOfPhaseRequirementEvents(LicenceSchedulePhase licenceSchedulePhase) {
    return workProgrammeActivityService.getActiveWorkProgrammeActivitiesByPhaseAndDateOption(
            licenceSchedulePhase,
            WorkProgrammeActivityDateOption.WITHIN_A_PHASE
        ).stream()
        .sorted(Comparator.comparing(WorkProgrammeActivity::getCategoryString))
        .map(TimelineWorkProgrammeActivityView::getScheduleEventFrom)
        .toList();
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
