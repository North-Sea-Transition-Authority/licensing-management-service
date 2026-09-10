package uk.co.nstauthority.licensingmanagementservice.licence.crosslicenceeventtracker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.fds.table.SortableTableRow;
import uk.co.nstauthority.licensingmanagementservice.fds.table.SortableTableValue;
import uk.co.nstauthority.licensingmanagementservice.fds.table.SortableTableView;
import uk.co.nstauthority.licensingmanagementservice.formatting.DateFormatUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceSchedulePhaseTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceScheduleTermTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceService;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.OrganisationUnit;
import uk.co.nstauthority.licensingmanagementservice.licence.PhaseType;
import uk.co.nstauthority.licensingmanagementservice.licence.TermType;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisationService;
import uk.co.nstauthority.licensingmanagementservice.licence.overview.LicenceScheduleTabController;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhase;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhaseService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTermService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline.ScheduleEventType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivity;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityCategory;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityDateOption;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@ExtendWith(MockitoExtension.class)
class CrossLicenceEventTrackerServiceTest {

  @Mock
  private LicenceScheduleTermService licenceScheduleTermService;

  @Mock
  private LicenceSchedulePhaseService licenceSchedulePhaseService;

  @Mock
  private LicenceEventCacheRepository licenceEventCacheRepository;

  @Mock
  private LicenceService licenceService;

  @Mock
  private LicenceResponsibleOrganisationService licenceResponsibleOrganisationService;

  @Mock
  private WorkProgrammeActivityService workProgrammeActivityService;

  @InjectMocks
  private CrossLicenceEventTrackerService crossLicenceEventTrackerService;

  @Captor
  private ArgumentCaptor<List<LicenceEventCache>> eventCacheListCaptor;

