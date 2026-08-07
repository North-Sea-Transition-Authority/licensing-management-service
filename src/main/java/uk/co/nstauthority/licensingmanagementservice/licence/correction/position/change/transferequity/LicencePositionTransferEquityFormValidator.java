package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.transferequity;

import java.math.BigDecimal;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import uk.co.fivium.formlibrary.validator.decimal.DecimalInputValidator;

@Service
public class LicencePositionTransferEquityFormValidator {

  private static final BigDecimal SMALLEST_TRANSFERABLE_EQUITY = new BigDecimal("0.0000000001");

  public boolean hasErrors(
      LicencePositionTransferEquityForm form,
      Errors errors
  ) {
    ValidationUtils.rejectIfEmptyOrWhitespace(errors, "transferFrom", "transferFrom.required",
        "Select the organisation equity is being transferred from");
    ValidationUtils.rejectIfEmptyOrWhitespace(errors, "transferTo", "transferTo.required",
        "Select the organisation equity is being transferred to");

    if (StringUtils.isNotBlank(form.getTransferFrom()) && form.getTransferFrom().equals(form.getTransferTo())) {
      errors.rejectValue("transferTo", "transferTo.sameAsTransferFrom",
          "The organisation equity is transferred to must be different from the organisation it is transferred from");
    }

    DecimalInputValidator.builder()
        .emptyInputErrorMessage("Enter the equity amount being transferred as a percentage")
        .invalidInputErrorMessage("Equity amount must be a number")
        .mustBeMoreThanOrEqualTo(SMALLEST_TRANSFERABLE_EQUITY)
        .mustBeMoreThanOrEqualToErrorMessage("Equity amount must be more than 0%")
        .mustBeLessThanOrEqualTo(new BigDecimal("100"))
        .mustBeLessThanOrEqualToErrorMessage("Equity amount must be 100% or less")
        .mustHaveNoMoreThanDecimalPlaces(10)
        .mustHaveNoMoreThanDecimalPlacesErrorMessage("Equity amount must have 10 decimal places or fewer")
        .validate(form.getEquity(), errors);

    return errors.hasErrors();
  }
}