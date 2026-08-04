package uk.co.nstauthority.licensingmanagementservice.licence;

import java.util.List;

public class EditLicenceDetailsForm {

  private List<String> organisationUnitIds;

  private String organisationUnitSelector;

  public List<String> getOrganisationUnitIds() {
    return organisationUnitIds;
  }

  public void setOrganisationUnitIds(List<String> organisationUnitIds) {
    this.organisationUnitIds = organisationUnitIds;
  }

  public String getOrganisationUnitSelector() {
    return organisationUnitSelector;
  }

  public void setOrganisationUnitSelector(String organisationUnitSelector) {
    this.organisationUnitSelector = organisationUnitSelector;
  }
}
