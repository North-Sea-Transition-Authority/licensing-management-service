package uk.co.nstauthority.licensingmanagementservice.workarea;

import java.io.Serial;
import java.io.Serializable;

public class WorkAreaFilterForm implements Serializable {

  @Serial
  private static final long serialVersionUID = 5206043094614692850L;

  private String licenceReference;

  public String getLicenceReference() {
    return licenceReference;
  }

  public void setLicenceReference(String licenceReference) {
    this.licenceReference = licenceReference;
  }

  public void clearFilter() {
    setLicenceReference(null);
  }
}
