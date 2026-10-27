package uk.co.nstauthority.licensingmanagementservice.mockups.uploaddsp;

import java.util.ArrayList;
import java.util.List;
import uk.co.fivium.fileuploadlibrary.fds.UploadedFileForm;
import uk.co.fivium.formlibrary.input.ThreeFieldDateInput;

public class DeedOfVariationForm {

  private List<UploadedFileForm> deedOfVariation = new ArrayList<>();
  private ThreeFieldDateInput signingDeadline = new ThreeFieldDateInput("signingDeadline", "signing deadline");

  public List<UploadedFileForm> getDeedOfVariation() {
    return deedOfVariation;
  }

  public void setDeedOfVariation(List<UploadedFileForm> deedOfVariation) {
    this.deedOfVariation = deedOfVariation;
  }

  public ThreeFieldDateInput getSigningDeadline() {
    return signingDeadline;
  }

  public void setSigningDeadline(ThreeFieldDateInput signingDeadline) {
    this.signingDeadline = signingDeadline;
  }
}
