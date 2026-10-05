package uk.co.nstauthority.licensingmanagementservice.migration.pears.operation;

import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaDetails;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.PearsLicenceHistory;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.PearsPosition;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.history.PearsOperation;

/**
 * Carries a licence's subarea operations across from PEARS.
 *
 * <p>
 * There are three types of subarea operations CREATE/CHANGE/END, which each modify subareas, however subareas area also effected
 * by all the block operations, so both sets of operations need to be accounted for in the migration.
 */
@Component
@Order(300)
class SubareaOperationMigrator implements PearsOperationMigrator {

  private final PearsFeatureResolver featureResolver;

  SubareaOperationMigrator(PearsFeatureResolver featureResolver) {
    this.featureResolver = featureResolver;
  }

  @Override
  public String name() {
    return "licence subareas";
  }

  /**
   * None, on the same terms as blocks. A subarea operation's payload is mostly the boundary of the
   * shape it made, and the query has already shredded the identity out of it.
   */
  @Override
  public Set<String> pearsOperationTypes() {
    return Set.of();
  }

  /**
   * The subarea operations. A block change is {@link BlockOperationMigrator}'s, even though the
   * subareas it adds or drops while redrawing a block are carried across here.
   */
  @Override
  public boolean supports(PearsOperation operation) {
    return operation instanceof PearsOperation.SubareaCreate
        || operation instanceof PearsOperation.SubareaChange
        || operation instanceof PearsOperation.SubareaEnd;
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
   * One position's subarea changes: one per operation per block, in PEARS' own order.
   */
  private List<MigratedChange> positionChanges(
      PearsPosition position,
      MigrationNotes notes
  ) {
    var operations = subareaBearingOperations(position);
    if (operations.isEmpty()) {
      return List.of();
    }

    var featureIdsBySiId = featureResolver.resolve(siIds(operations), notes);
    var changes = new ArrayList<MigratedChange>();

    for (var operation : operations) {
      var at = PearsOperationKey.of(operation);
      var entries = entriesOf(operation);

      // A subarea operation that named no subareas acts on nothing, so every branch below would
      // fall through it without a word, and the migration would read as clean rather than as the
      // document having arrived wrong. A block change with nothing left after the carried subareas
      // are taken out is the ordinary case, not a loss, so it says nothing.
      if (entries.isEmpty()) {
        if (!(operation instanceof PearsOperation.BlockChange)) {
          notes.unmapped("subarea operation that named no subareas", at.operationId());
        }
        continue;
      }

      for (var group : entriesByGroup(entries, featureIdsBySiId, notes, at).values()) {
        var operationForBlock = groupOperation(group, featureIdsBySiId, notes, at);
        if (operationForBlock != null) {
          changes.add(MigratedChange.from(
              new PearsPositionKey(position.transactionId()),
              at,
              List.of(operationForBlock)
          ));
        }
      }
    }

    return changes;
  }

  /**
   * One group's change, or null, with the reason noted, where its block or its subareas could not be resolved.
   */
  @Nullable
  private static LicenceOperation groupOperation(
      List<PearsOperation.SubareaEntry> group,
      Map<Integer, UUID> featureIdsBySiId,
      MigrationNotes notes,
      PearsOperationKey at
  ) {
    var blockFeatureId = groupBlockFeatureId(group, featureIdsBySiId);
    if (blockFeatureId == null) {
      notes.unmapped("subarea whose block could not be resolved", at.operationId());
      return null;
    }
    var operation = toOperation(blockFeatureId, group, featureIdsBySiId, notes, at);
    if (operation == null) {
      notes.unmapped("subarea change whose subareas could not be resolved", at.operationId());
    }
    return operation;
  }

  /**
   * Which change an entry belongs to. Exactly one of the two is set.
   *
   * @param blockEntrySeq  the block entry the subarea rode in on, for a block operation's entries
   * @param blockFeatureId the block the entry names, for a subarea operation's own entries
   */
  private record SubareaGroupKey(
      @Nullable Integer blockEntrySeq,
      @Nullable UUID blockFeatureId
  ) {
  }

  /**
   * The entries of one operation, grouped by the change each of them belongs to.
   *
   * <p>A block operation's entries group by the block entry they rode in on, which is the key
   * {@link BlockOperationMigrator} groups its carried subareas by. That is what keeps a subarea
   * ended with its block together with the one that replaced it: the two sit on different
   * PED_LICENCE_BLOCKS rows and so on different features, and grouped by feature they come apart
   * again into an ending and a creation -- the same split the query fixed on its own side, and what
   * made a block re-cut with fresh subareas read as a creation PEARS never made.
   *
   * <p>A subarea operation's own entries have no block entry, the operation being the subarea's
   * work rather than a block's, and group by the block they name. Both of their sides are looked up
   * on that one block, so a group is a block either way.
   */
  private static Map<SubareaGroupKey, List<PearsOperation.SubareaEntry>> entriesByGroup(
      List<PearsOperation.SubareaEntry> entries,
      Map<Integer, UUID> featureIdsBySiId,
      MigrationNotes notes,
      PearsOperationKey at
  ) {
    var byGroup = new LinkedHashMap<SubareaGroupKey, List<PearsOperation.SubareaEntry>>();

    for (var entry : entries) {
      SubareaGroupKey key;
      if (entry.blockEntrySeq() != null) {
        key = new SubareaGroupKey(entry.blockEntrySeq(), null);
      } else {
        var blockFeatureId = blockFeatureId(entry, featureIdsBySiId);
        if (blockFeatureId == null) {
          notes.unmapped("subarea whose block could not be resolved", at.operationId());
          continue;
        }
        key = new SubareaGroupKey(null, blockFeatureId);
      }
      byGroup.computeIfAbsent(key, ignored -> new ArrayList<>()).add(entry);
    }

    return byGroup;
  }

  /**
   * The block a group's change is against: the successor block, which is the one the licence goes
   * on holding, falling back to the block that went where nothing succeeded it.
   *
   * <p>Read across the group rather than off one entry, because only the entries that left
   * something behind name the successor. A subarea a block change dropped names the block that
   * ended and nothing else, and the change it belongs to is still against the block that replaced
   * it.
   */
  @Nullable
  private static UUID groupBlockFeatureId(
      List<PearsOperation.SubareaEntry> entries,
      Map<Integer, UUID> featureIdsBySiId
  ) {
    for (var entry : entries) {
      var outputBlock = blockFeatureId(entry.output(), featureIdsBySiId);
      if (outputBlock != null) {
        return outputBlock;
      }
    }
    for (var entry : entries) {
      var inputBlock = blockFeatureId(entry.input(), featureIdsBySiId);
      if (inputBlock != null) {
        return inputBlock;
      }
    }
    return null;
  }

  private static LicenceOperation toOperation(
      UUID blockFeatureId,
      List<PearsOperation.SubareaEntry> entries,
      Map<Integer, UUID> featureIdsBySiId,
      MigrationNotes notes,
      PearsOperationKey at
  ) {
    var output = new LinkedHashSet<SubareaDetails>();
    var replaced = new LinkedHashSet<SubareaDetails>();
    var leavesSomethingBehind = false;
    var endsSomething = false;

    for (var entry : entries) {
      if (entry.hasOutputSide()) {
        leavesSomethingBehind = true;
        addSubarea(
            output,
            entry,
            entry.output(),
            featureIdsBySiId,
            notes,
            at,
            "subarea an operation left behind could not be resolved"
        );
      }
      if (entry.hasInputSide()) {
        endsSomething = true;
        addSubarea(
            replaced,
            entry,
            entry.input(),
            featureIdsBySiId,
            notes,
            at,
            "subarea version an operation ended could not be resolved"
        );
      }
    }

    if (output.isEmpty() && replaced.isEmpty()) {
      return null;
    }
    if (!endsSomething) {
      return LicenceOperation.newSubareaCreateOperation()
          .withBlockFeatureId(blockFeatureId)
          .withCreatedSubareas(output)
          .build();
    }
    if (!leavesSomethingBehind) {
      return LicenceOperation.newSubareaEndOperation()
          .withBlockFeatureId(blockFeatureId)
          .withEndedSubareas(replaced)
          .build();
    }
    return LicenceOperation.newSubAreaOperation()
        .withBlockFeatureId(blockFeatureId)
        .withReplacedSubareas(replaced)
        .withOutputSubareas(output)
        .build();
  }

  /**
   * The operations this migrator takes subareas from.
   *
   * <p>PED_BLOCK_CREATE and PED_BLOCK_END are absent on purpose: every subarea on those arrived or
   * left with its block, so it belongs to the block's own change and {@link BlockOperationMigrator}
   * records it there. PED_BLOCK_CHANGE is here only for the subareas it added or dropped, which
   * {@link #entriesOf} filters down to.
   */
  private List<PearsOperation> subareaBearingOperations(PearsPosition position) {
    return position.operations().stream()
        .filter(operation -> supports(operation) || operation instanceof PearsOperation.BlockChange)
        .toList();
  }

  private static Set<Integer> siIds(List<PearsOperation> operations) {
    var siIds = new LinkedHashSet<Integer>();
    for (var operation : operations) {
      for (var entry : entriesOf(operation)) {
        addSiIds(siIds, entry.input());
        addSiIds(siIds, entry.output());
      }
    }
    return siIds;
  }

  private static void addSiIds(
      Set<Integer> siIds,
      PearsOperation.Subarea subarea
  ) {
    if (subarea == null) {
      return;
    }
    if (subarea.siId() != null) {
      siIds.add(subarea.siId());
    }
    if (subarea.blockSiId() != null) {
      siIds.add(subarea.blockSiId());
    }
  }

  /**
   * One side, as the operation will hold it: the feature where PEARS scribed the subarea a shape,
   * and the names either way.
   *
   * <p>A side with no feature is still recorded, because the operation says the side was there and
   * PEARS still says what it was. It is reported as well, so the shapes missing from the spatial
   * data stay visible rather than being quietly absorbed -- the note is a report now, not the
   * reason an operation disappears.
   *
   * <p>The short name comes from the entry rather than the side: it is read off the operation's own
   * payload, so it is there even where the data point held no row to take a title from.
   */
  private static void addSubarea(
      Set<SubareaDetails> subareas,
      PearsOperation.SubareaEntry entry,
      PearsOperation.Subarea subarea,
      Map<Integer, UUID> featureIdsBySiId,
      MigrationNotes notes,
      PearsOperationKey at,
      String reason
  ) {
    var featureId = subarea == null || subarea.siId() == null
        ? null
        : featureIdsBySiId.get(subarea.siId());
    if (featureId == null) {
      notes.unmapped(reason, at.operationId());
    }
    subareas.add(new SubareaDetails(
        featureId,
        subarea == null ? null : subarea.name(),
        entry.shortName()
    ));
  }

  /**
   * The block an entry happened on, taken from whichever side has one. The two sides name the same
   * block except on a block change, where the output side is the successor block and is the one
   * that matters, since that is the block the licence goes on holding.
   */
  private static UUID blockFeatureId(
      PearsOperation.SubareaEntry entry,
      Map<Integer, UUID> featureIdsBySiId
  ) {
    var outputBlock = blockFeatureId(entry.output(), featureIdsBySiId);
    return outputBlock != null ? outputBlock : blockFeatureId(entry.input(), featureIdsBySiId);
  }

  private static UUID blockFeatureId(
      PearsOperation.Subarea subarea,
      Map<Integer, UUID> featureIdsBySiId
  ) {
    if (subarea == null || subarea.blockSiId() == null) {
      return null;
    }
    return featureIdsBySiId.get(subarea.blockSiId());
  }

  /**
   * The entries this migrator is responsible for.
   *
   * <p>A subarea operation's are all of them. A block change's are only those that did not come
   * across with the block: one with both sides was carried by the block change and is recorded on
   * that change, so taking it here would show the same subarea twice on the one position -- which
   * is exactly what this migrator did before, inventing thousands of subarea creations against the
   * two PEARS holds.
   */
  private static List<PearsOperation.SubareaEntry> entriesOf(PearsOperation operation) {
    return switch (operation) {
      case PearsOperation.SubareaCreate create -> create.subareaEntries();
      case PearsOperation.SubareaChange change -> change.subareaEntries();
      case PearsOperation.SubareaEnd end -> end.subareaEntries();
      case PearsOperation.BlockChange change -> change.subareaEntries().stream()
          .filter(entry -> !entry.isCarriedWithItsBlock())
          .toList();
      default -> List.of();
    };
  }
}
