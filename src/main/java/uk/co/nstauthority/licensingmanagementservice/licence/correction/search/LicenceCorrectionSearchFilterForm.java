package uk.co.nstauthority.licensingmanagementservice.licence.correction.search;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

public class LicenceCorrectionSearchFilterForm implements Serializable {

  @Serial
  private static final long serialVersionUID = 4127365981042736519L;

  private List<String> correctionStatuses;

  public List<String> getCorrectionStatuses() {
    return correctionStatuses;
  }

  public void setCorrectionStatuses(List<String> correctionStatuses) {
    this.correctionStatuses = correctionStatuses;
  }
}