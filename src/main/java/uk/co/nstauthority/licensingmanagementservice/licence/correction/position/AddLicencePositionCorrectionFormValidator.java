package uk.co.nstauthority.licensingmanagementservice.licence.correction.position;

import java.time.Clock;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import uk.co.fivium.formlibrary.validator.date.ThreeFieldDateInputValidator;
import uk.co.fivium.formlibrary.validator.string.StringInputValidator;

@Service
class AddLicencePositionCorrectionFormValidator {

  private final Clock clock;

  AddLicencePositionCorrectionFormValidator(Clock clock) {
    this.clock = clock;
  }

  boolean hasErrors(AddLicencePositionCorrectionForm form, Errors errors) {
    StringInputValidator.builder().validate(form.getCorrectionReference(), errors);

    ThreeFieldDateInputValidator.builder().mustBeBeforeOrEqualTo(LocalDate.now(clock)).validate(form.getPositionDate(), errors);

    return errors.hasErrors();
  }
}