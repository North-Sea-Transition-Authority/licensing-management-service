package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.PartialSurrenderValidator;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.UpdateLicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.GeospatialLicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.PartialSurrenderOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.LicencePositionStateResolver;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.LicencePositionState;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.PositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ResolvedStates;

@Service
public class LicencePositionValidationService {

  private final PartialSurrenderValidator partialSurrenderValidator;
  private final SpatialStateValidator spatialStateValidator;

  LicencePositionValidationService(
      PartialSurrenderValidator partialSurrenderValidator,
      SpatialStateValidator spatialStateValidator
  ) {
    this.partialSurrenderValidator = partialSurrenderValidator;
    this.spatialStateValidator = spatialStateValidator;
  }

  public List<PositionValidationError> validate(
      List<ChronologicalPosition> allChronologicalPositions,
      ResolvedStates resolvedStates,
      List<LicencePositionCorrection> positionCorrections,
      boolean isCarbonStorage,
      boolean isExtantLicence
  ) {
    var positionValidationErrors = new ArrayList<PositionValidationError>();

    var transactionsWithMultipleChanges = isCarbonStorage
        ? transactionIdsWithMultipleBeneficialInterestChanges(allChronologicalPositions)
        : Set.<UUID>of();

    var correctedChangeIds = getCorrectedChangeIds(allChronologicalPositions, positionCorrections);
    var hasStagedSpatialChange = hasStagedSpatialChange(
        allChronologicalPositions,
        positionCorrections,
        correctedChangeIds
    );

    var isFirstPosition = true;
    ChronologicalPosition lastPosition = allChronologicalPositions.isEmpty()
        ? null
        : allChronologicalPositions.getLast();
    for (var chronologicalPosition : allChronologicalPositions) {
      var isLastPosition = chronologicalPosition.equals(lastPosition);
      var positionValidationContext = new PositionValidationContext(
          chronologicalPosition,
          resolvedStates.currentState(chronologicalPosition.id()),
          resolvedStates.previousState(chronologicalPosition.id()),
          isFirstPosition,
          isLastPosition,
          isCarbonStorage,
          isExtantLicence
      );

      positionValidationErrors.addAll(chronologicalPosition.validate(positionValidationContext));
      if (hasStagedSpatialChange) {
        positionValidationErrors.addAll(validateSpatialChanges(positionValidationContext, correctedChangeIds));
      }

      if (chronologicalPosition.beneficialInterestChangeCount() > 0
          && transactionsWithMultipleChanges.contains(chronologicalPosition.transactionId())) {
        positionValidationErrors.add(PositionValidationError.forPosition(
            positionValidationContext,
            EquityPositionRule.SINGLE_CHANGE_PER_TRANSACTION
        ));
      }

      isFirstPosition = false;
    }

    return positionValidationErrors;
  }

  private List<PositionValidationError> validateSpatialChanges(
      PositionValidationContext positionValidationContext,
      Set<String> correctedChangeIds
  ) {
    var errors = new ArrayList<PositionValidationError>();

    var stateGoingIntoChange = positionValidationContext.previousState();
    for (var change : positionValidationContext.position().changes()) {
      if (!LicencePositionChangeType.REMOVE_CHANGE.equals(change.changeType())) {
        if (correctedChangeIds.contains(change.changeId())) {
          errors.addAll(validatePartialSurrenders(positionValidationContext, change, stateGoingIntoChange));
        }
        errors.addAll(spatialStateValidator.validate(positionValidationContext, change, stateGoingIntoChange));
      }

      stateGoingIntoChange = LicencePositionStateResolver.applyChange(stateGoingIntoChange, change);
    }

    return errors;
  }

  private List<PositionValidationError> validatePartialSurrenders(
      PositionValidationContext positionValidationContext,
      PositionChange change,
      LicencePositionState stateGoingIntoChange
  ) {
    var errors = new ArrayList<PositionValidationError>();

    for (var operation : change.operations()) {
      if (operation instanceof PartialSurrenderOperation surrender) {
        partialSurrenderValidator
            .validate(surrender, stateGoingIntoChange, positionValidationContext.position().date(), change.changeId())
            .forEach(message -> errors.add(PositionValidationError
                .forOperation(positionValidationContext, LicenceOperation.PARTIAL_SURRENDER, message)
                .withChangeId(change.changeId())));
      }
    }

    return errors;
  }

  private static Set<String> getCorrectedChangeIds(
      List<ChronologicalPosition> chronologicalPositions,
      List<LicencePositionCorrection> positionCorrections
  ) {
    var stagedChangeIds = positionCorrections.stream()
        .filter(correction -> correction.getPayload() != null)
        .flatMap(correction -> correction.getPayload().changes().stream())
        .map(LicencePositionChangeType::changeId)
        .collect(Collectors.toSet());

    var movedPositionIds = positionCorrections.stream()
        .filter(correction -> correction.getPayload() instanceof UpdateLicencePositionPayload payload
            && payload.changesPositionDate())
        .map(correction -> correction.getTargetLicencePosition().getId())
        .collect(Collectors.toSet());

    return chronologicalPositions.stream()
        .flatMap(chronologicalPosition -> chronologicalPosition.changes().stream()
            .filter(change -> movedPositionIds.contains(chronologicalPosition.id())
                || stagedChangeIds.contains(change.changeId())))
        .map(PositionChange::changeId)
        .collect(Collectors.toSet());
  }

  private static boolean hasStagedSpatialChange(
      List<ChronologicalPosition> chronologicalPositions,
      List<LicencePositionCorrection> positionCorrections,
      Set<String> correctedChangeIds
  ) {
    var hasRemovedPosition = positionCorrections.stream()
        .anyMatch(correction -> correction.getChangeType() == LicencePositionCorrectionChangeType.REMOVE_POSITION);

    return chronologicalPositions.stream()
        .flatMap(chronologicalPosition -> chronologicalPosition.changes().stream())
        .filter(change -> hasRemovedPosition || correctedChangeIds.contains(change.changeId()))
        .flatMap(change -> change.operations().stream())
        .anyMatch(GeospatialLicenceOperation.class::isInstance);
  }

  private static Set<UUID> transactionIdsWithMultipleBeneficialInterestChanges(
      List<ChronologicalPosition> chronologicalPositions
  ) {
    return chronologicalPositions.stream()
        .collect(Collectors.groupingBy(
            ChronologicalPosition::transactionId,
            Collectors.summingLong(ChronologicalPosition::beneficialInterestChangeCount)
        ))
        .entrySet().stream()
        .filter(entry -> entry.getValue() > 1)
        .map(Map.Entry::getKey)
        .collect(Collectors.toSet());
  }
}