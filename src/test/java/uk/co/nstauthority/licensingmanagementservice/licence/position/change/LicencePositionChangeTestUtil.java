package uk.co.nstauthority.licensingmanagementservice.licence.position.change;

import java.util.List;
import java.util.UUID;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;

public class LicencePositionChangeTestUtil {
  private UUID id = UUID.randomUUID();
  private LicencePosition licencePosition = LicencePositionTestUtil.newBuilder().build();
  private List<LicenceOperation> operations =
      List.of(LicenceOperation.newAdministratorChange().withOperator(1).build());
  private long changeOrder = 1L;
  private LicencePositionChangeStatus status = LicencePositionChangeStatus.CONSENTED;


  public static LicencePositionChangeTestUtil newBuilder() {
    return new LicencePositionChangeTestUtil();
  }

  public LicencePositionChangeTestUtil withId(UUID id) {
    this.id = id;
    return this;
  }

  public LicencePositionChangeTestUtil withLicencePosition(LicencePosition licencePosition) {
    this.licencePosition = licencePosition;
    return this;
  }

  public LicencePositionChangeTestUtil withOperations(List<LicenceOperation> operations) {
    this.operations = operations;
    return this;
  }

  public LicencePositionChangeTestUtil withChangeOrder(long changeOrder) {
    this.changeOrder = changeOrder;
    return this;
  }

  public LicencePositionChangeTestUtil withStatus(LicencePositionChangeStatus status) {
    this.status = status;
    return this;
  }

  public LicencePositionChange build() {
    var licencePositionChange = new LicencePositionChange(id);
    licencePositionChange.setLicencePosition(licencePosition);
    licencePositionChange.setOperations(operations);
    licencePositionChange.setChangeOrder(changeOrder);
    licencePositionChange.setStatus(status);

    return licencePositionChange;
  }
}
