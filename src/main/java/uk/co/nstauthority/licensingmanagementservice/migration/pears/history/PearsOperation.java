package uk.co.nstauthority.licensingmanagementservice.migration.pears.history;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * One operation from a licence's PEARS history, with its type-specific payload pulled out of the
 * generic NAME/VALUE attributes.
 */
public sealed interface PearsOperation {

  /**
   * The PEARS OPERATION_TYPE. Redundant with the runtime type for the modelled variants, and the
   * only thing known about an {@link Omitted} or {@link Unrecognised} one.
   */
  String typeName();

  /**
   * Which transaction and operation this is, and the position it belongs to.
   */
  Header header();

  //The status PEARS gives an operation, narrowed to what {@code licence-history.sql} can return.
  enum OperationStatus { LIVE, LEGACY, CORRECTED }

  enum EntryType { SET, REMOVE, TRANSFER }

  /**
   * CONSORTIUM_LIST_CREATE -- a company list brought into being, whichever list
   * {@code companyListType} names.
   *
   * @param systemComment   nullable
   * @param companyListType one of {@link PearsCompanyListType}
   */
  record ConsortiumListCreate(
      Header header,
      String systemComment,
      String companyListType,
      List<ConsortiumEntry> entries
  ) implements PearsOperation {

    @Override
    public String typeName() {
      return PearsOperationType.CONSORTIUM_LIST_CREATE;
    }
  }

  record ConsortiumListChange(
      Header header,
      String systemComment,
      String companyListType,
      List<ConsortiumEntry> entries
  ) implements PearsOperation {

    @Override
    public String typeName() {
      return PearsOperationType.CONSORTIUM_LIST_CHANGE;
    }
  }

  /**
   * An operation whose XML the query did not fetch, because no migrator asked for its type. Only its
   * type and position are known, which is all the migration needs of it.
   */
  record Omitted(Header header, String typeName) implements PearsOperation {
  }

  /**
   * An OPERATION_TYPE this model does not know about, whose XML was fetched. Keeps the raw
   * attribute bag so nothing is silently lost, and keeps schema drift from throwing.
   */
  record Unrecognised(Header header, String typeName, Map<String, String> attributes) implements PearsOperation {
  }

  /**
   * Which operation this is, and where it sits in the licence's history.
   *
   * @param regulatorReference the transaction's reference in PEARS, which is the position's name;
   *                           nullable
   * @param positionDate       the date PEARS holds the position on; nullable
   * @param positionSequence   where the transaction comes in the live simulation's order for that date
   * @param eventDate          nullable
   */
  record Header(
      long tranId,
      String regulatorReference,
      LocalDate positionDate,
      int positionSequence,
      long operationId,
      int operationSequence,
      OperationStatus operationStatus,
      LocalDate eventDate,
      String licenceType
  ) {
  }

  /**
   * One line of a company list: what happened, and to whom.
   *
   * @param status    nullable, e.g. EXTANT
   * @param primary   nullable -- the feed emits an empty organisation element rather than omitting it
   * @param secondary null unless {@code type} is TRANSFER
   */
  record ConsortiumEntry(
      EntryType type,
      Organisation primary,
      String status,
      Organisation secondary
  ) {
  }

  /**
   * An organisation, by its Energy Portal organisation unit id.
   *
   * @param name nullable because some organisations appear by id only
   */
  record Organisation(long id, String name) {
  }
}
