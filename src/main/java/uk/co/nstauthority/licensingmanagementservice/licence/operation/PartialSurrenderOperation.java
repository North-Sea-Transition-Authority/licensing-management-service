package uk.co.nstauthority.licensingmanagementservice.licence.operation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.annotation.Nullable;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.blocksurrendertype.BlockSurrenderType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.PositionValidationContext;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.PositionValidationError;

/**
 * Partial surrender operation.
 *
 * @param id The operation ID. A position only ever carries one partial surrender, so this is fixed, allowing a staged
 *     correction to be matched back to the live operation it corrects.
 * @param surrenderDate The date of the surrender. This is nullable, as on corrections it will be the same as a selected
 *                      position.
 * @param featureIds The blocks selected to surrender. This is the selection step and may contain blocks that have not
 *                   yet had their surrender detail (type/journey) chosen.
 * @param featureIdToSurrenderDetails The per-block surrender detail, keyed by the original block feature id. A featureId present
 *                                    in {@code featureIds} but absent here means "surrender type not yet chosen".
 */
public record PartialSurrenderOperation(
    UUID id,
    @Nullable LocalDate surrenderDate,
    List<UUID> featureIds,
    Map<UUID, SurrenderDetails> featureIdToSurrenderDetails
) implements LicenceOperation {

  // Fixed, as a position only ever carries one partial surrender.
  public static final UUID PARTIAL_SURRENDER_OPERATION_ID = new UUID(0L, 1L);

  public PartialSurrenderOperation {
    Objects.requireNonNull(id, "id must not be null");
    if (CollectionUtils.isEmpty(featureIds)) {
      throw new IllegalArgumentException("featureIds must not be null or empty");
    }
    featureIdToSurrenderDetails = featureIdToSurrenderDetails == null ? Map.of() : Map.copyOf(featureIdToSurrenderDetails);
  }

  public PartialSurrenderOperation(
      @Nullable LocalDate surrenderDate,
      List<UUID> featureIds,
      @Nullable Map<UUID, SurrenderDetails> featureIdToSurrenderDetails
  ) {
    this(PARTIAL_SURRENDER_OPERATION_ID, surrenderDate, featureIds, featureIdToSurrenderDetails);
  }

  /**
   * Per-block surrender detail. A command journey is always present (created for both full and partial surrenders) so
   * that downstream processing is uniform: every block is a journey plus the active features being surrendered.
   *
   * @param type The surrender type for the block.
   * @param commandJourneyId The journey capturing the block's split edits. For a full surrender this journey has no
   *                         splits, so its active feature is the input block itself.
   * @param surrenderedFeatureIds The active feature ids being surrendered. For a full surrender this is the whole block
   *                              (the input feature); for a partial surrender it is the chosen split parts, and is empty
   *                              until those parts have been selected.
   */
  public record SurrenderDetails(
      BlockSurrenderType type,
      UUID commandJourneyId,
      List<UUID> surrenderedFeatureIds
  ) {
    public SurrenderDetails {
      surrenderedFeatureIds = surrenderedFeatureIds == null ? List.of() : surrenderedFeatureIds;
    }

    @JsonIgnore
    public boolean isComplete() {
      // TODO: Change when criteria for complete partial surrender exists
      return type == BlockSurrenderType.FULL_SURRENDER;
    }
  }

  /**
   * Whether this surrender differs from the one currently live on the position, so a correction of a live change knows
   * whether anything actually needs staging. The command journey id is deliberately excluded: it is recreated whenever a
   * type is (re)chosen, so it never reflects a meaningful change to the surrender itself.
   */
  public boolean hasUpdateOccurred(PartialSurrenderOperation liveSurrender) {
    return !Set.copyOf(liveSurrender.featureIds()).equals(Set.copyOf(featureIds))
        || !surrenderStateByFeatureId().equals(liveSurrender.surrenderStateByFeatureId());
  }

  private Map<UUID, SurrenderState> surrenderStateByFeatureId() {
    return featureIdToSurrenderDetails.entrySet().stream()
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            entry -> new SurrenderState(entry.getValue().type(), Set.copyOf(entry.getValue().surrenderedFeatureIds()))));
  }

  private record SurrenderState(BlockSurrenderType type, Set<UUID> surrenderedFeatureIds) {
  }

  @Override
  public String type() {
    return PARTIAL_SURRENDER;
  }

  @Override
  public String displayName() {
    return "Partial surrender";
  }

  @Override
  public PositionValidationError validate(PositionValidationContext positionValidationContext) {
    //TODO EPGF-205: identify when a partial surrender results in an invalid licence position
    return null;
  }

  public static class Builder {

    private LocalDate surrenderDate;
    private Collection<UUID> featureIds;
    private Map<UUID, SurrenderDetails> featureIdToSurrenderDetails;

    public Builder withSurrenderDate(@Nullable LocalDate surrenderDate) {
      this.surrenderDate = surrenderDate;
      return this;
    }

    public Builder withFeatureIds(Collection<UUID> featureIds) {
      this.featureIds = featureIds;
      return this;
    }

    public Builder withSurrenderDetails(Map<UUID, SurrenderDetails> featureIdToSurrenderDetails) {
      this.featureIdToSurrenderDetails = featureIdToSurrenderDetails;
      return this;
    }

    public PartialSurrenderOperation build() {
      return new PartialSurrenderOperation(
          PARTIAL_SURRENDER_OPERATION_ID,
          surrenderDate,
          featureIds == null ? List.of() : featureIds.stream().distinct().toList(),
          featureIdToSurrenderDetails == null ? Map.of() : featureIdToSurrenderDetails
      );
    }
  }
}
