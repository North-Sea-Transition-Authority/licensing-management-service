package uk.co.nstauthority.licensingmanagementservice.licence.overview.responsibleteam;

import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;

@Service
public class LicenceResponsibleTeamValidator {

  boolean isValid(LicenceResponsibleTeamForm form, Errors errors) {

    ValidationUtils.rejectIfEmptyOrWhitespace(
        errors,
        "responsibleTeam",
        "responsibleTeam.required",
        "Select the responsible team"
    );

    return !errors.hasErrors();
  }

}
