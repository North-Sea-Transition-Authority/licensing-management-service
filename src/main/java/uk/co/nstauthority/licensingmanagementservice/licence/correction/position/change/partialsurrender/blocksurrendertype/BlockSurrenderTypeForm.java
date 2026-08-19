package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.blocksurrendertype;

import jakarta.annotation.Nullable;
import java.util.UUID;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.PartialSurrenderOperation;

public class BlockSurrenderTypeForm {
  private String surrenderType;

  public static BlockSurrenderTypeForm from(
      @Nullable PartialSurrenderOperation operation,
      UUID featureId
  ) {
    var form = new BlockSurrenderTypeForm();

    if (operation != null && operation.blockSurrenderTypeByFeatureId().get(featureId) != null) {
      form.setSurrenderType(operation.blockSurrenderTypeByFeatureId().get(featureId).name());
    }

    return form;
  }

  public String getSurrenderType() {
    return surrenderType;
  }

  public void setSurrenderType(String surrenderType) {
    this.surrenderType = surrenderType;
  }
}
