package uk.co.nstauthority.licensingmanagementservice.workarea;

import java.io.Serial;
import java.io.Serializable;

public class WorkAreaFilterForm implements Serializable {

  @Serial
  private static final long serialVersionUID = 5206043094614692850L;

  private String reference;

  public String getReference() {
    return reference;
  }

  public void setReference(String reference) {
    this.reference = reference;
  }

  public void clearFilter() {
    setReference(null);
  }
}
