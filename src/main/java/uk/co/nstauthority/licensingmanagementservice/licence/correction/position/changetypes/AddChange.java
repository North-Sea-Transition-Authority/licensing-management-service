package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes;

import java.util.List;
import java.util.UUID;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changeoperation.LicencePositionChangeOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;


public record AddChange(
    String changeId,
    Integer changeOrder,
    List<LicencePositionChangeOperation> operations
) implements LicencePositionChangeType {

  @Override
  public String type() {
    return ADD_CHANGE;
  }

  public static AddChange buildAddAdminChange(Integer administratorId, int changeOrder) {
    var administratorOperation = LicenceOperation.newAdministratorChange()
        .withOperator(administratorId)
        .build();

    var changeOperation = LicencePositionChangeOperation.newLicencePositionAddOperation()
        .withOperationId(administratorOperation.id())
        .withOperation(administratorOperation)
        .build();

    return LicencePositionChangeType.addChange()
        .withChangeId(UUID.randomUUID().toString())
        .withChangeOrder(changeOrder)
        .withOperations(List.of(changeOperation))
        .build();
  }

  public static class Builder {

    private String changeId;
    private Integer changeOrder;
    private List<LicencePositionChangeOperation> operations = List.of();


    public Builder withChangeId(String changeId) {
      this.changeId = changeId;
      return this;
    }

    public Builder withChangeOrder(Integer changeOrder) {
      this.changeOrder = changeOrder;
      return this;
    }

    public Builder withOperations(List<LicencePositionChangeOperation> operations) {
      this.operations = operations;
      return this;
    }

    public AddChange build() {
      return new AddChange(
          changeId,
          changeOrder,
          operations
      );
    }
  }
}