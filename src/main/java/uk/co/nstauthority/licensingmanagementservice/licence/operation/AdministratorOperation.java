package uk.co.nstauthority.licensingmanagementservice.licence.operation;

import static uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.LicencePositionChangeUtil.NOT_AVAILABLE;

import jakarta.annotation.Nullable;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.AddPositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.action.ChangeActionHandler;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.administrator.AdministratorChangeActionHandler;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.PositionValidationContext;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.PositionValidationError;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeViewContext;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.LicencePositionState;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.PositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.AdministratorChangeView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.ChangeViewUrls;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.LicencePositionChangeView;

public record AdministratorOperation(
    UUID id,
    Integer operatorId
) implements VisibleLicenceOperation {

  public static final UUID ADMINISTRATOR_OPERATION_ID = new UUID(0L, 0L);

  public AdministratorOperation {
    Objects.requireNonNull(operatorId, "operatorId must not be null");
  }

  @Override
  public String type() {
    return LICENCE_ADMINISTRATOR;
  }

  @Override
  public String displayName() {
    return "Licence administrator change";
  }

  @Override
  public Class<? extends ChangeActionHandler> changeActionHandlerType() {
    return AdministratorChangeActionHandler.class;
  }

  @Override
  public boolean atMostOnePerTransaction() {
    return true;
  }

  @Override
  public Set<Integer> organisationUnitIds() {
    return Set.of(operatorId);
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

  @Override
  public LicencePositionState applyState(LicencePositionState licencePositionState) {
    return licencePositionState.withAdministratorId(operatorId);
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
    var withdrawingId = previousState.administratorId();
    var withdrawingName = (withdrawingId == null) ? null : organisationNames.getOrDefault(withdrawingId, NOT_AVAILABLE);

    return new AdministratorChangeView(
        withdrawingName,
        organisationNames.getOrDefault(operatorId, NOT_AVAILABLE),
        change.changeId(),
        change.changeType(),
        urls
    );
  }

  @Override
  public OperationRoutes getOperationUrls() {
    return OperationRoutes.changeForm(AddPositionChangeType.ADMINISTRATOR);
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