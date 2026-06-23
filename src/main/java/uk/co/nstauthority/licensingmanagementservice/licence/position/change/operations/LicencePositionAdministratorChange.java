package uk.co.nstauthority.licensingmanagementservice.licence.position.change.operations;

import java.util.Objects;

public record LicencePositionAdministratorChange(
    Integer operatorId
) implements LicencePositionChangeOperation {

  public LicencePositionAdministratorChange {
    Objects.requireNonNull(operatorId, "operatorId must not be null");
  }

  @Override
  public String type() {
    return LICENCE_ADMINISTRATOR;
  }

  public static class Builder {

    private Integer operatorId = null;

    public Builder withOperator(Integer operatorId) {
      this.operatorId = operatorId;
      return this;
    }

    public LicencePositionAdministratorChange build() {
      return new LicencePositionAdministratorChange(operatorId);
    }
  }
}
