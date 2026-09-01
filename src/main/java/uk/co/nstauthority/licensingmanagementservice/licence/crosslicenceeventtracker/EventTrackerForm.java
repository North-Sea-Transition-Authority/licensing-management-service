package uk.co.nstauthority.licensingmanagementservice.licence.crosslicenceeventtracker;

import java.util.List;

public class EventTrackerForm {

  private List<String> licenceTypes;
  private List<String> requestTypes;
  private String fromDate;
  private String toDate;
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

  public String getFromDate() {
    return fromDate;
  }

  public void setFromDate(String fromDate) {
    this.fromDate = fromDate;
  }

  public String getToDate() {
    return toDate;
  }

  public void setToDate(String toDate) {
    this.toDate = toDate;
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
