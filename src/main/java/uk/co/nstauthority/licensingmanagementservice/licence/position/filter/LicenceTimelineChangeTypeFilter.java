package uk.co.nstauthority.licensingmanagementservice.licence.position.filter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPosition;

/**
 * The types of change the licence timeline can be filtered by, which are the {@link LicenceOperation#type()}s of the
 * operations a position's changes are made of.
 *
 * <p>Only the types shown as change cards can be filtered on. The block and subarea operations the PEARS migration
 * produces have no card, so they are never offered.
 */
public final class LicenceTimelineChangeTypeFilter {

  static final List<String> FILTERABLE_CHANGE_TYPES = List.of(
      LicenceOperation.LICENCE_ADMINISTRATOR,
      LicenceOperation.LICENSEE,
      LicenceOperation.SET_EQUITY,
      LicenceOperation.TRANSFER_EQUITY,
      LicenceOperation.PARTIAL_SURRENDER,
      LicenceOperation.SUBAREA
  );

  private LicenceTimelineChangeTypeFilter() {
    throw new IllegalStateException("Utility class should not be instantiated.");
  }

  /**
   * The filterable change types made anywhere on the timeline, keyed by type with the display name as the value, in a
   * fixed order.
   */
  public static Map<String, String> getAvailableChangeTypeOptions(List<ChronologicalPosition> chronologicalPositions) {
    var displayNamesByType = chronologicalPositions.stream()
        .flatMap(position -> position.changes().stream())
        .flatMap(change -> change.operations().stream())
        .filter(operation -> FILTERABLE_CHANGE_TYPES.contains(operation.type()))
        .collect(Collectors.toMap(
            LicenceOperation::type,
            LicenceOperation::displayName,
            (first, second) -> first
        ));

    return FILTERABLE_CHANGE_TYPES.stream()
        .filter(displayNamesByType::containsKey)
        .collect(Collectors.toMap(
            Function.identity(),
            displayNamesByType::get,
            (first, second) -> first,
            LinkedHashMap::new
        ));
  }
}
