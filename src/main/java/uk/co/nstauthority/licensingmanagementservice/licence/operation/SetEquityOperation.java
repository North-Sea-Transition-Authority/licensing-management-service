package uk.co.nstauthority.licensingmanagementservice.licence.operation;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.PositionValidationContext;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.PositionValidationError;

public record SetEquityOperation(
    Integer transferTo,
    BigDecimal equity
) implements LicenceOperation {

  public SetEquityOperation {
    Objects.requireNonNull(transferTo, "transferTo must not be null");
    Objects.requireNonNull(equity, "equity must not be null");
  }

  @Override
  public String type() {
    return SET_EQUITY;
  }

  @Override
  public UUID id() {
    return UUID.randomUUID();
  }

  @Override
  public PositionValidationError validate(PositionValidationContext positionValidationContext) {
    //TODO LMS2-131: identify when a correction to a CS beneficial interest results in an invalid licence position
    return null;
  }

  public static class Builder {

    private Integer transferTo;
    private BigDecimal equity;

    public Builder withTransferTo(Integer transferTo) {
      this.transferTo = transferTo;
      return this;
    }

    public Builder withEquity(BigDecimal equity) {
      this.equity = equity;
      return this;
    }

    public SetEquityOperation build() {
      return new SetEquityOperation(transferTo, equity);
    }
  }
}