package uk.co.nstauthority.licensingmanagementservice.licence.operation;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.PositionValidationContext;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.PositionValidationError;

/**
 * Operation for when a licence block is ended on a licence.
 *
 * @param endedFeatureIds the blocks the licence stopped holding
 */
public record BlockEndOperation(
    UUID id,
    List<UUID> endedFeatureIds
) implements LicenceOperation {

  public BlockEndOperation {
    Objects.requireNonNull(id, "id must not be null");
    endedFeatureIds = endedFeatureIds == null ? List.of() : List.copyOf(endedFeatureIds);
  }

  public BlockEndOperation(List<UUID> endedFeatureIds) {
    this(UUID.randomUUID(), endedFeatureIds);
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

  public static class Builder {

    private Collection<UUID> endedFeatureIds;

    public Builder withEndedFeatureIds(Collection<UUID> endedFeatureIds) {
      this.endedFeatureIds = endedFeatureIds;
      return this;
    }

    public BlockEndOperation build() {
      return new BlockEndOperation(
          endedFeatureIds == null ? List.of() : endedFeatureIds.stream().distinct().toList());
    }
  }
}
