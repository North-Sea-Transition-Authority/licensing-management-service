package uk.co.nstauthority.licensingmanagementservice.licence;


import java.util.List;

public class NewLicenceForm {

  private LicenceType licenceType;

  private String licenceNumber;

  private LicenceStatus licenceStatus;

  private List<String> organisationUnitIds;

  private String organisationUnitSelector;

  public LicenceType getLicenceType() {
    return licenceType;
  }

  public void setLicenceType(LicenceType licenceType) {
    this.licenceType = licenceType;
  }

  public LicenceStatus getLicenceStatus() {
    return licenceStatus;
  }

  public void setLicenceStatus(LicenceStatus licenceStatus) {
    this.licenceStatus = licenceStatus;
  }

  public String getLicenceNumber() {
    return licenceNumber;
  }

  public void setLicenceNumber(String licenceNumber) {
    this.licenceNumber = licenceNumber;
  }

  public List<String> getOrganisationUnitIds() {
    return organisationUnitIds;
  }

  public void setOrganisationUnitIds(List<String> organisationUnitIds) {
    this.organisationUnitIds = organisationUnitIds;
  }

  public String getOrganisationUnitSelector() {
    return organisationUnitSelector;
  }

  public void setOrganisationUnitSelector(String organisationUnitSelector) {
    this.organisationUnitSelector = organisationUnitSelector;
  }
}
