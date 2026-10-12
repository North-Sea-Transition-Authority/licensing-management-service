package uk.co.nstauthority.licensingmanagementservice.licence.correction.search;

import java.io.Serial;
import java.io.Serializable;

public class LicenceCorrectionSearchSession implements Serializable {

  @Serial
  private static final long serialVersionUID = -2846193057218364470L;

  private LicenceCorrectionSearchFilterForm searchFilterForm;

  public LicenceCorrectionSearchSession(LicenceCorrectionSearchFilterForm searchFilterForm) {
    this.searchFilterForm = searchFilterForm;
  }

  public void update(LicenceCorrectionSearchFilterForm searchFilterForm) {
    this.searchFilterForm = searchFilterForm;
  }

  public LicenceCorrectionSearchFilterForm getSearchFilterForm() {
    return searchFilterForm;
  }
}