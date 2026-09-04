package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDuration;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceSchedulePhaseTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceScheduleTermTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.PhaseType;
import uk.co.nstauthority.licensingmanagementservice.licence.TermType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhase;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhaseRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTermRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationService;

@ExtendWith(MockitoExtension.class)
class RecordDurationChangesServiceTest {

  @Mock
  private ScheduleWorkProgrammeApplicationService scheduleWorkProgrammeApplicationService;

  @Mock
  private LicenceScheduleTermRepository licenceScheduleTermRepository;

  @Mock
  private LicenceSchedulePhaseRepository licenceSchedulePhaseRepository;

  @Mock
  private RecordOfDecisionExtensionRepository recordOfDecisionExtensionRepository;

  @Mock
  private RecordOfDecisionReductionRepository recordOfDecisionReductionRepository;

  @Mock
  private LicenceScheduleDetail licenceScheduleDetail;

  @Mock
  private Clock clock;

  @InjectMocks
  private RecordDurationChangesService recordDurationChangesService;

  private ScheduleWorkProgrammeApplicationDetail applicationDetail;

  @BeforeEach
  void setUp() {
    applicationDetail = ScheduleWorkProgrammeApplicationDetailTestUtil.builder()
        .withId(UUID.randomUUID())
        .build();
  }

  @Test
  void getDurationChangeViews_whenLicenceInItsFirstTerm_currentCannotBeReducedAndFinalCannotBeExtended() {
    mockToday();
    mockSchedule(List.of(initialTerm(), secondTerm(), thirdTerm()));

    var views = recordDurationChangesService.getDurationChangeViews(applicationDetail);

    assertThat(views)
        .extracting(
            RecordDurationChangeView::displayName,
            RecordDurationChangeView::canReduce,
            RecordDurationChangeView::canExtend)
        .containsExactly(
            tuple(TermType.INITIAL.getDisplayName(), false, true),
            tuple(TermType.SECOND.getDisplayName(), true, true),
            tuple(TermType.THIRD.getDisplayName(), true, false));
  }

  @Test
  void getDurationChangeViews_whenLicenceInItsSecondTerm_theFinalTermIsStillReducible() {
    mockToday();
    mockSchedule(List.of(endedInitialTerm(), currentSecondTerm(), thirdTerm()));

    var views = recordDurationChangesService.getDurationChangeViews(applicationDetail);

    assertThat(views)
        .extracting(
            RecordDurationChangeView::displayName,
            RecordDurationChangeView::canReduce,
            RecordDurationChangeView::canExtend)
        .containsExactly(
            tuple(TermType.INITIAL.getDisplayName(), false, false),
            tuple(TermType.SECOND.getDisplayName(), false, true),
            tuple(TermType.THIRD.getDisplayName(), true, false));
  }

  @Test
  void getDurationChangeViews_whenTermIsPhased_thePhasesReplaceTheTerm() {
    var term = initialTerm();
    mockToday();
    mockSchedule(List.of(term, secondTerm()));
    when(licenceSchedulePhaseRepository.findAllByLicenceScheduleTerm(term))
        .thenReturn(List.of(phaseB(term), phaseA(term)));

    var views = recordDurationChangesService.getDurationChangeViews(applicationDetail);

    assertThat(views)
        .extracting(RecordDurationChangeView::displayName, RecordDurationChangeView::isPhase)
        .containsExactly(
            tuple(PhaseType.PHASE_A.getDisplayName(), true),
            tuple(PhaseType.PHASE_B.getDisplayName(), true),
            tuple(TermType.SECOND.getDisplayName(), false));
  }

