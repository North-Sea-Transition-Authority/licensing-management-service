package uk.co.nstauthority.licensingmanagementservice.licence.crosslicenceeventtracker;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import static uk.co.nstauthority.licensingmanagementservice.jooq.tables.LicenceEventCache.LICENCE_EVENT_CACHE;
import static uk.co.nstauthority.licensingmanagementservice.jooq.tables.Licences.LICENCES;

import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisationgroup.OrganisationGroupQueryService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.EnergyPortalUserJson;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.EnergyPortalUserService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.WebUserAccountId;
import uk.co.nstauthority.licensingmanagementservice.exception.LmsEntityNotFoundException;
import uk.co.nstauthority.licensingmanagementservice.fds.table.SortableTableRow;
import uk.co.nstauthority.licensingmanagementservice.fds.table.SortableTableSortDirection;
import uk.co.nstauthority.licensingmanagementservice.fds.table.SortableTableValue;
import uk.co.nstauthority.licensingmanagementservice.fds.table.SortableTableView;
import uk.co.nstauthority.licensingmanagementservice.fds.table.Tag;
import uk.co.nstauthority.licensingmanagementservice.formatting.DateFormatUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceApplication;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceService;
import uk.co.nstauthority.licensingmanagementservice.licence.OrganisationUnit;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationType;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplication;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetailQueryService;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.overview.LicenceContinuationApplicationOverviewController;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisationService;
import uk.co.nstauthority.licensingmanagementservice.licence.overview.LicenceScheduleTabController;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhase;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhaseService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTermService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline.ScheduleEventType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivity;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityDateOption;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplication;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetailQueryService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney.LicenceWorkProgrammeAmendmentRequestQueryService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.extendjourney.LicenceScheduleExtensionRequest;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.extendjourney.LicenceScheduleExtensionRequestQueryService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overview.ScheduleWorkProgrammeApplicationOverviewController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.teams.IndustryRoleService;
import uk.co.nstauthority.licensingmanagementservice.teams.RegulatorRoleService;
import uk.co.nstauthority.licensingmanagementservice.util.FilterUtil;
import uk.co.nstauthority.licensingmanagementservice.util.StreamUtil;

@Service
public class CrossLicenceEventTrackerService {

  private static final DateTimeFormatter EVENT_DATE_FILTER_FORMAT = DateTimeFormatter.ofPattern("dd/MM/uuuu")
      .withResolverStyle(ResolverStyle.STRICT);

  private static final String GET_STEWARDS_PURPOSE = "Fetch stewards for cross licence event tracker";

  private final LicenceScheduleTermService licenceScheduleTermService;
  private final LicenceSchedulePhaseService licenceSchedulePhaseService;
  private final LicenceEventCacheRepository licenceEventCacheRepository;
  private final LicenceService licenceService;
  private final LicenceResponsibleOrganisationService licenceResponsibleOrganisationService;
  private final WorkProgrammeActivityService workProgrammeActivityService;
  private final DSLContext dslContext;
  private final RegulatorRoleService regulatorRoleService;
  private final ScheduleWorkProgrammeApplicationDetailQueryService scheduleWorkProgrammeApplicationDetailQueryService;
  private final LicenceScheduleExtensionRequestQueryService licenceScheduleExtensionRequestQueryService;
  private final LicenceWorkProgrammeAmendmentRequestQueryService licenceWorkProgrammeAmendmentRequestQueryService;
  private final EnergyPortalUserService energyPortalUserService;
  private final IndustryRoleService industryRoleService;
  private final LicenceContinuationApplicationDetailQueryService licenceContinuationApplicationDetailQueryService;
  private final OrganisationGroupQueryService organisationGroupQueryService;

