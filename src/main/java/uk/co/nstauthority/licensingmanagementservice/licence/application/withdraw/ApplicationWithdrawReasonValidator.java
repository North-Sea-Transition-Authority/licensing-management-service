package uk.co.nstauthority.licensingmanagementservice.licence.application.withdraw;

import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;

@Service
public class ApplicationWithdrawReasonValidator {

  public boolean isValid(Errors errors) {
    ValidationUtils.rejectIfEmptyOrWhitespace(
        errors,
        "reasonForWithdrawal",
        "reasonForWithdrawal.required",
        "Enter a reason for withdrawing the application"
    );

    return !errors.hasErrors();
  }
}