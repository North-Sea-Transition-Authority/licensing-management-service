package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changeoperation;

import java.util.Objects;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;

public record LicencePositionUpdateOperation(
    Integer operationId,
    LicenceOperation operation
) implements LicencePositionChangeOperation {

  public LicencePositionUpdateOperation {
    Objects.requireNonNull(operationId, "operationId must not be null");
    Objects.requireNonNull(operation, "operation must not be null");
  }

  @Override
  public String type() {
    return UPDATE_OPERATION;
  }

  public static class Builder {

    private Integer operationId = null;
    private LicenceOperation operation = null;

    public LicencePositionUpdateOperation.Builder withOperationId(Integer operationId) {
      this.operationId = operationId;
      return this;
    }

    public LicencePositionUpdateOperation.Builder withOperation(LicenceOperation operation) {
      this.operation = operation;
      return this;
    }

    public LicencePositionUpdateOperation build() {
      return new LicencePositionUpdateOperation(operationId, operation);
    }
  }

}
