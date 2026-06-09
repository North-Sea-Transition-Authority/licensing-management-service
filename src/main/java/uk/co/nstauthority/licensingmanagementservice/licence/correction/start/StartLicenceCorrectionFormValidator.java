package uk.co.nstauthority.licensingmanagementservice.licence.correction.start;

import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import uk.co.fivium.formlibrary.validator.string.StringInputValidator;

@Service
public class StartLicenceCorrectionFormValidator {

  boolean hasErrors(StartLicenceCorrectionForm form, Errors errors) {
    StringInputValidator.builder().validate(form.getCorrectionReference(), errors);

    StringInputValidator.builder().validate(form.getReason(), errors);

    return errors.hasErrors();
  }
}
