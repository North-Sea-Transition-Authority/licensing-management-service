package uk.co.nstauthority.licensingmanagementservice.migration.pears.operation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.fivium.gisframework.feature.FeatureService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.blocksurrendertype.BlockSurrenderType;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.PartialSurrenderOperation;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.PearsLicenceHistory;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.PearsPosition;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.history.PearsOperation;

/**
 * Carries a licence's block operations across from PEARS.
 *
 * <p>Three judgements shape everything below.
 *
 * <p>First, a block change is a partial surrender where the licence ends up holding less ground than
 * it did, and a redefinition where it does not. PEARS does not distinguish them -- both are
 * PED_BLOCK_CHANGE and both flag the position as BLOCK_CHANGE -- so the areas PEARS holds are the
 * only thing to go on, summed across the successors a block was broken into rather than weighed one
 * at a time.
 *
 * <p>Second, a position holds at most one partial surrender, because
 * {@link PartialSurrenderOperation} has a fixed id so that a correction can find it again. A
 * transaction can hold nine block changes, so they fold into one surrender covering every block the
 * transaction gave up.
 *
 * <p>Third, transactions can hold multiple types of block operations, so the order they are applied
 * in has to be the order PEARS put them in.
 *
 * <p>A block ended outright is a PED_BLOCK_END, carried across as its own {@code BlockEndOperation}.
 *
 */
@Component
@Order(200)
class BlockOperationMigrator implements PearsOperationMigrator {

  private final FeatureService featureService;

  BlockOperationMigrator(FeatureService featureService) {
    this.featureService = featureService;
  }

  @Override
  public String name() {
    return "licence blocks";
  }

  /**
   * None. Block operations are the one kind the history describes without their payload: the query
   * shreds the blocks they acted on out of the XML and leaves the geometry, which is the bulk of a
   * document, behind. Asking for the payload here would undo that.
   */
  @Override
  public Set<String> pearsOperationTypes() {
    return Set.of();
  }

  @Override
  public boolean supports(PearsOperation operation) {
    return operation instanceof PearsOperation.BlockCreate
        || operation instanceof PearsOperation.BlockChange
        || operation instanceof PearsOperation.BlockEnd;
  }

  @Override
  public List<MigratedChange> migrate(PearsLicenceHistory history, MigrationNotes notes) {
    var changes = new ArrayList<MigratedChange>();
    for (PearsPosition position : history.positions()) {
      changes.addAll(positionChanges(position, notes));
    }

    return List.copyOf(changes);
  }

  /**
   * One position's block changes.
   *
   * <p>The surrender and the redefinition are accumulated across the position's operations and
   * emitted once each; a creation is emitted per operation. All three name the PEARS operation they
   * stand in for, which is what puts them back into PEARS' order once every migrator has
   * contributed.
   */
  private List<MigratedChange> positionChanges(
      PearsPosition position,
      MigrationNotes notes
  ) {
    var blockOperations = blockOperations(position);
    if (blockOperations.isEmpty()) {
      return List.of();
    }

    var featureIdsBySiId = resolveFeatures(blockOperations, notes);
    var changes = new ArrayList<MigratedChange>();

    // Keyed by the block given up, holding the successors PEARS says it left behind, which are the parts the
    // licence kept and the only account of a surrender's shape PEARS records.
    var surrenderedBlockIdToNewBlockIds = new LinkedHashMap<UUID, List<UUID>>();
    var redefined = new LinkedHashSet<UUID>();
    var redefinitionOutputs = new LinkedHashSet<UUID>();

    // The operations that gave ground up and that redrew it, in PEARS' order, so the one change each is folded into
    // stands at the first of them.
    var surrenderKeys = new LinkedHashSet<PearsOperationKey>();
    var redefinitionKeys = new LinkedHashSet<PearsOperationKey>();

    var accumulator = new BlockChangeAccumulator(
        surrenderedBlockIdToNewBlockIds,
        redefined,
        redefinitionOutputs,
        surrenderKeys,
        redefinitionKeys
    );

    for (var operation : blockOperations) {
      var at = keyOf(operation);

      // A block operation with no blocks acts on nothing, so every branch below would fall through
      // it without a word. That is the one way this migrator can drop something in silence, and it
      // would read as a clean migration rather than as the document arriving wrong.
      if (entriesOf(operation).isEmpty()) {
        notes.unmapped("block operation that named no blocks", at.operationId());
        continue;
      }

      switch (operation) {
        case PearsOperation.BlockCreate create -> addBlockCreation(
            create,
            position,
            at,
            featureIdsBySiId,
            changes,
            notes
        );
        case PearsOperation.BlockChange change -> addBlockChange(
            change,
            at,
            featureIdsBySiId,
            accumulator,
            notes
        );
        case PearsOperation.BlockEnd end -> addBlockEnd(
            end,
            position,
            at,
            featureIdsBySiId,
            changes,
            notes
        );
        default -> throw new IllegalStateException("Not a block operation: " + operation.typeName());
      }
    }

    if (!redefined.isEmpty()) {
      changes.add(MigratedChange.from(new PearsPositionKey(position.transactionId()), firstOf(redefinitionKeys),
          List.of(LicenceOperation.newBlockRedefinitionOperation()
              .withReplacedFeatureIds(redefined)
              .withOutputFeatureIds(redefinitionOutputs)
              .build())));
    }

    if (!surrenderedBlockIdToNewBlockIds.isEmpty()) {
      changes.add(MigratedChange.from(new PearsPositionKey(position.transactionId()), firstOf(surrenderKeys),
          List.of(LicenceOperation.newPartialSurrenderOperation()
              .withSurrenderDate(position.positionDate())
              .withSurrenderedFeatureIds(surrenderedBlockIdToNewBlockIds.keySet())
              .withSurrenderDetails(surrenderDetails(surrenderedBlockIdToNewBlockIds))
              .build())));
    }

    return changes;
  }

