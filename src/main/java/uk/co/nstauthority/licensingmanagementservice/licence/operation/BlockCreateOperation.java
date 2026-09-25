package uk.co.nstauthority.licensingmanagementservice.licence.operation;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.PositionValidationContext;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.PositionValidationError;

/**
 * Operation for creating entirely new blocks.
 *
 * @param featureIds the blocks created.
 *
 */
public record BlockCreateOperation(
    UUID id,
    List<UUID> featureIds
) implements LicenceOperation {

  public BlockCreateOperation {
    Objects.requireNonNull(id, "id must not be null");
    featureIds = featureIds == null ? List.of() : List.copyOf(featureIds);
  }

  public BlockCreateOperation(List<UUID> featureIds) {
    this(UUID.randomUUID(), featureIds);
  }

  @Override
  public String type() {
    return BLOCK_CREATE;
  }

  @Override
  public String displayName() {
    return "Blocks created";
  }

  @Override
  public PositionValidationError validate(PositionValidationContext positionValidationContext) {
    return null;
  }

  public static class Builder {

    private Collection<UUID> featureIds;

    public Builder withFeatureIds(Collection<UUID> featureIds) {
      this.featureIds = featureIds;
      return this;
    }

    public BlockCreateOperation build() {
      return new BlockCreateOperation(
          featureIds == null ? List.of() : featureIds.stream().distinct().toList());
    }
  }
}
