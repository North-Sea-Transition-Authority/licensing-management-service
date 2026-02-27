package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overview.steward;

import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;

@Service
class AllocateStewardValidator {

  boolean isValid(AllocateStewardForm form, Errors errors, Map<String, String> stewardOptions) {
    if (form.getStewardWuaId() == null) {
      errors.rejectValue("stewardWuaId", "stewardWuaId.required", "Select a steward");
    } else if (!stewardOptions.containsKey(form.getStewardWuaId())) {
      errors.rejectValue("stewardWuaId", "stewardWuaId.invalid", "Select a valid steward");
    }

    return !errors.hasErrors();
  }
}
