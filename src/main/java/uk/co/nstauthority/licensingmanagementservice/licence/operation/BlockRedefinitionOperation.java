package uk.co.nstauthority.licensingmanagementservice.licence.operation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.PositionValidationContext;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.PositionValidationError;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.LicencePositionState;

/**
 * Blocks redrawn without any ground being given up.
 *
 * <p>PEARS records this as the same kind of operation as a partial surrender -- a block ends and a
 * successor takes its place -- and flags it identically. What separates the two is that a surrender
 * leaves the licence holding less than it did, and a redefinition leaves it holding the same ground
 * described differently, usually after a boundary was recalculated.
 *
 * <p>Both sides are recorded because the replacement is not a surrender of the old block: the
 * licence holds the successor from here on, and a position replaying this has to swap one for the
 * other rather than simply dropping or adding.
 *
 * <p>Redrawing a block redraws the subareas on it, each becoming its own new shape against the
 * successor block. PEARS raises no subarea operation for that, so the subareas are recorded here
 * rather than as a change beside this one.
 *
 * @param replacedFeatureIds         the blocks that ended
 * @param outputFeatureIds           the blocks that took their place
 * @param replacedSubareas           the subarea versions that ended with them
 * @param outputFeatureIdToSubareas  the subarea versions that took their place, keyed by the output block each is on
 */
public record BlockRedefinitionOperation(
    UUID id,
    List<UUID> replacedFeatureIds,
    List<UUID> outputFeatureIds,
    List<SubareaDetails> replacedSubareas,
    Map<UUID, List<SubareaDetails>> outputFeatureIdToSubareas
) implements HiddenLicenceOperation, GeospatialLicenceOperation {

  public BlockRedefinitionOperation {
    Objects.requireNonNull(id, "id must not be null");
    replacedFeatureIds = replacedFeatureIds == null ? List.of() : List.copyOf(replacedFeatureIds);
    outputFeatureIds = outputFeatureIds == null ? List.of() : List.copyOf(outputFeatureIds);
    replacedSubareas = replacedSubareas == null ? List.of() : List.copyOf(replacedSubareas);
    outputFeatureIdToSubareas = outputFeatureIdToSubareas == null
        ? Map.of()
        : outputFeatureIdToSubareas.entrySet().stream()
            .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> List.copyOf(entry.getValue())));
  }

  public BlockRedefinitionOperation(
      List<UUID> replacedFeatureIds,
      List<UUID> outputFeatureIds,
      List<SubareaDetails> replacedSubareas,
      Map<UUID, List<SubareaDetails>> outputFeatureIdToSubareas
  ) {
    this(UUID.randomUUID(), replacedFeatureIds, outputFeatureIds, replacedSubareas, outputFeatureIdToSubareas);
  }

  @JsonIgnore
  public List<SubareaDetails> outputSubareas() {
    return outputFeatureIdToSubareas.values().stream()
        .flatMap(List::stream)
        .toList();
  }

  @Override
  public String type() {
    return BLOCK_REDEFINITION;
  }

  @Override
  public String displayName() {
    return "Block redefinition";
  }

  @Override
  public PositionValidationError validate(PositionValidationContext positionValidationContext) {
    return null;
  }

  /**
   * Swaps the replaced blocks for their successors. The replaced blocks go first, so a block that is both replaced and
   * output is still held.
   */
  @Override
  public LicencePositionState applyState(LicencePositionState licencePositionState) {
    var updatedState = licencePositionState.withoutBlocks(replacedFeatureIds);
    for (var blockFeatureId : outputFeatureIds) {
      updatedState = updatedState.withBlock(
          blockFeatureId,
          outputFeatureIdToSubareas.getOrDefault(blockFeatureId, List.of())
      );
    }
    return updatedState;
  }

  @Override
  public Set<UUID> featureIds() {
    return Stream.of(
            replacedFeatureIds.stream(),
            outputFeatureIds.stream(),
            SubareaDetails.featureIds(replacedSubareas).stream(),
            SubareaDetails.featureIds(outputSubareas()).stream()
        )
        .flatMap(Function.identity())
        .collect(Collectors.toSet());
  }

  public static class Builder {

    private Collection<UUID> replacedFeatureIds;
    private Collection<UUID> outputFeatureIds;
    private Collection<SubareaDetails> replacedSubareas;
    private Map<UUID, ? extends Collection<SubareaDetails>> outputFeatureIdToSubareas;

    public Builder withReplacedFeatureIds(Collection<UUID> replacedFeatureIds) {
      this.replacedFeatureIds = replacedFeatureIds;
      return this;
    }

    public Builder withOutputFeatureIds(Collection<UUID> outputFeatureIds) {
      this.outputFeatureIds = outputFeatureIds;
      return this;
    }

    public Builder withReplacedSubareas(Collection<SubareaDetails> replacedSubareas) {
      this.replacedSubareas = replacedSubareas;
      return this;
    }

    public Builder withOutputSubareas(Map<UUID, ? extends Collection<SubareaDetails>> outputFeatureIdToSubareas) {
      this.outputFeatureIdToSubareas = outputFeatureIdToSubareas;
      return this;
    }

    public BlockRedefinitionOperation build() {
      return new BlockRedefinitionOperation(
          replacedFeatureIds == null ? List.of() : replacedFeatureIds.stream().distinct().toList(),
          outputFeatureIds == null ? List.of() : outputFeatureIds.stream().distinct().toList(),
          replacedSubareas == null ? List.of() : replacedSubareas.stream().distinct().toList(),
          outputFeatureIdToSubareas == null
              ? Map.of()
              : outputFeatureIdToSubareas.entrySet().stream()
                  .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().stream().distinct().toList()))
      );
    }
  }
}
