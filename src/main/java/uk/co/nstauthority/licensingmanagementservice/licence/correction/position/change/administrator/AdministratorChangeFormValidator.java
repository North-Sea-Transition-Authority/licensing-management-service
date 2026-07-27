package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.administrator;

import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import uk.co.fivium.formlibrary.validator.string.StringInputValidator;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;

@Component
public class AdministratorChangeFormValidator {

  private static final String ADMIN_ID_FIELD = "adminId.inputValue";

  private final OrganisationUnitQueryService organisationUnitQueryService;

  public AdministratorChangeFormValidator(OrganisationUnitQueryService organisationUnitQueryService) {
    this.organisationUnitQueryService = organisationUnitQueryService;
  }

  public boolean hasErrors(AdministratorChangeForm form, Errors errors, Integer currentAdministratorId) {
    return hasErrors(form, errors, currentAdministratorId, null);
  }

  public boolean hasErrors(
      AdministratorChangeForm form,
      Errors errors,
      Integer currentAdministratorId,
      Integer previousAdministratorId
  ) {
    StringInputValidator.builder()
        .emptyInputErrorMessage("Select a licence administrator")
        .validate(form.getAdminId(), errors);

    if (errors.hasErrors()) {
      return true;
    }

    int newAdministratorId;
    try {
      newAdministratorId = Integer.parseInt(form.getAdminId().getInputValue());
    } catch (NumberFormatException e) {
      errors.rejectValue(
          ADMIN_ID_FIELD,
          "adminId.invalid",
          "Select a valid licence administrator"
      );
      return true;
    }

    if (organisationUnitQueryService.getOrganisationUnit(newAdministratorId).isEmpty()) {
      errors.rejectValue(
          ADMIN_ID_FIELD,
          "adminId.invalid",
          "Select a valid licence administrator"
      );
      return true;
    }

    if (Objects.equals(currentAdministratorId, newAdministratorId)) {
      errors.rejectValue(
          ADMIN_ID_FIELD,
          "adminId.sameAsCurrent",
          "The new licence administrator must be different to the current administrator"
      );
    }

    if (Objects.equals(previousAdministratorId, newAdministratorId)) {
      errors.rejectValue(
          ADMIN_ID_FIELD,
          "adminId.sameAsPrevious",
          "The new licence administrator must be different to the previous administrator"
      );
    }

    return errors.hasErrors();
  }
}
