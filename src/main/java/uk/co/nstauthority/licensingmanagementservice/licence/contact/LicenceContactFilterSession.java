package uk.co.nstauthority.licensingmanagementservice.licence.contact;

import java.io.Serial;
import java.io.Serializable;

public class LicenceContactFilterSession implements Serializable {

  @Serial
  private static final long serialVersionUID = -8323412226857541639L;

  private LicenceContactFilterForm filterForm;

  public LicenceContactFilterSession(LicenceContactFilterForm filterForm) {
    this.filterForm = filterForm;
  }

  public void update(LicenceContactFilterForm filterForm) {
    this.filterForm = filterForm;
  }

  public LicenceContactFilterForm getFilterForm() {
    return filterForm;
  }
}
