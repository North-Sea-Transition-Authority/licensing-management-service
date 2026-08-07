package uk.co.nstauthority.licensingmanagementservice.licence.operation;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TransferEquityOperation(
    Integer transferFrom,
    Integer transferTo,
    BigDecimal equity,
    Boolean retainBeneficialInterest
) implements LicenceOperation {

  public TransferEquityOperation {
    Objects.requireNonNull(transferFrom, "transferFrom must not be null");
    Objects.requireNonNull(transferTo, "transferTo must not be null");
    Objects.requireNonNull(equity, "equity must not be null");
  }

  @Override
  public String type() {
    return TRANSFER_EQUITY;
  }

  @Override
  public UUID id() {
    return UUID.randomUUID();
  }

  public static class Builder {

    private Integer transferFrom;
    private Integer transferTo;
    private BigDecimal equity;
    private Boolean retainBeneficialInterest;

    public Builder withTransferFrom(Integer transferFrom) {
      this.transferFrom = transferFrom;
      return this;
    }

    public Builder withTransferTo(Integer transferTo) {
      this.transferTo = transferTo;
      return this;
    }

    public Builder withEquity(BigDecimal equity) {
      this.equity = equity;
      return this;
    }

    public Builder withRetainBeneficialInterest(Boolean retainBeneficialInterest) {
      this.retainBeneficialInterest = retainBeneficialInterest;
      return this;
    }

    public TransferEquityOperation build() {
      return new TransferEquityOperation(transferFrom, transferTo, equity, retainBeneficialInterest);
    }
  }
}