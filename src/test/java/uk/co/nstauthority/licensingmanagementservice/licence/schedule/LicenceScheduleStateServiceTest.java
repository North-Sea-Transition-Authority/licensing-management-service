package uk.co.nstauthority.licensingmanagementservice.licence.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceSchedulePhaseTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceScheduleTermTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.PhaseType;
import uk.co.nstauthority.licensingmanagementservice.licence.TermType;
import uk.co.nstauthority.licensingmanagementservice.licence.rules.LicenceTypeRulesResolver;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhase;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhaseService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTermService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney.WorkProgrammeActivityView;

@ExtendWith(MockitoExtension.class)
class LicenceScheduleStateServiceTest {

  @Mock
  private LicenceScheduleTermService licenceScheduleTermService;

  @Mock
  private LicenceSchedulePhaseService licenceSchedulePhaseService;

  @Mock
  private LicenceTypeRulesResolver licenceTypeRulesResolver;

  @Mock
  private WorkProgrammeActivityService workProgrammeActivityService;

  @Mock
  private Clock clock;

  @InjectMocks
  private LicenceScheduleStateService licenceScheduleStateService;

  @Test
  void getNextTermPhaseStartDate_findsNextPhaseInSameTerm() {
    var licence = LicenceTestUtil.builder().withLicenceType(LicenceType.SEAWARD_PRODUCTION).build();
    var licenceSchedule = LicenceScheduleTestUtil.createLicenceSchedule(licence);
    var scheduleDetail = LicenceScheduleTestUtil.createLicenceScheduleDetail(licenceSchedule);

    var currentTerm = LicenceScheduleTermTestUtil.builder()
        .withStartDate(LocalDate.of(2025, 1, 1))
        .withEndDate(LocalDate.of(2030, 1, 1))
        .build();
    var currentPhase = LicenceSchedulePhaseTestUtil.builder()
        .withStartDate(LocalDate.of(2025, 1, 1))
        .withEndDate(LocalDate.of(2026, 1, 1))
        .build();
    var nextPhase = LicenceSchedulePhaseTestUtil.builder()
        .withStartDate(LocalDate.of(2026, 1, 1))
        .withEndDate(LocalDate.of(2027, 1, 1))
        .build();

    when(clock.instant()).thenReturn(LocalDate.of(2025, 6, 1).atStartOfDay(ZoneId.systemDefault()).toInstant());
    when(clock.getZone()).thenReturn(ZoneId.systemDefault());

    when(licenceScheduleTermService.getTermsByLicenceScheduleDetail(scheduleDetail)).thenReturn(List.of(currentTerm));
    when(licenceTypeRulesResolver.hasPhases(LicenceType.SEAWARD_PRODUCTION)).thenReturn(true);
    when(licenceSchedulePhaseService.getPhasesByTerm(currentTerm)).thenReturn(List.of(currentPhase, nextPhase));

    var result = licenceScheduleStateService.getNextTermPhaseStartDate(scheduleDetail);

    assertThat(result).hasValue(LocalDate.of(2026, 1, 1));
  }

  @Test
  void getNextTermPhaseStartDate_findsNextTermStartIfNoNextPhase() {
    var licence = LicenceTestUtil.builder().withLicenceType(LicenceType.SEAWARD_PRODUCTION).build();
    var licenceSchedule = LicenceScheduleTestUtil.createLicenceSchedule(licence);
    var scheduleDetail = LicenceScheduleTestUtil.createLicenceScheduleDetail(licenceSchedule);

    var currentTerm = LicenceScheduleTermTestUtil.builder()
        .withStartDate(LocalDate.of(2025, 1, 1))
        .withEndDate(LocalDate.of(2030, 1, 1))
        .build();
    var currentPhase = LicenceSchedulePhaseTestUtil.builder()
        .withStartDate(LocalDate.of(2025, 1, 1))
        .withEndDate(LocalDate.of(2030, 1, 1))
        .build();
    var nextTerm = LicenceScheduleTermTestUtil.builder()
        .withStartDate(LocalDate.of(2030, 1, 1))
        .withEndDate(LocalDate.of(2035, 1, 1))
        .build();

    when(clock.instant()).thenReturn(LocalDate.of(2025, 6, 1).atStartOfDay(ZoneId.systemDefault()).toInstant());
    when(clock.getZone()).thenReturn(ZoneId.systemDefault());

    when(licenceScheduleTermService.getTermsByLicenceScheduleDetail(scheduleDetail)).thenReturn(List.of(currentTerm, nextTerm));
    when(licenceTypeRulesResolver.hasPhases(LicenceType.SEAWARD_PRODUCTION)).thenReturn(true);
    when(licenceSchedulePhaseService.getPhasesByTerm(currentTerm)).thenReturn(List.of(currentPhase));
    when(licenceSchedulePhaseService.getPhasesByTerm(nextTerm)).thenReturn(List.of());

    var result = licenceScheduleStateService.getNextTermPhaseStartDate(scheduleDetail);

    assertThat(result).hasValue(LocalDate.of(2030, 1, 1));
  }