  /**
   * Blocks created outright, emitted as a change of their own.
   */
  private static void addBlockCreation(
      PearsOperation.BlockCreate create,
      PearsPosition position,
      PearsOperationKey at,
      Map<Integer, UUID> featureIdsBySiId,
      List<MigratedChange> changes,
      MigrationNotes notes
  ) {
    var created = outputFeatureIds(create.entries(), featureIdsBySiId, notes, at);
    if (created.isEmpty()) {
      notes.unmapped("block creation whose blocks could not be resolved", at.operationId());
      return;
    }

    changes.add(MigratedChange.from(
        new PearsPositionKey(position.transactionId()),
        at,
        List.of(LicenceOperation.newBlockCreateOperation().withFeatureIds(created).build())
    ));
  }

  /**
   * Blocks ended outright, emitted as a change of their own.
   */
  private static void addBlockEnd(
      PearsOperation.BlockEnd end,
      PearsPosition position,
      PearsOperationKey at,
      Map<Integer, UUID> featureIdsBySiId,
      List<MigratedChange> changes,
      MigrationNotes notes
  ) {
    var ended = inputFeatureIds(end.entries(), featureIdsBySiId, notes, at);
    if (ended.isEmpty()) {
      notes.unmapped("block end whose blocks could not be resolved", at.operationId());
      return;
    }

    changes.add(MigratedChange.from(
        new PearsPositionKey(position.transactionId()),
        at,
        List.of(LicenceOperation.newBlockEndOperation().withEndedFeatureIds(ended).build())
    ));
  }

  /**
   * The surrender and redefinition accumulators a position's block changes are folded into, grouped so they can be
   * threaded through {@link #addBlockChange} as one parameter.
   */
  private record BlockChangeAccumulator(
      Map<UUID, List<UUID>> surrendered,
      Set<UUID> redefined,
      Set<UUID> redefinitionOutputs,
      Set<PearsOperationKey> surrenderKeys,
      Set<PearsOperationKey> redefinitionKeys
  ) {}

  /**
   * One block change, folded a block at a time into the surrender and the redefinition the position emits once each.
   */
  private static void addBlockChange(
      PearsOperation.BlockChange change,
      PearsOperationKey at,
      Map<Integer, UUID> featureIdsBySiId,
      BlockChangeAccumulator accumulator,
      MigrationNotes notes
  ) {
    // Worked out across the whole operation rather than per entry: a change names one entry per
    // successor, so a block broken into parts is only a surrender if they leave the block short.
    var surrenderedInputSiIds = change.surrenderedInputSiIds();

    for (var entry : change.entries()) {
      var input = featureId(entry.input(), featureIdsBySiId);
      var outputs = outputFeatureIds(List.of(entry), featureIdsBySiId, notes, at);

      if (input == null) {
        notes.unmapped("block change whose superseded block could not be resolved", at.operationId());
        continue;
      }

      if (surrenderedInputSiIds.contains(entry.input().siId())) {
        accumulator.surrendered().merge(input, outputs, (existing, added) ->
            Stream.concat(existing.stream(), added.stream()).distinct().toList());
        accumulator.surrenderKeys().add(at);
      } else {
        if (accumulator.redefined().add(input)) {
          notes.note("block change that gave up no area, carried across as a redefinition");
        }
        accumulator.redefinitionOutputs().addAll(outputs);
        accumulator.redefinitionKeys().add(at);
      }
    }
  }

