package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.administrator.AdministratorChangeForm;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.explorationoperator.ExplorationOperatorChangeForm;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.storageoperator.StorageOperatorChangeForm;
import uk.co.nstauthority.licensingmanagementservice.validation.ValidatorTestingUtil;

@ExtendWith(MockitoExtension.class)
class OrganisationChangeFormValidatorTest {

  @Mock
  private OrganisationUnitQueryService organisationUnitQueryService;

  @InjectMocks
  private OrganisationChangeFormValidator organisationChangeFormValidator;
  
  private static final String OPERATOR_INPUT_CODE = "id.inputValue";
  private static final String STORAGE_OPERATOR_NAME = "storage operator";
  private static final String EXPLORATION_OPERATOR_NAME = "exploration operator";
  private static final String ADMINISTRATOR_NAME = "licence administrator";

  @ParameterizedTest
  @NullAndEmptySource
  void hasErrors_whenStorageOperatorIdEmpty_thenErrorWithMessage(String id) {
    var storageOperatorChangeForm = new StorageOperatorChangeForm();
    storageOperatorChangeForm.getId().setInputValue(id);
    var errors = new BeanPropertyBindingResult(storageOperatorChangeForm, "form");
    var result = organisationChangeFormValidator.hasErrors(storageOperatorChangeForm, errors, null, "storage operator");

    assertThat(result).isTrue();
    assertThat(ValidatorTestingUtil.getErrorsFieldsAndMessages(errors))
        .containsOnly(
            entry(OPERATOR_INPUT_CODE, List.of("Select a storage operator"))
        );
  }

  @ParameterizedTest
  @NullAndEmptySource
  void hasErrors_whenExplorationOperatorIdEmpty_thenErrorWithMessage(String id) {
    var explorationOperatorChangeForm = new ExplorationOperatorChangeForm();
    explorationOperatorChangeForm.getId().setInputValue(id);
    var errors = new BeanPropertyBindingResult(explorationOperatorChangeForm, "form");
    var result = organisationChangeFormValidator.hasErrors(explorationOperatorChangeForm, errors, null, "exploration operator");

    assertThat(result).isTrue();
    assertThat(ValidatorTestingUtil.getErrorsFieldsAndMessages(errors))
        .containsOnly(
            entry(OPERATOR_INPUT_CODE, List.of("Select an exploration operator"))
        );
  }

  @ParameterizedTest
  @NullAndEmptySource
  void hasErrors_whenAdminIdEmpty_thenErrorWithMessage(String id) {
    var administratorChangeForm = new AdministratorChangeForm();
    administratorChangeForm.getId().setInputValue(id);
    var errors = new BeanPropertyBindingResult(administratorChangeForm, "form");
    var result = organisationChangeFormValidator.hasErrors(administratorChangeForm, errors, null, "licence administrator");

    assertThat(result).isTrue();
    assertThat(ValidatorTestingUtil.getErrorsFieldsAndMessages(errors))
        .containsOnly(
            entry(OPERATOR_INPUT_CODE, List.of("Select a licence administrator"))
        );
  }

  @ParameterizedTest(name = "{argumentSetName}")
  @MethodSource("hasErrors")
  void hasErrors_whenOperatorIdNotAnExistingOrganisationUnit_thenErrorWithMessage(
      OrganisationChangeForm form,
      String operatorType
  ) {
    form.getId().setInputValue("123");
    when(organisationUnitQueryService.getOrganisationUnit(123)).thenReturn(Optional.empty());

    var errors = new BeanPropertyBindingResult(form, "form");
    var result = organisationChangeFormValidator.hasErrors(form, errors, OrganisationUnitTestUtil.ORG_UNIT_ID_1, operatorType);

    assertThat(result).isTrue();
    assertThat(ValidatorTestingUtil.getErrorsFieldsAndMessages(errors))
        .containsOnly(
            entry(OPERATOR_INPUT_CODE, List.of("Select a valid %s".formatted(operatorType)))
        );
  }

  @ParameterizedTest(name = "{argumentSetName}")
  @MethodSource("hasErrors")
  void hasErrors_whenOperatorIdNotNumeric_thenErrorWithMessage(
      OrganisationChangeForm form,
      String operatorType
  ) {
    form.getId().setInputValue("not-a-number");

    var errors = new BeanPropertyBindingResult(form, "form");
    var result = organisationChangeFormValidator.hasErrors(form, errors, OrganisationUnitTestUtil.ORG_UNIT_ID_1, operatorType);

    assertThat(result).isTrue();
    assertThat(ValidatorTestingUtil.getErrorsFieldsAndMessages(errors))
        .containsOnly(
            entry(OPERATOR_INPUT_CODE, List.of("Select a valid %s".formatted(operatorType)))
        );
  }

