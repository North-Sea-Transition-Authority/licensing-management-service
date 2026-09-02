package uk.co.nstauthority.licensingmanagementservice.licence.correction.update;

import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import uk.co.fivium.formlibrary.validator.string.StringInputValidator;

@Service
public class UpdateCorrectionGeneralDetailsFormValidator {

  boolean hasErrors(UpdateCorrectionGeneralDetailsForm form, Errors errors, Map<String, String> allocatableUsers) {
    StringInputValidator.builder().validate(form.getCorrectionReference(), errors);

    StringInputValidator.builder().validate(form.getReason(), errors);

    if (form.getAllocatedToWuaId() == null) {
      errors.rejectValue(
          "allocatedToWuaId",
          "allocatedToWuaId.required",
          "Select the user to allocate this correction to"
      );
    } else if (!allocatableUsers.containsKey(form.getAllocatedToWuaId())) {
      errors.rejectValue(
          "allocatedToWuaId",
          "allocatedToWuaId.invalid",
          "Select a valid user to allocate this correction to"
      );
    }

    return errors.hasErrors();
  }
}