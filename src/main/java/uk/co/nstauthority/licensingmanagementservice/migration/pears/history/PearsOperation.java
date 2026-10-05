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
   *
   * <p>Every block create also creates subareas, so {@code subareaEntries} is the subareas created
   * alongside it.
   */
  record BlockCreate(
      Header header,
      List<BlockEntry> entries,
      List<SubareaEntry> subareaEntries
  ) implements PearsOperation {

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
   *
   * <p>Redrawing a block redraws every subarea on it, which is what {@code subareaEntries} holds.
   * A subarea moved onto a successor block arrives as an ending on the block that went and a
   * creation on the one that came, because in PEARS those are two rows against two blocks.
   */
  record BlockChange(
      Header header,
      List<BlockEntry> entries,
      List<SubareaEntry> subareaEntries
  ) implements PearsOperation {

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
  record BlockEnd(
      Header header,
      List<BlockEntry> entries,
      List<SubareaEntry> subareaEntries
  ) implements PearsOperation {

    @Override
    public String typeName() {
      return PearsOperationType.PED_BLOCK_END;
    }
  }

  /**
   * PED_SUBAREA_CREATE -- subareas brought into the licence in their own right, rather than
   * alongside the block they sit on. Its entries are SET, each naming an output subarea and no
   * input.
   */
  record SubareaCreate(
      Header header,
      List<SubareaEntry> subareaEntries
  ) implements PearsOperation {

    @Override
    public String typeName() {
      return PearsOperationType.PED_SUBAREA_CREATE;
    }
  }

  /**
   * PED_SUBAREA_CHANGE -- a subarea re-cut within the block it sits on. Its entries are mostly
   * TRANSFER, naming the version that ended and the one that took its place, but a single
   * operation can also SET a subarea it added and REMOVE one it dropped.
   */
  record SubareaChange(
      Header header,
      List<SubareaEntry> subareaEntries
  ) implements PearsOperation {

    @Override
    public String typeName() {
      return PearsOperationType.PED_SUBAREA_CHANGE;
    }
  }

  /**
   * PED_SUBAREA_END -- subareas leaving the licence while the block they sat on stays. Its entries
   * are REMOVE, each naming an input subarea with no surviving output.
   */
  record SubareaEnd(
      Header header,
      List<SubareaEntry> subareaEntries
  ) implements PearsOperation {

    @Override
    public String typeName() {
      return PearsOperationType.PED_SUBAREA_END;
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
   * @param seq    this entry's place in the operation, which a subarea entry names to say it came
   *               in on this block; nullable only on a document written before it was emitted
   * @param input  nullable
   * @param output nullable
   */
  record BlockEntry(
      EntryType type,
      Integer seq,
      Block input,
      Block output
  ) {
  }

  /**
   * One side of a block entry, including all the data needed to identify it.
   * The area is also used to determine whether a partial surrender or redefinition happened when compared to another block.
   */
  record Block(
      Integer siId,
      String ref,
      String quadrantNo,
      String blockNo,
      String blockSuffix,
      BigDecimal areaKm2
  ) {
  }

  /**
   * One subarea an operation acted on, on one block: the version it ended, the version it left
   * behind, or both.
   *
   * @param type          nullable, for an entry type PEARS has added since
   * @param blockEntrySeq the block entry this subarea came in on, and so which of the operation's
   *                      block changes it belongs to. Null on a subarea operation, which has no
   *                      block entries.
   * @param shortName     the name PEARS knows the subarea by, which is what identifies it across
   *                      versions; nullable only where the entry resolved nothing
   * @param input         nullable
   * @param output        nullable
   */
  record SubareaEntry(
      EntryType type,
      Integer blockEntrySeq,
      String shortName,
      Subarea input,
      Subarea output
  ) {

    /**
     * Whether this is one subarea carried from the block that ended to the block that replaced it,
     * which is what a block change does to every subarea on the ground it redrew. PEARS raises no
     * operation of its own for that -- 92% of its block changes sit in a transaction holding no
     * subarea operation at all -- so it belongs to the block change rather than standing alone.
     *
     * <p>Read off the entry type rather than off whether both sides resolved. PED_SUBAREAS.SI_ID
     * is nullable and around a quarter of its rows have none, so a side can be real and still
     * unidentifiable; treating that as an absent side is what made this migration report
     * creations PEARS never made.
     */
    public boolean isCarriedWithItsBlock() {
      return type == null ? input != null && output != null : type == EntryType.TRANSFER;
    }

    /**
     * Whether the operation left this subarea behind -- true for a subarea created and for one
     * re-cut, false for one ended.
     */
    public boolean hasOutputSide() {
      return type == null ? output != null : type != EntryType.REMOVE;
    }

    /**
     * Whether the operation ended a version of this subarea -- true for one ended and for one
     * re-cut, false for one created.
     */
    public boolean hasInputSide() {
      return type == null ? input != null : type != EntryType.SET;
    }
  }

  /**
   * One side of a subarea entry, together with the block it sits on.
   *
   * <p>The block is part of the side rather than of the entry because a block change moves a
   * subarea from one block to another, so the two sides can name different blocks.
   *
   * @param siId      nullable -- the subarea could not be placed in the live simulation
   * @param name      nullable -- PED_SUBAREAS.TITLE, the subarea's full name
   * @param blockSiId nullable
   * @param blockRef  nullable -- PED_LICENCE_BLOCK_REFS composes it and may not have
   */
  record Subarea(
      Integer siId,
      String name,
      Integer blockSiId,
      String blockRef
  ) {
  }
}
