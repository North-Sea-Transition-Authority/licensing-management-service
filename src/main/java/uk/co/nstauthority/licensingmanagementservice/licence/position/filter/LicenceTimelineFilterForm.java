package uk.co.nstauthority.licensingmanagementservice.licence.position.filter;

import java.util.List;

public class LicenceTimelineFilterForm {

  private List<String> changeTypes = List.of();

  private List<Integer> organisationIds = List.of();

  public static LicenceTimelineFilterForm from(LicenceTimelineFilter filter) {
    var form = new LicenceTimelineFilterForm();
    form.setChangeTypes(List.copyOf(filter.changeTypes()));
    form.setOrganisationIds(List.copyOf(filter.organisationIds()));
    return form;
  }

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
