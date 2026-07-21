package uk.co.nstauthority.licensingmanagementservice.workarea;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

public class WorkAreaFilterForm implements Serializable {

  @Serial
  private static final long serialVersionUID = 5206043094614692850L;

  private String licenceReference;
  private List<String> licenceTypes;
  private String applicationReference;
  private List<String> applicationTypes;
  private List<String> applicationStatuses;

  public String getLicenceReference() {
    return licenceReference;
  }

  public void setLicenceReference(String licenceReference) {
    this.licenceReference = licenceReference;
  }

  public List<String> getLicenceTypes() {
    return licenceTypes;
  }

  public void setLicenceTypes(List<String> licenceTypes) {
    this.licenceTypes = licenceTypes;
  }

  public String getApplicationReference() {
    return applicationReference;
  }

  public void setApplicationReference(String applicationReference) {
    this.applicationReference = applicationReference;
  }

  public List<String> getApplicationTypes() {
    return applicationTypes;
  }

  public void setApplicationTypes(List<String> applicationTypes) {
    this.applicationTypes = applicationTypes;
  }

  public List<String> getApplicationStatuses() {
    return applicationStatuses;
  }

  public void setApplicationStatuses(List<String> applicationStatuses) {
    this.applicationStatuses = applicationStatuses;
  }

  public void clearFilter() {
    setLicenceReference(null);
    setLicenceTypes(null);
    setApplicationReference(null);
    setApplicationTypes(null);
    setApplicationStatuses(null);
  }
}
