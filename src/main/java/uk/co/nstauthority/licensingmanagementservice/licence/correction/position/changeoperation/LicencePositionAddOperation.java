package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changeoperation;

import java.util.Objects;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;

public record LicencePositionAddOperation(
    Integer operationId,
    LicenceOperation operation
) implements LicencePositionChangeOperation {

  public LicencePositionAddOperation {
    Objects.requireNonNull(operationId, "operationId must not be null");
    Objects.requireNonNull(operation, "operation must not be null");
  }

  @Override
  public String type() {
    return ADD_OPERATION;
  }

  public static class Builder {

    private Integer operationId = null;
    private LicenceOperation operation = null;

    public LicencePositionAddOperation.Builder withOperationId(Integer operationId) {
      this.operationId = operationId;
      return this;
    }

    public LicencePositionAddOperation.Builder withOperation(LicenceOperation operation) {
      this.operation = operation;
      return this;
    }

    public LicencePositionAddOperation build() {
      return new LicencePositionAddOperation(operationId, operation);
    }
  }
}
