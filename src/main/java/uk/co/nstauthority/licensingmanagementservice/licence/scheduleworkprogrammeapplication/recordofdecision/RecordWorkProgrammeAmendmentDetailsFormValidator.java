package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision;

import org.apache.commons.lang3.BooleanUtils;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDurationValidationUtil;

@Service
public class RecordWorkProgrammeAmendmentDetailsFormValidator {

  static final String DECISION_REQUIRED_ERROR_MESSAGE = "Select the decision in relation to this activity";
  static final String AMENDMENT_TYPE_REQUIRED_ERROR_MESSAGE = "Select whether the duration or the text is amended";
  static final String AMENDED_TEXT_REQUIRED_ERROR_MESSAGE = "Enter the amended work programme text";

  boolean isValid(RecordWorkProgrammeAmendmentDetailsForm form, Errors errors) {
    if (form.getDecision() == null) {
      errors.rejectValue("decision", "decision.required", DECISION_REQUIRED_ERROR_MESSAGE);
      return false;
    }

    if (form.getDecision() == WorkProgrammeAmendmentDecision.AMEND) {
      validateAmendment(form, errors);
    }

    return !errors.hasErrors();
  }

  private void validateAmendment(RecordWorkProgrammeAmendmentDetailsForm form, Errors errors) {
    var amendDuration = BooleanUtils.isTrue(form.getAmendDuration());
    var amendText = BooleanUtils.isTrue(form.getAmendText());

    if (!amendDuration && !amendText) {
      errors.rejectValue("amendDuration", "amendDuration.required", AMENDMENT_TYPE_REQUIRED_ERROR_MESSAGE);
      return;
    }

    if (amendDuration) {
      ThreeFieldDurationValidationUtil.validate(form.getAmendedDuration(), errors);
    }

    if (amendText) {
      ValidationUtils.rejectIfEmptyOrWhitespace(
          errors,
          "amendedText",
          "amendedText.required",
          AMENDED_TEXT_REQUIRED_ERROR_MESSAGE
      );
    }
  }
}
