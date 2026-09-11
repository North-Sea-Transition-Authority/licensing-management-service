package uk.co.nstauthority.licensingmanagementservice.licence.position.change.view;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;

public class PositionChangeTestUtil {

  private String changeId = UUID.randomUUID().toString();
  private int changeOrder = 1;
  private String changeType = null;
  private List<LicenceOperation> operations =
      List.of(LicenceOperation.newAdministratorChange().withOperator(1).build());

  public static PositionChangeTestUtil newBuilder() {
    return new PositionChangeTestUtil();
  }

  public PositionChangeTestUtil withChangeId(String changeId) {
    this.changeId = changeId;
    return this;
  }

  public PositionChangeTestUtil withChangeOrder(int changeOrder) {
    this.changeOrder = changeOrder;
    return this;
  }

  public PositionChangeTestUtil withChangeType(String changeType) {
    this.changeType = changeType;
    return this;
  }

  public PositionChangeTestUtil withOperations(List<LicenceOperation> operations) {
    this.operations = operations;
    return this;
  }

  public PositionChangeTestUtil withAdministratorOperation(int operatorId) {
    this.operations = List.of(LicenceOperation.newAdministratorChange().withOperator(operatorId).build());
    return this;
  }

  public PositionChangeTestUtil withSetEquityOperation(int transferTo, BigDecimal equity) {
    this.operations = List.of(LicenceOperation.newSetEquityOperation()
        .withTransferTo(transferTo)
        .withEquity(equity)
        .build());
    return this;
  }

  public PositionChange build() {
    return new PositionChange(changeId, changeOrder, changeType, operations);
  }
}
