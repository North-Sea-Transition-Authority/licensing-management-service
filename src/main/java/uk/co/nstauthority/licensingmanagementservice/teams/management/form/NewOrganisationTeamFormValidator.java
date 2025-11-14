package uk.co.nstauthority.licensingmanagementservice.teams.management.form;

import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamScopeReference;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;
import uk.co.nstauthority.licensingmanagementservice.teams.management.TeamManagementService;

@Service
public class NewOrganisationTeamFormValidator {
  private final TeamManagementService teamManagementService;

  public NewOrganisationTeamFormValidator(TeamManagementService teamManagementService) {
    this.teamManagementService = teamManagementService;
  }

  public boolean validate(NewOrganisationTeamForm form, Errors errors) {
    if (form.getOrgGroupId() == null || form.getOrgGroupId().isEmpty()) {
      errors.rejectValue("orgGroupId", "orgGroupId.required", "Select an organisation");
      return false;
    }

    if (teamManagementService.doesScopedTeamWithReferenceExist(
        TeamType.ORGANISATION,
        TeamScopeReference.from(form.getOrgGroupId(), TeamType.ORGANISATION.name()))
    ) {
      errors.rejectValue(
          "orgGroupId",
          "orgGroupId.alreadyExists",
          "A team for this organisation already exists"
      );
    }

    return !errors.hasErrors();
  }

}
