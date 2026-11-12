package uk.co.nstauthority.licensingmanagementservice.licence.assignmentapplication.startjourney;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Collection;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import uk.co.fivium.formlibrary.validator.date.ThreeFieldDateInputValidator;
import uk.co.fivium.formlibrary.validator.string.StringInputValidator;

@Service
class LicenceAssignmentApplicationDetailsFormValidator {

  static final int APPLICATION_REFERENCE_MAX_LENGTH = 100;

  private final Clock clock;

  LicenceAssignmentApplicationDetailsFormValidator(Clock clock) {
    this.clock = clock;
  }

  boolean isValid(LicenceAssignmentApplicationDetailsForm form,
                  Errors errors,
                  Collection<Integer> availableOrganisationUnitIds) {
    ValidationUtils.rejectIfEmpty(
        errors,
        "organisationUnitId",
        "organisationUnitId.required",
        "Select the organisation this application is on behalf of"
    );

    var organisationUnitId = form.getOrganisationUnitId();
    if (organisationUnitId != null && !availableOrganisationUnitIds.contains(organisationUnitId)) {
      errors.rejectValue("organisationUnitId", "organisationUnitId.notAvailable", "Select a valid organisation");
    }

    StringInputValidator.builder()
        .emptyInputErrorMessage("Enter an application reference")
        .mustHaveCharacterCountAtMost(APPLICATION_REFERENCE_MAX_LENGTH)
        .mustHaveCharacterCountAtMostErrorMessage(
            "Application reference must be %d characters or fewer".formatted(APPLICATION_REFERENCE_MAX_LENGTH)
        )
        .validate(form.getApplicationReference(), errors);

    ThreeFieldDateInputValidator.builder()
        .emptyInputErrorMessage("Enter the planned completion date")
        .mustBeAfterOrEqualTo(LocalDate.now(clock))
        .mustBeAfterOrEqualToErrorMessage("Planned completion date must be today or in the future")
        .validate(form.getPlannedDate(), errors);

    return !errors.hasErrors();
  }

}
