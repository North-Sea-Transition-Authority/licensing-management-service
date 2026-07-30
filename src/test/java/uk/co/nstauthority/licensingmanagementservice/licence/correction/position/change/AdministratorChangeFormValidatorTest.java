package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.administrator.AdministratorChangeForm;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.administrator.AdministratorChangeFormValidator;
import uk.co.nstauthority.licensingmanagementservice.validation.ValidatorTestingUtil;

@ExtendWith(MockitoExtension.class)
class AdministratorChangeFormValidatorTest {

  @Mock
  private OrganisationUnitQueryService organisationUnitQueryService;

  @InjectMocks
  private AdministratorChangeFormValidator administratorChangeFormValidator;

  private AdministratorChangeForm form;

  private Errors errors;

  @BeforeEach
  void setUp() {
    form = new AdministratorChangeForm();
    errors = new BeanPropertyBindingResult(form, "form");
  }

  @Test
  void hasErrors_whenAdminIdEmpty_thenErrorWithMessage() {
    var result = administratorChangeFormValidator.hasErrors(form, errors, null);

    assertThat(result).isTrue();
    assertThat(ValidatorTestingUtil.getErrorsFieldsAndMessages(errors))
        .containsOnly(
            entry("adminId.inputValue", Collections.singletonList("Select a licence administrator"))
        );
  }

  @Test
  void hasErrors_whenAdminIdNotAnExistingOrganisationUnit_thenErrorWithMessage() {
    form.getAdminId().setInputValue("123");
    when(organisationUnitQueryService.getOrganisationUnit(123)).thenReturn(Optional.empty());

    var result = administratorChangeFormValidator.hasErrors(form, errors, null);

    assertThat(result).isTrue();
    assertThat(ValidatorTestingUtil.getErrorsFieldsAndMessages(errors))
        .containsOnly(
            entry("adminId.inputValue", Collections.singletonList("Select a valid licence administrator"))
        );
  }

  @Test
  void hasErrors_whenAdminIdNotNumeric_thenErrorWithMessage() {
    form.getAdminId().setInputValue("not-a-number");

    var result = administratorChangeFormValidator.hasErrors(form, errors, null);

    assertThat(result).isTrue();
    assertThat(ValidatorTestingUtil.getErrorsFieldsAndMessages(errors))
        .containsOnly(
            entry("adminId.inputValue", Collections.singletonList("Select a valid licence administrator"))
        );
  }

  @Test
  void hasErrors_whenAdminIdIsSameAsCurrentAdministrator_thenErrorWithMessage() {
    form.getAdminId().setInputValue(OrganisationUnitTestUtil.ORG_UNIT_ID_1.toString());
    when(organisationUnitQueryService.getOrganisationUnit(OrganisationUnitTestUtil.ORG_UNIT_ID_1))
        .thenReturn(Optional.of(OrganisationUnitTestUtil.ORG_UNIT_1));

    var result = administratorChangeFormValidator.hasErrors(form, errors, OrganisationUnitTestUtil.ORG_UNIT_ID_1);

    assertThat(result).isTrue();
    assertThat(ValidatorTestingUtil.getErrorsFieldsAndMessages(errors))
        .containsOnly(
            entry("adminId.inputValue",
                Collections.singletonList("The new licence administrator must be different to the current administrator"))
        );
  }

  @Test
  void hasErrors_whenAdminIdIsSameAsPreviousAdministrator_thenErrorWithMessage() {
    form.getAdminId().setInputValue(OrganisationUnitTestUtil.ORG_UNIT_ID_1.toString());
    when(organisationUnitQueryService.getOrganisationUnit(OrganisationUnitTestUtil.ORG_UNIT_ID_1))
        .thenReturn(Optional.of(OrganisationUnitTestUtil.ORG_UNIT_1));

    var result = administratorChangeFormValidator.hasErrors(form, errors, OrganisationUnitTestUtil.ORG_UNIT_ID_2, OrganisationUnitTestUtil.ORG_UNIT_ID_1);

    assertThat(result).isTrue();
    assertThat(ValidatorTestingUtil.getErrorsFieldsAndMessages(errors))
        .containsOnly(
            entry("adminId.inputValue",
                Collections.singletonList("The new licence administrator must be different to the previous administrator"))
        );
  }

  @Test
  void hasErrors_whenAdminIdDiffersFromCurrentAdministrator_thenNoErrors() {
    form.getAdminId().setInputValue(OrganisationUnitTestUtil.ORG_UNIT_ID_2.toString());
    when(organisationUnitQueryService.getOrganisationUnit(OrganisationUnitTestUtil.ORG_UNIT_ID_2))
        .thenReturn(Optional.of(OrganisationUnitTestUtil.ORG_UNIT_2));

    var result = administratorChangeFormValidator.hasErrors(form, errors, OrganisationUnitTestUtil.ORG_UNIT_ID_1);

    assertThat(result).isFalse();
    assertThat(errors.hasErrors()).isFalse();
  }

  @Test
  void hasErrors_whenAdminIdIsExistingOrganisationUnitAndNoCurrentAdministrator_thenNoErrors() {
    form.getAdminId().setInputValue(OrganisationUnitTestUtil.ORG_UNIT_ID_1.toString());
    when(organisationUnitQueryService.getOrganisationUnit(OrganisationUnitTestUtil.ORG_UNIT_ID_1))
        .thenReturn(Optional.of(OrganisationUnitTestUtil.ORG_UNIT_1));

    var result = administratorChangeFormValidator.hasErrors(form, errors, null);

    assertThat(result).isFalse();
    assertThat(errors.hasErrors()).isFalse();
  }
}
