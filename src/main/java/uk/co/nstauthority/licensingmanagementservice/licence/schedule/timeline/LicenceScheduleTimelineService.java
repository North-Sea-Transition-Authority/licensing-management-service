package uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDuration;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDurationDisplayUtil;
import uk.co.nstauthority.licensingmanagementservice.formatting.DateFormatUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.PhaseType;
import uk.co.nstauthority.licensingmanagementservice.licence.rules.LicenceTypeRulesResolver;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventcomments.EventCommentController;
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
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent.OtherScheduleEvent;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent.OtherScheduleEventDateOption;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent.OtherScheduleEventService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivity;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityDateOption;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.status.WorkProgrammeActivityStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.status.WorkProgrammeActivityStatusService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamQueryService;

@Service
public class LicenceScheduleTimelineService {

  private final LicenceStartDateService licenceStartDateService;
  private final LicenceTypeRulesResolver licenceTypeRulesResolver;
  private final LicenceScheduleTermService licenceScheduleTermService;
  private final LicenceSchedulePhaseService licenceSchedulePhaseService;
  private final WorkProgrammeActivityService workProgrammeActivityService;
  private final LicenceScheduleRateService licenceScheduleRateService;
  private final LicenceScheduleExpiryService licenceScheduleExpiryService;
  private final OtherScheduleEventService otherScheduleEventService;
  private final TeamQueryService teamQueryService;
  private final WorkProgrammeActivityStatusService workProgrammeActivityStatusService;

