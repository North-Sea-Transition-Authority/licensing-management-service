package uk.co.nstauthority.licensingmanagementservice.licence.operation;

import java.util.Objects;
import java.util.UUID;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.PositionValidationContext;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.PositionValidationError;

public record AdministratorOperation(
    UUID id,
    Integer operatorId
) implements LicenceOperation {

  public static final UUID ADMINISTRATOR_OPERATION_ID = new UUID(0L, 0L);

  public AdministratorOperation {
    Objects.requireNonNull(operatorId, "operatorId must not be null");
  }

  @Override
  public String type() {
    return LICENCE_ADMINISTRATOR;
  }

  @Override
  public PositionValidationError validate(PositionValidationContext positionValidationContext) {
    if (positionValidationContext.isCarbonStorage()) {
      return null;
    }

    var previousAdministratorId = positionValidationContext.previousState().administratorId();

    if (Objects.equals(previousAdministratorId, operatorId)) {
      return PositionValidationError.forOperation(
          positionValidationContext,
          type(),
          "The joining administrator cannot be the same as the withdrawing administrator"
      );
    }

    return null;
  }

  public static class Builder {

    private Integer operatorId = null;

    public Builder withOperator(Integer operatorId) {
      this.operatorId = operatorId;
      return this;
    }

    public AdministratorOperation build() {
      return new AdministratorOperation(ADMINISTRATOR_OPERATION_ID, operatorId);
    }
  }
}