package uk.co.nstauthority.licensingmanagementservice.licence.continuation.supportinginformation;

import java.util.ArrayList;
import java.util.List;
import uk.co.fivium.fileuploadlibrary.fds.UploadedFileForm;

public class LicenceContinuationSupportingInformationForm {

  private Boolean hasAdditionalSupportingInformation;
  private List<UploadedFileForm> documents = new ArrayList<>();

  public Boolean getHasAdditionalSupportingInformation() {
    return hasAdditionalSupportingInformation;
  }

  public void setHasAdditionalSupportingInformation(Boolean hasAdditionalSupportingInformation) {
    this.hasAdditionalSupportingInformation = hasAdditionalSupportingInformation;
  }

  public List<UploadedFileForm> getDocuments() {
    return documents;
  }

  public void setDocuments(List<UploadedFileForm> documents) {
    this.documents = documents;
  }
}
