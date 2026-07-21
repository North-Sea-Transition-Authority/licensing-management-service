package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes;

import java.util.List;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changeoperation.LicencePositionChangeOperation;


public record AddLicencePositionChange(
    String changeId,
    Integer changeOrder,
    List<LicencePositionChangeOperation> operations
) implements LicencePositionChangeType {

  @Override
  public String type() {
    return ADD_CHANGE;
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

    public AddLicencePositionChange build() {
      return new AddLicencePositionChange(
          changeId,
          changeOrder,
          operations
      );
    }
  }
}