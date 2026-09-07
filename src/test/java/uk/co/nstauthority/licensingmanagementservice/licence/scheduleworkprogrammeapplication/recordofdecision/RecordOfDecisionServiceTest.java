package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetailTestUtil;

@ExtendWith(MockitoExtension.class)
class RecordOfDecisionServiceTest {

  @Mock
  private RecordOfDecisionRepository recordOfDecisionRepository;

  @Mock
  private RecordOfDecisionExtensionRepository recordOfDecisionExtensionRepository;

  @InjectMocks
  private RecordOfDecisionService recordOfDecisionService;

  private ScheduleWorkProgrammeApplicationDetail applicationDetail;

  @BeforeEach
  void setUp() {
    applicationDetail = ScheduleWorkProgrammeApplicationDetailTestUtil.builder()
        .withId(UUID.randomUUID())
        .build();
  }

  @Test
  void findByApplicationDetail_delegatesToRepository() {
    var recordOfDecision = new RecordOfDecision();
    when(recordOfDecisionRepository.findByScheduleWorkProgrammeApplicationDetail(applicationDetail))
        .thenReturn(Optional.of(recordOfDecision));

    assertThat(recordOfDecisionService.findByApplicationDetail(applicationDetail))
        .contains(recordOfDecision);
  }

  @Test
  void isExtensionApproved_whenGranted_returnsTrue() {
    var recordOfDecision = new RecordOfDecision();
    recordOfDecision.setExtensionDecision(RecordOfDecisionResponse.GRANTED);
    when(recordOfDecisionRepository.findByScheduleWorkProgrammeApplicationDetail(applicationDetail))
        .thenReturn(Optional.of(recordOfDecision));

    assertThat(recordOfDecisionService.isExtensionApproved(applicationDetail)).isTrue();
  }

  @Test
  void isExtensionApproved_whenRejected_returnsFalse() {
    var recordOfDecision = new RecordOfDecision();
    recordOfDecision.setExtensionDecision(RecordOfDecisionResponse.REJECTED);
    when(recordOfDecisionRepository.findByScheduleWorkProgrammeApplicationDetail(applicationDetail))
        .thenReturn(Optional.of(recordOfDecision));

    assertThat(recordOfDecisionService.isExtensionApproved(applicationDetail)).isFalse();
  }

  @Test
  void isExtensionApproved_whenNoRecord_returnsFalse() {
    when(recordOfDecisionRepository.findByScheduleWorkProgrammeApplicationDetail(applicationDetail))
        .thenReturn(Optional.empty());

    assertThat(recordOfDecisionService.isExtensionApproved(applicationDetail)).isFalse();
  }

  @Test
  void isExtensionDetailsSaved_whenExtensionRowsExist_returnsTrue() {
    when(recordOfDecisionExtensionRepository.existsByScheduleWorkProgrammeApplicationDetail(applicationDetail))
        .thenReturn(true);

    assertThat(recordOfDecisionService.isExtensionDetailsSaved(applicationDetail)).isTrue();
  }

  @Test
  void isExtensionDetailsSaved_whenNoExtensionRows_returnsFalse() {
    when(recordOfDecisionExtensionRepository.existsByScheduleWorkProgrammeApplicationDetail(applicationDetail))
        .thenReturn(false);

    assertThat(recordOfDecisionService.isExtensionDetailsSaved(applicationDetail)).isFalse();
  }

  @Test
  void isWorkProgrammeAmendmentApproved_whenGranted_returnsTrue() {
    var recordOfDecision = new RecordOfDecision();
    recordOfDecision.setWorkProgrammeDecision(RecordOfDecisionResponse.GRANTED);
    when(recordOfDecisionRepository.findByScheduleWorkProgrammeApplicationDetail(applicationDetail))
        .thenReturn(Optional.of(recordOfDecision));

    assertThat(recordOfDecisionService.isWorkProgrammeAmendmentApproved(applicationDetail)).isTrue();
  }

  @Test
  void isWorkProgrammeAmendmentApproved_whenNotRequested_returnsFalse() {
    var recordOfDecision = new RecordOfDecision();
    recordOfDecision.setWorkProgrammeDecision(RecordOfDecisionResponse.NOT_REQUESTED);
    when(recordOfDecisionRepository.findByScheduleWorkProgrammeApplicationDetail(applicationDetail))
        .thenReturn(Optional.of(recordOfDecision));

    assertThat(recordOfDecisionService.isWorkProgrammeAmendmentApproved(applicationDetail)).isFalse();
  }

  @Test
  void isWorkProgrammeAmendmentApproved_whenNoRecord_returnsFalse() {
    when(recordOfDecisionRepository.findByScheduleWorkProgrammeApplicationDetail(applicationDetail))
        .thenReturn(Optional.empty());

    assertThat(recordOfDecisionService.isWorkProgrammeAmendmentApproved(applicationDetail)).isFalse();
  }

