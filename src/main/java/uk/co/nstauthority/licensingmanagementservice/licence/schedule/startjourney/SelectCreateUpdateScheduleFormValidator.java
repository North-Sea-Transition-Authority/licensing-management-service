package uk.co.nstauthority.licensingmanagementservice.licence.schedule.startjourney;

import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;

@Service
public class SelectCreateUpdateScheduleFormValidator {

  boolean isValid(SelectCreateUpdateScheduleForm form, Errors errors) {
    ValidationUtils.rejectIfEmpty(errors, "selectedJourneyOption", "selectedJourneyOption.required");

    return !errors.hasErrors();
  }

}
