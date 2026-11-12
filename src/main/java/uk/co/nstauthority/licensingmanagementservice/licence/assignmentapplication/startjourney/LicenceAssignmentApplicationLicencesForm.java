package uk.co.nstauthority.licensingmanagementservice.licence.assignmentapplication.startjourney;

import java.util.ArrayList;
import java.util.List;

public class LicenceAssignmentApplicationLicencesForm {

  private List<Integer> licenceIds = new ArrayList<>();

  private String licenceSelector;

  public List<Integer> getLicenceIds() {
    return licenceIds;
  }

  public void setLicenceIds(List<Integer> licenceIds) {
    this.licenceIds = licenceIds;
  }

  public String getLicenceSelector() {
    return licenceSelector;
  }

  public void setLicenceSelector(String licenceSelector) {
    this.licenceSelector = licenceSelector;
  }
}
