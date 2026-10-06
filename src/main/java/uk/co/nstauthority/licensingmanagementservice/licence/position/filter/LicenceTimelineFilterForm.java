package uk.co.nstauthority.licensingmanagementservice.licence.position.filter;

import java.util.List;

public class LicenceTimelineFilterForm {

  private List<String> changeTypes = List.of();

  private List<Integer> organisationIds = List.of();

  public List<String> getChangeTypes() {
    return changeTypes;
  }

  public void setChangeTypes(List<String> changeTypes) {
    this.changeTypes = changeTypes;
  }

  public List<Integer> getOrganisationIds() {
    return organisationIds;
  }

  public void setOrganisationIds(List<Integer> organisationIds) {
    this.organisationIds = organisationIds;
  }
}
