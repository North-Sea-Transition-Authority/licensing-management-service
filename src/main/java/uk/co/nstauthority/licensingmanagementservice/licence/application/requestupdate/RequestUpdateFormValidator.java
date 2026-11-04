package uk.co.nstauthority.licensingmanagementservice.licence.application.requestupdate;

import java.time.Clock;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import uk.co.fivium.formlibrary.validator.date.ThreeFieldDateInputValidator;

@Service
public class RequestUpdateFormValidator {

  private final Clock clock;

  RequestUpdateFormValidator(Clock clock) {
    this.clock = clock;
  }

  public boolean isValid(RequestUpdateForm form, Errors errors) {
    ThreeFieldDateInputValidator.builder()
        .emptyInputErrorMessage("Enter the date the update is needed by")
        .mustBeAfterDate(LocalDate.now(clock))
        .mustBeAfterDateErrorMessage("The update deadline date must be in the future")
        .validate(form.getUpdateDeadlineDate(), errors);

    ValidationUtils.rejectIfEmptyOrWhitespace(
        errors,
        "updateReason",
        "updateReason.required",
        "Enter the reason an update is needed"
    );

    return !errors.hasErrors();
  }
}
