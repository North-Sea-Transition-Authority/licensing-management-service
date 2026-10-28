package uk.co.nstauthority.licensingmanagementservice.licence.position.change.view;

import jakarta.annotation.Nullable;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaDetails;

/**
 * Everything a licence holds at a position, built up by applying its operations in order.
 *
 * @param blockFeatureIdToSubareas the blocks the licence holds and the subareas dividing up each of them. Only block
 *                                 operations change which blocks are held; subarea operations only change the subareas
 *                                 on a held block.
 * @param blockFeatureIdToSubareaEquity the beneficial interests held in each subarea, by organisation. Ending a block
 *                                      or subarea drops its beneficial interests.
 */
public record LicencePositionState(
    @Nullable Integer administratorId,
    @Nullable Integer storageOperatorId,
    Set<Integer> licenseeIds,
    Map<Integer, BigDecimal> equityByOrganisationId,
    Map<UUID, Set<SubareaDetails>> blockFeatureIdToSubareas,
    Map<UUID, Map<SubareaDetails, Map<Integer, BigDecimal>>> blockFeatureIdToSubareaEquity
) {

  public static final LicencePositionState EMPTY = new LicencePositionState(null, null, Set.of(), Map.of(), Map.of(), Map.of());

  public LicencePositionState withAdministratorId(Integer administratorId) {
    return new LicencePositionState(
        administratorId,
        storageOperatorId,
        licenseeIds,
        equityByOrganisationId,
        blockFeatureIdToSubareas,
        blockFeatureIdToSubareaEquity
    );
  }

  public LicencePositionState withStorageOperatorId(Integer storageOperatorId) {
    return new LicencePositionState(
        administratorId,
        storageOperatorId,
        licenseeIds,
        equityByOrganisationId,
        blockFeatureIdToSubareas,
        blockFeatureIdToSubareaEquity
    );
  }

  public LicencePositionState withEquityByOrganisationId(Map<Integer, BigDecimal> equityByOrganisationId) {
    return new LicencePositionState(
        administratorId,
        storageOperatorId,
        licenseeIds,
        equityByOrganisationId,
        blockFeatureIdToSubareas,
        blockFeatureIdToSubareaEquity
    );
  }

  public LicencePositionState withLicenseeIds(Collection<Integer> licenseesToAdd, Collection<Integer> licenseesToRemove) {
    var newLicenseeIds = new HashSet<>(licenseeIds);
    newLicenseeIds.addAll(licenseesToAdd);
    newLicenseeIds.removeAll(licenseesToRemove);
    return new LicencePositionState(
        administratorId,
        storageOperatorId,
        newLicenseeIds,
        equityByOrganisationId,
        blockFeatureIdToSubareas,
        blockFeatureIdToSubareaEquity
    );
  }

  public Set<UUID> blockFeatureIds() {
    return Set.copyOf(blockFeatureIdToSubareas.keySet());
  }

  public List<SubareaDetails> subareasOf(UUID blockFeatureId) {
    return List.copyOf(blockFeatureIdToSubareas.getOrDefault(blockFeatureId, Set.of()));
  }

  public boolean hasFeatures(Collection<UUID> blockFeatureIds, Collection<SubareaDetails> subareas) {
    if (!blockFeatureIdToSubareas.keySet().containsAll(blockFeatureIds)) {
      return false;
    }

    var subareasOnBlocks = blockFeatureIds.stream()
        .flatMap(blockFeatureId -> blockFeatureIdToSubareas.get(blockFeatureId).stream())
        .collect(Collectors.toSet());
    return subareasOnBlocks.containsAll(subareas);
  }

  /**
   * Holds the block, adding the subareas to any it is already divided into.
   */
  public LicencePositionState withBlock(
      UUID blockFeatureId,
      Collection<SubareaDetails> subareas
  ) {
    var blockSubareas = new LinkedHashSet<>(blockFeatureIdToSubareas.getOrDefault(blockFeatureId, Set.of()));
    blockSubareas.addAll(subareas);
    return withBlockSubareas(blockFeatureId, blockSubareas);
  }

  public LicencePositionState withoutBlocks(Collection<UUID> blockFeatureIds) {
    var updatedBlockFeatureIdToSubareas = new HashMap<>(blockFeatureIdToSubareas);
    updatedBlockFeatureIdToSubareas.keySet().removeAll(blockFeatureIds);
    var updatedBlockFeatureIdToSubareaEquity = new HashMap<>(blockFeatureIdToSubareaEquity);
    updatedBlockFeatureIdToSubareaEquity.keySet().removeAll(blockFeatureIds);

    return withBlockFeatureIdToSubareas(updatedBlockFeatureIdToSubareas)
        .withBlockFeatureIdToSubareaEquity(updatedBlockFeatureIdToSubareaEquity);
  }

  public LicencePositionState withSubareaEquity(
      UUID blockFeatureId,
      SubareaDetails subareaDetails,
      Integer organisationId,
      BigDecimal equity
  ) {
    var subareaEquityForBlock = new HashMap<>(blockFeatureIdToSubareaEquity.getOrDefault(blockFeatureId, Map.of()));
    var subareaEquityByOrganisationId = new HashMap<>(subareaEquityForBlock.getOrDefault(subareaDetails, Map.of()));

    subareaEquityForBlock.put(subareaDetails, subareaEquityByOrganisationId);
    subareaEquityByOrganisationId.put(organisationId, equity);

    return withBlockSubareaEquity(blockFeatureId, subareaEquityForBlock);
  }

  /**
   * Adds the subareas to the block, leaving the state as it was when the block is not held.
   */
  public LicencePositionState withSubareas(
      UUID blockFeatureId,
      Collection<SubareaDetails> subareas
  ) {
    var currentSubareas = blockFeatureIdToSubareas.get(blockFeatureId);
    if (currentSubareas == null) {
      return this;
    }

    var blockSubareas = new LinkedHashSet<>(currentSubareas);
    blockSubareas.addAll(subareas);
    return withBlockSubareas(blockFeatureId, blockSubareas);
  }

  /**
   * Removes the subareas and their beneficial interests from the block, leaving the state as it was when the block is
   * not held.
   */
  public LicencePositionState withoutSubareas(
      UUID blockFeatureId,
      Collection<SubareaDetails> subareas
  ) {
    var currentSubareas = blockFeatureIdToSubareas.get(blockFeatureId);
    if (currentSubareas == null) {
      return this;
    }

    var blockSubareas = new LinkedHashSet<>(currentSubareas);
    blockSubareas.removeAll(subareas);
    var subareaEquityForBlock = new HashMap<>(blockFeatureIdToSubareaEquity.getOrDefault(blockFeatureId, Map.of()));
    subareaEquityForBlock.keySet().removeAll(subareas);
    return withBlockSubareas(blockFeatureId, blockSubareas)
        .withBlockSubareaEquity(blockFeatureId, subareaEquityForBlock);
  }

  private LicencePositionState withBlockSubareas(
      UUID blockFeatureId,
      Set<SubareaDetails> blockSubareas
  ) {
    var updatedBlockFeatureIdToSubareas = new HashMap<>(blockFeatureIdToSubareas);
    updatedBlockFeatureIdToSubareas.put(blockFeatureId, Collections.unmodifiableSet(blockSubareas));
    return withBlockFeatureIdToSubareas(updatedBlockFeatureIdToSubareas);
  }

  private LicencePositionState withBlockFeatureIdToSubareas(Map<UUID, Set<SubareaDetails>> blockFeatureIdToSubareas) {
    return new LicencePositionState(
        administratorId,
        storageOperatorId,
        licenseeIds,
        equityByOrganisationId,
        Collections.unmodifiableMap(blockFeatureIdToSubareas),
        blockFeatureIdToSubareaEquity
    );
  }

  private LicencePositionState withBlockSubareaEquity(
      UUID blockFeatureId,
      Map<SubareaDetails, Map<Integer, BigDecimal>> subareaEquityForBlock
  ) {
    var updatedBlockFeatureIdToSubareaEquity = new HashMap<>(blockFeatureIdToSubareaEquity);
    if (subareaEquityForBlock.isEmpty()) {
      updatedBlockFeatureIdToSubareaEquity.remove(blockFeatureId);
    } else {
      updatedBlockFeatureIdToSubareaEquity.put(blockFeatureId, subareaEquityForBlock);
    }
    return withBlockFeatureIdToSubareaEquity(updatedBlockFeatureIdToSubareaEquity);
  }

  private LicencePositionState withBlockFeatureIdToSubareaEquity(
      Map<UUID, Map<SubareaDetails, Map<Integer, BigDecimal>>> blockFeatureIdToSubareaEquity
  ) {
    return new LicencePositionState(
        administratorId,
        storageOperatorId,
        licenseeIds,
        equityByOrganisationId,
        blockFeatureIdToSubareas,
        Collections.unmodifiableMap(blockFeatureIdToSubareaEquity)
    );
  }
}
