package uk.co.nstauthority.licensingmanagementservice.teams.management.form;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import uk.co.fivium.energyportal.serviceproviders.epmq.ScopeType;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamScopeReference;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;
import uk.co.nstauthority.licensingmanagementservice.teams.management.TeamManagementService;

@ExtendWith(MockitoExtension.class)
class NewOrganisationTeamFormValidatorTest {
  @Mock
  private TeamManagementService teamManagementService;

  @InjectMocks
  private NewOrganisationTeamFormValidator newOrganisationTeamFormValidator;

  private NewOrganisationTeamForm form;
  private BeanPropertyBindingResult errors;

  @BeforeEach
  void setUp() {
    form = new NewOrganisationTeamForm();
    errors = new BeanPropertyBindingResult(form, "form");
  }

  @Test
  void validate() {
    form.setOrgGroupId("50");

    when(teamManagementService.doesScopedTeamWithReferenceExist(eq(TeamType.ORGANISATION),
        refEq(TeamScopeReference.from("50", ScopeType.ORGANISATION_GROUP.name())))
    ).thenReturn(false);

    assertThat(newOrganisationTeamFormValidator.validate(form, errors)).isTrue();
    assertThat(errors.hasErrors()).isFalse();
  }

  @Test
  void validate_noId() {
    form.setOrgGroupId("");

    assertThat(newOrganisationTeamFormValidator.validate(form, errors)).isFalse();
    assertThat(errors.hasErrors()).isTrue();
  }

  @Test
  void validate_orgTeamAlreadyExists() {
    form.setOrgGroupId("50");

    when(teamManagementService.doesScopedTeamWithReferenceExist(eq(TeamType.ORGANISATION),
        refEq(TeamScopeReference.from("50", ScopeType.ORGANISATION_GROUP.name())))
    ).thenReturn(true);

    assertThat(newOrganisationTeamFormValidator.validate(form, errors)).isFalse();
    assertThat(errors.hasErrors()).isTrue();
  }
}
