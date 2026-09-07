package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision;

import jakarta.transaction.Transactional;
import java.util.Optional;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;

@Service
public class RecordOfDecisionService {

  private final RecordOfDecisionRepository recordOfDecisionRepository;
  private final RecordOfDecisionExtensionRepository recordOfDecisionExtensionRepository;

  public RecordOfDecisionService(
      RecordOfDecisionRepository recordOfDecisionRepository,
      RecordOfDecisionExtensionRepository recordOfDecisionExtensionRepository
  ) {
    this.recordOfDecisionRepository = recordOfDecisionRepository;
    this.recordOfDecisionExtensionRepository = recordOfDecisionExtensionRepository;
  }

  public Optional<RecordOfDecision> findByApplicationDetail(ScheduleWorkProgrammeApplicationDetail applicationDetail) {
    return recordOfDecisionRepository.findByScheduleWorkProgrammeApplicationDetail(applicationDetail);
  }

  public RecordDecisionForm getFilledDecisionForm(ScheduleWorkProgrammeApplicationDetail applicationDetail) {
    var form = new RecordDecisionForm();
    findByApplicationDetail(applicationDetail).ifPresent(recordOfDecision -> {
      form.setExtensionDecision(recordOfDecision.getExtensionDecision());
      form.setWorkProgrammeDecision(recordOfDecision.getWorkProgrammeDecision());
    });
    return form;
  }

  @Transactional
  public void saveDecision(ScheduleWorkProgrammeApplicationDetail applicationDetail, RecordDecisionForm form) {
    var recordOfDecision = getOrCreate(applicationDetail);
    recordOfDecision.setExtensionDecision(form.getExtensionDecision());
    recordOfDecision.setWorkProgrammeDecision(form.getWorkProgrammeDecision());
    recordOfDecisionRepository.save(recordOfDecision);
  }

  public RecordWorkProgrammeAmendmentSummaryForm getFilledWorkProgrammeSummaryForm(
      ScheduleWorkProgrammeApplicationDetail applicationDetail
  ) {
    var form = new RecordWorkProgrammeAmendmentSummaryForm();
    findByApplicationDetail(applicationDetail).ifPresent(recordOfDecision ->
        form.setRecordWorkProgrammeAmendmentSummaryOptions(recordOfDecision.getWorkProgrammeSummaryOption()));
    return form;
  }

  @Transactional
  public void saveWorkProgrammeSummaryOption(
      ScheduleWorkProgrammeApplicationDetail applicationDetail,
      RecordWorkProgrammeAmendmentSummaryForm form
  ) {
    var recordOfDecision = getOrCreate(applicationDetail);
    recordOfDecision.setWorkProgrammeSummaryOption(form.getRecordWorkProgrammeAmendmentSummaryOptions());
    recordOfDecisionRepository.save(recordOfDecision);
  }

  public boolean isWorkProgrammeAmendmentDetailsComplete(ScheduleWorkProgrammeApplicationDetail applicationDetail) {
    return findByApplicationDetail(applicationDetail)
        .map(recordOfDecision -> recordOfDecision.getWorkProgrammeSummaryOption()
            == RecordWorkProgrammeAmendmentSummaryOptions.NO_ALL_ADDED)
        .orElse(false);
  }

  private RecordOfDecision getOrCreate(ScheduleWorkProgrammeApplicationDetail applicationDetail) {
    return findByApplicationDetail(applicationDetail)
        .orElseGet(() -> {
          var newRecordOfDecision = new RecordOfDecision();
          newRecordOfDecision.setScheduleWorkProgrammeApplicationDetail(applicationDetail);
          return newRecordOfDecision;
        });
  }

  public boolean isExtensionApproved(ScheduleWorkProgrammeApplicationDetail applicationDetail) {
    return findByApplicationDetail(applicationDetail)
        .map(recordOfDecision -> recordOfDecision.getExtensionDecision() == RecordOfDecisionResponse.GRANTED)
        .orElse(false);
  }

  public boolean isExtensionDetailsSaved(ScheduleWorkProgrammeApplicationDetail applicationDetail) {
    return recordOfDecisionExtensionRepository.existsByScheduleWorkProgrammeApplicationDetail(applicationDetail);
  }

  public boolean isWorkProgrammeAmendmentApproved(ScheduleWorkProgrammeApplicationDetail applicationDetail) {
    return findByApplicationDetail(applicationDetail)
        .map(recordOfDecision -> recordOfDecision.getWorkProgrammeDecision() == RecordOfDecisionResponse.GRANTED)
        .orElse(false);
  }
}