  public CrossLicenceEventTrackerService(
      LicenceScheduleTermService licenceScheduleTermService,
      LicenceSchedulePhaseService licenceSchedulePhaseService,
      LicenceEventCacheRepository licenceEventCacheRepository,
      LicenceService licenceService,
      LicenceResponsibleOrganisationService licenceResponsibleOrganisationService,
      WorkProgrammeActivityService workProgrammeActivityService,
      DSLContext dslContext,
      RegulatorRoleService regulatorRoleService,
      ScheduleWorkProgrammeApplicationDetailQueryService scheduleWorkProgrammeApplicationDetailQueryService,
      LicenceScheduleExtensionRequestQueryService licenceScheduleExtensionRequestQueryService,
      LicenceWorkProgrammeAmendmentRequestQueryService licenceWorkProgrammeAmendmentRequestQueryService,
      EnergyPortalUserService energyPortalUserService,
      IndustryRoleService industryRoleService,
      LicenceContinuationApplicationDetailQueryService licenceContinuationApplicationDetailQueryService,
      OrganisationGroupQueryService organisationGroupQueryService
  ) {
    this.licenceScheduleTermService = licenceScheduleTermService;
    this.licenceSchedulePhaseService = licenceSchedulePhaseService;
    this.licenceEventCacheRepository = licenceEventCacheRepository;
    this.licenceService = licenceService;
    this.licenceResponsibleOrganisationService = licenceResponsibleOrganisationService;
    this.workProgrammeActivityService = workProgrammeActivityService;
    this.dslContext = dslContext;
    this.regulatorRoleService = regulatorRoleService;
    this.scheduleWorkProgrammeApplicationDetailQueryService = scheduleWorkProgrammeApplicationDetailQueryService;
    this.licenceScheduleExtensionRequestQueryService = licenceScheduleExtensionRequestQueryService;
    this.licenceWorkProgrammeAmendmentRequestQueryService = licenceWorkProgrammeAmendmentRequestQueryService;
    this.energyPortalUserService = energyPortalUserService;
    this.industryRoleService = industryRoleService;
    this.licenceContinuationApplicationDetailQueryService = licenceContinuationApplicationDetailQueryService;
    this.organisationGroupQueryService = organisationGroupQueryService;
  }

  public SortableTableView getEventTrackerTable(EventTrackerForm form, ServiceUserDetail user) {
    var eventCaches = dslContext.select(LICENCE_EVENT_CACHE.fields())
        .from(LICENCE_EVENT_CACHE)
        .join(LICENCES).on(LICENCES.ID.eq(LICENCE_EVENT_CACHE.LICENCE_ID))
        .where(getLicenceTypeCondition(form))
        .and(getEventDateCondition(form))
        .and(getRequestTypeCondition(form))
        .fetchInto(LicenceEventCache.class);

    var licenceIds = eventCaches.stream()
        .map(LicenceEventCache::getLicenceId)
        .distinct()
        .toList();

    var responsibleOrganisationsByLicence = licenceResponsibleOrganisationService
        .getResponsibleOrganisationsByLicences(licenceService.getLicencesByIds(licenceIds));
    var licenseesByLicenceId = toLicenseeNamesByLicenceId(responsibleOrganisationsByLicence);
    var licenseeOrgUnitIdsByLicenceId = getLicenceIdToLicenseeOrgUnitIdMap(responsibleOrganisationsByLicence);

    var applicationDisplayInfoByApplicationId = getApplicationDisplayInfoByApplicationId(eventCaches);
    var stewardNameByWuaId = getStewardNamesByWuaId(eventCaches);

    var isRegulator = regulatorRoleService.isRegulator(user);
    var ownOrganisationUnitIds = isRegulator
        ? List.<Integer>of()
        : industryRoleService.getEditorOrSubmitterOrganisationUnitIds(user.wuaId()).stream().toList();
    var licenseeGroupOrgUnitIds = form.getLicenseeOrgGroupId() == null
        ? null
        : organisationGroupQueryService.getOrganisationUnitIdsByOrganisationGroupId(form.getLicenseeOrgGroupId());

    var tableBuilder = SortableTableView.sortableTableBuilder()
        .newWithHeadings(
            "Licence",
            "Term / phase transition",
            "Work programme activity",
            "Event end / due date",
            "Application status",
            "Licensee(s)",
            "Quad/block",
            "Steward"
        )
        .withDefaultSortIndex(4)
        .withDefaultSortDirection(SortableTableSortDirection.ASCENDING);

    eventCaches.stream()
        .filter(eventCache -> matchesLicenseeCondition(
            eventCache, licenseeOrgUnitIdsByLicenceId, form, isRegulator, ownOrganisationUnitIds, licenseeGroupOrgUnitIds
        ))
        .filter(eventCache -> matchesEventStatusCondition(eventCache, form, applicationDisplayInfoByApplicationId))
        .sorted(Comparator.comparing(LicenceEventCache::getEventDate, Comparator.nullsLast(Comparator.naturalOrder())))
        .forEach(eventCache -> tableBuilder.addRow(
            toRow(eventCache, licenseesByLicenceId, applicationDisplayInfoByApplicationId, stewardNameByWuaId)
        ));

    return tableBuilder.build();
  }

