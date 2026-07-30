package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.when;

import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.validation.ValidatorTestingUtil;

@ExtendWith(MockitoExtension.class)
class AddPositionChangeFormValidatorTest {

  @Mock
  private LicenceService licenceService;

  @Mock
  private LicencePositionCorrectionService licencePositionCorrectionService;

  @InjectMocks
  private AddPositionChangeFormValidator addPositionChangeFormValidator;

  private AddPositionChangeForm form;
  private Errors errors;
  private LicenceCorrection correction;
  private LicencePositionCorrection positionCorrection;

  @BeforeEach
  void setUp() {
    form = new AddPositionChangeForm();
    errors = new BeanPropertyBindingResult(form, "form");
    correction = LicenceCorrectionTestUtil.newBuilder().build();
    positionCorrection = LicencePositionCorrectionTestUtil.newBuilder().build();
  }

  @Test
  void hasErrors_whenChangeTypeEmpty_thenErrorWithMessage() {
    var result = addPositionChangeFormValidator.hasErrors(form, errors, correction, positionCorrection);

    assertThat(result).isTrue();
    assertThat(ValidatorTestingUtil.getErrorsFieldsAndMessages(errors))
        .containsOnly(entry("changeType", Collections.singletonList("Select the type of change to add")));
  }

  @Test
  void hasErrors_whenChangeTypeNotRecognised_thenErrorWithMessage() {
    form.setChangeType("NOT_A_CHANGE_TYPE");

    var result = addPositionChangeFormValidator.hasErrors(form, errors, correction, positionCorrection);

    assertThat(result).isTrue();
    assertThat(ValidatorTestingUtil.getErrorsFieldsAndMessages(errors))
        .containsOnly(entry("changeType", Collections.singletonList("Select the type of change to add")));
  }

  @Test
  void hasErrors_whenSetEquityAndLicenceNotCarbonStorage_thenErrorWithMessage() {
    form.setChangeType(AddPositionChangeType.SET_EQUITY.name());
    when(licenceService.isCarbonStorageLicence(correction.getLicence())).thenReturn(false);

    var result = addPositionChangeFormValidator.hasErrors(form, errors, correction, positionCorrection);

    assertThat(result).isTrue();
    assertThat(ValidatorTestingUtil.getErrorsFieldsAndMessages(errors))
        .containsOnly(entry("changeType", Collections.singletonList("Select the type of change to add")));
  }

  @Test
  void hasErrors_whenSetEquityAlreadyExistsForPosition_thenErrorWithMessage() {
    form.setChangeType(AddPositionChangeType.SET_EQUITY.name());
    when(licenceService.isCarbonStorageLicence(correction.getLicence())).thenReturn(true);
    when(licencePositionCorrectionService.setEquityChangeExists(positionCorrection)).thenReturn(true);

    var result = addPositionChangeFormValidator.hasErrors(form, errors, correction, positionCorrection);

    assertThat(result).isTrue();
    assertThat(ValidatorTestingUtil.getErrorsFieldsAndMessages(errors))
        .containsOnly(entry("changeType",
            Collections.singletonList("Set equity has already been added to this position")));
  }

  @Test
  void hasErrors_whenSetEquityDoesNotExistForPosition_thenNoErrors() {
    form.setChangeType(AddPositionChangeType.SET_EQUITY.name());
    when(licenceService.isCarbonStorageLicence(correction.getLicence())).thenReturn(true);
    when(licencePositionCorrectionService.setEquityChangeExists(positionCorrection)).thenReturn(false);

    var result = addPositionChangeFormValidator.hasErrors(form, errors, correction, positionCorrection);

    assertThat(result).isFalse();
    assertThat(errors.hasErrors()).isFalse();
  }

  @Test
  void hasErrors_whenAdministratorChange_thenNoDuplicateCheckAndNoErrors() {
    form.setChangeType(AddPositionChangeType.ADMINISTRATOR_CHANGE.name());

    var result = addPositionChangeFormValidator.hasErrors(form, errors, correction, positionCorrection);

    assertThat(result).isFalse();
    assertThat(errors.hasErrors()).isFalse();
  }
}