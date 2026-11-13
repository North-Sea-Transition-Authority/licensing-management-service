package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.transferequity;

import java.math.BigDecimal;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import uk.co.fivium.formlibrary.validator.decimal.DecimalInputValidator;

@Service
public class LicencePositionTransferEquityFormValidator {

  private static final String TRANSFER_FROM = "transferFrom";
  private static final String TRANSFER_TO = "transferTo";

  private static final BigDecimal SMALLEST_TRANSFERABLE_EQUITY = new BigDecimal("0.0000000001");

  public boolean hasErrors(
      LicencePositionTransferEquityForm form,
      Errors errors,
      Map<Integer, BigDecimal> equityHoldings
  ) {
    ValidationUtils.rejectIfEmptyOrWhitespace(errors, TRANSFER_FROM, "transferFrom.required",
        "Select the organisation equity is being transferred from");
    ValidationUtils.rejectIfEmptyOrWhitespace(errors, TRANSFER_TO, "transferTo.required",
        "Select the organisation equity is being transferred to");

    if (StringUtils.isNotBlank(form.getTransferFrom()) && form.getTransferFrom().equals(form.getTransferTo())) {
      errors.rejectValue(TRANSFER_TO, "transferTo.sameAsTransferFrom",
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

    var licenseeEquity = getLicenseeEquityByOrganisationId(equityHoldings);
    validateTransferorHoldsEnoughEquity(form, errors, licenseeEquity);
    validateTransferToIsLicensee(form, errors, licenseeEquity);

    return errors.hasErrors();
  }

  private void validateTransferorHoldsEnoughEquity(
      LicencePositionTransferEquityForm form,
      Errors errors,
      Map<String, BigDecimal> licenseeEquity
  ) {
    if (errors.hasFieldErrors(TRANSFER_FROM) || errors.hasFieldErrors("equity.inputValue")) {
      return;
    }

    var availableEquity = licenseeEquity.getOrDefault(form.getTransferFrom(), BigDecimal.ZERO);
    var requestedEquity = form.getEquity().getAsBigDecimal().orElse(BigDecimal.ZERO);
    if (requestedEquity.compareTo(availableEquity) > 0) {
      errors.rejectValue(TRANSFER_FROM, "transferFrom.insufficientEquity",
          "This organisation does not hold enough equity to transfer");
    }
  }

  private void validateTransferToIsLicensee(
      LicencePositionTransferEquityForm form,
      Errors errors,
      Map<String, BigDecimal> licenseeEquity
  ) {
    if (!errors.hasFieldErrors(TRANSFER_TO) && !licenseeEquity.containsKey(form.getTransferTo())) {
      errors.rejectValue(TRANSFER_TO, "transferTo.notLicensee",
          "This organisation is not an equity holder on this licence");
    }
  }

  private Map<String, BigDecimal> getLicenseeEquityByOrganisationId(Map<Integer, BigDecimal> equityHoldings) {
    return equityHoldings.entrySet().stream()
        .collect(Collectors.toMap(holding -> String.valueOf(holding.getKey()), Map.Entry::getValue));
  }
}