package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision;

import jakarta.transaction.Transactional;
import java.util.Optional;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;

@Service
public class RecordOfDecisionService {

  private final RecordOfDecisionRepository recordOfDecisionRepository;

  public RecordOfDecisionService(RecordOfDecisionRepository recordOfDecisionRepository) {
    this.recordOfDecisionRepository = recordOfDecisionRepository;
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
    var recordOfDecision = findByApplicationDetail(applicationDetail)
        .orElseGet(() -> {
          var newRecordOfDecision = new RecordOfDecision();
          newRecordOfDecision.setScheduleWorkProgrammeApplicationDetail(applicationDetail);
          return newRecordOfDecision;
        });
    recordOfDecision.setExtensionDecision(form.getExtensionDecision());
    recordOfDecision.setWorkProgrammeDecision(form.getWorkProgrammeDecision());
    recordOfDecisionRepository.save(recordOfDecision);
  }

  public boolean isExtensionApproved(ScheduleWorkProgrammeApplicationDetail applicationDetail) {
    return findByApplicationDetail(applicationDetail)
        .map(recordOfDecision -> recordOfDecision.getExtensionDecision() == RecordOfDecisionResponse.GRANTED)
        .orElse(false);
  }

  public boolean isExtensionDetailsSaved(ScheduleWorkProgrammeApplicationDetail applicationDetail) {
    // TODO LMS1-543: always false until the extension details step saves data
    return false;
  }

  public boolean isWorkProgrammeAmendmentApproved(ScheduleWorkProgrammeApplicationDetail applicationDetail) {
    return findByApplicationDetail(applicationDetail)
        .map(recordOfDecision -> recordOfDecision.getWorkProgrammeDecision() == RecordOfDecisionResponse.GRANTED)
        .orElse(false);
  }
}
