package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.GeospatialLicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.LicencePositionState;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.PositionChange;

@Service
public class SpatialStateValidator {

  static final String INPUTS_NOT_HELD_ERROR = "%s must only occur on blocks and subareas that exist on that position";

  public List<PositionValidationError> validate(
      PositionValidationContext positionValidationContext,
      PositionChange change,
      LicencePositionState stateGoingIntoChange
  ) {
    var errors = new ArrayList<PositionValidationError>();

    var state = stateGoingIntoChange;
    for (var operation : change.operations()) {
      if (operation instanceof GeospatialLicenceOperation geospatialOperation
          && !geospatialOperation.doInputsMatchPreviousStateOutputs(state)) {
        errors.add(PositionValidationError
            .forOperation(positionValidationContext, operation.type(), INPUTS_NOT_HELD_ERROR.formatted(operation.displayName()))
            .withChangeId(change.changeId()));
      }
      state = operation.applyState(state);
    }

    return errors;
  }
}
