package uk.co.nstauthority.licensingmanagementservice.licence.operation;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.PositionValidationContext;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.PositionValidationError;

/**
 * Operation for creating entirely new blocks.
 *
 * <p>A block arrives covered by its subareas, so the two are created together rather than as
 * separate changes at the same position.
 *
 * @param createdBlockFeatureIds   the blocks created. Held under {@code featureIds} in JSON,
 *                                 which is what the payloads written before subareas were carried
 *                                 across call it.
 * @param createdSubareas the subareas those blocks arrived covered by
 */
public record BlockCreateOperation(
    UUID id,
    @JsonProperty("featureIds") List<UUID> createdBlockFeatureIds,
    List<SubareaDetails> createdSubareas
) implements LicenceOperation {

  public BlockCreateOperation {
    Objects.requireNonNull(id, "id must not be null");
    createdBlockFeatureIds = createdBlockFeatureIds == null ? List.of() : List.copyOf(createdBlockFeatureIds);
    createdSubareas = createdSubareas == null ? List.of() : List.copyOf(createdSubareas);
  }

  public BlockCreateOperation(List<UUID> createdBlockFeatureIds, List<SubareaDetails> createdSubareas) {
    this(UUID.randomUUID(), createdBlockFeatureIds, createdSubareas);
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

    private Collection<UUID> createdBlockFeatureIds;
    private Collection<SubareaDetails> createdSubareas;

    public Builder withFeatureIds(Collection<UUID> createdBlockFeatureIds) {
      this.createdBlockFeatureIds = createdBlockFeatureIds;
      return this;
    }

    public Builder withCreatedSubareas(Collection<SubareaDetails> createdSubareas) {
      this.createdSubareas = createdSubareas;
      return this;
    }

    public BlockCreateOperation build() {
      return new BlockCreateOperation(
          createdBlockFeatureIds == null ? List.of() : createdBlockFeatureIds.stream().distinct().toList(),
          createdSubareas == null ? List.of() : createdSubareas.stream().distinct().toList());
    }
  }
}