  @Test
  void getEventTrackerTable_whenNoEventCaches_thenOnlyHeadingRow() {
    when(licenceEventCacheRepository.findAll()).thenReturn(List.of());
    when(licenceService.getLicencesByIds(anyList())).thenReturn(List.of());
    when(licenceResponsibleOrganisationService.getResponsibleOrganisationsByLicences(anyList())).thenReturn(Map.of());

    var result = crossLicenceEventTrackerService.getEventTrackerTable();

    var expected = SortableTableView.sortableTableBuilder()
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
        .build();

    assertThat(result).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void getEventTrackerTable_whenEventCaches_thenRowsSortedByEventDateWithLicenseesPopulated() {
    var licenceWithOneLicensee = LicenceTestUtil.builder().withId(100).build();
    var licenceWithMultipleLicensees = LicenceTestUtil.builder().withId(200).build();
    var licenceWithNoLicensees = LicenceTestUtil.builder().withId(300).build();

    var earliestEventCache = buildEventCache(
        licenceWithOneLicensee.getId(),
        "P 111",
        LocalDate.of(2025, 6, 15),
        "Initial term",
        null,
        null,
        null
    );
    var latestEventCache = buildEventCache(
        licenceWithMultipleLicensees.getId(),
        "P 222",
        LocalDate.of(2026, 1, 1),
        "Phase A",
        "Phase B",
        WorkProgrammeActivityCategory.DRILL_WELL.getDisplayName(),
        "12/3"
    );
    var undatedEventCache = buildEventCache(
        licenceWithNoLicensees.getId(),
        "P 333",
        null,
        "Terms",
        null,
        null,
        null
    );

    when(licenceEventCacheRepository.findAll())
        .thenReturn(List.of(latestEventCache, undatedEventCache, earliestEventCache));

    var licences = List.of(licenceWithMultipleLicensees, licenceWithNoLicensees, licenceWithOneLicensee);
    when(licenceService.getLicencesByIds(anyList())).thenReturn(licences);
    when(licenceResponsibleOrganisationService.getResponsibleOrganisationsByLicences(eq(licences)))
        .thenReturn(Map.of(
            licenceWithOneLicensee, List.of(new OrganisationUnit(1, "Licensee One")),
            licenceWithMultipleLicensees, List.of(
                new OrganisationUnit(2, "Licensee Two"),
                new OrganisationUnit(3, "Licensee Three")
            )
        ));

    var result = crossLicenceEventTrackerService.getEventTrackerTable();

    var expected = SortableTableView.sortableTableBuilder()
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
        .addRow(SortableTableRow.builder()
            .withValue(licenceValue("P 111", licenceWithOneLicensee.getId()))
            .withValues(
                "Initial term",
                "",
                DateFormatUtil.convertToDisplayText(LocalDate.of(2025, 6, 15)),
                "",
                "Licensee One",
                "",
                ""
            )
            .build())
        .addRow(SortableTableRow.builder()
            .withValue(licenceValue("P 222", licenceWithMultipleLicensees.getId()))
            .withValues(
                "Phase A to Phase B",
                WorkProgrammeActivityCategory.DRILL_WELL.getDisplayName(),
                DateFormatUtil.convertToDisplayText(LocalDate.of(2026, 1, 1)),
                "",
                "Licensee Two, Licensee Three",
                "12/3",
                ""
            )
            .build())
        .addRow(SortableTableRow.builder()
            .withValue(licenceValue("P 333", licenceWithNoLicensees.getId()))
            .withValues(
                "Terms",
                "",
                "",
                "",
                "",
                "",
                ""
            )
            .build())
        .build();

    assertThat(result).usingRecursiveComparison().isEqualTo(expected);
  }

  private SortableTableValue licenceValue(String licenceReference, Integer licenceId) {
    var licenceLink = StringUtils.removeStart(
        ReverseRouter.route(on(LicenceScheduleTabController.class).renderLicenceOverview(licenceId, null, null, null)),
        "/"
    );
    return new SortableTableValue(licenceReference, licenceLink, List.of());
  }

  private LicenceEventCache buildEventCache(
      Integer licenceId,
      String licenceReference,
      LocalDate eventDate,
      String currentTermPhase,
      String nextTermPhase,
      String activityType,
      String quadBlock
  ) {
    var eventCache = new LicenceEventCache();
    eventCache.setLicenceId(licenceId);
    eventCache.setLicenceReference(licenceReference);
    eventCache.setEventType(ScheduleEventType.TERM);
    eventCache.setEventDate(eventDate);
    eventCache.setCurrentTermPhase(currentTermPhase);
    eventCache.setNextTermPhase(nextTermPhase);
    eventCache.setActivityType(activityType);
    eventCache.setQuadBlock(quadBlock);
    return eventCache;
  }

  @Test
  void refreshScheduleCache_whenNoExistingCaches_thenSavesCachesForTermsWithoutPhasesAndAllPhases() {
    var licence = LicenceTestUtil.builder().withId(100).withLicenceReference("P 111").build();
    var licenceScheduleDetail = LicenceScheduleTestUtil.createLicenceScheduleDetail(
        LicenceScheduleTestUtil.createLicenceSchedule(licence)
    );

    var initialTerm = buildTerm(TermType.INITIAL, LocalDate.of(2030, 1, 1));
    var secondTerm = buildTerm(TermType.SECOND, LocalDate.of(2035, 1, 1));
    var thirdTerm = buildTerm(TermType.THIRD, LocalDate.of(2040, 1, 1));

    var phaseOfSecondTerm = buildPhase(secondTerm, PhaseType.PHASE_A, LocalDate.of(2032, 1, 1));

    when(licenceEventCacheRepository.getAllByLicenceId(licence.getId())).thenReturn(List.of());
    when(licenceScheduleTermService.getTermsByLicenceScheduleDetail(licenceScheduleDetail))
        .thenReturn(new ArrayList<>(List.of(thirdTerm, initialTerm, secondTerm)));
    when(licenceSchedulePhaseService.getPhasesByLicenceScheduleDetail(licenceScheduleDetail))
        .thenReturn(new ArrayList<>(List.of(phaseOfSecondTerm)));
    when(workProgrammeActivityService.getWorkProgrammeActivities(licenceScheduleDetail)).thenReturn(List.of());

    crossLicenceEventTrackerService.refreshScheduleCache(licenceScheduleDetail);

    verify(licenceEventCacheRepository, times(3)).saveAll(eventCacheListCaptor.capture());
    var savedTermCaches = eventCacheListCaptor.getAllValues().get(0);
    var savedPhaseCaches = eventCacheListCaptor.getAllValues().get(1);

    assertThat(savedTermCaches)
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactly(
            buildExpectedEventCache(
                licence, initialTerm.getOriginalEventId(), ScheduleEventType.TERM,
                "Initial Term", "Second Term", initialTerm.getEndDate()
            ),
            buildExpectedEventCache(
                licence, thirdTerm.getOriginalEventId(), ScheduleEventType.TERM,
                "Third Term", "", thirdTerm.getEndDate()
            )
        );

    assertThat(savedPhaseCaches)
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactly(
            buildExpectedEventCache(
                licence, phaseOfSecondTerm.getOriginalEventId(), ScheduleEventType.PHASE,
                "Phase A", "Third Term", phaseOfSecondTerm.getEndDate()
            )
        );
  }

  @Test
  void refreshScheduleCache_whenExistingCacheForOriginalEventId_thenUpdatesInPlace() {
    var licence = LicenceTestUtil.builder().withId(100).withLicenceReference("P 111").build();
    var licenceScheduleDetail = LicenceScheduleTestUtil.createLicenceScheduleDetail(
        LicenceScheduleTestUtil.createLicenceSchedule(licence)
    );

    var initialTerm = buildTerm(TermType.INITIAL, LocalDate.of(2030, 1, 1));

    var existingCacheId = UUID.randomUUID();
    var existingCache = new LicenceEventCache();
    existingCache.setId(existingCacheId);
    existingCache.setOriginalEventId(initialTerm.getOriginalEventId());

    when(licenceEventCacheRepository.getAllByLicenceId(licence.getId())).thenReturn(List.of(existingCache));
    when(licenceScheduleTermService.getTermsByLicenceScheduleDetail(licenceScheduleDetail))
        .thenReturn(new ArrayList<>(List.of(initialTerm)));
    when(licenceSchedulePhaseService.getPhasesByLicenceScheduleDetail(licenceScheduleDetail))
        .thenReturn(new ArrayList<>());
    when(workProgrammeActivityService.getWorkProgrammeActivities(licenceScheduleDetail)).thenReturn(List.of());

    crossLicenceEventTrackerService.refreshScheduleCache(licenceScheduleDetail);

    verify(licenceEventCacheRepository, times(3)).saveAll(eventCacheListCaptor.capture());
    var savedTermCaches = eventCacheListCaptor.getAllValues().get(0);

    assertThat(savedTermCaches).hasSize(1);
    assertThat(savedTermCaches.get(0).getId()).isEqualTo(existingCacheId);
  }

  @Test
  void refreshScheduleCache_whenPhaseIsFollowedByAnotherPhaseInSameTerm_thenNextTermPhaseIsThatPhase() {
    var licence = LicenceTestUtil.builder().withId(100).withLicenceReference("P 111").build();
    var licenceScheduleDetail = LicenceScheduleTestUtil.createLicenceScheduleDetail(
        LicenceScheduleTestUtil.createLicenceSchedule(licence)
    );

    var initialTerm = buildTerm(TermType.INITIAL, LocalDate.of(2030, 1, 1));
    var secondTerm = buildTerm(TermType.SECOND, LocalDate.of(2035, 1, 1));

    var phaseA = buildPhase(initialTerm, PhaseType.PHASE_A, LocalDate.of(2027, 1, 1));
    var phaseB = buildPhase(initialTerm, PhaseType.PHASE_B, LocalDate.of(2028, 1, 1));

    when(licenceEventCacheRepository.getAllByLicenceId(licence.getId())).thenReturn(List.of());
    when(licenceScheduleTermService.getTermsByLicenceScheduleDetail(licenceScheduleDetail))
        .thenReturn(new ArrayList<>(List.of(secondTerm, initialTerm)));
    when(licenceSchedulePhaseService.getPhasesByLicenceScheduleDetail(licenceScheduleDetail))
        .thenReturn(new ArrayList<>(List.of(phaseB, phaseA)));
    when(workProgrammeActivityService.getWorkProgrammeActivities(licenceScheduleDetail)).thenReturn(List.of());

    crossLicenceEventTrackerService.refreshScheduleCache(licenceScheduleDetail);

    verify(licenceEventCacheRepository, times(3)).saveAll(eventCacheListCaptor.capture());
    var savedPhaseCaches = eventCacheListCaptor.getAllValues().get(1);

    assertThat(savedPhaseCaches)
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactly(
            buildExpectedEventCache(
                licence, phaseA.getOriginalEventId(), ScheduleEventType.PHASE,
                "Phase A", "Phase B", phaseA.getEndDate()
            ),
            buildExpectedEventCache(
                licence, phaseB.getOriginalEventId(), ScheduleEventType.PHASE,
                "Phase B", "Second Term", phaseB.getEndDate()
            )
        );
  }

  @Test
  void refreshScheduleCache_whenWorkProgrammeActivities_thenSavesActivityCachesWithResolvedDates() {
    var licence = LicenceTestUtil.builder().withId(100).withLicenceReference("P 111").build();
    var licenceScheduleDetail = LicenceScheduleTestUtil.createLicenceScheduleDetail(
        LicenceScheduleTestUtil.createLicenceSchedule(licence)
    );

    var initialTerm = buildTerm(TermType.INITIAL, LocalDate.of(2030, 1, 1));
    var secondTerm = buildTerm(TermType.SECOND, LocalDate.of(2035, 1, 1));
    var phase = buildPhase(initialTerm, PhaseType.PHASE_A, LocalDate.of(2028, 6, 1));

    var withinTermActivity = buildWorkProgrammeActivity(
        WorkProgrammeActivityCategory.DRILL_WELL, null, WorkProgrammeActivityDateOption.WITHIN_A_TERM, initialTerm, null, null
    );
    var withinPhaseActivity = buildWorkProgrammeActivity(
        WorkProgrammeActivityCategory.DRILL_OR_DROP_WELL, null, WorkProgrammeActivityDateOption.WITHIN_A_PHASE, null, phase, null
    );
    var relativeDateActivity = buildWorkProgrammeActivity(
        WorkProgrammeActivityCategory.OTHER_ACTIVITY, "Custom activity", WorkProgrammeActivityDateOption.RELATIVE_DATE,
        null, null, LocalDate.of(2029, 3, 15)
    );

    when(licenceEventCacheRepository.getAllByLicenceId(licence.getId())).thenReturn(List.of());
    when(licenceScheduleTermService.getTermsByLicenceScheduleDetail(licenceScheduleDetail))
        .thenReturn(new ArrayList<>(List.of(initialTerm, secondTerm)));
    when(licenceSchedulePhaseService.getPhasesByLicenceScheduleDetail(licenceScheduleDetail)).thenReturn(new ArrayList<>());
    when(workProgrammeActivityService.getWorkProgrammeActivities(licenceScheduleDetail))
        .thenReturn(List.of(withinTermActivity, withinPhaseActivity, relativeDateActivity));

    crossLicenceEventTrackerService.refreshScheduleCache(licenceScheduleDetail);

    verify(licenceEventCacheRepository, times(3)).saveAll(eventCacheListCaptor.capture());
    var savedActivityCaches = eventCacheListCaptor.getAllValues().get(2);

    assertThat(savedActivityCaches)
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactly(
            buildExpectedActivityEventCache(
                licence, withinTermActivity.getOriginalEventId(),
                WorkProgrammeActivityCategory.DRILL_WELL.getDisplayName(), initialTerm.getEndDate(),
                "Initial Term", "Second Term"
            ),
            buildExpectedActivityEventCache(
                licence, withinPhaseActivity.getOriginalEventId(),
                WorkProgrammeActivityCategory.DRILL_OR_DROP_WELL.getDisplayName(), phase.getEndDate(),
                "Phase A", "Second Term"
            ),
            buildExpectedActivityEventCache(
                licence, relativeDateActivity.getOriginalEventId(),
                "Custom activity", relativeDateActivity.getDueDate(),
                null, null
            )
        );
  }

  private WorkProgrammeActivity buildWorkProgrammeActivity(
      WorkProgrammeActivityCategory category,
      String otherCategoryName,
      WorkProgrammeActivityDateOption dateOption,
      LicenceScheduleTerm term,
      LicenceSchedulePhase phase,
      LocalDate dueDate
  ) {
    var activity = new WorkProgrammeActivity();
    activity.setId(UUID.randomUUID());
    activity.setOriginalEventId(UUID.randomUUID());
    activity.setCategory(category);
    activity.setOtherCategoryName(otherCategoryName);
    activity.setDateOption(dateOption);
    activity.setLicenceScheduleTerm(term);
    activity.setLicenceSchedulePhase(phase);
    activity.setDueDate(dueDate);
    return activity;
  }

  private LicenceEventCache buildExpectedActivityEventCache(
      Licence licence,
      UUID originalEventId,
      String activityType,
      LocalDate eventDate,
      String currentTermPhase,
      String nextTermPhase
  ) {
    var eventCache = new LicenceEventCache();
    eventCache.setLicenceId(licence.getId());
    eventCache.setLicenceReference(licence.getLicenceReference());
    eventCache.setOriginalEventId(originalEventId);
    eventCache.setEventType(ScheduleEventType.WORK_PROGRAMME_ACTIVITY);
    eventCache.setActivityType(activityType);
    eventCache.setEventDate(eventDate);
    eventCache.setCurrentTermPhase(currentTermPhase);
    eventCache.setNextTermPhase(nextTermPhase);
    return eventCache;
  }

  private LicenceScheduleTerm buildTerm(TermType termType, LocalDate endDate) {
    var term = LicenceScheduleTermTestUtil.builder()
        .withId(UUID.randomUUID())
        .withTermType(termType)
        .withEndDate(endDate)
        .build();
    term.setOriginalEventId(UUID.randomUUID());
    return term;
  }

  private LicenceSchedulePhase buildPhase(LicenceScheduleTerm term, PhaseType phaseType, LocalDate endDate) {
    var phase = LicenceSchedulePhaseTestUtil.builder()
        .withId(UUID.randomUUID())
        .withLicenceScheduleTerm(term)
        .withPhaseType(phaseType)
        .withEndDate(endDate)
        .build();
    phase.setOriginalEventId(UUID.randomUUID());
    return phase;
  }

  private LicenceEventCache buildExpectedEventCache(
      Licence licence,
      UUID originalEventId,
      ScheduleEventType eventType,
      String currentTermPhase,
      String nextTermPhase,
      LocalDate eventDate
  ) {
    var eventCache = new LicenceEventCache();
    eventCache.setLicenceId(licence.getId());
    eventCache.setLicenceReference(licence.getLicenceReference());
    eventCache.setOriginalEventId(originalEventId);
    eventCache.setEventType(eventType);
    eventCache.setCurrentTermPhase(currentTermPhase);
    eventCache.setNextTermPhase(nextTermPhase);
    eventCache.setEventDate(eventDate);
    return eventCache;
  }
}
