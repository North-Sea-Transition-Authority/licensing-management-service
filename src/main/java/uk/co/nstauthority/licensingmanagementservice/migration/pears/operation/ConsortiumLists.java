package uk.co.nstauthority.licensingmanagementservice.migration.pears.operation;

import java.util.List;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.history.PearsOperation;

/**
 * What the migrators of company lists share. PEARS holds the administrator and the licensees alike,
 * as consortium list operations told apart by their COMPANY_LIST_TYPE.
 */
final class ConsortiumLists {

  private ConsortiumLists() {
    throw new IllegalStateException("Utility class should not be instantiated.");
  }

  /**
   * Whether the operation is a consortium list of the given COMPANY_LIST_TYPE.
   */
  static boolean isList(PearsOperation operation, String companyListType) {
    return companyListType.equals(companyListTypeOf(operation));
  }

  static List<PearsOperation.ConsortiumEntry> entriesOf(PearsOperation operation) {
    return switch (operation) {
      case PearsOperation.ConsortiumListCreate create -> create.entries();
      case PearsOperation.ConsortiumListChange change -> change.entries();
      default -> List.of();
    };
  }

  /**
   * The organisation as the Energy Portal organisation unit id this application holds.
   */
  static int organisationUnitId(PearsOperation.Organisation organisation) {
    return Math.toIntExact(organisation.id());
  }

  private static String companyListTypeOf(PearsOperation operation) {
    return switch (operation) {
      case PearsOperation.ConsortiumListCreate create -> create.companyListType();
      case PearsOperation.ConsortiumListChange change -> change.companyListType();
      default -> null;
    };
  }
}
