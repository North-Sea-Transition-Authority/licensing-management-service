package uk.co.nstauthority.licensingmanagementservice.licence.operation;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.PositionValidationContext;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.PositionValidationError;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeViewContext;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.ChangeUrlTarget;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.LicencePositionState;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.PositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.ChangeViewUrls;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.LicencePositionChangeView;

public record ExplorationOperatorOperation(
    UUID id,
    Integer operatorId
) implements VisibleLicenceOperation {

  public static final UUID EXPLORATION_OPERATOR_OPERATION_ID = new UUID(0L, 0L);

  public ExplorationOperatorOperation {
    Objects.requireNonNull(operatorId, "operatorId must not be null");
  }

  @Override
  public String type() {
    return EXPLORATION_OPERATOR;
  }

  @Override
  public String displayName() {
    return "Exploration operator change";
  }

  @Override
  public Set<Integer> organisationUnitIds() {
    return Set.of(operatorId);
  }

  @Nullable
  @Override
  public PositionValidationError validate(PositionValidationContext positionValidationContext) {
    if (!positionValidationContext.isCarbonStorage()) {
      return null;
    }

    var previousOperatorId = positionValidationContext.previousState().explorationOperatorId();

    if (Objects.equals(previousOperatorId, operatorId)) {
      return PositionValidationError.forOperation(
          positionValidationContext,
          type(),
          "The joining exploration operator cannot be the same as the withdrawing operator"
      );
    }

    return null;
  }

  @Override
  public LicencePositionState applyState(LicencePositionState licencePositionState) {
    return licencePositionState.withExplorationOperatorId(operatorId);
  }

  @Override
  public LicencePositionChangeView getChangeView(
      PositionChange change,
      LicencePositionState previousState,
      @Nullable LocalDate currentPositionDate,
      LicencePositionChangeViewContext context,
      ChangeViewUrls urls
  ) {
    return null;
  }

  @Override
  public OperationRoutes getOperationUrls() {
    return new OperationRoutes() {
      @Override
      public String correct(ChangeUrlTarget target) {
        return null;
      }

      @Override
      public String remove(ChangeUrlTarget target) {
        return null;
      }

      @Override
      public String undo(ChangeUrlTarget target) {
        return null;
      }
    };
  }

  public static class Builder {

    private Integer operatorId = null;

    public ExplorationOperatorOperation.Builder withOperator(Integer operatorId) {
      this.operatorId = operatorId;
      return this;
    }

    public ExplorationOperatorOperation build() {
      return new ExplorationOperatorOperation(EXPLORATION_OPERATOR_OPERATION_ID, operatorId);
    }
  }
}