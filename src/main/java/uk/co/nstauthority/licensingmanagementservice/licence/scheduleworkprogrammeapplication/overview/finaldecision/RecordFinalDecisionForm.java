package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overview.finaldecision;

import java.util.ArrayList;
import java.util.List;
import uk.co.fivium.fileuploadlibrary.fds.UploadedFileForm;
import uk.co.fivium.formlibrary.input.ThreeFieldDateInput;

public class RecordFinalDecisionForm {

  private ThreeFieldDateInput decisionDate = new ThreeFieldDateInput("decisionDate", "decision date");
  private List<UploadedFileForm> finalDecisionSupportPapers = new ArrayList<>();

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
}
