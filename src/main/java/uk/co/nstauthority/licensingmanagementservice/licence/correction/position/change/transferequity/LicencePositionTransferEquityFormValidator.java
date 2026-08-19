package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.transferequity;

import java.math.BigDecimal;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import uk.co.fivium.formlibrary.validator.decimal.DecimalInputValidator;

@Service
public class LicencePositionTransferEquityFormValidator {

  private static final String TRANSFER_FROM = "transferFrom";

  private static final BigDecimal SMALLEST_TRANSFERABLE_EQUITY = new BigDecimal("0.0000000001");

  public boolean hasErrors(
      LicencePositionTransferEquityForm form,
      Errors errors,
      Map<Integer, BigDecimal> equityHoldings
  ) {
    ValidationUtils.rejectIfEmptyOrWhitespace(errors, TRANSFER_FROM, "transferFrom.required",
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

    validateTransferorHoldsEnoughEquity(form, errors, equityHoldings);

    return errors.hasErrors();
  }

  private void validateTransferorHoldsEnoughEquity(
      LicencePositionTransferEquityForm form,
      Errors errors,
      Map<Integer, BigDecimal> equityHoldings
  ) {
    if (errors.hasFieldErrors(TRANSFER_FROM)
        || errors.hasFieldErrors("equity.inputValue")
        || StringUtils.isBlank(form.getTransferFrom())) {
      return;
    }

    int transferFrom;
    try {
      transferFrom = Integer.parseInt(form.getTransferFrom());
    } catch (NumberFormatException e) {
      return;
    }

    var availableEquity = equityHoldings.getOrDefault(transferFrom, BigDecimal.ZERO);
    var requestedEquity = form.getEquity().getAsBigDecimal().orElse(BigDecimal.ZERO);
    if (requestedEquity.compareTo(availableEquity) > 0) {
      errors.rejectValue(TRANSFER_FROM, "transferFrom.insufficientEquity",
          "This organisation does not hold enough equity to transfer");
    }
  }
}