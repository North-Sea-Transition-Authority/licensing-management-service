package uk.co.nstauthority.licensingmanagementservice.licence;


import java.util.List;
import uk.co.nstauthority.licensingmanagementservice.licence.overview.responsibleteam.LicenceTeam;

public class NewLicenceForm {

  private LicenceType licenceType;

  private LicenceTeam responsibleTeam;

  private String licenceNumber;

  private List<String> organisationUnitIds;

  private String organisationUnitSelector;

  public LicenceType getLicenceType() {
    return licenceType;
  }

  public void setLicenceType(LicenceType licenceType) {
    this.licenceType = licenceType;
  }

  public LicenceTeam getResponsibleTeam() {
    return responsibleTeam;
  }

  public void setResponsibleTeam(
      LicenceTeam responsibleTeam) {
    this.responsibleTeam = responsibleTeam;
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
