package uk.co.nstauthority.licensingmanagementservice.licence.operation;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.EquityOperationRule;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.PositionValidationContext;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.PositionValidationError;

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

  @Override
  public PositionValidationError validate(PositionValidationContext positionValidationContext) {
    if (!positionValidationContext.isCarbonStorage()) {
      return PositionValidationError.forPosition(
          positionValidationContext,
          EquityOperationRule.CARBON_STORAGE_LICENCE_ONLY
      );
    }

    if (equity.signum() > 0) {
      var availableEquity = positionValidationContext.previousState()
          .equityByOrganisationId()
          .getOrDefault(transferFrom, BigDecimal.ZERO)
          .max(BigDecimal.ZERO);
      if (equity.compareTo(availableEquity) > 0) {
        return PositionValidationError.forPosition(
            positionValidationContext,
            EquityOperationRule.INSUFFICIENT_EQUITY_TO_TRANSFER
        );
      }
    }

    return null;
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