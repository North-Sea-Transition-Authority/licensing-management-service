package uk.co.nstauthority.licensingmanagementservice.licence.operation;

import java.util.Objects;

public record AdministratorOperation(
    Integer id,
    Integer operatorId
) implements LicenceOperation {

  public AdministratorOperation {
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

    public AdministratorOperation build() {
      return new AdministratorOperation(1, operatorId);
    }
  }
}
