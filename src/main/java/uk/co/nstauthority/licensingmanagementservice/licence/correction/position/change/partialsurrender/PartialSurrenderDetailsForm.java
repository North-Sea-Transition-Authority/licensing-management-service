package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public class PartialSurrenderDetailsForm {

  private Set<UUID> featureIds = new LinkedHashSet<>();

  public Set<UUID> getFeatureIds() {
    return featureIds;
  }

  public void setFeatureIds(Set<UUID> featureIds) {
    this.featureIds = featureIds;
  }
}
