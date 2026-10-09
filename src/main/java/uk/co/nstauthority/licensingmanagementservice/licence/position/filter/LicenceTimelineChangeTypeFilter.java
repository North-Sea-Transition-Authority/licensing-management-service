package uk.co.nstauthority.licensingmanagementservice.licence.position.filter;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.VisibleLicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPosition;

/**
 * The types of change the licence timeline can be filtered by, which are the {@link LicenceOperation#type()}s of the
 * operations a position's changes are made of.
 *
 * <p>Only {@link VisibleLicenceOperation}s, the types shown as change cards, can be filtered on. The block and subarea
 * operations the PEARS migration produces have no card, so they are never offered, even when they share a change with
 * an operation that does.
 */
public final class LicenceTimelineChangeTypeFilter {

  static final List<String> CHANGE_TYPE_ORDER = List.of(
      LicenceOperation.LICENCE_ADMINISTRATOR,
      LicenceOperation.LICENSEE,
      LicenceOperation.SET_EQUITY,
      LicenceOperation.TRANSFER_EQUITY,
      LicenceOperation.PARTIAL_SURRENDER,
      LicenceOperation.SUBAREA
  );

  private static final Comparator<LicenceOperation> CHANGE_TYPE_COMPARATOR =
      Comparator.comparingInt(LicenceTimelineChangeTypeFilter::changeTypeOrder)
          .thenComparing(LicenceOperation::displayName);

  private LicenceTimelineChangeTypeFilter() {
    throw new IllegalStateException("Utility class should not be instantiated.");
  }

  /**
   * The filterable change types made anywhere on the timeline, keyed by type with the display name as the value, in the
   * order of {@link #CHANGE_TYPE_ORDER}. A type missing from that order comes after the ones in it, by display name.
   */
  public static Map<String, String> getAvailableChangeTypeOptions(List<ChronologicalPosition> chronologicalPositions) {
    return chronologicalPositions.stream()
        .flatMap(position -> position.changes().stream())
        .flatMap(change -> change.visibleLicenceOperations().stream())
        .sorted(CHANGE_TYPE_COMPARATOR)
        .collect(Collectors.toMap(
            LicenceOperation::type,
            LicenceOperation::displayName,
            (first, second) -> first,
            LinkedHashMap::new
        ));
  }

  private static int changeTypeOrder(LicenceOperation operation) {
    var order = CHANGE_TYPE_ORDER.indexOf(operation.type());
    return order == -1 ? CHANGE_TYPE_ORDER.size() : order;
  }
}
