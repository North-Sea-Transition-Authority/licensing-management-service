package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.when;

import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.PartialSurrenderCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.AdministratorOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.PartialSurrenderOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SetEquityOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeService;
import uk.co.nstauthority.licensingmanagementservice.validation.ValidatorTestingUtil;

@ExtendWith(MockitoExtension.class)
class AddPositionChangeFormValidatorTest {

  private static final String SELECT_A_CHANGE_TYPE = "Select the type of change to add";

  @Mock
  private LicencePositionChangeService licencePositionChangeService;

  @Mock
  private PartialSurrenderCorrectionService partialSurrenderCorrectionService;

  @InjectMocks
  private AddPositionChangeFormValidator addPositionChangeFormValidator;

  private AddPositionChangeForm form;
  private Errors errors;
  private LicencePositionCorrection positionCorrection;

  @BeforeEach
  void setUp() {
    form = new AddPositionChangeForm();
    errors = new BeanPropertyBindingResult(form, "form");
    positionCorrection = LicencePositionCorrectionTestUtil.newBuilder().build();
  }

  private LicenceCorrection correctionForLicenceType(LicenceType licenceType) {
    return LicenceCorrectionTestUtil.newBuilder()
        .withLicence(LicenceTestUtil.builder().withLicenceType(licenceType).build())
        .build();
  }

  @Test
  void hasErrors_whenChangeTypeEmpty_thenErrorWithMessage() {
    var result = addPositionChangeFormValidator.hasErrors(
        form, errors, correctionForLicenceType(LicenceType.SEAWARD_PRODUCTION), positionCorrection);

    assertThat(result).isTrue();
    assertThat(ValidatorTestingUtil.getErrorsFieldsAndMessages(errors))
        .containsOnly(entry("changeType", Collections.singletonList(SELECT_A_CHANGE_TYPE)));
  }

  @Test
  void hasErrors_whenChangeTypeNotRecognised_thenErrorWithMessage() {
    form.setChangeType("NOT_A_CHANGE_TYPE");

    var result = addPositionChangeFormValidator.hasErrors(
        form, errors, correctionForLicenceType(LicenceType.SEAWARD_PRODUCTION), positionCorrection);

    assertThat(result).isTrue();
    assertThat(ValidatorTestingUtil.getErrorsFieldsAndMessages(errors))
        .containsOnly(entry("changeType", Collections.singletonList(SELECT_A_CHANGE_TYPE)));
  }

  @Test
  void hasErrors_whenSetEquityAndLicenceNotCarbonStorage_thenErrorWithMessage() {
    form.setChangeType(AddPositionChangeType.SET_EQUITY.name());

    var result = addPositionChangeFormValidator.hasErrors(
        form, errors, correctionForLicenceType(LicenceType.SEAWARD_PRODUCTION), positionCorrection);

    assertThat(result).isTrue();
    assertThat(ValidatorTestingUtil.getErrorsFieldsAndMessages(errors))
        .containsOnly(entry("changeType", Collections.singletonList(SELECT_A_CHANGE_TYPE)));
  }

  @Test
  void hasErrors_whenSetEquityAlreadyExistsForPosition_thenErrorWithMessage() {
    form.setChangeType(AddPositionChangeType.SET_EQUITY.name());
    when(licencePositionChangeService.changeExists(positionCorrection.getTargetLicencePosition().getId(), SetEquityOperation.class))
        .thenReturn(true);

    var result = addPositionChangeFormValidator.hasErrors(
        form, errors, correctionForLicenceType(LicenceType.CARBON_STORAGE), positionCorrection);

    assertThat(result).isTrue();
    assertThat(ValidatorTestingUtil.getErrorsFieldsAndMessages(errors))
        .containsOnly(entry("changeType",
            Collections.singletonList("Set equity has already been added to this position")));
  }

  @Test
  void hasErrors_whenSetEquityDoesNotExistForPosition_thenNoErrors() {
    form.setChangeType(AddPositionChangeType.SET_EQUITY.name());
    when(licencePositionChangeService.changeExists(positionCorrection.getTargetLicencePosition().getId(), SetEquityOperation.class))
        .thenReturn(false);

    var result = addPositionChangeFormValidator.hasErrors(
        form, errors, correctionForLicenceType(LicenceType.CARBON_STORAGE), positionCorrection);

    assertThat(result).isFalse();
    assertThat(errors.hasErrors()).isFalse();
  }

  @Test
  void hasErrors_whenAdministratorChangeAndNoneExistsForPosition_thenNoErrors() {
    form.setChangeType(AddPositionChangeType.ADMINISTRATOR_CHANGE.name());
    when(licencePositionChangeService.changeExists(positionCorrection.getTargetLicencePosition().getId(), AdministratorOperation.class))
        .thenReturn(false);

    var result = addPositionChangeFormValidator.hasErrors(
        form, errors, correctionForLicenceType(LicenceType.SEAWARD_PRODUCTION), positionCorrection);

    assertThat(result).isFalse();
    assertThat(errors.hasErrors()).isFalse();
  }

