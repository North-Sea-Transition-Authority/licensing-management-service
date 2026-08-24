package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
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
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDurationInput;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceSchedulePhaseTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceScheduleTermTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.PhaseType;
import uk.co.nstauthority.licensingmanagementservice.licence.TermType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhaseRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTermRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.extendjourney.LicenceScheduleExtensionService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.extendjourney.LicenceScheduleTermAndPhases;

@ExtendWith(MockitoExtension.class)
class RecordExtensionDetailsServiceTest {

  @Mock
  private RecordOfDecisionExtensionRepository recordOfDecisionExtensionRepository;

  @Mock
  private LicenceScheduleExtensionService licenceScheduleExtensionService;

  @Mock
  private ScheduleWorkProgrammeApplicationService scheduleWorkProgrammeApplicationService;

  @Mock
  private LicenceSchedulePhaseRepository licenceSchedulePhaseRepository;

  @Mock
  private LicenceScheduleTermRepository licenceScheduleTermRepository;

  @Mock
  private LicenceScheduleDetail licenceScheduleDetail;

  @InjectMocks
  private RecordExtensionDetailsService recordExtensionDetailsService;

  private ScheduleWorkProgrammeApplicationDetail applicationDetail;
  private final UUID termId = UUID.randomUUID();
  private final UUID phaseId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    applicationDetail = ScheduleWorkProgrammeApplicationDetailTestUtil.builder()
        .withId(UUID.randomUUID())
        .build();
  }

  @Test
  void hasExtensionDetails_delegatesToRepository() {
    when(recordOfDecisionExtensionRepository.existsByScheduleWorkProgrammeApplicationDetail(applicationDetail))
        .thenReturn(true);

    assertThat(recordExtensionDetailsService.hasExtensionDetails(applicationDetail)).isTrue();
  }

  @Test
  void getTotalExtensionDuration_whenNoExtensions_returnsZero() {
    when(recordOfDecisionExtensionRepository.findAllByScheduleWorkProgrammeApplicationDetail(applicationDetail))
        .thenReturn(List.of());

    assertThat(recordExtensionDetailsService.getTotalExtensionDuration(applicationDetail))
        .isEqualTo(new ThreeFieldDuration(0, 0, 0));
  }

  @Test
  void getTotalExtensionDuration_sumsEveryExtensionAndCarriesMonthsIntoYears() {
    when(recordOfDecisionExtensionRepository.findAllByScheduleWorkProgrammeApplicationDetail(applicationDetail))
        .thenReturn(List.of(
            extensionWithDuration(new ThreeFieldDuration(0, 8, 10)),
            extensionWithDuration(new ThreeFieldDuration(1, 6, 5))));

    assertThat(recordExtensionDetailsService.getTotalExtensionDuration(applicationDetail))
        .isEqualTo(new ThreeFieldDuration(2, 2, 15));
  }

  @Test
  void getExtensionDetailsViews_buildsViewWithEndDateAndRequestedState() {
    mockExtendableTerm();
    when(recordOfDecisionExtensionRepository.findAllByScheduleWorkProgrammeApplicationDetail(applicationDetail))
        .thenReturn(List.of());

    var views = recordExtensionDetailsService.getExtensionDetailsViews(applicationDetail);

    assertThat(views).hasSize(1);
    var view = views.getFirst();
    assertThat(view.id()).isEqualTo(termId.toString());
    assertThat(view.displayName()).isEqualTo(TermType.SECOND.getDisplayName());
    assertThat(view.endDate()).isNotBlank();
    assertThat(view.isPhase()).isFalse();
    assertThat(view.isRequested()).isFalse();
    assertThat(view.duration()).isNull();
  }

  @Test
  void getExtensionDetailsViews_whenExtensionSaved_marksRequestedWithDuration() {
    mockExtendableTerm();
    var savedExtension = new RecordOfDecisionExtension();
    savedExtension.setLicenceScheduleTerm(LicenceScheduleTermTestUtil.builder().withId(termId).build());
    savedExtension.setExtensionDuration(new ThreeFieldDuration(1, 0, 0));
    when(recordOfDecisionExtensionRepository.findAllByScheduleWorkProgrammeApplicationDetail(applicationDetail))
        .thenReturn(List.of(savedExtension));

    var view = recordExtensionDetailsService.getExtensionDetailsViews(applicationDetail).getFirst();

    assertThat(view.isRequested()).isTrue();
    assertThat(view.duration()).isEqualTo(new ThreeFieldDuration(1, 0, 0));
  }

  @Test
  void getFilledForm_populatesSelectionAndDuration() {
    mockExtendableTerm();
    var savedExtension = new RecordOfDecisionExtension();
    savedExtension.setLicenceScheduleTerm(LicenceScheduleTermTestUtil.builder().withId(termId).build());
    savedExtension.setExtensionDuration(new ThreeFieldDuration(2, 3, 0));
    when(recordOfDecisionExtensionRepository.findAllByScheduleWorkProgrammeApplicationDetail(applicationDetail))
        .thenReturn(List.of(savedExtension));

    var form = recordExtensionDetailsService.getFilledForm(applicationDetail);

    assertThat(form.getSelectedTerm()).containsEntry(termId.toString(), true);
    assertThat(form.getExtensionDuration().get(termId.toString()).getYears()).isEqualTo("2");
    assertThat(form.getExtensionDuration().get(termId.toString()).getMonths()).isEqualTo("3");
  }

  @Test
  void saveExtensionDetails_savesSelectedTerm() {
    var term = LicenceScheduleTermTestUtil.builder().withId(termId).build();
    when(recordOfDecisionExtensionRepository
        .findByScheduleWorkProgrammeApplicationDetailAndLicenceScheduleTermId(applicationDetail, termId))
        .thenReturn(Optional.empty());
    when(licenceScheduleTermRepository.findById(termId)).thenReturn(Optional.of(term));
    when(recordOfDecisionExtensionRepository.findAllByScheduleWorkProgrammeApplicationDetail(applicationDetail))
        .thenReturn(List.of());

    var form = new RecordExtensionDetailsForm();
    form.getSelectedTerm().put(termId.toString(), true);
    form.getExtensionDuration().put(termId.toString(), durationInput("1", "0", "0"));

    recordExtensionDetailsService.saveExtensionDetails(form, applicationDetail);

    var captor = ArgumentCaptor.forClass(RecordOfDecisionExtension.class);
    verify(recordOfDecisionExtensionRepository).save(captor.capture());
    assertThat(captor.getValue().getScheduleWorkProgrammeApplicationDetail()).isEqualTo(applicationDetail);
    assertThat(captor.getValue().getLicenceScheduleTerm()).isEqualTo(term);
    assertThat(captor.getValue().getLicenceSchedulePhase()).isNull();
    assertThat(captor.getValue().getExtensionDuration()).isEqualTo(new ThreeFieldDuration(1, 0, 0));
  }

  @Test
  void saveExtensionDetails_whenADifferentOptionIsSelected_deletesTheUnselectedExtension() {
    var otherTermId = UUID.randomUUID();
    var otherTerm = LicenceScheduleTermTestUtil.builder().withId(otherTermId).build();
    var existing = new RecordOfDecisionExtension();
    existing.setLicenceScheduleTerm(LicenceScheduleTermTestUtil.builder().withId(termId).build());

    when(licenceScheduleTermRepository.findById(otherTermId)).thenReturn(Optional.of(otherTerm));
    when(recordOfDecisionExtensionRepository
        .findByScheduleWorkProgrammeApplicationDetailAndLicenceScheduleTermId(applicationDetail, otherTermId))
        .thenReturn(Optional.empty());
    when(recordOfDecisionExtensionRepository.findAllByScheduleWorkProgrammeApplicationDetail(applicationDetail))
        .thenReturn(List.of(existing));

    var form = new RecordExtensionDetailsForm();
    form.getSelectedTerm().put(termId.toString(), false);
    form.getSelectedTerm().put(otherTermId.toString(), true);
    form.getExtensionDuration().put(termId.toString(), durationInput(null, null, null));
    form.getExtensionDuration().put(otherTermId.toString(), durationInput("1", "0", "0"));

    recordExtensionDetailsService.saveExtensionDetails(form, applicationDetail);

    verify(recordOfDecisionExtensionRepository).delete(existing);
  }

  @Test
  void saveExtensionDetails_whenNothingIsSelected_deletesNothing() {
    var form = new RecordExtensionDetailsForm();
    form.getSelectedTerm().put(termId.toString(), false);
    form.getExtensionDuration().put(termId.toString(), durationInput(null, null, null));
    form.getExtensionDuration().put(UUID.randomUUID().toString(), durationInput(null, null, null));

    recordExtensionDetailsService.saveExtensionDetails(form, applicationDetail);

    verify(recordOfDecisionExtensionRepository, never()).delete(any(RecordOfDecisionExtension.class));
  }

  @Test
  void getExtensionDetailsViews_whenTermHasPhases_buildsPhaseViews() {
    mockExtendablePhase();
    when(recordOfDecisionExtensionRepository.findAllByScheduleWorkProgrammeApplicationDetail(applicationDetail))
        .thenReturn(List.of());

    var views = recordExtensionDetailsService.getExtensionDetailsViews(applicationDetail);

    assertThat(views).hasSize(1);
    var view = views.getFirst();
    assertThat(view.id()).isEqualTo(phaseId.toString());
    assertThat(view.displayName()).isEqualTo(PhaseType.PHASE_A.getDisplayName());
    assertThat(view.endDate()).isEqualTo("17 May 2025");
    assertThat(view.isPhase()).isTrue();
    assertThat(view.isRequested()).isFalse();
    assertThat(view.duration()).isNull();
  }

  @Test
  void getExtensionDetailsViews_whenPhaseExtensionSaved_marksRequestedWithDuration() {
    mockExtendablePhase();
    var savedExtension = new RecordOfDecisionExtension();
    savedExtension.setLicenceSchedulePhase(LicenceSchedulePhaseTestUtil.builder().withId(phaseId).build());
    savedExtension.setExtensionDuration(new ThreeFieldDuration(0, 6, 0));
    when(recordOfDecisionExtensionRepository.findAllByScheduleWorkProgrammeApplicationDetail(applicationDetail))
        .thenReturn(List.of(savedExtension));

    var view = recordExtensionDetailsService.getExtensionDetailsViews(applicationDetail).getFirst();

    assertThat(view.isRequested()).isTrue();
    assertThat(view.duration()).isEqualTo(new ThreeFieldDuration(0, 6, 0));
  }

  @Test
  void getFilledForm_whenPhaseNotYetExtended_populatesUnselectedPhaseAndEmptyDuration() {
    mockExtendablePhase();
    when(recordOfDecisionExtensionRepository.findAllByScheduleWorkProgrammeApplicationDetail(applicationDetail))
        .thenReturn(List.of());

    var form = recordExtensionDetailsService.getFilledForm(applicationDetail);

    assertThat(form.getSelectedPhase()).containsEntry(phaseId.toString(), false);
    assertThat(form.getSelectedTerm()).isEmpty();
    assertThat(form.getExtensionDuration().get(phaseId.toString()).getYears()).isNull();
  }

  @Test
  void saveExtensionDetails_savesSelectedPhase() {
    var phase = LicenceSchedulePhaseTestUtil.builder().withId(phaseId).build();
    when(recordOfDecisionExtensionRepository
        .findByScheduleWorkProgrammeApplicationDetailAndLicenceSchedulePhaseId(applicationDetail, phaseId))
        .thenReturn(Optional.empty());
    when(licenceSchedulePhaseRepository.findById(phaseId)).thenReturn(Optional.of(phase));
    when(recordOfDecisionExtensionRepository.findAllByScheduleWorkProgrammeApplicationDetail(applicationDetail))
        .thenReturn(List.of());

    var form = new RecordExtensionDetailsForm();
    form.getSelectedPhase().put(phaseId.toString(), true);
    form.getExtensionDuration().put(phaseId.toString(), durationInput("0", "6", "0"));

    recordExtensionDetailsService.saveExtensionDetails(form, applicationDetail);

    var captor = ArgumentCaptor.forClass(RecordOfDecisionExtension.class);
    verify(recordOfDecisionExtensionRepository).save(captor.capture());
    assertThat(captor.getValue().getScheduleWorkProgrammeApplicationDetail()).isEqualTo(applicationDetail);
    assertThat(captor.getValue().getLicenceSchedulePhase()).isEqualTo(phase);
    assertThat(captor.getValue().getLicenceScheduleTerm()).isNull();
    assertThat(captor.getValue().getExtensionDuration()).isEqualTo(new ThreeFieldDuration(0, 6, 0));
  }

  @Test
  void saveExtensionDetails_whenSelectedPhaseDoesNotExist_throwsException() {
    when(recordOfDecisionExtensionRepository
        .findByScheduleWorkProgrammeApplicationDetailAndLicenceSchedulePhaseId(applicationDetail, phaseId))
        .thenReturn(Optional.empty());
    when(licenceSchedulePhaseRepository.findById(phaseId)).thenReturn(Optional.empty());

    var form = new RecordExtensionDetailsForm();
    form.getSelectedPhase().put(phaseId.toString(), true);
    form.getExtensionDuration().put(phaseId.toString(), durationInput("0", "6", "0"));

    assertThatThrownBy(() -> recordExtensionDetailsService.saveExtensionDetails(form, applicationDetail))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("LicenceSchedulePhase not found for ID: " + phaseId);
  }

  @Test
  void saveExtensionDetails_whenSelectedTermDoesNotExist_throwsException() {
    when(recordOfDecisionExtensionRepository
        .findByScheduleWorkProgrammeApplicationDetailAndLicenceScheduleTermId(applicationDetail, termId))
        .thenReturn(Optional.empty());
    when(licenceScheduleTermRepository.findById(termId)).thenReturn(Optional.empty());

    var form = new RecordExtensionDetailsForm();
    form.getSelectedTerm().put(termId.toString(), true);
    form.getExtensionDuration().put(termId.toString(), durationInput("1", "0", "0"));

    assertThatThrownBy(() -> recordExtensionDetailsService.saveExtensionDetails(form, applicationDetail))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("LicenceScheduleTerm not found for ID: " + termId);
  }

  @Test
  void saveExtensionDetails_whenSingleOptionAndNothingSelected_savesThatOption() {
    var phase = LicenceSchedulePhaseTestUtil.builder().withId(phaseId).build();
    when(licenceSchedulePhaseRepository.existsById(phaseId)).thenReturn(true);
    when(licenceScheduleTermRepository.existsById(phaseId)).thenReturn(false);
    when(recordOfDecisionExtensionRepository
        .findByScheduleWorkProgrammeApplicationDetailAndLicenceSchedulePhaseId(applicationDetail, phaseId))
        .thenReturn(Optional.empty());
    when(licenceSchedulePhaseRepository.findById(phaseId)).thenReturn(Optional.of(phase));
    when(recordOfDecisionExtensionRepository.findAllByScheduleWorkProgrammeApplicationDetail(applicationDetail))
        .thenReturn(List.of());

    var form = new RecordExtensionDetailsForm();
    form.getExtensionDuration().put(phaseId.toString(), durationInput("0", "3", "0"));

    recordExtensionDetailsService.saveExtensionDetails(form, applicationDetail);

    var captor = ArgumentCaptor.forClass(RecordOfDecisionExtension.class);
    verify(recordOfDecisionExtensionRepository).save(captor.capture());
    assertThat(captor.getValue().getLicenceSchedulePhase()).isEqualTo(phase);
    assertThat(captor.getValue().getExtensionDuration()).isEqualTo(new ThreeFieldDuration(0, 3, 0));
  }

  @Test
  void saveExtensionDetails_whenSingleOptionIsNeitherTermNorPhase_savesNothingAndDeletesNothing() {
    var unknownId = UUID.randomUUID();
    when(licenceSchedulePhaseRepository.existsById(unknownId)).thenReturn(false);
    when(licenceScheduleTermRepository.existsById(unknownId)).thenReturn(false);

    var form = new RecordExtensionDetailsForm();
    form.getExtensionDuration().put(unknownId.toString(), durationInput("1", "0", "0"));

    recordExtensionDetailsService.saveExtensionDetails(form, applicationDetail);

    verify(recordOfDecisionExtensionRepository, never()).save(any(RecordOfDecisionExtension.class));
    verify(recordOfDecisionExtensionRepository, never()).delete(any(RecordOfDecisionExtension.class));
  }

  @Test
  void saveExtensionDetails_whenIdIsUnknown_doesNotDeletePreviouslyRecordedExtensions() {
    var unknownId = UUID.randomUUID();
    when(licenceSchedulePhaseRepository.existsById(unknownId)).thenReturn(false);
    when(licenceScheduleTermRepository.existsById(unknownId)).thenReturn(false);

    var form = new RecordExtensionDetailsForm();
    form.getExtensionDuration().put(unknownId.toString(), durationInput("1", "0", "0"));

    recordExtensionDetailsService.saveExtensionDetails(form, applicationDetail);

    verify(recordOfDecisionExtensionRepository, never())
        .findAllByScheduleWorkProgrammeApplicationDetail(applicationDetail);
    verify(recordOfDecisionExtensionRepository, never()).delete(any(RecordOfDecisionExtension.class));
  }

  @Test
  void saveExtensionDetails_whenSelectionMapsAreNull_savesNothingAndDeletesNothing() {
    var form = new RecordExtensionDetailsForm();
    form.setSelectedPhase(null);
    form.setSelectedTerm(null);

    recordExtensionDetailsService.saveExtensionDetails(form, applicationDetail);

    verify(recordOfDecisionExtensionRepository, never()).save(any(RecordOfDecisionExtension.class));
    verify(recordOfDecisionExtensionRepository, never()).delete(any(RecordOfDecisionExtension.class));
  }

  @Test
  void saveExtensionDetails_whenExtensionStillSelected_isNotDeleted() {
    var term = LicenceScheduleTermTestUtil.builder().withId(termId).build();
    var existing = new RecordOfDecisionExtension();
    existing.setLicenceScheduleTerm(term);
    when(recordOfDecisionExtensionRepository
        .findByScheduleWorkProgrammeApplicationDetailAndLicenceScheduleTermId(applicationDetail, termId))
        .thenReturn(Optional.of(existing));
    when(licenceScheduleTermRepository.findById(termId)).thenReturn(Optional.of(term));
    when(recordOfDecisionExtensionRepository.findAllByScheduleWorkProgrammeApplicationDetail(applicationDetail))
        .thenReturn(List.of(existing));

    var form = new RecordExtensionDetailsForm();
    form.getSelectedTerm().put(termId.toString(), true);
    form.getExtensionDuration().put(termId.toString(), durationInput("1", "0", "0"));

    recordExtensionDetailsService.saveExtensionDetails(form, applicationDetail);

    verify(recordOfDecisionExtensionRepository).save(existing);
    verify(recordOfDecisionExtensionRepository, never()).delete(existing);
  }

  private void mockExtendableTerm() {
    var term = LicenceScheduleTermTestUtil.builder()
        .withId(termId)
        .withTermType(TermType.SECOND)
        .withEndDate(LocalDate.of(2026, 7, 17))
        .build();
    when(scheduleWorkProgrammeApplicationService.getScheduleDetailFromApplicationDetail(applicationDetail))
        .thenReturn(licenceScheduleDetail);
    when(licenceScheduleExtensionService.getExtendableTermAndPhases(licenceScheduleDetail))
        .thenReturn(List.of(new LicenceScheduleTermAndPhases(termId.toString(), TermType.SECOND.getDisplayName(), List.of())));
    when(licenceScheduleTermRepository.findById(termId)).thenReturn(Optional.of(term));
  }

  private void mockExtendablePhase() {
    var phase = LicenceSchedulePhaseTestUtil.builder()
        .withId(phaseId)
        .withPhaseType(PhaseType.PHASE_A)
        .withEndDate(LocalDate.of(2025, 5, 17))
        .build();
    when(scheduleWorkProgrammeApplicationService.getScheduleDetailFromApplicationDetail(applicationDetail))
        .thenReturn(licenceScheduleDetail);
    when(licenceScheduleExtensionService.getExtendableTermAndPhases(licenceScheduleDetail))
        .thenReturn(List.of(new LicenceScheduleTermAndPhases(
            null,
            TermType.INITIAL.getDisplayName(),
            List.of(new LicenceScheduleTermAndPhases.PhaseDetails(
                phaseId.toString(),
                PhaseType.PHASE_A.getDisplayName())))));
    when(licenceSchedulePhaseRepository.findById(phaseId)).thenReturn(Optional.of(phase));
  }

  private RecordOfDecisionExtension extensionWithDuration(ThreeFieldDuration duration) {
    var extension = new RecordOfDecisionExtension();
    extension.setExtensionDuration(duration);
    return extension;
  }

  private ThreeFieldDurationInput durationInput(String years, String months, String days) {
    var input = new ThreeFieldDurationInput("extensionDuration[" + termId + "]", "extension");
    input.setYears(years);
    input.setMonths(months);
    input.setDays(days);
    return input;
  }
}
