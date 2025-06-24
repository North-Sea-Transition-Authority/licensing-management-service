package uk.co.nstauthority.licensingmanagementservice.teams.management.form;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import uk.co.fivium.energyportalapi.generated.types.User;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.EnergyPortalUserJson;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.EnergyPortalUserService;
import uk.co.nstauthority.licensingmanagementservice.util.EnergyPortalUserTestUtil;
import uk.co.nstauthority.licensingmanagementservice.validation.ValidatorTestingUtil;

@ExtendWith(MockitoExtension.class)
class AddMemberFormValidatorTest {

  @Mock
  private EnergyPortalUserService energyPortalUserService;

  @InjectMocks
  private AddMemberFormValidator addMemberFormValidator;

  private AddMemberForm form;
  private User user;
  private BeanPropertyBindingResult errors;

  private static final String FIELD_NAME = "username";
  private static final String PORTAL_USER_LOOKUP_PURPOSE = "Find user to add to team";

  @BeforeEach
  void setUp() {
    form = new AddMemberForm();
    user = EnergyPortalUserTestUtil.newBuilder().build();
    errors = new BeanPropertyBindingResult(form, "form");
  }

  @Test
  void isValid() {
    form.setUsername("foo");
    user.setIsAccountShared(false);
    user.setCanLogin(true);

    when(energyPortalUserService.findUsersByEmail("foo", PORTAL_USER_LOOKUP_PURPOSE))
        .thenReturn(List.of(EnergyPortalUserJson.from(user)));

    addMemberFormValidator.validate(form, errors);

    assertThat(errors.hasErrors()).isFalse();

    var extractedErrors = ValidatorTestingUtil.extractErrors(errors);
    assertThat(extractedErrors).isEmpty();
  }

  @Test
  void isValid_noUsername() {
    form.setUsername(null);

    addMemberFormValidator.validate(form, errors);

    assertThat(errors.hasErrors()).isTrue();

    var extractedErrors = ValidatorTestingUtil.extractErrors(errors);
    assertThat(extractedErrors).containsExactly(
        entry(FIELD_NAME, Set.of(FIELD_NAME + ".required")));

    var errorMessages = ValidatorTestingUtil.extractErrorMessages(errors);
    assertThat(errorMessages).containsExactly(
        entry(FIELD_NAME, Set.of("Enter an Energy Portal username")));
  }

  @Test
  void isValid_noEpaUser() {
    form.setUsername("foo");

    when(energyPortalUserService.findUsersByEmail("foo", PORTAL_USER_LOOKUP_PURPOSE))
        .thenReturn(List.of());

    addMemberFormValidator.validate(form, errors);

    assertThat(errors.hasErrors()).isTrue();

    var extractedErrors = ValidatorTestingUtil.extractErrors(errors);
    assertThat(extractedErrors).containsExactly(
        entry(FIELD_NAME, Set.of(FIELD_NAME + ".notFound")));

    var errorMessages = ValidatorTestingUtil.extractErrorMessages(errors);
    assertThat(errorMessages).containsExactly(
        entry(FIELD_NAME, Set.of("No Energy Portal user exists with this username")));
  }

  @Test
  void isValid_tooManyEpaUsers() {
    form.setUsername("foo");

    var user1 = EnergyPortalUserTestUtil.newBuilder().build();

    var user2 = EnergyPortalUserTestUtil.newBuilder().build();

    when(energyPortalUserService.findUsersByEmail("foo", PORTAL_USER_LOOKUP_PURPOSE))
        .thenReturn(List.of(
            EnergyPortalUserJson.from(user1),
            EnergyPortalUserJson.from(user2)
        ));

    addMemberFormValidator.validate(form, errors);

    assertThat(errors.hasErrors()).isTrue();

    var extractedErrors = ValidatorTestingUtil.extractErrors(errors);
    assertThat(extractedErrors).containsExactly(
        entry(FIELD_NAME, Set.of(FIELD_NAME + ".tooMany")));

    var errorMessages = ValidatorTestingUtil.extractErrorMessages(errors);
    assertThat(errorMessages).containsExactly(
        entry(FIELD_NAME, Set.of(
            "More than one Energy Portal user exists with this email address. Enter the username of the user instead."
        )));
  }

  @Test
  void isValid_sharedAccount() {
    form.setUsername("foo");
    user.setIsAccountShared(true);
    user.setCanLogin(true);

    when(energyPortalUserService.findUsersByEmail("foo", PORTAL_USER_LOOKUP_PURPOSE))
        .thenReturn(List.of(EnergyPortalUserJson.from(user)));

    addMemberFormValidator.validate(form, errors);

    assertThat(errors.hasErrors()).isTrue();

    var extractedErrors = ValidatorTestingUtil.extractErrors(errors);
    assertThat(extractedErrors).containsExactly(
        entry(FIELD_NAME, Set.of(FIELD_NAME + ".sharedAccount")));

    var errorMessages = ValidatorTestingUtil.extractErrorMessages(errors);
    assertThat(errorMessages).containsExactly(
        entry(FIELD_NAME, Set.of("You cannot add shared accounts to this service")));
  }

  @Test
  void isValid_canNotLogin() {
    form.setUsername("foo");
    user.setIsAccountShared(false);
    user.setCanLogin(false);

    when(energyPortalUserService.findUsersByEmail("foo", PORTAL_USER_LOOKUP_PURPOSE))
        .thenReturn(List.of(EnergyPortalUserJson.from(user)));

    addMemberFormValidator.validate(form, errors);

    assertThat(errors.hasErrors()).isTrue();

    var extractedErrors = ValidatorTestingUtil.extractErrors(errors);
    assertThat(extractedErrors).containsExactly(
        entry(FIELD_NAME, Set.of(FIELD_NAME + ".inactiveAccount")));

    var errorMessages = ValidatorTestingUtil.extractErrorMessages(errors);
    assertThat(errorMessages).containsExactly(
        entry(FIELD_NAME, Set.of(
            "This user does not have login access to the Energy Portal and can't be added to this service"
        )));
  }
}
