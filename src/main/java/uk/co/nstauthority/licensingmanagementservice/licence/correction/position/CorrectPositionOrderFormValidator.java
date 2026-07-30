package uk.co.nstauthority.licensingmanagementservice.licence.correction.position;

import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import uk.co.fivium.formlibrary.validator.string.StringInputValidator;

@Service
public class CorrectPositionOrderFormValidator {

  boolean hasErrors(CorrectPositionOrderForm form, Errors errors, Set<String> allowedMoves) {
    StringInputValidator.builder().validate(form.getPositionMove(), errors);

    var value = form.getPositionMove().getInputValue();
    if (!StringUtils.isBlank(value) && !allowedMoves.contains(value)) {
      errors.rejectValue("positionMove.inputValue", "positionMove.invalid",
          "Select where to move the position to");
    }

    return errors.hasErrors();
  }
}