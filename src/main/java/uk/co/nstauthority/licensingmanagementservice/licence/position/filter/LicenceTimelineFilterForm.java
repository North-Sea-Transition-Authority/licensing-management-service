package uk.co.nstauthority.licensingmanagementservice.licence.position.filter;

import java.util.List;

public class LicenceTimelineFilterForm {

  private List<String> changeTypes = List.of();

  public List<String> getChangeTypes() {
    return changeTypes;
  }

  public void setChangeTypes(List<String> changeTypes) {
    this.changeTypes = changeTypes;
  }
}
