package uk.co.nstauthority.licensingmanagementservice.licence.crosslicenceeventtracker;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.fds.table.SortableTableRow;
import uk.co.nstauthority.licensingmanagementservice.fds.table.SortableTableValue;
import uk.co.nstauthority.licensingmanagementservice.fds.table.SortableTableView;
import uk.co.nstauthority.licensingmanagementservice.formatting.DateFormatUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceService;
import uk.co.nstauthority.licensingmanagementservice.licence.OrganisationUnit;
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
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.util.StreamUtil;

@Service
public class CrossLicenceEventTrackerService {

  private final LicenceScheduleTermService licenceScheduleTermService;
  private final LicenceSchedulePhaseService licenceSchedulePhaseService;
  private final LicenceEventCacheRepository licenceEventCacheRepository;
  private final LicenceService licenceService;
  private final LicenceResponsibleOrganisationService licenceResponsibleOrganisationService;
  private final WorkProgrammeActivityService workProgrammeActivityService;

  public CrossLicenceEventTrackerService(
      LicenceScheduleTermService licenceScheduleTermService,
      LicenceSchedulePhaseService licenceSchedulePhaseService,
      LicenceEventCacheRepository licenceEventCacheRepository,
      LicenceService licenceService,
      LicenceResponsibleOrganisationService licenceResponsibleOrganisationService,
      WorkProgrammeActivityService workProgrammeActivityService
  ) {
    this.licenceScheduleTermService = licenceScheduleTermService;
    this.licenceSchedulePhaseService = licenceSchedulePhaseService;
    this.licenceEventCacheRepository = licenceEventCacheRepository;
    this.licenceService = licenceService;
    this.licenceResponsibleOrganisationService = licenceResponsibleOrganisationService;
    this.workProgrammeActivityService = workProgrammeActivityService;
  }

  public SortableTableView getEventTrackerTable() {
    var eventCaches = licenceEventCacheRepository.findAll();

    var licenceIds = eventCaches.stream()
        .map(LicenceEventCache::getLicenceId)
        .distinct()
        .toList();

    var licenseesByLicenceId = getLicenseesByLicenceId(licenceIds);

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
        .withDefaultSortIndex(4);

    eventCaches.stream()
        .sorted(Comparator.comparing(LicenceEventCache::getEventDate, Comparator.nullsLast(Comparator.naturalOrder())))
        .forEach(eventCache -> tableBuilder.addRow(toRow(eventCache, licenseesByLicenceId)));

    return tableBuilder.build();
  }

  private Map<Integer, List<String>> getLicenseesByLicenceId(List<Integer> licenceIds) {
    var licences = licenceService.getLicencesByIds(licenceIds);

    return licenceResponsibleOrganisationService.getResponsibleOrganisationsByLicences(licences)
        .entrySet()
        .stream()
        .collect(Collectors.toMap(
            entry -> entry.getKey().getId(),
            entry -> entry.getValue().stream()
                .map(OrganisationUnit::organisationUnitName)
                .toList()
        ));
  }

  private SortableTableRow toRow(LicenceEventCache eventCache, Map<Integer, List<String>> licenseesByLicenceId) {
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

    var licenceLink = StringUtils.removeStart(
        ReverseRouter.route(on(LicenceScheduleTabController.class)
            .renderLicenceOverview(eventCache.getLicenceId(), null, null, null)),
        "/"
    );

    return SortableTableRow.builder()
        .withValue(new SortableTableValue(eventCache.getLicenceReference(), null, licenceLink, List.of()))
        .withValue(getTermPhaseTransition(eventCache))
        .withValue(workProgrammeActivity)
        .withValue(new SortableTableValue(eventDate, eventDateSort, null, List.of()))
        .withValue("")
        .withValue(licensees)
        .withValue(Objects.toString(eventCache.getQuadBlock(), ""))
        .withValue("")
        .build();
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
