package uk.co.nstauthority.licensingmanagementservice.licence.contact;

public record LicenceContactFormView(
    String licenceReference,
    String currentEmail
) {

  public boolean isUpdate() {
    return currentEmail != null;
  }
}
