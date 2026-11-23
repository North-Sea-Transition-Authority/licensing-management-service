package uk.co.nstauthority.licensingmanagementservice.licence.operation;

import static uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.LicencePositionChangeUtil.NOT_AVAILABLE;

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
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.ExplorationOperatorChangeView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.LicencePositionChangeView;

/** Operation related to exploration operators on a carbon storage licence.
 *  @param operatorId id of the exploration operator, is null when a storage operator joins the licence
 *                    for the first time and the exploration operator is removed
 */
public record ExplorationOperatorOperation(
    UUID id,
    @Nullable Integer operatorId
) implements VisibleLicenceOperation {

  public static final UUID EXPLORATION_OPERATOR_OPERATION_ID = new UUID(0L, 0L);

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
    if (operatorId == null) {
      return Set.of();
    }
    return Set.of(operatorId);
  }

  @Nullable
  @Override
  public PositionValidationError validate(PositionValidationContext positionValidationContext) {
    if (!positionValidationContext.isCarbonStorage()) {
      return null;
    }

    var previousOperatorId = positionValidationContext.previousState().explorationOperatorId();

    if (previousOperatorId != null && Objects.equals(previousOperatorId, operatorId)) {
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
    var organisationNames = context.organisationNames();
    var withdrawingId = previousState.explorationOperatorId();
    var withdrawingName = (withdrawingId == null) ? null : organisationNames.getOrDefault(withdrawingId, NOT_AVAILABLE);

    return new ExplorationOperatorChangeView(
        withdrawingName,
        organisationNames.getOrDefault(operatorId, NOT_AVAILABLE),
        change.changeId(),
        change.changeType(),
        urls
    );
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