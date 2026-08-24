package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
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
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDurationInput;
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
class RecordReductionDetailsServiceTest {

  @Mock
  private RecordOfDecisionReductionRepository recordOfDecisionReductionRepository;

  @Mock
  private RecordExtensionDetailsService recordExtensionDetailsService;

  @Mock
  private ScheduleWorkProgrammeApplicationService scheduleWorkProgrammeApplicationService;

  @Mock
  private LicenceSchedulePhaseRepository licenceSchedulePhaseRepository;

  @Mock
  private LicenceScheduleTermRepository licenceScheduleTermRepository;

  @Mock
  private LicenceScheduleDetail licenceScheduleDetail;

  @Mock
  private Clock clock;

  @InjectMocks
  private RecordReductionDetailsService recordReductionDetailsService;

  private ScheduleWorkProgrammeApplicationDetail applicationDetail;

  private final UUID firstTermId = UUID.randomUUID();
  private final UUID firstPhaseId = UUID.randomUUID();

  private final UUID termId = UUID.randomUUID();
  private final UUID otherTermId = UUID.randomUUID();
  private final UUID phaseId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    applicationDetail = ScheduleWorkProgrammeApplicationDetailTestUtil.builder()
        .withId(UUID.randomUUID())
        .build();
  }

  @Test
  void isReductionComplete_whenTotalMatchesTheExtension_returnsTrue() {
    mockReductions(
        reductionWithDuration(new ThreeFieldDuration(0, 8, 10)),
        reductionWithDuration(new ThreeFieldDuration(1, 6, 5)));
    when(recordExtensionDetailsService.getTotalExtensionDuration(applicationDetail))
        .thenReturn(new ThreeFieldDuration(2, 2, 15));

    assertThat(recordReductionDetailsService.isReductionComplete(applicationDetail)).isTrue();
  }

  @Test
  void isReductionComplete_whenMonthsAddUpToTheExtensionInYears_returnsTrue() {
    mockReductions(reductionWithDuration(new ThreeFieldDuration(0, 12, 0)));
    when(recordExtensionDetailsService.getTotalExtensionDuration(applicationDetail))
        .thenReturn(new ThreeFieldDuration(1, 0, 0));

    assertThat(recordReductionDetailsService.isReductionComplete(applicationDetail)).isTrue();
  }

  @Test
  void isReductionComplete_whenTheExtensionHasSinceChanged_returnsFalse() {
    mockReductions(reductionWithDuration(new ThreeFieldDuration(1, 0, 0)));
    when(recordExtensionDetailsService.getTotalExtensionDuration(applicationDetail))
        .thenReturn(new ThreeFieldDuration(2, 0, 0));

    assertThat(recordReductionDetailsService.isReductionComplete(applicationDetail)).isFalse();
  }

  @Test
  void isReductionComplete_whenNoReductionRecorded_returnsFalse() {
    mockNoReductions();

    assertThat(recordReductionDetailsService.isReductionComplete(applicationDetail)).isFalse();
  }

  @Test
  void getReductionDetailsViews_whenTermHasNoPhases_buildsTermView() {
    mockSchedule(List.of(initialTerm(), secondTerm()));
    mockNoReductions();

    var views = recordReductionDetailsService.getReductionDetailsViews(applicationDetail);

    assertThat(views).hasSize(1);
    var view = views.getFirst();
    assertThat(view.id()).isEqualTo(termId.toString());
    assertThat(view.displayName()).isEqualTo(TermType.SECOND.getDisplayName());
    assertThat(view.endDate()).isEqualTo("17 July 2027");
    assertThat(view.isPhase()).isFalse();
    assertThat(view.isSelected()).isFalse();
    assertThat(view.duration()).isNull();
  }

  @Test
  void getReductionDetailsViews_whenTermsHaveNoPhases_excludesTheFirstTerm() {
    mockSchedule(List.of(thirdTerm(), initialTerm(), secondTerm()));
    mockNoReductions();

    var views = recordReductionDetailsService.getReductionDetailsViews(applicationDetail);

    assertThat(views)
        .extracting(RecordReductionDetailsView::displayName)
        .containsExactly(TermType.SECOND.getDisplayName(), TermType.THIRD.getDisplayName());
  }

  @Test
  void getReductionDetailsViews_whenInitialTermIsPhased_excludesTheFirstPhaseAndNotTheTerm() {
    var term = initialTerm();
    mockSchedule(List.of(term));
    when(licenceSchedulePhaseRepository.findAllByLicenceScheduleTerm(term))
        .thenReturn(List.of(phaseB(term), phaseA(term), phaseC(term)));
    mockNoReductions();

    var views = recordReductionDetailsService.getReductionDetailsViews(applicationDetail);

    assertThat(views)
        .extracting(RecordReductionDetailsView::displayName, RecordReductionDetailsView::isPhase)
        .containsExactly(
            tuple(PhaseType.PHASE_B.getDisplayName(), true),
            tuple(PhaseType.PHASE_C.getDisplayName(), true));
  }

  @Test
  void getReductionDetailsViews_whenFirstTermIsPhased_excludesOnlyItsFirstPhaseAndKeepsLaterTerms() {
    var term = initialTerm();
    mockSchedule(List.of(term, secondTerm()));
    when(licenceSchedulePhaseRepository.findAllByLicenceScheduleTerm(term))
        .thenReturn(List.of(phaseA(term), phaseB(term)));
    mockNoReductions();

    var views = recordReductionDetailsService.getReductionDetailsViews(applicationDetail);

    assertThat(views)
        .extracting(RecordReductionDetailsView::displayName)
        .containsExactly(PhaseType.PHASE_B.getDisplayName(), TermType.SECOND.getDisplayName());
  }

  @Test
  void getReductionDetailsViews_whenTermHasAlreadyEnded_excludesIt() {
    mockSchedule(List.of(initialTerm(), endedSecondTerm(), thirdTerm()));
    mockNoReductions();

    var views = recordReductionDetailsService.getReductionDetailsViews(applicationDetail);

    assertThat(views)
        .extracting(RecordReductionDetailsView::displayName)
        .containsExactly(TermType.THIRD.getDisplayName());
  }

  @Test
  void getReductionDetailsViews_whenPhaseHasAlreadyEnded_excludesIt() {
    var term = initialTerm();
    mockSchedule(List.of(term));
    when(licenceSchedulePhaseRepository.findAllByLicenceScheduleTerm(term))
        .thenReturn(List.of(phaseA(term), endedPhaseB(term), phaseC(term)));
    mockNoReductions();

    var views = recordReductionDetailsService.getReductionDetailsViews(applicationDetail);

    assertThat(views)
        .extracting(RecordReductionDetailsView::displayName)
        .containsExactly(PhaseType.PHASE_C.getDisplayName());
  }

  @Test
  void getReductionDetailsViews_whenTermIsStillRunning_includesIt() {
    mockSchedule(List.of(initialTerm(), currentSecondTerm()));
    mockNoReductions();

    var views = recordReductionDetailsService.getReductionDetailsViews(applicationDetail);

    assertThat(views)
        .extracting(RecordReductionDetailsView::displayName)
        .containsExactly(TermType.SECOND.getDisplayName());
  }

  @Test
  void getReductionDetailsViews_whenTermEndsToday_includesIt() {
    mockSchedule(List.of(initialTerm(), secondTermEndingToday()));
    mockNoReductions();

    var views = recordReductionDetailsService.getReductionDetailsViews(applicationDetail);

    assertThat(views)
        .extracting(RecordReductionDetailsView::displayName)
        .containsExactly(TermType.SECOND.getDisplayName());
  }

  @Test
  void getReductionDetailsViews_whenScheduleHasOnlyOneTerm_returnsNothing() {
    mockSchedule(List.of(initialTerm()));
    mockNoReductions();

    assertThat(recordReductionDetailsService.getReductionDetailsViews(applicationDetail)).isEmpty();
  }

  @Test
  void getReductionDetailsViews_whenReductionSaved_marksSelectedWithDuration() {
    mockSchedule(List.of(initialTerm(), secondTerm()));
    var savedReduction = new RecordOfDecisionReduction();
    savedReduction.setLicenceScheduleTerm(LicenceScheduleTermTestUtil.builder().withId(termId).build());
    savedReduction.setReductionDuration(new ThreeFieldDuration(1, 6, 0));
    mockReductions(savedReduction);

    var view = recordReductionDetailsService.getReductionDetailsViews(applicationDetail).getFirst();

    assertThat(view.isSelected()).isTrue();
    assertThat(view.duration()).isEqualTo(new ThreeFieldDuration(1, 6, 0));
  }

  @Test
  void getFilledForm_populatesSelectionAndDuration() {
    mockSchedule(List.of(initialTerm(), secondTerm()));
    var savedReduction = new RecordOfDecisionReduction();
    savedReduction.setLicenceScheduleTerm(LicenceScheduleTermTestUtil.builder().withId(termId).build());
    savedReduction.setReductionDuration(new ThreeFieldDuration(2, 3, 0));
    mockReductions(savedReduction);

    var form = recordReductionDetailsService.getFilledForm(applicationDetail);

    assertThat(form.getSelectedTerm()).containsEntry(termId.toString(), true);
    assertThat(form.getReductionDuration().get(termId.toString()).getYears()).isEqualTo("2");
    assertThat(form.getReductionDuration().get(termId.toString()).getMonths()).isEqualTo("3");
  }

  @Test
  void getFilledForm_whenPhaseNotYetReduced_populatesUnselectedPhaseAndEmptyDuration() {
    var term = initialTerm();
    mockSchedule(List.of(term));
    when(licenceSchedulePhaseRepository.findAllByLicenceScheduleTerm(term))
        .thenReturn(List.of(phaseA(term), phaseB(term)));
    mockNoReductions();

    var form = recordReductionDetailsService.getFilledForm(applicationDetail);

    assertThat(form.getSelectedPhase()).containsExactly(Map.entry(phaseId.toString(), false));
    assertThat(form.getSelectedTerm()).isEmpty();
    assertThat(form.getReductionDuration().get(phaseId.toString()).getYears()).isNull();
  }

  @Test
  void saveReductionDetails_savesSelectedTerm() {
    var term = secondTerm();
    mockSchedule(List.of(initialTerm(), term));
    when(recordOfDecisionReductionRepository
        .findByScheduleWorkProgrammeApplicationDetailAndLicenceScheduleTermId(applicationDetail, termId))
        .thenReturn(Optional.empty());
    when(licenceScheduleTermRepository.findById(termId)).thenReturn(Optional.of(term));
    mockNoReductions();

    var form = new RecordReductionDetailsForm();
    form.getSelectedTerm().put(termId.toString(), true);
    form.getReductionDuration().put(termId.toString(), durationInput(termId, "1", "0", "0"));

    recordReductionDetailsService.saveReductionDetails(form, applicationDetail);

    var captor = ArgumentCaptor.forClass(RecordOfDecisionReduction.class);
    verify(recordOfDecisionReductionRepository).save(captor.capture());
    assertThat(captor.getValue().getScheduleWorkProgrammeApplicationDetail()).isEqualTo(applicationDetail);
    assertThat(captor.getValue().getLicenceScheduleTerm()).isEqualTo(term);
    assertThat(captor.getValue().getLicenceSchedulePhase()).isNull();
    assertThat(captor.getValue().getReductionDuration()).isEqualTo(new ThreeFieldDuration(1, 0, 0));
  }

  @Test
  void saveReductionDetails_savesSelectedPhase() {
    var term = initialTerm();
    var phase = phaseB(term);
    mockSchedule(List.of(term));
    when(licenceSchedulePhaseRepository.findAllByLicenceScheduleTerm(term))
        .thenReturn(List.of(phaseA(term), phase));
    when(recordOfDecisionReductionRepository
        .findByScheduleWorkProgrammeApplicationDetailAndLicenceSchedulePhaseId(applicationDetail, phaseId))
        .thenReturn(Optional.empty());
    when(licenceSchedulePhaseRepository.findById(phaseId)).thenReturn(Optional.of(phase));
    mockNoReductions();

    var form = new RecordReductionDetailsForm();
    form.getSelectedPhase().put(phaseId.toString(), true);
    form.getReductionDuration().put(phaseId.toString(), durationInput(phaseId, "0", "6", "0"));

    recordReductionDetailsService.saveReductionDetails(form, applicationDetail);

    var captor = ArgumentCaptor.forClass(RecordOfDecisionReduction.class);
    verify(recordOfDecisionReductionRepository).save(captor.capture());
    assertThat(captor.getValue().getLicenceSchedulePhase()).isEqualTo(phase);
    assertThat(captor.getValue().getLicenceScheduleTerm()).isNull();
    assertThat(captor.getValue().getReductionDuration()).isEqualTo(new ThreeFieldDuration(0, 6, 0));
  }

  @Test
  void saveReductionDetails_whenSubmittedTermWasNotOffered_savesNothing() {
    var foreignTermId = UUID.randomUUID();
    mockSchedule(List.of(initialTerm(), secondTerm()));
    mockNoReductions();

    var form = new RecordReductionDetailsForm();
    form.getSelectedTerm().put(foreignTermId.toString(), true);
    form.getReductionDuration().put(foreignTermId.toString(), durationInput(foreignTermId, "1", "0", "0"));
    form.getReductionDuration().put(termId.toString(), durationInput(termId, null, null, null));

    recordReductionDetailsService.saveReductionDetails(form, applicationDetail);

    verify(recordOfDecisionReductionRepository, never()).save(any(RecordOfDecisionReduction.class));
    verify(recordOfDecisionReductionRepository, never()).delete(any(RecordOfDecisionReduction.class));
  }

  @Test
  void saveReductionDetails_whenSelectedTermHasNoSubmittedDuration_savesNothing() {
    mockSchedule(List.of(initialTerm(), secondTerm()));
    mockNoReductions();

    var form = new RecordReductionDetailsForm();
    form.getSelectedTerm().put(termId.toString(), true);
    form.getSelectedTerm().put(otherTermId.toString(), true);

    recordReductionDetailsService.saveReductionDetails(form, applicationDetail);

    verify(recordOfDecisionReductionRepository, never()).save(any(RecordOfDecisionReduction.class));
  }

  @Test
  void saveReductionDetails_whenADifferentOptionIsSelected_deletesTheUnselectedReduction() {
    var otherTerm = thirdTerm();
    var existing = new RecordOfDecisionReduction();
    existing.setLicenceScheduleTerm(LicenceScheduleTermTestUtil.builder().withId(termId).build());

    mockSchedule(List.of(initialTerm(), secondTerm(), otherTerm));
    when(licenceScheduleTermRepository.findById(otherTermId)).thenReturn(Optional.of(otherTerm));
    when(recordOfDecisionReductionRepository
        .findByScheduleWorkProgrammeApplicationDetailAndLicenceScheduleTermId(applicationDetail, otherTermId))
        .thenReturn(Optional.empty());
    mockReductions(existing);

    var form = new RecordReductionDetailsForm();
    form.getSelectedTerm().put(termId.toString(), false);
    form.getSelectedTerm().put(otherTermId.toString(), true);
    form.getReductionDuration().put(termId.toString(), durationInput(termId, null, null, null));
    form.getReductionDuration().put(otherTermId.toString(), durationInput(otherTermId, "1", "0", "0"));

    recordReductionDetailsService.saveReductionDetails(form, applicationDetail);

    verify(recordOfDecisionReductionRepository).delete(existing);
  }

  @Test
  void saveReductionDetails_whenReductionStillSelected_isNotDeleted() {
    var term = secondTerm();
    var existing = new RecordOfDecisionReduction();
    existing.setLicenceScheduleTerm(term);
    mockSchedule(List.of(initialTerm(), term));
    when(recordOfDecisionReductionRepository
        .findByScheduleWorkProgrammeApplicationDetailAndLicenceScheduleTermId(applicationDetail, termId))
        .thenReturn(Optional.of(existing));
    when(licenceScheduleTermRepository.findById(termId)).thenReturn(Optional.of(term));
    mockReductions(existing);

    var form = new RecordReductionDetailsForm();
    form.getSelectedTerm().put(termId.toString(), true);
    form.getReductionDuration().put(termId.toString(), durationInput(termId, "1", "0", "0"));

    recordReductionDetailsService.saveReductionDetails(form, applicationDetail);

    verify(recordOfDecisionReductionRepository).save(existing);
    verify(recordOfDecisionReductionRepository, never()).delete(existing);
  }

  @Test
  void saveReductionDetails_whenNothingIsSelected_deletesNothing() {
    mockSchedule(List.of(initialTerm(), secondTerm(), thirdTerm()));
    mockNoReductions();

    var form = new RecordReductionDetailsForm();
    form.getSelectedTerm().put(termId.toString(), false);
    form.getReductionDuration().put(termId.toString(), durationInput(termId, null, null, null));
    form.getReductionDuration().put(otherTermId.toString(), durationInput(otherTermId, null, null, null));

    recordReductionDetailsService.saveReductionDetails(form, applicationDetail);

    verify(recordOfDecisionReductionRepository, never()).save(any(RecordOfDecisionReduction.class));
    verify(recordOfDecisionReductionRepository, never()).delete(any(RecordOfDecisionReduction.class));
  }

  @Test
  void saveReductionDetails_whenSingleOptionAndNothingSelected_savesThatOption() {
    var term = secondTerm();
    mockSchedule(List.of(initialTerm(), term));
    when(recordOfDecisionReductionRepository
        .findByScheduleWorkProgrammeApplicationDetailAndLicenceScheduleTermId(applicationDetail, termId))
        .thenReturn(Optional.empty());
    when(licenceScheduleTermRepository.findById(termId)).thenReturn(Optional.of(term));
    mockNoReductions();

    var form = new RecordReductionDetailsForm();
    form.getReductionDuration().put(termId.toString(), durationInput(termId, "0", "3", "0"));

    recordReductionDetailsService.saveReductionDetails(form, applicationDetail);

    var captor = ArgumentCaptor.forClass(RecordOfDecisionReduction.class);
    verify(recordOfDecisionReductionRepository).save(captor.capture());
    assertThat(captor.getValue().getLicenceScheduleTerm()).isEqualTo(term);
    assertThat(captor.getValue().getReductionDuration()).isEqualTo(new ThreeFieldDuration(0, 3, 0));
  }

  @Test
  void saveReductionDetails_whenTheOnlySubmittedIdWasNotOffered_savesNothingAndDeletesNothing() {
    var unknownId = UUID.randomUUID();
    mockSchedule(List.of(initialTerm(), secondTerm()));
    mockNoReductions();

    var form = new RecordReductionDetailsForm();
    form.getReductionDuration().put(unknownId.toString(), durationInput(unknownId, "1", "0", "0"));

    recordReductionDetailsService.saveReductionDetails(form, applicationDetail);

    verify(recordOfDecisionReductionRepository, never()).save(any(RecordOfDecisionReduction.class));
    verify(recordOfDecisionReductionRepository, never()).delete(any(RecordOfDecisionReduction.class));
  }

  @Test
  void saveReductionDetails_whenSelectedTermDoesNotExist_throwsException() {
    mockSchedule(List.of(initialTerm(), secondTerm()));
    when(recordOfDecisionReductionRepository
        .findByScheduleWorkProgrammeApplicationDetailAndLicenceScheduleTermId(applicationDetail, termId))
        .thenReturn(Optional.empty());
    when(licenceScheduleTermRepository.findById(termId)).thenReturn(Optional.empty());
    mockNoReductions();

    var form = new RecordReductionDetailsForm();
    form.getSelectedTerm().put(termId.toString(), true);
    form.getReductionDuration().put(termId.toString(), durationInput(termId, "1", "0", "0"));

    assertThatThrownBy(() -> recordReductionDetailsService.saveReductionDetails(form, applicationDetail))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("LicenceScheduleTerm not found for ID: " + termId);
  }

  @Test
  void saveReductionDetails_whenSelectedPhaseDoesNotExist_throwsException() {
    var term = initialTerm();
    mockSchedule(List.of(term));
    when(licenceSchedulePhaseRepository.findAllByLicenceScheduleTerm(term))
        .thenReturn(List.of(phaseA(term), phaseB(term)));
    when(recordOfDecisionReductionRepository
        .findByScheduleWorkProgrammeApplicationDetailAndLicenceSchedulePhaseId(applicationDetail, phaseId))
        .thenReturn(Optional.empty());
    when(licenceSchedulePhaseRepository.findById(phaseId)).thenReturn(Optional.empty());
    mockNoReductions();

    var form = new RecordReductionDetailsForm();
    form.getSelectedPhase().put(phaseId.toString(), true);
    form.getReductionDuration().put(phaseId.toString(), durationInput(phaseId, "0", "6", "0"));

    assertThatThrownBy(() -> recordReductionDetailsService.saveReductionDetails(form, applicationDetail))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("LicenceSchedulePhase not found for ID: " + phaseId);
  }

  private void mockSchedule(List<LicenceScheduleTerm> terms) {
    when(clock.instant()).thenReturn(Instant.parse("2026-08-13T10:00:00.00Z"));
    when(clock.getZone()).thenReturn(ZoneOffset.UTC);
    when(scheduleWorkProgrammeApplicationService.getScheduleDetailFromApplicationDetail(applicationDetail))
        .thenReturn(licenceScheduleDetail);
    when(licenceScheduleTermRepository.findAllByLicenceScheduleDetail(licenceScheduleDetail)).thenReturn(terms);
  }

  private void mockNoReductions() {
    when(recordOfDecisionReductionRepository.findAllByScheduleWorkProgrammeApplicationDetail(applicationDetail))
        .thenReturn(List.of());
  }

  private void mockReductions(RecordOfDecisionReduction... reductions) {
    when(recordOfDecisionReductionRepository.findAllByScheduleWorkProgrammeApplicationDetail(applicationDetail))
        .thenReturn(List.of(reductions));
  }

  private LicenceScheduleTerm initialTerm() {
    return LicenceScheduleTermTestUtil.builder()
        .withId(firstTermId)
        .withTermType(TermType.INITIAL)
        .withEndDate(LocalDate.of(2027, 1, 17))
        .build();
  }

  private LicenceScheduleTerm secondTerm() {
    return LicenceScheduleTermTestUtil.builder()
        .withId(termId)
        .withTermType(TermType.SECOND)
        .withEndDate(LocalDate.of(2027, 7, 17))
        .build();
  }

  private LicenceScheduleTerm thirdTerm() {
    return LicenceScheduleTermTestUtil.builder()
        .withId(otherTermId)
        .withTermType(TermType.THIRD)
        .withEndDate(LocalDate.of(2030, 7, 17))
        .build();
  }

  private LicenceScheduleTerm currentSecondTerm() {
    return LicenceScheduleTermTestUtil.builder()
        .withId(termId)
        .withTermType(TermType.SECOND)
        .withStartDate(LocalDate.of(2026, 1, 17))
        .withEndDate(LocalDate.of(2027, 7, 17))
        .build();
  }

  private LicenceScheduleTerm secondTermEndingToday() {
    return LicenceScheduleTermTestUtil.builder()
        .withId(termId)
        .withTermType(TermType.SECOND)
        .withEndDate(LocalDate.of(2026, 8, 13))
        .build();
  }

  private LicenceScheduleTerm endedSecondTerm() {
    return LicenceScheduleTermTestUtil.builder()
        .withId(termId)
        .withTermType(TermType.SECOND)
        .withEndDate(LocalDate.of(2026, 8, 12))
        .build();
  }

  private LicenceSchedulePhase phaseA(LicenceScheduleTerm term) {
    return LicenceSchedulePhaseTestUtil.builder()
        .withId(firstPhaseId)
        .withLicenceScheduleTerm(term)
        .withPhaseType(PhaseType.PHASE_A)
        .withEndDate(LocalDate.of(2026, 11, 17))
        .build();
  }

  private LicenceSchedulePhase phaseB(LicenceScheduleTerm term) {
    return LicenceSchedulePhaseTestUtil.builder()
        .withId(phaseId)
        .withLicenceScheduleTerm(term)
        .withPhaseType(PhaseType.PHASE_B)
        .withEndDate(LocalDate.of(2027, 5, 17))
        .build();
  }

  private LicenceSchedulePhase phaseC(LicenceScheduleTerm term) {
    return LicenceSchedulePhaseTestUtil.builder()
        .withId(UUID.randomUUID())
        .withLicenceScheduleTerm(term)
        .withPhaseType(PhaseType.PHASE_C)
        .withEndDate(LocalDate.of(2027, 11, 17))
        .build();
  }

  private LicenceSchedulePhase endedPhaseB(LicenceScheduleTerm term) {
    return LicenceSchedulePhaseTestUtil.builder()
        .withId(phaseId)
        .withLicenceScheduleTerm(term)
        .withPhaseType(PhaseType.PHASE_B)
        .withEndDate(LocalDate.of(2026, 8, 12))
        .build();
  }

  private RecordOfDecisionReduction reductionWithDuration(ThreeFieldDuration duration) {
    var reduction = new RecordOfDecisionReduction();
    reduction.setReductionDuration(duration);
    return reduction;
  }

  private ThreeFieldDurationInput durationInput(UUID id, String years, String months, String days) {
    var input = RecordReductionDetailsForm.newDurationInput(id.toString());
    input.setYears(years);
    input.setMonths(months);
    input.setDays(days);
    return input;
  }
}
