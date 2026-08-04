package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision;

import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;

@Service
public class RecordDecisionFormValidator {

  boolean isValid(RecordDecisionForm form, Errors errors) {
    if (form.getExtensionDecision() == null) {
      errors.rejectValue("extensionDecision", "extensionDecision.required",
          "Select whether there is a change to a phase or term duration");
    }
    if (form.getWorkProgrammeDecision() == null) {
      errors.rejectValue("workProgrammeDecision", "workProgrammeDecision.required",
          "Select whether there is a change to a work programme activity");
    }
    return !errors.hasErrors();
  }
}
