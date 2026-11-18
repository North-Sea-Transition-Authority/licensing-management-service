package uk.co.nstauthority.licensingmanagementservice.licence.operation;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import static uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.LicencePositionChangeUtil.NOT_AVAILABLE;

import jakarta.annotation.Nullable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.action.ChangeActionHandler;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.equity.EquityChangeActionHandler;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.setequity.LicencePositionSetEquityController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.EquityOperationRule;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.PositionValidationContext;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.PositionValidationError;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeViewContext;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.LicencePositionState;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.PositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.ChangeViewUrls;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.LicencePositionChangeView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.SetEquityChangeView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.SetEquityRow;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

public record SetEquityOperation(
    Integer transferTo,
    BigDecimal equity
) implements VisibleLicenceOperation, EquitySetterOperation {

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
  public Class<? extends ChangeActionHandler> changeActionHandlerType() {
    return EquityChangeActionHandler.class;
  }

  @Override
  public String changeName() {
    return "beneficial interest change";
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
  public LicencePositionChangeView getChangeView(
      PositionChange change,
      LicencePositionState previousState,
      @Nullable LocalDate currentPositionDate,
      LicencePositionChangeViewContext context,
      ChangeViewUrls urls
  ) {
    var organisationNames = context.organisationNames();

    return new SetEquityChangeView(
        List.of(new SetEquityRow(organisationNames.getOrDefault(transferTo, NOT_AVAILABLE), equity)),
        change.changeType(),
        urls
    );
  }

  @Override
  public OperationRoutes getOperationUrls() {
    return changeUrlTarget -> {
      if (!OperationRoutes.isStagedEquityChange(changeUrlTarget)) {
        return null;
      }

      return ReverseRouter.route(on(LicencePositionSetEquityController.class)
          .renderSummaryForPosition(changeUrlTarget.correctionPosition()));
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