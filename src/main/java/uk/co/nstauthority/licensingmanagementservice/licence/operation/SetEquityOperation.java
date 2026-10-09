package uk.co.nstauthority.licensingmanagementservice.licence.operation;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.setequity.LicencePositionSetEquityController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.EquityOperationRule;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.PositionValidationContext;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.PositionValidationError;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.ChangeUrlTarget;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.LicencePositionState;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

public record SetEquityOperation(
    Integer transferTo,
    BigDecimal equity
) implements VisibleLicenceOperation {

  public SetEquityOperation {
    Objects.requireNonNull(transferTo, "transferTo must not be null");
    Objects.requireNonNull(equity, "equity must not be null");
  }

  @Override
  public String type() {
    return SET_EQUITY;
  }

  @Override
  public String displayName() {
    return "Set equity";
  }

  @Override
  public UUID id() {
    return UUID.randomUUID();
  }

  @Override
  public Set<Integer> organisationUnitIds() {
    return Set.of(transferTo);
  }

  @Override
  public PositionValidationError validate(PositionValidationContext positionValidationContext) {
    if (!positionValidationContext.isCarbonStorage()) {
      return PositionValidationError.forPosition(
          positionValidationContext,
          EquityOperationRule.CARBON_STORAGE_LICENCE_ONLY
      );
    }

    //TODO LMS2-131: identify when a correction to a CS beneficial interest results in an invalid licence position
    return null;
  }

  @Override
  public LicencePositionState applyState(LicencePositionState licencePositionState) {
    var equityByOrganisationId = new HashMap<>(licencePositionState.equityByOrganisationId());
    equityByOrganisationId.put(transferTo, equity);
    return licencePositionState.withEquityByOrganisationId(equityByOrganisationId);
  }

  @Override
  public OperationRoutes getOperationUrls() {
    return new OperationRoutes() {
      @Override
      public String correct(ChangeUrlTarget target) {
        if (!OperationRoutes.isStagedEquityChange(target)) {
          return null;
        }
        return target.addedPosition()
            ? ReverseRouter.route(on(LicencePositionSetEquityController.class)
            .renderSummaryForAddedPosition(target.correction(), target.positionCorrection()))
            : ReverseRouter.route(on(LicencePositionSetEquityController.class)
            .renderSummaryForExecutedPosition(target.correction(), target.licencePosition()));
      }

      @Override
      public String remove(ChangeUrlTarget target) {
        return OperationRoutes.removeEquityChange(target);
      }

      @Override
      public String undo(ChangeUrlTarget target) {
        return OperationRoutes.undoEquityChange(target);
      }
    };
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