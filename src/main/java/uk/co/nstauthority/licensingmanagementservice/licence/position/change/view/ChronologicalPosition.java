package uk.co.nstauthority.licensingmanagementservice.licence.position.change.view;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.CreateLicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.PositionValidationContext;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.PositionValidationError;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.util.DateUtil;

public record ChronologicalPosition(
    UUID id,
    LocalDate date,
    int order,
    List<PositionChange> changes
) {

  public static ChronologicalPosition fromLicencePosition(
      LicencePosition position,
      LocalDate date,
      int order,
      List<PositionChange> changes
  ) {
    return new ChronologicalPosition(
        position.getId(),
        date,
        order,
        changes
    );
  }

  public static ChronologicalPosition fromPayload(CreateLicencePositionPayload payload) {
    return new ChronologicalPosition(
        UUID.fromString(payload.licencePositionId()),
        payload.effectiveDate(),
        payload.effectiveDateOrder(),
        PositionChange.fromPayload(payload)
    );
  }

  public String positionName() {
    return DateUtil.formatLongDateWithOrder(date, order);
  }

  public List<PositionValidationError> validate(PositionValidationContext positionValidationContext) {
    var positionValidationErrors = new ArrayList<PositionValidationError>();

    positionValidationErrors.addAll(validateAdministratorChange(positionValidationContext));

    changes.forEach(change -> positionValidationErrors.addAll(change.validate(positionValidationContext)));

    return positionValidationErrors;
  }

  private List<PositionValidationError> validateAdministratorChange(PositionValidationContext positionValidationContext) {
    if (positionValidationContext.isCarbonStorage()) {
      return List.of();
    }

    var administratorChangeCount = changes.stream()
        .filter(change -> !LicencePositionChangeType.REMOVE_CHANGE.equals(change.changeType()))
        .flatMap(change -> change.operations().stream())
        .filter(licenceOperation -> LicenceOperation.LICENCE_ADMINISTRATOR.equals(licenceOperation.type()))
        .count();

    if (positionValidationContext.isFirstPosition() && administratorChangeCount == 0) {
      return List.of(PositionValidationError.forPosition(
          positionValidationContext,
          "The first licence position must have an administrator change"
      ));
    }

    if (administratorChangeCount > 1) {
      return List.of(PositionValidationError.forPosition(
          positionValidationContext,
          "A licence position can only have one administrator change"
      ));
    }

    return List.of();
  }

}