  @Test
  void saveDecision_whenNoExistingRecord_createsWithFormValuesAndSaves() {
    when(recordOfDecisionRepository.findByScheduleWorkProgrammeApplicationDetail(applicationDetail))
        .thenReturn(Optional.empty());
    var form = new RecordDecisionForm();
    form.setExtensionDecision(RecordOfDecisionResponse.GRANTED);
    form.setWorkProgrammeDecision(RecordOfDecisionResponse.NOT_REQUESTED);

    recordOfDecisionService.saveDecision(applicationDetail, form);

    var captor = ArgumentCaptor.forClass(RecordOfDecision.class);
    verify(recordOfDecisionRepository).save(captor.capture());
    assertThat(captor.getValue().getScheduleWorkProgrammeApplicationDetail()).isEqualTo(applicationDetail);
    assertThat(captor.getValue().getExtensionDecision()).isEqualTo(RecordOfDecisionResponse.GRANTED);
    assertThat(captor.getValue().getWorkProgrammeDecision()).isEqualTo(RecordOfDecisionResponse.NOT_REQUESTED);
  }

  @Test
  void saveDecision_whenExistingRecord_updatesAndSaves() {
    var existingRecord = new RecordOfDecision();
    existingRecord.setScheduleWorkProgrammeApplicationDetail(applicationDetail);
    existingRecord.setExtensionDecision(RecordOfDecisionResponse.REJECTED);
    when(recordOfDecisionRepository.findByScheduleWorkProgrammeApplicationDetail(applicationDetail))
        .thenReturn(Optional.of(existingRecord));
    var form = new RecordDecisionForm();
    form.setExtensionDecision(RecordOfDecisionResponse.GRANTED);
    form.setWorkProgrammeDecision(RecordOfDecisionResponse.GRANTED);

    recordOfDecisionService.saveDecision(applicationDetail, form);

    assertThat(existingRecord.getExtensionDecision()).isEqualTo(RecordOfDecisionResponse.GRANTED);
    assertThat(existingRecord.getWorkProgrammeDecision()).isEqualTo(RecordOfDecisionResponse.GRANTED);
    verify(recordOfDecisionRepository).save(existingRecord);
  }

  @Test
  void getFilledDecisionForm_whenRecordExists_populatesForm() {
    var recordOfDecision = new RecordOfDecision();
    recordOfDecision.setExtensionDecision(RecordOfDecisionResponse.GRANTED);
    recordOfDecision.setWorkProgrammeDecision(RecordOfDecisionResponse.NOT_REQUESTED);
    when(recordOfDecisionRepository.findByScheduleWorkProgrammeApplicationDetail(applicationDetail))
        .thenReturn(Optional.of(recordOfDecision));

    var form = recordOfDecisionService.getFilledDecisionForm(applicationDetail);

    assertThat(form.getExtensionDecision()).isEqualTo(RecordOfDecisionResponse.GRANTED);
    assertThat(form.getWorkProgrammeDecision()).isEqualTo(RecordOfDecisionResponse.NOT_REQUESTED);
  }

  @Test
  void getFilledDecisionForm_whenNoRecord_returnsEmptyForm() {
    when(recordOfDecisionRepository.findByScheduleWorkProgrammeApplicationDetail(applicationDetail))
        .thenReturn(Optional.empty());

    var form = recordOfDecisionService.getFilledDecisionForm(applicationDetail);

    assertThat(form.getExtensionDecision()).isNull();
    assertThat(form.getWorkProgrammeDecision()).isNull();
  }

  @Test
  void getFilledWorkProgrammeSummaryForm_whenRecordExists_populatesForm() {
    var recordOfDecision = new RecordOfDecision();
    recordOfDecision.setWorkProgrammeSummaryOption(RecordWorkProgrammeAmendmentSummaryOptions.NO_LATER);
    when(recordOfDecisionRepository.findByScheduleWorkProgrammeApplicationDetail(applicationDetail))
        .thenReturn(Optional.of(recordOfDecision));

    var form = recordOfDecisionService.getFilledWorkProgrammeSummaryForm(applicationDetail);

    assertThat(form.getRecordWorkProgrammeAmendmentSummaryOptions())
        .isEqualTo(RecordWorkProgrammeAmendmentSummaryOptions.NO_LATER);
  }

  @Test
  void getFilledWorkProgrammeSummaryForm_whenNoRecord_returnsEmptyForm() {
    when(recordOfDecisionRepository.findByScheduleWorkProgrammeApplicationDetail(applicationDetail))
        .thenReturn(Optional.empty());

    var form = recordOfDecisionService.getFilledWorkProgrammeSummaryForm(applicationDetail);

    assertThat(form.getRecordWorkProgrammeAmendmentSummaryOptions()).isNull();
  }

