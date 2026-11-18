package uk.co.nstauthority.licensingmanagementservice.licence.operation;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import static uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.LicencePositionChangeUtil.NOT_AVAILABLE;

import jakarta.annotation.Nullable;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.action.ChangeActionHandler;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.licensee.LicencePositionLicenseeChangeController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.licensee.LicenseeChangeActionHandler;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.PositionValidationContext;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.PositionValidationError;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeViewContext;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.LicencePositionState;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.PositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.ChangeViewUrls;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.LicencePositionChangeView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.LicenseeChangeView;
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
  public Class<? extends ChangeActionHandler> changeActionHandlerType() {
    return LicenseeChangeActionHandler.class;
  }

  @Override
  public boolean atMostOnePerPosition() {
    return true;
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
  public LicencePositionChangeView getChangeView(
      PositionChange change,
      LicencePositionState previousState,
      @Nullable LocalDate currentPositionDate,
      LicencePositionChangeViewContext context,
      ChangeViewUrls urls
  ) {
    var organisationNames = context.organisationNames();

    return new LicenseeChangeView(
        licenseeIdToNames(licenseesToRemove, organisationNames),
        licenseeIdToNames(licenseesToAdd, organisationNames),
        change.changeType(),
        urls
    );
  }

  @Override
  public OperationRoutes getOperationUrls() {
    return changeUrlTarget -> {
      if (changeUrlTarget.addedPosition()) {
        return ReverseRouter.route(on(LicencePositionLicenseeChangeController.class)
            .renderForAddedPosition(changeUrlTarget.correction(), changeUrlTarget.positionCorrection()));
      }
      return switch (changeUrlTarget.state()) {
        case ADDED -> ReverseRouter.route(on(LicencePositionLicenseeChangeController.class)
            .renderForExecutedPosition(
                changeUrlTarget.correction(),
                changeUrlTarget.licencePosition()
            ));
        case LIVE, OPERATIONS_UPDATED, ORDER_UPDATED -> ReverseRouter.route(on(LicencePositionLicenseeChangeController.class)
            .renderForCorrectingChange(
                changeUrlTarget.correction(),
                changeUrlTarget.licencePosition(),
                changeUrlTarget.changeEntity()
            ));
        case REMOVED -> null;
      };
    };
  }

  private static List<String> licenseeIdToNames(List<Integer> licenseeIds, Map<Integer, String> organisationNames) {
    return licenseeIds
        .stream()
        .map(licenseeId -> organisationNames.getOrDefault(licenseeId, NOT_AVAILABLE))
        .sorted()
        .toList();
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
