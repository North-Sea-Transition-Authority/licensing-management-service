package uk.co.nstauthority.licensingmanagementservice.licence.operation;

import jakarta.annotation.Nullable;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.apache.commons.collections.CollectionUtils;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.PositionValidationContext;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.PositionValidationError;

/**
 * Partial surrender operation.
 * @param surrenderDate The date of the surrender. This is nullable, as on corrections it will be the same as a selected position.
 * @param featureIds A list of feature IDs that are being surrendered.
 */
public record PartialSurrenderOperation(
    @Nullable LocalDate surrenderDate,
    List<UUID> featureIds
) implements LicenceOperation {

  public PartialSurrenderOperation {
    if (CollectionUtils.isEmpty(featureIds)) {
      throw new IllegalArgumentException("featureIds must not be null or empty");
    }
  }

  @Override
  public String type() {
    return PARTIAL_SURRENDER;
  }

  @Override
  public UUID id() {
    return UUID.randomUUID();
  }

  @Override
  public PositionValidationError validate(PositionValidationContext positionValidationContext) {
    //TODO EPGF-205: identify when a partial surrender results in an invalid licence position
    return null;
  }

  public static class Builder {

    private LocalDate surrenderDate;
    private Collection<UUID> featureIds;

    public Builder withSurrenderDate(@Nullable LocalDate surrenderDate) {
      this.surrenderDate = surrenderDate;
      return this;
    }

    public Builder withFeatureIds(Collection<UUID> featureIds) {
      this.featureIds = featureIds;
      return this;
    }

    public PartialSurrenderOperation build() {
      return new PartialSurrenderOperation(
          surrenderDate,
          featureIds == null ? List.of() : featureIds.stream().distinct().toList()
      );
    }
  }
}
