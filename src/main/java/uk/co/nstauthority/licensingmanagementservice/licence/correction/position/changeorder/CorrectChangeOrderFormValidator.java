package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changeorder;

import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import uk.co.fivium.formlibrary.validator.string.StringInputValidator;

@Service
public class CorrectChangeOrderFormValidator {

  boolean hasErrors(CorrectChangeOrderForm form, Errors errors, Set<String> allowedMoves) {
    StringInputValidator.builder().validate(form.getChangeMove(), errors);

    var value = form.getChangeMove().getInputValue();
    if (!StringUtils.isBlank(value) && !allowedMoves.contains(value)) {
      errors.rejectValue("changeMove.inputValue", "changeMove.invalid",
          "Select where to move the change to");
    }

    return errors.hasErrors();
  }
}