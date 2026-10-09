package uk.co.nstauthority.licensingmanagementservice.licence.operation;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import static uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.LicencePositionChangeUtil.NOT_AVAILABLE;

import jakarta.annotation.Nullable;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.administrator.LicencePositionAdministratorChangeController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.administrator.RemoveAdministratorChangeController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.PositionValidationContext;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.PositionValidationError;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeViewContext;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.ChangeUrlTarget;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.LicencePositionState;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.PositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.AdministratorChangeView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.ChangeViewUrls;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.LicencePositionChangeView;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

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
    return new OperationRoutes() {
      @Override
      public String correct(ChangeUrlTarget target) {
        if (target.addedPosition()) {
          return ReverseRouter.route(on(LicencePositionAdministratorChangeController.class)
              .renderForAddedPosition(target.correction(), target.positionCorrection()));
        }
        return switch (target.state()) {
          case ADDED -> ReverseRouter.route(on(LicencePositionAdministratorChangeController.class)
              .renderForExecutedPosition(target.correction(), target.licencePosition()));
          case LIVE, OPERATIONS_UPDATED, ORDER_UPDATED ->
              ReverseRouter.route(on(LicencePositionAdministratorChangeController.class)
                  .renderForCorrectingChange(target.correction(), target.licencePosition(), target.changeEntity()));
          case REMOVED -> null;
        };
      }

      @Override
      public String remove(ChangeUrlTarget target) {
        return ReverseRouter.route(on(RemoveAdministratorChangeController.class)
            .renderRemoveExecutedAdminChange(target.correction(), target.licencePosition(), target.changeEntity()));
      }

      @Override
      public String undo(ChangeUrlTarget target) {
        return ReverseRouter.route(on(RemoveAdministratorChangeController.class)
            .renderUndoAdminChange(target.correction(), target.changeId()));
      }
    };
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