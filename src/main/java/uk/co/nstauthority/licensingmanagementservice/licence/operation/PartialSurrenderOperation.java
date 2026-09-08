package uk.co.nstauthority.licensingmanagementservice.licence.operation;

import com.fasterxml.jackson.annotation.JsonAlias;
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
 * @param surrenderedFeatureIds The blocks selected to surrender. This is the selection step and may contain blocks that
 *                              have not yet had their surrender detail (type/journey) chosen.
 * @param featureIdToSurrenderDetails The per-block surrender detail, keyed by the original block feature id. A featureId present
 *                                    in {@code surrenderedFeatureIds} but absent here means "surrender type not yet
 *                                    chosen".
 * @param outputFeatureIds The licence blocks held once this surrender has been submitted, and so the input feature set
 *                         for whatever spatial operation comes next. This is the point the surrender journey is
 *                         complete, not the point the correction carrying it is applied, so a surrender staged on an
 *                         earlier position takes effect on the positions after it straight away. Only blocks are
 *                         recorded: the subareas a licence holds follow from the blocks it holds and the subareas' own
 *                         start and end dates, so a surrendered block's subareas remain reachable through the position
 *                         that still held the block.
 */
public record PartialSurrenderOperation(
    UUID id,
    @Nullable LocalDate surrenderDate,
    // aliased so partial surrenders persisted before the rename still deserialize
    @JsonAlias("featureIds") List<UUID> surrenderedFeatureIds,
    Map<UUID, SurrenderDetails> featureIdToSurrenderDetails,
    List<UUID> outputFeatureIds
) implements LicenceOperation {

  // Fixed, as a position only ever carries one partial surrender.
  public static final UUID PARTIAL_SURRENDER_OPERATION_ID = new UUID(0L, 1L);

  public PartialSurrenderOperation {
    Objects.requireNonNull(id, "id must not be null");
    if (CollectionUtils.isEmpty(surrenderedFeatureIds)) {
      throw new IllegalArgumentException("surrenderedFeatureIds must not be null or empty");
    }
    featureIdToSurrenderDetails = featureIdToSurrenderDetails == null ? Map.of() : Map.copyOf(featureIdToSurrenderDetails);
    // operations persisted before output features existed have no such field, so absent reads as "no outputs yet"
    outputFeatureIds = outputFeatureIds == null ? List.of() : List.copyOf(outputFeatureIds);
  }

  public PartialSurrenderOperation(
      @Nullable LocalDate surrenderDate,
      List<UUID> surrenderedFeatureIds,
      @Nullable Map<UUID, SurrenderDetails> featureIdToSurrenderDetails,
      @Nullable List<UUID> outputFeatureIds
  ) {
    this(
        PARTIAL_SURRENDER_OPERATION_ID,
        surrenderDate,
        surrenderedFeatureIds,
        featureIdToSurrenderDetails,
        outputFeatureIds
    );
  }

  /**
   * A surrender that has not yet produced output features, which is how one starts out before it is complete.
   */
  public PartialSurrenderOperation(
      @Nullable LocalDate surrenderDate,
      List<UUID> surrenderedFeatureIds,
      @Nullable Map<UUID, SurrenderDetails> featureIdToSurrenderDetails
  ) {
    this(surrenderDate, surrenderedFeatureIds, featureIdToSurrenderDetails, List.of());
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
      // TODO EPGF-192: Change when criteria for complete partial surrender exists
      return type == BlockSurrenderType.FULL_SURRENDER;
    }
  }

  /**
   * Whether this surrender differs from the one currently live on the position, so a correction of a live change knows
   * whether anything actually needs staging. The command journey id is deliberately excluded: it is recreated whenever a
   * type is (re)chosen, so it never reflects a meaningful change to the surrender itself. The output features are
   * excluded for the same reason: they are derived from the inputs and per-block state compared here.
   */
  public boolean hasUpdateOccurred(PartialSurrenderOperation liveSurrender) {
    return !Set.copyOf(liveSurrender.surrenderedFeatureIds()).equals(Set.copyOf(surrenderedFeatureIds))
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

  /**
   * A surrender type is chosen per block after the surrender itself is staged, so a block that has not reached that
   * step yet has no type to show.
   */
  @Nullable
  public String surrenderTypeDisplayName(UUID featureId) {
    var surrenderDetails = featureIdToSurrenderDetails.get(featureId);
    return surrenderDetails == null ? null : surrenderDetails.type().getDisplayName();
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
    private Collection<UUID> surrenderedFeatureIds;
    private Map<UUID, SurrenderDetails> featureIdToSurrenderDetails;
    private Collection<UUID> outputFeatureIds;

    public Builder withSurrenderDate(@Nullable LocalDate surrenderDate) {
      this.surrenderDate = surrenderDate;
      return this;
    }

    public Builder withSurrenderedFeatureIds(Collection<UUID> surrenderedFeatureIds) {
      this.surrenderedFeatureIds = surrenderedFeatureIds;
      return this;
    }

    public Builder withSurrenderDetails(Map<UUID, SurrenderDetails> featureIdToSurrenderDetails) {
      this.featureIdToSurrenderDetails = featureIdToSurrenderDetails;
      return this;
    }

    public Builder withOutputFeatureIds(Collection<UUID> outputFeatureIds) {
      this.outputFeatureIds = outputFeatureIds;
      return this;
    }

    public PartialSurrenderOperation build() {
      return new PartialSurrenderOperation(
          PARTIAL_SURRENDER_OPERATION_ID,
          surrenderDate,
          surrenderedFeatureIds == null ? List.of() : surrenderedFeatureIds.stream().distinct().toList(),
          featureIdToSurrenderDetails == null ? Map.of() : featureIdToSurrenderDetails,
          outputFeatureIds == null ? List.of() : outputFeatureIds.stream().distinct().toList()
      );
    }
  }
}
