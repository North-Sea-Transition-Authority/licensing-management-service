package uk.co.nstauthority.licensingmanagementservice.teams.management.form;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import uk.co.nstauthority.licensingmanagementservice.teams.management.TeamManagementService;

@Service
public class AddMemberFormValidator {

  private static final String FIELD_NAME = "emailAddress";

  private final TeamManagementService teamManagementService;

  public AddMemberFormValidator(
      TeamManagementService teamManagementService
  ) {
    this.teamManagementService = teamManagementService;
  }

  public boolean isValid(AddMemberForm form, Errors errors) {

    if (StringUtils.isBlank(form.getEmailAddress())) {
      errors.rejectValue(FIELD_NAME, FIELD_NAME + ".required", "Enter a UK Energy Portal email address");
      return false;
    }

    var users = teamManagementService.getEnergyPortalUser(form.getEmailAddress());
    if (users.isEmpty()) {
      errors.rejectValue(FIELD_NAME, FIELD_NAME + ".notFound", "No UK Energy Portal account exists with this email address");
      return false;
    }

    if (!users.get().canLogin()) {
      errors.rejectValue(
          FIELD_NAME,
          FIELD_NAME + ".inactiveAccount",
          "This user does not have login access to the UK Energy Portal and can't be added to this service"
      );
    }

    return !errors.hasErrors();
  }
}
