package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.storageoperator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitTestUtil;
import uk.co.nstauthority.licensingmanagementservice.validation.ValidatorTestingUtil;

@ExtendWith(MockitoExtension.class)
class StorageOperatorChangeFormValidatorTest {

  @Mock
  private OrganisationUnitQueryService organisationUnitQueryService;

  @InjectMocks
  private StorageOperatorChangeFormValidator storageOperatorChangeFormValidator;

  private StorageOperatorChangeForm form;

  private Errors errors;

  private static final String OPERATOR_INPUT_CODE = "operatorId.inputValue";
  private static final String VALID_OPERATOR_MESSAGE = "Select a valid storage operator";

  @BeforeEach
  void setUp() {
    form = new StorageOperatorChangeForm();
    errors = new BeanPropertyBindingResult(form, "form");
  }

  @ParameterizedTest
  @NullAndEmptySource
  void hasErrors_whenOperatorIdEmpty_thenErrorWithMessage(String id) {
    form.getOperatorId().setInputValue(id);
    var result = storageOperatorChangeFormValidator.hasErrors(form, errors, null);

    assertThat(result).isTrue();
    assertThat(ValidatorTestingUtil.getErrorsFieldsAndMessages(errors))
        .containsOnly(
            entry(OPERATOR_INPUT_CODE, List.of("Select a storage operator"))
        );
  }

  @Test
  void hasErrors_whenOperatorIdNotAnExistingOrganisationUnit_thenErrorWithMessage() {
    form.getOperatorId().setInputValue("123");
    when(organisationUnitQueryService.getOrganisationUnit(123)).thenReturn(Optional.empty());

    var result = storageOperatorChangeFormValidator.hasErrors(form, errors, OrganisationUnitTestUtil.ORG_UNIT_ID_1);

    assertThat(result).isTrue();
    assertThat(ValidatorTestingUtil.getErrorsFieldsAndMessages(errors))
        .containsOnly(
            entry(OPERATOR_INPUT_CODE, List.of(VALID_OPERATOR_MESSAGE))
        );
  }

  @Test
  void hasErrors_whenOperatorIdNotNumeric_thenErrorWithMessage() {
    form.getOperatorId().setInputValue("not-a-number");

    var result = storageOperatorChangeFormValidator.hasErrors(form, errors, OrganisationUnitTestUtil.ORG_UNIT_ID_1);

    assertThat(result).isTrue();
    assertThat(ValidatorTestingUtil.getErrorsFieldsAndMessages(errors))
        .containsOnly(
            entry(OPERATOR_INPUT_CODE, List.of(VALID_OPERATOR_MESSAGE))
        );
  }

  @Test
  void hasErrors_whenOperatorIdIsSameAsPreviousOperator_thenErrorWithMessage() {
    form.getOperatorId().setInputValue(OrganisationUnitTestUtil.ORG_UNIT_ID_1.toString());
    when(organisationUnitQueryService.getOrganisationUnit(OrganisationUnitTestUtil.ORG_UNIT_ID_1))
        .thenReturn(Optional.of(OrganisationUnitTestUtil.ORG_UNIT_1));

    var result = storageOperatorChangeFormValidator.hasErrors(form, errors, OrganisationUnitTestUtil.ORG_UNIT_ID_1);

    assertThat(result).isTrue();
    assertThat(ValidatorTestingUtil.getErrorsFieldsAndMessages(errors))
        .containsOnly(
            entry(OPERATOR_INPUT_CODE,
                List.of("The new storage operator must be different to the previous operator"))
        );
  }

  @Test
  void hasErrors_whenOperatorIdDiffersFromPreviousOperator_thenNoErrors() {
    form.getOperatorId().setInputValue(OrganisationUnitTestUtil.ORG_UNIT_ID_2.toString());
    when(organisationUnitQueryService.getOrganisationUnit(OrganisationUnitTestUtil.ORG_UNIT_ID_2))
        .thenReturn(Optional.of(OrganisationUnitTestUtil.ORG_UNIT_2));

    var result = storageOperatorChangeFormValidator.hasErrors(form, errors, OrganisationUnitTestUtil.ORG_UNIT_ID_1);

    assertThat(result).isFalse();
    assertThat(errors.hasErrors()).isFalse();
  }

  @Test
  void hasErrors_whenOperatorIdIsExistingOrganisationUnitAndNoPreviousOperator_thenNoErrors() {
    form.getOperatorId().setInputValue(OrganisationUnitTestUtil.ORG_UNIT_ID_1.toString());
    when(organisationUnitQueryService.getOrganisationUnit(OrganisationUnitTestUtil.ORG_UNIT_ID_1))
        .thenReturn(Optional.of(OrganisationUnitTestUtil.ORG_UNIT_1));

    var result = storageOperatorChangeFormValidator.hasErrors(form, errors, null);

    assertThat(result).isFalse();
    assertThat(errors.hasErrors()).isFalse();
  }
}