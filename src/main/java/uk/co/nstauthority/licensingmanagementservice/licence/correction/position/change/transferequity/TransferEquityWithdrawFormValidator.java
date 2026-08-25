package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.transferequity;

import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.EnumValidationUtil;

@Service
public class TransferEquityWithdrawFormValidator {

  public boolean hasErrors(
      TransferEquityWithdrawForm form,
      Errors errors
  ) {
    ValidationUtils.rejectIfEmptyOrWhitespace(errors, "withdrawalDecision", "withdrawalDecision.required",
        "Select whether the organisation should retain a beneficial interest in the licence");

    if (!errors.hasFieldErrors("withdrawalDecision")
        && EnumValidationUtil.isNotValidEnumValue(TransferEquityWithdrawalDecision.class, form.getWithdrawalDecision())) {
      errors.rejectValue("withdrawalDecision", "withdrawalDecision.invalid",
          "Select whether the organisation should retain a beneficial interest in the licence");
    }

    return errors.hasErrors();
  }
}