  @Test
  void getNextTermPhaseStartDate_returnsEmptyIfNoCurrentTerm() {
    var licence = LicenceTestUtil.builder().withLicenceType(LicenceType.SEAWARD_PRODUCTION).build();
    var licenceSchedule = LicenceScheduleTestUtil.createLicenceSchedule(licence);
    var scheduleDetail = LicenceScheduleTestUtil.createLicenceScheduleDetail(licenceSchedule);

    when(licenceScheduleTermService.getTermsByLicenceScheduleDetail(scheduleDetail)).thenReturn(List.of());

    var result = licenceScheduleStateService.getNextTermPhaseStartDate(scheduleDetail);

    assertThat(result).isEmpty();
  }

  @Test
  void getScheduleState_WhenNoCurrentTerm_ReturnsAllNulls() {
    var detail = new LicenceScheduleDetail();

    when(licenceScheduleTermService.getTermsByLicenceScheduleDetail(detail))
        .thenReturn(Collections.emptyList());

    var state = licenceScheduleStateService.getScheduleState(detail);

    assertThat(state.currentTerm()).isNull();
    assertThat(state.currentPhase()).isNull();
    assertThat(state.nextTerm()).isNull();
    assertThat(state.nextPhase()).isNull();
  }

  @Test
  void getScheduleState_WhenNextPhaseIsInSameTerm_NextTermRemainsCurrentTerm() {
    when(clock.instant()).thenReturn(LocalDate.of(2025, 6, 1).atStartOfDay(ZoneId.systemDefault()).toInstant());
    when(clock.getZone()).thenReturn(ZoneId.systemDefault());

    var detail = new LicenceScheduleDetail();

    var currentTerm = new LicenceScheduleTerm();
    currentTerm.setStartDate(LocalDate.of(2025, 1, 1));
    currentTerm.setEndDate(LocalDate.of(2030, 1, 1));

    var currentPhase = new LicenceSchedulePhase();
    currentPhase.setStartDate(LocalDate.of(2025, 1, 1));
    currentPhase.setEndDate(LocalDate.of(2026, 1, 1));

    var nextPhaseInSameTerm = new LicenceSchedulePhase();
    nextPhaseInSameTerm.setStartDate(LocalDate.of(2026, 1, 1));
    nextPhaseInSameTerm.setEndDate(LocalDate.of(2027, 1, 1));

    var terms = List.of(currentTerm);
    var phases = List.of(currentPhase, nextPhaseInSameTerm);

    when(licenceScheduleTermService.getTermsByLicenceScheduleDetail(detail)).thenReturn(terms);
    when(licenceSchedulePhaseService.getPhasesByTerm(currentTerm)).thenReturn(phases);

    var state = licenceScheduleStateService.getScheduleState(detail);

    assertThat(state.currentTerm()).isEqualTo(currentTerm);
    assertThat(state.currentPhase()).isEqualTo(currentPhase);
    assertThat(state.nextTerm()).isEqualTo(currentTerm);
    assertThat(state.nextPhase()).isEqualTo(nextPhaseInSameTerm);
  }

