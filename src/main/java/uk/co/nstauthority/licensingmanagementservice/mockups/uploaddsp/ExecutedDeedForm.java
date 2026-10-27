package uk.co.nstauthority.licensingmanagementservice.mockups.uploaddsp;

import java.util.ArrayList;
import java.util.List;
import uk.co.fivium.fileuploadlibrary.fds.UploadedFileForm;

public class ExecutedDeedForm {

  private List<UploadedFileForm> executedDeed = new ArrayList<>();

  public List<UploadedFileForm> getExecutedDeed() {
    return executedDeed;
  }

  public void setExecutedDeed(List<UploadedFileForm> executedDeed) {
    this.executedDeed = executedDeed;
  }
}
