package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.setequity;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import uk.co.fivium.formlibrary.validator.decimal.DecimalInputValidator;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SetEquityOperation;

@Service
public class LicencePositionSetEquityFormValidator {

  public boolean hasErrors(
      LicencePositionSetEquityForm form,
      Errors errors,
      List<SetEquityOperation> existingOperations
  ) {
    ValidationUtils.rejectIfEmptyOrWhitespace(errors, "transferTo", "transferTo.required",
        "Select the organisation equity is being allocated to");

    if (!errors.hasFieldErrors("transferTo")) {
      try {
        var transferTo = Integer.parseInt(form.getTransferTo());
        if (existingOperations.stream().anyMatch(operation -> operation.transferTo().equals(transferTo))) {
          errors.rejectValue("transferTo", "transferTo.duplicate",
              "This organisation has already been added");
        }
      } catch (NumberFormatException e) {
        errors.rejectValue("transferTo", "transferTo.invalid",
            "Select the organisation equity is being allocated to");
      }
    }

    DecimalInputValidator.builder()
        .emptyInputErrorMessage("Enter the equity amount this organisation should have as a percentage")
        .invalidInputErrorMessage("Equity amount must be a number")
        .mustBeMoreThanOrEqualTo(BigDecimal.ZERO)
        .mustBeMoreThanOrEqualToErrorMessage("Equity amount must be 0% or more")
        .mustBeLessThanOrEqualTo(new BigDecimal("100"))
        .mustHaveNoMoreThanDecimalPlaces(10)
        .mustHaveNoMoreThanDecimalPlacesErrorMessage("Equity amount must have 10 decimal places or fewer")
        .mustBeLessThanOrEqualToErrorMessage("Equity amount must be 100% or less")
        .validate(form.getEquity(), errors);

    return errors.hasErrors();
  }
}