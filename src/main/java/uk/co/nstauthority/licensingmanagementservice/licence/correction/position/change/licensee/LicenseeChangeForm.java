package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.licensee;

import java.util.List;

public class LicenseeChangeForm {

  private List<String> joiningOrganisationIds;

  private String joiningOrganisationSelector;

  private List<String> withdrawingOrganisationIds;

  private String withdrawingOrganisationSelector;

  public List<String> getJoiningOrganisationIds() {
    return joiningOrganisationIds;
  }

  public LicenseeChangeForm setJoiningOrganisationIds(List<String> joiningOrganisationIds) {
    this.joiningOrganisationIds = joiningOrganisationIds;
    return this;
  }

  public String getJoiningOrganisationSelector() {
    return joiningOrganisationSelector;
  }

  public LicenseeChangeForm setJoiningOrganisationSelector(String joiningOrganisationSelector) {
    this.joiningOrganisationSelector = joiningOrganisationSelector;
    return this;
  }

  public List<String> getWithdrawingOrganisationIds() {
    return withdrawingOrganisationIds;
  }

  public LicenseeChangeForm setWithdrawingOrganisationIds(List<String> withdrawingOrganisationIds) {
    this.withdrawingOrganisationIds = withdrawingOrganisationIds;
    return this;
  }

  public String getWithdrawingOrganisationSelector() {
    return withdrawingOrganisationSelector;
  }

  public LicenseeChangeForm setWithdrawingOrganisationSelector(String withdrawingOrganisationSelector) {
    this.withdrawingOrganisationSelector = withdrawingOrganisationSelector;
    return this;
  }
}
