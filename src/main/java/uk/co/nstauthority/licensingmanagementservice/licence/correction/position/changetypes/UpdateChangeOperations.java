package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes;

import java.util.List;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changeoperation.LicencePositionChangeOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;

public record UpdateChangeOperations(
    String changeId,
    List<LicencePositionChangeOperation> operations
) implements LicencePositionChangeType {

  @Override
  public String type() {
    return UPDATE_CHANGE_OPERATIONS;
  }

  public static UpdateChangeOperations buildUpdateAdminChange(String originalChangeId, Integer administratorId) {
    var administratorOperation = LicenceOperation.newAdministratorChange().withOperator(administratorId).build();
    var updateOperation = LicencePositionChangeOperation.newLicencePositionUpdateOperation()
        .withOperationId(administratorOperation.id())
        .withOperation(administratorOperation)
        .build();

    return LicencePositionChangeType.updateChangeOperations()
        .withChangeId(originalChangeId)
        .withOperations(List.of(updateOperation))
        .build();
  }

  public static class Builder {
    private String changeId;
    private List<LicencePositionChangeOperation> operations = List.of();

    public Builder withChangeId(String changeId) {
      this.changeId = changeId;
      return this;
    }

    public Builder withOperations(List<LicencePositionChangeOperation> operations) {
      this.operations = operations;
      return this;
    }

    public UpdateChangeOperations build() {
      return new UpdateChangeOperations(changeId, operations);
    }
  }
}
