package uk.co.nstauthority.licensingmanagementservice.licence.operation;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.EquityOperationRule;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.PositionValidationContext;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.PositionValidationError;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.LicencePositionState;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TransferEquityOperation(
    Integer transferFrom,
    Integer transferTo,
    BigDecimal equity,
    Boolean retainBeneficialInterest
) implements VisibleLicenceOperation {

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
  public String displayName() {
    return "Transfer equity";
  }

  @Override
  public UUID id() {
    return UUID.randomUUID();
  }

  @Override
  public Set<Integer> organisationUnitIds() {
    return Stream.of(transferFrom, transferTo).collect(Collectors.toSet());
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

  @Override
  public LicencePositionState applyState(LicencePositionState licencePositionState) {
    var equityByOrganisationId = new HashMap<>(licencePositionState.equityByOrganisationId());

    var isAddingEquity = equity.signum() > 0;
    if (!isAddingEquity) {
      return licencePositionState.withEquityByOrganisationId(equityByOrganisationId);
    }
    var availableEquity = equityByOrganisationId.getOrDefault(transferFrom, BigDecimal.ZERO).max(BigDecimal.ZERO);
    var transferEquity = equity.min(availableEquity);
    var remainingEquity = availableEquity.subtract(transferEquity);

    var retainsInterest = Boolean.TRUE.equals(retainBeneficialInterest);

    if (remainingEquity.signum() <= 0 && !retainsInterest) {
      equityByOrganisationId.remove(transferFrom);
    } else {
      equityByOrganisationId.put(transferFrom, remainingEquity);
    }

    if (transferEquity.signum() > 0) {
      equityByOrganisationId.merge(transferTo, transferEquity, BigDecimal::add);
    }

    return licencePositionState.withEquityByOrganisationId(equityByOrganisationId);
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