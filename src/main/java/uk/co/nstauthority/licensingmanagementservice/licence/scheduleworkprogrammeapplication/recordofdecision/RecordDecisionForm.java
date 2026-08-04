package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision;

public class RecordDecisionForm {

  private RecordOfDecisionResponse extensionDecision;
  private RecordOfDecisionResponse workProgrammeDecision;

  public RecordOfDecisionResponse getExtensionDecision() {
    return extensionDecision;
  }

  public void setExtensionDecision(RecordOfDecisionResponse extensionDecision) {
    this.extensionDecision = extensionDecision;
  }

  public RecordOfDecisionResponse getWorkProgrammeDecision() {
    return workProgrammeDecision;
  }

  public void setWorkProgrammeDecision(RecordOfDecisionResponse workProgrammeDecision) {
    this.workProgrammeDecision = workProgrammeDecision;
  }
}
