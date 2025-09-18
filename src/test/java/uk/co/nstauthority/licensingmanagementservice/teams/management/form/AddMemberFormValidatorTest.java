package uk.co.nstauthority.licensingmanagementservice.teams.management.form;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.EnergyPortalUserJson;
import uk.co.nstauthority.licensingmanagementservice.teams.management.TeamManagementService;
import uk.co.nstauthority.licensingmanagementservice.util.EnergyPortalUserTestUtil;

@ExtendWith(MockitoExtension.class)
class AddMemberFormValidatorTest {

  @Mock
  private TeamManagementService teamManagementService;

  @InjectMocks
  private AddMemberFormValidator addMemberFormValidator;

  private AddMemberForm form;
  private EnergyPortalUserJson user;
  private BeanPropertyBindingResult errors;

  @BeforeEach
  void setUp() {
    form = new AddMemberForm();
    errors = new BeanPropertyBindingResult(form, "form");
  }

  @Test
  void isValid() {
    form.setEmailAddress("foo");
    user = EnergyPortalUserTestUtil.newBuilder()
        .setSharedAccount(false)
        .canLogin(true)
        .buildJson();

    when(teamManagementService.getEnergyPortalUser("foo")).thenReturn(Optional.of(user));

    assertThat(addMemberFormValidator.isValid(form, errors)).isTrue();
    assertThat(errors.hasErrors()).isFalse();
  }

  @Test
  void isValid_noEmailAddress() {
    var error = new BeanPropertyBindingResult(form, "form");
    form.setEmailAddress(null);
    assertThat(addMemberFormValidator.isValid(form, error)).isFalse();
    assertThat(error.hasErrors()).isTrue();
  }

  @Test
  void isValid_noEpaUser() {
    form.setEmailAddress("foo");

    when(teamManagementService.getEnergyPortalUser("foo")).thenReturn(Optional.empty());
    assertThat(addMemberFormValidator.isValid(form, errors)).isFalse();
    assertThat(errors.hasErrors()).isTrue();
  }

  @Test
  void isValid_canNotLogin() {
    form.setEmailAddress("foo");
    user = EnergyPortalUserTestUtil.newBuilder()
        .setSharedAccount(false)
        .canLogin(false)
        .buildJson();

    when(teamManagementService.getEnergyPortalUser("foo")).thenReturn(Optional.of(user));

    assertThat(addMemberFormValidator.isValid(form, errors)).isFalse();
    assertThat(errors.hasErrors()).isTrue();
  }
}