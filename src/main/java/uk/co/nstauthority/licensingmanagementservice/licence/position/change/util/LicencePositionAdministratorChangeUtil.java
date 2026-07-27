package uk.co.nstauthority.licensingmanagementservice.licence.position.change.util;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.AdministratorOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.PositionChange;

public final class LicencePositionAdministratorChangeUtil {

  private LicencePositionAdministratorChangeUtil() {
    throw new IllegalStateException("Utility class should not be instantiated.");
  }

  public static Map<UUID, Integer> administratorIdChangeByPositionId(List<ChronologicalPosition> chronologicalPositions) {
    var administratorIdByPositionId = new HashMap<UUID, Integer>();
    chronologicalPositions.forEach(chronologicalPosition ->
        chronologicalPosition.changes().stream()
            .sorted(Comparator.comparingLong(PositionChange::changeOrder))
            .flatMap(change -> change.operations().stream())
            .filter(AdministratorOperation.class::isInstance)
            .map(operation -> ((AdministratorOperation) operation).operatorId())
            .forEach(operatorId -> administratorIdByPositionId.put(chronologicalPosition.id(), operatorId)));

    return administratorIdByPositionId;
  }

  /**
   * Resolves the administrator as at the given position, i.e. including any administrator change made on the
   * position itself.
   *
   * @param currentLicencePositionId the position to resolve the administrator at
   * @param chronologicalPositions positions in chronological order
   * @return the resolved administrator id, or {@code null} if none applies
   */
  public static Integer resolveCurrentAdministratorId(
      UUID currentLicencePositionId,
      List<ChronologicalPosition> chronologicalPositions
  ) {
    return resolveAdministratorId(currentLicencePositionId, chronologicalPositions, true);
  }

  /**
   * Resolves the administrator carried in from before the given position, i.e. excluding any administrator change
   * made on the position itself. Useful to compare against a change being made on the position.
   *
   * @param currentLicencePositionId the position to resolve the administrator at
   * @param chronologicalPositions positions in chronological order
   * @return the resolved administrator id, or {@code null} if none applies
   */
  public static Integer resolvePreviousAdministratorId(
      UUID currentLicencePositionId,
      List<ChronologicalPosition> chronologicalPositions
  ) {
    return resolveAdministratorId(currentLicencePositionId, chronologicalPositions, false);
  }

  /**
   * Resolves the administrator in effect at a given position by walking the chronological positions in order and
   * keeping the most recent administrator change up to (and optionally including) the target position.
   *
   * <p>The {@code inclusive} flag controls whether an administrator change made <em>on</em> the target position
   * itself is counted:
   * <ul>
   *   <li>{@code inclusive = true} — considers changes on all positions up to and including
   *   {@code currentLicencePositionId}, giving the administrator as at that position (the "current" administrator).</li>
   *   <li>{@code inclusive = false} — considers changes on all earlier positions but excludes any change on
   *   {@code currentLicencePositionId}, giving the administrator carried in from before that position (the
   *   "previous" administrator).</li>
   * </ul>
   *
   * <p>Iteration stops at {@code currentLicencePositionId}; positions after it are never considered. Returns
   * {@code null} if no administrator change applies within the considered range.
   */
  private static Integer resolveAdministratorId(
      UUID currentLicencePositionId,
      List<ChronologicalPosition> chronologicalPositions,
      boolean inclusive
  ) {
    var administratorIdChangeByPositionId = administratorIdChangeByPositionId(chronologicalPositions);

    Integer administratorId = null;
    for (var chronologicalPosition : chronologicalPositions) {
      var isCurrentPosition = chronologicalPosition.id().equals(currentLicencePositionId);
      if (inclusive || !isCurrentPosition) {
        var operatorId = administratorIdChangeByPositionId.get(chronologicalPosition.id());
        if (operatorId != null) {
          administratorId = operatorId;
        }
      }
      if (isCurrentPosition) {
        break;
      }
    }

    return administratorId;
  }
}
