package uk.co.nstauthority.licensingmanagementservice.licence.application.requestupdate;

import org.apache.commons.lang3.BooleanUtils;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;

@Service
public class ConfirmOnlyRequestedChangesFormValidator {

  public boolean isValid(ConfirmOnlyRequestedChangesForm form, Errors errors) {
    if (!BooleanUtils.isTrue(form.getOnlyRequestedChangesMade())) {
      errors.rejectValue(
          "onlyRequestedChangesMade",
          "onlyRequestedChangesMade.required",
          "Confirm that you have only made the changes that were requested"
      );
    }

    return !errors.hasErrors();
  }
}
