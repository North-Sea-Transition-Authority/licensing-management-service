package uk.co.nstauthority.licensingmanagementservice.licence.contact;

import java.io.Serial;
import java.io.Serializable;

public class LicenceContactFilterForm implements Serializable {

  @Serial
  private static final long serialVersionUID = 4258817599038731245L;

  private String licenceReference;
  private Integer licenseeOrgGroupId;
  private Integer licenseeOrgUnitId;
  private String contactEmail;
  private Boolean noContactAssigned;

  public String getLicenceReference() {
    return licenceReference;
  }

  public void setLicenceReference(String licenceReference) {
    this.licenceReference = licenceReference;
  }

  public Integer getLicenseeOrgGroupId() {
    return licenseeOrgGroupId;
  }

  public void setLicenseeOrgGroupId(Integer licenseeOrgGroupId) {
    this.licenseeOrgGroupId = licenseeOrgGroupId;
  }

  public Integer getLicenseeOrgUnitId() {
    return licenseeOrgUnitId;
  }

  public void setLicenseeOrgUnitId(Integer licenseeOrgUnitId) {
    this.licenseeOrgUnitId = licenseeOrgUnitId;
  }

  public String getContactEmail() {
    return contactEmail;
  }

  public void setContactEmail(String contactEmail) {
    this.contactEmail = contactEmail;
  }

  public Boolean getNoContactAssigned() {
    return noContactAssigned;
  }

  public void setNoContactAssigned(Boolean noContactAssigned) {
    this.noContactAssigned = noContactAssigned;
  }
}
