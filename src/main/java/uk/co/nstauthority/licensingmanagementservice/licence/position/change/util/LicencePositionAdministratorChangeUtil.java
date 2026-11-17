package uk.co.nstauthority.licensingmanagementservice.licence.position.change.util;

import java.util.List;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.AdministratorOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;

public final class LicencePositionAdministratorChangeUtil {

  private LicencePositionAdministratorChangeUtil() {
    throw new IllegalStateException("Utility class should not be instantiated.");
  }

  public static boolean adminChangeExists(List<LicencePositionChangeType> changes) {
    return LicencePositionChangeOperationUtil.changeExists(changes, AdministratorOperation.class);
  }

  public static boolean adminIdNotChanged(LicencePositionChange change, Integer administratorId) {
    return change.getOperations().stream()
        .filter(AdministratorOperation.class::isInstance)
        .map(operation -> ((AdministratorOperation) operation).operatorId())
        .anyMatch(operatorId -> operatorId.equals(administratorId));
  }
}