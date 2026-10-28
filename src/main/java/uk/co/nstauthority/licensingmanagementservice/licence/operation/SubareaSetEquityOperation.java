package uk.co.nstauthority.licensingmanagementservice.licence.operation;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import static uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.LicencePositionChangeUtil.NOT_AVAILABLE;

import jakarta.annotation.Nullable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.AddPositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.subareaequity.LicencePositionSubareaSetEquityController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.PositionValidationContext;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.PositionValidationError;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.SubareaEquityPositionRule;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeViewContext;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.ChangeUrlTarget;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.LicencePositionState;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.PositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.ChangeViewUrls;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.LicencePositionChangeView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.SetEquityRow;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.SubareaEquityChangeView;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

/**
 * The beneficial interest one organisation holds in a single subarea.
 *
 * @param blockFeatureId the block the subarea sits on
 * @param subarea        the subarea the beneficial interest is held in
 * @param transferTo     the organisation holding the beneficial interest
 * @param equity         the percentage held
 */
public record SubareaSetEquityOperation(
    UUID blockFeatureId,
    SubareaDetails subarea,
    Integer transferTo,
    BigDecimal equity
) implements VisibleLicenceOperation, GeospatialLicenceOperation, EquitySetterOperation {

  public SubareaSetEquityOperation {
    Objects.requireNonNull(blockFeatureId, "blockFeatureId must not be null");
    Objects.requireNonNull(subarea, "subarea must not be null");
    Objects.requireNonNull(transferTo, "transferTo must not be null");
    Objects.requireNonNull(equity, "equity must not be null");
  }

  @Override
  public String type() {
    return SUBAREA_SET_EQUITY;
  }

  @Override
  public String displayName() {
    return "Subarea set equity";
  }

  @Override
  public UUID id() {
    return UUID.randomUUID();
  }

  @Override
  public Set<Integer> organisationUnitIds() {
    return Set.of(transferTo);
  }

  @Override
  public Set<UUID> featureIds() {
    return Set.of(blockFeatureId);
  }

  @Override
  public boolean doInputsMatchPreviousStateOutputs(LicencePositionState licencePositionState) {
    return licencePositionState.hasFeatures(Collections.singleton(blockFeatureId), Collections.singleton(subarea));
  }

  @Override
  public PositionValidationError validate(PositionValidationContext positionValidationContext) {
    if (!AddPositionChangeType.SUBAREA_SET_EQUITY.isAvailableFor(positionValidationContext.licenceType())) {
      return PositionValidationError.forPosition(
          positionValidationContext,
          SubareaEquityPositionRule.PRODUCTION_LICENCE_ONLY
      );
    }
    return null;
  }

  @Override
  public LicencePositionState applyState(LicencePositionState licencePositionState) {
    return licencePositionState.withSubareaEquity(blockFeatureId, subarea, transferTo, equity);
  }

  public SubareaSetEquityOperation withSubarea(SubareaDetails subarea) {
    return new SubareaSetEquityOperation(blockFeatureId, subarea, transferTo, equity);
  }

  @Override
  public LicencePositionChangeView getChangeView(
      PositionChange change,
      LicencePositionState previousState,
      @Nullable LocalDate currentPositionDate,
      LicencePositionChangeViewContext context,
      ChangeViewUrls urls
  ) {
    return new SubareaEquityChangeView(
        subareaLabel(context.featureNames()),
        List.of(new SetEquityRow(context.organisationNames().getOrDefault(transferTo, NOT_AVAILABLE), equity)),
        change.changeType(),
        urls
    );
  }

  @Override
  public OperationRoutes getOperationUrls() {
    var subareaKey = subareaKey();
    return new OperationRoutes() {
      @Override
      public String correct(ChangeUrlTarget target) {
        if (!OperationRoutes.isStagedEquityChange(target)) {
          return null;
        }
        return target.addedPosition()
            ? ReverseRouter.route(on(LicencePositionSubareaSetEquityController.class)
            .renderSummaryForAddedPosition(target.correction(), target.positionCorrection(), subareaKey))
            : ReverseRouter.route(on(LicencePositionSubareaSetEquityController.class)
            .renderSummaryForExecutedPosition(target.correction(), target.licencePosition(), subareaKey));
      }

      @Override
      public String remove(ChangeUrlTarget target) {
        return OperationRoutes.removeEquityChange(target);
      }

      @Override
      public String undo(ChangeUrlTarget target) {
        return OperationRoutes.undoEquityChange(target);
      }
    };
  }

  public String subareaKey() {
    return subarea.keyForBlockFeatureId(blockFeatureId);
  }

  public String subareaLabel(Map<UUID, String> featureNamesById) {
    return subarea.labelForBlockName(featureNamesById.get(blockFeatureId));
  }

  public static class Builder {

    private UUID blockFeatureId;
    private SubareaDetails subarea;
    private Integer transferTo;
    private BigDecimal equity;

    public Builder withBlockFeatureId(UUID blockFeatureId) {
      this.blockFeatureId = blockFeatureId;
      return this;
    }

    public Builder withSubarea(SubareaDetails subarea) {
      this.subarea = subarea;
      return this;
    }

    public Builder withTransferTo(Integer transferTo) {
      this.transferTo = transferTo;
      return this;
    }

    public Builder withEquity(BigDecimal equity) {
      this.equity = equity;
      return this;
    }

    public SubareaSetEquityOperation build() {
      return new SubareaSetEquityOperation(blockFeatureId, subarea, transferTo, equity);
    }
  }
}