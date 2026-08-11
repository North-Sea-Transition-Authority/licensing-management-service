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
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.FieldError;
import uk.co.fivium.formlibrary.input.ThreeFieldDateInput;
import uk.co.nstauthority.licensingmanagementservice.licence.status.LicenceStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.status.LicenceStatusService;
import uk.co.nstauthority.licensingmanagementservice.validation.ValidatorTestingUtil;

@ExtendWith(MockitoExtension.class)
class EditLicenceDetailsValidatorTest {

  private static final LocalDate TODAY = LocalDate.of(2024, Month.JUNE, 15);

  private final Licence licence = new Licence();

  @Mock
  private Clock clock;

  @Mock
  private LicenceStatusService licenceStatusService;

  @InjectMocks
  private EditLicenceDetailsValidator editLicenceDetailsValidator;

  @BeforeEach
  void setUp() {
    when(clock.instant()).thenReturn(TODAY.atStartOfDay(ZoneOffset.UTC).toInstant());
    when(clock.getZone()).thenReturn(ZoneOffset.UTC);

    when(licenceStatusService.getLatestLicenceStatus(licence)).thenReturn(Optional.empty());
  }

  private EditLicenceDetailsForm validForm() {
    var form = new EditLicenceDetailsForm();
    form.setLicenceStatus(LicenceStatusType.EXTANT);
    form.getLicenceStatusDate().setDate(TODAY);
    form.setOrganisationUnitIds(List.of("1"));
    return form;
  }

  @Test
  void isValid() {
    var form = validForm();

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(editLicenceDetailsValidator.isValid(form, licence, bindingResult)).isTrue();
  }

  @Test
  void isValid_whenDateAfterPreviousStatusDate_isValid() {
    var form = validForm();

    var previousLicenceStatus = new LicenceStatus();
    previousLicenceStatus.setStatusDate(TODAY.minusDays(10));
    when(licenceStatusService.getLatestLicenceStatus(licence)).thenReturn(Optional.of(previousLicenceStatus));

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(editLicenceDetailsValidator.isValid(form, licence, bindingResult)).isTrue();
  }

  @Test
  void isValid_invalidForm_noLicenceStatus() {
    var form = validForm();
    form.setLicenceStatus(null);

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(editLicenceDetailsValidator.isValid(form, licence, bindingResult)).isFalse();

    assertThat(ValidatorTestingUtil.extractErrors(bindingResult))
        .containsExactly(entry("licenceStatus", Set.of("licenceStatus.required")));
  }

  @Test
  void isValid_invalidForm_noLicenceStatusDate() {
    var form = validForm();
    form.setLicenceStatusDate(new ThreeFieldDateInput("licenceStatusDate", "licence status date"));

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(editLicenceDetailsValidator.isValid(form, licence, bindingResult)).isFalse();

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

    assertThat(editLicenceDetailsValidator.isValid(form, licence, bindingResult)).isFalse();

    assertThat(bindingResult.getFieldErrors())
        .extracting(FieldError::getField, FieldError::getDefaultMessage)
        .containsExactly(
            tuple("licenceStatusDate.dayInput.inputValue", "The date the licence entered this status must not be in the future"),
            tuple("licenceStatusDate.monthInput.inputValue", ""),
            tuple("licenceStatusDate.yearInput.inputValue", "")
        );
  }

  @Test
  void isValid_invalidForm_licenceStatusDateOnPreviousStatusDate() {
    var form = validForm();
    var previousStatusDate = TODAY.minusDays(10);
    form.getLicenceStatusDate().setDate(previousStatusDate);

    var previousLicenceStatus = new LicenceStatus();
    previousLicenceStatus.setStatusDate(previousStatusDate);
    when(licenceStatusService.getLatestLicenceStatus(licence)).thenReturn(Optional.of(previousLicenceStatus));

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(editLicenceDetailsValidator.isValid(form, licence, bindingResult)).isFalse();

    assertThat(bindingResult.getFieldErrors())
        .extracting(FieldError::getField, FieldError::getDefaultMessage)
        .containsExactly(
            tuple("licenceStatusDate.dayInput.inputValue",
                "The date the licence entered this status must be after 5 June 2024"),
            tuple("licenceStatusDate.monthInput.inputValue", ""),
            tuple("licenceStatusDate.yearInput.inputValue", "")
        );
  }

  @Test
  void isValid_invalidForm_licenceStatusDateBeforePreviousStatusDate() {
    var form = validForm();
    var previousStatusDate = TODAY.minusDays(10);
    form.getLicenceStatusDate().setDate(previousStatusDate.minusDays(1));

    var previousLicenceStatus = new LicenceStatus();
    previousLicenceStatus.setStatusDate(previousStatusDate);
    when(licenceStatusService.getLatestLicenceStatus(licence)).thenReturn(Optional.of(previousLicenceStatus));

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(editLicenceDetailsValidator.isValid(form, licence, bindingResult)).isFalse();

    assertThat(bindingResult.getFieldErrors())
        .extracting(FieldError::getField, FieldError::getDefaultMessage)
        .containsExactly(
            tuple("licenceStatusDate.dayInput.inputValue",
                "The date the licence entered this status must be after 5 June 2024"),
            tuple("licenceStatusDate.monthInput.inputValue", ""),
            tuple("licenceStatusDate.yearInput.inputValue", "")
        );
  }

  @Test
  void isValid_invalidForm_noLicensees() {
    var form = validForm();
    form.setOrganisationUnitIds(null);

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(editLicenceDetailsValidator.isValid(form, licence, bindingResult)).isFalse();

    assertThat(ValidatorTestingUtil.extractErrors(bindingResult))
        .containsExactly(entry("organisationUnitSelector", Set.of("organisationUnitSelector.notEmpty")));
  }

}