  @ParameterizedTest(name = "{argumentSetName}")
  @MethodSource("hasErrors")
  void hasErrors_whenOperatorIdIsSameAsPreviousOperator_thenErrorWithMessage(
      OrganisationChangeForm form,
      String operatorType
  ) {
    form.getId().setInputValue(OrganisationUnitTestUtil.ORG_UNIT_ID_1.toString());
    when(organisationUnitQueryService.getOrganisationUnit(OrganisationUnitTestUtil.ORG_UNIT_ID_1))
        .thenReturn(Optional.of(OrganisationUnitTestUtil.ORG_UNIT_1));

    var errors = new BeanPropertyBindingResult(form, "form");
    var result = organisationChangeFormValidator.hasErrors(form, errors, OrganisationUnitTestUtil.ORG_UNIT_ID_1, operatorType);

    var last = operatorType.substring(operatorType.trim().lastIndexOf(' ') + 1).trim();

    assertThat(result).isTrue();
    assertThat(ValidatorTestingUtil.getErrorsFieldsAndMessages(errors))
        .containsOnly(
            entry(OPERATOR_INPUT_CODE,
                List.of("The new %s must be different to the previous %s".formatted(operatorType, last)))
        );
  }

  @ParameterizedTest(name = "{argumentSetName}")
  @MethodSource("hasErrors")
  void hasErrors_whenOperatorIdDiffersFromPreviousOperator_thenNoErrors(
      OrganisationChangeForm form,
      String operatorType
  ) {
    form.getId().setInputValue(OrganisationUnitTestUtil.ORG_UNIT_ID_2.toString());
    when(organisationUnitQueryService.getOrganisationUnit(OrganisationUnitTestUtil.ORG_UNIT_ID_2))
        .thenReturn(Optional.of(OrganisationUnitTestUtil.ORG_UNIT_2));

    var errors = new BeanPropertyBindingResult(form, "form");
    var result = organisationChangeFormValidator.hasErrors(form, errors, OrganisationUnitTestUtil.ORG_UNIT_ID_1, operatorType);

    assertThat(result).isFalse();
    assertThat(errors.hasErrors()).isFalse();
  }

  @ParameterizedTest(name = "{argumentSetName}")
  @MethodSource("hasErrors")
  void hasErrors_whenOperatorIdIsExistingOrganisationUnitAndNoPreviousOperator_thenNoErrors(
      OrganisationChangeForm form,
      String operatorType
  ) {
    form.getId().setInputValue(OrganisationUnitTestUtil.ORG_UNIT_ID_1.toString());
    when(organisationUnitQueryService.getOrganisationUnit(OrganisationUnitTestUtil.ORG_UNIT_ID_1))
        .thenReturn(Optional.of(OrganisationUnitTestUtil.ORG_UNIT_1));

    var errors = new BeanPropertyBindingResult(form, "form");
    var result = organisationChangeFormValidator.hasErrors(form, errors, null, operatorType);

    assertThat(result).isFalse();
    assertThat(errors.hasErrors()).isFalse();
  }

  @ParameterizedTest()
  @ValueSource(strings = {"a", "e", "i", "o", "u", "A", "E", "I", "O", "U"})
  void hasErrors_whenEmptyInput_displaysArticleCorrectly(String vowel) {
    var storageOperatorChangeForm = new StorageOperatorChangeForm();
    var errors = new BeanPropertyBindingResult(storageOperatorChangeForm, "form");
    var result = organisationChangeFormValidator.hasErrors(storageOperatorChangeForm, errors, null, vowel);

    assertThat(result).isTrue();
    assertThat(ValidatorTestingUtil.getErrorsFieldsAndMessages(errors))
        .containsOnly(
            entry(OPERATOR_INPUT_CODE, List.of("Select an %s".formatted(vowel)))
        );
  }

  private static Stream<Arguments> hasErrors() {
    return Stream.of(
        Arguments.argumentSet("Storage operator", new StorageOperatorChangeForm(), STORAGE_OPERATOR_NAME),
        Arguments.argumentSet("Exploration operator", new ExplorationOperatorChangeForm(), EXPLORATION_OPERATOR_NAME),
        Arguments.argumentSet("Licence Administrator", new AdministratorChangeForm(), ADMINISTRATOR_NAME)
    );
  }
}