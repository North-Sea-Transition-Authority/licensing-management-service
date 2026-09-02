package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.subarea;

import jakarta.annotation.Nullable;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaOperation;

public class SubareaChangeStartForm {

  private String featureId;

  public static SubareaChangeStartForm from(@Nullable SubareaOperation committedSubareaOperation) {
    var form = new SubareaChangeStartForm();

    if (committedSubareaOperation != null) {
      form.setFeatureId(committedSubareaOperation.featureId().toString());
    }

    return form;
  }

  public String getFeatureId() {
    return featureId;
  }

  public void setFeatureId(String featureId) {
    this.featureId = featureId;
  }
}