  public LicenceScheduleTimelineService(
      LicenceStartDateService licenceStartDateService,
      LicenceTypeRulesResolver licenceTypeRulesResolver,
      LicenceScheduleTermService licenceScheduleTermService,
      LicenceSchedulePhaseService licenceSchedulePhaseService,
      WorkProgrammeActivityService workProgrammeActivityService,
      LicenceScheduleRateService licenceScheduleRateService,
      LicenceScheduleExpiryService licenceScheduleExpiryService,
      OtherScheduleEventService otherScheduleEventService,
      TeamQueryService teamQueryService,
      WorkProgrammeActivityStatusService workProgrammeActivityStatusService
  ) {
    this.licenceStartDateService = licenceStartDateService;
    this.licenceTypeRulesResolver = licenceTypeRulesResolver;
    this.licenceScheduleTermService = licenceScheduleTermService;
    this.licenceSchedulePhaseService = licenceSchedulePhaseService;
    this.workProgrammeActivityService = workProgrammeActivityService;
    this.licenceScheduleRateService = licenceScheduleRateService;
    this.licenceScheduleExpiryService = licenceScheduleExpiryService;
    this.otherScheduleEventService = otherScheduleEventService;
    this.teamQueryService = teamQueryService;
    this.workProgrammeActivityStatusService = workProgrammeActivityStatusService;
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

  List<TimelineActionView> getLicenceScheduleTimelineActions(
      LicenceScheduleDetail licenceScheduleDetail,
      List<ScheduleEventAction> allowedActions
  ) {
    var licenceType = licenceScheduleDetail.getLicenceSchedule().getLicence().getType();
    var actions = new ArrayList<LicenceScheduleTimelineAction>();

    var canEditScheduleEvents = allowedActions.contains(ScheduleEventAction.EDIT_SCHEDULE_EVENTS);

    if (canEditScheduleEvents) {
      actions.add(LicenceScheduleTimelineAction.ADD_A_TERM);
    }

    if (licenceTypeRulesResolver.arePhasesCaptured(licenceType) && canEditScheduleEvents) {
      actions.add(LicenceScheduleTimelineAction.ADD_A_PHASE);
    }

    if (licenceTypeRulesResolver.hasWorkProgramme(licenceType)
        && allowedActions.contains(ScheduleEventAction.EDIT_WORK_PROGRAMME)) {
      actions.add(LicenceScheduleTimelineAction.ADD_A_WORK_PROGRAMME_ACTIVITY);
    }

    if (licenceTypeRulesResolver.hasRentalRate(licenceType) && canEditScheduleEvents) {
      actions.add(LicenceScheduleTimelineAction.ADD_A_RATE);
    }

    if (canEditScheduleEvents) {
      actions.add(LicenceScheduleTimelineAction.ADD_A_SCHEDULE_EVENT);
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

  public List<TimelineTermView> getLicenceScheduleEventViewsForOverview(
      LicenceScheduleDetail licenceScheduleDetail,
      TimelineFilterForm timelineFilterForm,
      ServiceUserDetail userDetail
  ) {
    Map<UUID, WorkProgrammeActivityStatus> eventRefWpStatusMap = new HashMap<>();
    List<ScheduleEventAction> eventActions = new ArrayList<>();

    if (teamQueryService.userIsInRegulatorTeam(userDetail.wuaId())) {
      eventRefWpStatusMap.putAll(getLatestWpStatusesForSchedule(licenceScheduleDetail));

      if (teamQueryService.userHasAtLeastOneRoleIn(userDetail.wuaId(), Set.of(Role.SCHEDULE_ADMINISTRATOR))) {
        eventActions.add(ScheduleEventAction.ADD_SCHEDULE_COMMENT);
      }

      var updateWorkProgrammeStatusRoles = Set.of(
          Role.WORK_PROGRAMME_ADMINISTRATOR,
          Role.WORK_PROGRAMME_STATUS_ADMINISTRATOR
      );

      if (teamQueryService.userHasAtLeastOneRoleIn(userDetail.wuaId(), updateWorkProgrammeStatusRoles)) {
        eventActions.add(ScheduleEventAction.EDIT_WORK_PROGRAMME_STATUS);
        eventActions.add(ScheduleEventAction.ADD_WORK_PROGRAMME_COMMENT);
      }
    }

    return getLicenceScheduleEventViews(
        licenceScheduleDetail,
        timelineFilterForm,
        eventActions,
        eventRefWpStatusMap
    );
  }

  List<TimelineTermView> getEditableLicenceScheduleEventViews(
      LicenceScheduleDetail licenceScheduleDetail,
      TimelineFilterForm timelineFilterForm,
      List<ScheduleEventAction> allowedActions
  ) {
    var eventRefWpStatusMap = getLatestWpStatusesForSchedule(licenceScheduleDetail);

    return getLicenceScheduleEventViews(
        licenceScheduleDetail,
        timelineFilterForm,
        allowedActions,
        eventRefWpStatusMap
    );
  }

  public List<ScheduleEventAction> getAllowedEventActionsForUser(ServiceUserDetail userDetail) {
    List<ScheduleEventAction> allowedActions = new ArrayList<>();

    if (teamQueryService.userHasAtLeastOneRoleIn(userDetail.wuaId(), Set.of(Role.SCHEDULE_ADMINISTRATOR))) {
      allowedActions.add(ScheduleEventAction.EDIT_SCHEDULE_EVENTS);
    }

    if (teamQueryService.userHasAtLeastOneRoleIn(userDetail.wuaId(), Set.of(Role.WORK_PROGRAMME_ADMINISTRATOR))) {
      allowedActions.add(ScheduleEventAction.EDIT_WORK_PROGRAMME);
    }

    return allowedActions;
  }

  private List<TimelineTermView> getLicenceScheduleEventViews(
      LicenceScheduleDetail licenceScheduleDetail,
      TimelineFilterForm timelineFilterForm,
      List<ScheduleEventAction> allowedActions,
      Map<UUID, WorkProgrammeActivityStatus> eventRefWpStatusMap
  ) {
    var includedEventTypes = timelineFilterForm.getEventTypes().stream()
        .map(ScheduleEventType::valueOf)
        .toList();

    return licenceScheduleTermService.getActiveTermsByLicenceScheduleDetail(licenceScheduleDetail).stream()
        .sorted(Comparator.comparing(term -> term.getTermType().getDisplayOrder()))
        .map(term -> convertToTimelineTermView(term, includedEventTypes, allowedActions, eventRefWpStatusMap))
        .toList();
  }

  private TimelineTermView convertToTimelineTermView(
      LicenceScheduleTerm licenceScheduleTerm,
      List<ScheduleEventType> includedEventTypes,
      List<ScheduleEventAction> allowedActions,
      Map<UUID, WorkProgrammeActivityStatus> eventRefWpStatusMap
  ) {
    var dateDurationString = getDateDurationString(
        licenceScheduleTerm.getStartDate(),
        licenceScheduleTerm.getEndDate(),
        licenceScheduleTerm.getTermDuration()
    );

    var scheduleEvents = getScheduleEventsForTerm(
        licenceScheduleTerm, 
        includedEventTypes, 
        allowedActions, 
        eventRefWpStatusMap
    );

    var hasPhases = scheduleEvents.stream()
        .map(ScheduleEvent::getEventType)
        .anyMatch(eventType -> eventType.equals(ScheduleEventType.PHASE));

    var endOfTermEvents = getEndOfTermRequirementEvents(
        licenceScheduleTerm,
        includedEventTypes,
        allowedActions, 
        eventRefWpStatusMap
    );

    var editUrl = allowedActions.contains(ScheduleEventAction.EDIT_SCHEDULE_EVENTS)
        ? ReverseRouter.route(on(LicenceScheduleTermController.class).renderUpdateTermForm(licenceScheduleTerm.getId()))
        : "";

    var deleteUrl = allowedActions.contains(ScheduleEventAction.EDIT_SCHEDULE_EVENTS)
        ? ReverseRouter.route(on(LicenceScheduleTermDeletionController.class).renderDeleteTermPage(licenceScheduleTerm.getId()))
        : "";

    var addCommentUrl = allowedActions.contains(ScheduleEventAction.ADD_SCHEDULE_COMMENT)
        ? ReverseRouter.route(on(EventCommentController.class)
          .renderAddCommentForm(ScheduleEventType.TERM.getUrlSlug(), licenceScheduleTerm.getEventReference().getId()))
        : "";

    return new TimelineTermView(
        scheduleEvents,
        endOfTermEvents,
        licenceScheduleTerm.getTermType(),
        dateDurationString,
        DateFormatUtil.convertToDisplayText(licenceScheduleTerm.getEndDate()),
        editUrl,
        deleteUrl,
        addCommentUrl,
        hasPhases
    );
  }

  private List<ScheduleEvent> getScheduleEventsForTerm(
      LicenceScheduleTerm licenceScheduleTerm,
      List<ScheduleEventType> includedEventTypes,
      List<ScheduleEventAction> allowedActions,
      Map<UUID, WorkProgrammeActivityStatus> eventRefWpStatusMap
  ) {
    var phases = licenceSchedulePhaseService.getActivePhasesByTerm(licenceScheduleTerm);

    var firstPhaseType = phases.stream()
        .min(Comparator.comparing(phase -> phase.getPhaseType().getDisplayOrder()))
        .map(LicenceSchedulePhase::getPhaseType)
        .orElse(null);

    var phaseViews = phases.stream()
        .sorted(Comparator.comparing(phase -> phase.getPhaseType().getDisplayOrder()))
        .map(phase -> convertToTimelinePhaseView(phase, firstPhaseType, includedEventTypes, allowedActions, eventRefWpStatusMap))
        .toList();

    if (!phaseViews.isEmpty()) {
      var termRateViews = licenceScheduleRateService.getActiveLicenceScheduleRatesAttachedToTerm(licenceScheduleTerm).stream()
          .map(licenceScheduleRate -> TimelineRateView.getScheduleEventFrom(licenceScheduleRate, allowedActions))
          .filter(event -> includedEventTypes.contains(event.getEventType()))
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
        .map(workProgrammeActivity ->
            TimelineWorkProgrammeActivityView.getScheduleEventFrom(workProgrammeActivity, allowedActions, eventRefWpStatusMap));

    var rates = licenceScheduleRateService.getActiveLicenceScheduleRatesByTerm(licenceScheduleTerm)
        .stream()
        .map(licenceScheduleRate -> TimelineRateView.getScheduleEventFrom(licenceScheduleRate, allowedActions));

    var otherScheduleEvents = otherScheduleEventService
        .getActiveScheduleEventsByDateRangeFor(licenceScheduleTerm).stream()
        .sorted(
            Comparator.comparing(OtherScheduleEvent::getEventDate)
                .thenComparing(OtherScheduleEvent::getCategoryString))
        .map(otherScheduleEvent ->
            TimelineOtherScheduleEventView.getScheduleEventFrom(otherScheduleEvent, allowedActions));

    return Stream.of(workProgrammeActivities, rates, otherScheduleEvents)
        .flatMap(Function.identity())
        .filter(event -> includedEventTypes.contains(event.getEventType()))
        .sorted(Comparator.comparing(ScheduleEvent::getSortingDate)
            .thenComparing(event -> event.getEventType().getEventTypeOrder()))
        .toList();
  }

  private List<ScheduleEvent> getEndOfTermRequirementEvents(
      LicenceScheduleTerm licenceScheduleTerm,
      List<ScheduleEventType> includedEventTypes,
      List<ScheduleEventAction> allowedActions,
      Map<UUID, WorkProgrammeActivityStatus> eventRefWpStatusMap
  ) {
    var workProgrammeActivities = workProgrammeActivityService.getActiveWorkProgrammeActivitiesByTermAndDateOption(
        licenceScheduleTerm,
        WorkProgrammeActivityDateOption.WITHIN_A_TERM
    ).stream()
        .sorted(Comparator.comparing(WorkProgrammeActivity::getCategoryString))
        .map(workProgrammeActivity ->
            TimelineWorkProgrammeActivityView.getScheduleEventFrom(workProgrammeActivity, allowedActions, eventRefWpStatusMap));

    var otherScheduleEvents = otherScheduleEventService.getActiveScheduleEventsByTermAndDateOption(
        licenceScheduleTerm,
        OtherScheduleEventDateOption.WITHIN_A_TERM
    ).stream()
        .sorted(Comparator.comparing(OtherScheduleEvent::getCategoryString))
        .map(otherScheduleEvent ->
            TimelineOtherScheduleEventView.getScheduleEventFrom(otherScheduleEvent, allowedActions));

    return Stream.of(workProgrammeActivities, otherScheduleEvents)
        .flatMap(Function.identity())
        .filter(event -> includedEventTypes.contains(event.getEventType()))
        .sorted(Comparator.comparing(event -> event.getEventType().getEventTypeOrder()))
        .toList();
  }

  private ScheduleEvent convertToTimelinePhaseView(
      LicenceSchedulePhase licenceSchedulePhase,
      PhaseType firstPhaseType,
      List<ScheduleEventType> includedEventTypes,
      List<ScheduleEventAction> allowedActions,
      Map<UUID, WorkProgrammeActivityStatus> eventRefWpStatusMap
  ) {
    var dateDurationString = getDateDurationString(
        licenceSchedulePhase.getStartDate(),
        licenceSchedulePhase.getEndDate(),
        licenceSchedulePhase.getPhaseDuration()
    );

    var editUrl = allowedActions.contains(ScheduleEventAction.EDIT_SCHEDULE_EVENTS)
        ? ReverseRouter.route(on(LicenceSchedulePhaseController.class).renderUpdatePhaseForm(licenceSchedulePhase.getId()))
        : "";

    var deleteUrl = allowedActions.contains(ScheduleEventAction.EDIT_SCHEDULE_EVENTS)
        ? ReverseRouter.route(on(LicenceSchedulePhaseDeletionController.class)
          .renderDeletePhasePage(licenceSchedulePhase.getId()))
        : "";

    var addCommentUrl = allowedActions.contains(ScheduleEventAction.ADD_SCHEDULE_COMMENT)
        ? ReverseRouter.route(on(EventCommentController.class)
          .renderAddCommentForm(ScheduleEventType.PHASE.getUrlSlug(), licenceSchedulePhase.getEventReference().getId()))
        : "";

    return new TimelinePhaseView(
        getScheduleEventsForPhase(licenceSchedulePhase, firstPhaseType, includedEventTypes, allowedActions, eventRefWpStatusMap),
        getEndOfPhaseRequirementEvents(licenceSchedulePhase, includedEventTypes, allowedActions, eventRefWpStatusMap),
        licenceSchedulePhase.getPhaseType(),
        licenceSchedulePhase.getStartDate(),
        dateDurationString,
        DateFormatUtil.convertToDisplayText(licenceSchedulePhase.getEndDate()),
        editUrl,
        deleteUrl,
        addCommentUrl
    );
  }

  private List<ScheduleEvent> getScheduleEventsForPhase(
      LicenceSchedulePhase licenceSchedulePhase,
      PhaseType firstPhaseType,
      List<ScheduleEventType> includedEventTypes,
      List<ScheduleEventAction> allowedActions,
      Map<UUID, WorkProgrammeActivityStatus> eventRefWpStatusMap
  ) {
    var workProgrammeActivities = workProgrammeActivityService
        .getActiveWorkProgrammeActivitiesByDateRangeFor(licenceSchedulePhase).stream()
        .sorted(
            Comparator.comparing(WorkProgrammeActivity::getDueDate)
                .thenComparing(WorkProgrammeActivity::getCategoryString)
        )
        .map(workProgrammeActivity ->
            TimelineWorkProgrammeActivityView.getScheduleEventFrom(workProgrammeActivity, allowedActions, eventRefWpStatusMap));

    var rates = licenceScheduleRateService.getActiveLicenceScheduleRatesByPhase(licenceSchedulePhase, firstPhaseType).stream()
        .map(licenceScheduleRate -> TimelineRateView.getScheduleEventFrom(licenceScheduleRate, allowedActions));

    var otherScheduleEvents = otherScheduleEventService
        .getActiveScheduleEventsByDateRangeFor(licenceSchedulePhase).stream()
        .sorted(
            Comparator.comparing(OtherScheduleEvent::getEventDate)
                .thenComparing(OtherScheduleEvent::getCategoryString))
        .map(otherScheduleEvent ->
            TimelineOtherScheduleEventView.getScheduleEventFrom(otherScheduleEvent, allowedActions));

    return Stream.of(workProgrammeActivities, rates, otherScheduleEvents)
        .flatMap(Function.identity())
        .filter(event -> includedEventTypes.contains(event.getEventType()))
        .sorted(Comparator.comparing(ScheduleEvent::getSortingDate)
            .thenComparing(event -> event.getEventType().getEventTypeOrder()))
        .toList();
  }

  private List<ScheduleEvent> getEndOfPhaseRequirementEvents(
      LicenceSchedulePhase licenceSchedulePhase,
      List<ScheduleEventType> includedEventTypes,
      List<ScheduleEventAction> allowedActions,
      Map<UUID, WorkProgrammeActivityStatus> eventRefWpStatusMap
  ) {
    var workProgrammeActivities = workProgrammeActivityService.getActiveWorkProgrammeActivitiesByPhaseAndDateOption(
            licenceSchedulePhase,
            WorkProgrammeActivityDateOption.WITHIN_A_PHASE
        ).stream()
        .sorted(Comparator.comparing(WorkProgrammeActivity::getCategoryString))
        .map(workProgrammeActivity ->
            TimelineWorkProgrammeActivityView.getScheduleEventFrom(workProgrammeActivity, allowedActions, eventRefWpStatusMap));

    var otherScheduleEvents = otherScheduleEventService.getActiveScheduleEventsByPhaseAndDateOption(
            licenceSchedulePhase,
            OtherScheduleEventDateOption.WITHIN_A_PHASE
        ).stream()
        .sorted(Comparator.comparing(OtherScheduleEvent::getCategoryString))
        .map(otherScheduleEvent ->
            TimelineOtherScheduleEventView.getScheduleEventFrom(otherScheduleEvent, allowedActions));

    return Stream.of(workProgrammeActivities, otherScheduleEvents)
        .flatMap(Function.identity())
        .filter(event -> includedEventTypes.contains(event.getEventType()))
        .sorted(Comparator.comparing(event -> event.getEventType().getEventTypeOrder()))
        .toList();
  }

  public List<ScheduleEvent> getEventsBeyondFinalTerm(
      LicenceScheduleDetail licenceScheduleDetail,
      List<ScheduleEventAction> allowedActions
  ) {
    var finalTerm = licenceScheduleTermService.getActiveTermsByLicenceScheduleDetail(licenceScheduleDetail).stream()
        .max(Comparator.comparing(term -> term.getTermType().getDisplayOrder()));

    if (finalTerm.isEmpty()) {
      return List.of();
    }

    var finalTermEndDate = finalTerm.get().getEndDate();

    var eventRefWpStatusMap = getLatestWpStatusesForSchedule(licenceScheduleDetail);
    
    var workProgrammeActivities = workProgrammeActivityService.getActiveWorkProgrammeActivitiesAfterDate(
        licenceScheduleDetail,
        finalTermEndDate
    )
        .stream()
        .map(activity -> TimelineWorkProgrammeActivityView.getScheduleEventFrom(activity, allowedActions, eventRefWpStatusMap));

    var rates = licenceScheduleRateService.getActiveRatesAfterDate(
        licenceScheduleDetail,
        finalTermEndDate
    )
        .stream()
        .map(rate -> TimelineRateView.getScheduleEventFrom(rate, allowedActions));

    var otherScheduleEvents = otherScheduleEventService.getActiveEventsAfterDate(
        licenceScheduleDetail,
        finalTermEndDate
    )
        .stream()
        .map(event -> TimelineOtherScheduleEventView.getScheduleEventFrom(event, allowedActions));

    return Stream.of(workProgrammeActivities, rates, otherScheduleEvents)
        .flatMap(Function.identity())
        .sorted(Comparator.comparing(ScheduleEvent::getSortingDate)
            .thenComparing(event -> event.getEventType().getEventTypeOrder()))
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

  private Map<UUID, WorkProgrammeActivityStatus> getLatestWpStatusesForSchedule(LicenceScheduleDetail licenceScheduleDetail) {
    return workProgrammeActivityStatusService.getLatestStatusesFor(
        workProgrammeActivityService.getActiveWorkProgrammeActivities(licenceScheduleDetail)
    );
  }
}