  @Test
  void getScheduleState_WhenCurrentTermOutOfPhases_JumpsToNextTermAndItsFirstPhase() {
    when(clock.instant()).thenReturn(LocalDate.of(2025, 6, 1).atStartOfDay(ZoneId.systemDefault()).toInstant());
    when(clock.getZone()).thenReturn(ZoneId.systemDefault());

    var detail = new LicenceScheduleDetail();

    var currentTerm = new LicenceScheduleTerm();
    currentTerm.setStartDate(LocalDate.of(2025, 1, 1));
    currentTerm.setEndDate(LocalDate.of(2030, 1, 1));

    var nextTerm = new LicenceScheduleTerm();
    nextTerm.setStartDate(LocalDate.of(2030, 1, 1));
    nextTerm.setEndDate(LocalDate.of(2035, 1, 1));

    var currentPhase = new LicenceSchedulePhase();
    currentPhase.setStartDate(LocalDate.of(2025, 1, 1));
    currentPhase.setEndDate(LocalDate.of(2030, 1, 1));

    var firstPhaseOfNextTerm = new LicenceSchedulePhase();
    firstPhaseOfNextTerm.setStartDate(LocalDate.of(2030, 1, 1));
    firstPhaseOfNextTerm.setEndDate(LocalDate.of(2032, 1, 1));

    when(licenceScheduleTermService.getTermsByLicenceScheduleDetail(detail)).thenReturn(List.of(currentTerm, nextTerm));
    when(licenceSchedulePhaseService.getPhasesByTerm(currentTerm)).thenReturn(List.of(currentPhase));

    when(licenceSchedulePhaseService.getPhasesByTerm(nextTerm)).thenReturn(List.of(firstPhaseOfNextTerm));

    var state = licenceScheduleStateService.getScheduleState(detail);

    assertThat(state.currentTerm()).isEqualTo(currentTerm);
    assertThat(state.currentPhase()).isEqualTo(currentPhase);
    assertThat(state.nextTerm()).isEqualTo(nextTerm);
    assertThat(state.nextPhase()).isEqualTo(firstPhaseOfNextTerm);
  }

  @Test
  void getScheduleState_WhenNoNextPhase_FindsNextTerm() {
    when(clock.instant()).thenReturn(LocalDate.of(2025, 6, 1).atStartOfDay(ZoneId.systemDefault()).toInstant());
    when(clock.getZone()).thenReturn(ZoneId.systemDefault());

    var detail = new LicenceScheduleDetail();

    var currentTerm = new LicenceScheduleTerm();
    currentTerm.setStartDate(LocalDate.of(2025, 1, 1));
    currentTerm.setEndDate(LocalDate.of(2030, 1, 1));

    var nextTerm = new LicenceScheduleTerm();
    nextTerm.setStartDate(LocalDate.of(2030, 1, 1));
    nextTerm.setEndDate(LocalDate.of(2035, 1, 1));

    var currentPhase = new LicenceSchedulePhase();
    currentPhase.setStartDate(LocalDate.of(2025, 1, 1));
    currentPhase.setEndDate(LocalDate.of(2030, 1, 1));

    var terms = List.of(currentTerm, nextTerm);
    var phases = List.of(currentPhase);

    when(licenceScheduleTermService.getTermsByLicenceScheduleDetail(detail)).thenReturn(terms);
    when(licenceSchedulePhaseService.getPhasesByTerm(currentTerm)).thenReturn(phases);

    when(licenceSchedulePhaseService.getPhasesByTerm(nextTerm)).thenReturn(Collections.emptyList());

    var state = licenceScheduleStateService.getScheduleState(detail);

    assertThat(state.currentTerm()).isEqualTo(currentTerm);
    assertThat(state.currentPhase()).isEqualTo(currentPhase);
    assertThat(state.nextTerm()).isEqualTo(nextTerm);
    assertThat(state.nextPhase()).isNull();
  }

