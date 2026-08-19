package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ResolvedStates;

@Service
public class LicencePositionValidationService {

  public List<PositionValidationError> validate(
      List<ChronologicalPosition> allChronologicalPositions,
      ResolvedStates resolvedStates,
      boolean isCarbonStorage
  ) {
    var positionValidationErrors = new ArrayList<PositionValidationError>();

    var transactionsWithMultipleChanges = isCarbonStorage
        ? transactionIdsWithMultipleBeneficialInterestChanges(allChronologicalPositions)
        : Set.<UUID>of();

    var firstPosition = true;
    for (var chronologicalPosition : allChronologicalPositions) {
      var positionValidationContext = new PositionValidationContext(
          chronologicalPosition,
          resolvedStates.currentState(chronologicalPosition.id()),
          resolvedStates.previousState(chronologicalPosition.id()),
          firstPosition,
          isCarbonStorage
      );

      positionValidationErrors.addAll(chronologicalPosition.validate(positionValidationContext));

      if (chronologicalPosition.beneficialInterestChangeCount() > 0
          && transactionsWithMultipleChanges.contains(chronologicalPosition.transactionId())) {
        positionValidationErrors.add(PositionValidationError.forPosition(
            positionValidationContext,
            EquityPositionRule.SINGLE_CHANGE_PER_TRANSACTION
        ));
      }

      firstPosition = false;
    }

    return positionValidationErrors;
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