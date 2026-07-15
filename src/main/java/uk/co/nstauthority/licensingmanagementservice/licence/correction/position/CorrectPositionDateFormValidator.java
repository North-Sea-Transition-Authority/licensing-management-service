package uk.co.nstauthority.licensingmanagementservice.licence.correction.position;

import java.time.Clock;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import uk.co.fivium.formlibrary.validator.date.ThreeFieldDateInputValidator;

@Service
class CorrectPositionDateFormValidator {

  private final Clock clock;

  CorrectPositionDateFormValidator(Clock clock) {
    this.clock = clock;
  }

  boolean hasErrors(CorrectPositionDateForm form, Errors errors) {

    ThreeFieldDateInputValidator.builder()
        .mustBeBeforeOrEqualTo(LocalDate.now(clock))
        .validate(form.getCorrectPositionDate(), errors);

    return errors.hasErrors();
  }
}