  @Test
  void saveWorkProgrammeSummaryOption_whenNoExistingRecord_createsWithOptionAndSaves() {
    var form = new RecordWorkProgrammeAmendmentSummaryForm();
    form.setRecordWorkProgrammeAmendmentSummaryOptions(RecordWorkProgrammeAmendmentSummaryOptions.NO_ALL_ADDED);
    when(recordOfDecisionRepository.findByScheduleWorkProgrammeApplicationDetail(applicationDetail))
        .thenReturn(Optional.empty());

    recordOfDecisionService.saveWorkProgrammeSummaryOption(applicationDetail, form);

    var captor = ArgumentCaptor.forClass(RecordOfDecision.class);
    verify(recordOfDecisionRepository).save(captor.capture());
    assertThat(captor.getValue().getScheduleWorkProgrammeApplicationDetail()).isEqualTo(applicationDetail);
    assertThat(captor.getValue().getWorkProgrammeSummaryOption())
        .isEqualTo(RecordWorkProgrammeAmendmentSummaryOptions.NO_ALL_ADDED);
  }

  @Test
  void saveWorkProgrammeSummaryOption_whenExistingRecord_updatesOptionAndLeavesDecisionsUntouched() {
    var existingRecord = new RecordOfDecision();
    existingRecord.setScheduleWorkProgrammeApplicationDetail(applicationDetail);
    existingRecord.setExtensionDecision(RecordOfDecisionResponse.GRANTED);
    existingRecord.setWorkProgrammeDecision(RecordOfDecisionResponse.GRANTED);
    existingRecord.setWorkProgrammeSummaryOption(RecordWorkProgrammeAmendmentSummaryOptions.YES_NOW);
    var form = new RecordWorkProgrammeAmendmentSummaryForm();
    form.setRecordWorkProgrammeAmendmentSummaryOptions(RecordWorkProgrammeAmendmentSummaryOptions.NO_ALL_ADDED);
    when(recordOfDecisionRepository.findByScheduleWorkProgrammeApplicationDetail(applicationDetail))
        .thenReturn(Optional.of(existingRecord));

    recordOfDecisionService.saveWorkProgrammeSummaryOption(applicationDetail, form);

    assertThat(existingRecord.getWorkProgrammeSummaryOption())
        .isEqualTo(RecordWorkProgrammeAmendmentSummaryOptions.NO_ALL_ADDED);
    assertThat(existingRecord.getExtensionDecision()).isEqualTo(RecordOfDecisionResponse.GRANTED);
    assertThat(existingRecord.getWorkProgrammeDecision()).isEqualTo(RecordOfDecisionResponse.GRANTED);
    verify(recordOfDecisionRepository).save(existingRecord);
  }

  @Test
  void isWorkProgrammeAmendmentDetailsComplete_whenAllAdded_returnsTrue() {
    var recordOfDecision = new RecordOfDecision();
    recordOfDecision.setWorkProgrammeSummaryOption(RecordWorkProgrammeAmendmentSummaryOptions.NO_ALL_ADDED);
    when(recordOfDecisionRepository.findByScheduleWorkProgrammeApplicationDetail(applicationDetail))
        .thenReturn(Optional.of(recordOfDecision));

    assertThat(recordOfDecisionService.isWorkProgrammeAmendmentDetailsComplete(applicationDetail)).isTrue();
  }

  @ParameterizedTest
  @EnumSource(value = RecordWorkProgrammeAmendmentSummaryOptions.class, names = {"YES_NOW", "NO_LATER"})
  void isWorkProgrammeAmendmentDetailsComplete_whenMoreToAdd_returnsFalse(
      RecordWorkProgrammeAmendmentSummaryOptions option
  ) {
    var recordOfDecision = new RecordOfDecision();
    recordOfDecision.setWorkProgrammeSummaryOption(option);
    when(recordOfDecisionRepository.findByScheduleWorkProgrammeApplicationDetail(applicationDetail))
        .thenReturn(Optional.of(recordOfDecision));

    assertThat(recordOfDecisionService.isWorkProgrammeAmendmentDetailsComplete(applicationDetail)).isFalse();
  }

  @Test
  void isWorkProgrammeAmendmentDetailsComplete_whenNoRecord_returnsFalse() {
    when(recordOfDecisionRepository.findByScheduleWorkProgrammeApplicationDetail(applicationDetail))
        .thenReturn(Optional.empty());

    assertThat(recordOfDecisionService.isWorkProgrammeAmendmentDetailsComplete(applicationDetail)).isFalse();
  }
}
