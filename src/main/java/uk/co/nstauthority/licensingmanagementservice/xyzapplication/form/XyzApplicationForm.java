package uk.co.nstauthority.licensingmanagementservice.xyzapplication.form;

import java.util.ArrayList;
import java.util.List;
import uk.co.fivium.fileuploadlibrary.fds.UploadedFileForm;
import uk.co.fivium.formlibrary.input.IntegerInput;
import uk.co.fivium.formlibrary.input.StringInput;

public class XyzApplicationForm {

  private final StringInput applicationName;

  private final IntegerInput applicationNumber;

  private List<UploadedFileForm> documents = new ArrayList<>();

  private String selectedApplication;

  public XyzApplicationForm() {
    this.applicationNumber = new IntegerInput("applicationNumber", "application number");
    this.applicationName = new StringInput("applicationName", "application name");
  }

  public StringInput getApplicationName() {
    return applicationName;
  }

  public void setApplicationName(String applicationName) {
    this.applicationName.setInputValue(applicationName);
  }

  public IntegerInput getApplicationNumber() {
    return applicationNumber;
  }

  public void setApplicationNumber(Integer applicationNumber) {
    this.applicationNumber.setInteger(applicationNumber);
  }

  public List<UploadedFileForm> getDocuments() {
    return documents;
  }

  public void setDocuments(List<UploadedFileForm> documents) {
    this.documents = documents;
  }

  public String getSelectedApplication() {
    return selectedApplication;
  }

  public void setSelectedApplication(String selectedApplication) {
    this.selectedApplication = selectedApplication;
  }
}
