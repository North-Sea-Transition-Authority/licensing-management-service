package uk.co.nstauthority.licensingmanagementservice.licence.operation;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import static uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.LicencePositionChangeUtil.UNKNOWN_BLOCK;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.annotation.Nullable;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.action.ChangeActionHandler;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.subarea.SubareaChangeActionHandler;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.subarea.review.SubareaChangeReviewController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.subarea.review.SubareaChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.PositionValidationContext;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.PositionValidationError;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.SubareaPositionRule;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeViewContext;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.ChangeUrlTarget;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.LicencePositionState;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.PositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.ChangeViewUrls;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.LicencePositionChangeView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.SubareaChangeView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.feature.FeatureSummary;
import uk.co.nstauthority.licensingmanagementservice.licence.position.feature.LicenceBlockFeatureUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.subareachange.SubareaChangeSummaryController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

/**
 * Subarea operation contains information identifying what subareas were changed.
 *
 * @param blockFeatureId   the block whose subareas changed
 * @param transfers        which subarea versions ended and which took their place
 * @param commandJourneyId the journey the subareas are changed through while the change is staged in a correction.
 *                         Live operations do not have a command journey.
 */
public record SubareaOperation(
    UUID id,
    UUID blockFeatureId,
    List<SubareaTransfer> transfers,
    @Nullable UUID commandJourneyId
) implements VisibleLicenceOperation, GeospatialLicenceOperation {

  public SubareaOperation {
    Objects.requireNonNull(id, "id must not be null");
    Objects.requireNonNull(blockFeatureId, "blockFeatureId must not be null");
    transfers = transfers == null ? List.of() : List.copyOf(transfers);
  }

  public SubareaOperation(
      UUID id,
      UUID blockFeatureId,
      List<SubareaDetails> replacedSubareas,
      List<SubareaDetails> outputSubareas
  ) {
    this(id, blockFeatureId, singleTransfer(new SubareaTransfer(replacedSubareas, outputSubareas)), null);
  }

  public SubareaOperation(
      UUID blockFeatureId,
      List<SubareaDetails> replacedSubareas,
      List<SubareaDetails> outputSubareas
  ) {
    this(UUID.randomUUID(), blockFeatureId, replacedSubareas, outputSubareas);
  }

  @JsonIgnore
  public List<SubareaDetails> replacedSubareas() {
    return transfers.stream()
        .flatMap(transfer -> transfer.replacedSubareas().stream())
        .distinct()
        .toList();
  }

  @JsonIgnore
  public List<SubareaDetails> outputSubareas() {
    return transfers.stream()
        .flatMap(transfer -> transfer.outputSubareas().stream())
        .distinct()
        .toList();
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
  public Class<? extends ChangeActionHandler> changeActionHandlerType() {
    return SubareaChangeActionHandler.class;
  }

  @Override
  public PositionValidationError validate(PositionValidationContext positionValidationContext) {
    if (transfers.isEmpty()) {
      return PositionValidationError.forOperation(
          positionValidationContext,
          type(),
          SubareaPositionRule.NO_CHANGE.getMessage()
      );
    }
    return null;
  }

  @Override
  public LicencePositionState applyState(LicencePositionState licencePositionState) {
    return licencePositionState
        .withoutSubareas(blockFeatureId, replacedSubareas())
        .withSubareas(blockFeatureId, outputSubareas());
  }

  @Override
  public LicencePositionChangeView getChangeView(
      PositionChange change,
      LicencePositionState previousState,
      @Nullable LocalDate currentPositionDate,
      LicencePositionChangeViewContext context,
      ChangeViewUrls urls
  ) {
    var featuresById = context.featuresById();

    return new SubareaChangeView(
        Optional.ofNullable(featuresById.get(blockFeatureId)).map(FeatureSummary::name).orElse(UNKNOWN_BLOCK),
        getSubareaRows(featuresById),
        change.changeType(),
        urls
    );
  }

  private List<SubareaChangeView.SubareaRow> getSubareaRows(Map<UUID, FeatureSummary> featuresById) {
    return transfers.stream()
        .flatMap(transfer -> transfer.replacedSubareas().isEmpty()
            ? Stream.of(getSubareaRow(null, transfer, featuresById))
            : transfer.replacedSubareas().stream()
            .map(replaced -> getSubareaRow(replaced, transfer, featuresById)))
        .sorted(Comparator.comparing(SubareaChangeView.SubareaRow::inputSubarea))
        .toList();
  }

  /**
   * Get the row detailing what happened in the change for the replaced subarea.
   * @param replacedSubarea null when a subarea is SET
   */
  private SubareaChangeView.SubareaRow getSubareaRow(
      @Nullable SubareaDetails replacedSubarea,
      SubareaTransfer transfer,
      Map<UUID, FeatureSummary> featuresById
  ) {
    var outputs = transfer.outputSubareas().stream()
        .map(output -> subareaLabelWithArea(output, featuresById))
        .collect(Collectors.joining(", "));
    return new SubareaChangeView.SubareaRow(
        replacedSubarea == null ? "" : subareaLabelWithArea(replacedSubarea, featuresById),
        SubareaChangeType.getLiveChangeType(transfer).toTag(),
        outputs
    );
  }

  private String subareaLabelWithArea(SubareaDetails subarea, Map<UUID, FeatureSummary> featuresById) {
    var name = subarea.displayName();
    return Optional.ofNullable(subarea.featureId())
        .map(featuresById::get)
        .map(FeatureSummary::area)
        .map(LicenceBlockFeatureUtil::formatArea)
        .map(area -> "%s (%s)".formatted(name, area))
        .orElse(name);
  }

  @Override
  public OperationRoutes getOperationUrls() {
    return new OperationRoutes() {
      @Override
      public String correct(ChangeUrlTarget target) {
        return switch (target.state()) {
          case ADDED, OPERATIONS_UPDATED -> ReverseRouter.route(on(SubareaChangeReviewController.class)
              .renderStagedReview(target.correction(), target.positionStagedPayload(), blockFeatureId, null));
          case LIVE, ORDER_UPDATED -> ReverseRouter.route(on(SubareaChangeReviewController.class)
              .renderLiveReview(target.correction(), target.licencePosition(), target.changeEntity(), null));
          case REMOVED -> null;
        };
      }

      @Override
      public String view(ChangeUrlTarget target) {
        if (!target.inCorrection()) {
          var licencePosition = target.licencePosition();
          return ReverseRouter.route(on(SubareaChangeSummaryController.class).renderSubareaChangeSummary(
              licencePosition.getLicence(), licencePosition, target.changeEntity(), null));
        }
        if (target.addedPosition()) {
          return stagedReviewAndSubmit(target);
        }
        return switch (target.state()) {
          case ADDED, OPERATIONS_UPDATED -> stagedReviewAndSubmit(target);
          case LIVE, ORDER_UPDATED -> ReverseRouter.route(on(SubareaChangeReviewController.class)
              .renderLiveReview(
                  target.correction(), target.licencePosition(), target.changeEntity(), null));
          case REMOVED -> null;
        };
      }

      private String stagedReviewAndSubmit(ChangeUrlTarget target) {
        return ReverseRouter.route(on(SubareaChangeReviewController.class)
            .renderStagedReview(target.correction(), target.positionStagedPayload(), blockFeatureId, null));
      }
    };
  }

  @Override
  public Set<UUID> featureIds() {
    return Stream.of(
            Stream.of(blockFeatureId),
            SubareaDetails.featureIds(replacedSubareas()).stream(),
            SubareaDetails.featureIds(outputSubareas()).stream()
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
    return licencePositionState.hasFeatures(List.of(blockFeatureId), replacedSubareas());
  }

  private static List<SubareaTransfer> singleTransfer(SubareaTransfer transfer) {
    return transfer.replacedSubareas().isEmpty() && transfer.outputSubareas().isEmpty()
        ? List.of()
        : List.of(transfer);
  }

  public static class Builder {

    private UUID blockFeatureId;
    private List<SubareaTransfer> transfers;
    private UUID commandJourneyId;

    public Builder withBlockFeatureId(UUID blockFeatureId) {
      this.blockFeatureId = blockFeatureId;
      return this;
    }

    public Builder withTransfers(List<SubareaTransfer> transfers) {
      this.transfers = transfers;
      return this;
    }

    public Builder withCommandJourneyId(UUID commandJourneyId) {
      this.commandJourneyId = commandJourneyId;
      return this;
    }

    public SubareaOperation build() {
      return new SubareaOperation(UUID.randomUUID(), blockFeatureId, transfers, commandJourneyId);
    }
  }
}
