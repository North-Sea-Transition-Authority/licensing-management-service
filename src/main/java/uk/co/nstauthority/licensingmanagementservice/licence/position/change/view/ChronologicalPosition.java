package uk.co.nstauthority.licensingmanagementservice.licence.position.change.view;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.CreateLicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;

public record ChronologicalPosition(
    UUID id,
    LocalDate date,
    int order,
    List<PositionChange> changes
) {

  public static ChronologicalPosition fromLicencePosition(
      LicencePosition position,
      List<PositionChange> changes
  ) {
    return new ChronologicalPosition(
        position.getId(),
        position.getPositionDate(),
        position.getPositionDateOrder(),
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

}
