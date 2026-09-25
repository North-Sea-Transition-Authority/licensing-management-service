package uk.co.nstauthority.licensingmanagementservice.licence.operation;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.PositionValidationContext;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.PositionValidationError;

/**
 * Subareas added to a block the licence already holds.
 *
 * @param blockFeatureId  the block the subareas were added to
 * @param createdSubareas the subareas created
 */
public record SubareaCreateOperation(
    UUID id,
    UUID blockFeatureId,
    List<SubareaDetails> createdSubareas
) implements LicenceOperation {

  public SubareaCreateOperation {
    Objects.requireNonNull(id, "id must not be null");
    Objects.requireNonNull(blockFeatureId, "blockFeatureId must not be null");
    createdSubareas = createdSubareas == null ? List.of() : List.copyOf(createdSubareas);
  }

  public SubareaCreateOperation(UUID blockFeatureId, List<SubareaDetails> createdSubareas) {
    this(UUID.randomUUID(), blockFeatureId, createdSubareas);
  }

  @Override
  public String type() {
    return SUBAREA_CREATE;
  }

  @Override
  public String displayName() {
    return "Subareas created";
  }

  @Override
  public PositionValidationError validate(PositionValidationContext positionValidationContext) {
    return null;
  }

  public static class Builder {

    private UUID blockFeatureId;
    private Collection<SubareaDetails> createdSubareas;

    public Builder withBlockFeatureId(UUID blockFeatureId) {
      this.blockFeatureId = blockFeatureId;
      return this;
    }

    public Builder withCreatedSubareas(Collection<SubareaDetails> createdSubareas) {
      this.createdSubareas = createdSubareas;
      return this;
    }

    public SubareaCreateOperation build() {
      return new SubareaCreateOperation(
          blockFeatureId,
          createdSubareas == null ? List.of() : createdSubareas.stream().distinct().toList());
    }
  }
}
