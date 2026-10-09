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
 * Subareas leaving a block the licence keeps.
 *
 * @param blockFeatureId the block the subareas sat on
 * @param endedSubareas the subareas ended
 */
public record SubareaEndOperation(
    UUID id,
    UUID blockFeatureId,
    List<SubareaDetails> endedSubareas
) implements HiddenLicenceOperation, GeospatialLicenceOperation {

  public SubareaEndOperation {
    Objects.requireNonNull(id, "id must not be null");
    Objects.requireNonNull(blockFeatureId, "blockFeatureId must not be null");
    endedSubareas = endedSubareas == null ? List.of() : List.copyOf(endedSubareas);
  }

  public SubareaEndOperation(UUID blockFeatureId, List<SubareaDetails> endedSubareas) {
    this(UUID.randomUUID(), blockFeatureId, endedSubareas);
  }

  @Override
  public String type() {
    return SUBAREA_END;
  }

  @Override
  public String displayName() {
    return "Subareas ended";
  }

  @Override
  public PositionValidationError validate(PositionValidationContext positionValidationContext) {
    return null;
  }

  @Override
  public LicencePositionState applyState(LicencePositionState licencePositionState) {
    return licencePositionState.withoutSubareas(blockFeatureId, endedSubareas);
  }

  @Override
  public Set<UUID> featureIds() {
    return Stream.concat(Stream.of(blockFeatureId), SubareaDetails.featureIds(endedSubareas).stream())
        .collect(Collectors.toSet());
  }

  public static class Builder {

    private UUID blockFeatureId;
    private Collection<SubareaDetails> endedSubareas;

    public Builder withBlockFeatureId(UUID blockFeatureId) {
      this.blockFeatureId = blockFeatureId;
      return this;
    }

    public Builder withEndedSubareas(Collection<SubareaDetails> endedSubareas) {
      this.endedSubareas = endedSubareas;
      return this;
    }

    public SubareaEndOperation build() {
      return new SubareaEndOperation(
          blockFeatureId,
          endedSubareas == null ? List.of() : endedSubareas.stream().distinct().toList());
    }
  }
}
