package uk.co.nstauthority.licensingmanagementservice.licence.operation;

import static uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.LicencePositionChangeUtil.NOT_AVAILABLE;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.annotation.Nullable;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.PositionValidationContext;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.PositionValidationError;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeViewContext;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.LicencePositionState;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.PositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.ChangeViewUrls;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.LicencePositionChangeView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.SubareaChangeView;

/**
 * Subarea operation contains information identifying what subareas were changed.
 *
 * @param blockFeatureId   the block whose subareas changed
 * @param replacedSubareas the subarea versions that ended
 * @param outputSubareas   the subarea versions that took their place
 * @param commandJourneyId the journey the subareas are changed through while the change is staged in a correction
 */
public record SubareaOperation(
    UUID id,
    UUID blockFeatureId,
    List<SubareaDetails> replacedSubareas,
    List<SubareaDetails> outputSubareas,
    @Nullable UUID commandJourneyId
) implements VisibleLicenceOperation, GeospatialLicenceOperation {

  public SubareaOperation {
    Objects.requireNonNull(id, "id must not be null");
    Objects.requireNonNull(blockFeatureId, "blockFeatureId must not be null");
    replacedSubareas = replacedSubareas == null ? List.of() : List.copyOf(replacedSubareas);
    outputSubareas = outputSubareas == null ? List.of() : List.copyOf(outputSubareas);
  }

  public SubareaOperation(
      UUID id,
      UUID blockFeatureId,
      List<SubareaDetails> replacedSubareas,
      List<SubareaDetails> outputSubareas
  ) {
    this(id, blockFeatureId, replacedSubareas, outputSubareas, null);
  }

  public SubareaOperation(
      UUID blockFeatureId,
      List<SubareaDetails> replacedSubareas,
      List<SubareaDetails> outputSubareas
  ) {
    this(UUID.randomUUID(), blockFeatureId, replacedSubareas, outputSubareas);
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

  @Override
  public LicencePositionState applyState(LicencePositionState licencePositionState) {
    return licencePositionState
        .withoutSubareas(blockFeatureId, replacedSubareas)
        .withSubareas(blockFeatureId, outputSubareas);
  }

  @Override
  public LicencePositionChangeView getChangeView(
      PositionChange change,
      LicencePositionState previousState,
      @Nullable LocalDate currentPositionDate,
      LicencePositionChangeViewContext context,
      ChangeViewUrls urls
  ) {
    return new SubareaChangeView(
        context.featureNames().getOrDefault(blockFeatureId, NOT_AVAILABLE),
        change.changeType(),
        urls
    );
  }

  @Override
  public OperationRoutes getOperationUrls() {
    return OperationRoutes.none();
  }

  @Override
  public Set<UUID> featureIds() {
    return Stream.of(
            Stream.of(blockFeatureId),
            SubareaDetails.featureIds(replacedSubareas).stream(),
            SubareaDetails.featureIds(outputSubareas).stream()
        )
        .flatMap(Function.identity())
        .collect(Collectors.toSet());
  }

  @JsonIgnore
  public UUID commandJourneyIdOrThrow() {
    if (commandJourneyId == null) {
      throw new IllegalStateException("No command journey started for subarea change %s".formatted(id));
    }
    return commandJourneyId;
  }

  @Override
  public boolean doInputsMatchPreviousStateOutputs(LicencePositionState licencePositionState) {
    return licencePositionState.hasFeatures(List.of(blockFeatureId), replacedSubareas);
  }

  public static class Builder {

    private UUID blockFeatureId;
    private Collection<SubareaDetails> replacedSubareas;
    private Collection<SubareaDetails> outputSubareas;
    private UUID commandJourneyId;

    public Builder withBlockFeatureId(UUID blockFeatureId) {
      this.blockFeatureId = blockFeatureId;
      return this;
    }

    public Builder withReplacedSubareas(Collection<SubareaDetails> replacedSubareas) {
      this.replacedSubareas = replacedSubareas;
      return this;
    }

    public Builder withOutputSubareas(Collection<SubareaDetails> outputSubareas) {
      this.outputSubareas = outputSubareas;
      return this;
    }

    public Builder withCommandJourneyId(UUID commandJourneyId) {
      this.commandJourneyId = commandJourneyId;
      return this;
    }

    public SubareaOperation build() {
      return new SubareaOperation(
          UUID.randomUUID(),
          blockFeatureId,
          replacedSubareas == null ? List.of() : replacedSubareas.stream().distinct().toList(),
          outputSubareas == null ? List.of() : outputSubareas.stream().distinct().toList(),
          commandJourneyId
      );
    }
  }
}
