package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.administrator;

import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import uk.co.fivium.formlibrary.validator.string.StringInputValidator;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;

@Component
public class AdministratorChangeFormValidator {

  private final OrganisationUnitQueryService organisationUnitQueryService;

  public AdministratorChangeFormValidator(OrganisationUnitQueryService organisationUnitQueryService) {
    this.organisationUnitQueryService = organisationUnitQueryService;
  }

  public boolean hasErrors(AdministratorChangeForm form, Errors errors, Integer currentAdministratorId) {
    StringInputValidator.builder()
        .emptyInputErrorMessage("Select a licence administrator")
        .validate(form.getAdminId(), errors);

    if (errors.hasErrors()) {
      return true;
    }

    var newAdministratorId = NumberUtils.toInt(form.getAdminId().getInputValue());

    if (organisationUnitQueryService.getOrganisationUnit(newAdministratorId).isEmpty()) {
      errors.rejectValue(
          "adminId.inputValue",
          "adminId.invalid",
          "Select a valid licence administrator"
      );
      return true;
    }

    if (currentAdministratorId != null && newAdministratorId == currentAdministratorId) {
      errors.rejectValue(
          "adminId.inputValue",
          "adminId.sameAsCurrent",
          "The new licence administrator must be different to the current administrator"
      );
    }

    return errors.hasErrors();
  }
}
