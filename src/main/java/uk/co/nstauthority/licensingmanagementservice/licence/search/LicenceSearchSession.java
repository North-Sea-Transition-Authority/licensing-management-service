package uk.co.nstauthority.licensingmanagementservice.licence.search;

import java.io.Serial;
import java.io.Serializable;

public class LicenceSearchSession implements Serializable {

  @Serial
  private static final long serialVersionUID = -5195852515145968016L;

  private LicenceSearchFilterForm searchFilterForm;
  private boolean searchInvoked;

  public LicenceSearchSession(LicenceSearchFilterForm searchFilterForm) {
    this.searchFilterForm = searchFilterForm;
  }

  public boolean hasSearchBeenInvoked() {
    return searchInvoked;
  }

  public void update(LicenceSearchFilterForm searchFilterForm) {
    this.searchFilterForm = searchFilterForm;
    searchInvoked = true;
  }

  public LicenceSearchFilterForm getSearchFilterForm() {
    return searchFilterForm;
  }
}
