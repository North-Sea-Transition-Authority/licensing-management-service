package uk.co.nstauthority.licensingmanagementservice.licence;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.FieldError;
import uk.co.fivium.formlibrary.input.ThreeFieldDateInput;
import uk.co.nstauthority.licensingmanagementservice.validation.ValidatorTestingUtil;

@ExtendWith(MockitoExtension.class)
class NewLicenceValidatorTest {

  private static final LocalDate TODAY = LocalDate.of(2024, Month.JUNE, 15);

  @Mock
  private LicenceService licenceService;

  @Mock
  private Clock clock;

  @InjectMocks
  private NewLicenceValidator newLicenceValidator;

  @BeforeEach
  void setUp() {
    when(clock.instant()).thenReturn(TODAY.atStartOfDay(ZoneOffset.UTC).toInstant());
    when(clock.getZone()).thenReturn(ZoneOffset.UTC);
  }

  private NewLicenceForm validForm() {
    var form = new NewLicenceForm();
    form.setLicenceType(LicenceType.CARBON_STORAGE);
    form.setLicenceNumber("001");
    form.setLicenceStatus(LicenceStatusType.EXTANT);
    form.getLicenceStatusDate().setDate(TODAY);
    form.setOrganisationUnitIds(List.of("1"));
    return form;
  }

  @Test
  void isValid() {
    var form = validForm();

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(newLicenceValidator.isValid(form, bindingResult)).isTrue();
  }

  @Test
  void isValid_invalidForm_noLicenceType() {
    var form = validForm();
    form.setLicenceType(null);

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(newLicenceValidator.isValid(form, bindingResult)).isFalse();

    assertThat(ValidatorTestingUtil.extractErrors(bindingResult))
        .containsExactly(entry("licenceType", Set.of("licenceType.required")));
  }

  @Test
  void isValid_invalidForm_noLicenceNumber() {
    var form = validForm();
    form.setLicenceNumber(null);

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(newLicenceValidator.isValid(form, bindingResult)).isFalse();

    assertThat(ValidatorTestingUtil.extractErrors(bindingResult))
        .containsExactly(entry("licenceNumber", Set.of("licenceNumber.required")));
  }

  @Test
  void isValid_invalidForm_invalidLicenceNumber() {
    var form = validForm();
    form.setLicenceNumber("CS001");

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(newLicenceValidator.isValid(form, bindingResult)).isFalse();

    assertThat(ValidatorTestingUtil.extractErrors(bindingResult))
        .containsExactly(entry("licenceNumber", Set.of("licenceNumber.invalid")));
  }

  @Test
  void isValid_invalidForm_licenceNumberAlreadyExistsForType() {
    var form = validForm();

    when(licenceService.licenceNumberExistsForType(LicenceType.CARBON_STORAGE, "001")).thenReturn(true);

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(newLicenceValidator.isValid(form, bindingResult)).isFalse();

    assertThat(ValidatorTestingUtil.getErrorsFieldsAndMessages(bindingResult))
        .containsExactly(entry("licenceNumber", List.of("The licence number already exists for the selected licence type")));
  }

  @Test
  void isValid_invalidForm_noLicenceStatus() {
    var form = validForm();
    form.setLicenceStatus(null);

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(newLicenceValidator.isValid(form, bindingResult)).isFalse();

    assertThat(ValidatorTestingUtil.extractErrors(bindingResult))
        .containsExactly(entry("licenceStatus", Set.of("licenceStatus.required")));
  }

  @Test
  void isValid_invalidForm_noLicenceStatusDate() {
    var form = validForm();
    form.setLicenceStatusDate(new ThreeFieldDateInput(
        "licenceStatusDate", "the date the licence entered this status"));

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(newLicenceValidator.isValid(form, bindingResult)).isFalse();

    assertThat(bindingResult.getFieldErrors())
        .extracting(FieldError::getField, FieldError::getDefaultMessage)
        .containsExactly(
            tuple("licenceStatusDate.dayInput.inputValue", "Enter the date the licence entered this status"),
            tuple("licenceStatusDate.monthInput.inputValue", ""),
            tuple("licenceStatusDate.yearInput.inputValue", "")
        );
  }

  @Test
  void isValid_invalidForm_licenceStatusDateInFuture() {
    var form = validForm();
    form.getLicenceStatusDate().setDate(TODAY.plusDays(1));

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(newLicenceValidator.isValid(form, bindingResult)).isFalse();

    assertThat(bindingResult.getFieldErrors())
        .extracting(FieldError::getField, FieldError::getDefaultMessage)
        .containsExactly(
            tuple("licenceStatusDate.dayInput.inputValue", "The date the licence entered this status must not be in the future"),
            tuple("licenceStatusDate.monthInput.inputValue", ""),
            tuple("licenceStatusDate.yearInput.inputValue", "")
        );
  }

  @Test
  void isValid_invalidForm_noLicensees() {
    var form = validForm();
    form.setOrganisationUnitIds(null);

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(newLicenceValidator.isValid(form, bindingResult)).isFalse();

    assertThat(ValidatorTestingUtil.extractErrors(bindingResult))
        .containsExactly(entry("organisationUnitSelector", Set.of("organisationUnitSelector.notEmpty")));
  }
}