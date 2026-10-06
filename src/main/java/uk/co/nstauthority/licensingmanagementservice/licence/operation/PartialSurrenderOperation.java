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
import java.util.stream.Stream;
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
 */
public record PartialSurrenderOperation(
    UUID id,
    @Nullable LocalDate surrenderDate,
    // aliased so partial surrenders persisted before the rename still deserialize
    @JsonAlias("featureIds") List<UUID> surrenderedFeatureIds,
    Map<UUID, SurrenderDetails> featureIdToSurrenderDetails
) implements LicenceOperation {

  // Fixed, as a position only ever carries one partial surrender.
  public static final UUID PARTIAL_SURRENDER_OPERATION_ID = new UUID(0L, 1L);

  public PartialSurrenderOperation {
    Objects.requireNonNull(id, "id must not be null");
    if (CollectionUtils.isEmpty(surrenderedFeatureIds)) {
      throw new IllegalArgumentException("surrenderedFeatureIds must not be null or empty");
    }
    featureIdToSurrenderDetails = featureIdToSurrenderDetails == null ? Map.of() : Map.copyOf(featureIdToSurrenderDetails);
  }

  public PartialSurrenderOperation(
      @Nullable LocalDate surrenderDate,
      List<UUID> surrenderedFeatureIds,
      @Nullable Map<UUID, SurrenderDetails> featureIdToSurrenderDetails
  ) {
    this(PARTIAL_SURRENDER_OPERATION_ID, surrenderDate, surrenderedFeatureIds, featureIdToSurrenderDetails);
  }

  /**
   * Per-block surrender detail. A command journey belongs to a correction while it is being made; once a change has
   * been executed its shape is recorded outright in {@code surrenderedFeatureIds} and {@code retainedFeatureIds} and it
   * carries no journey at all.
   *
   * @param type The surrender type for the block.
   * @param commandJourneyId The journey holding the block's split edits while a correction is in progress, and null on
   *                         an executed change.
   * @param surrenderedFeatureIds The active feature ids being surrendered. For a full surrender this is the whole block
   *                              (the input feature); for a partial surrender it is the chosen split parts, and is empty
   *                              until those parts have been selected. Always empty for a surrender carried across from
   *                              PEARS, which records the ground a licence kept and nothing at all for the ground it
   *                              gave up.
   * @param retainedFeatureIds The features the licence kept, recorded outright rather than derived from a journey.
   *                           This is how an executed change holds its shape; a correction in progress leaves it empty
   *                           and works the parts kept out from its journey instead. A surrender carried across from
   *                           PEARS arrives with these already set, the successor blocks it left behind being the one
   *                           thing PEARS records about a surrender's shape.
   * @param retainedFeatureIdToSubareas What the surrender did to each of the block's subareas, keyed by the retained
   *                                    feature it is relative to. Empty for a full surrender, which keeps no part.
   */
  public record SurrenderDetails(
      BlockSurrenderType type,
      @Nullable UUID commandJourneyId,
      List<UUID> surrenderedFeatureIds,
      List<UUID> retainedFeatureIds,
      Map<UUID, List<SubareaSurrenderOutcome>> retainedFeatureIdToSubareas
  ) {
    public SurrenderDetails {
      surrenderedFeatureIds = surrenderedFeatureIds == null ? List.of() : surrenderedFeatureIds;
      // details persisted before retained features existed have no such field, so absent reads as "none recorded"
      retainedFeatureIds = retainedFeatureIds == null ? List.of() : List.copyOf(retainedFeatureIds);
      retainedFeatureIdToSubareas = retainedFeatureIdToSubareas == null
          ? Map.of()
          : retainedFeatureIdToSubareas.entrySet().stream()
              .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> List.copyOf(entry.getValue())));
    }

    public SurrenderDetails(
        BlockSurrenderType type,
        @Nullable UUID commandJourneyId,
        List<UUID> surrenderedFeatureIds,
        List<UUID> retainedFeatureIds
    ) {
      this(type, commandJourneyId, surrenderedFeatureIds, retainedFeatureIds, Map.of());
    }

    public SurrenderDetails(BlockSurrenderType type, @Nullable UUID commandJourneyId, List<UUID> surrenderedFeatureIds) {
      this(type, commandJourneyId, surrenderedFeatureIds, List.of());
    }

    public SurrenderDetails withSubareas(Map<UUID, List<SubareaSurrenderOutcome>> retainedFeatureIdToSubareas) {
      return new SurrenderDetails(
          type,
          commandJourneyId,
          surrenderedFeatureIds,
          retainedFeatureIds,
          retainedFeatureIdToSubareas
      );
    }

    @JsonIgnore
    public List<UUID> subareaFeatureIds() {
      return allSubareaOutcomes()
          .flatMap(outcome -> Stream.of(outcome.subarea(), outcome.croppedSubarea()))
          .filter(Objects::nonNull)
          .map(SubareaDetails::featureId)
          .filter(Objects::nonNull)
          .distinct()
          .toList();
    }

    @JsonIgnore
    public List<UUID> croppedSubareaFeatureIds() {
      return allSubareaOutcomes()
          .map(SubareaSurrenderOutcome::croppedSubarea)
          .filter(Objects::nonNull)
          .map(SubareaDetails::featureId)
          .filter(Objects::nonNull)
          .toList();
    }

    private Stream<SubareaSurrenderOutcome> allSubareaOutcomes() {
      return retainedFeatureIdToSubareas.values().stream().flatMap(List::stream);
    }

    @JsonIgnore
    public boolean hasCommandJourney() {
      return commandJourneyId != null;
    }

    @JsonIgnore
    public UUID commandJourneyIdOrThrow() {
      if (commandJourneyId == null) {
        throw new IllegalStateException("No split journey started for block surrender");
      }
      return commandJourneyId;
    }

    @JsonIgnore
    public boolean isComplete() {
      // TODO EPGF-192: Change when criteria for complete partial surrender exists
      return type == BlockSurrenderType.FULL_SURRENDER;
    }

    /**
     * A block's surrender takes effect once it is complete, or once its shape is recorded outright rather than held
     * on a journey.
     */
    @JsonIgnore
    public boolean takesEffect() {
      return isComplete() || !hasCommandJourney();
    }
  }

  /**
   * A surrender takes effect on the licence only once every block it surrenders does, until then it leaves the licence
   * as it was.
   */
  @JsonIgnore
  public boolean takesEffect() {
    return !surrenderedFeatureIds.isEmpty() && surrenderedFeatureIds.stream()
        .map(featureIdToSurrenderDetails::get)
        .allMatch(surrenderDetails -> surrenderDetails != null && surrenderDetails.takesEffect());
  }

  public boolean hasUpdateOccurred(PartialSurrenderOperation liveSurrender) {
    return !Set.copyOf(liveSurrender.surrenderedFeatureIds()).equals(Set.copyOf(surrenderedFeatureIds))
        || !surrenderStateByFeatureId().equals(liveSurrender.surrenderStateByFeatureId());
  }

  public Map<UUID, SurrenderState> surrenderStateByFeatureId() {
    return featureIdToSurrenderDetails.entrySet().stream()
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            entry -> new SurrenderState(entry.getValue().type(), Set.copyOf(entry.getValue().surrenderedFeatureIds()))));
  }

  public record SurrenderState(BlockSurrenderType type, Set<UUID> surrenderedFeatureIds) {
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

    public PartialSurrenderOperation build() {
      return new PartialSurrenderOperation(
          PARTIAL_SURRENDER_OPERATION_ID,
          surrenderDate,
          surrenderedFeatureIds == null ? List.of() : surrenderedFeatureIds.stream().distinct().toList(),
          featureIdToSurrenderDetails == null ? Map.of() : featureIdToSurrenderDetails
      );
    }
  }
}
