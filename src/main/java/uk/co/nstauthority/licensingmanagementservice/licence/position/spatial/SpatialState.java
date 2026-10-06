package uk.co.nstauthority.licensingmanagementservice.licence.position.spatial;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.AdministratorOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.BlockCreateOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.BlockEndOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.BlockRedefinitionOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenseeOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.PartialSurrenderOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.PartialSurrenderOperation.SurrenderDetails;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SetEquityOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaCreateOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaDetails;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaEndOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaSurrenderOutcome;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.TransferEquityOperation;

/**
 * The blocks a licence holds and the subareas dividing up each of them, built up by applying its operations in order.
 * Only block operations change which blocks are held; subarea operations only change the subareas on a held block.
 */
class SpatialState {

  private final Map<UUID, Set<SubareaDetails>> blockIdToSubareas = new HashMap<>();

  Set<UUID> blockFeatureIds() {
    return Set.copyOf(blockIdToSubareas.keySet());
  }

  List<SubareaDetails> subareasOf(UUID blockFeatureId) {
    return List.copyOf(blockIdToSubareas.getOrDefault(blockFeatureId, Set.of()));
  }

  void apply(LicenceOperation operation) {
    switch (operation) {
      case BlockCreateOperation blockCreate -> blockCreate.createdBlockFeatureIds().forEach(blockFeatureId ->
          addBlock(blockFeatureId, blockCreate.createdBlockFeatureIdToSubareas().getOrDefault(blockFeatureId, List.of())));
      case BlockRedefinitionOperation blockRedefinition -> {
        blockRedefinition.replacedFeatureIds().forEach(blockIdToSubareas::remove);
        blockRedefinition.outputFeatureIds().forEach(blockFeatureId -> addBlock(
            blockFeatureId,
            blockRedefinition.outputFeatureIdToSubareas().getOrDefault(blockFeatureId, List.of())
        ));
      }
      case BlockEndOperation blockEnd -> blockEnd.endedFeatureIds().forEach(blockIdToSubareas::remove);
      case PartialSurrenderOperation partialSurrender -> applySurrender(partialSurrender);
      case SubareaCreateOperation subareaCreate ->
          addSubareas(subareaCreate.blockFeatureId(), subareaCreate.createdSubareas());
      case SubareaEndOperation subareaEnd -> removeSubareas(subareaEnd.blockFeatureId(), subareaEnd.endedSubareas());
      case SubareaOperation subarea -> {
        removeSubareas(subarea.blockFeatureId(), subarea.replacedSubareas());
        addSubareas(subarea.blockFeatureId(), subarea.outputSubareas());
      }
      case AdministratorOperation ignored -> {
        // Does not change the blocks or subareas held
      }
      case SetEquityOperation ignored -> {
        // Does not change the blocks or subareas held
      }
      case TransferEquityOperation ignored -> {
        // Does not change the blocks or subareas held
      }
      case LicenseeOperation ignored -> {
        // Does not change the blocks or subareas held
      }
    }
  }

  /**
   * A surrender that has not taken effect leaves the licence as it was. Once it has, the licence gives up the blocks
   * surrendered and holds each part kept of them instead, with the subareas left on that part.
   */
  private void applySurrender(PartialSurrenderOperation surrender) {
    if (!surrender.takesEffect()) {
      return;
    }

    blockIdToSubareas.keySet().removeAll(surrender.surrenderedFeatureIds());
    surrender.featureIdToSurrenderDetails().values().forEach(surrenderDetails -> surrenderDetails
        .retainedFeatureIds()
        .forEach(retainedFeatureId -> addBlock(retainedFeatureId, outputSubareasOf(surrenderDetails, retainedFeatureId))));
  }

  private static List<SubareaDetails> outputSubareasOf(
      SurrenderDetails surrenderDetails,
      UUID retainedFeatureId
  ) {
    return surrenderDetails.retainedFeatureIdToSubareas().getOrDefault(retainedFeatureId, List.of()).stream()
        .map(SubareaSurrenderOutcome::outputSubarea)
        .filter(Objects::nonNull)
        .toList();
  }

  private void addBlock(
      UUID blockFeatureId,
      Collection<SubareaDetails> subareas
  ) {
    blockIdToSubareas.computeIfAbsent(blockFeatureId, id -> new LinkedHashSet<>()).addAll(subareas);
  }

  private void addSubareas(
      UUID blockFeatureId,
      Collection<SubareaDetails> subareas
  ) {
    var blockSubareas = blockIdToSubareas.get(blockFeatureId);
    if (blockSubareas != null) {
      blockSubareas.addAll(subareas);
    }
  }

  private void removeSubareas(
      UUID blockFeatureId,
      Collection<SubareaDetails> subareas
  ) {
    var blockSubareas = blockIdToSubareas.get(blockFeatureId);
    if (blockSubareas != null) {
      blockSubareas.removeAll(subareas);
    }
  }
}
