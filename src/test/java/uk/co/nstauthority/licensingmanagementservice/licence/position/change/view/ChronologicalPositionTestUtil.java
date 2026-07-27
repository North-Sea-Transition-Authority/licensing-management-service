package uk.co.nstauthority.licensingmanagementservice.licence.position.change.view;

import java.util.List;
import java.util.UUID;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;

public class ChronologicalPositionTestUtil {

  private ChronologicalPositionTestUtil() {
  }

  public static ChronologicalPosition live(LicencePosition position, LicenceOperation... operations) {
    var changes = (operations.length == 0)
        ? List.<PositionChange>of()
        : List.of(new PositionChange(UUID.randomUUID().toString(), 1, null, List.of(operations)));
    return ChronologicalPosition.fromLicencePosition(position, changes);
  }
}
