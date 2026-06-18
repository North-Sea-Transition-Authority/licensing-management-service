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
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.calculation.LicenceScheduleCalculationService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.calculation.StartEndDates;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventcomments.EventCommentController;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventcomments.EventCommentService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventcomments.EventCommentView;
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
  private final EventCommentService eventCommentService;
  private final LicenceScheduleCalculationService licenceScheduleCalculationService;

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
      WorkProgrammeActivityStatusService workProgrammeActivityStatusService,
      EventCommentService eventCommentService,
      LicenceScheduleCalculationService licenceScheduleCalculationService
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
    this.eventCommentService = eventCommentService;
    this.licenceScheduleCalculationService = licenceScheduleCalculationService;
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
    Map<UUID, List<EventCommentView>> eventRefCommentsMap = new HashMap<>();
    List<ScheduleEventAction> eventActions = new ArrayList<>();

    if (teamQueryService.userIsInRegulatorTeam(userDetail.wuaId())) {
      eventRefWpStatusMap.putAll(getLatestWpStatusesForSchedule(licenceScheduleDetail));
      eventRefCommentsMap.putAll(eventCommentService.getEventCommentViewsForSchedule(licenceScheduleDetail.getLicenceSchedule()));

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
        eventRefWpStatusMap,
        eventRefCommentsMap
    );
  }

  List<TimelineTermView> getEditableLicenceScheduleEventViews(
      LicenceScheduleDetail licenceScheduleDetail,
      TimelineFilterForm timelineFilterForm,
      List<ScheduleEventAction> allowedActions
  ) {
    var eventRefWpStatusMap = getLatestWpStatusesForSchedule(licenceScheduleDetail);
    var eventRefCommentsMap = eventCommentService.getEventCommentViewsForSchedule(licenceScheduleDetail.getLicenceSchedule());

    return getLicenceScheduleEventViews(
        licenceScheduleDetail,
        timelineFilterForm,
        allowedActions,
        eventRefWpStatusMap,
        eventRefCommentsMap
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
      Map<UUID, WorkProgrammeActivityStatus> eventRefWpStatusMap,
      Map<UUID, List<EventCommentView>> eventRefCommentsMap
  ) {
    var includedEventTypes = timelineFilterForm.getEventTypes().stream()
        .map(ScheduleEventType::valueOf)
        .toList();

    var rateDatesMap = licenceScheduleCalculationService.calculateRateEndDatesForDisplay(licenceScheduleDetail);

    return licenceScheduleTermService.getActiveTermsByLicenceScheduleDetail(licenceScheduleDetail).stream()
        .sorted(Comparator.comparing(term -> term.getTermType().getDisplayOrder()))
        .map(term -> convertToTimelineTermView(
            term,
            includedEventTypes,
            allowedActions,
            eventRefWpStatusMap,
            eventRefCommentsMap,
            rateDatesMap
        ))
        .toList();
  }

  private TimelineTermView convertToTimelineTermView(
      LicenceScheduleTerm licenceScheduleTerm,
      List<ScheduleEventType> includedEventTypes,
      List<ScheduleEventAction> allowedActions,
      Map<UUID, WorkProgrammeActivityStatus> eventRefWpStatusMap,
      Map<UUID, List<EventCommentView>> eventRefCommentsMap,
      Map<UUID, StartEndDates> rateDatesMap
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
        eventRefWpStatusMap,
        eventRefCommentsMap,
        rateDatesMap
    );

    var hasPhases = scheduleEvents.stream()
        .map(ScheduleEvent::getEventType)
        .anyMatch(eventType -> eventType.equals(ScheduleEventType.PHASE));

    var endOfTermEvents = getEndOfTermRequirementEvents(
        licenceScheduleTerm,
        includedEventTypes,
        allowedActions,
        eventRefWpStatusMap,
        eventRefCommentsMap
    );

    var editUrl = allowedActions.contains(ScheduleEventAction.EDIT_SCHEDULE_EVENTS)
        ? ReverseRouter.route(on(LicenceScheduleTermController.class).renderUpdateTermForm(licenceScheduleTerm.getId()))
        : "";

    var deleteUrl = allowedActions.contains(ScheduleEventAction.EDIT_SCHEDULE_EVENTS)
        ? ReverseRouter.route(on(LicenceScheduleTermDeletionController.class).renderDeleteTermPage(licenceScheduleTerm.getId()))
        : "";

    var addCommentUrl = allowedActions.contains(ScheduleEventAction.ADD_SCHEDULE_COMMENT)
        ? ReverseRouter.route(on(EventCommentController.class)
          .renderAddCommentForm(licenceScheduleTerm.getEventReference().getId(), null))
        : "";

    var termComments = eventRefCommentsMap.getOrDefault(licenceScheduleTerm.getEventReference().getId(), List.of());

    return new TimelineTermView(
        scheduleEvents,
        endOfTermEvents,
        licenceScheduleTerm.getTermType(),
        dateDurationString,
        DateFormatUtil.convertToDisplayText(licenceScheduleTerm.getEndDate()),
        editUrl,
        deleteUrl,
        addCommentUrl,
        hasPhases,
        termComments
    );
  }

  private List<ScheduleEvent> getScheduleEventsForTerm(
      LicenceScheduleTerm licenceScheduleTerm,
      List<ScheduleEventType> includedEventTypes,
      List<ScheduleEventAction> allowedActions,
      Map<UUID, WorkProgrammeActivityStatus> eventRefWpStatusMap,
      Map<UUID, List<EventCommentView>> eventRefCommentsMap,
      Map<UUID, StartEndDates> rateDatesMap
  ) {
    var phases = licenceSchedulePhaseService.getActivePhasesByTerm(licenceScheduleTerm);

    var firstPhaseType = phases.stream()
        .min(Comparator.comparing(phase -> phase.getPhaseType().getDisplayOrder()))
        .map(LicenceSchedulePhase::getPhaseType)
        .orElse(null);

    var phaseViews = phases.stream()
        .sorted(Comparator.comparing(phase -> phase.getPhaseType().getDisplayOrder()))
        .map(phase -> convertToTimelinePhaseView(
            phase,
            firstPhaseType,
            includedEventTypes,
            allowedActions,
            eventRefWpStatusMap,
            eventRefCommentsMap,
            rateDatesMap
        ))
        .toList();

    if (!phaseViews.isEmpty()) {
      var termRateViews = licenceScheduleRateService.getActiveLicenceScheduleRatesAttachedToTerm(licenceScheduleTerm).stream()
          .map(licenceScheduleRate -> TimelineRateView.getScheduleEventFrom(
              licenceScheduleRate,
              rateDatesMap,
              allowedActions,
              eventRefCommentsMap
          ))
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
            TimelineWorkProgrammeActivityView.getScheduleEventFrom(
                workProgrammeActivity,
                allowedActions,
                eventRefWpStatusMap,
                eventRefCommentsMap
            ));

    var rates = licenceScheduleRateService.getActiveLicenceScheduleRatesByTerm(licenceScheduleTerm)
        .stream()
        .map(licenceScheduleRate -> TimelineRateView.getScheduleEventFrom(
            licenceScheduleRate,
            rateDatesMap,
            allowedActions,
            eventRefCommentsMap
        ));

    var otherScheduleEvents = otherScheduleEventService
        .getActiveScheduleEventsByDateRangeFor(licenceScheduleTerm).stream()
        .sorted(
            Comparator.comparing(OtherScheduleEvent::getEventDate)
                .thenComparing(OtherScheduleEvent::getCategoryString))
        .map(otherScheduleEvent ->
            TimelineOtherScheduleEventView.getScheduleEventFrom(
                otherScheduleEvent,
                allowedActions,
                eventRefCommentsMap
            ));

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
      Map<UUID, WorkProgrammeActivityStatus> eventRefWpStatusMap,
      Map<UUID, List<EventCommentView>> eventRefCommentsMap
  ) {
    var workProgrammeActivities = workProgrammeActivityService.getActiveWorkProgrammeActivitiesByTermAndDateOption(
        licenceScheduleTerm,
        WorkProgrammeActivityDateOption.WITHIN_A_TERM
    ).stream()
        .sorted(Comparator.comparing(WorkProgrammeActivity::getCategoryString))
        .map(workProgrammeActivity ->
            TimelineWorkProgrammeActivityView.getScheduleEventFrom(
                workProgrammeActivity,
                allowedActions,
                eventRefWpStatusMap,
                eventRefCommentsMap
            ));

    var otherScheduleEvents = otherScheduleEventService.getActiveScheduleEventsByTermAndDateOption(
        licenceScheduleTerm,
        OtherScheduleEventDateOption.WITHIN_A_TERM
    ).stream()
        .sorted(Comparator.comparing(OtherScheduleEvent::getCategoryString))
        .map(otherScheduleEvent ->
            TimelineOtherScheduleEventView.getScheduleEventFrom(
                otherScheduleEvent,
                allowedActions,
                eventRefCommentsMap
            ));

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
      Map<UUID, WorkProgrammeActivityStatus> eventRefWpStatusMap,
      Map<UUID, List<EventCommentView>> eventRefCommentsMap,
      Map<UUID, StartEndDates> rateDatesMap
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
          .renderAddCommentForm(licenceSchedulePhase.getEventReference().getId(), null))
        : "";

    var phaseComments = eventRefCommentsMap.getOrDefault(licenceSchedulePhase.getEventReference().getId(), List.of());

    return new TimelinePhaseView(
        getScheduleEventsForPhase(
            licenceSchedulePhase,
            firstPhaseType,
            includedEventTypes,
            allowedActions,
            eventRefWpStatusMap,
            eventRefCommentsMap,
            rateDatesMap
        ),
        getEndOfPhaseRequirementEvents(
            licenceSchedulePhase,
            includedEventTypes,
            allowedActions,
            eventRefWpStatusMap,
            eventRefCommentsMap
        ),
        licenceSchedulePhase.getPhaseType(),
        licenceSchedulePhase.getStartDate(),
        dateDurationString,
        DateFormatUtil.convertToDisplayText(licenceSchedulePhase.getEndDate()),
        editUrl,
        deleteUrl,
        addCommentUrl,
        phaseComments
    );
  }

  private List<ScheduleEvent> getScheduleEventsForPhase(
      LicenceSchedulePhase licenceSchedulePhase,
      PhaseType firstPhaseType,
      List<ScheduleEventType> includedEventTypes,
      List<ScheduleEventAction> allowedActions,
      Map<UUID, WorkProgrammeActivityStatus> eventRefWpStatusMap,
      Map<UUID, List<EventCommentView>> eventRefCommentsMap,
      Map<UUID, StartEndDates> rateDatesMap
  ) {
    var workProgrammeActivities = workProgrammeActivityService
        .getActiveWorkProgrammeActivitiesByDateRangeFor(licenceSchedulePhase).stream()
        .sorted(
            Comparator.comparing(WorkProgrammeActivity::getDueDate)
                .thenComparing(WorkProgrammeActivity::getCategoryString)
        )
        .map(workProgrammeActivity ->
            TimelineWorkProgrammeActivityView.getScheduleEventFrom(
                workProgrammeActivity,
                allowedActions,
                eventRefWpStatusMap,
                eventRefCommentsMap
            ));

    var rates = licenceScheduleRateService.getActiveLicenceScheduleRatesByPhase(licenceSchedulePhase, firstPhaseType).stream()
        .map(licenceScheduleRate -> TimelineRateView.getScheduleEventFrom(
            licenceScheduleRate,
            rateDatesMap,
            allowedActions,
            eventRefCommentsMap
        ));

    var otherScheduleEvents = otherScheduleEventService
        .getActiveScheduleEventsByDateRangeFor(licenceSchedulePhase).stream()
        .sorted(
            Comparator.comparing(OtherScheduleEvent::getEventDate)
                .thenComparing(OtherScheduleEvent::getCategoryString))
        .map(otherScheduleEvent ->
            TimelineOtherScheduleEventView.getScheduleEventFrom(
                otherScheduleEvent,
                allowedActions,
                eventRefCommentsMap
            ));

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
      Map<UUID, WorkProgrammeActivityStatus> eventRefWpStatusMap,
      Map<UUID, List<EventCommentView>> eventRefCommentsMap
  ) {
    var workProgrammeActivities = workProgrammeActivityService.getActiveWorkProgrammeActivitiesByPhaseAndDateOption(
            licenceSchedulePhase,
            WorkProgrammeActivityDateOption.WITHIN_A_PHASE
        ).stream()
        .sorted(Comparator.comparing(WorkProgrammeActivity::getCategoryString))
        .map(workProgrammeActivity ->
            TimelineWorkProgrammeActivityView.getScheduleEventFrom(
                workProgrammeActivity,
                allowedActions,
                eventRefWpStatusMap,
                eventRefCommentsMap
            ));

    var otherScheduleEvents = otherScheduleEventService.getActiveScheduleEventsByPhaseAndDateOption(
            licenceSchedulePhase,
            OtherScheduleEventDateOption.WITHIN_A_PHASE
        ).stream()
        .sorted(Comparator.comparing(OtherScheduleEvent::getCategoryString))
        .map(otherScheduleEvent ->
            TimelineOtherScheduleEventView.getScheduleEventFrom(
                otherScheduleEvent,
                allowedActions,
                eventRefCommentsMap
            ));

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
    var eventRefCommentsMap = eventCommentService.getEventCommentViewsForSchedule(licenceScheduleDetail.getLicenceSchedule());
    var rateDatesMap = licenceScheduleCalculationService.calculateRateEndDatesForDisplay(licenceScheduleDetail);

    var workProgrammeActivities = workProgrammeActivityService.getActiveWorkProgrammeActivitiesAfterDate(
        licenceScheduleDetail,
        finalTermEndDate
    )
        .stream()
        .map(activity -> TimelineWorkProgrammeActivityView.getScheduleEventFrom(
            activity,
            allowedActions,
            eventRefWpStatusMap,
            eventRefCommentsMap
        ));

    var rates = licenceScheduleRateService.getActiveRatesAfterDate(
        licenceScheduleDetail,
        finalTermEndDate
    )
        .stream()
        .map(rate -> TimelineRateView.getScheduleEventFrom(
            rate,
            rateDatesMap,
            allowedActions,
            eventRefCommentsMap
        ));

    var otherScheduleEvents = otherScheduleEventService.getActiveEventsAfterDate(
        licenceScheduleDetail,
        finalTermEndDate
    )
        .stream()
        .map(event -> TimelineOtherScheduleEventView.getScheduleEventFrom(
            event,
            allowedActions,
            eventRefCommentsMap
        ));

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
