package uk.co.nstauthority.licensingmanagementservice.migration.pears.history;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
   * PED_BLOCK_CREATE -- blocks brought into the licence. Its entries are all SET, and each names an
   * output block and no input, because nothing was there before.
   */
  record BlockCreate(Header header, List<BlockEntry> entries) implements PearsOperation {

    @Override
    public String typeName() {
      return PearsOperationType.PED_BLOCK_CREATE;
    }
  }

  /**
   * PED_BLOCK_CHANGE -- a block replaced by one or more successors. Its entries are TRANSFER, and
   * each names the block that ended and the block that took its place.
   *
   * <p>This is what a partial surrender looks like in PEARS, and also what a redefinition looks
   * like; the two are told apart by whether any area was given up, since PEARS flags them alike.
   */
  record BlockChange(Header header, List<BlockEntry> entries) implements PearsOperation {

    /**
     * Below this, a difference in area is PEARS working its geometry out rather than ground given
     * up: the smallest surrender PEARS holds gives up more than a square kilometre, and every
     * difference under a tenth of one is arithmetic.
     */
    private static final BigDecimal SURRENDER_TOLERANCE_KM2 = new BigDecimal("0.1");

    /**
     * The si_ids of the blocks this change gave ground up from: those whose successors do not add
     * back up to what they replaced. A change names one entry per successor, each repeating the
     * block it replaced, so a block broken into parts that together cover it is a redefinition even
     * though each part alone is smaller than the block it replaced.
     *
     * <p>A block whose area is missing on either side gave up nothing, since nothing can be said
     * about it.
     */
    public Set<Integer> surrenderedInputSiIds() {
      var replacedAreas = new LinkedHashMap<Integer, BigDecimal>();
      var successorAreas = new HashMap<Integer, BigDecimal>();
      var unknown = new HashSet<Integer>();

      for (var entry : entries) {
        var input = entry.input();
        if (input == null || input.siId() == null || input.areaKm2() == null) {
          continue;
        }
        replacedAreas.putIfAbsent(input.siId(), input.areaKm2());

        var output = entry.output();
        if (output == null || output.areaKm2() == null) {
          unknown.add(input.siId());
        } else {
          successorAreas.merge(input.siId(), output.areaKm2(), BigDecimal::add);
        }
      }

      var surrendered = new LinkedHashSet<Integer>();
      replacedAreas.forEach((siId, replaced) -> {
        var kept = successorAreas.getOrDefault(siId, BigDecimal.ZERO);
        if (!unknown.contains(siId) && replaced.subtract(kept).compareTo(SURRENDER_TOLERANCE_KM2) > 0) {
          surrendered.add(siId);
        }
      });
      return surrendered;
    }

    @Override
    public String typeName() {
      return PearsOperationType.PED_BLOCK_CHANGE;
    }
  }

  /**
   * PED_BLOCK_END -- blocks leaving the licence outright. Its entries are REMOVE, and each names an
   * input block with no surviving output.
   */
  record BlockEnd(Header header, List<BlockEntry> entries) implements PearsOperation {

    @Override
    public String typeName() {
      return PearsOperationType.PED_BLOCK_END;
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

  /**
   * One block a block operation acted on: the block it ended, the block it left behind, or both.
   *
   * <p>An si_id is a spatial instance in PEARS and the legacy id of a gis-framework feature, which
   * is how a block here becomes a block this application holds. Either side can be absent, and an
   * absent one is not an error: a SET has no input, a REMOVE has no surviving output, and an entry
   * on a transaction PEARS holds no live data point for resolves neither.
   *
   * @param type   nullable, for an entry type PEARS has added since
   * @param input  nullable
   * @param output nullable
   */
  record BlockEntry(EntryType type, Block input, Block output) {
  }

  /**
   * One side of a block entry, including all the data needed to identify it.
   * The area is also used to determine whether a partial surrender or redefinition happened when compared to another block.
   */
  record Block(Integer siId, String ref, String quadrantNo, String blockNo, String blockSuffix, BigDecimal areaKm2) {
  }
}
