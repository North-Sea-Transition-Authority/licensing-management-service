package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.tasklist.requestpurpose;

import java.util.HashSet;
import java.util.Set;

public class SwpApplicationRequestPurposeForm {

  private Set<SwpApplicationRequestPurposeOption> requestPurposes = new HashSet<>();

  public Set<SwpApplicationRequestPurposeOption> getRequestPurposes() {
    return requestPurposes;
  }

  public void setRequestPurposes(Set<SwpApplicationRequestPurposeOption> requestPurposes) {
    this.requestPurposes = requestPurposes;
  }
}
