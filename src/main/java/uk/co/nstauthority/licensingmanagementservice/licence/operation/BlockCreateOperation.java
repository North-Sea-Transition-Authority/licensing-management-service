package uk.co.nstauthority.licensingmanagementservice.licence.operation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
 * @param createdBlockFeatureIdToSubareas the subareas those blocks arrived covered by, keyed by the block each is on
 */
public record BlockCreateOperation(
    UUID id,
    @JsonProperty("featureIds") List<UUID> createdBlockFeatureIds,
    Map<UUID, List<SubareaDetails>> createdBlockFeatureIdToSubareas
) implements HiddenLicenceOperation, GeospatialLicenceOperation {

  public BlockCreateOperation {
    Objects.requireNonNull(id, "id must not be null");
    createdBlockFeatureIds = createdBlockFeatureIds == null ? List.of() : List.copyOf(createdBlockFeatureIds);
    createdBlockFeatureIdToSubareas = createdBlockFeatureIdToSubareas == null
        ? Map.of()
        : createdBlockFeatureIdToSubareas.entrySet().stream()
            .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> List.copyOf(entry.getValue())));
  }

  public BlockCreateOperation(
      List<UUID> createdBlockFeatureIds,
      Map<UUID, List<SubareaDetails>> createdBlockFeatureIdToSubareas
  ) {
    this(UUID.randomUUID(), createdBlockFeatureIds, createdBlockFeatureIdToSubareas);
  }

  @JsonIgnore
  public List<SubareaDetails> createdSubareas() {
    return createdBlockFeatureIdToSubareas.values().stream()
        .flatMap(List::stream)
        .toList();
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

  @Override
  public Set<UUID> featureIds() {
    return Stream.concat(createdBlockFeatureIds.stream(), SubareaDetails.featureIds(createdSubareas()).stream())
        .collect(Collectors.toSet());
  }

  public static class Builder {

    private Collection<UUID> createdBlockFeatureIds;
    private Map<UUID, ? extends Collection<SubareaDetails>> createdBlockFeatureIdToSubareas;

    public Builder withFeatureIds(Collection<UUID> createdBlockFeatureIds) {
      this.createdBlockFeatureIds = createdBlockFeatureIds;
      return this;
    }

    public Builder withCreatedSubareas(
        Map<UUID, ? extends Collection<SubareaDetails>> createdBlockFeatureIdToSubareas
    ) {
      this.createdBlockFeatureIdToSubareas = createdBlockFeatureIdToSubareas;
      return this;
    }

    public BlockCreateOperation build() {
      return new BlockCreateOperation(
          createdBlockFeatureIds == null ? List.of() : createdBlockFeatureIds.stream().distinct().toList(),
          createdBlockFeatureIdToSubareas == null
              ? Map.of()
              : createdBlockFeatureIdToSubareas.entrySet().stream()
                  .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().stream().distinct().toList()))
      );
    }
  }
}
