package uk.co.nstauthority.licensingmanagementservice.licence.contact;

import java.util.ArrayList;
import java.util.List;

public class LicenceContactForm {

  private String contactEmail;

  private List<Integer> bulkUpdateLicenceIds = new ArrayList<>();

  public String getContactEmail() {
    return contactEmail;
  }

  public void setContactEmail(String contactEmail) {
    this.contactEmail = contactEmail;
  }

  public List<Integer> getBulkUpdateLicenceIds() {
    return bulkUpdateLicenceIds;
  }

  public void setBulkUpdateLicenceIds(List<Integer> bulkUpdateLicenceIds) {
    this.bulkUpdateLicenceIds = bulkUpdateLicenceIds;
  }
}
