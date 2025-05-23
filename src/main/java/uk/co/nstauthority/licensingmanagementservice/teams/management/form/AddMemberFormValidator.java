package uk.co.nstauthority.licensingmanagementservice.teams.management.form;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.EnergyPortalUserService;

@Service
public class AddMemberFormValidator {

  private static final String FIELD_NAME = "username";
  private static final String PORTAL_USER_LOOKUP_PURPOSE = "Find user to add to team";

  private final EnergyPortalUserService energyPortalUserService;

  public AddMemberFormValidator(EnergyPortalUserService energyPortalUserService) {
    this.energyPortalUserService = energyPortalUserService;
  }

  public void validate(AddMemberForm form, Errors errors) {

    if (StringUtils.isBlank(form.getUsername())) {
      errors.rejectValue(FIELD_NAME, FIELD_NAME + ".required", "Enter an Energy Portal username");
      return;
    }

    var users = energyPortalUserService.findUsersByEmail(form.getUsername(), PORTAL_USER_LOOKUP_PURPOSE);
    if (users.isEmpty()) {
      errors.rejectValue(FIELD_NAME, FIELD_NAME + ".notFound", "No Energy Portal user exists with this username");
      return;
    }

    if (users.size() > 1) {
      errors.rejectValue(FIELD_NAME, FIELD_NAME + ".tooMany",
          "More than one Energy Portal user exists with this email address. Enter the username of the user instead.");
    }

    if (users.getFirst().sharedAccount()) {
      errors.rejectValue(FIELD_NAME, FIELD_NAME + ".sharedAccount", "You cannot add shared accounts to this service");
    }

    if (!users.getFirst().canLogin()) {
      errors.rejectValue(FIELD_NAME, FIELD_NAME + ".inactiveAccount",
          "This user does not have login access to the Energy Portal and can't be added to this service");
    }
  }
}