  @Test
  void saveDurationChanges_recordsExtensionsAndReductionsAndClearsMaintainedPeriods() {
    var initial = initialTerm();
    var second = secondTerm();
    var third = thirdTerm();
    mockSchedule(List.of(initial, second, third));
    when(licenceScheduleTermRepository.findById(initial.getId())).thenReturn(Optional.of(initial));
    when(licenceScheduleTermRepository.findById(third.getId())).thenReturn(Optional.of(third));
    when(recordOfDecisionExtensionRepository
        .findByScheduleWorkProgrammeApplicationDetailAndLicenceScheduleTermId(applicationDetail, initial.getId()))
        .thenReturn(Optional.empty());
    when(recordOfDecisionReductionRepository
        .findByScheduleWorkProgrammeApplicationDetailAndLicenceScheduleTermId(applicationDetail, third.getId()))
        .thenReturn(Optional.empty());
    when(recordOfDecisionExtensionRepository.findAllByScheduleWorkProgrammeApplicationDetail(applicationDetail))
        .thenReturn(List.of());
    when(recordOfDecisionReductionRepository.findAllByScheduleWorkProgrammeApplicationDetail(applicationDetail))
        .thenReturn(List.of());

    var form = new RecordDurationChangesForm();
    extend(form, initial, new ThreeFieldDuration(1, 0, 0));
    maintain(form, second);
    reduce(form, third, new ThreeFieldDuration(1, 0, 0));

    recordDurationChangesService.saveDurationChanges(form, applicationDetail);

    var extensionCaptor = ArgumentCaptor.forClass(RecordOfDecisionExtension.class);
    verify(recordOfDecisionExtensionRepository).save(extensionCaptor.capture());
    assertThat(extensionCaptor.getValue().getLicenceScheduleTerm()).isEqualTo(initial);
    assertThat(extensionCaptor.getValue().getExtensionDuration()).isEqualTo(new ThreeFieldDuration(1, 0, 0));

    var reductionCaptor = ArgumentCaptor.forClass(RecordOfDecisionReduction.class);
    verify(recordOfDecisionReductionRepository).save(reductionCaptor.capture());
    assertThat(reductionCaptor.getValue().getLicenceScheduleTerm()).isEqualTo(third);
    assertThat(reductionCaptor.getValue().getReductionDuration()).isEqualTo(new ThreeFieldDuration(1, 0, 0));
  }

  @Test
  void saveDurationChanges_whenAPeriodChangesFromReducedToMaintained_theReductionIsDeleted() {
    var second = secondTerm();
    mockSchedule(List.of(initialTerm(), second, thirdTerm()));
    var existingReduction = new RecordOfDecisionReduction();
    existingReduction.setLicenceScheduleTerm(second);
    when(recordOfDecisionExtensionRepository.findAllByScheduleWorkProgrammeApplicationDetail(applicationDetail))
        .thenReturn(List.of());
    when(recordOfDecisionReductionRepository.findAllByScheduleWorkProgrammeApplicationDetail(applicationDetail))
        .thenReturn(List.of(existingReduction));

    var form = new RecordDurationChangesForm();
    maintain(form, second);

    recordDurationChangesService.saveDurationChanges(form, applicationDetail);

    verify(recordOfDecisionReductionRepository).delete(existingReduction);
    verify(recordOfDecisionReductionRepository, never()).save(existingReduction);
  }

  @Test
  void isComplete_whenTheExtensionIsBalancedByTheReduction_returnsTrue() {
    mockTotals(new ThreeFieldDuration(1, 0, 0), new ThreeFieldDuration(0, 12, 0));
    when(recordOfDecisionExtensionRepository.existsByScheduleWorkProgrammeApplicationDetail(applicationDetail))
        .thenReturn(true);

    assertThat(recordDurationChangesService.isComplete(applicationDetail)).isTrue();
  }

  @Test
  void isComplete_whenTheTotalsDoNotMatch_returnsFalse() {
    mockTotals(new ThreeFieldDuration(2, 0, 0), new ThreeFieldDuration(1, 0, 0));
    when(recordOfDecisionExtensionRepository.existsByScheduleWorkProgrammeApplicationDetail(applicationDetail))
        .thenReturn(true);

    assertThat(recordDurationChangesService.isComplete(applicationDetail)).isFalse();
  }

  @Test
  void isComplete_whenNothingIsExtended_returnsFalse() {
    when(recordOfDecisionExtensionRepository.existsByScheduleWorkProgrammeApplicationDetail(applicationDetail))
        .thenReturn(false);

    assertThat(recordDurationChangesService.isComplete(applicationDetail)).isFalse();
  }

  @Test
  void getFilledForm_populatesTheChangeTypeAndDurationAlreadyRecorded() {
    var initial = initialTerm();
    var second = secondTerm();
    mockSchedule(List.of(initial, second));
    var extension = new RecordOfDecisionExtension();
    extension.setLicenceScheduleTerm(initial);
    extension.setExtensionDuration(new ThreeFieldDuration(1, 6, 0));
    var reduction = new RecordOfDecisionReduction();
    reduction.setLicenceScheduleTerm(second);
    reduction.setReductionDuration(new ThreeFieldDuration(1, 6, 0));
    when(recordOfDecisionExtensionRepository.findAllByScheduleWorkProgrammeApplicationDetail(applicationDetail))
        .thenReturn(List.of(extension));
    when(recordOfDecisionReductionRepository.findAllByScheduleWorkProgrammeApplicationDetail(applicationDetail))
        .thenReturn(List.of(reduction));

    var form = recordDurationChangesService.getFilledForm(applicationDetail);

    assertThat(form.getChangeType())
        .containsEntry(initial.getId().toString(), DurationChangeType.EXTEND)
        .containsEntry(second.getId().toString(), DurationChangeType.REDUCE);
    assertThat(form.getExtendDuration().get(initial.getId().toString()).getYears()).isEqualTo("1");
    assertThat(form.getExtendDuration().get(initial.getId().toString()).getMonths()).isEqualTo("6");
  }

