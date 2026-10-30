package uk.co.nstauthority.licensingmanagementservice.licence.operation;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import static uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.LicencePositionChangeUtil.NOT_AVAILABLE;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.storageoperator.LicencePositionStorageOperatorChangeController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.PositionValidationContext;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.PositionValidationError;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeViewContext;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.ChangeUrlTarget;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.LicencePositionState;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.PositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.ChangeViewUrls;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.LicencePositionChangeView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.StorageOperatorChangeView;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

public record StorageOperatorOperation(
    UUID id,
    Integer operatorId
) implements VisibleLicenceOperation {

  public static final UUID STORAGE_OPERATOR_OPERATION_ID = new UUID(0L, 0L);

  public StorageOperatorOperation {
    Objects.requireNonNull(operatorId, "operatorId must not be null");
  }

  @Override
  public String type() {
    return STORAGE_OPERATOR;
  }

  @Override
  public String displayName() {
    return "Storage operator change";
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

    var previousOperatorId = positionValidationContext.previousState().storageOperatorId();

    if (Objects.equals(previousOperatorId, operatorId)) {
      return PositionValidationError.forOperation(
          positionValidationContext,
          type(),
          "The joining storage operator cannot be the same as the withdrawing operator"
      );
    }

    return null;
  }

  @Override
  public LicencePositionState applyState(LicencePositionState licencePositionState) {
    return licencePositionState.withStorageOperatorId(operatorId);
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
    var withdrawingId = previousState.storageOperatorId();
    var withdrawingName = (withdrawingId == null) ? null : organisationNames.getOrDefault(withdrawingId, NOT_AVAILABLE);

    return new StorageOperatorChangeView(
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
        if (target.addedPosition()) {
          return ReverseRouter.route(on(LicencePositionStorageOperatorChangeController.class)
              .renderForAddedPosition(target.correction(), target.positionCorrection()));
        }
        return switch (target.state()) {
          case ADDED -> ReverseRouter.route(on(LicencePositionStorageOperatorChangeController.class)
              .renderForExecutedPosition(target.correction(), target.licencePosition()));
          case LIVE, OPERATIONS_UPDATED, ORDER_UPDATED ->
              ReverseRouter.route(on(LicencePositionStorageOperatorChangeController.class)
                  .renderForCorrectingChange(target.correction(), target.licencePosition(), target.changeEntity()));
          case REMOVED -> null;
        };
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

    public StorageOperatorOperation.Builder withOperator(Integer operatorId) {
      this.operatorId = operatorId;
      return this;
    }

    public StorageOperatorOperation build() {
      return new StorageOperatorOperation(STORAGE_OPERATOR_OPERATION_ID, operatorId);
    }
  }
}
