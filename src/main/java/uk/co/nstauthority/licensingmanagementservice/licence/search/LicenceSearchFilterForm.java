package uk.co.nstauthority.licensingmanagementservice.licence.search;

import java.io.Serial;
import java.io.Serializable;

public class LicenceSearchFilterForm implements Serializable {

  @Serial
  private static final long serialVersionUID = 155413159338411457L;

  private String reference;

  public String getReference() {
    return reference;
  }

  public void setReference(String reference) {
    this.reference = reference;
  }
}
