package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.licensee;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitJson;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;

@ExtendWith(MockitoExtension.class)
class LicenseeChangeFormValidatorTest {

  @Mock
  private OrganisationUnitQueryService organisationUnitQueryService;

  @InjectMocks
  private LicenseeChangeFormValidator licenseeChangeFormValidator;

  private static final String JOINING_SELECTOR_FIELD = "joiningOrganisationSelector";
  private static final String WITHDRAWING_SELECTOR_FIELD = "withdrawingOrganisationSelector";

  @Test
  void hasErrors_emptyForm() {
    var form = new LicenseeChangeForm()
        .setJoiningOrganisationIds(List.of())
        .setWithdrawingOrganisationIds(List.of());
    var errors = new BeanPropertyBindingResult(form, "form");
    errors.rejectValue(
        JOINING_SELECTOR_FIELD,
        "joiningOrganisationSelector.required",
        "Select licensees to join or withdraw");
    errors.rejectValue(
        WITHDRAWING_SELECTOR_FIELD,
        "withdrawingOrganisationSelector.required",
        "");
    var inputErrors = new BeanPropertyBindingResult(form, "form");
    assertThat(licenseeChangeFormValidator.hasErrors(form, inputErrors, List.of())).isTrue();
    assertThat(inputErrors).usingRecursiveComparison().isEqualTo(errors);
  }

  @Test
  void hasErrors_invalidJoiningIds() {
    var form = new LicenseeChangeForm()
        .setJoiningOrganisationIds(List.of("abc"))
        .setWithdrawingOrganisationIds(List.of());
    var errors = new BeanPropertyBindingResult(form, "form");
    errors.rejectValue(
        JOINING_SELECTOR_FIELD,
        "joiningOrganisationSelector.invalid",
        "Select a valid licensee");
    var inputErrors = new BeanPropertyBindingResult(form, "form");
    assertThat(licenseeChangeFormValidator.hasErrors(form, inputErrors, List.of())).isTrue();
    assertThat(inputErrors).usingRecursiveComparison().isEqualTo(errors);
  }

  @Test
  void hasErrors_invalidWithdrawingIds() {
    var form = new LicenseeChangeForm()
        .setJoiningOrganisationIds(List.of())
        .setWithdrawingOrganisationIds(List.of("abc"));
    var errors = new BeanPropertyBindingResult(form, "form");
    errors.rejectValue(
        WITHDRAWING_SELECTOR_FIELD,
        "withdrawingOrganisationSelector.invalid",
        "Select a valid licensee");
    var inputErrors = new BeanPropertyBindingResult(form, "form");
    assertThat(licenseeChangeFormValidator.hasErrors(form, inputErrors, List.of())).isTrue();
    assertThat(inputErrors).usingRecursiveComparison().isEqualTo(errors);
  }

  @Test
  void hasErrors_organisationNotFound() {
    var form = new LicenseeChangeForm()
        .setJoiningOrganisationIds(List.of("1"))
        .setWithdrawingOrganisationIds(List.of("2"));
    var errors = new BeanPropertyBindingResult(form, "form");
    errors.rejectValue(
        JOINING_SELECTOR_FIELD,
        "joiningOrganisationSelector.organisationNotFound",
        "Select a valid licensee");
    errors.rejectValue(
        WITHDRAWING_SELECTOR_FIELD,
        "withdrawingOrganisationSelector.organisationNotFound",
        "Select a valid licensee");
    var inputErrors = new BeanPropertyBindingResult(form, "form");

    when(organisationUnitQueryService.getOrganisationUnitsByIds(List.of(1, 2))).thenReturn(List.of());

    assertThat(licenseeChangeFormValidator.hasErrors(form, inputErrors, List.of(2))).isTrue();
    assertThat(inputErrors).usingRecursiveComparison().isEqualTo(errors);
  }

  @Test
  void hasErrors_duplicateIds() {
    var form = new LicenseeChangeForm()
        .setJoiningOrganisationIds(List.of("1"))
        .setWithdrawingOrganisationIds(List.of());
    var errors = new BeanPropertyBindingResult(form, "form");
    errors.rejectValue(
        JOINING_SELECTOR_FIELD,
        "joiningOrganisationSelector.duplicateIds",
        "Select licensees that are not already on the licence");
    var inputErrors = new BeanPropertyBindingResult(form, "form");
    var unit = new OrganisationUnitJson(1, "org1");

    when(organisationUnitQueryService.getOrganisationUnitsByIds(List.of(1))).thenReturn(List.of(unit));

    assertThat(licenseeChangeFormValidator.hasErrors(form, inputErrors, List.of(1))).isTrue();
    assertThat(inputErrors).usingRecursiveComparison().isEqualTo(errors);
  }

