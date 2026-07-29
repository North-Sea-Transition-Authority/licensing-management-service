package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetailTestUtil;

@ExtendWith(MockitoExtension.class)
class RecordOfDecisionServiceTest {

  @Mock
  private RecordOfDecisionRepository recordOfDecisionRepository;

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
  void isExtensionDetailsSaved_returnsFalseUntilExtensionDetailsStepIsBuilt() {
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
}
