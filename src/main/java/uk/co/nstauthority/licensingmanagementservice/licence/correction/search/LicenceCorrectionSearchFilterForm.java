package uk.co.nstauthority.licensingmanagementservice.licence.correction.search;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

public class LicenceCorrectionSearchFilterForm implements Serializable {

  @Serial
  private static final long serialVersionUID = 4127365981042736519L;

  private List<String> correctionStatuses;

  private String licenceReference;

  private String allocatedToWuaId;

  public List<String> getCorrectionStatuses() {
    return correctionStatuses;
  }

  public void setCorrectionStatuses(List<String> correctionStatuses) {
    this.correctionStatuses = correctionStatuses;
  }

  public String getLicenceReference() {
    return licenceReference;
  }

  public void setLicenceReference(String licenceReference) {
    this.licenceReference = licenceReference;
  }

  public String getAllocatedToWuaId() {
    return allocatedToWuaId;
  }

  public void setAllocatedToWuaId(String allocatedToWuaId) {
    this.allocatedToWuaId = allocatedToWuaId;
  }
}