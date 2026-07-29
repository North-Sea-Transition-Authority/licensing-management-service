package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision;

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
