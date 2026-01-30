package uk.co.nstauthority.licensingmanagementservice.teams.management.form;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.Team;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamTestUtil;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;
import uk.co.nstauthority.licensingmanagementservice.teams.management.TeamManagementService;
import uk.co.nstauthority.licensingmanagementservice.validation.ValidatorTestingUtil;

@ExtendWith(MockitoExtension.class)
class MemberRolesFormValidatorTest {

  @Mock
  private TeamManagementService teamManagementService;

  @InjectMocks
  private MemberRolesFormValidator memberRolesFormValidator;

  private MemberRolesForm form;
  private BeanPropertyBindingResult errors;
  private Team team;
  private static final String FIELD_NAME = "roles";

  @BeforeEach
  void setUp() {
    form = new MemberRolesForm();
    errors = new BeanPropertyBindingResult(form, "form");
    team = TeamTestUtil.newBuilder()
        .withTeamType(TeamType.ORGANISATION)
        .withId(UUID.randomUUID())
        .build();
  }

  @Test
  void isValid() {
    form.setRoles(List.of("MANAGE_TEAM"));

    when(teamManagementService.willManageTeamRoleBePresentAfterMemberRoleUpdate(team, 1L, List.of(Role.MANAGE_TEAM)))
        .thenReturn(true);

    memberRolesFormValidator.validate(form,1L, team, errors);

    assertThat(errors.hasErrors()).isFalse();

    var extractedErrors = ValidatorTestingUtil.extractErrors(errors);
    assertThat(extractedErrors).isEmpty();
  }

  @Test
  void isValid_noRoles() {
    form.setRoles(null);

    memberRolesFormValidator.validate(form,1L, team, errors);

    assertThat(errors.hasErrors()).isTrue();

    var extractedErrors = ValidatorTestingUtil.extractErrors(errors);
    assertThat(extractedErrors).containsExactly(
        entry(FIELD_NAME, Set.of(FIELD_NAME + ".required")));

    var errorMessages = ValidatorTestingUtil.extractErrorMessages(errors);
    assertThat(errorMessages).containsExactly(
        entry(FIELD_NAME, Set.of("Select at least one role")));
  }

  @Test
  void isValid_noTeamManagerLeft() {
    form.setRoles(List.of("MANAGE_TEAM"));

    when(teamManagementService.willManageTeamRoleBePresentAfterMemberRoleUpdate(team, 1L, List.of(Role.MANAGE_TEAM)))
        .thenReturn(false);

    memberRolesFormValidator.validate(form,1L, team, errors);

    assertThat(errors.hasErrors()).isTrue();

    var extractedErrors = ValidatorTestingUtil.extractErrors(errors);
    assertThat(extractedErrors).containsExactly(
        entry(FIELD_NAME, Set.of(FIELD_NAME + ".noTeamManager")));

    var errorMessages = ValidatorTestingUtil.extractErrorMessages(errors);
    assertThat(errorMessages).containsExactly(
        entry(FIELD_NAME, Set.of(
            "There must always be at least one user who can add, remove and update members of this team."
        )));
  }
}
