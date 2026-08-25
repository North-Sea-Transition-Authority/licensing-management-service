package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.definearea;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.PartialSurrenderOperation.SurrenderDetails;

public class PartialSurrenderSelectAreasForm {

  private Set<UUID> surrenderedFeatureIds = new HashSet<>();

  public static PartialSurrenderSelectAreasForm from(SurrenderDetails surrenderDetails) {
    var form =  new PartialSurrenderSelectAreasForm();

    if (surrenderDetails != null) {
      form.setSurrenderedFeatureIds(new HashSet<>(surrenderDetails.surrenderedFeatureIds()));
    }

    return form;
  }

  public Set<UUID> getSurrenderedFeatureIds() {
    return surrenderedFeatureIds;
  }

  public void setSurrenderedFeatureIds(Set<UUID> surrenderedFeatureIds) {
    this.surrenderedFeatureIds = surrenderedFeatureIds;
  }
}
