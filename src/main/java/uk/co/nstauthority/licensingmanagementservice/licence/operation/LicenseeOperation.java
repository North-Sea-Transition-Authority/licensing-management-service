package uk.co.nstauthority.licensingmanagementservice.licence.operation;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.licensee.LicencePositionLicenseeChangeController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.PositionValidationContext;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.PositionValidationError;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.ChangeUrlTarget;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.LicencePositionState;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

public record LicenseeOperation(
    UUID id,
    List<Integer> licenseesToAdd,
    List<Integer> licenseesToRemove
) implements VisibleLicenceOperation {

  public static final UUID LICENSEE_OPERATION_ID = new UUID(0L, 0L);

  @Override
  public String type() {
    return LICENSEE;
  }

  @Override
  public String displayName() {
    return "Licensee change";
  }

  @Override
  public Set<Integer> organisationUnitIds() {
    return Stream.concat(licenseesToAdd.stream(), licenseesToRemove.stream()).collect(Collectors.toSet());
  }

  @Override
  public PositionValidationError validate(PositionValidationContext positionValidationContext) {
    return null;
  }

  @Override
  public LicencePositionState applyState(LicencePositionState licencePositionState) {
    return licencePositionState.withLicenseeIds(licenseesToAdd, licenseesToRemove);
  }

  @Override
  public OperationRoutes getOperationUrls() {
    return new OperationRoutes() {
      @Override
      public String correct(ChangeUrlTarget target) {
        if (target.addedPosition()) {
          return ReverseRouter.route(on(LicencePositionLicenseeChangeController.class)
              .renderForAddedPosition(target.correction(), target.positionCorrection()));
        }
        return switch (target.state()) {
          case ADDED -> ReverseRouter.route(on(LicencePositionLicenseeChangeController.class)
              .renderForExecutedPosition(target.correction(), target.licencePosition()));
          case LIVE, OPERATIONS_UPDATED, ORDER_UPDATED ->
              ReverseRouter.route(on(LicencePositionLicenseeChangeController.class)
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

    private List<Integer> licenseesToAdd = List.of();
    private List<Integer> licenseesToRemove = List.of();

    public LicenseeOperation.Builder withLicenseesToAdd(List<Integer> licenseesToAdd) {
      this.licenseesToAdd = licenseesToAdd;
      return this;
    }

    public LicenseeOperation.Builder withLicenseesToRemove(List<Integer> licenseesToRemove) {
      this.licenseesToRemove = licenseesToRemove;
      return this;
    }

    public LicenseeOperation build() {
      return new LicenseeOperation(LICENSEE_OPERATION_ID, licenseesToAdd, licenseesToRemove);
    }
  }
}
