package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation;

import java.util.ArrayList;
import java.util.List;
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

    var firstPosition = true;
    for (var chronologicalPosition : allChronologicalPositions) {
      var previousState = resolvedStates.previousState(chronologicalPosition.id());

      positionValidationErrors.addAll(chronologicalPosition.validate(
          new PositionValidationContext(
              chronologicalPosition,
              previousState,
              firstPosition,
              isCarbonStorage
          )
      ));

      firstPosition = false;
    }

    return positionValidationErrors;
  }
}
