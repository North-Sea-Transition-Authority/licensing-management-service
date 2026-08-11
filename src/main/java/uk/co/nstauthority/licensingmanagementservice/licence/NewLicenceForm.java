package uk.co.nstauthority.licensingmanagementservice.licence;


import java.util.List;
import uk.co.fivium.formlibrary.input.ThreeFieldDateInput;

public class NewLicenceForm {

  private LicenceType licenceType;

  private String licenceNumber;

  private LicenceStatusType licenceStatus;

  private ThreeFieldDateInput licenceStatusDate = new ThreeFieldDateInput("licenceStatusDate", "licence status date");

  private List<String> organisationUnitIds;

  private String organisationUnitSelector;

  public LicenceType getLicenceType() {
    return licenceType;
  }

  public void setLicenceType(LicenceType licenceType) {
    this.licenceType = licenceType;
  }

  public LicenceStatusType getLicenceStatus() {
    return licenceStatus;
  }

  public void setLicenceStatus(LicenceStatusType licenceStatus) {
    this.licenceStatus = licenceStatus;
  }

  public ThreeFieldDateInput getLicenceStatusDate() {
    return licenceStatusDate;
  }

  public void setLicenceStatusDate(ThreeFieldDateInput licenceStatusDate) {
    this.licenceStatusDate = licenceStatusDate;
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