  private Map<UUID, ApplicationDisplayInfo> getApplicationDisplayInfoByApplicationId(List<LicenceEventCache> eventCaches) {
    var scheduleAmendmentApplicationIds = getApplicationIdsOfType(eventCaches, ApplicationType.SCHEDULE_AMENDMENT_APPLICATION);
    var continuationApplicationIds = getApplicationIdsOfType(eventCaches, ApplicationType.CONTINUATION_APPLICATION);

    var scheduleAmendmentDisplayInfo = scheduleWorkProgrammeApplicationDetailQueryService
        .getLatestSummariesByApplicationIds(scheduleAmendmentApplicationIds)
        .stream()
        .map(summary -> Map.entry(
            summary.applicationId(),
            ApplicationDisplayInfo.from(summary, applicationOverviewLink(summary.detailId()))
        ));

    var continuationDisplayInfo = licenceContinuationApplicationDetailQueryService
        .getLatestSummariesByApplicationIds(continuationApplicationIds)
        .stream()
        .map(summary -> Map.entry(
            summary.applicationId(),
            ApplicationDisplayInfo.from(summary, continuationApplicationOverviewLink(summary.detailId()))
        ));

    return Stream.concat(scheduleAmendmentDisplayInfo, continuationDisplayInfo)
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  private List<UUID> getApplicationIdsOfType(List<LicenceEventCache> eventCaches, ApplicationType applicationType) {
    return eventCaches.stream()
        .filter(eventCache -> eventCache.getApplicationType() == applicationType)
        .map(LicenceEventCache::getApplicationId)
        .filter(Objects::nonNull)
        .distinct()
        .toList();
  }

  private Map<Long, String> getStewardNamesByWuaId(List<LicenceEventCache> eventCaches) {
    var stewardWuaIds = eventCaches.stream()
        .map(LicenceEventCache::getStewardWuaId)
        .filter(Objects::nonNull)
        .distinct()
        .map(WebUserAccountId::from)
        .toList();

    if (stewardWuaIds.isEmpty()) {
      return Map.of();
    }

    return energyPortalUserService.findByWuaIds(stewardWuaIds, GET_STEWARDS_PURPOSE)
        .stream()
        .collect(StreamUtil.toLinkedHashMap(EnergyPortalUserJson::webUserAccountId, EnergyPortalUserJson::displayName));
  }

  private Condition getLicenceTypeCondition(EventTrackerForm form) {
    if (form.getLicenceTypes() == null || form.getLicenceTypes().isEmpty()) {
      return DSL.noCondition();
    }

    return LICENCES.TYPE.in(form.getLicenceTypes());
  }

  private Condition getRequestTypeCondition(EventTrackerForm form) {
    if (CollectionUtils.isEmpty(form.getRequestTypes())) {
      return DSL.noCondition();
    }

    var applicationTypeNames = form.getRequestTypes().stream()
        .map(EventTrackerRequestType::valueOf)
        .map(EventTrackerRequestType::getApplicationType)
        .map(Enum::name)
        .toList();

    return LICENCE_EVENT_CACHE.APPLICATION_TYPE.in(applicationTypeNames);
  }

  private Condition getEventDateCondition(EventTrackerForm form) {
    var condition = DSL.noCondition();

    var fromDate = parseFilterDate(form.getFromDate());
    if (fromDate != null) {
      condition = condition.and(LICENCE_EVENT_CACHE.EVENT_DATE.ge(fromDate));
    }

    var toDate = parseFilterDate(form.getToDate());
    if (toDate != null) {
      condition = condition.and(LICENCE_EVENT_CACHE.EVENT_DATE.le(toDate));
    }

    return condition;
  }

  private LocalDate parseFilterDate(String date) {
    if (StringUtils.isBlank(date)) {
      return null;
    }

    try {
      return LocalDate.parse(date, EVENT_DATE_FILTER_FORMAT);
    } catch (DateTimeParseException e) {
      return null;
    }
  }

  private boolean matchesLicenseeCondition(
      LicenceEventCache eventCache,
      Map<Integer, List<Integer>> licenseeOrgUnitIdsByLicenceId,
      EventTrackerForm form,
      boolean isRegulator,
      List<Integer> ownOrganisationUnitIds,
      List<Integer> licenseeGroupOrgUnitIds
  ) {
    var orgUnitIds = licenseeOrgUnitIdsByLicenceId.getOrDefault(eventCache.getLicenceId(), List.of());

    if (!isRegulator) {
      return FilterUtil.listMatchesIdList(orgUnitIds, ownOrganisationUnitIds);
    }

    return FilterUtil.matchesIdList(orgUnitIds, form.getLicenseeOrgUnitId())
        && FilterUtil.listMatchesIdList(orgUnitIds, licenseeGroupOrgUnitIds);
  }

  private boolean matchesEventStatusCondition(
      LicenceEventCache eventCache,
      EventTrackerForm form,
      Map<UUID, ApplicationDisplayInfo> applicationDisplayInfoByApplicationId
  ) {
    if (CollectionUtils.isEmpty(form.getEventStatuses())) {
      return true;
    }

    var matchingApplicationStatuses = form.getEventStatuses().stream()
        .map(EventTrackerApplicationStatus::valueOf)
        .flatMap(status -> status.getApplicationStatuses().stream())
        .collect(Collectors.toSet());

    return Optional.ofNullable(eventCache.getApplicationId())
        .map(applicationDisplayInfoByApplicationId::get)
        .map(ApplicationDisplayInfo::status)
        .map(matchingApplicationStatuses::contains)
        .orElse(false);
  }

  private Map<Integer, List<String>> toLicenseeNamesByLicenceId(
      Map<Licence, List<OrganisationUnit>> responsibleOrganisationsByLicence
  ) {
    return responsibleOrganisationsByLicence.entrySet().stream()
        .collect(Collectors.toMap(
            entry -> entry.getKey().getId(),
            entry -> entry.getValue().stream()
                .map(OrganisationUnit::organisationUnitName)
                .toList()
        ));
  }

  private Map<Integer, List<Integer>> getLicenceIdToLicenseeOrgUnitIdMap(
      Map<Licence, List<OrganisationUnit>> responsibleOrganisationsByLicence
  ) {
    return responsibleOrganisationsByLicence.entrySet().stream()
        .collect(Collectors.toMap(
            entry -> entry.getKey().getId(),
            entry -> entry.getValue().stream()
                .map(OrganisationUnit::organisationUnitId)
                .toList()
        ));
  }

  private SortableTableRow toRow(
      LicenceEventCache eventCache,
      Map<Integer, List<String>> licenseesByLicenceId,
      Map<UUID, ApplicationDisplayInfo> applicationDisplayInfoByApplicationId,
      Map<Long, String> stewardNameByWuaId
  ) {
    var eventDate = eventCache.getEventDate() != null
        ? DateFormatUtil.convertToDisplayText(eventCache.getEventDate())
        : "";
    var eventDateSort = eventCache.getEventDate() != null
        ? eventCache.getEventDate().toString()
        : null;
    var workProgrammeActivity = eventCache.getActivityType() != null
        ? eventCache.getActivityType()
        : "";
    var licensees = String.join(", ", licenseesByLicenceId.getOrDefault(eventCache.getLicenceId(), List.of()));

    var applicationDisplayInfo = Optional.ofNullable(eventCache.getApplicationId())
        .map(applicationDisplayInfoByApplicationId::get);

    var applicationStatus = applicationDisplayInfo
        .map(ApplicationDisplayInfo::status)
        .map(ApplicationStatus::getDisplayName)
        .orElse("");

    var steward = Optional.ofNullable(eventCache.getStewardWuaId())
        .map(stewardNameByWuaId::get)
        .orElse("");

    var licenceValue = applicationDisplayInfo
        .map(info -> "%s (%s)".formatted(eventCache.getLicenceReference(), info.applicationReference()))
        .orElse(eventCache.getLicenceReference());

    var licenceLink = applicationDisplayInfo
        .map(ApplicationDisplayInfo::overviewLink)
        .orElseGet(() -> licenceOverviewLink(eventCache.getLicenceId()));

    var licenceTags = applicationDisplayInfo
        .map(info -> List.of(new Tag(
            eventCache.getApplicationType().getTagDisplayName(),
            eventCache.getApplicationType().getTagColour()
        )))
        .orElse(List.<Tag>of());

    return SortableTableRow.builder()
        .withValue(new SortableTableValue(licenceValue, null, licenceLink, licenceTags))
        .withValue(getTermPhaseTransition(eventCache))
        .withValue(workProgrammeActivity)
        .withValue(new SortableTableValue(eventDate, eventDateSort, null, List.of()))
        .withValue(applicationStatus)
        .withValue(licensees)
        .withValue(Objects.toString(eventCache.getQuadBlock(), ""))
        .withValue(steward)
        .build();
  }

  private String licenceOverviewLink(Integer licenceId) {
    return ReverseRouter.route(on(LicenceScheduleTabController.class)
        .renderLicenceOverview(licenceId, null, null, null));
  }

  private String applicationOverviewLink(UUID scheduleWorkProgrammeApplicationDetailId) {
    return ReverseRouter.route(on(ScheduleWorkProgrammeApplicationOverviewController.class)
        .renderOverview(scheduleWorkProgrammeApplicationDetailId, null, null, null));
  }

  private String continuationApplicationOverviewLink(UUID licenceContinuationApplicationDetailId) {
    return ReverseRouter.route(on(LicenceContinuationApplicationOverviewController.class)
        .renderOverview(licenceContinuationApplicationDetailId, null, null, null));
  }

  private String getTermPhaseTransition(LicenceEventCache eventCache) {
    if (StringUtils.isBlank(eventCache.getNextTermPhase())) {
      return eventCache.getCurrentTermPhase();
    }

    return "%s to %s".formatted(eventCache.getCurrentTermPhase(), eventCache.getNextTermPhase());
  }

  @Transactional
  public void refreshScheduleCache(LicenceScheduleDetail licenceScheduleDetail) {
    var licence = licenceScheduleDetail.getLicenceSchedule().getLicence();

    var existingEventCaches = licenceEventCacheRepository.getAllByLicenceId(licence.getId())
        .stream()
        .collect(StreamUtil.toLinkedHashMap(LicenceEventCache::getOriginalEventId, Function.identity()));

    var terms = licenceScheduleTermService.getTermsByLicenceScheduleDetail(licenceScheduleDetail);
    var phases = licenceSchedulePhaseService.getPhasesByLicenceScheduleDetail(licenceScheduleDetail);
    var workProgrammeActivities = workProgrammeActivityService.getWorkProgrammeActivities(licenceScheduleDetail);

    refreshLicenceTerms(licence, terms, phases, existingEventCaches);
    refreshLicencePhases(licence, terms, phases, existingEventCaches);
    refreshWorkProgrammeActivities(licence, workProgrammeActivities, terms, phases, existingEventCaches);
  }

  @Transactional
  public void refreshApplicationCache(LicenceApplication licenceApplication) {
    if (licenceApplication instanceof ScheduleWorkProgrammeApplication scheduleWorkProgrammeApplication) {
      refreshScheduleWorkProgrammeApplicationCache(scheduleWorkProgrammeApplication);
    } else if (licenceApplication instanceof LicenceContinuationApplication continuationApplication) {
      refreshContinuationApplicationCache(continuationApplication);
    }
  }

  private void refreshScheduleWorkProgrammeApplicationCache(ScheduleWorkProgrammeApplication application) {
    var applicationDetail = scheduleWorkProgrammeApplicationDetailQueryService
        .getLatestByApplication(application)
        .orElseThrow(() -> new LmsEntityNotFoundException(
            "schedule work programme application detail", application.getId()));

    var relatedEvents = getRelatedEvents(applicationDetail);
    if (relatedEvents.originalEventIds().isEmpty()) {
      return;
    }

    var relatedEventCaches = licenceEventCacheRepository.findAllByOriginalEventIdIn(relatedEvents.originalEventIds());

    var eventCachesToSave = relatedEventCaches.stream()
        .filter(eventCache -> canForkEventCache(
            eventCache, application, relatedEvents.originalEventIdsWithoutDurationUpdate()
        ))
        .map(eventCache -> applyApplicationToEventCache(eventCache, application, application.getStewardWuaId()))
        .toList();

    licenceEventCacheRepository.saveAll(eventCachesToSave);
  }

  private void refreshContinuationApplicationCache(LicenceContinuationApplication application) {
    var applicationDetail = licenceContinuationApplicationDetailQueryService
        .getLatestByApplication(application)
        .orElseThrow(() -> new LmsEntityNotFoundException(
            "licence continuation application detail", application.getId()));

    UUID originalEventId;
    if (applicationDetail.getCurrentPhase() != null) {
      originalEventId = applicationDetail.getCurrentPhase().getOriginalEventId();
    } else if (applicationDetail.getCurrentTerm() != null) {
      originalEventId = applicationDetail.getCurrentTerm().getOriginalEventId();
    } else {
      return;
    }

    var relatedEventCaches = licenceEventCacheRepository.findAllByOriginalEventIdIn(Set.of(originalEventId));
    if (relatedEventCaches.isEmpty()) {
      return;
    }

    // Duplicate original_event_id rows can exist by the time this runs (a fork left behind by a different
    // application). A continuation still only ever touches one row, so pick a row not already claimed by
    // another application to update in place, or, if every row is claimed, fork from any one of them.
    var targetEventCache = relatedEventCaches.stream()
        .filter(eventCache -> !isClaimedByAnotherApplication(eventCache, application))
        .findFirst()
        .orElseGet(() -> relatedEventCaches.get(0));

    licenceEventCacheRepository.save(applyApplicationToEventCache(targetEventCache, application, null));
  }

  private RelatedEvents getRelatedEvents(ScheduleWorkProgrammeApplicationDetail applicationDetail) {
    var originalEventIds = new HashSet<UUID>();
    var termOrPhaseLinkedActivityOriginalEventIds = new HashSet<UUID>();

    var extensionRequests = licenceScheduleExtensionRequestQueryService.getAllByApplicationDetail(applicationDetail);

    var requestedTerms = extensionRequests.stream()
        .map(LicenceScheduleExtensionRequest::getLicenceScheduleTerm)
        .filter(Objects::nonNull)
        .toList();
    var requestedPhases = extensionRequests.stream()
        .map(LicenceScheduleExtensionRequest::getLicenceSchedulePhase)
        .filter(Objects::nonNull)
        .toList();

    requestedTerms.forEach(term -> originalEventIds.add(term.getOriginalEventId()));
    requestedPhases.forEach(phase -> originalEventIds.add(phase.getOriginalEventId()));

    // Fetched once for every term/phase in the application rather than once per request, to avoid an
    // N+1 query per extension request when an application extends several terms or phases at once.
    var termAndPhaseLinkedActivities = Stream.concat(
        workProgrammeActivityService.getAllActivitiesLinkedToTerms(requestedTerms).stream(),
        workProgrammeActivityService.getAllActivitiesLinkedToPhases(requestedPhases).stream()
    ).toList();

    termAndPhaseLinkedActivities.forEach(activity -> {
      originalEventIds.add(activity.getOriginalEventId());
      termOrPhaseLinkedActivityOriginalEventIds.add(activity.getOriginalEventId());
    });

    // A work programme activity requested for amendment without a duration change (e.g. only additional
    // information) should not fork a competing row when another application already holds it - unlike an
    // activity whose due date is genuinely changing, showing it as a distinct version would be misleading.
    // This never overrides an activity reached via its term or phase (above), which forks unconditionally.
    var originalEventIdsWithoutDurationUpdate = new HashSet<UUID>();

    licenceWorkProgrammeAmendmentRequestQueryService.getAllByApplicationDetail(applicationDetail)
        .forEach(request -> {
          var activityOriginalEventId = request.getWorkProgrammeActivity().getOriginalEventId();
          originalEventIds.add(activityOriginalEventId);

          if (request.getWorkProgrammeExtensionDuration() == null) {
            originalEventIdsWithoutDurationUpdate.add(activityOriginalEventId);
          }
        });

    originalEventIdsWithoutDurationUpdate.removeAll(termOrPhaseLinkedActivityOriginalEventIds);

    return new RelatedEvents(originalEventIds, originalEventIdsWithoutDurationUpdate);
  }

  private boolean canForkEventCache(
      LicenceEventCache eventCache,
      LicenceApplication application,
      Set<UUID> originalEventIdsWithoutDurationUpdate
  ) {
    return !isClaimedByAnotherApplication(eventCache, application)
        || !originalEventIdsWithoutDurationUpdate.contains(eventCache.getOriginalEventId());
  }

  // If the row already belongs to a different application, it must be preserved for that application, so the
  // current application's data is written to a new row against the same event rather than overwriting it.
  private LicenceEventCache applyApplicationToEventCache(
      LicenceEventCache eventCache,
      LicenceApplication application,
      Long stewardWuaId
  ) {
    var targetEventCache = isClaimedByAnotherApplication(eventCache, application)
        ? copyEventCacheForNewApplication(eventCache)
        : eventCache;

    targetEventCache.setApplicationId(application.getId());
    targetEventCache.setApplicationType(application.getApplicationType());
    targetEventCache.setStewardWuaId(stewardWuaId);

    return targetEventCache;
  }

  private record RelatedEvents(Set<UUID> originalEventIds, Set<UUID> originalEventIdsWithoutDurationUpdate) {
  }

  private boolean isClaimedByAnotherApplication(LicenceEventCache eventCache, LicenceApplication application) {
    return eventCache.getApplicationId() != null && !eventCache.getApplicationId().equals(application.getId());
  }

  private LicenceEventCache copyEventCacheForNewApplication(LicenceEventCache eventCache) {
    var newEventCache = new LicenceEventCache();
    newEventCache.setLicenceId(eventCache.getLicenceId());
    newEventCache.setLicenceReference(eventCache.getLicenceReference());
    newEventCache.setOriginalEventId(eventCache.getOriginalEventId());
    newEventCache.setEventType(eventCache.getEventType());
    newEventCache.setCurrentTermPhase(eventCache.getCurrentTermPhase());
    newEventCache.setNextTermPhase(eventCache.getNextTermPhase());
    newEventCache.setActivityType(eventCache.getActivityType());
    newEventCache.setEventDate(eventCache.getEventDate());
    newEventCache.setQuadBlock(eventCache.getQuadBlock());
    return newEventCache;
  }

  private void refreshLicenceTerms(
      Licence licence,
      List<LicenceScheduleTerm> terms,
      List<LicenceSchedulePhase> phases,
      Map<UUID, LicenceEventCache> existingEventCaches
  ) {
    var termIdsWithPhases = phases.stream()
        .map(LicenceSchedulePhase::getLicenceScheduleTerm)
        .map(LicenceScheduleTerm::getId)
        .toList();

    terms.sort(Comparator.comparing(term -> term.getTermType().getDisplayOrder()));

    var termsToAdd = terms.stream()
        .filter(term -> !termIdsWithPhases.contains(term.getId()))
        .map(term -> createOrUpdateEventCacheForTerm(licence, term, terms, existingEventCaches))
        .toList();

    licenceEventCacheRepository.saveAll(termsToAdd);

    // A term that has gained phases since it was last cached should no longer have its own term-level
    // row - the phases now cover that transition individually. Remove any such now-stale cache entry.
    var staleTermCaches = terms.stream()
        .filter(term -> termIdsWithPhases.contains(term.getId()))
        .map(term -> existingEventCaches.get(term.getOriginalEventId()))
        .filter(cache -> cache != null && cache.getEventType() == ScheduleEventType.TERM)
        .toList();

    licenceEventCacheRepository.deleteAll(staleTermCaches);
  }

  private LicenceEventCache createOrUpdateEventCacheForTerm(
      Licence licence,
      LicenceScheduleTerm term,
      List<LicenceScheduleTerm> terms,
      Map<UUID, LicenceEventCache> existingEventCaches
  ) {
    var nextTermIndex = terms.indexOf(term) + 1;
    var nextTermPhase = nextTermIndex < terms.size()
        ? terms.get(nextTermIndex).getTermType().getDisplayName()
        : "";

    var cache = existingEventCaches.getOrDefault(term.getOriginalEventId(), new LicenceEventCache());
    cache.setLicenceId(licence.getId());
    cache.setLicenceReference(licence.getLicenceReference());
    cache.setOriginalEventId(term.getOriginalEventId());
    cache.setEventType(ScheduleEventType.TERM);
    cache.setCurrentTermPhase(term.getTermType().getDisplayName());
    cache.setNextTermPhase(nextTermPhase);
    cache.setEventDate(term.getEndDate());

    return cache;
  }

  private void refreshLicencePhases(
      Licence licence,
      List<LicenceScheduleTerm> terms,
      List<LicenceSchedulePhase> phases,
      Map<UUID, LicenceEventCache> existingEventCaches
  ) {
    phases.sort(Comparator.comparing(phase -> phase.getPhaseType().getDisplayOrder()));

    var phasesToAdd = phases.stream()
        .map(phase -> createOrUpdateEventCacheForPhase(licence, phase, terms, phases, existingEventCaches))
        .toList();

    licenceEventCacheRepository.saveAll(phasesToAdd);
  }

  private LicenceEventCache createOrUpdateEventCacheForPhase(
      Licence licence,
      LicenceSchedulePhase phase,
      List<LicenceScheduleTerm> terms,
      List<LicenceSchedulePhase> phases,
      Map<UUID, LicenceEventCache> existingEventCaches
  ) {
    var nextTermPhase = getNextPhaseInSameTerm(phase, phases)
        .map(nextPhase -> nextPhase.getPhaseType().getDisplayName())
        .orElseGet(() -> getNextTermDisplayName(terms, phase.getLicenceScheduleTerm().getId()));

    var cache = existingEventCaches.getOrDefault(phase.getOriginalEventId(), new LicenceEventCache());
    cache.setLicenceId(licence.getId());
    cache.setLicenceReference(licence.getLicenceReference());
    cache.setOriginalEventId(phase.getOriginalEventId());
    cache.setEventType(ScheduleEventType.PHASE);
    cache.setCurrentTermPhase(phase.getPhaseType().getDisplayName());
    cache.setNextTermPhase(nextTermPhase);
    cache.setEventDate(phase.getEndDate());

    return cache;
  }

  private void refreshWorkProgrammeActivities(
      Licence licence,
      List<WorkProgrammeActivity> workProgrammeActivities,
      List<LicenceScheduleTerm> terms,
      List<LicenceSchedulePhase> phases,
      Map<UUID, LicenceEventCache> existingEventCaches
  ) {
    var activitiesToAdd = workProgrammeActivities.stream()
        .map(activity -> createOrUpdateEventCacheForActivity(licence, activity, terms, phases, existingEventCaches))
        .toList();

    licenceEventCacheRepository.saveAll(activitiesToAdd);
  }

  private LicenceEventCache createOrUpdateEventCacheForActivity(
      Licence licence,
      WorkProgrammeActivity workProgrammeActivity,
      List<LicenceScheduleTerm> terms,
      List<LicenceSchedulePhase> phases,
      Map<UUID, LicenceEventCache> existingEventCaches
  ) {
    var cache = existingEventCaches.getOrDefault(workProgrammeActivity.getOriginalEventId(), new LicenceEventCache());
    cache.setLicenceId(licence.getId());
    cache.setLicenceReference(licence.getLicenceReference());
    cache.setOriginalEventId(workProgrammeActivity.getOriginalEventId());
    cache.setEventType(ScheduleEventType.WORK_PROGRAMME_ACTIVITY);
    cache.setActivityType(workProgrammeActivity.getCategoryString());
    cache.setEventDate(getWorkProgrammeActivityDate(workProgrammeActivity));

    if (!workProgrammeActivity.getDateOption().equals(WorkProgrammeActivityDateOption.RELATIVE_DATE)) {
      cache.setCurrentTermPhase(getWorkProgrammeActivityCurrentTermPhase(workProgrammeActivity));
      cache.setNextTermPhase(getWorkProgrammeActivityNextTermPhase(workProgrammeActivity, terms, phases));
    }

    return cache;
  }

  private LocalDate getWorkProgrammeActivityDate(WorkProgrammeActivity workProgrammeActivity) {
    if (workProgrammeActivity.getDateOption().equals(WorkProgrammeActivityDateOption.RELATIVE_DATE)) {
      return workProgrammeActivity.getDueDate();
    }

    if (workProgrammeActivity.getDateOption().equals(WorkProgrammeActivityDateOption.WITHIN_A_TERM)) {
      return workProgrammeActivity.getLicenceScheduleTerm().getEndDate();
    }

    return workProgrammeActivity.getLicenceSchedulePhase().getEndDate();
  }

  private String getWorkProgrammeActivityCurrentTermPhase(WorkProgrammeActivity workProgrammeActivity) {
    if (workProgrammeActivity.getDateOption().equals(WorkProgrammeActivityDateOption.WITHIN_A_TERM)) {
      return workProgrammeActivity.getLicenceScheduleTerm().getTermType().getDisplayName();
    }

    return workProgrammeActivity.getLicenceSchedulePhase().getPhaseType().getDisplayName();
  }

  private String getWorkProgrammeActivityNextTermPhase(
      WorkProgrammeActivity workProgrammeActivity,
      List<LicenceScheduleTerm> terms,
      List<LicenceSchedulePhase> phases
  ) {
    if (workProgrammeActivity.getDateOption().equals(WorkProgrammeActivityDateOption.WITHIN_A_PHASE)) {
      var phase = workProgrammeActivity.getLicenceSchedulePhase();
      return getNextPhaseInSameTerm(phase, phases)
          .map(nextPhase -> nextPhase.getPhaseType().getDisplayName())
          .orElseGet(() -> getNextTermDisplayName(terms, phase.getLicenceScheduleTerm().getId()));
    }

    return getNextTermDisplayName(terms, workProgrammeActivity.getLicenceScheduleTerm().getId());
  }

  private String getNextTermDisplayName(List<LicenceScheduleTerm> terms, UUID termId) {
    var termIds = terms.stream()
        .map(LicenceScheduleTerm::getId)
        .toList();

    var nextTermIndex = termIds.indexOf(termId) + 1;
    return nextTermIndex > 0 && nextTermIndex < terms.size()
        ? terms.get(nextTermIndex).getTermType().getDisplayName()
        : "";
  }

  private Optional<LicenceSchedulePhase> getNextPhaseInSameTerm(LicenceSchedulePhase phase, List<LicenceSchedulePhase> phases) {
    var phasesInTerm = phases.stream()
        .filter(candidate -> candidate.getLicenceScheduleTerm().getId().equals(phase.getLicenceScheduleTerm().getId()))
        .toList();

    var phaseIds = phasesInTerm.stream()
        .map(LicenceSchedulePhase::getId)
        .toList();

    var nextPhaseIndex = phaseIds.indexOf(phase.getId()) + 1;
    return nextPhaseIndex > 0 && nextPhaseIndex < phasesInTerm.size()
        ? Optional.of(phasesInTerm.get(nextPhaseIndex))
        : Optional.empty();
  }
}
