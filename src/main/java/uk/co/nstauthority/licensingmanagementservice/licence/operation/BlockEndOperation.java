package uk.co.nstauthority.licensingmanagementservice.licence.operation;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.PositionValidationContext;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.PositionValidationError;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.LicencePositionState;

/**
 * Operation for when a licence block is ended on a licence.
 *
 * <p>Ending a block ends the subareas on it, which PEARS records without raising a subarea
 * operation of its own, so they are part of this operation rather than a change beside it.
 *
 * @param endedFeatureIds        the blocks the licence stopped holding
 * @param endedSubareas the subareas that went with them
 */
public record BlockEndOperation(
    UUID id,
    List<UUID> endedFeatureIds,
    List<SubareaDetails> endedSubareas
) implements HiddenLicenceOperation, GeospatialLicenceOperation {

  public BlockEndOperation {
    Objects.requireNonNull(id, "id must not be null");
    endedFeatureIds = endedFeatureIds == null ? List.of() : List.copyOf(endedFeatureIds);
    endedSubareas = endedSubareas == null ? List.of() : List.copyOf(endedSubareas);
  }

  public BlockEndOperation(List<UUID> endedFeatureIds, List<SubareaDetails> endedSubareas) {
    this(UUID.randomUUID(), endedFeatureIds, endedSubareas);
  }

  @Override
  public String type() {
    return BLOCK_END;
  }

  @Override
  public String displayName() {
    return "Blocks ended";
  }

  @Override
  public PositionValidationError validate(PositionValidationContext positionValidationContext) {
    return null;
  }

  @Override
  public LicencePositionState applyState(LicencePositionState licencePositionState) {
    return licencePositionState.withoutBlocks(endedFeatureIds);
  }

  @Override
  public Set<UUID> featureIds() {
    return Stream.concat(endedFeatureIds.stream(), SubareaDetails.featureIds(endedSubareas).stream())
        .collect(Collectors.toSet());
  }

  public static class Builder {

    private Collection<UUID> endedFeatureIds;
    private Collection<SubareaDetails> endedSubareas;

    public Builder withEndedFeatureIds(Collection<UUID> endedFeatureIds) {
      this.endedFeatureIds = endedFeatureIds;
      return this;
    }

    public Builder withEndedSubareas(Collection<SubareaDetails> endedSubareas) {
      this.endedSubareas = endedSubareas;
      return this;
    }

    public BlockEndOperation build() {
      return new BlockEndOperation(
          endedFeatureIds == null ? List.of() : endedFeatureIds.stream().distinct().toList(),
          endedSubareas == null ? List.of() : endedSubareas.stream().distinct().toList());
    }
  }
}
