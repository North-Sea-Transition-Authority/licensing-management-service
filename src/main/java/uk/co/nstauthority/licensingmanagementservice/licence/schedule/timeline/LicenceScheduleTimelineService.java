package uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDuration;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDurationDisplayUtil;
import uk.co.nstauthority.licensingmanagementservice.formatting.DateFormatUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenseeUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.PhaseType;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisationService;
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
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate.LicenceScheduleRate;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate.LicenceScheduleRateService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate.RateDefinitionOption;
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
import uk.co.nstauthority.licensingmanagementservice.licence.status.LicenceStatusService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamQueryService;
import uk.co.nstauthority.licensingmanagementservice.util.DateUtil;

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
  private final LicenceStatusService licenceStatusService;
  private final LicenceResponsibleOrganisationService licenceResponsibleOrganisationService;
  private final Clock clock;

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
      LicenceScheduleCalculationService licenceScheduleCalculationService,
      LicenceStatusService licenceStatusService,
      LicenceResponsibleOrganisationService licenceResponsibleOrganisationService,
      Clock clock
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
    this.licenceStatusService = licenceStatusService;
    this.licenceResponsibleOrganisationService = licenceResponsibleOrganisationService;
    this.clock = clock;
  }

  public TimelineSummaryCardView getTimelineSummaryCardView(LicenceScheduleDetail licenceScheduleDetail) {
    var licence = licenceScheduleDetail.getLicenceSchedule().getLicence();
    var licenceStartDate = licenceStartDateService.getByLicenceScheduleDetailOrThrow(licenceScheduleDetail);
    var licenceExpiryDateString = licenceScheduleExpiryService.getExpiryForLicenceScheduleDetail(licenceScheduleDetail)
        .map(this::getExpiryDateString)
        .orElse("");

    var licenceEndDateString = getDateEndedOnString(licence, licenceStartDate.getStartDate());
    var finalTermEndDateString = getFinalTermDateString(licenceScheduleDetail, licenceStartDate.getStartDate());

    return new TimelineSummaryCardView(
        DateFormatUtil.convertToDisplayText(licenceStartDate.getStartDate()),
        licenceExpiryDateString,
        licenceTypeRulesResolver.canShowLicenceRoundIssuedOn(licence.getType()),
        licence.getRoundIssuedOn(),
        licenceStatusService.getCurrentStatus(licence).getDisplayName(),
        licenceEndDateString,
        finalTermEndDateString,
        LicenseeUtil.getLicenseeNames(licence, licenceResponsibleOrganisationService)
    );
  }

  private String getExpiryDateString(LicenceScheduleExpiry licenceScheduleExpiry) {
    if (licenceScheduleExpiry.getExpiryDate() == null) {
      return "";
    }

    return DateFormatUtil.convertToDisplayText(licenceScheduleExpiry.getExpiryDate());
  }

  private String getDateEndedOnString(
      Licence licence,
      LocalDate startDate
  ) {
    var endDate = licence.getEndDate();

    if (endDate == null) {
      return "";
    }

    return "%s (%s)".formatted(
        DateFormatUtil.convertToDisplayText(endDate),
        ThreeFieldDurationDisplayUtil.convertDatesToDurationDisplayText(startDate, endDate)
    );
  }

  private String getFinalTermDateString(
      LicenceScheduleDetail licenceScheduleDetail,
      LocalDate startDate
  ) {
    var finalTerm = licenceScheduleTermService.getTermsByLicenceScheduleDetail(licenceScheduleDetail).stream()
        .max(Comparator.comparing(term -> term.getTermType().getDisplayOrder()));

    if (finalTerm.isEmpty()) {
      return "";
    }

    var finalTermEndDate = finalTerm.get().getEndDate();

    return "%s (%s)".formatted(
        DateFormatUtil.convertToDisplayText(finalTermEndDate),
        ThreeFieldDurationDisplayUtil.convertDatesToDurationDisplayText(startDate, finalTermEndDate)
    );
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
      ScheduleTimelineFilterForm scheduleTimelineFilterForm,
      ServiceUserDetail userDetail
  ) {
    Map<UUID, WorkProgrammeActivityStatus> eventRefWpStatusMap = new HashMap<>();
    Map<UUID, List<EventCommentView>> eventRefCommentsMap = new HashMap<>();
    List<ScheduleEventAction> eventActions = new ArrayList<>();
    var finalProgressDate = getTimelineProgressDate(licenceScheduleDetail);

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
        scheduleTimelineFilterForm,
        eventActions,
        eventRefWpStatusMap,
        eventRefCommentsMap,
        finalProgressDate
    );
  }

  private LocalDate getTimelineProgressDate(LicenceScheduleDetail licenceScheduleDetail) {
    var licenceEndedOnDate = licenceScheduleDetail.getLicenceSchedule().getLicence().getEndDate();
    if (licenceEndedOnDate != null) {
      return licenceEndedOnDate;
    }

    return LocalDate.now(clock);
  }

  List<TimelineTermView> getEditableLicenceScheduleEventViews(
      LicenceScheduleDetail licenceScheduleDetail,
      ScheduleTimelineFilterForm scheduleTimelineFilterForm,
      List<ScheduleEventAction> allowedActions
  ) {
    var eventRefWpStatusMap = getLatestWpStatusesForSchedule(licenceScheduleDetail);
    var eventRefCommentsMap = eventCommentService.getEventCommentViewsForSchedule(licenceScheduleDetail.getLicenceSchedule());
    var finalProgressDate = getTimelineProgressDate(licenceScheduleDetail);

    return getLicenceScheduleEventViews(
        licenceScheduleDetail,
        scheduleTimelineFilterForm,
        allowedActions,
        eventRefWpStatusMap,
        eventRefCommentsMap,
        finalProgressDate
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
      ScheduleTimelineFilterForm scheduleTimelineFilterForm,
      List<ScheduleEventAction> allowedActions,
      Map<UUID, WorkProgrammeActivityStatus> eventRefWpStatusMap,
      Map<UUID, List<EventCommentView>> eventRefCommentsMap,
      LocalDate finalProgressDate
  ) {
    var includedEventTypes = scheduleTimelineFilterForm.getEventTypes().stream()
        .map(ScheduleEventType::valueOf)
        .toList();

    var rateDatesMap = licenceScheduleCalculationService.calculateRateEndDatesForDisplay(licenceScheduleDetail);

    var terms = licenceScheduleTermService.getTermsByLicenceScheduleDetail(licenceScheduleDetail);
    var scheduleEventData = buildScheduleEventData(licenceScheduleDetail, terms);

    return terms.stream()
        .sorted(Comparator.comparing(term -> term.getTermType().getDisplayOrder()))
        .map(term -> convertToTimelineTermView(
            term,
            includedEventTypes,
            allowedActions,
            eventRefWpStatusMap,
            eventRefCommentsMap,
            rateDatesMap,
            finalProgressDate,
            scheduleEventData
        ))
        .toList();
  }

  /**
   * Pre-fetches all child data for a licence schedule detail in a fixed number of queries, rather than
   * querying per-term/per-phase as the timeline is built. See documentation/investigations/timeline-n-plus-one-plan.md.
   */
  private ScheduleEventData buildScheduleEventData(
      LicenceScheduleDetail licenceScheduleDetail,
      List<LicenceScheduleTerm> terms
  ) {
    var allPhases = licenceSchedulePhaseService.getPhasesByLicenceScheduleDetail(licenceScheduleDetail);
    var phasesByTerm = allPhases.stream()
        .collect(Collectors.groupingBy(phase -> phase.getLicenceScheduleTerm().getId()));

    var allActivities = workProgrammeActivityService.getWorkProgrammeActivities(licenceScheduleDetail);
    var withinTermActivities = allActivities.stream()
        .filter(activity -> activity.getDateOption() == WorkProgrammeActivityDateOption.WITHIN_A_TERM)
        .collect(Collectors.groupingBy(activity -> activity.getLicenceScheduleTerm().getId()));
    var withinPhaseActivities = allActivities.stream()
        .filter(activity -> activity.getDateOption() == WorkProgrammeActivityDateOption.WITHIN_A_PHASE)
        .collect(Collectors.groupingBy(activity -> activity.getLicenceSchedulePhase().getId()));

    var allOtherEvents = otherScheduleEventService.getOtherScheduleEvents(licenceScheduleDetail);
    var withinTermOtherEvents = allOtherEvents.stream()
        .filter(event -> event.getDateOption() == OtherScheduleEventDateOption.WITHIN_A_TERM)
        .collect(Collectors.groupingBy(event -> event.getLicenceScheduleTerm().getId()));
    var withinPhaseOtherEvents = allOtherEvents.stream()
        .filter(event -> event.getDateOption() == OtherScheduleEventDateOption.WITHIN_A_PHASE)
        .collect(Collectors.groupingBy(event -> event.getLicenceSchedulePhase().getId()));

    var termLinkedRatesByTerm = licenceScheduleRateService
        .getLicenceScheduleRatesForTermsAndDefinitionOption(terms, RateDefinitionOption.TERM)
        .stream()
        .collect(Collectors.groupingBy(rate -> rate.getLicenceScheduleTerm().getId()));

    var phaseLinkedRatesByPhase = licenceScheduleRateService
        .getLicenceScheduleRatesForPhasesAndDefinitionOption(allPhases, RateDefinitionOption.PHASE)
        .stream()
        .collect(Collectors.groupingBy(rate -> rate.getLicenceSchedulePhase().getId()));

    var allRates = licenceScheduleRateService.getLicenceScheduleRates(licenceScheduleDetail);

    return new ScheduleEventData(
        phasesByTerm,
        withinTermActivities,
        withinPhaseActivities,
        allActivities,
        withinTermOtherEvents,
        withinPhaseOtherEvents,
        allOtherEvents,
        termLinkedRatesByTerm,
        phaseLinkedRatesByPhase,
        allRates
    );
  }

  private TimelineTermView convertToTimelineTermView(
      LicenceScheduleTerm licenceScheduleTerm,
      List<ScheduleEventType> includedEventTypes,
      List<ScheduleEventAction> allowedActions,
      Map<UUID, WorkProgrammeActivityStatus> eventRefWpStatusMap,
      Map<UUID, List<EventCommentView>> eventRefCommentsMap,
      Map<UUID, StartEndDates> rateDatesMap,
      LocalDate finalProgressDate,
      ScheduleEventData scheduleEventData
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
        rateDatesMap,
        finalProgressDate,
        scheduleEventData
    );

    var hasPhases = scheduleEvents.stream()
        .map(ScheduleEvent::getEventType)
        .anyMatch(eventType -> eventType.equals(ScheduleEventType.PHASE));

    var endOfTermEvents = getEndOfTermRequirementEvents(
        licenceScheduleTerm,
        includedEventTypes,
        allowedActions,
        eventRefWpStatusMap,
        eventRefCommentsMap,
        finalProgressDate,
        scheduleEventData
    );

    var editUrl = allowedActions.contains(ScheduleEventAction.EDIT_SCHEDULE_EVENTS)
        ? ReverseRouter.route(on(LicenceScheduleTermController.class).renderUpdateTermForm(licenceScheduleTerm.getId()))
        : "";

    var deleteUrl = allowedActions.contains(ScheduleEventAction.EDIT_SCHEDULE_EVENTS)
        ? ReverseRouter.route(on(LicenceScheduleTermDeletionController.class).renderDeleteTermPage(licenceScheduleTerm.getId()))
        : "";

    var addCommentUrl = allowedActions.contains(ScheduleEventAction.ADD_SCHEDULE_COMMENT)
        ? ReverseRouter.route(on(EventCommentController.class)
          .renderAddCommentForm(licenceScheduleTerm.getId(), null))
        : "";

    var termComments = eventRefCommentsMap.getOrDefault(licenceScheduleTerm.getOriginalEventId(), List.of());

    var showStartDateProgress = !licenceScheduleTerm.getStartDate().isAfter(finalProgressDate);
    var showEndDateProgress = !licenceScheduleTerm.getEndDate().isAfter(finalProgressDate);

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
        termComments,
        showStartDateProgress,
        showEndDateProgress
    );
  }

  private List<ScheduleEvent> getScheduleEventsForTerm(
      LicenceScheduleTerm licenceScheduleTerm,
      List<ScheduleEventType> includedEventTypes,
      List<ScheduleEventAction> allowedActions,
      Map<UUID, WorkProgrammeActivityStatus> eventRefWpStatusMap,
      Map<UUID, List<EventCommentView>> eventRefCommentsMap,
      Map<UUID, StartEndDates> rateDatesMap,
      LocalDate finalProgressDate,
      ScheduleEventData scheduleEventData
  ) {
    var phases = scheduleEventData.phasesByTerm().getOrDefault(licenceScheduleTerm.getId(), List.of());

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
            rateDatesMap,
            finalProgressDate,
            scheduleEventData
        ))
        .toList();

    if (!phaseViews.isEmpty()) {
      var termRateViews = scheduleEventData.termLinkedRatesByTerm().getOrDefault(licenceScheduleTerm.getId(), List.of()).stream()
          .map(licenceScheduleRate -> TimelineRateView.getScheduleEventFrom(
              licenceScheduleRate,
              rateDatesMap,
              allowedActions,
              eventRefCommentsMap,
              finalProgressDate
          ))
          .filter(event -> includedEventTypes.contains(event.getEventType()))
          .toList();

      return Stream.concat(phaseViews.stream(), termRateViews.stream())
          .sorted(Comparator.comparing(ScheduleEvent::getSortingDate)
              .thenComparing(event -> event.getEventType().getEventTypeOrder()))
          .toList();
    }

    var workProgrammeActivities = DateUtil.filterByDateRange(
        scheduleEventData.allActivities(),
        WorkProgrammeActivity::getDueDate,
        licenceScheduleTerm.getStartDate(),
        licenceScheduleTerm.getEndDate()
    ).stream()
        .sorted(
            Comparator.comparing(WorkProgrammeActivity::getDueDate)
            .thenComparing(WorkProgrammeActivity::getCategoryString))
        .map(workProgrammeActivity ->
            TimelineWorkProgrammeActivityView.getScheduleEventFrom(
                workProgrammeActivity,
                allowedActions,
                eventRefWpStatusMap,
                eventRefCommentsMap,
                finalProgressDate
            ));

    var rates = resolveRatesForTerm(scheduleEventData, licenceScheduleTerm)
        .stream()
        .map(licenceScheduleRate -> TimelineRateView.getScheduleEventFrom(
            licenceScheduleRate,
            rateDatesMap,
            allowedActions,
            eventRefCommentsMap,
            finalProgressDate
        ));

    var otherScheduleEvents = DateUtil.filterByDateRange(
        scheduleEventData.allOtherEvents(),
        OtherScheduleEvent::getEventDate,
        licenceScheduleTerm.getStartDate(),
        licenceScheduleTerm.getEndDate()
    ).stream()
        .sorted(
            Comparator.comparing(OtherScheduleEvent::getEventDate)
                .thenComparing(OtherScheduleEvent::getCategoryString))
        .map(otherScheduleEvent ->
            TimelineOtherScheduleEventView.getScheduleEventFrom(
                otherScheduleEvent,
                allowedActions,
                eventRefCommentsMap,
                finalProgressDate
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
      Map<UUID, List<EventCommentView>> eventRefCommentsMap,
      LocalDate finalProgressDate,
      ScheduleEventData scheduleEventData
  ) {
    var workProgrammeActivities = scheduleEventData.withinTermActivities()
        .getOrDefault(licenceScheduleTerm.getId(), List.of())
        .stream()
        .sorted(Comparator.comparing(WorkProgrammeActivity::getCategoryString))
        .map(workProgrammeActivity ->
            TimelineWorkProgrammeActivityView.getScheduleEventFrom(
                workProgrammeActivity,
                allowedActions,
                eventRefWpStatusMap,
                eventRefCommentsMap,
                finalProgressDate
            ));

    var otherScheduleEvents = scheduleEventData.withinTermOtherEvents()
        .getOrDefault(licenceScheduleTerm.getId(), List.of())
        .stream()
        .sorted(Comparator.comparing(OtherScheduleEvent::getCategoryString))
        .map(otherScheduleEvent ->
            TimelineOtherScheduleEventView.getScheduleEventFrom(
                otherScheduleEvent,
                allowedActions,
                eventRefCommentsMap,
                finalProgressDate
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
      Map<UUID, StartEndDates> rateDatesMap,
      LocalDate finalProgressDate,
      ScheduleEventData scheduleEventData
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
          .renderAddCommentForm(licenceSchedulePhase.getId(), null))
        : "";

    var phaseComments = eventRefCommentsMap.getOrDefault(licenceSchedulePhase.getOriginalEventId(), List.of());

    var showStartDateProgress = !licenceSchedulePhase.getStartDate().isAfter(finalProgressDate);
    var showEndDateProgress = !licenceSchedulePhase.getEndDate().isAfter(finalProgressDate);

    return new TimelinePhaseView(
        getScheduleEventsForPhase(
            licenceSchedulePhase,
            firstPhaseType,
            includedEventTypes,
            allowedActions,
            eventRefWpStatusMap,
            eventRefCommentsMap,
            rateDatesMap,
            finalProgressDate,
            scheduleEventData
        ),
        getEndOfPhaseRequirementEvents(
            licenceSchedulePhase,
            includedEventTypes,
            allowedActions,
            eventRefWpStatusMap,
            eventRefCommentsMap,
            finalProgressDate,
            scheduleEventData
        ),
        licenceSchedulePhase.getPhaseType(),
        licenceSchedulePhase.getStartDate(),
        dateDurationString,
        DateFormatUtil.convertToDisplayText(licenceSchedulePhase.getEndDate()),
        editUrl,
        deleteUrl,
        addCommentUrl,
        phaseComments,
        showStartDateProgress,
        showEndDateProgress
    );
  }

  private List<ScheduleEvent> getScheduleEventsForPhase(
      LicenceSchedulePhase licenceSchedulePhase,
      PhaseType firstPhaseType,
      List<ScheduleEventType> includedEventTypes,
      List<ScheduleEventAction> allowedActions,
      Map<UUID, WorkProgrammeActivityStatus> eventRefWpStatusMap,
      Map<UUID, List<EventCommentView>> eventRefCommentsMap,
      Map<UUID, StartEndDates> rateDatesMap,
      LocalDate finalProgressDate,
      ScheduleEventData scheduleEventData
  ) {
    var workProgrammeActivities = DateUtil.filterByDateRange(
        scheduleEventData.allActivities(),
        WorkProgrammeActivity::getDueDate,
        licenceSchedulePhase.getStartDate(),
        licenceSchedulePhase.getEndDate()
    ).stream()
        .sorted(
            Comparator.comparing(WorkProgrammeActivity::getDueDate)
                .thenComparing(WorkProgrammeActivity::getCategoryString)
        )
        .map(workProgrammeActivity ->
            TimelineWorkProgrammeActivityView.getScheduleEventFrom(
                workProgrammeActivity,
                allowedActions,
                eventRefWpStatusMap,
                eventRefCommentsMap,
                finalProgressDate
            ));

    var rates = resolveRatesForPhase(scheduleEventData, licenceSchedulePhase, firstPhaseType).stream()
        .map(licenceScheduleRate -> TimelineRateView.getScheduleEventFrom(
            licenceScheduleRate,
            rateDatesMap,
            allowedActions,
            eventRefCommentsMap,
            finalProgressDate
        ));

    var otherScheduleEvents = DateUtil.filterByDateRange(
        scheduleEventData.allOtherEvents(),
        OtherScheduleEvent::getEventDate,
        licenceSchedulePhase.getStartDate(),
        licenceSchedulePhase.getEndDate()
    ).stream()
        .sorted(
            Comparator.comparing(OtherScheduleEvent::getEventDate)
                .thenComparing(OtherScheduleEvent::getCategoryString))
        .map(otherScheduleEvent ->
            TimelineOtherScheduleEventView.getScheduleEventFrom(
                otherScheduleEvent,
                allowedActions,
                eventRefCommentsMap,
                finalProgressDate
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
      Map<UUID, List<EventCommentView>> eventRefCommentsMap,
      LocalDate finalProgressDate,
      ScheduleEventData scheduleEventData
  ) {
    var workProgrammeActivities = scheduleEventData.withinPhaseActivities()
        .getOrDefault(licenceSchedulePhase.getId(), List.of())
        .stream()
        .sorted(Comparator.comparing(WorkProgrammeActivity::getCategoryString))
        .map(workProgrammeActivity ->
            TimelineWorkProgrammeActivityView.getScheduleEventFrom(
                workProgrammeActivity,
                allowedActions,
                eventRefWpStatusMap,
                eventRefCommentsMap,
                finalProgressDate
            ));

    var otherScheduleEvents = scheduleEventData.withinPhaseOtherEvents()
        .getOrDefault(licenceSchedulePhase.getId(), List.of())
        .stream()
        .sorted(Comparator.comparing(OtherScheduleEvent::getCategoryString))
        .map(otherScheduleEvent ->
            TimelineOtherScheduleEventView.getScheduleEventFrom(
                otherScheduleEvent,
                allowedActions,
                eventRefCommentsMap,
                finalProgressDate
            ));

    return Stream.of(workProgrammeActivities, otherScheduleEvents)
        .flatMap(Function.identity())
        .filter(event -> includedEventTypes.contains(event.getEventType()))
        .sorted(Comparator.comparing(event -> event.getEventType().getEventTypeOrder()))
        .toList();
  }

  /**
   * Replicates {@code LicenceScheduleRateService.getLicenceScheduleRatesByTerm} against pre-fetched data:
   * TERM-linked rates take priority, falling back to rates whose start date falls within the term.
   */
  private List<LicenceScheduleRate> resolveRatesForTerm(
      ScheduleEventData scheduleEventData,
      LicenceScheduleTerm licenceScheduleTerm
  ) {
    var termLinkedRates = scheduleEventData.termLinkedRatesByTerm().getOrDefault(licenceScheduleTerm.getId(), List.of());

    if (!termLinkedRates.isEmpty()) {
      return termLinkedRates;
    }

    return DateUtil.filterByDateRange(
        scheduleEventData.allRates(),
        LicenceScheduleRate::getStartDate,
        licenceScheduleTerm.getStartDate(),
        licenceScheduleTerm.getEndDate()
    );
  }

  /**
   * Replicates {@code LicenceScheduleRateService.getLicenceScheduleRatesByPhase} against pre-fetched data.
   */
  private List<LicenceScheduleRate> resolveRatesForPhase(
      ScheduleEventData scheduleEventData,
      LicenceSchedulePhase licenceSchedulePhase,
      PhaseType firstPhaseType
  ) {
    if (licenceSchedulePhase.getPhaseType().equals(firstPhaseType)) {
      var termLinkedRates = scheduleEventData.termLinkedRatesByTerm()
          .getOrDefault(licenceSchedulePhase.getLicenceScheduleTerm().getId(), List.of());

      if (!termLinkedRates.isEmpty()) {
        return List.of();
      }
    }

    var phaseLinkedRates = scheduleEventData.phaseLinkedRatesByPhase().getOrDefault(licenceSchedulePhase.getId(), List.of());

    if (!phaseLinkedRates.isEmpty()) {
      return phaseLinkedRates;
    }

    return DateUtil.filterByDateRange(
        scheduleEventData.allRates(),
        LicenceScheduleRate::getStartDate,
        licenceSchedulePhase.getStartDate(),
        licenceSchedulePhase.getEndDate()
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

  private Map<UUID, WorkProgrammeActivityStatus> getLatestWpStatusesForSchedule(LicenceScheduleDetail licenceScheduleDetail) {
    return workProgrammeActivityStatusService.getLatestStatusesFor(
        workProgrammeActivityService.getWorkProgrammeActivities(licenceScheduleDetail)
    );
  }

  /**
   * Holds every term/phase-scoped piece of schedule data needed to render the timeline, fetched once per
   * schedule detail instead of once per term/phase. See documentation/investigations/timeline-n-plus-one-plan.md.
   */
  private record ScheduleEventData(
      Map<UUID, List<LicenceSchedulePhase>> phasesByTerm,
      Map<UUID, List<WorkProgrammeActivity>> withinTermActivities,
      Map<UUID, List<WorkProgrammeActivity>> withinPhaseActivities,
      List<WorkProgrammeActivity> allActivities,
      Map<UUID, List<OtherScheduleEvent>> withinTermOtherEvents,
      Map<UUID, List<OtherScheduleEvent>> withinPhaseOtherEvents,
      List<OtherScheduleEvent> allOtherEvents,
      Map<UUID, List<LicenceScheduleRate>> termLinkedRatesByTerm,
      Map<UUID, List<LicenceScheduleRate>> phaseLinkedRatesByPhase,
      List<LicenceScheduleRate> allRates
  ) {
  }
}
