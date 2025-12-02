package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.startjourney;

public class LicenseeInformationForm {

  private Integer responsibleOrganisationUnitId;

  private Boolean allLicenseesPermissionConfirmed;

  public Integer getResponsibleOrganisationUnitId() {
    return responsibleOrganisationUnitId;
  }

  public void setResponsibleOrganisationUnitId(Integer responsibleOrganisationUnitId) {
    this.responsibleOrganisationUnitId = responsibleOrganisationUnitId;
  }

  public Boolean getAllLicenseesPermissionConfirmed() {
    return allLicenseesPermissionConfirmed;
  }

  public void setAllLicenseesPermissionConfirmed(Boolean allLicenseesPermissionConfirmed) {
    this.allLicenseesPermissionConfirmed = allLicenseesPermissionConfirmed;
  }
}
