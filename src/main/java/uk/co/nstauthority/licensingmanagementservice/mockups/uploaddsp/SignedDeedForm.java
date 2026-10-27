package uk.co.nstauthority.licensingmanagementservice.mockups.uploaddsp;

import java.util.ArrayList;
import java.util.List;
import uk.co.fivium.fileuploadlibrary.fds.UploadedFileForm;

public class SignedDeedForm {

  private List<UploadedFileForm> signedCounterparts = new ArrayList<>();

  public List<UploadedFileForm> getSignedCounterparts() {
    return signedCounterparts;
  }

  public void setSignedCounterparts(List<UploadedFileForm> signedCounterparts) {
    this.signedCounterparts = signedCounterparts;
  }
}
