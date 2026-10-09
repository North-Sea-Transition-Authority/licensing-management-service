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
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaDetails;

/**
 * Everything a licence holds at a position, built up by applying its operations in order.
 *
 * @param blockFeatureIdToSubareas the blocks the licence holds and the subareas dividing up each of them. Only block
 *                                 operations change which blocks are held; subarea operations only change the subareas
 *                                 on a held block.
 */
public record LicencePositionState(
    @Nullable Integer administratorId,
    Set<Integer> licenseeIds,
    Map<Integer, BigDecimal> equityByOrganisationId,
    Map<UUID, Set<SubareaDetails>> blockFeatureIdToSubareas
) {

  public static final LicencePositionState EMPTY = new LicencePositionState(null, Set.of(), Map.of(), Map.of());

  public LicencePositionState withAdministratorId(Integer administratorId) {
    return new LicencePositionState(administratorId, licenseeIds, equityByOrganisationId, blockFeatureIdToSubareas);
  }

  public LicencePositionState withEquityByOrganisationId(Map<Integer, BigDecimal> equityByOrganisationId) {
    return new LicencePositionState(administratorId, licenseeIds, equityByOrganisationId, blockFeatureIdToSubareas);
  }

  public LicencePositionState withLicenseeIds(Collection<Integer> licenseesToAdd, Collection<Integer> licenseesToRemove) {
    var newLicenseeIds = new HashSet<>(licenseeIds);
    newLicenseeIds.addAll(licenseesToAdd);
    newLicenseeIds.removeAll(licenseesToRemove);
    return new LicencePositionState(administratorId, newLicenseeIds, equityByOrganisationId, blockFeatureIdToSubareas);
  }

  public Set<UUID> blockFeatureIds() {
    return Set.copyOf(blockFeatureIdToSubareas.keySet());
  }

  public List<SubareaDetails> subareasOf(UUID blockFeatureId) {
    return List.copyOf(blockFeatureIdToSubareas.getOrDefault(blockFeatureId, Set.of()));
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
    return withBlockFeatureIdToSubareas(updatedBlockFeatureIdToSubareas);
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
   * Removes the subareas from the block, leaving the state as it was when the block is not held.
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
    return withBlockSubareas(blockFeatureId, blockSubareas);
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
        licenseeIds,
        equityByOrganisationId,
        Collections.unmodifiableMap(blockFeatureIdToSubareas)
    );
  }
}