  @Test
  void getScheduleState_WhenNoPhasesInCurrentTerm_FindsNextTerm() {
    when(clock.instant()).thenReturn(LocalDate.of(2025, 6, 1).atStartOfDay(ZoneId.systemDefault()).toInstant());
    when(clock.getZone()).thenReturn(ZoneId.systemDefault());

    var detail = new LicenceScheduleDetail();

    var currentTerm = new LicenceScheduleTerm();
    currentTerm.setStartDate(LocalDate.of(2025, 1, 1));
    currentTerm.setEndDate(LocalDate.of(2030, 1, 1));

    var nextTerm = new LicenceScheduleTerm();
    nextTerm.setStartDate(LocalDate.of(2030, 1, 1));
    nextTerm.setEndDate(LocalDate.of(2035, 1, 1));

    var terms = List.of(currentTerm, nextTerm);

    when(licenceScheduleTermService.getTermsByLicenceScheduleDetail(detail)).thenReturn(terms);
    when(licenceSchedulePhaseService.getPhasesByTerm(currentTerm)).thenReturn(Collections.emptyList());

    when(licenceSchedulePhaseService.getPhasesByTerm(nextTerm)).thenReturn(Collections.emptyList());

    var state = licenceScheduleStateService.getScheduleState(detail);

    assertThat(state.currentTerm()).isEqualTo(currentTerm);
    assertThat(state.currentPhase()).isNull();
    assertThat(state.nextTerm()).isEqualTo(nextTerm);
    assertThat(state.nextPhase()).isNull();
  }

  @Test
  void formatTermPhaseDisplay_WhenBothPresent_ReturnsPhaseWithTermInParentheses() {
    var term = new LicenceScheduleTerm();
    term.setTermType(TermType.INITIAL);

    var phase = new LicenceSchedulePhase();
    phase.setPhaseType(PhaseType.PHASE_A);

    String result = licenceScheduleStateService.formatTermPhaseDisplay(term, phase);
    assertEquals("Phase A (Initial Term)", result);
  }

  @Test
  void formatTermPhaseDisplay_WhenOnlyTermPresent_ReturnsTermName() {
    var term = new LicenceScheduleTerm();
    term.setTermType(TermType.INITIAL);

    String result = licenceScheduleStateService.formatTermPhaseDisplay(term, null);
    assertEquals("Initial Term", result);
  }

  @Test
  void formatTermPhaseDisplay_WhenBothNull_ReturnsNull() {
    String result = licenceScheduleStateService.formatTermPhaseDisplay(null, null);
    assertNull(result);
  }

  @Test
  void hasCurrentWorkProgrammeActivities_whenCurrentPhaseIsNotNull_delegatesToPhaseExistsQuery() {
    when(clock.instant()).thenReturn(LocalDate.of(2025, 6, 1).atStartOfDay(ZoneId.systemDefault()).toInstant());
    when(clock.getZone()).thenReturn(ZoneId.systemDefault());

    var detail = new LicenceScheduleDetail();
    var term = new LicenceScheduleTerm();
    term.setStartDate(LocalDate.of(2025, 1, 1));
    term.setEndDate(LocalDate.of(2030, 1, 1));

    var phase = new LicenceSchedulePhase();
    phase.setStartDate(LocalDate.of(2025, 1, 1));
    phase.setEndDate(LocalDate.of(2026, 1, 1));

    when(licenceScheduleTermService.getTermsByLicenceScheduleDetail(detail)).thenReturn(List.of(term));
    when(licenceSchedulePhaseService.getPhasesByTerm(term)).thenReturn(List.of(phase));
    when(workProgrammeActivityService.hasActivitiesForPhase(phase)).thenReturn(true);

    assertThat(licenceScheduleStateService.hasCurrentWorkProgrammeActivities(detail)).isTrue();
  }

