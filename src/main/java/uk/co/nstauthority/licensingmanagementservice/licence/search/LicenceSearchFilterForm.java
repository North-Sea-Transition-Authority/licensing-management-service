package uk.co.nstauthority.licensingmanagementservice.licence.search;

public class LicenceSearchFilterForm {

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
