package uk.co.nstauthority.licensingmanagementservice.licence.search;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

public class LicenceSearchFilterForm implements Serializable {

  @Serial
  private static final long serialVersionUID = 155413159338411457L;

  private String reference;
  private List<String> licenceTypes;
  private Integer licenseeOrgUnitId;

  public String getReference() {
    return reference;
  }

  public void setReference(String reference) {
    this.reference = reference;
  }

  public List<String> getLicenceTypes() {
    return licenceTypes;
  }

  public void setLicenceTypes(List<String> licenceTypes) {
    this.licenceTypes = licenceTypes;
  }

  public Integer getLicenseeOrgUnitId() {
    return licenseeOrgUnitId;
  }

  public void setLicenseeOrgUnitId(Integer licenseeOrgUnitId) {
    this.licenseeOrgUnitId = licenseeOrgUnitId;
  }
}