  @Test
  void hasCurrentWorkProgrammeActivities_whenCurrentPhaseIsNullAndTermIsNotNull_delegatesToTermExistsQuery() {
    when(clock.instant()).thenReturn(LocalDate.of(2025, 6, 1).atStartOfDay(ZoneId.systemDefault()).toInstant());
    when(clock.getZone()).thenReturn(ZoneId.systemDefault());

    var detail = new LicenceScheduleDetail();
    var term = new LicenceScheduleTerm();
    term.setStartDate(LocalDate.of(2025, 1, 1));
    term.setEndDate(LocalDate.of(2030, 1, 1));

    when(licenceScheduleTermService.getTermsByLicenceScheduleDetail(detail)).thenReturn(List.of(term));
    when(licenceSchedulePhaseService.getPhasesByTerm(term)).thenReturn(Collections.emptyList());
    when(workProgrammeActivityService.hasActivitiesForTerm(term)).thenReturn(false);

    assertThat(licenceScheduleStateService.hasCurrentWorkProgrammeActivities(detail)).isFalse();
  }

  @Test
  void hasCurrentWorkProgrammeActivities_whenPhaseAndTermAreNull_returnsFalse() {
    var detail = new LicenceScheduleDetail();

    when(licenceScheduleTermService.getTermsByLicenceScheduleDetail(detail)).thenReturn(Collections.emptyList());

    assertThat(licenceScheduleStateService.hasCurrentWorkProgrammeActivities(detail)).isFalse();
  }

  @Test
  void getCurrentWorkProgrammeActivitiesViews_whenCurrentPhaseIsNotNull_returnsViewsForPhase() {
    when(clock.instant()).thenReturn(LocalDate.of(2025, 6, 1).atStartOfDay(ZoneId.systemDefault()).toInstant());
    when(clock.getZone()).thenReturn(ZoneId.systemDefault());

    var detail = new LicenceScheduleDetail();
    var term = new LicenceScheduleTerm();
    term.setStartDate(LocalDate.of(2025, 1, 1));
    term.setEndDate(LocalDate.of(2030, 1, 1));

    var phase = new LicenceSchedulePhase();
    phase.setStartDate(LocalDate.of(2025, 1, 1));
    phase.setEndDate(LocalDate.of(2026, 1, 1));

    var view = mock(WorkProgrammeActivityView.class);

    when(licenceScheduleTermService.getTermsByLicenceScheduleDetail(detail)).thenReturn(List.of(term));
    when(licenceSchedulePhaseService.getPhasesByTerm(term)).thenReturn(List.of(phase));
    when(workProgrammeActivityService.getLicenceWorkProgramActivitiesViewsForGivenPhase(phase)).thenReturn(List.of(view));

    assertThat(licenceScheduleStateService.getCurrentWorkProgrammeActivitiesViews(detail)).containsExactly(view);
  }

  @Test
  void getCurrentWorkProgrammeActivitiesViews_whenCurrentPhaseIsNullAndTermIsNotNull_returnsViewsForTerm() {
    when(clock.instant()).thenReturn(LocalDate.of(2025, 6, 1).atStartOfDay(ZoneId.systemDefault()).toInstant());
    when(clock.getZone()).thenReturn(ZoneId.systemDefault());

    var detail = new LicenceScheduleDetail();
    var term = new LicenceScheduleTerm();
    term.setStartDate(LocalDate.of(2025, 1, 1));
    term.setEndDate(LocalDate.of(2030, 1, 1));

    var view = mock(WorkProgrammeActivityView.class);

    when(licenceScheduleTermService.getTermsByLicenceScheduleDetail(detail)).thenReturn(List.of(term));
    when(licenceSchedulePhaseService.getPhasesByTerm(term)).thenReturn(Collections.emptyList());
    when(workProgrammeActivityService.getLicenceWorkProgramActivitiesViewsForGivenTerm(term)).thenReturn(List.of(view));

    assertThat(licenceScheduleStateService.getCurrentWorkProgrammeActivitiesViews(detail)).containsExactly(view);
  }

  @Test
  void getCurrentWorkProgrammeActivitiesViews_whenPhaseAndTermAreNull_returnsEmptyList() {
    var detail = new LicenceScheduleDetail();

    when(licenceScheduleTermService.getTermsByLicenceScheduleDetail(detail)).thenReturn(Collections.emptyList());

    assertThat(licenceScheduleStateService.getCurrentWorkProgrammeActivitiesViews(detail)).isEmpty();
  }
}