  /**
   * The operation a folded change stands at, which is the first that contributed to it. Null only when nothing did,
   * and nothing is emitted in that case.
   */
  private static PearsOperationKey firstOf(Set<PearsOperationKey> operationKeys) {
    return operationKeys.stream().findFirst().orElse(null);
  }

  /**
   * Every surrendered block is a partial surrender rather than a full one: a PED_BLOCK_CHANGE always
   * leaves a successor behind, so part of the block was always kept. A block given up outright is a
   * PED_BLOCK_END, which this does not carry across.
   *
   * <p>There is no command journey, because there was no journey: the splitting happened in PEARS. The
   * detail records none, and one is made if and when someone corrects the block here.
   *
   * <p>The features inside the detail are the split parts that were given up, and those do not exist
   * here: PEARS records the part of a block a licence kept and nothing at all for the part it did
   * not. What is carried across is the other side of that -- the successor blocks the change left
   * behind, which is what the licence retained.
   */
  private static Map<UUID, PartialSurrenderOperation.SurrenderDetails> surrenderDetails(
      Map<UUID, List<UUID>> retainedBySurrenderedBlock
  ) {
    var details = new LinkedHashMap<UUID, PartialSurrenderOperation.SurrenderDetails>();
    retainedBySurrenderedBlock.forEach((featureId, retained) -> details.put(featureId,
        new PartialSurrenderOperation.SurrenderDetails(
            BlockSurrenderType.PARTIAL_SURRENDER, null, List.of(), retained)));
    return details;
  }

  private List<PearsOperation> blockOperations(PearsPosition position) {
    return position.operations().stream()
        .filter(this::supports)
        .toList();
  }

  /**
   * The features every block in this position names, looked up in one go rather than one block at a
   * time. A block's si_id in PEARS is the legacy id of the feature the GIS migration made from it.
   */
  private Map<Integer, UUID> resolveFeatures(List<PearsOperation> blockOperations, MigrationNotes notes) {
    var siIds = new LinkedHashSet<Integer>();
    for (var operation : blockOperations) {
      for (var entry : entriesOf(operation)) {
        addSiId(siIds, entry.input());
        addSiId(siIds, entry.output());
      }
    }

    if (siIds.isEmpty()) {
      return Map.of();
    }

    var featureIdsBySiId = new HashMap<Integer, UUID>();
    for (Feature feature : featureService.findAllByLegacyIdIn(siIds)) {
      featureIdsBySiId.put(feature.getLegacyId(), feature.getId());
    }

    for (var siId : siIds) {
      if (!featureIdsBySiId.containsKey(siId)) {
        notes.note("PEARS block has no migrated GIS feature, so no position can hold it");
      }
    }

    return featureIdsBySiId;
  }

  private static void addSiId(Set<Integer> siIds, PearsOperation.Block block) {
    if (block != null && block.siId() != null) {
      siIds.add(block.siId());
    }
  }

  private static List<PearsOperation.BlockEntry> entriesOf(PearsOperation operation) {
    return switch (operation) {
      case PearsOperation.BlockCreate create -> create.entries();
      case PearsOperation.BlockChange change -> change.entries();
      case PearsOperation.BlockEnd end -> end.entries();
      default -> List.of();
    };
  }

  private static List<UUID> outputFeatureIds(
      List<PearsOperation.BlockEntry> entries,
      Map<Integer, UUID> featureIdsBySiId,
      MigrationNotes notes,
      PearsOperationKey at
  ) {
    var featureIds = new LinkedHashSet<UUID>();
    for (var entry : entries) {
      var featureId = featureId(entry.output(), featureIdsBySiId);
      if (featureId != null) {
        featureIds.add(featureId);
      } else if (entry.output() != null) {
        notes.unmapped("block the operation left behind could not be resolved", at.operationId());
      }
    }
    return List.copyOf(featureIds);
  }

  private static List<UUID> inputFeatureIds(
      List<PearsOperation.BlockEntry> entries,
      Map<Integer, UUID> featureIdsBySiId,
      MigrationNotes notes,
      PearsOperationKey at
  ) {
    var featureIds = new LinkedHashSet<UUID>();
    for (var entry : entries) {
      var featureId = featureId(entry.input(), featureIdsBySiId);
      if (featureId != null) {
        featureIds.add(featureId);
      } else if (entry.input() != null) {
        notes.unmapped("block the operation ended could not be resolved", at.operationId());
      }
    }
    return List.copyOf(featureIds);
  }

  private static UUID featureId(PearsOperation.Block block, Map<Integer, UUID> featureIdsBySiId) {
    if (block == null || block.siId() == null) {
      return null;
    }
    return featureIdsBySiId.get(block.siId());
  }

  private static PearsOperationKey keyOf(PearsOperation operation) {
    return new PearsOperationKey(operation.header().operationSequence(), operation.header().operationId());
  }
}
