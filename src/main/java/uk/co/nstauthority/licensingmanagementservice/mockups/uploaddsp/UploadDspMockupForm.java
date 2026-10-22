package uk.co.nstauthority.licensingmanagementservice.mockups.uploaddsp;

import java.util.ArrayList;
import java.util.List;
import uk.co.fivium.fileuploadlibrary.fds.UploadedFileForm;
import uk.co.fivium.formlibrary.input.ThreeFieldDateInput;

public class UploadDspMockupForm {

  private ThreeFieldDateInput decisionDate = new ThreeFieldDateInput("decisionDate", "decision date");
  private List<UploadedFileForm> finalDecisionSupportPapers = new ArrayList<>();
  private String dspAction;
  private String replaceTarget;
  private List<UploadedFileForm> correctedDsp = new ArrayList<>();
  private List<List<UploadedFileForm>> correctedAddendums = new ArrayList<>();
  private List<UploadedFileForm> dspAddendums = new ArrayList<>();

  public ThreeFieldDateInput getDecisionDate() {
    return decisionDate;
  }

  public void setDecisionDate(ThreeFieldDateInput decisionDate) {
    this.decisionDate = decisionDate;
  }

  public List<UploadedFileForm> getFinalDecisionSupportPapers() {
    return finalDecisionSupportPapers;
  }

  public void setFinalDecisionSupportPapers(List<UploadedFileForm> finalDecisionSupportPapers) {
    this.finalDecisionSupportPapers = finalDecisionSupportPapers;
  }

  public String getDspAction() {
    return dspAction;
  }

  public void setDspAction(String dspAction) {
    this.dspAction = dspAction;
  }

  public String getReplaceTarget() {
    return replaceTarget;
  }

  public void setReplaceTarget(String replaceTarget) {
    this.replaceTarget = replaceTarget;
  }

  public List<List<UploadedFileForm>> getCorrectedAddendums() {
    return correctedAddendums;
  }

  public void setCorrectedAddendums(List<List<UploadedFileForm>> correctedAddendums) {
    this.correctedAddendums = correctedAddendums;
  }

  public List<UploadedFileForm> getCorrectedDsp() {
    return correctedDsp;
  }

  public void setCorrectedDsp(List<UploadedFileForm> correctedDsp) {
    this.correctedDsp = correctedDsp;
  }

  public List<UploadedFileForm> getDspAddendums() {
    return dspAddendums;
  }

  public void setDspAddendums(List<UploadedFileForm> dspAddendums) {
    this.dspAddendums = dspAddendums;
  }
}
