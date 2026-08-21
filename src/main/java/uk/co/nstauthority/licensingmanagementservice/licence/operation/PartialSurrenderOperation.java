package uk.co.nstauthority.licensingmanagementservice.licence.operation;

import jakarta.annotation.Nullable;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.apache.commons.collections.CollectionUtils;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.blocksurrendertype.BlockSurrenderType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.PositionValidationContext;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.PositionValidationError;

/**
 * Partial surrender operation.
 * @param id The operation ID. A position only ever carries one partial surrender, so this is fixed, allowing a staged
 *     correction to be matched back to the live operation it corrects.
 * @param surrenderDate The date of the surrender. This is nullable, as on corrections it will be the same as a selected position.
 * @param featureIds A list of feature IDs that are being surrendered.
 * @param blockSurrenderTypeByFeatureId Maps the id of the licence block to the BlockSurrenderType
 */
public record PartialSurrenderOperation(
    UUID id,
    @Nullable LocalDate surrenderDate,
    List<UUID> featureIds,
    Map<UUID, BlockSurrenderType> blockSurrenderTypeByFeatureId
) implements LicenceOperation {

  // Fixed, as a position only ever carries one partial surrender.
  public static final UUID PARTIAL_SURRENDER_OPERATION_ID = new UUID(0L, 1L);

  public PartialSurrenderOperation {
    Objects.requireNonNull(id, "id must not be null");
    if (CollectionUtils.isEmpty(featureIds)) {
      throw new IllegalArgumentException("featureIds must not be null or empty");
    }
    blockSurrenderTypeByFeatureId = blockSurrenderTypeByFeatureId == null ? Map.of() : Map.copyOf(blockSurrenderTypeByFeatureId);
  }

  public PartialSurrenderOperation(
      @Nullable LocalDate surrenderDate,
      List<UUID> featureIds,
      @Nullable Map<UUID, BlockSurrenderType> blockSurrenderTypeByFeatureId
  ) {
    this(PARTIAL_SURRENDER_OPERATION_ID, surrenderDate, featureIds, blockSurrenderTypeByFeatureId);
  }

  public boolean hasUpdateOccurred(PartialSurrenderOperation liveSurrender) {
    return !Set.copyOf(liveSurrender.featureIds()).equals(Set.copyOf(featureIds))
        || !liveSurrender.blockSurrenderTypeByFeatureId().equals(blockSurrenderTypeByFeatureId);
  }

  @Override
  public String type() {
    return PARTIAL_SURRENDER;
  }

  @Override
  public PositionValidationError validate(PositionValidationContext positionValidationContext) {
    //TODO EPGF-205: identify when a partial surrender results in an invalid licence position
    return null;
  }

  public static class Builder {

    private LocalDate surrenderDate;
    private Collection<UUID> featureIds;
    private Map<UUID, BlockSurrenderType> blockSurrenderTypeByFeatureId;

    public Builder withSurrenderDate(@Nullable LocalDate surrenderDate) {
      this.surrenderDate = surrenderDate;
      return this;
    }

    public Builder withFeatureIds(Collection<UUID> featureIds) {
      this.featureIds = featureIds;
      return this;
    }

    public Builder withBlockSurrenderTypeByFeatureId(Map<UUID, BlockSurrenderType> blockSurrenderTypeByFeatureId) {
      this.blockSurrenderTypeByFeatureId = blockSurrenderTypeByFeatureId;
      return this;
    }

    public PartialSurrenderOperation build() {
      return new PartialSurrenderOperation(
          PARTIAL_SURRENDER_OPERATION_ID,
          surrenderDate,
          featureIds == null ? List.of() : featureIds.stream().distinct().toList(),
          blockSurrenderTypeByFeatureId == null ? Map.of() : blockSurrenderTypeByFeatureId
      );
    }
  }
}
