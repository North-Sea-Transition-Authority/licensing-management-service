package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changeoperation;

import java.util.Objects;
import java.util.UUID;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;

public record LicencePositionAddOperation(
    UUID operationId,
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

    private UUID operationId = null;
    private LicenceOperation operation = null;

    public LicencePositionAddOperation.Builder withOperationId(UUID operationId) {
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