package uk.co.nstauthority.licensingmanagementservice.licence.operation;

import java.util.Objects;
import java.util.UUID;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.PositionValidationContext;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.PositionValidationError;

public record SubareaOperation(
    UUID id,
    UUID featureId
) implements LicenceOperation {

  public SubareaOperation {
    Objects.requireNonNull(id, "id must not be null");
    Objects.requireNonNull(featureId, "featureId must not be null");
  }

  public SubareaOperation(
      UUID featureId
  ) {
    this(UUID.randomUUID(), featureId);
  }

  @Override
  public String type() {
    return SUBAREA;
  }

  @Override
  public String displayName() {
    return "Subarea change";
  }

  @Override
  public PositionValidationError validate(PositionValidationContext positionValidationContext) {
    //TODO - LMS2-164: identify when a subarea change results in an invalid licence position
    return null;
  }

  public static class Builder {

    private UUID featureId;

    public Builder withFeatureId(UUID featureId) {
      this.featureId = featureId;
      return this;
    }

    public SubareaOperation build() {
      return new SubareaOperation(featureId);
    }
  }
}
