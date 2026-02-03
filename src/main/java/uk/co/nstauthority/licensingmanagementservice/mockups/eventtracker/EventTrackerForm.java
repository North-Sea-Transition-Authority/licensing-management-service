package uk.co.nstauthority.licensingmanagementservice.mockups.eventtracker;

import java.util.List;

public class EventTrackerForm {

  private List<String> licenceTypes;
  private List<String> requestTypes;
  private String year;
  private Integer licenseeOrgUnitId;
  private List<String> eventStatuses;

  public List<String> getLicenceTypes() {
    return licenceTypes;
  }

  public void setLicenceTypes(List<String> licenceTypes) {
    this.licenceTypes = licenceTypes;
  }

  public List<String> getRequestTypes() {
    return requestTypes;
  }

  public void setRequestTypes(List<String> requestTypes) {
    this.requestTypes = requestTypes;
  }

  public String getYear() {
    return year;
  }

  public void setYear(String year) {
    this.year = year;
  }

  public Integer getLicenseeOrgUnitId() {
    return licenseeOrgUnitId;
  }

  public void setLicenseeOrgUnitId(Integer licenseeOrgUnitId) {
    this.licenseeOrgUnitId = licenseeOrgUnitId;
  }

  public List<String> getEventStatuses() {
    return eventStatuses;
  }

  public void setEventStatuses(List<String> eventStatuses) {
    this.eventStatuses = eventStatuses;
  }
}
