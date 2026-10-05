package uk.co.nstauthority.licensingmanagementservice.migration.pears.operation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.blocksurrendertype.BlockSurrenderType;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.PartialSurrenderOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaDetails;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaSurrenderOutcome;
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

  private final PearsFeatureResolver featureResolver;

  BlockOperationMigrator(PearsFeatureResolver featureResolver) {
    this.featureResolver = featureResolver;
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
  public List<MigratedChange> migrate(
      PearsLicenceHistory history,
      MigrationNotes notes
  ) {
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

    var featureIdsBySiId = featureResolver.resolve(siIds(blockOperations), notes);
    var changes = new ArrayList<MigratedChange>();

    // Keyed by the block given up, holding the successors PEARS says it left behind, which are the parts the
    // licence kept and the only account of a surrender's shape PEARS records.
    var surrenderedBlockIdToNewBlockIds = new LinkedHashMap<UUID, List<UUID>>();
    var redefined = new LinkedHashSet<UUID>();
    var redefinitionOutputs = new LinkedHashSet<UUID>();

    // The subareas redrawn along with the ground, folded into whichever of the two changes redrew it. A surrender's
    // are keyed by the block given up and then by the part of it kept; a redefinition's outputs by the block they
    // were carried onto.
    var surrenderedSubareaOutcomes = new LinkedHashMap<UUID, Map<UUID, List<SubareaSurrenderOutcome>>>();
    var redefinedSubareas = new LinkedHashSet<SubareaDetails>();
    var redefinitionOutputSubareas = new LinkedHashMap<UUID, Set<SubareaDetails>>();

    // The operations that gave ground up and that redrew it, in PEARS' order, so the one change each is folded into
    // stands at the first of them.
    var surrenderKeys = new LinkedHashSet<PearsOperationKey>();
    var redefinitionKeys = new LinkedHashSet<PearsOperationKey>();

    var accumulator = new BlockChangeAccumulator(
        surrenderedBlockIdToNewBlockIds,
        redefined,
        redefinitionOutputs,
        surrenderKeys,
        redefinitionKeys,
        surrenderedSubareaOutcomes,
        redefinedSubareas,
        redefinitionOutputSubareas
    );

    for (var operation : blockOperations) {
      var at = PearsOperationKey.of(operation);

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
      changes.add(MigratedChange.from(
          new PearsPositionKey(position.transactionId()),
          firstOf(redefinitionKeys),
          List.of(LicenceOperation.newBlockRedefinitionOperation()
              .withReplacedFeatureIds(redefined)
              .withOutputFeatureIds(redefinitionOutputs)
              .withReplacedSubareas(redefinedSubareas)
              .withOutputSubareas(redefinitionOutputSubareas)
              .build())
      ));
    }

    if (!surrenderedBlockIdToNewBlockIds.isEmpty()) {
      changes.add(MigratedChange.from(
          new PearsPositionKey(position.transactionId()),
          firstOf(surrenderKeys),
          List.of(LicenceOperation.newPartialSurrenderOperation()
              .withSurrenderDate(position.positionDate())
              .withSurrenderedFeatureIds(surrenderedBlockIdToNewBlockIds.keySet())
              .withSurrenderDetails(surrenderDetails(surrenderedBlockIdToNewBlockIds, surrenderedSubareaOutcomes))
              .build())
      ));
    }

    return changes;
  }

  /**
   * Blocks created outright, emitted as a change of their own.
   *
   * <p>A block arrives covered by its subareas, and those go on the creation rather than becoming a
   * change of their own -- nothing happened to them that did not happen to the block. Every later
   * subarea change is {@code SubareaOperationMigrator}'s, which is why it leaves PED_BLOCK_CREATE
   * alone.
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
        List.of(LicenceOperation.newBlockCreateOperation()
            .withFeatureIds(created)
            .withCreatedSubareas(createdSubareas(create, featureIdsBySiId, notes, at))
            .build())
    ));
  }

  /**
   * The subareas the created blocks arrived covered by. PEARS names none of them in the operation's
   * payload, so these came out of the block rows {@code licence-history.sql} resolved.
   */
  private static Map<UUID, Set<SubareaDetails>> createdSubareas(
      PearsOperation.BlockCreate create,
      Map<Integer, UUID> featureIdsBySiId,
      MigrationNotes notes,
      PearsOperationKey at
  ) {
    var subareasByBlock = new LinkedHashMap<UUID, Set<SubareaDetails>>();
    for (var entry : create.subareaEntries()) {
      var output = entry.output();
      var blockFeatureId = output == null || output.blockSiId() == null
          ? null
          : featureIdsBySiId.get(output.blockSiId());
      if (output != null && blockFeatureId == null) {
        notes.unmapped("subarea a block arrived with sits on a block that could not be resolved", at.operationId());
        continue;
      }
      addOutputSubarea(
          subareasByBlock,
          blockFeatureId,
          entry,
          featureIdsBySiId,
          notes,
          at,
          "subarea a block arrived with could not be resolved"
      );
    }
    return subareasByBlock;
  }

  /**
   * Blocks ended outright, emitted as a change of their own.
   *
   * <p>Ending a block ends every subarea on it, which goes on the same change for the same reason a creation's
   * subareas do: nothing happened to them that did not happen to the block.
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

    var endedSubareas = new LinkedHashSet<SubareaDetails>();
    for (var entry : end.subareaEntries()) {
      var subarea = subareaDetails(
          entry,
          entry.input(),
          featureIdsBySiId,
          notes,
          at,
          "subarea ended with its block could not be resolved"
      );
      if (subarea != null) {
        endedSubareas.add(subarea);
      }
    }

    changes.add(MigratedChange.from(
        new PearsPositionKey(position.transactionId()),
        at,
        List.of(LicenceOperation.newBlockEndOperation()
            .withEndedFeatureIds(ended)
            .withEndedSubareas(endedSubareas)
            .build())
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
      Set<PearsOperationKey> redefinitionKeys,
      Map<UUID, Map<UUID, List<SubareaSurrenderOutcome>>> surrenderedSubareaOutcomes,
      Set<SubareaDetails> redefinedSubareas,
      Map<UUID, Set<SubareaDetails>> redefinitionOutputSubareas
  ) {}

  /**
   * One block change, folded a block at a time into the surrender and the redefinition the position emits once each.
   *
   * <p>The subareas on each block entry are folded into whichever of the two that entry went to. Only the ones carried
   * from the old block to the new belong here: a subarea added or dropped while the block was being redrawn is a
   * change to that subarea in its own right, and {@code SubareaOperationMigrator} makes it one.
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
    var subareasByBlockEntry = carriedSubareasByBlockEntry(change.subareaEntries());

    for (var entry : change.entries()) {
      var input = featureId(entry.input(), featureIdsBySiId);
      var outputs = outputFeatureIds(List.of(entry), featureIdsBySiId, notes, at);

      if (input == null) {
        notes.unmapped("block change whose superseded block could not be resolved", at.operationId());
        continue;
      }

      var subareas = subareasByBlockEntry.getOrDefault(entry.seq(), List.of());
      // A block entry names one successor, which is the block its subareas were carried onto.
      var successor = outputs.isEmpty() ? null : outputs.getFirst();
      if (successor == null && !subareas.isEmpty()) {
        notes.unmapped("subareas a block change carried onto a successor that could not be resolved", at.operationId());
      }

      var resolved = new ResolvedBlockEntry(input, outputs, successor, subareas);
      if (surrenderedInputSiIds.contains(entry.input().siId())) {
        addSurrenderedEntry(resolved, featureIdsBySiId, accumulator, notes, at);
      } else {
        addRedefinedEntry(resolved, featureIdsBySiId, accumulator, notes, at);
      }
    }
  }

  /**
   * One block entry of a block change, with its blocks resolved to features and the subareas it carried.
   */
  private record ResolvedBlockEntry(
      UUID input,
      List<UUID> outputs,
      UUID successor,
      List<PearsOperation.SubareaEntry> subareas
  ) {}

  private static void addSurrenderedEntry(
      ResolvedBlockEntry entry,
      Map<Integer, UUID> featureIdsBySiId,
      BlockChangeAccumulator accumulator,
      MigrationNotes notes,
      PearsOperationKey at
  ) {
    accumulator.surrendered().merge(entry.input(), entry.outputs(), (existing, added) ->
        Stream.concat(existing.stream(), added.stream()).distinct().toList());
    accumulator.surrenderKeys().add(at);
    if (entry.successor() != null && !entry.subareas().isEmpty()) {
      addSurrenderOutcomes(
          entry.subareas(),
          featureIdsBySiId,
          accumulator.surrenderedSubareaOutcomes()
              .computeIfAbsent(entry.input(), ignored -> new LinkedHashMap<>())
              .computeIfAbsent(entry.successor(), ignored -> new ArrayList<>()),
          notes,
          at
      );
    }
  }

  private static void addRedefinedEntry(
      ResolvedBlockEntry entry,
      Map<Integer, UUID> featureIdsBySiId,
      BlockChangeAccumulator accumulator,
      MigrationNotes notes,
      PearsOperationKey at
  ) {
    if (accumulator.redefined().add(entry.input())) {
      notes.note("block change that gave up no area, carried across as a redefinition");
    }
    accumulator.redefinitionOutputs().addAll(entry.outputs());
    accumulator.redefinitionKeys().add(at);
    for (var subarea : entry.subareas()) {
      var replaced = subareaDetails(
          subarea,
          subarea.input(),
          featureIdsBySiId,
          notes,
          at,
          "subarea version a block change carried across could not be resolved"
      );
      if (replaced != null) {
        accumulator.redefinedSubareas().add(replaced);
      }
      if (entry.successor() != null) {
        addOutputSubarea(
            accumulator.redefinitionOutputSubareas(),
            entry.successor(),
            subarea,
            featureIdsBySiId,
            notes,
            at,
            "subarea a block change carried onto its successor could not be resolved"
        );
      }
    }
  }

  /**
   * The subareas a block change carried from one block to its successor, by the block entry that carried them.
   *
   * <p>Keyed by block entry rather than by block because a block broken into two successors gives two entries naming
   * the one block they replaced, and each successor keeps its own share of the subareas.
   */
  private static Map<Integer, List<PearsOperation.SubareaEntry>> carriedSubareasByBlockEntry(
      List<PearsOperation.SubareaEntry> subareaEntries
  ) {
    var byBlockEntry = new LinkedHashMap<Integer, List<PearsOperation.SubareaEntry>>();
    for (var entry : subareaEntries) {
      if (entry.isCarriedWithItsBlock() && entry.blockEntrySeq() != null) {
        byBlockEntry.computeIfAbsent(entry.blockEntrySeq(), ignored -> new ArrayList<>()).add(entry);
      }
    }
    return byBlockEntry;
  }

  /**
   * What a surrender did to each subarea carried onto one part of a block kept. PEARS re-cuts a subarea onto the
   * successor as a new version, so one whose version changed is cropped to it; one PEARS carried as it was is kept.
   */
  private static void addSurrenderOutcomes(
      List<PearsOperation.SubareaEntry> subareaEntries,
      Map<Integer, UUID> featureIdsBySiId,
      List<SubareaSurrenderOutcome> outcomes,
      MigrationNotes notes,
      PearsOperationKey at
  ) {
    for (var entry : subareaEntries) {
      var replaced = subareaDetails(
          entry,
          entry.input(),
          featureIdsBySiId,
          notes,
          at,
          "subarea version a block change carried across could not be resolved"
      );
      var output = subareaDetails(
          entry,
          entry.output(),
          featureIdsBySiId,
          notes,
          at,
          "subarea a block change carried onto its successor could not be resolved"
      );
      if (replaced == null && output == null) {
        continue;
      }
      if (replaced == null || replaced.equals(output)) {
        outcomes.add(SubareaSurrenderOutcome.kept(replaced == null ? output : replaced));
      } else if (output == null) {
        outcomes.add(SubareaSurrenderOutcome.relinquished(replaced));
      } else {
        outcomes.add(SubareaSurrenderOutcome.cropped(replaced, output));
      }
    }
  }

  private static void addOutputSubarea(
      Map<UUID, Set<SubareaDetails>> subareasByBlock,
      UUID blockFeatureId,
      PearsOperation.SubareaEntry entry,
      Map<Integer, UUID> featureIdsBySiId,
      MigrationNotes notes,
      PearsOperationKey at,
      String reason
  ) {
    var details = subareaDetails(entry, entry.output(), featureIdsBySiId, notes, at, reason);
    if (details != null) {
      subareasByBlock.computeIfAbsent(blockFeatureId, ignored -> new LinkedHashSet<>()).add(details);
    }
  }

  /**
   * One side of a subarea, as the operation will hold it.
   */
  private static SubareaDetails subareaDetails(
      PearsOperation.SubareaEntry entry,
      PearsOperation.Subarea subarea,
      Map<Integer, UUID> featureIdsBySiId,
      MigrationNotes notes,
      PearsOperationKey at,
      String reason
  ) {
    if (subarea == null) {
      return null;
    }
    var featureId = subarea.siId() == null ? null : featureIdsBySiId.get(subarea.siId());
    if (featureId == null) {
      notes.unmapped(reason, at.operationId());
    }
    return new SubareaDetails(featureId, subarea.name(), entry.shortName());
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
      Map<UUID, List<UUID>> retainedBySurrenderedBlock,
      Map<UUID, Map<UUID, List<SubareaSurrenderOutcome>>> subareaOutcomesBySurrenderedBlock
  ) {
    var details = new LinkedHashMap<UUID, PartialSurrenderOperation.SurrenderDetails>();
    retainedBySurrenderedBlock.forEach((featureId, retained) -> details.put(
        featureId,
        new PartialSurrenderOperation.SurrenderDetails(
            BlockSurrenderType.PARTIAL_SURRENDER,
            null,
            List.of(),
            retained,
            subareaOutcomesBySurrenderedBlock.getOrDefault(featureId, Map.of())
        )
    ));
    return details;
  }

  private List<PearsOperation> blockOperations(PearsPosition position) {
    return position.operations().stream()
        .filter(this::supports)
        .toList();
  }

  private static Set<Integer> siIds(List<PearsOperation> blockOperations) {
    var siIds = new LinkedHashSet<Integer>();
    for (var operation : blockOperations) {
      for (var entry : entriesOf(operation)) {
        addSiId(siIds, entry.input());
        addSiId(siIds, entry.output());
      }
      for (var entry : subareaEntriesOf(operation)) {
        addSubareaSiId(siIds, entry.input());
        addSubareaSiId(siIds, entry.output());
      }
    }
    return siIds;
  }

  private static void addSiId(
      Set<Integer> siIds,
      PearsOperation.Block block
  ) {
    if (block != null && block.siId() != null) {
      siIds.add(block.siId());
    }
  }

  private static void addSubareaSiId(
      Set<Integer> siIds,
      PearsOperation.Subarea subarea
  ) {
    if (subarea != null && subarea.siId() != null) {
      siIds.add(subarea.siId());
    }
  }

  private static List<PearsOperation.SubareaEntry> subareaEntriesOf(PearsOperation operation) {
    return switch (operation) {
      case PearsOperation.BlockCreate create -> create.subareaEntries();
      case PearsOperation.BlockChange change -> change.subareaEntries();
      case PearsOperation.BlockEnd end -> end.subareaEntries();
      default -> List.of();
    };
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

  private static UUID featureId(
      PearsOperation.Block block,
      Map<Integer, UUID> featureIdsBySiId
  ) {
    if (block == null || block.siId() == null) {
      return null;
    }
    return featureIdsBySiId.get(block.siId());
  }
}