  @Test
  void hasErrors_whenAdministratorChangeAndLicenceIsCarbonStorage_thenErrorWithMessage() {
    form.setChangeType(AddPositionChangeType.ADMINISTRATOR_CHANGE.name());

    var result = addPositionChangeFormValidator.hasErrors(
        form, errors, correctionForLicenceType(LicenceType.CARBON_STORAGE), positionCorrection);

    assertThat(result).isTrue();
    assertThat(ValidatorTestingUtil.getErrorsFieldsAndMessages(errors))
        .containsOnly(entry("changeType", Collections.singletonList("Select the type of change to add")));
  }

  @Test
  void hasErrors_whenAdministratorChangeAlreadyExistsForPosition_thenErrorWithMessage() {
    form.setChangeType(AddPositionChangeType.ADMINISTRATOR_CHANGE.name());
    when(licencePositionChangeService.changeExists(positionCorrection.getTargetLicencePosition().getId(), AdministratorOperation.class))
        .thenReturn(true);

    var result = addPositionChangeFormValidator.hasErrors(
        form, errors, correctionForLicenceType(LicenceType.SEAWARD_PRODUCTION), positionCorrection);

    assertThat(result).isTrue();
    assertThat(ValidatorTestingUtil.getErrorsFieldsAndMessages(errors))
        .containsOnly(entry("changeType",
            Collections.singletonList("Administrator change has already been added to this position")));
  }

  @ParameterizedTest
  @EnumSource(value = LicenceType.class, names = {"SEAWARD_PRODUCTION", "LANDWARD_PRODUCTION" }, mode = EnumSource.Mode.EXCLUDE)
  void hasErrors_whenPartialSurrenderAndLicenceNotProduction_thenErrorWithMessage(LicenceType licenceType) {
    form.setChangeType(AddPositionChangeType.PARTIAL_SURRENDER.name());

    var result = addPositionChangeFormValidator.hasErrors(
        form, errors, correctionForLicenceType(licenceType), positionCorrection);

    assertThat(result).isTrue();
    assertThat(ValidatorTestingUtil.getErrorsFieldsAndMessages(errors))
        .containsOnly(entry("changeType", Collections.singletonList(SELECT_A_CHANGE_TYPE)));
  }

  @Test
  void hasErrors_whenPartialSurrenderAlreadyExecutedOnPosition_thenErrorWithMessage() {
    form.setChangeType(AddPositionChangeType.PARTIAL_SURRENDER.name());
    when(licencePositionChangeService.changeExists(
        positionCorrection.getTargetLicencePosition().getId(), PartialSurrenderOperation.class))
        .thenReturn(true);

    var result = addPositionChangeFormValidator.hasErrors(
        form, errors, correctionForLicenceType(LicenceType.SEAWARD_PRODUCTION), positionCorrection);

    assertThat(result).isTrue();
    assertThat(ValidatorTestingUtil.getErrorsFieldsAndMessages(errors))
        .containsOnly(entry("changeType",
            Collections.singletonList("Partial surrender has already been added to this position")));
  }

  @Test
  void hasErrors_whenPartialSurrenderAlreadyStagedInThisCorrection_thenErrorWithMessage() {
    form.setChangeType(AddPositionChangeType.PARTIAL_SURRENDER.name());
    when(licencePositionChangeService.changeExists(
        positionCorrection.getTargetLicencePosition().getId(), PartialSurrenderOperation.class))
        .thenReturn(false);
    when(partialSurrenderCorrectionService.hasStagedPartialSurrender(positionCorrection)).thenReturn(true);

    var result = addPositionChangeFormValidator.hasErrors(
        form, errors, correctionForLicenceType(LicenceType.SEAWARD_PRODUCTION), positionCorrection);

    assertThat(result).isTrue();
    assertThat(ValidatorTestingUtil.getErrorsFieldsAndMessages(errors))
        .containsOnly(entry("changeType",
            Collections.singletonList("Partial surrender has already been added to this position")));
  }

  @Test
  void hasErrors_whenPartialSurrenderDoesNotExistForPosition_thenNoErrors() {
    form.setChangeType(AddPositionChangeType.PARTIAL_SURRENDER.name());
    when(licencePositionChangeService.changeExists(
        positionCorrection.getTargetLicencePosition().getId(), PartialSurrenderOperation.class))
        .thenReturn(false);
    when(partialSurrenderCorrectionService.hasStagedPartialSurrender(positionCorrection)).thenReturn(false);

    var result = addPositionChangeFormValidator.hasErrors(
        form, errors, correctionForLicenceType(LicenceType.LANDWARD_PRODUCTION), positionCorrection);

    assertThat(result).isFalse();
    assertThat(errors.hasErrors()).isFalse();
  }
}