  private void extend(RecordDurationChangesForm form, LicenceScheduleTerm term, ThreeFieldDuration duration) {
    var id = term.getId().toString();
    form.getChangeType().put(id, DurationChangeType.EXTEND);
    var input = RecordDurationChangesForm.newExtendDurationInput(id);
    input.setFromThreeFieldDuration(duration);
    form.getExtendDuration().put(id, input);
  }

  private void reduce(RecordDurationChangesForm form, LicenceScheduleTerm term, ThreeFieldDuration duration) {
    var id = term.getId().toString();
    form.getChangeType().put(id, DurationChangeType.REDUCE);
    var input = RecordDurationChangesForm.newReduceDurationInput(id);
    input.setFromThreeFieldDuration(duration);
    form.getReduceDuration().put(id, input);
  }

  private void maintain(RecordDurationChangesForm form, LicenceScheduleTerm term) {
    form.getChangeType().put(term.getId().toString(), DurationChangeType.MAINTAIN);
  }

  private void mockToday() {
    when(clock.instant()).thenReturn(Instant.parse("2026-08-25T10:00:00.00Z"));
    when(clock.getZone()).thenReturn(ZoneOffset.UTC);
  }

  private void mockSchedule(List<LicenceScheduleTerm> terms) {
    when(scheduleWorkProgrammeApplicationService.getScheduleDetailFromApplicationDetail(applicationDetail))
        .thenReturn(licenceScheduleDetail);
    when(licenceScheduleTermRepository.findAllByLicenceScheduleDetail(licenceScheduleDetail))
        .thenReturn(terms);
  }

  private void mockTotals(ThreeFieldDuration extension, ThreeFieldDuration reduction) {
    var recordedExtension = new RecordOfDecisionExtension();
    recordedExtension.setExtensionDuration(extension);
    var recordedReduction = new RecordOfDecisionReduction();
    recordedReduction.setReductionDuration(reduction);
    when(recordOfDecisionExtensionRepository.findAllByScheduleWorkProgrammeApplicationDetail(applicationDetail))
        .thenReturn(List.of(recordedExtension));
    when(recordOfDecisionReductionRepository.findAllByScheduleWorkProgrammeApplicationDetail(applicationDetail))
        .thenReturn(List.of(recordedReduction));
  }

  private LicenceScheduleTerm initialTerm() {
    return term(TermType.INITIAL, LocalDate.of(2024, 1, 1), LocalDate.of(2027, 12, 31), 4);
  }

  private LicenceScheduleTerm endedInitialTerm() {
    return term(TermType.INITIAL, LocalDate.of(2020, 1, 1), LocalDate.of(2023, 12, 31), 4);
  }

  private LicenceScheduleTerm secondTerm() {
    return term(TermType.SECOND, LocalDate.of(2028, 1, 1), LocalDate.of(2031, 12, 31), 4);
  }

  private LicenceScheduleTerm currentSecondTerm() {
    return term(TermType.SECOND, LocalDate.of(2024, 1, 1), LocalDate.of(2027, 12, 31), 4);
  }

  private LicenceScheduleTerm thirdTerm() {
    return term(TermType.THIRD, LocalDate.of(2032, 1, 1), LocalDate.of(2049, 12, 31), 18);
  }

  private LicenceScheduleTerm term(TermType termType, LocalDate startDate, LocalDate endDate, int years) {
    return LicenceScheduleTermTestUtil.builder()
        .withId(UUID.randomUUID())
        .withLicenceScheduleDetail(licenceScheduleDetail)
        .withTermType(termType)
        .withTermDuration(new ThreeFieldDuration(years, 0, 0))
        .withStartDate(startDate)
        .withEndDate(endDate)
        .build();
  }

  private LicenceSchedulePhase phaseA(LicenceScheduleTerm term) {
    return phase(term, PhaseType.PHASE_A, LocalDate.of(2024, 1, 1), LocalDate.of(2025, 12, 31));
  }

  private LicenceSchedulePhase phaseB(LicenceScheduleTerm term) {
    return phase(term, PhaseType.PHASE_B, LocalDate.of(2026, 1, 1), LocalDate.of(2027, 12, 31));
  }

  private LicenceSchedulePhase phase(
      LicenceScheduleTerm term, PhaseType phaseType, LocalDate startDate, LocalDate endDate
  ) {
    return LicenceSchedulePhaseTestUtil.builder()
        .withId(UUID.randomUUID())
        .withLicenceScheduleTerm(term)
        .withPhaseType(phaseType)
        .withPhaseDuration(new ThreeFieldDuration(2, 0, 0))
        .withStartDate(startDate)
        .withEndDate(endDate)
        .build();
  }
}
