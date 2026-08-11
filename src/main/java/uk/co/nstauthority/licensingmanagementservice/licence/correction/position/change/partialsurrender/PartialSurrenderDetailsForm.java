package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender;

import jakarta.annotation.Nullable;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.PartialSurrenderOperation;

public class PartialSurrenderDetailsForm {

  private Set<UUID> featureIds = new LinkedHashSet<>();

  public static PartialSurrenderDetailsForm from(@Nullable PartialSurrenderOperation committedPartialSurrender) {
    var form = new PartialSurrenderDetailsForm();

    if (committedPartialSurrender != null) {
      form.setFeatureIds(new LinkedHashSet<>(committedPartialSurrender.featureIds()));
    }

    return form;
  }

  public Set<UUID> getFeatureIds() {
    return featureIds;
  }

  public void setFeatureIds(Set<UUID> featureIds) {
    this.featureIds = featureIds;
  }
}
