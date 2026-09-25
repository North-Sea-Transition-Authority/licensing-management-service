package uk.co.nstauthority.licensingmanagementservice.licence.operation;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.PositionValidationContext;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.PositionValidationError;

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
 * rather than as a change beside this one. They take no part in the replay -- what a licence holds
 * is its blocks -- and are here as the record of what the redefinition did.
 *
 * @param replacedFeatureIds        the blocks that ended
 * @param outputFeatureIds          the blocks that took their place
 * @param replacedSubareas the subarea versions that ended with them
 * @param outputSubareas   the subarea versions that took their place
 */
public record BlockRedefinitionOperation(
    UUID id,
    List<UUID> replacedFeatureIds,
    List<UUID> outputFeatureIds,
    List<SubareaDetails> replacedSubareas,
    List<SubareaDetails> outputSubareas
) implements LicenceOperation {

  public BlockRedefinitionOperation {
    Objects.requireNonNull(id, "id must not be null");
    replacedFeatureIds = replacedFeatureIds == null ? List.of() : List.copyOf(replacedFeatureIds);
    outputFeatureIds = outputFeatureIds == null ? List.of() : List.copyOf(outputFeatureIds);
    replacedSubareas = replacedSubareas == null ? List.of() : List.copyOf(replacedSubareas);
    outputSubareas = outputSubareas == null ? List.of() : List.copyOf(outputSubareas);
  }

  public BlockRedefinitionOperation(
      List<UUID> replacedFeatureIds,
      List<UUID> outputFeatureIds,
      List<SubareaDetails> replacedSubareas,
      List<SubareaDetails> outputSubareas
  ) {
    this(UUID.randomUUID(), replacedFeatureIds, outputFeatureIds, replacedSubareas, outputSubareas);
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

  public static class Builder {

    private Collection<UUID> replacedFeatureIds;
    private Collection<UUID> outputFeatureIds;
    private Collection<SubareaDetails> replacedSubareas;
    private Collection<SubareaDetails> outputSubareas;

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

    public Builder withOutputSubareas(Collection<SubareaDetails> outputSubareas) {
      this.outputSubareas = outputSubareas;
      return this;
    }

    public BlockRedefinitionOperation build() {
      return new BlockRedefinitionOperation(
          replacedFeatureIds == null ? List.of() : replacedFeatureIds.stream().distinct().toList(),
          outputFeatureIds == null ? List.of() : outputFeatureIds.stream().distinct().toList(),
          replacedSubareas == null ? List.of() : replacedSubareas.stream().distinct().toList(),
          outputSubareas == null ? List.of() : outputSubareas.stream().distinct().toList());
    }
  }
}
