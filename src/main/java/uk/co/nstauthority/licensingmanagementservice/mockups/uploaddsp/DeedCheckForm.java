package uk.co.nstauthority.licensingmanagementservice.mockups.uploaddsp;

import java.util.ArrayList;
import java.util.List;
import uk.co.fivium.fileuploadlibrary.fds.UploadedFileForm;

public class DeedCheckForm {

  private String outcome;
  private String reason;
  private List<UploadedFileForm> postedHardcopy = new ArrayList<>();

  public String getOutcome() {
    return outcome;
  }

  public void setOutcome(String outcome) {
    this.outcome = outcome;
  }

  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }

  public List<UploadedFileForm> getPostedHardcopy() {
    return postedHardcopy;
  }

  public void setPostedHardcopy(List<UploadedFileForm> postedHardcopy) {
    this.postedHardcopy = postedHardcopy;
  }
}