  @Test
  void hasErrors_idsNotFound() {
    var form = new LicenseeChangeForm()
        .setJoiningOrganisationIds(List.of())
        .setWithdrawingOrganisationIds(List.of("1"));
    var errors = new BeanPropertyBindingResult(form, "form");
    errors.rejectValue(
        WITHDRAWING_SELECTOR_FIELD,
        "withdrawingOrganisationSelector.notFound",
        "Select licensees that are on the licence");
    var inputErrors = new BeanPropertyBindingResult(form, "form");
    var unit = new OrganisationUnitJson(1, "org1");

    when(organisationUnitQueryService.getOrganisationUnitsByIds(List.of(1))).thenReturn(List.of(unit));

    assertThat(licenseeChangeFormValidator.hasErrors(form, inputErrors, List.of())).isTrue();
    assertThat(inputErrors).usingRecursiveComparison().isEqualTo(errors);
  }

  @Test
  void hasErrors_addIdMultipleTimes() {
    var form = new LicenseeChangeForm()
        .setJoiningOrganisationIds(List.of("1", "1"))
        .setWithdrawingOrganisationIds(List.of());
    var errors = new BeanPropertyBindingResult(form, "form");
    errors.rejectValue(
        JOINING_SELECTOR_FIELD,
        "joiningOrganisationSelector.duplicateIds",
        "Only add a licensee a single time");
    var inputErrors = new BeanPropertyBindingResult(form, "form");
    var unit = new OrganisationUnitJson(1, "org1");

    when(organisationUnitQueryService.getOrganisationUnitsByIds(List.of(1, 1))).thenReturn(List.of(unit));

    assertThat(licenseeChangeFormValidator.hasErrors(form, inputErrors, List.of())).isTrue();
    assertThat(inputErrors).usingRecursiveComparison().isEqualTo(errors);
  }

  @Test
  void hasErrors_removeIdMultipleTimes() {
    var form = new LicenseeChangeForm()
        .setJoiningOrganisationIds(List.of())
        .setWithdrawingOrganisationIds(List.of("1", "1"));
    var errors = new BeanPropertyBindingResult(form, "form");
    errors.rejectValue(
        WITHDRAWING_SELECTOR_FIELD,
        "withdrawingOrganisationSelector.duplicateIds",
        "Only remove a licensee a single time");
    var inputErrors = new BeanPropertyBindingResult(form, "form");
    var unit = new OrganisationUnitJson(1, "org1");

    when(organisationUnitQueryService.getOrganisationUnitsByIds(List.of(1, 1))).thenReturn(List.of(unit));

    assertThat(licenseeChangeFormValidator.hasErrors(form, inputErrors, List.of(1))).isTrue();
    assertThat(inputErrors).usingRecursiveComparison().isEqualTo(errors);
  }

  @Test
  void hasErrors_addAndRemoveSameId() {
    var form = new LicenseeChangeForm()
        .setJoiningOrganisationIds(List.of("1"))
        .setWithdrawingOrganisationIds(List.of("1"));
    var errors = new BeanPropertyBindingResult(form, "form");
    errors.rejectValue(
        WITHDRAWING_SELECTOR_FIELD,
        "withdrawingOrganisationSelector.notFound",
        "Select licensees that are on the licence");
    errors.rejectValue(
        JOINING_SELECTOR_FIELD,
        "joiningOrganisationSelector.conflict",
        "Cannot add and remove a licensee at the same position");
    errors.rejectValue(
        WITHDRAWING_SELECTOR_FIELD,
        "withdrawingOrganisationSelector.conflict",
        "");
    var inputErrors = new BeanPropertyBindingResult(form, "form");
    var unit = new OrganisationUnitJson(1, "org1");

    when(organisationUnitQueryService.getOrganisationUnitsByIds(List.of(1, 1))).thenReturn(List.of(unit));

    assertThat(licenseeChangeFormValidator.hasErrors(form, inputErrors, List.of())).isTrue();
    assertThat(inputErrors).usingRecursiveComparison().isEqualTo(errors);
  }

  @Test
  void hasErrors_noErrors() {
    var form = new LicenseeChangeForm()
        .setJoiningOrganisationIds(List.of("2"))
        .setWithdrawingOrganisationIds(List.of("1"));
    var errors = new BeanPropertyBindingResult(form, "form");
    var inputErrors = new BeanPropertyBindingResult(form, "form");
    var unit1 = new OrganisationUnitJson(1, "org1");
    var unit2 = new OrganisationUnitJson(2, "org2");

    when(organisationUnitQueryService.getOrganisationUnitsByIds(List.of(2, 1))).thenReturn(List.of(unit1, unit2));

    assertThat(licenseeChangeFormValidator.hasErrors(form, inputErrors, List.of(1))).isFalse();
    assertThat(inputErrors).usingRecursiveComparison().isEqualTo(errors);
  }
}