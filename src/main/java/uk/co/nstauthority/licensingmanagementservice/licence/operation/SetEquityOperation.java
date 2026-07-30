package uk.co.nstauthority.licensingmanagementservice.licence.operation;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

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