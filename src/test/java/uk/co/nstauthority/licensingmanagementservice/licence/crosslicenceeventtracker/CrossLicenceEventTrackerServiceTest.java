package uk.co.nstauthority.licensingmanagementservice.licence.crosslicenceeventtracker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDuration;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceApplication;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceSchedulePhaseTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceScheduleTermTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceService;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.PhaseType;
import uk.co.nstauthority.licensingmanagementservice.licence.TermType;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationType;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisationService;
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
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplication;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetailRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney.LicenceWorkProgrammeAmendmentRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney.LicenceWorkProgrammeAmendmentRequest;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.extendjourney.LicenceScheduleExtensionRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.extendjourney.LicenceScheduleExtensionRequest;

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

  @Mock
  private ScheduleWorkProgrammeApplicationDetailRepository scheduleWorkProgrammeApplicationDetailRepository;

  @Mock
  private LicenceScheduleExtensionRepository licenceScheduleExtensionRepository;

  @Mock
  private LicenceWorkProgrammeAmendmentRepository licenceWorkProgrammeAmendmentRepository;

  @InjectMocks
  private CrossLicenceEventTrackerService crossLicenceEventTrackerService;

  @Captor
  private ArgumentCaptor<List<LicenceEventCache>> eventCacheListCaptor;

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
  void refreshScheduleCache_whenTermGainsPhases_thenStaleTermLevelCacheIsDeleted() {
    var licence = LicenceTestUtil.builder().withId(100).withLicenceReference("P 111").build();
    var licenceScheduleDetail = LicenceScheduleTestUtil.createLicenceScheduleDetail(
        LicenceScheduleTestUtil.createLicenceSchedule(licence)
    );

    var initialTerm = buildTerm(TermType.INITIAL, LocalDate.of(2030, 1, 1));
    var phaseA = buildPhase(initialTerm, PhaseType.PHASE_A, LocalDate.of(2027, 1, 1));

    var staleTermCache = new LicenceEventCache();
    staleTermCache.setId(UUID.randomUUID());
    staleTermCache.setOriginalEventId(initialTerm.getOriginalEventId());
    staleTermCache.setEventType(ScheduleEventType.TERM);

    when(licenceEventCacheRepository.getAllByLicenceId(licence.getId())).thenReturn(List.of(staleTermCache));
    when(licenceScheduleTermService.getTermsByLicenceScheduleDetail(licenceScheduleDetail))
        .thenReturn(new ArrayList<>(List.of(initialTerm)));
    when(licenceSchedulePhaseService.getPhasesByLicenceScheduleDetail(licenceScheduleDetail))
        .thenReturn(new ArrayList<>(List.of(phaseA)));
    when(workProgrammeActivityService.getWorkProgrammeActivities(licenceScheduleDetail)).thenReturn(List.of());

    crossLicenceEventTrackerService.refreshScheduleCache(licenceScheduleDetail);

    verify(licenceEventCacheRepository, times(3)).saveAll(eventCacheListCaptor.capture());
    var savedTermCaches = eventCacheListCaptor.getAllValues().get(0);
    assertThat(savedTermCaches).isEmpty();

    verify(licenceEventCacheRepository).deleteAll(List.of(staleTermCache));
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

  @Test
  void refreshApplicationCache_whenNotScheduleAmendmentApplication_thenDoesNothing() {
    var licenceApplication = mock(LicenceApplication.class);

    crossLicenceEventTrackerService.refreshApplicationCache(licenceApplication);

    verifyNoInteractions(scheduleWorkProgrammeApplicationDetailRepository, licenceEventCacheRepository);
  }

  @Test
  void refreshApplicationCache_whenEventCacheUnclaimed_thenClaimsRowInPlace() {
    var application = buildApplication();
    var applicationDetail = new ScheduleWorkProgrammeApplicationDetail();

    var term = buildTerm(TermType.INITIAL, LocalDate.of(2030, 1, 1));
    var extensionRequest = new LicenceScheduleExtensionRequest();
    extensionRequest.setLicenceScheduleTerm(term);

    var existingCacheId = UUID.randomUUID();
    var existingCache = new LicenceEventCache();
    existingCache.setId(existingCacheId);
    existingCache.setOriginalEventId(term.getOriginalEventId());
    existingCache.setLicenceId(100);

    when(scheduleWorkProgrammeApplicationDetailRepository.getFirstByScheduleWorkProgrammeApplicationOrderByVersionNumberDesc(application))
        .thenReturn(Optional.of(applicationDetail));
    when(licenceScheduleExtensionRepository.findAllByScheduleWorkProgrammeApplicationDetails(applicationDetail))
        .thenReturn(List.of(extensionRequest));
    when(licenceWorkProgrammeAmendmentRepository.findAllByScheduleWorkProgrammeApplicationDetails(applicationDetail))
        .thenReturn(List.of());
    when(licenceEventCacheRepository.findAllByOriginalEventIdIn(Set.of(term.getOriginalEventId())))
        .thenReturn(List.of(existingCache));

    crossLicenceEventTrackerService.refreshApplicationCache(application);

    verify(licenceEventCacheRepository).saveAll(eventCacheListCaptor.capture());
    var savedCaches = eventCacheListCaptor.getValue();

    assertThat(savedCaches).hasSize(1);
    var savedCache = savedCaches.get(0);
    assertThat(savedCache.getId()).isEqualTo(existingCacheId);
    assertThat(savedCache.getApplicationId()).isEqualTo(application.getId());
    assertThat(savedCache.getApplicationType()).isEqualTo(ApplicationType.SCHEDULE_AMENDMENT_APPLICATION);
    assertThat(savedCache.getStewardWuaId()).isEqualTo(application.getStewardWuaId());
  }

  @Test
  void refreshApplicationCache_whenEventCacheClaimedByDifferentApplication_thenCreatesNewRow() {
    var application = buildApplication();
    var applicationDetail = new ScheduleWorkProgrammeApplicationDetail();

    var activity = new WorkProgrammeActivity();
    activity.setId(UUID.randomUUID());
    activity.setOriginalEventId(UUID.randomUUID());

    var amendmentRequest = new LicenceWorkProgrammeAmendmentRequest();
    amendmentRequest.setWorkProgrammeActivity(activity);
    amendmentRequest.setWorkProgrammeExtensionDuration(new ThreeFieldDuration(1, 0, 0));

    var existingCache = new LicenceEventCache();
    existingCache.setId(UUID.randomUUID());
    existingCache.setOriginalEventId(activity.getOriginalEventId());
    existingCache.setLicenceId(100);
    existingCache.setLicenceReference("P 111");
    existingCache.setEventType(ScheduleEventType.WORK_PROGRAMME_ACTIVITY);
    existingCache.setActivityType("Drill well");
    existingCache.setEventDate(LocalDate.of(2030, 1, 1));
    existingCache.setApplicationId(UUID.randomUUID());
    existingCache.setApplicationType(ApplicationType.SCHEDULE_AMENDMENT_APPLICATION);

    when(scheduleWorkProgrammeApplicationDetailRepository.getFirstByScheduleWorkProgrammeApplicationOrderByVersionNumberDesc(application))
        .thenReturn(Optional.of(applicationDetail));
    when(licenceScheduleExtensionRepository.findAllByScheduleWorkProgrammeApplicationDetails(applicationDetail))
        .thenReturn(List.of());
    when(licenceWorkProgrammeAmendmentRepository.findAllByScheduleWorkProgrammeApplicationDetails(applicationDetail))
        .thenReturn(List.of(amendmentRequest));
    when(licenceEventCacheRepository.findAllByOriginalEventIdIn(Set.of(activity.getOriginalEventId())))
        .thenReturn(List.of(existingCache));

    crossLicenceEventTrackerService.refreshApplicationCache(application);

    verify(licenceEventCacheRepository).saveAll(eventCacheListCaptor.capture());
    var savedCaches = eventCacheListCaptor.getValue();

    assertThat(savedCaches).hasSize(1);
    var savedCache = savedCaches.get(0);
    assertThat(savedCache.getId()).isNull();
    assertThat(savedCache.getLicenceId()).isEqualTo(existingCache.getLicenceId());
    assertThat(savedCache.getLicenceReference()).isEqualTo(existingCache.getLicenceReference());
    assertThat(savedCache.getOriginalEventId()).isEqualTo(existingCache.getOriginalEventId());
    assertThat(savedCache.getEventType()).isEqualTo(existingCache.getEventType());
    assertThat(savedCache.getActivityType()).isEqualTo(existingCache.getActivityType());
    assertThat(savedCache.getEventDate()).isEqualTo(existingCache.getEventDate());
    assertThat(savedCache.getApplicationId()).isEqualTo(application.getId());
    assertThat(savedCache.getApplicationType()).isEqualTo(ApplicationType.SCHEDULE_AMENDMENT_APPLICATION);
    assertThat(savedCache.getStewardWuaId()).isEqualTo(application.getStewardWuaId());

    verify(licenceEventCacheRepository, never()).delete(existingCache);
  }

  @Test
  void refreshApplicationCache_whenAmendedActivityWithoutDurationUpdateClaimedByDifferentApplication_thenLeftAsIs() {
    var application = buildApplication();
    var applicationDetail = new ScheduleWorkProgrammeApplicationDetail();

    var activity = new WorkProgrammeActivity();
    activity.setId(UUID.randomUUID());
    activity.setOriginalEventId(UUID.randomUUID());

    var amendmentRequest = new LicenceWorkProgrammeAmendmentRequest();
    amendmentRequest.setWorkProgrammeActivity(activity);
    amendmentRequest.setWorkProgrammeChangeRequested(true);

    var otherApplicationId = UUID.randomUUID();
    var existingCache = new LicenceEventCache();
    existingCache.setId(UUID.randomUUID());
    existingCache.setOriginalEventId(activity.getOriginalEventId());
    existingCache.setLicenceId(100);
    existingCache.setApplicationId(otherApplicationId);
    existingCache.setApplicationType(ApplicationType.SCHEDULE_AMENDMENT_APPLICATION);

    when(scheduleWorkProgrammeApplicationDetailRepository.getFirstByScheduleWorkProgrammeApplicationOrderByVersionNumberDesc(application))
        .thenReturn(Optional.of(applicationDetail));
    when(licenceScheduleExtensionRepository.findAllByScheduleWorkProgrammeApplicationDetails(applicationDetail))
        .thenReturn(List.of());
    when(licenceWorkProgrammeAmendmentRepository.findAllByScheduleWorkProgrammeApplicationDetails(applicationDetail))
        .thenReturn(List.of(amendmentRequest));
    when(licenceEventCacheRepository.findAllByOriginalEventIdIn(Set.of(activity.getOriginalEventId())))
        .thenReturn(List.of(existingCache));

    crossLicenceEventTrackerService.refreshApplicationCache(application);

    verify(licenceEventCacheRepository).saveAll(eventCacheListCaptor.capture());
    assertThat(eventCacheListCaptor.getValue()).isEmpty();
    assertThat(existingCache.getApplicationId()).isEqualTo(otherApplicationId);
  }

  @Test
  void refreshApplicationCache_whenTermLinkedActivityClaimedByDifferentApplicationWithoutDurationUpdate_thenStillForksNewRow() {
    var application = buildApplication();
    var applicationDetail = new ScheduleWorkProgrammeApplicationDetail();

    var term = buildTerm(TermType.INITIAL, LocalDate.of(2030, 1, 1));
    var extensionRequest = new LicenceScheduleExtensionRequest();
    extensionRequest.setLicenceScheduleTerm(term);

    var linkedActivity = new WorkProgrammeActivity();
    linkedActivity.setId(UUID.randomUUID());
    linkedActivity.setOriginalEventId(UUID.randomUUID());
    linkedActivity.setLicenceScheduleTerm(term);

    var termCache = new LicenceEventCache();
    termCache.setId(UUID.randomUUID());
    termCache.setOriginalEventId(term.getOriginalEventId());
    termCache.setLicenceId(100);

    var otherApplicationId = UUID.randomUUID();
    var activityCache = new LicenceEventCache();
    activityCache.setId(UUID.randomUUID());
    activityCache.setOriginalEventId(linkedActivity.getOriginalEventId());
    activityCache.setLicenceId(100);
    activityCache.setApplicationId(otherApplicationId);
    activityCache.setApplicationType(ApplicationType.SCHEDULE_AMENDMENT_APPLICATION);

    when(scheduleWorkProgrammeApplicationDetailRepository.getFirstByScheduleWorkProgrammeApplicationOrderByVersionNumberDesc(application))
        .thenReturn(Optional.of(applicationDetail));
    when(licenceScheduleExtensionRepository.findAllByScheduleWorkProgrammeApplicationDetails(applicationDetail))
        .thenReturn(List.of(extensionRequest));
    when(workProgrammeActivityService.getAllActivitiesLinkedTo(term))
        .thenReturn(List.of(linkedActivity));
    when(licenceWorkProgrammeAmendmentRepository.findAllByScheduleWorkProgrammeApplicationDetails(applicationDetail))
        .thenReturn(List.of());
    when(licenceEventCacheRepository.findAllByOriginalEventIdIn(
        Set.of(term.getOriginalEventId(), linkedActivity.getOriginalEventId())
    )).thenReturn(List.of(termCache, activityCache));

    crossLicenceEventTrackerService.refreshApplicationCache(application);

    verify(licenceEventCacheRepository).saveAll(eventCacheListCaptor.capture());
    var savedCaches = eventCacheListCaptor.getValue();

    assertThat(savedCaches)
        .extracting(LicenceEventCache::getOriginalEventId)
        .containsExactlyInAnyOrder(term.getOriginalEventId(), linkedActivity.getOriginalEventId());

    var forkedActivityCache = savedCaches.stream()
        .filter(cache -> cache.getOriginalEventId().equals(linkedActivity.getOriginalEventId()))
        .findFirst()
        .orElseThrow();
    assertThat(forkedActivityCache.getId()).isNull();
    assertThat(forkedActivityCache.getApplicationId()).isEqualTo(application.getId());
  }

  @Test
  void refreshApplicationCache_whenActivityLinkedToExtendedTerm_thenActivityCacheAlsoClaimed() {
    var application = buildApplication();
    var applicationDetail = new ScheduleWorkProgrammeApplicationDetail();

    var term = buildTerm(TermType.INITIAL, LocalDate.of(2030, 1, 1));
    var extensionRequest = new LicenceScheduleExtensionRequest();
    extensionRequest.setLicenceScheduleTerm(term);

    var linkedActivity = new WorkProgrammeActivity();
    linkedActivity.setId(UUID.randomUUID());
    linkedActivity.setOriginalEventId(UUID.randomUUID());
    linkedActivity.setLicenceScheduleTerm(term);

    var termCache = new LicenceEventCache();
    termCache.setId(UUID.randomUUID());
    termCache.setOriginalEventId(term.getOriginalEventId());
    termCache.setLicenceId(100);

    var activityCache = new LicenceEventCache();
    activityCache.setId(UUID.randomUUID());
    activityCache.setOriginalEventId(linkedActivity.getOriginalEventId());
    activityCache.setLicenceId(100);

    when(scheduleWorkProgrammeApplicationDetailRepository.getFirstByScheduleWorkProgrammeApplicationOrderByVersionNumberDesc(application))
        .thenReturn(Optional.of(applicationDetail));
    when(licenceScheduleExtensionRepository.findAllByScheduleWorkProgrammeApplicationDetails(applicationDetail))
        .thenReturn(List.of(extensionRequest));
    when(workProgrammeActivityService.getAllActivitiesLinkedTo(term))
        .thenReturn(List.of(linkedActivity));
    when(licenceWorkProgrammeAmendmentRepository.findAllByScheduleWorkProgrammeApplicationDetails(applicationDetail))
        .thenReturn(List.of());
    when(licenceEventCacheRepository.findAllByOriginalEventIdIn(
        Set.of(term.getOriginalEventId(), linkedActivity.getOriginalEventId())
    )).thenReturn(List.of(termCache, activityCache));

    crossLicenceEventTrackerService.refreshApplicationCache(application);

    verify(licenceEventCacheRepository).saveAll(eventCacheListCaptor.capture());
    var savedCaches = eventCacheListCaptor.getValue();

    assertThat(savedCaches)
        .extracting(LicenceEventCache::getOriginalEventId)
        .containsExactlyInAnyOrder(term.getOriginalEventId(), linkedActivity.getOriginalEventId());
    assertThat(savedCaches).allSatisfy(cache -> {
      assertThat(cache.getApplicationId()).isEqualTo(application.getId());
      assertThat(cache.getApplicationType()).isEqualTo(ApplicationType.SCHEDULE_AMENDMENT_APPLICATION);
    });
  }

  @Test
  void refreshApplicationCache_whenActivityLinkedToExtendedPhase_thenActivityCacheAlsoClaimed() {
    var application = buildApplication();
    var applicationDetail = new ScheduleWorkProgrammeApplicationDetail();

    var term = buildTerm(TermType.INITIAL, LocalDate.of(2030, 1, 1));
    var phase = buildPhase(term, PhaseType.PHASE_A, LocalDate.of(2028, 1, 1));
    var extensionRequest = new LicenceScheduleExtensionRequest();
    extensionRequest.setLicenceSchedulePhase(phase);

    var linkedActivity = new WorkProgrammeActivity();
    linkedActivity.setId(UUID.randomUUID());
    linkedActivity.setOriginalEventId(UUID.randomUUID());
    linkedActivity.setLicenceSchedulePhase(phase);

    var phaseCache = new LicenceEventCache();
    phaseCache.setId(UUID.randomUUID());
    phaseCache.setOriginalEventId(phase.getOriginalEventId());
    phaseCache.setLicenceId(100);

    var activityCache = new LicenceEventCache();
    activityCache.setId(UUID.randomUUID());
    activityCache.setOriginalEventId(linkedActivity.getOriginalEventId());
    activityCache.setLicenceId(100);

    when(scheduleWorkProgrammeApplicationDetailRepository.getFirstByScheduleWorkProgrammeApplicationOrderByVersionNumberDesc(application))
        .thenReturn(Optional.of(applicationDetail));
    when(licenceScheduleExtensionRepository.findAllByScheduleWorkProgrammeApplicationDetails(applicationDetail))
        .thenReturn(List.of(extensionRequest));
    when(workProgrammeActivityService.getAllActivitiesLinkedTo(phase))
        .thenReturn(List.of(linkedActivity));
    when(licenceWorkProgrammeAmendmentRepository.findAllByScheduleWorkProgrammeApplicationDetails(applicationDetail))
        .thenReturn(List.of());
    when(licenceEventCacheRepository.findAllByOriginalEventIdIn(
        Set.of(phase.getOriginalEventId(), linkedActivity.getOriginalEventId())
    )).thenReturn(List.of(phaseCache, activityCache));

    crossLicenceEventTrackerService.refreshApplicationCache(application);

    verify(licenceEventCacheRepository).saveAll(eventCacheListCaptor.capture());
    var savedCaches = eventCacheListCaptor.getValue();

    assertThat(savedCaches)
        .extracting(LicenceEventCache::getOriginalEventId)
        .containsExactlyInAnyOrder(phase.getOriginalEventId(), linkedActivity.getOriginalEventId());
    assertThat(savedCaches).allSatisfy(cache -> {
      assertThat(cache.getApplicationId()).isEqualTo(application.getId());
      assertThat(cache.getApplicationType()).isEqualTo(ApplicationType.SCHEDULE_AMENDMENT_APPLICATION);
    });
  }

  private ScheduleWorkProgrammeApplication buildApplication() {
    var application = new ScheduleWorkProgrammeApplication();
    application.setId(UUID.randomUUID());
    application.setStewardWuaId(500L);
    return application;
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
