package uk.co.nstauthority.licensingmanagementservice.mockups.licencecorrection.licensee;

import java.util.List;

public class LicenseeCorrectionForm {

  private List<String> joiningOrganisationUnitIds;

  private String joiningOrganisationUnitSelector;

  private List<String> withdrawingOrganisationUnitIds;

  private String withdrawingOrganisationUnitSelector;

  public List<String> getJoiningOrganisationUnitIds() {
    return joiningOrganisationUnitIds;
  }

  public void setJoiningOrganisationUnitIds(List<String> joiningOrganisationUnitIds) {
    this.joiningOrganisationUnitIds = joiningOrganisationUnitIds;
  }

  public String getJoiningOrganisationUnitSelector() {
    return joiningOrganisationUnitSelector;
  }

  public void setJoiningOrganisationUnitSelector(String joiningOrganisationUnitSelector) {
    this.joiningOrganisationUnitSelector = joiningOrganisationUnitSelector;
  }

  public List<String> getWithdrawingOrganisationUnitIds() {
    return withdrawingOrganisationUnitIds;
  }

  public void setWithdrawingOrganisationUnitIds(List<String> withdrawingOrganisationUnitIds) {
    this.withdrawingOrganisationUnitIds = withdrawingOrganisationUnitIds;
  }

  public String getWithdrawingOrganisationUnitSelector() {
    return withdrawingOrganisationUnitSelector;
  }

  public void setWithdrawingOrganisationUnitSelector(String withdrawingOrganisationUnitSelector) {
    this.withdrawingOrganisationUnitSelector = withdrawingOrganisationUnitSelector;
  }
}
