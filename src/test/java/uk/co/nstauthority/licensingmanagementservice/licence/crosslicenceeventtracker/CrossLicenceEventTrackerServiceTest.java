package uk.co.nstauthority.licensingmanagementservice.licence.crosslicenceeventtracker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.fds.table.SortableTableRow;
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
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhase;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhaseService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTermService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline.ScheduleEventType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityCategory;

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
        WorkProgrammeActivityCategory.DRILL_WELL,
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
            .withValues(
                "P 111",
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
            .withValues(
                "P 222",
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
            .withValues(
                "P 333",
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

  private LicenceEventCache buildEventCache(
      Integer licenceId,
      String licenceReference,
      LocalDate eventDate,
      String currentTermPhase,
      String nextTermPhase,
      WorkProgrammeActivityCategory activityType,
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

    crossLicenceEventTrackerService.refreshScheduleCache(licenceScheduleDetail);

    verify(licenceEventCacheRepository, times(2)).saveAll(eventCacheListCaptor.capture());
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

    crossLicenceEventTrackerService.refreshScheduleCache(licenceScheduleDetail);

    verify(licenceEventCacheRepository, times(2)).saveAll(eventCacheListCaptor.capture());
    var savedTermCaches = eventCacheListCaptor.getAllValues().get(0);

    assertThat(savedTermCaches).hasSize(1);
    assertThat(savedTermCaches.get(0).getId()).isEqualTo(existingCacheId);
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
